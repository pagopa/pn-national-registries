package it.pagopa.pn.national.registries.client.infocamere;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.RegisteredClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.model.SSLData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_ANPR;

@Slf4j
@Component
public class InfoCamereJwsGenerator {

    private final String aud;
    private final String clientId;
    private final KmsClient kmsClient;
    private final String infoCamereAuthRestSecret;
    private final SsmParameterConsumerActivation ssmParameterConsumerActivation;
    private static final Pattern myRegex = Pattern.compile("=+$");
    private static final Pattern certRegex = Pattern.compile("(?<=-----BEGIN CERTIFICATE-----)[\\s\\S]*?(?=-----END CERTIFICATE-----)");

    public InfoCamereJwsGenerator(KmsClient kmsClient,
                                  SsmParameterConsumerActivation ssmParameterConsumerActivation,
                                  @Value("${pn.national.registries.infocamere.base-path}") String aud,
                                  @Value("${pn.national.registries.infocamere.client-id}") String clientId,
                                  @Value("${pn.national.registries.ssm.infocamere.auth-rest}") String infoCamereAuthRestSecret) {
        this.aud = aud;
        this.clientId = clientId;
        this.kmsClient = kmsClient;
        this.ssmParameterConsumerActivation = ssmParameterConsumerActivation;
        this.infoCamereAuthRestSecret = infoCamereAuthRestSecret;
    }

    public String createAuthRest(String scope) {
        log.info("start to createAuthRest");
        long startTime = System.currentTimeMillis();
        try {
            Optional<SSLData> optSslData = ssmParameterConsumerActivation.getAuthParameter(infoCamereAuthRestSecret, SSLData.class);
            if (optSslData.isPresent()) {

                ObjectMapper mapper = new ObjectMapper();
                String headerBase64String = jsonObjectToUrlSafeBase64String(mapper.writeValueAsString(createHeaderMap(optSslData.get())));
                String payloadBase64String = jsonObjectToUrlSafeBase64String(mapper.writeValueAsString(createClaimMap(scope)));
                String jwtContent = headerBase64String + "." + payloadBase64String;

                SdkBytes awsBytesJwtContent = SdkBytes.fromByteArray(jwtContent.getBytes(StandardCharsets.UTF_8));
                SignRequest signRequest = SignRequest.builder()
                        .message(awsBytesJwtContent)
                        .messageType(MessageType.RAW)
                        .signingAlgorithm(SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256)
                        .keyId(optSslData.get().getKeyId())
                        .build();

                log.info("START - KmsClient.sign Request: {}",
                        signRequest);
                long startTimeKms = System.currentTimeMillis();
                SignResponse signResult = kmsClient.sign(signRequest);
                log.info("END - KmsClient.sign Timelapse: {} ms", System.currentTimeMillis() - startTimeKms);

                byte[] signature = signResult.signature().asByteArray();
                String signatureString = bytesToUrlSafeBase64String(signature);
                log.info("END - AdigJwtSignature.createAgidJwt Timelapse: {} ms", System.currentTimeMillis() - startTime);
                return jwtContent + "." + signatureString;
            } else {
                throw new PnInternalException(ERROR_MESSAGE_ANPR, ERROR_CODE_ANPR);
            }
        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_INFOCAMERE, ERROR_CODE_INFOCAMERE, e);
        }
    }

    private Map<String, Object> createHeaderMap(SSLData sslData) {
        Map<String, Object> map = new HashMap<>();
        String x5c = "";
        map.put(HeaderParams.TYPE, "JWT");
        map.put(HeaderParams.ALGORITHM, "ES256");
        byte[] cert = Base64.getDecoder().decode(sslData.getCert());
        String certString = new String(cert, StandardCharsets.UTF_8);
        final Matcher matcher = certRegex.matcher(certString);
        while (matcher.find()) {
            x5c = matcher.group()
                    .replaceAll(Pattern.compile("\\n").pattern(), "")
                    .replaceAll(Pattern.compile("\\r").pattern(), "");
        }
        map.put("x5c", List.of(x5c));
        map.put("use", "sig");
        log.debug("HeaderMap type: {}, alg: {}",map.get(HeaderParams.TYPE), map.get(HeaderParams.ALGORITHM));
        return map;
    }

    private Map<String, Object> createClaimMap(String scope) {
        Map<String, Object> map = new HashMap<>();
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expireSeconds = nowSeconds + 360;

        map.put(RegisteredClaims.AUDIENCE, aud);
        map.put(RegisteredClaims.EXPIRES_AT, expireSeconds);
        map.put(RegisteredClaims.ISSUER, clientId);
        map.put(RegisteredClaims.SUBJECT, clientId);
        map.put(RegisteredClaims.JWT_ID, UUID.randomUUID().toString());
        map.put("scope", scope);

        log.debug("ClaimMap audience: {}", map.get(RegisteredClaims.AUDIENCE));
        return map;
    }

    private String stringToUrlSafeBase64String(String inString) {
        byte[] jsonBytes = inString.getBytes(StandardCharsets.UTF_8);
        return bytesToUrlSafeBase64String(jsonBytes);
    }

    private String bytesToUrlSafeBase64String(byte[] bytes) {
        byte[] base64JsonBytes = Base64Utils.encodeUrlSafe(bytes);
        return new String(base64JsonBytes, StandardCharsets.UTF_8)
                .replaceFirst(String.valueOf(myRegex), "");
    }

    private String jsonObjectToUrlSafeBase64String(String jsonString) {
        return stringToUrlSafeBase64String(jsonString);
    }
}

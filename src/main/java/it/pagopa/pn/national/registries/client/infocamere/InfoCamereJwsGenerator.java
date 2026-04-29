package it.pagopa.pn.national.registries.client.infocamere;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.RegisteredClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.model.SSLData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_INFOCAMERE;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INFOCAMERE;

@Slf4j
@Component
@RequiredArgsConstructor
public class InfoCamereJwsGenerator {

    private final KmsClient kmsClient;
    private final SsmParameterConsumerActivation ssmParameterConsumerActivation;
    private final NationalRegistriesConfig nationalRegistriesConfig;
    private static final Pattern myRegex = Pattern.compile("=+$");
    private static final Pattern certRegex = Pattern.compile("(?<=-----BEGIN CERTIFICATE-----)[\\s\\S]*?(?=-----END CERTIFICATE-----)");
    private static final int SECONDS_TO_ADD_TO_EXPIRE = 360;
    private static final String REGEX_NEW_LINE = "\\n";
    private static final String REGEX_CARRIAGE_RETURN = "\\r";

    private static final Pattern PATTERN_NEW_LINE = Pattern.compile(REGEX_NEW_LINE);
    private static final Pattern PATTERN_CARRIAGE_RETURN = Pattern.compile(REGEX_CARRIAGE_RETURN);


    public String createAuthRest(String scope) {
        log.info("start to createAuthRest");
        long startTime = System.currentTimeMillis();
        try {
            Optional<SSLData> optSslData = ssmParameterConsumerActivation.getParameterValue(nationalRegistriesConfig.getInfoCamere().getAuth(), SSLData.class);
            if (optSslData.isEmpty()) {
                throw new PnInternalException(ERROR_MESSAGE_INFOCAMERE, ERROR_CODE_INFOCAMERE);
            }
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

            long startTimeKms = System.currentTimeMillis();
            SignResponse signResult = kmsClient.sign(signRequest);
            log.info("END - KmsClient.sign Timelapse: {} ms", System.currentTimeMillis() - startTimeKms);

            byte[] signature = signResult.signature().asByteArray();
            String signatureString = bytesToUrlSafeBase64String(signature);
            log.info("END - AdigJwtSignature.createAgidJwt Timelapse: {} ms", System.currentTimeMillis() - startTime);
            return jwtContent + "." + signatureString;
        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_INFOCAMERE, ERROR_CODE_INFOCAMERE, e);
        }
    }

    private Map<String, Object> createHeaderMap(SSLData sslData) {
        Map<String, Object> map = new HashMap<>();
        String x5c = "";
        map.put(HeaderParams.TYPE, "JWT");
        map.put(HeaderParams.ALGORITHM, "RS256");
        byte[] cert = Base64.getDecoder().decode(sslData.getCert());
        String certString = new String(cert, StandardCharsets.UTF_8);
        final Matcher matcher = certRegex.matcher(certString);
        while (matcher.find()) {
            x5c = matcher.group()
                    .replaceAll(PATTERN_NEW_LINE.pattern(), "")
                    .replaceAll(PATTERN_CARRIAGE_RETURN.pattern(), "");
        }
        map.put("x5c", List.of(x5c));
        map.put("use", "sig");
        log.debug("HeaderMap type: {}, alg: {}", map.get(HeaderParams.TYPE), map.get(HeaderParams.ALGORITHM));
        return map;
    }

    private Map<String, Object> createClaimMap(String scope) {
        Map<String, Object> map = new HashMap<>();
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expireSeconds = nowSeconds + SECONDS_TO_ADD_TO_EXPIRE;

        map.put(RegisteredClaims.AUDIENCE, nationalRegistriesConfig.getInfoCamere().getBaseUrl());
        map.put(RegisteredClaims.EXPIRES_AT, expireSeconds);
        map.put(RegisteredClaims.ISSUER, nationalRegistriesConfig.getInfoCamere().getClientId());
        map.put(RegisteredClaims.SUBJECT, nationalRegistriesConfig.getInfoCamere().getClientId());
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
        byte[] base64JsonBytes = Base64.getUrlEncoder().encode(bytes);
        return new String(base64JsonBytes, StandardCharsets.UTF_8)
                .replaceFirst(String.valueOf(myRegex), "");
    }

    private String jsonObjectToUrlSafeBase64String(String jsonString) {
        return stringToUrlSafeBase64String(jsonString);
    }
}

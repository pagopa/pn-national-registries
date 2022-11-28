package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.SecretValue;
import it.pagopa.pn.national.registries.model.TokenHeader;
import it.pagopa.pn.national.registries.model.TokenPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_CLIENTASSERTION;
import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_MESSAGE_CLIENTASSERTION;

@Slf4j
@Component
public class PdndAssertionGenerator {

    private static final Pattern myRegex = Pattern.compile("=+$");
    private final KmsClient kmsClient;

    public PdndAssertionGenerator(KmsClient kmsClient) {
        this.kmsClient = kmsClient;
    }

    public String generateClientAssertion(SecretValue jwtCfg){
        log.info("START - PdndAssertionsGenerator.generateClientAssertion");
        long startTime = System.currentTimeMillis();
        try {
            TokenHeader th = new TokenHeader(jwtCfg.getJwtConfig());
            TokenPayload tp = new TokenPayload(jwtCfg.getJwtConfig());
            ObjectMapper mapper = new ObjectMapper();

            String headerBase64String = jsonObjectToUrlSafeBase64String(mapper.writeValueAsString(th));
            String payloadBase64String = jsonObjectToUrlSafeBase64String(mapper.writeValueAsString(tp));
            String jwtContent = headerBase64String + "." + payloadBase64String;

            SdkBytes awsBytesJwtContent = SdkBytes.fromByteArray(jwtContent.getBytes(StandardCharsets.UTF_8));
            SignRequest signRequest = SignRequest.builder()
                    .message(awsBytesJwtContent)
                    .messageType(MessageType.RAW)
                    .signingAlgorithm(SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256)
                    .keyId(jwtCfg.getKeyId())
                    .build();

            log.info("START - KmsClient.sign Request: {}",
                    signRequest);
            long startTimeKms = System.currentTimeMillis();
            SignResponse signResult = kmsClient.sign(signRequest);
            log.info("END - KmsClient.sign Timelapse: {} ms", System.currentTimeMillis() - startTimeKms);

            byte[] signature = signResult.signature().asByteArray();
            String signatureString = bytesToUrlSafeBase64String(signature);
            log.info("END - PdndAssertionGenerator.generateClientAssertion Timelapse: {} ms", System.currentTimeMillis() - startTime);
            return jwtContent + "." + signatureString;

        } catch (Exception e) {
            throw new PnInternalException(ERROR_MESSAGE_CLIENTASSERTION, ERROR_CODE_CLIENTASSERTION,e);
        }
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

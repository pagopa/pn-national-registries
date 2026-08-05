package it.pagopa.pn.national.registries.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.TokenHeader;
import it.pagopa.pn.national.registries.model.TokenPayload;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@lombok.CustomLog
public class ClientUtils {

    private ClientUtils(){}

    private static final Pattern myRegex = Pattern.compile("=+$");

    public static String createSignature(SignResponse signResult){
        byte[] signature = signResult.signature().asByteArray();
        return bytesToUrlSafeBase64String(signature);
    }

    public static SignRequest createSignRequest(String jwtContent, String keyId){
        SdkBytes awsBytesJwtContent = SdkBytes.fromByteArray(jwtContent.getBytes(StandardCharsets.UTF_8));
        return SignRequest.builder()
                .message(awsBytesJwtContent)
                .messageType(MessageType.RAW)
                .signingAlgorithm(SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256)
                .keyId(keyId)
                .build();
    }

    public static String createJwtContent(Map<String, Object> header, Map<String, Object> payload) throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();

            String headerString = mapper.writeValueAsString(header);
            String payloadString = mapper.writeValueAsString(payload);

            String headerBase64String = jsonObjectToUrlSafeBase64String(headerString);
            String payloadBase64String = jsonObjectToUrlSafeBase64String(payloadString);

            return headerBase64String + "." + payloadBase64String;
    }

    public static String createJwtContent(TokenHeader header, TokenPayload payload) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String headerString = mapper.writeValueAsString(header);
        String payloadString = mapper.writeValueAsString(payload);

        String headerBase64String = jsonObjectToUrlSafeBase64String(headerString);
        String payloadBase64String = jsonObjectToUrlSafeBase64String(payloadString);

        return headerBase64String + "." + payloadBase64String;
    }

    private static String stringToUrlSafeBase64String(String inString) {
        byte[] jsonBytes = inString.getBytes(StandardCharsets.UTF_8);
        return bytesToUrlSafeBase64String(jsonBytes);
    }

    private static String bytesToUrlSafeBase64String(byte[] bytes) {
        byte[] base64JsonBytes = Base64.getUrlEncoder().encode(bytes);
        return new String(base64JsonBytes, StandardCharsets.UTF_8)
                .replaceFirst(String.valueOf(myRegex), "");
    }

    private static String jsonObjectToUrlSafeBase64String(String jsonString) {
        return stringToUrlSafeBase64String(jsonString);
    }
}

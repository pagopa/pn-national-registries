package it.pagopa.pn.national.registries.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.TokenHeader;
import it.pagopa.pn.national.registries.model.TokenPayload;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@lombok.CustomLog
public class ClientUtils {

    private static final Pattern myRegex = Pattern.compile("=+$");

    public static RSAPublicKey getPublicKey(String pub) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        log.debug("start getPublicKey");
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(pub));
        X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(is.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(encodedKeySpec);
    }

    public static  RSAPrivateKey getPrivateKey(String key) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        log.debug("start getPrivateKey");
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(key));
        PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(encodedKeySpec);
    }

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
        byte[] base64JsonBytes = Base64Utils.encodeUrlSafe(bytes);
        return new String(base64JsonBytes, StandardCharsets.UTF_8)
                .replaceFirst(String.valueOf(myRegex), "");
    }

    private static String jsonObjectToUrlSafeBase64String(String jsonString) {
        return stringToUrlSafeBase64String(jsonString);
    }
}

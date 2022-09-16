package it.pagopa.pn.national.registries;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.SecretValue;
import it.pagopa.pn.national.registries.utils.TokenProviderUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.nio.charset.StandardCharsets;

import static it.pagopa.pn.national.registries.utils.TokenProviderUtils.bytesToUrlSafeBase64String;
import static it.pagopa.pn.national.registries.utils.TokenProviderUtils.jsonObjectToUrlSafeBase64String;


@Slf4j
@Component
public class PdndAssertionGenerator {

    private final KmsClient kmsClient;

    public PdndAssertionGenerator(KmsClient kmsClient) {
        this.kmsClient = kmsClient;
    }

    public String generateClientAssertion(SecretValue jwtCfg) throws Exception {
        try {

            TokenProviderUtils.TokenHeader th = new TokenProviderUtils.TokenHeader(jwtCfg.getJwtConfig());
            TokenProviderUtils.TokenPayload tp = new TokenProviderUtils.TokenPayload(jwtCfg.getJwtConfig());
            log.debug("jwtTokenObject header={} payload={}", th, tp);
            ObjectMapper mapper = new ObjectMapper();

            String headerBase64String = jsonObjectToUrlSafeBase64String(mapper.writeValueAsString(th));
            String payloadBase64String = jsonObjectToUrlSafeBase64String(mapper.writeValueAsString(tp));
            String jwtContent = headerBase64String + "." + payloadBase64String;
            log.info("jwtContent={}", jwtContent);

            SdkBytes awsBytesJwtContent = SdkBytes.fromByteArray(jwtContent.getBytes(StandardCharsets.UTF_8));
            SignRequest signRequest = SignRequest.builder()
                    .message(awsBytesJwtContent)
                    .messageType(MessageType.RAW)
                    .signingAlgorithm(SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256)
                    .keyId(jwtCfg.getKeyId()) //KeyId dal secret
                    .build();

            SignResponse signResult = kmsClient.sign(signRequest);

            byte[] signature = signResult.signature().asByteArray();
            String signatureString = bytesToUrlSafeBase64String(signature);
            String result = jwtContent + "." + signatureString;
            log.info("Sign result OK - jwt={}", result);
            return result;


        } catch (Exception exc) {
            //TODO: GESTIONE ECCEZIONI
            log.error("Error creating client_assertion: -> ", exc);
            throw new Exception(exc);
        }
    }



}

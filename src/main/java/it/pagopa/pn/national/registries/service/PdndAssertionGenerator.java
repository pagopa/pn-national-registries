package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.TokenHeader;
import it.pagopa.pn.national.registries.model.TokenPayload;
import it.pagopa.pn.national.registries.utils.ClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CLIENTASSERTION;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CLIENTASSERTION;

@Slf4j
@Component
public class PdndAssertionGenerator {

    private final KmsClient kmsClient;

    public PdndAssertionGenerator(KmsClient kmsClient) {
        this.kmsClient = kmsClient;
    }

    public String generateClientAssertion(PdndSecretValue jwtCfg) {
        log.info("START - PdndAssertionsGenerator.generateClientAssertion");
        long startTime = System.currentTimeMillis();
        try {
            TokenHeader th = new TokenHeader(jwtCfg.getJwtConfig());
            TokenPayload tp = new TokenPayload(jwtCfg.getJwtConfig(), jwtCfg.getAuditDigest());

            String jwtContent = ClientUtils.createJwtContent(th, tp);

            SignRequest signRequest = ClientUtils.createSignRequest(jwtContent, jwtCfg.getKeyId());

            log.info("START - KmsClient.sign Request: {}", signRequest);
            long startTimeKms = System.currentTimeMillis();
            SignResponse signResult = kmsClient.sign(signRequest);
            log.info("END - KmsClient.sign Timelapse: {} ms", System.currentTimeMillis() - startTimeKms);

            String signatureString = ClientUtils.createSignature(signResult);
            log.info("END - PdndAssertionGenerator.generateClientAssertion Timelapse: {} ms", System.currentTimeMillis() - startTime);
            log.info("PdndAssertionGenerator: {}", jwtContent + "." + signatureString);
            return jwtContent + "." + signatureString;

        } catch (Exception e) {
            throw new PnInternalException(ERROR_MESSAGE_CLIENTASSERTION, ERROR_CODE_CLIENTASSERTION,e);
        }
    }

}

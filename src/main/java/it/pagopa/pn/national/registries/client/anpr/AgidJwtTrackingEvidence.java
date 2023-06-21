package it.pagopa.pn.national.registries.client.anpr;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.RegisteredClaims;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.utils.ClientUtils;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.TokenHeader;
import it.pagopa.pn.national.registries.model.TokenPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_ANPR;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_ANPR;

@Slf4j
@Component
public class AgidJwtTrackingEvidence {

    private static final String PN_NATIONAL_REGISTRIES = "pn-national-registries-";
    private final AnprSecretConfig anprSecretConfig;
    private final KmsClient kmsClient;
    private final String environmentType;

    public AgidJwtTrackingEvidence(AnprSecretConfig anprSecretConfig,
                                   KmsClient kmsClient,
                                   @Value("${pn.national.registries.environment.type}") String environmentType) {
        this.anprSecretConfig = anprSecretConfig;
        this.kmsClient = kmsClient;
        this.environmentType = environmentType;
    }

    public String createAgidJwt() {
        log.info("START - AgidJwtTrackingEvidence.createAgidJwt");
        long startTime = System.currentTimeMillis();
        try {
            PdndSecretValue pdndSecretValue = anprSecretConfig.getAnprPdndSecretValue();

            TokenHeader th = new TokenHeader(pdndSecretValue.getJwtConfig());
            TokenPayload tp = new TokenPayload(pdndSecretValue.getJwtConfig(), null);

            String jwtContent = ClientUtils.createJwtContent(createHeaderMap(th), createClaimMap(tp, pdndSecretValue.getEserviceAudience()));

            SignRequest signRequest = ClientUtils.createSignRequest(jwtContent, pdndSecretValue.getKeyId());
            log.info("START - KmsClient.sign Request: {}", signRequest);
            long startTimeKms = System.currentTimeMillis();
            SignResponse signResult = kmsClient.sign(signRequest);
            log.info("END - KmsClient.sign Timelapse: {} ms", System.currentTimeMillis() - startTimeKms);

            String signatureString = ClientUtils.createSignature(signResult);
            log.info("END - AgidJwtTrackingEvidence.createAgidJwt Timelapse: {} ms", System.currentTimeMillis() - startTime);
            log.info("AgidTrackingEvidence: {}", jwtContent + "." + signatureString);
            return jwtContent + "." + signatureString;

        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_ANPR, ERROR_CODE_ANPR, e);
        }
    }

    private Map<String, Object> createHeaderMap(TokenHeader th) {
        Map<String, Object> map = new HashMap<>();
        map.put(HeaderParams.ALGORITHM, th.getAlg());
        map.put(HeaderParams.TYPE, th.getTyp());
        map.put(HeaderParams.KEY_ID, th.getKid());
        return map;
    }

    private Map<String, Object> createClaimMap(TokenPayload tp, String eserviceAudience) {
        Map<String, Object> map = new HashMap<>();
        map.put(RegisteredClaims.AUDIENCE, eserviceAudience);
        map.put(RegisteredClaims.ISSUER, tp.getIss());
        map.put("purposeId",tp.getPurposeId());
        map.put("dnonce", generateRandomDnonce());
        map.put("userID", PN_NATIONAL_REGISTRIES + environmentType);
        map.put("userLocation",environmentType);
        map.put("LoA","LoA3");
        map.put(RegisteredClaims.ISSUED_AT, tp.getIat());
        map.put(RegisteredClaims.EXPIRES_AT, tp.getExp());
        map.put(RegisteredClaims.JWT_ID, tp.getJti());

        return map;
    }

    private String generateRandomDnonce() {
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(13);

        for (int i = 0; i < 13; i++) {
            int digit = random.nextInt(10);
            sb.append(digit);
        }

        return sb.toString();
    }

}

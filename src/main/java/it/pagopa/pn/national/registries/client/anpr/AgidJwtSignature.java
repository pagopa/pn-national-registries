package it.pagopa.pn.national.registries.client.anpr;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.RegisteredClaims;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.TokenHeader;
import it.pagopa.pn.national.registries.model.TokenPayload;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import it.pagopa.pn.national.registries.utils.ClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_ANPR;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_ANPR;

@Slf4j
@Component
public class AgidJwtSignature {

    private final AnprSecretConfig anprSecretConfig;
    private final KmsClient kmsClient;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    public AgidJwtSignature(AnprSecretConfig anprSecretConfig,
                            KmsClient kmsClient,
                            PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        this.anprSecretConfig = anprSecretConfig;
        this.kmsClient = kmsClient;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    public String createAgidJwt(String digest) {
        log.info("START - AgidJwtSignature.createAgidJwt");
        long startTime = System.currentTimeMillis();
        try {
            PdndSecretValue pdndSecretValue = pnNationalRegistriesSecretService.getPdndSecretValue(anprSecretConfig.getPdndSecretName());

            TokenHeader th = new TokenHeader(pdndSecretValue.getJwtConfig());
            TokenPayload tp = new TokenPayload(pdndSecretValue.getJwtConfig(), null);

            String jwtContent = ClientUtils.createJwtContent(createHeaderMap(th), createClaimMap(digest, tp, pdndSecretValue.getEserviceAudience()));

            SignRequest signRequest = ClientUtils.createSignRequest(jwtContent, pdndSecretValue.getKeyId());
            log.info("START - KmsClient.sign Request: {}", signRequest);
            long startTimeKms = System.currentTimeMillis();
            SignResponse signResult = kmsClient.sign(signRequest);
            log.info("END - KmsClient.sign Timelapse: {} ms", System.currentTimeMillis() - startTimeKms);

            String signatureString = ClientUtils.createSignature(signResult);
            log.info("END - AgidJwtSignature.createAgidJwt Timelapse: {} ms", System.currentTimeMillis() - startTime);
            return jwtContent + "." + signatureString;

        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_ANPR, ERROR_CODE_ANPR, e);
        }
    }

    private Map<String, Object> createHeaderMap(TokenHeader th) {
        Map<String, Object> map = new HashMap<>();
        map.put(HeaderParams.TYPE, th.getTyp());
        map.put(HeaderParams.ALGORITHM, th.getAlg());
        map.put(HeaderParams.KEY_ID, th.getKid());
        return map;
    }

    private Map<String, Object> createClaimMap(String digest, TokenPayload tp, String eserviceAudience) {
        Map<String, Object> map = new HashMap<>();
        map.put(RegisteredClaims.ISSUER, tp.getIss());
        map.put(RegisteredClaims.SUBJECT, tp.getSub());
        map.put(RegisteredClaims.EXPIRES_AT, tp.getExp());
        map.put(RegisteredClaims.AUDIENCE, eserviceAudience);
        map.put(RegisteredClaims.ISSUED_AT, tp.getIat());
        map.put(RegisteredClaims.JWT_ID, tp.getJti());
        map.put("signed_headers", createSignedHeaders(digest));
        log.debug("ClaimMap audience: {}", map.get(RegisteredClaims.AUDIENCE));
        return map;
    }

    private List<Object> createSignedHeaders(String digest) {
        List<Object> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("digest", digest);
        list.add(map);
        Map<String, String> map1 = new HashMap<>();
        map1.put("content-encoding", "UTF-8");
        list.add(map1);
        Map<String, String> map2 = new HashMap<>();
        map2.put("content-type", "application/json");
        list.add(map2);

        return list;
    }
}

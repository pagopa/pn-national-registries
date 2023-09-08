package it.pagopa.pn.national.registries.client.anpr;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.RegisteredClaims;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import it.pagopa.pn.national.registries.utils.ClientUtils;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.TokenHeader;
import it.pagopa.pn.national.registries.model.TokenPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_ANPR;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_ANPR;

@Slf4j
@Component
public class AgidJwtTrackingEvidence {

    static final Pattern patternRoot = Pattern.compile(".*Root=(.*);P.*");
    static final Pattern patternTraceId = Pattern.compile("traceId:(.*)");
    private final AnprSecretConfig anprSecretConfig;
    private final KmsClient kmsClient;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;
    private static final int MAX_SIZE_NUMBER = 13;
    private static final int MAX_BOUND_NUMBER = 10;

    public AgidJwtTrackingEvidence(AnprSecretConfig anprSecretConfig,
                                   KmsClient kmsClient,
                                   PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        this.anprSecretConfig = anprSecretConfig;
        this.kmsClient = kmsClient;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    public String createAgidJwt() {
        log.info("START - AgidJwtTrackingEvidence.createAgidJwt");
        long startTime = System.currentTimeMillis();
        try {
            PdndSecretValue pdndSecretValue = pnNationalRegistriesSecretService.getPdndSecretValue(anprSecretConfig.getPurposeId(), anprSecretConfig.getPdndSecretName());

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
        String traceId = "unknown";
        if (MDCUtils.retrieveMDCContextMap() != null) {
            traceId = retrieveTraceId(traceId);
        }
        Map<String, Object> map = new HashMap<>();
        map.put(RegisteredClaims.AUDIENCE, eserviceAudience);
        map.put(RegisteredClaims.ISSUER, tp.getIss());
        map.put("purposeId",tp.getPurposeId());
        map.put("dnonce", generateRandomDnonce());
        map.put("userID", traceId);
        map.put("userLocation",anprSecretConfig.getEnvironmentType());
        map.put("LoA","LoA3");
        map.put(RegisteredClaims.ISSUED_AT, tp.getIat());
        map.put(RegisteredClaims.EXPIRES_AT, tp.getExp());
        map.put(RegisteredClaims.JWT_ID, tp.getJti());

        return map;
    }

    private String retrieveTraceId(String traceId) {
        String tmpTraceId = MDCUtils.retrieveMDCContextMap().get(MDC_TRACE_ID_KEY);
        if (StringUtils.hasText(tmpTraceId)) {
            final Matcher matcherRoot = patternRoot.matcher(tmpTraceId);
            final Matcher matcherTraceId = patternTraceId.matcher(tmpTraceId);
            if (matcherRoot.find()) {
                traceId = matcherRoot.group(1);
            } else if (matcherTraceId.find()) {
                traceId = matcherTraceId.group(1);
            }
        }
        return traceId;
    }

    private String generateRandomDnonce() {
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(MAX_SIZE_NUMBER);

        for (int i = 0; i < MAX_SIZE_NUMBER; i++) {
            int digit = random.nextInt(MAX_BOUND_NUMBER);
            sb.append(digit);
        }

        return sb.toString();
    }

}

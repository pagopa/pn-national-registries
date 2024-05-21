package it.pagopa.pn.national.registries.client.agenziaentrate;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.SecureWebClient;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CHECK_CF;

@Component
@Slf4j
public class CheckCfWebClient extends SecureWebClient {

    private final String basePath;
    private final SsmParameterConsumerActivation ssmParameterConsumerActivation;
    private final String authChannelData;
    private final CheckCfSecretConfig checkCfSecretConfig;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    public CheckCfWebClient(@Value("${pn.national.registries.ade-check-cf.base-path}") String basePath,
                            @Value("${pn.national.registries.ade.auth}") String authChannelData,
                            SsmParameterConsumerActivation ssmParameterConsumerActivation,
                            CheckCfSecretConfig checkCfSecretConfig,
                            PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        this.basePath = basePath;
        this.ssmParameterConsumerActivation = ssmParameterConsumerActivation;
        this.authChannelData = authChannelData;
        this.checkCfSecretConfig = checkCfSecretConfig;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    protected WebClient init() {
        return super.initWebClient(basePath);
    }

    protected SslContext buildSslContext() {
        try {
            TrustData trustData = pnNationalRegistriesSecretService.getTrustedCertFromSecret(checkCfSecretConfig.getTrustData());
            Optional<SSLData> optSslData = ssmParameterConsumerActivation.getParameterValue(authChannelData, SSLData.class);
            if (optSslData.isEmpty()) {
                throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF);
            }
            SSLData sslData = optSslData.get();
            String privateKey = pnNationalRegistriesSecretService.getSecret(sslData.getSecretid());
            SslContextBuilder sslContext = SslContextBuilder.forClient()
                    .keyManager(getCertInputStream(sslData.getCert()), getKeyInputStream(privateKey));
            return getSslContext(sslContext, trustData.getTrust());

        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, e);
        }
    }
}

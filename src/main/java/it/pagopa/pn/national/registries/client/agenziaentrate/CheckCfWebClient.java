package it.pagopa.pn.national.registries.client.agenziaentrate;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.client.SecureWebClientUtils;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.util.Optional;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CHECK_CF;

@Component
@Slf4j
public class CheckCfWebClient extends CommonBaseClient {

    private final String basePath;
    private final String authChannelData;
    private final SecureWebClientUtils secureWebClientUtils;
    private final SsmParameterConsumerActivation ssmParameterConsumerActivation;
    private final CheckCfSecretConfig checkCfSecretConfig;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    public CheckCfWebClient(@Value("${pn.national.registries.ade-check-cf.base-path}") String basePath,
                            @Value("${pn.national.registries.ade.auth}") String authChannelData,
                            SecureWebClientUtils secureWebClientUtils,
                            SsmParameterConsumerActivation ssmParameterConsumerActivation,
                            CheckCfSecretConfig checkCfSecretConfig,
                            PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        this.basePath = basePath;
        this.authChannelData = authChannelData;
        this.secureWebClientUtils = secureWebClientUtils;
        this.ssmParameterConsumerActivation = ssmParameterConsumerActivation;
        this.checkCfSecretConfig = checkCfSecretConfig;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    public WebClient init() {
        ExchangeStrategies strategies = ExchangeStrategies.builder().build();
        WebClient.Builder webClientBuilder = WebClient.builder().baseUrl(basePath).exchangeStrategies(strategies);
        return super.initWebClient(webClientBuilder);
    }

    @Override
    protected HttpClient buildHttpClient() {
        return super.buildHttpClient().secure(t -> t.sslContext(buildSslContext()));
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
                    .keyManager(secureWebClientUtils.getCertInputStream(sslData.getCert()), secureWebClientUtils.getKeyInputStream(privateKey));
            return secureWebClientUtils.getSslContext(sslContext, trustData.getTrust());

        } catch (IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_CHECK_CF, ERROR_CODE_CHECK_CF, e);
        }
    }
}

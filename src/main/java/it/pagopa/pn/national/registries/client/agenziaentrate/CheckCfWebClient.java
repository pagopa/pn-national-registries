package it.pagopa.pn.national.registries.client.agenziaentrate;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfWebClientConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_CHECK_CF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_CHECK_CF;

@Component
@Slf4j
public class CheckCfWebClient extends CommonWebClient {

    private final String basePath;
    private final CheckCfWebClientConfig webClientConfig;
    private final SsmParameterConsumerActivation ssmParameterConsumerActivation;
    private final String authChannelData;
    private final CheckCfSecretConfig checkCfSecretConfig;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    public CheckCfWebClient(@Value("${pn.national.registries.webclient.ssl-cert-ver}") Boolean sslCertVer,
                            @Value("${pn.national.registries.ade-check-cf.base-path}") String basePath,
                            @Value("${pn.national.registries.ade.auth}") String authChannelData,
                            CheckCfWebClientConfig webClientConfig,
                            SsmParameterConsumerActivation ssmParameterConsumerActivation,
                            CheckCfSecretConfig checkCfSecretConfig,
                            PnNationalRegistriesSecretService pnNationalRegistriesSecretService) {
        super(sslCertVer);
        this.basePath = basePath;
        this.webClientConfig = webClientConfig;
        this.ssmParameterConsumerActivation = ssmParameterConsumerActivation;
        this.authChannelData = authChannelData;
        this.checkCfSecretConfig = checkCfSecretConfig;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
    }

    protected WebClient init() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("fixed")
                .maxConnections(webClientConfig.getTcpMaxPoolSize())
                .pendingAcquireMaxCount(webClientConfig.getTcpMaxQueuedConnections())
                .pendingAcquireTimeout(Duration.ofMillis(webClientConfig.getTcpPendingAcquiredTimeout()))
                .maxIdleTime(Duration.ofMillis(webClientConfig.getTcpPoolIdleTimeout()))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .secure(t -> t.sslContext(buildSslContext()));

        return super.initWebClient(httpClient, basePath);
    }

    protected SslContext buildSslContext() {
        try {
            TrustData trustData = pnNationalRegistriesSecretService.getTrustedCertFromSecret(checkCfSecretConfig.getTrustData());
            Optional<SSLData> optSslData = ssmParameterConsumerActivation.getParameterValue(authChannelData, SSLData.class);
            if(optSslData.isEmpty()) {
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

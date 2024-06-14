package it.pagopa.pn.national.registries.client.infocamere;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.ApiImpreseRappresentateElencoApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.ApiRecuperoElencoPecApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.ApiRecuperoSedeApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.ApiRichiestaElencoPecApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class InfoCamereClientTest {
    @Mock
    ApiImpreseRappresentateElencoApi legalRepresentativeApi;
    @Mock
    ApiRecuperoElencoPecApi pecApi;
    @Mock
    ApiRecuperoSedeApi sedeApi;
    @Mock
    ApiRichiestaElencoPecApi richiestaElencoPecApi;
    @Mock
    AccessTokenExpiringMap accessTokenExpiringMap;
    final String clientId = "tezt_clientId";

    @BeforeEach
    public void setup() {
        ApiClient apiClient = mock(ApiClient.class);
        legalRepresentativeApi = mock(ApiImpreseRappresentateElencoApi.class);
        pecApi = mock(ApiRecuperoElencoPecApi.class);
        sedeApi = mock(ApiRecuperoSedeApi.class);
        richiestaElencoPecApi = mock(ApiRichiestaElencoPecApi.class);

        doNothing().when(apiClient).setBearerToken(anyString());
        when(apiClient.addDefaultHeader(anyString(), anyString())).thenReturn(apiClient);

        when(legalRepresentativeApi.getApiClient()).thenReturn(apiClient);
        when(pecApi.getApiClient()).thenReturn(apiClient);
        when(sedeApi.getApiClient()).thenReturn(apiClient);
        when(richiestaElencoPecApi.getApiClient()).thenReturn(apiClient);
    }

    public WebClientResponseException buildException() {
        HttpHeaders headers = mock(HttpHeaders.class);
        byte[] testByteArray = new byte[0];
        String test = "test";
        return new WebClientResponseException(test, 500, test, headers, testByteArray, Charset.defaultCharset());
    }

    public WebClientResponseException buildExceptionUnauthorized() {
        HttpHeaders headers = mock(HttpHeaders.class);
        byte[] testByteArray = new byte[0];
        String test = "test";
        return new WebClientResponseException(test, 401, test, headers, testByteArray, Charset.defaultCharset());
    }

    @Test
    void testGetLegalInstitutions() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);

        CheckTaxIdRequestBodyFilterDto checkTaxIdRequestBodyFilterDto = new CheckTaxIdRequestBodyFilterDto();
        checkTaxIdRequestBodyFilterDto.setTaxId("taxId");

        LegaleRappresentanteLista200Response infoCamereLegalInstituionsResponse = new LegaleRappresentanteLista200Response();


        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));

        when(legalRepresentativeApi.legaleRappresentanteLista(any(), any()))
                .thenReturn(Mono.just(infoCamereLegalInstituionsResponse));


        StepVerifier.create(infoCamereClient.getLegalInstitutions(checkTaxIdRequestBodyFilterDto))
                .expectNext(infoCamereLegalInstituionsResponse)
                .verifyComplete();
    }


    @Test
    void testCallEServiceRequestId() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);


        IniPecBatchRequest.IniPecCf iniPecCf = new IniPecBatchRequest.IniPecCf();
        iniPecCf.setCf("taxId");
        IniPecBatchRequest request = new IniPecBatchRequest();
        request.setDataOraRichiesta(new Date());
        request.setElencoCf(List.of(iniPecCf));

        RichiestaElencoPec200Response elencoPec200Response = new RichiestaElencoPec200Response();
        elencoPec200Response.setIdentificativoRichiesta("correlationId");
        elencoPec200Response.setDataOraEstrazione(new Date());

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(richiestaElencoPecApi.richiestaElencoPec(any(), any())).thenReturn(Mono.just(elencoPec200Response));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(request)).expectNext(elencoPec200Response).verifyComplete();
    }

    @Test
    void testCallEServiceRequestIdWebException() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        IniPecBatchRequest.IniPecCf iniPecCf = new IniPecBatchRequest.IniPecCf();
        iniPecCf.setCf("taxId");
        IniPecBatchRequest request = new IniPecBatchRequest();
        request.setDataOraRichiesta(new Date());
        request.setElencoCf(List.of(iniPecCf));

        WebClientResponseException ex = buildException();
        when(richiestaElencoPecApi.richiestaElencoPec(any(), any()))
                .thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testCallEServiceRequestPec() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        String request = "correlationId";
        GetElencoPec200Response response = new GetElencoPec200Response();
        response.setIdentificativoRichiesta("correlationId");

        when(pecApi.getElencoPec(any(), any())).thenReturn(Mono.just(response));
        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request)).expectNext(response).verifyComplete();
    }

    @Test
    void testCallEServiceRequestPecWebException() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        String request = "correlationId";

        WebClientResponseException ex = buildException();
        when(pecApi.getElencoPec(any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testGetLegalAddress() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);

        String request = "taxId";
        RecuperoSedeImpresa200Response response = new RecuperoSedeImpresa200Response();
        response.setIndirizzoLocalizzazione(new IndirizzoLocalizzazioneDTO());
        response.setCf("taxId");

        String jws = "jws";
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials(jws);

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(sedeApi.recuperoSedeImpresa(anyString(), anyString())).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereClient.getLegalAddress(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testGetLegalAddressWebException() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);

        String request = "taxId";

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        WebClientResponseException ex = buildException();
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(sedeApi.recuperoSedeImpresa(anyString(), anyString())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.getLegalAddress(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }
    @Test
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);

        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);
        assertTrue(infoCamereClient.shouldRetry(webClientResponseException));
    }

    @Test
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);

        assertFalse(infoCamereClient.shouldRetry(new Exception()));
    }

    @Test
    void testCallEServiceRequestIdExceptionOnRetry() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);
        WebClientResponseException ex = buildExceptionUnauthorized();

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(richiestaElencoPecApi.richiestaElencoPec(any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(any()))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testCallEServiceRequestPecExceptionOnRetry() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);
        WebClientResponseException ex = buildExceptionUnauthorized();

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(pecApi.getElencoPec(any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(any()))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testGetLegalAddressExceptionOnRetry() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);
        WebClientResponseException ex = buildExceptionUnauthorized();

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(sedeApi.recuperoSedeImpresa(any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.getLegalAddress(any()))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testGetLegalInstitutionsExceptionOnRetry() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, legalRepresentativeApi, pecApi, sedeApi, richiestaElencoPecApi);
        WebClientResponseException ex = buildExceptionUnauthorized();
        CheckTaxIdRequestBodyFilterDto checkTaxIdRequestBodyFilterDto = new CheckTaxIdRequestBodyFilterDto();
        checkTaxIdRequestBodyFilterDto.setTaxId("taxId");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(legalRepresentativeApi.legaleRappresentanteLista(any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.getLegalInstitutions(checkTaxIdRequestBodyFilterDto))
                .expectError(PnInternalException.class)
                .verify();
    }
}
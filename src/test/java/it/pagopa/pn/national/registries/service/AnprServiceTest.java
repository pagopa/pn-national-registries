package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.dto.RichiestaE002Dto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPRRequestBodyDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@ContextConfiguration(classes = {AnprService.class})
@ExtendWith(SpringExtension.class)
class AnprServiceTest {
    @Autowired
    private AnprService anprService;

    /**
     * Method under test: {@link AnprService#getAddressANPR(GetAddressANPRRequestBodyDto)}
     */
    @Test
    void testGetAddressANPR() {
        // Arrange
        // TODO: Populate arranged inputs
        GetAddressANPRRequestBodyDto request = null;

        // Act
        Mono<GetAddressANPROKDto> actualAddressANPR = this.anprService.getAddressANPR(request);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link AnprService#createRequest(GetAddressANPRRequestBodyDto)}
     */
    @Test
    void testCreateRequest() {
        // Arrange
        // TODO: Populate arranged inputs
        GetAddressANPRRequestBodyDto request = null;

        // Act
        RichiestaE002Dto actualCreateRequestResult = this.anprService.createRequest(request);

        // Assert
        // TODO: Add assertions on result
    }
}


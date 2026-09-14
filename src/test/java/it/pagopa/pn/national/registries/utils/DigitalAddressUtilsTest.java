package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressSQSMessageDigitalAddressInnerDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.DigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DigitalAddressUtilsTest {

    @Test
    void removeInvalidEmails_shouldKeepOnlyValidEmails() {
        AddressSQSMessageDigitalAddressInnerDto validAddress =
                new AddressSQSMessageDigitalAddressInnerDto();
        validAddress.setAddress("valid@email.it");

        AddressSQSMessageDigitalAddressInnerDto invalidAddress =
                new AddressSQSMessageDigitalAddressInnerDto();
        invalidAddress.setAddress("invalid-email");

        AddressSQSMessageDto sqsDto = new AddressSQSMessageDto();
        sqsDto.setDigitalAddress(List.of(validAddress, invalidAddress));

        DigitalAddressUtils.removeInvalidEmails(sqsDto);

        assertNotNull(sqsDto.getDigitalAddress());
        assertEquals(1, sqsDto.getDigitalAddress().size());
        assertEquals(
                "valid@email.it",
                sqsDto.getDigitalAddress().getFirst().getAddress()
        );
    }

    @Test
    void removeInvalidEmails_shouldRemoveAllInvalidEmails() {
        AddressSQSMessageDigitalAddressInnerDto invalidAddress1 =
                new AddressSQSMessageDigitalAddressInnerDto();
        invalidAddress1.setAddress("invalid");

        AddressSQSMessageDigitalAddressInnerDto invalidAddress2 =
                new AddressSQSMessageDigitalAddressInnerDto();
        invalidAddress2.setAddress("another-invalid-email");

        AddressSQSMessageDto sqsDto = new AddressSQSMessageDto();
        sqsDto.setDigitalAddress(List.of(invalidAddress1, invalidAddress2));

        DigitalAddressUtils.removeInvalidEmails(sqsDto);

        assertNotNull(sqsDto.getDigitalAddress());
        assertTrue(sqsDto.getDigitalAddress().isEmpty());
    }

    @Test
    void removeInvalidEmails_shouldSetEmptyListWhenDigitalAddressIsNull() {
        AddressSQSMessageDto sqsDto = new AddressSQSMessageDto();
        sqsDto.setDigitalAddress(null);

        DigitalAddressUtils.removeInvalidEmails(sqsDto);

        assertNotNull(sqsDto.getDigitalAddress());
        assertTrue(sqsDto.getDigitalAddress().isEmpty());
    }

    @Test
    void removeInvalidEmails_shouldKeepEmptyListWhenDigitalAddressIsEmpty() {
        AddressSQSMessageDto sqsDto = new AddressSQSMessageDto();
        sqsDto.setDigitalAddress(List.of());

        DigitalAddressUtils.removeInvalidEmails(sqsDto);

        assertNotNull(sqsDto.getDigitalAddress());
        assertTrue(sqsDto.getDigitalAddress().isEmpty());
    }

    @Test
    void emailValidation_shouldReturnResponseWhenEmailIsValid() {
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("valid@email.it");

        GetDigitalAddressINADOKDto inadResponse =
                new GetDigitalAddressINADOKDto();
        inadResponse.setDigitalAddress(digitalAddressDto);

        StepVerifier.create(
                        DigitalAddressUtils.emailValidation(inadResponse)
                )
                .assertNext(result -> assertSame(inadResponse, result))
                .verifyComplete();
    }

    @Test
    void emailValidation_shouldReturnNotFoundWhenEmailIsInvalid() {
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("invalid-email");

        GetDigitalAddressINADOKDto inadResponse =
                new GetDigitalAddressINADOKDto();
        inadResponse.setDigitalAddress(digitalAddressDto);

        StepVerifier.create(
                        DigitalAddressUtils.emailValidation(inadResponse)
                )
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(
                            PnNationalRegistriesException.class,
                            error
                    );

                    PnNationalRegistriesException exception =
                            (PnNationalRegistriesException) error;

                    assertEquals(
                            HttpStatus.NOT_FOUND,
                            exception.getStatusCode()
                    );

                    assertEquals(
                            "CF non trovato",
                            exception.getMessage()
                    );
                })
                .verify();
    }

    @Test
    void emailValidation_shouldReturnNotFoundWhenEmailIsNull() {
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress(null);

        GetDigitalAddressINADOKDto inadResponse =
                new GetDigitalAddressINADOKDto();
        inadResponse.setDigitalAddress(digitalAddressDto);

        StepVerifier.create(
                        DigitalAddressUtils.emailValidation(inadResponse)
                )
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(
                            PnNationalRegistriesException.class,
                            error
                    );

                    PnNationalRegistriesException exception =
                            (PnNationalRegistriesException) error;

                    assertEquals(
                            HttpStatus.NOT_FOUND,
                            exception.getStatusCode()
                    );
                })
                .verify();
    }

    @Test
    void emailValidation_shouldReturnNotFoundWhenEmailIsBlank() {
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("   ");

        GetDigitalAddressINADOKDto inadResponse =
                new GetDigitalAddressINADOKDto();
        inadResponse.setDigitalAddress(digitalAddressDto);

        StepVerifier.create(
                        DigitalAddressUtils.emailValidation(inadResponse)
                )
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void isValidEmail_shouldReturnTrueForValidEmail() {
        assertTrue(
                DigitalAddressUtils.isValidEmail("test@example.com")
        );
    }

    @Test
    void isValidEmail_shouldReturnTrueForPecEmail() {
        assertTrue(
                DigitalAddressUtils.isValidEmail("test@pec.example.it")
        );
    }

    @Test
    void isValidEmail_shouldReturnTrueForEmailWithSpecialCharacters() {
        assertTrue(
                DigitalAddressUtils.isValidEmail(
                        "test.user+notification@example.com"
                )
        );
    }

    @Test
    void isValidEmail_shouldReturnFalseForEmailWithoutAt() {
        assertFalse(
                DigitalAddressUtils.isValidEmail("invalid-email")
        );
    }

    @Test
    void isValidEmail_shouldReturnFalseForEmailWithoutDomain() {
        assertFalse(
                DigitalAddressUtils.isValidEmail("test@")
        );
    }

    @Test
    void isValidEmail_shouldReturnFalseForNullEmail() {
        assertFalse(
                DigitalAddressUtils.isValidEmail(null)
        );
    }

    @Test
    void isValidEmail_shouldReturnFalseForEmptyEmail() {
        assertFalse(
                DigitalAddressUtils.isValidEmail("")
        );
    }

    @Test
    void isValidEmail_shouldReturnFalseForBlankEmail() {
        assertFalse(
                DigitalAddressUtils.isValidEmail("   ")
        );
    }
}

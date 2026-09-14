package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressSQSMessageDigitalAddressInnerDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DigitalAddressUtils {

    private static final String CF_NOT_FOUND = "CF non trovato";
    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);
    private static final Logger log = LoggerFactory.getLogger(DigitalAddressUtils.class);

    public static void removeInvalidEmails(CodeSqsDto sqsDto) {
        List<AddressSQSMessageDigitalAddressInnerDto> digitalAddresses = new ArrayList<>();
        if (!CollectionUtils.isEmpty(sqsDto.getDigitalAddress())) {
            digitalAddresses = sqsDto.getDigitalAddress().stream()
                    .filter(digitalAddress -> isValidEmail(digitalAddress.getAddress()))
                    .toList();
        }
        sqsDto.setDigitalAddress(digitalAddresses);
    }

    public static Mono<GetDigitalAddressINADOKDto> emailValidation(GetDigitalAddressINADOKDto inadResponse) {
        if (Objects.nonNull(inadResponse.getDigitalAddress()) && !isValidEmail(inadResponse.getDigitalAddress().getDigitalAddress())) {
            return Mono.error(new PnNationalRegistriesException(CF_NOT_FOUND, HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(), null, null, Charset.defaultCharset(), InadResponseKO.class));
        }
        return Mono.just(inadResponse);
    }

    public static boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        Matcher matcher = pattern.matcher(email);
        boolean match = matcher.matches();

        if(!match) {
            log.warn("Email {} is not valid", email);
        }

        return match;
    }
}

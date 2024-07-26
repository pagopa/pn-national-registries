package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckEmailUtils {

    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);
    private static final Logger log = LoggerFactory.getLogger(CheckEmailUtils.class);

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

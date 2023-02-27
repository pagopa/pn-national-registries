package it.pagopa.pn.national.registries.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {PnNationalRegistriesConfig.class})
@ExtendWith(SpringExtension.class)
class PnNationalRegistriesConfigTest {

    @Autowired
    private PnNationalRegistriesConfig pnNationalRegistriesConfig;

    /**
     * Method under test: {@link PnNationalRegistriesConfig#postProcessAfterInitialization(Object, String)}
     */
    @Test
    void testPostProcessAfterInitialization() throws BeansException {
        assertEquals("Bean", pnNationalRegistriesConfig.postProcessAfterInitialization("Bean", "Bean Name"));
    }

    /**
     * Method under test: {@link PnNationalRegistriesConfig#postProcessAfterInitialization(Object, String)}
     */
    @Test
    void testPostProcessAfterInitialization2() throws BeansException {
        ObjectMapper objectMapper = new ObjectMapper();
        assertSame(objectMapper, pnNationalRegistriesConfig.postProcessAfterInitialization(objectMapper, "Bean Name"));
    }
}

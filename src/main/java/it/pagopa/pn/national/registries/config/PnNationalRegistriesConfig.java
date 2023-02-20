package it.pagopa.pn.national.registries.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import it.pagopa.pn.national.registries.utils.JacksonCustomSpELSerializer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;

import static it.pagopa.pn.national.registries.utils.JacksonCustomSpELSerializer.FILTER_NAME;

@Configuration
@Import(SharedAutoConfiguration.class)
public class PnNationalRegistriesConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        if (bean instanceof ObjectMapper objectMapper) {
            objectMapper.setFilterProvider(new SimpleFilterProvider()
                    .addFilter(FILTER_NAME, new JacksonCustomSpELSerializer()));
        }
        return bean;
    }
}

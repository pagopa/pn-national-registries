package it.pagopa.pn.national.registries.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.impl.AbstractCachedSsmParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.Optional;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;

@Configuration
@Slf4j
public class SsmParameterConsumerActivation extends AbstractCachedSsmParameterConsumer {

    public SsmParameterConsumerActivation(SsmClient ssmClient) {
        super(ssmClient);
    }

    public <T> Optional<T> getAuthParameter(String parameterName, Class<T> clazz ) {
        Optional<T> opt = super.getParameterValue(parameterName, clazz);
        if(opt.isEmpty()){
            String json = super.getParameter(parameterName);
            if (StringUtils.hasText( json )) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    opt = Optional.of( objectMapper.readValue( json, clazz ) );
                } catch (JsonProcessingException e) {
                    throw new PnInternalException( "Unable to deserialize object", ERROR_CODE_PN_GENERIC_ERROR, e );
                }
            }
        }
        return opt;

    }
}

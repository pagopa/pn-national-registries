package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Objects;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_ADE_LEGAL_OPENSAML_INIT;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_ADE_LEGAL_OPENSAML_INIT;

@Component
@Slf4j
public class OpenSAMLUtils {

    private final XMLObjectBuilderFactory builderFactory;

    public OpenSAMLUtils() {
        init();
        builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
    }

    private void init() {
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADE_LEGAL_OPENSAML_INIT, ERROR_CODE_ADE_LEGAL_OPENSAML_INIT, e);
        }
    }

    public XMLObject buildSAMLObject(QName xmlObjectQName, QName customQName) {
        QName objectQName = customQName != null ? customQName : xmlObjectQName;
        return Objects.requireNonNull(builderFactory.getBuilder(xmlObjectQName)).buildObject(objectQName);
    }
}

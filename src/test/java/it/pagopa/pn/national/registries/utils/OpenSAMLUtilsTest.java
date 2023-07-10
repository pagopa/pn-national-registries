package it.pagopa.pn.national.registries.utils;


import javax.xml.namespace.QName;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.core.Subject;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenSAMLUtilsTest {

    @Test
    void testBuildSAMLObject() {
        XMLObjectBuilderFactory builderFactory = mock(XMLObjectBuilderFactory.class);
        MockedStatic<InitializationService> initializationService = mockStatic(InitializationService.class);
        MockedStatic<XMLObjectProviderRegistrySupport> support = mockStatic(XMLObjectProviderRegistrySupport.class);
        support.when(XMLObjectProviderRegistrySupport::getBuilderFactory).thenReturn(builderFactory);
        OpenSAMLUtils openSAMLUtils = new OpenSAMLUtils();
        XMLObjectBuilder builder = mock(XMLObjectBuilder.class);
        XMLObject object = mock(XMLObject.class);
        when(object.getElementQName()).thenReturn(Subject.DEFAULT_ELEMENT_NAME);
        when(builderFactory.getBuilder((QName) any())).thenReturn(builder);
        when(builder.buildObject(any(QName.class))).thenReturn(object);
        XMLObject xmlObject = openSAMLUtils.buildSAMLObject(Subject.DEFAULT_ELEMENT_NAME, Subject.DEFAULT_ELEMENT_NAME);
        Assertions.assertEquals(Subject.DEFAULT_ELEMENT_NAME, xmlObject.getElementQName());
        support.close();
        initializationService.close();
    }

    @Test
    void testBuildSAMLObjectInitializeException() {
        MockedStatic<InitializationService> initializationService = mockStatic(InitializationService.class);
        initializationService.when(InitializationService::initialize).thenThrow(new InitializationException("Exception"));
        Assertions.assertThrows(PnInternalException.class, OpenSAMLUtils::new,
                "Errore durante l'inizializzazione di OpenSAML");
        initializationService.close();
    }
}

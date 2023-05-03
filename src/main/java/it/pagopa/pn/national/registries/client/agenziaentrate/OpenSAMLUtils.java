package it.pagopa.pn.national.registries.client.agenziaentrate;

import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class OpenSAMLUtils {

    //TODO: mettere i dati cablati a codice in una classe di costanti
    //TODO: gestire le eccezioni correttamente (lancio eccezioni specifiche per gestirle nell'handler)

    private final XMLObjectBuilderFactory builderFactory;

    public OpenSAMLUtils() {
        XMLObjectProviderRegistry registry = new XMLObjectProviderRegistry();
        ConfigurationService.register(XMLObjectProviderRegistry.class, registry);
        registry.setParserPool(getParserPool());
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            e.printStackTrace();
        }
        builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
    }

    public <T> T buildSAMLObject(final Class<T> clazz, String name) {
        T object;
        QName defaultElementName;
        try {
            defaultElementName = (QName) clazz.getDeclaredField(name).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException("Could not create SAML object");
        }
        object = (T) builderFactory.getBuilder(defaultElementName).buildObject(defaultElementName);
        return object;
    }

    public <T> T buildSAMLCustomObject(final Class<T> clazz, String name, QName qName) {
        QName defaultElementName;
        try {
            defaultElementName = (QName) clazz.getDeclaredField(name).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException("Could not create SAML object");
        }
        XMLObjectBuilder builder = builderFactory.getBuilder(defaultElementName);
        return (T) builder.buildObject(qName);
    }

    private static ParserPool getParserPool() {
        BasicParserPool parserPool = new BasicParserPool();
        parserPool.setMaxPoolSize(100);
        parserPool.setCoalescing(true);
        parserPool.setIgnoreComments(true);
        parserPool.setIgnoreElementContentWhitespace(true);
        parserPool.setNamespaceAware(true);
        parserPool.setExpandEntityReferences(false);
        parserPool.setXincludeAware(false);

        final Map<String, Boolean> features = new HashMap<>();
        features.put("http://xml.org/sax/features/external-general-entities", Boolean.TRUE);
        features.put("http://xml.org/sax/features/external-parameter-entities", Boolean.FALSE);
        features.put("http://apache.org/xml/features/disallow-doctype-decl", Boolean.FALSE);
        features.put("http://apache.org/xml/features/validation/schema/normalized-value", Boolean.FALSE);
        features.put("http://javax.xml.XMLConstants/feature/secure-processing", Boolean.TRUE);

        parserPool.setBuilderFeatures(features);
        parserPool.setBuilderAttributes(new HashMap<>());

        try {
            parserPool.initialize();
        } catch (ComponentInitializationException e) {
            e.printStackTrace();
        }
        return parserPool;
    }
}

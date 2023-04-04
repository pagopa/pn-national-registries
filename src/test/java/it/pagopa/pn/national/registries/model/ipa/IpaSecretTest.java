package it.pagopa.pn.national.registries.model.ipa;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class IpaSecretTest {
    /**
     * Methods under test:
     *
     * <ul>
     *   <li>default or parameterless constructor of {@link IpaSecret}
     *   <li>{@link IpaSecret#getAuthId()}
     * </ul>
     */
    @Test
    void testConstructor() {
        assertNull((new IpaSecret()).getAuthId());
    }
}


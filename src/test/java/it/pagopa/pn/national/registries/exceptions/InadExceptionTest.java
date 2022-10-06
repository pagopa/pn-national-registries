package it.pagopa.pn.national.registries.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;

import java.util.List;

import org.junit.jupiter.api.Test;

class InadExceptionTest {
    /**
     * Method under test: {@link InadException#InadException(Throwable)}
     */
    @Test
    void testConstructor() {
        InadException actualInadException = new InadException(new Throwable());
        assertEquals(500, actualInadException.getStatus());
        Problem problem = actualInadException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals(500, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Errore durante la chiamata al servizio EstrazioniPuntualiApi", problem.getDetail());
        ProblemError getResult = errors.get(0);
        assertNull(getResult.getElement());
        assertNull(getResult.getDetail());
        assertEquals(PnNationalregistriesExceptionCodes.ERROR_CODE_INAD, getResult.getCode());
    }

    /**
     * Method under test: {@link InadException#InadException(Throwable)}
     */
    @Test
    void testConstructor2() {
        InadException actualInadException = new InadException(new Throwable("Not all who wander are lost"));
        assertEquals(500, actualInadException.getStatus());
        Problem problem = actualInadException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals(500, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Errore durante la chiamata al servizio EstrazioniPuntualiApi", problem.getDetail());
        ProblemError getResult = errors.get(0);
        assertNull(getResult.getElement());
        assertNull(getResult.getDetail());
        assertEquals(PnNationalregistriesExceptionCodes.ERROR_CODE_INAD, getResult.getCode());
    }
}


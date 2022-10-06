package it.pagopa.pn.national.registries.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;

import java.util.List;

import org.junit.jupiter.api.Test;

class AnprExceptionTest {
    /**
     * Method under test: {@link AnprException#AnprException(Throwable)}
     */
    @Test
    void testConstructor() {
        AnprException actualAnprException = new AnprException(new Throwable());
        assertEquals(500, actualAnprException.getStatus());
        Problem problem = actualAnprException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals(500, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Errore durante la chiamata al servizio E002 dell'E-Service C001 di ANPR", problem.getDetail());
        ProblemError getResult = errors.get(0);
        assertNull(getResult.getElement());
        assertNull(getResult.getDetail());
        assertEquals(PnNationalregistriesExceptionCodes.ERROR_CODE_ADDRESS_ANPR, getResult.getCode());
    }

    /**
     * Method under test: {@link AnprException#AnprException(Throwable)}
     */
    @Test
    void testConstructor2() {
        AnprException actualAnprException = new AnprException(new Throwable("Not all who wander are lost"));
        assertEquals(500, actualAnprException.getStatus());
        Problem problem = actualAnprException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals(500, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Errore durante la chiamata al servizio E002 dell'E-Service C001 di ANPR", problem.getDetail());
        ProblemError getResult = errors.get(0);
        assertNull(getResult.getElement());
        assertNull(getResult.getDetail());
        assertEquals(PnNationalregistriesExceptionCodes.ERROR_CODE_ADDRESS_ANPR, getResult.getCode());
    }
}


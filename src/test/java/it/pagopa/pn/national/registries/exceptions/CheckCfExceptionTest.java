package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CheckCfExceptionTest {
    /**
     * Method under test: {@link CheckCfException#CheckCfException(Throwable)}
     */
    @Test
    void testConstructor() {
        CheckCfException actualCheckCfException = new CheckCfException(new Throwable());
        assertEquals(500, actualCheckCfException.getStatus());
        Problem problem = actualCheckCfException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals(500, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Errore durante la chiamata al servizio VerificaCodiceFiscale", problem.getDetail());
        ProblemError getResult = errors.get(0);
        assertNull(getResult.getElement());
        assertNull(getResult.getDetail());
        assertEquals(PnNationalregistriesExceptionCodes.ERROR_CODE_CHECK_CF, getResult.getCode());
    }

    /**
     * Method under test: {@link CheckCfException#CheckCfException(Throwable)}
     */
    @Test
    void testConstructor2() {
        CheckCfException actualCheckCfException = new CheckCfException(new Throwable("Not all who wander are lost"));
        assertEquals(500, actualCheckCfException.getStatus());
        Problem problem = actualCheckCfException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals(500, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Errore durante la chiamata al servizio VerificaCodiceFiscale", problem.getDetail());
        ProblemError getResult = errors.get(0);
        assertNull(getResult.getElement());
        assertNull(getResult.getDetail());
        assertEquals(PnNationalregistriesExceptionCodes.ERROR_CODE_CHECK_CF, getResult.getCode());
    }
}


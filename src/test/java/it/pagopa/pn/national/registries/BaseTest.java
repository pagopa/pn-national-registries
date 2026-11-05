package it.pagopa.pn.national.registries;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class BaseTest {


    @Slf4j
    @SpringBootTest
    @ActiveProfiles("test")
    @Import(it.pagopa.pn.national.registries.LocalStackTestConfig.class)
    public static class WithLocalStack {

    }


}
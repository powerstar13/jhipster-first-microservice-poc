package backend.jshong.poc.jhipster.firstmicroservice;

import backend.jshong.poc.jhipster.firstmicroservice.config.AsyncSyncConfiguration;
import backend.jshong.poc.jhipster.firstmicroservice.config.JacksonConfiguration;
import backend.jshong.poc.jhipster.firstmicroservice.config.TestSecurityConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = { FirstMicroserviceApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class }
)
public @interface IntegrationTest {
}

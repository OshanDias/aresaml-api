package aresaml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the AresAML web API.
 *
 * This class sits in the "aresaml" package so Spring's component scan picks up
 * both the engine classes here and the web classes in "aresaml.web".
 *
 * Run it: right-click this file in NetBeans -> Run File, or `mvn spring-boot:run`.
 * The API then serves on http://localhost:8080
 */
@SpringBootApplication
public class AresApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AresApiApplication.class, args);
    }
}

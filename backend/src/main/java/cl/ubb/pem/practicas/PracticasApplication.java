package cl.ubb.pem.practicas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PracticasApplication {

    public static void main(String[] args) {
        SpringApplication.run(PracticasApplication.class, args);
    }
}

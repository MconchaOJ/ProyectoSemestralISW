package cl.ubb.pem.practicas.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** El reloj es un bean para poder fijar "hoy" en las pruebas unitarias. */
@Configuration
public class RelojConfig {

    @Bean
    Clock reloj() {
        return Clock.system(ZoneId.of("America/Santiago"));
    }
}

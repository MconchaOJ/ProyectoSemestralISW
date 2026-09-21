package cl.ubb.pem.practicas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación Swagger con el botón "Authorize" para pegar el token JWT. */
@Configuration
public class OpenApiConfig {

    static final String ESQUEMA = "bearerAuth";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Formación Práctica PEM-UBB - API")
                        .version("Sprint 1")
                        .description("""
                                Backend del Sprint 1 (HU-01 a HU-05).

                                **Cómo probar:** ejecuta `POST /api/auth/login` con un usuario de demostración \
                                (por ejemplo coordinador@demo.example, guia1@demo.example, curso1@demo.example o \
                                camila.rojas@demo.example; la clave de demostración está en el encabezado de db/seed.sql), \
                                copia el campo `token` y pégalo en el botón **Authorize** (sin escribir "Bearer").
                                """))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}

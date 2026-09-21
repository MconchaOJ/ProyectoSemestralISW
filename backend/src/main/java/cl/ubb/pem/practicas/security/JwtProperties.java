package cl.ubb.pem.practicas.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del token (prefijo app.jwt).
 *
 * @param secret          secreto de firma, mínimo 32 caracteres; si viene vacío se genera uno aleatorio al arrancar
 * @param expiracionHoras duración del token en horas (S-06: 8)
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, @DefaultValue("8") long expiracionHoras) {
}

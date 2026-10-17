package cl.ubb.pem.practicas.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del token (prefijo app.jwt).
 *
 * @param secret             secreto de firma, mínimo 32 caracteres; si viene vacío se genera uno aleatorio al arrancar
 * @param expiracionHoras    duración del token en horas (S-06: 8)
 * @param secretoObligatorio si es true y falta el secreto, la aplicación no arranca (perfil prod, Sprint 2)
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expiracionHoras, boolean secretoObligatorio) {

    @ConstructorBinding
    public JwtProperties(String secret, @DefaultValue("8") long expiracionHoras,
                         @DefaultValue("false") boolean secretoObligatorio) {
        this.secret = secret;
        this.expiracionHoras = expiracionHoras;
        this.secretoObligatorio = secretoObligatorio;
    }

    /** Configuración de desarrollo: el secreto es opcional. */
    public JwtProperties(String secret, long expiracionHoras) {
        this(secret, expiracionHoras, false);
    }
}

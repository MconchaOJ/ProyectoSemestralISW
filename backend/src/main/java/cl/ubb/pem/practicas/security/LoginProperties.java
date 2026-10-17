package cl.ubb.pem.practicas.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Límite de intentos de inicio de sesión (prefijo app.login, Sprint 2).
 *
 * @param intentosMaximos intentos fallidos consecutivos permitidos para un mismo correo antes de bloquearlo
 * @param bloqueoMinutos  minutos que dura el bloqueo; al terminar, el correo vuelve a tener todos sus intentos
 */
@ConfigurationProperties(prefix = "app.login")
public record LoginProperties(@DefaultValue("5") int intentosMaximos, @DefaultValue("15") long bloqueoMinutos) {

    public LoginProperties {
        if (intentosMaximos < 1 || bloqueoMinutos < 1) {
            throw new IllegalStateException("app.login.intentos-maximos y app.login.bloqueo-minutos deben ser 1 o más.");
        }
    }
}

package cl.ubb.pem.practicas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Recuperación de la clave por correo (prefijo app.recuperacion; SUPUESTOS S-97 y S-98).
 *
 * @param minutosValidez      minutos que vale el enlace (15 a 60); después ya no sirve
 * @param solicitudesMaximas  enlaces que puede pedir un mismo usuario dentro de la ventana
 * @param ventanaMinutos      largo de esa ventana, en minutos
 * @param urlPublica          dirección pública del sistema (ISW_URL_PUBLICA) con la que se arma el enlace del correo
 */
@ConfigurationProperties(prefix = "app.recuperacion")
public record RecuperacionProperties(@DefaultValue("30") int minutosValidez,
                                     @DefaultValue("3") int solicitudesMaximas,
                                     @DefaultValue("15") int ventanaMinutos,
                                     @DefaultValue("http://localhost:8080") String urlPublica) {

    public RecuperacionProperties {
        if (minutosValidez < 15 || minutosValidez > 60) {
            throw new IllegalStateException("app.recuperacion.minutos-validez debe estar entre 15 y 60.");
        }
        if (solicitudesMaximas < 1 || ventanaMinutos < 1) {
            throw new IllegalStateException(
                    "app.recuperacion.solicitudes-maximas y app.recuperacion.ventana-minutos deben ser 1 o más.");
        }
        if (urlPublica == null || !urlPublica.matches("https?://.+")) {
            throw new IllegalStateException("app.recuperacion.url-publica (ISW_URL_PUBLICA) debe empezar con http:// o https://.");
        }
        urlPublica = urlPublica.replaceAll("/+$", "");
    }
}

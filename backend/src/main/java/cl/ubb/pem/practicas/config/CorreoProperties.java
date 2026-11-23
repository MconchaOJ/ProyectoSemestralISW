package cl.ubb.pem.practicas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Correo saliente por SMTP (prefijo app.correo). Todo viene de variables de entorno (ISW_SMTP_HOST, ISW_SMTP_PUERTO,
 * ISW_SMTP_USUARIO, ISW_SMTP_CLAVE, ISW_SMTP_REMITENTE), sin valores reales por defecto. Es el mismo SMTP que
 * necesitará HU-19, pendiente de que la UBB lo entregue (SUPUESTO S-99).
 *
 * @param host      servidor SMTP; vacío = correo no configurado (la aplicación arranca igual)
 * @param puerto    587 (STARTTLS) o 465 (SSL directo)
 * @param usuario   usuario del SMTP; vacío = sin autenticación
 * @param clave     clave del SMTP (en Gmail, una "contraseña de aplicación")
 * @param remitente dirección "De:"; vacía = se usa el usuario
 */
@ConfigurationProperties(prefix = "app.correo")
public record CorreoProperties(@DefaultValue("") String host,
                               @DefaultValue("587") int puerto,
                               @DefaultValue("") String usuario,
                               @DefaultValue("") String clave,
                               @DefaultValue("") String remitente) {

    /** Hay servidor y una dirección desde la que mandar. */
    public boolean configurado() {
        return !host.isBlank() && !remitenteEfectivo().isBlank();
    }

    public String remitenteEfectivo() {
        return remitente.isBlank() ? usuario : remitente;
    }

    /** Sin la clave: así el registro nunca la muestra. */
    @Override
    public String toString() {
        return "CorreoProperties[host=" + host + ", puerto=" + puerto + ", usuario=" + usuario
                + ", remitente=" + remitenteEfectivo() + "]";
    }
}

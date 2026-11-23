package cl.ubb.pem.practicas.config;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Crea el JavaMailSender con app.correo (no con spring.mail.*, para que todo venga de las variables ISW_SMTP_*).
 * Sin ISW_SMTP_HOST el bean existe igual pero no se usa: EnvioCorreoService escribe el enlace en el log.
 * Crear el bean no abre ninguna conexión, así que la aplicación arranca aunque el SMTP no exista.
 */
@Configuration
public class CorreoConfig {

    private static final Logger log = LoggerFactory.getLogger(CorreoConfig.class);
    /** Milisegundos máximos para conectar, leer y escribir: un SMTP caído no deja hilos colgados. */
    private static final String TIEMPO_MAXIMO_MS = "10000";

    @Bean
    JavaMailSender javaMailSender(CorreoProperties props) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        Properties p = sender.getJavaMailProperties();
        p.put("mail.smtp.connectiontimeout", TIEMPO_MAXIMO_MS);
        p.put("mail.smtp.timeout", TIEMPO_MAXIMO_MS);
        p.put("mail.smtp.writetimeout", TIEMPO_MAXIMO_MS);
        if (!props.configurado()) {
            log.warn("Correo saliente sin configurar (falta ISW_SMTP_HOST o el remitente): los enlaces para "
                    + "restablecer la clave se escribirán en este registro en vez de mandarse por correo.");
            return sender;
        }
        sender.setHost(props.host());
        sender.setPort(props.puerto());
        sender.setDefaultEncoding("UTF-8");
        if (!props.usuario().isBlank()) {
            sender.setUsername(props.usuario());
            sender.setPassword(props.clave());
            p.put("mail.smtp.auth", "true");
        }
        if (props.puerto() == 465) {
            p.put("mail.smtp.ssl.enable", "true");
        } else {
            p.put("mail.smtp.starttls.enable", "true");
        }
        log.info("Correo saliente configurado: {}", props);
        return sender;
    }
}

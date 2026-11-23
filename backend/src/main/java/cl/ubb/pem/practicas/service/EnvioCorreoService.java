package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.config.CorreoProperties;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de correos (por ahora, solo el enlace para restablecer la clave; HU-19 usará el mismo SMTP).
 *
 * El correo se manda en un hilo aparte: la respuesta al usuario no espera al SMTP (así el tiempo de respuesta no
 * deja adivinar si el correo existe) y un SMTP caído no se convierte en un error para el usuario, solo en un registro.
 * Sin SMTP configurado (SUPUESTO S-99) no se manda nada: el enlace queda en el registro del servidor con nivel WARN,
 * para poder probar el flujo completo en la demo sin una cuenta de correo.
 */
@Service
public class EnvioCorreoService {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoService.class);
    static final String ASUNTO_RECUPERACION = "Restablecer tu clave · Sistema de Prácticas PEM-UBB";

    private final JavaMailSender mailSender;
    private final CorreoProperties props;
    /** Un solo hilo (daemon): el límite de solicitudes acota la cola y no impide que la aplicación se detenga. */
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "envio-correo");
        t.setDaemon(true);
        return t;
    });

    public EnvioCorreoService(JavaMailSender mailSender, CorreoProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    /** Manda (o, sin SMTP, registra) el enlace de recuperación. Nunca lanza excepciones hacia quien lo llama. */
    public void enviarEnlaceRecuperacion(String destinatario, String nombre, String enlace, int minutosValidez) {
        if (!props.configurado()) {
            log.warn("SMTP sin configurar (ISW_SMTP_HOST): no se manda el correo. Enlace para restablecer la clave "
                    + "de {} (vale {} minutos): {}", destinatario, minutosValidez, enlace);
            return;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(props.remitenteEfectivo());
        mensaje.setTo(destinatario);
        mensaje.setSubject(ASUNTO_RECUPERACION);
        mensaje.setText(textoRecuperacion(nombre, enlace, minutosValidez));
        ejecutor.execute(() -> {
            try {
                mailSender.send(mensaje);
                log.info("Correo para restablecer la clave enviado a {}", destinatario);
            } catch (RuntimeException e) {   // MailException y cualquier otra falla
                // Sin el enlace en el registro: con SMTP configurado, el enlace solo debe llegar al correo
                log.error("No se pudo mandar el correo para restablecer la clave a {}: {}", destinatario, e.getMessage());
            }
        });
    }

    static String textoRecuperacion(String nombre, String enlace, int minutosValidez) {
        return "Hola " + nombre + ":\n\n"
                + "Recibimos una solicitud para restablecer tu clave del Sistema de Prácticas PEM de la Universidad "
                + "del Bío-Bío. Para elegir una clave nueva, abre este enlace:\n\n"
                + enlace + "\n\n"
                + "El enlace vale " + minutosValidez + " minutos y sirve una sola vez.\n\n"
                + "Si no lo pediste tú, ignora este correo: tu clave actual sigue igual.\n\n"
                + "Pedagogía en Educación Matemática · Universidad del Bío-Bío\n";
    }

    @PreDestroy
    void detener() {
        ejecutor.shutdown();
    }
}

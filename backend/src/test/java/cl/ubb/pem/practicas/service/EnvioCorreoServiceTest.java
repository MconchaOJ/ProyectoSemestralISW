package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cl.ubb.pem.practicas.config.CorreoProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** EnvioCorreoService sin Spring: sin SMTP el enlace va al registro; con SMTP caído no hay excepción. */
@ExtendWith(OutputCaptureExtension.class)
class EnvioCorreoServiceTest {

    private static final String ENLACE = "http://localhost:8080/restablecer-clave.html?token=abc";

    @Test
    void sinSmtpNoMandaNadaYDejaElEnlaceEnElRegistro(CapturedOutput salida) {
        JavaMailSender mail = mock(JavaMailSender.class);
        EnvioCorreoService servicio = new EnvioCorreoService(mail, new CorreoProperties("", 587, "", "", ""));

        servicio.enviarEnlaceRecuperacion("persona@demo.example", "Persona", ENLACE, 30);

        verifyNoInteractions(mail);
        assertThat(salida.getAll()).contains("WARN").contains("SMTP sin configurar")
                .contains("persona@demo.example").contains(ENLACE);
    }

    @Test
    void conSmtpCaidoNoLanzaYNoDejaElEnlaceEnElRegistro(CapturedOutput salida) {
        JavaMailSender mail = mock(JavaMailSender.class);
        doThrow(new MailSendException("sin conexión")).when(mail).send(any(SimpleMailMessage.class));
        EnvioCorreoService servicio = new EnvioCorreoService(mail,
                new CorreoProperties("smtp.prueba.invalid", 587, "usuario@demo.example", "clave-secreta", ""));

        assertThatCode(() -> servicio.enviarEnlaceRecuperacion("persona@demo.example", "Persona", ENLACE, 30))
                .doesNotThrowAnyException();
        verify(mail, timeout(3000)).send(any(SimpleMailMessage.class));
        servicio.detener();
        assertThat(salida.getAll()).doesNotContain(ENLACE).doesNotContain("clave-secreta");
    }

    @Test
    void elRemitenteVacioUsaElUsuarioYLaClaveNoApareceEnToString() {
        CorreoProperties props = new CorreoProperties("smtp.demo", 587, "cuenta@demo.example", "clave-secreta", "");
        assertThat(props.configurado()).isTrue();
        assertThat(props.remitenteEfectivo()).isEqualTo("cuenta@demo.example");
        assertThat(props.toString()).doesNotContain("clave-secreta");
        assertThat(new CorreoProperties("smtp.demo", 587, "", "", "").configurado()).isFalse();
    }

    @Test
    void elTextoDelCorreoTraeElEnlaceYLaDuracion() {
        assertThat(EnvioCorreoService.textoRecuperacion("Camila", ENLACE, 30))
                .contains("Hola Camila").contains(ENLACE).contains("30 minutos").contains("una sola vez");
    }
}

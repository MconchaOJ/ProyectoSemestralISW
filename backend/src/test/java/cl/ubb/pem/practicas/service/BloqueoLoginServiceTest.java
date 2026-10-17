package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.ubb.pem.practicas.exception.LoginBloqueadoException;
import cl.ubb.pem.practicas.security.LoginProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BloqueoLoginServiceTest {

    /** Reloj que la prueba puede adelantar para simular que pasa el tiempo del bloqueo. */
    static final class RelojMovil extends Clock {
        private Instant ahora = Instant.parse("2026-10-12T12:00:00Z");

        void adelantar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private final RelojMovil reloj = new RelojMovil();
    private final BloqueoLoginService bloqueo = new BloqueoLoginService(new LoginProperties(3, 15), reloj);

    private void fallar(String email, int veces) {
        for (int i = 0; i < veces; i++) {
            bloqueo.registrarFallo(email);
        }
    }

    @Test
    void alLlegarAlMaximoDeFallosElCorreoQuedaBloqueado() {
        fallar("a@demo.example", 2);
        assertThatCode(() -> bloqueo.verificar("a@demo.example")).doesNotThrowAnyException();

        bloqueo.registrarFallo("a@demo.example");
        assertThatThrownBy(() -> bloqueo.verificar("a@demo.example"))
                .isInstanceOf(LoginBloqueadoException.class)
                .hasMessageContaining("15 minutos");
        // El bloqueo es por correo, sin importar mayúsculas ni espacios; otro correo no se ve afectado
        assertThatThrownBy(() -> bloqueo.verificar(" A@Demo.Example ")).isInstanceOf(LoginBloqueadoException.class);
        assertThatCode(() -> bloqueo.verificar("b@demo.example")).doesNotThrowAnyException();
    }

    @Test
    void elBloqueoTerminaAlPasarElTiempoYLosIntentosVuelvenACero() {
        fallar("a@demo.example", 3);
        reloj.adelantar(Duration.ofMinutes(14));
        assertThatThrownBy(() -> bloqueo.verificar("a@demo.example"))
                .isInstanceOf(LoginBloqueadoException.class)
                .satisfies(e -> assertThat(((LoginBloqueadoException) e).getSegundosRestantes()).isBetween(1L, 61L));

        reloj.adelantar(Duration.ofMinutes(1));
        assertThatCode(() -> bloqueo.verificar("a@demo.example")).doesNotThrowAnyException();
        fallar("a@demo.example", 2);
        assertThatCode(() -> bloqueo.verificar("a@demo.example")).doesNotThrowAnyException();
    }

    @Test
    void unIngresoCorrectoReiniciaLaCuentaDeFallosConsecutivos() {
        fallar("a@demo.example", 2);
        bloqueo.registrarExito("a@demo.example");
        fallar("a@demo.example", 2);
        assertThatCode(() -> bloqueo.verificar("a@demo.example")).doesNotThrowAnyException();
    }

    @Test
    void parametrosInvalidosImpidenArrancar() {
        assertThatThrownBy(() -> new LoginProperties(0, 15)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new LoginProperties(5, 0)).isInstanceOf(IllegalStateException.class);
    }
}

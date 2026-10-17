package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.exception.LoginBloqueadoException;
import cl.ubb.pem.practicas.security.LoginProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Límite de intentos fallidos de inicio de sesión (Sprint 2, deuda técnica del Sprint 1). Cuenta los fallos
 * consecutivos por correo; al llegar a app.login.intentos-maximos bloquea ese correo durante app.login.bloqueo-minutos,
 * aunque después se escriba la clave correcta. Un inicio de sesión correcto reinicia la cuenta.
 *
 * Se cuenta por correo escrito, exista o no: así el bloqueo no revela qué correos están registrados.
 * SUPUESTO S-49: el registro vive en memoria (una sola instancia de la aplicación); se pierde al reiniciar.
 */
@Service
public class BloqueoLoginService {

    private static final Logger log = LoggerFactory.getLogger(BloqueoLoginService.class);
    /** Tope de correos registrados a la vez; al pasarlo se descartan los que no están bloqueados. */
    private static final int MAXIMO_REGISTROS = 10_000;

    private record Estado(int fallos, Instant bloqueadoHasta) {
    }

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();
    private final Clock reloj;
    private final int intentosMaximos;
    private final Duration duracionBloqueo;

    public BloqueoLoginService(LoginProperties props, Clock reloj) {
        this.reloj = reloj;
        this.intentosMaximos = props.intentosMaximos();
        this.duracionBloqueo = Duration.ofMinutes(props.bloqueoMinutos());
    }

    /** Lanza 429 si el correo está bloqueado; si el bloqueo ya venció, lo levanta. */
    public void verificar(String email) {
        String clave = normalizar(email);
        Estado e = estados.get(clave);
        if (e == null || e.bloqueadoHasta() == null) {
            return;
        }
        Instant ahora = reloj.instant();
        if (ahora.isBefore(e.bloqueadoHasta())) {
            // Redondeo hacia arriba: 900 s exactos son "15 minutos" y 61 s son "2 minutos"
            long segundos = (Duration.between(ahora, e.bloqueadoHasta()).toMillis() + 999) / 1000;
            long minutos = (segundos + 59) / 60;
            throw new LoginBloqueadoException("Demasiados intentos fallidos. Por seguridad, el ingreso con este correo "
                    + "quedó bloqueado. Intenta nuevamente en " + minutos + (minutos == 1 ? " minuto." : " minutos."),
                    segundos);
        }
        estados.remove(clave, e);
    }

    public void registrarFallo(String email) {
        String clave = normalizar(email);
        if (estados.size() >= MAXIMO_REGISTROS) {
            estados.values().removeIf(e -> e.bloqueadoHasta() == null);
        }
        Estado nuevo = estados.compute(clave, (k, e) -> {
            int fallos = (e == null || e.bloqueadoHasta() != null ? 0 : e.fallos()) + 1;
            return fallos >= intentosMaximos ? new Estado(0, reloj.instant().plus(duracionBloqueo)) : new Estado(fallos, null);
        });
        if (nuevo.bloqueadoHasta() != null) {
            log.warn("Inicio de sesión bloqueado por {} minutos tras {} intentos fallidos: {}",
                    duracionBloqueo.toMinutes(), intentosMaximos, clave);
        }
    }

    public void registrarExito(String email) {
        estados.remove(normalizar(email));
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}

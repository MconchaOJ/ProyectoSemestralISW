package cl.ubb.pem.practicas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Reglas configurables de las planificaciones (prefijo app.planificacion).
 *
 * @param diasAnticipacionMinimos días mínimos entre el envío a revisión y la clase (S-35: "unos 3 días antes",
 *                                dato reportado por un estudiante). Con 0 se puede enviar hasta el mismo día de la clase.
 */
@ConfigurationProperties(prefix = "app.planificacion")
public record PlanificacionProperties(@DefaultValue("3") int diasAnticipacionMinimos) {
}

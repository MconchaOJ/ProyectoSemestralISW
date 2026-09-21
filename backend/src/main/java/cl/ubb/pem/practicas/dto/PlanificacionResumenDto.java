package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Planificacion;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Planificación en listados (bandeja del guía) y en el historial: sin el contenido ni las revisiones. */
public record PlanificacionResumenDto(Long id, Long inscripcionId, String estudiante, String titulo, LocalDate fechaClase,
                                      EstadoPlanificacion estado, LocalDateTime fechaActualizacion) {

    public static PlanificacionResumenDto desde(Planificacion p) {
        return new PlanificacionResumenDto(p.getId(), p.getInscripcion().getId(),
                p.getInscripcion().getEstudiante().getUsuario().getNombreCompleto(), p.getTitulo(), p.getFechaClase(),
                p.getEstado(), p.getFechaActualizacion());
    }
}

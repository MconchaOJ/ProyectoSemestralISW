package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.DecisionRevision;
import cl.ubb.pem.practicas.entity.EtapaRevision;
import cl.ubb.pem.practicas.entity.RevisionPlanificacion;
import java.time.LocalDateTime;

/** Una decisión del historial de la planificación: quién, cuándo, en qué etapa y con qué comentario. */
public record RevisionDto(Long id, UsuarioResumenDto revisor, EtapaRevision etapa, DecisionRevision decision,
                          String comentario, LocalDateTime fecha) {

    public static RevisionDto desde(RevisionPlanificacion r) {
        return new RevisionDto(r.getId(), UsuarioResumenDto.desde(r.getRevisor()), r.getEtapa(), r.getDecision(),
                r.getComentario(), r.getFecha());
    }
}

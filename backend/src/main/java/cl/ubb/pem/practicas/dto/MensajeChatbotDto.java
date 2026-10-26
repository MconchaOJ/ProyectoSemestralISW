package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.AutorMensaje;
import cl.ubb.pem.practicas.entity.MensajeChatbot;
import java.time.LocalDateTime;

/** Un mensaje del chat (HU-15). {@code planificacionId} y su título solo vienen si el estudiante adjuntó una. */
public record MensajeChatbotDto(Long id, AutorMensaje autor, String contenido, Long planificacionId,
                                String planificacionTitulo, LocalDateTime fecha) {

    public static MensajeChatbotDto desde(MensajeChatbot m) {
        return new MensajeChatbotDto(m.getId(), m.getAutor(), m.getContenido(),
                m.getPlanificacion() == null ? null : m.getPlanificacion().getId(),
                m.getPlanificacion() == null ? null : m.getPlanificacion().getTitulo(), m.getFecha());
    }
}

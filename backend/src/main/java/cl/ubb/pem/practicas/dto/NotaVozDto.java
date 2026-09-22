package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.NotaVoz;
import java.time.LocalDateTime;

/** Metadatos y transcripción de la nota de voz de una evaluación. El audio se transmite aparte (audioUrl). */
public record NotaVozDto(Long id, Integer duracionSegundos, String transcripcion, Boolean transcripcionCorregida,
                         LocalDateTime fechaGrabacion, String audioUrl) {

    public static NotaVozDto desde(NotaVoz n) {
        String url = "/api/evaluaciones/" + n.getEvaluacion().getId() + "/nota-voz/audio";
        return new NotaVozDto(n.getId(), n.getDuracionSegundos(), n.getTranscripcion(), n.getTranscripcionCorregida(),
                n.getFechaGrabacion(), url);
    }
}

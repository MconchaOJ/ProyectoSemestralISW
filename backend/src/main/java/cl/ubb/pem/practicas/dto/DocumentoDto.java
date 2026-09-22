package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.DocumentoEvaluacion;
import cl.ubb.pem.practicas.entity.DocumentoPlanificacion;
import java.time.LocalDateTime;

/** Metadatos de un documento adjunto (a una planificación o a una evaluación). El archivo se descarga aparte. */
public record DocumentoDto(Long id, String nombreArchivo, String tipoMime, Long tamanoBytes,
                           UsuarioResumenDto subidoPor, LocalDateTime fechaSubida) {

    public static DocumentoDto desde(DocumentoPlanificacion d) {
        return new DocumentoDto(d.getId(), d.getNombreArchivo(), d.getTipoMime(), d.getTamanoBytes(),
                UsuarioResumenDto.desde(d.getSubidoPor()), d.getFechaSubida());
    }

    public static DocumentoDto desde(DocumentoEvaluacion d) {
        return new DocumentoDto(d.getId(), d.getNombreArchivo(), d.getTipoMime(), d.getTamanoBytes(),
                UsuarioResumenDto.desde(d.getSubidoPor()), d.getFechaSubida());
    }
}

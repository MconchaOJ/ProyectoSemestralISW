package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.DocumentoBaseConocimiento;
import cl.ubb.pem.practicas.entity.TipoDocumentoBase;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Un documento de la base de conocimiento del chatbot (HU-29); los tipos sin archivo vienen con cargado = false. */
public record DocumentoBaseDto(
        @Schema(example = "RUBRICA_PLANIFICACION") TipoDocumentoBase tipo,
        String nombre,
        @Schema(description = "Para qué lo usa el chatbot, según la propuesta RetroPlani Mate") String uso,
        boolean cargado,
        String nombreArchivo,
        Long tamanoBytes,
        @Schema(description = "Caracteres de texto que se pudieron extraer") Integer caracteres,
        UsuarioResumenDto subidoPor,
        LocalDateTime fechaSubida) {

    public static DocumentoBaseDto vacio(TipoDocumentoBase t) {
        return new DocumentoBaseDto(t, t.nombre(), t.uso(), false, null, null, null, null, null);
    }

    public static DocumentoBaseDto desde(DocumentoBaseConocimiento d) {
        TipoDocumentoBase t = d.getTipo();
        return new DocumentoBaseDto(t, t.nombre(), t.uso(), true, d.getNombreArchivo(), d.getTamanoBytes(),
                d.getTexto().length(), UsuarioResumenDto.desde(d.getSubidoPor()), d.getFechaSubida());
    }
}

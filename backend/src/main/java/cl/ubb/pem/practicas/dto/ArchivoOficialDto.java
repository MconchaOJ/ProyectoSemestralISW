package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.ArchivoOficialPauta;
import java.time.LocalDateTime;

/** HU-25: metadatos de un archivo oficial de una pauta (una versión del archivo). El archivo se descarga aparte. */
public record ArchivoOficialDto(Long id, Integer pautaId, int version, String nombreArchivo, String tipoMime,
                                Long tamanoBytes, UsuarioResumenDto subidoPor, LocalDateTime fechaSubida) {

    public static ArchivoOficialDto desde(ArchivoOficialPauta a) {
        return new ArchivoOficialDto(a.getId(), a.getPauta().getId(), a.getVersion(), a.getNombreArchivo(),
                a.getTipoMime(), a.getTamanoBytes(), UsuarioResumenDto.desde(a.getSubidoPor()), a.getFechaSubida());
    }
}

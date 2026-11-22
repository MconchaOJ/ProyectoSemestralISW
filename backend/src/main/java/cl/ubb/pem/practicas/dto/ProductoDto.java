package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.ProductoPractica;
import cl.ubb.pem.practicas.entity.TipoProducto;
import java.time.LocalDateTime;

/** HU-18: metadatos de un informe o producto de la práctica. El archivo se descarga aparte. */
public record ProductoDto(Long id, Long inscripcionId, TipoProducto tipo, String tipoNombre, String nombreArchivo,
                          String tipoMime, Long tamanoBytes, UsuarioResumenDto subidoPor, LocalDateTime fechaSubida) {

    public static ProductoDto desde(ProductoPractica p) {
        return new ProductoDto(p.getId(), p.getInscripcion().getId(), p.getTipo(), p.getTipo().nombre(),
                p.getNombreArchivo(), p.getTipoMime(), p.getTamanoBytes(), UsuarioResumenDto.desde(p.getSubidoPor()),
                p.getFechaSubida());
    }
}

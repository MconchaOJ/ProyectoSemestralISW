package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.ParametroSistema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** HU-27: un parámetro del sistema con su rango y quién lo cambió por última vez (nulo = valor inicial). */
public record ParametroDto(String clave, int valor, int minimo, int maximo, String unidad, String descripcion,
                           UsuarioResumenDto editadoPor, LocalDateTime fechaEdicion) {

    public static ParametroDto desde(ParametroSistema p) {
        return new ParametroDto(p.getClave(), p.getValor(), p.getMinimo(), p.getMaximo(), p.getUnidad(),
                p.getDescripcion(), UsuarioResumenDto.desde(p.getEditadoPor()), p.getFechaEdicion());
    }

    /** Cuerpo de PUT /api/parametros/{clave}: el valor nuevo, dentro del rango del parámetro. */
    public record Cambio(@Schema(example = "5") @NotNull Integer valor) {
    }
}

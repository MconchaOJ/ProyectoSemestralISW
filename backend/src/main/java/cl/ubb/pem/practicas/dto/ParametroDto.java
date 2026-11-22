package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.ParametroSistema;
import cl.ubb.pem.practicas.service.ParametroService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-27: un parámetro del sistema con su rango, quién lo cambió por última vez (nulo = valor inicial) y qué roles lo
 * pueden editar ({@code rolesQueEditan}: la exigencia de la escala, también el jefe de carrera).
 */
public record ParametroDto(String clave, int valor, int minimo, int maximo, String unidad, String descripcion,
                           UsuarioResumenDto editadoPor, LocalDateTime fechaEdicion, List<String> rolesQueEditan) {

    public static ParametroDto desde(ParametroSistema p) {
        return new ParametroDto(p.getClave(), p.getValor(), p.getMinimo(), p.getMaximo(), p.getUnidad(),
                p.getDescripcion(), UsuarioResumenDto.desde(p.getEditadoPor()), p.getFechaEdicion(),
                ParametroService.rolesQueEditan(p.getClave()));
    }

    /** Cuerpo de PUT /api/parametros/{clave}: el valor nuevo, dentro del rango del parámetro. */
    public record Cambio(@Schema(example = "5") @NotNull Integer valor) {
    }
}

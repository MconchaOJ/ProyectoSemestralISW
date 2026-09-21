package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.Usuario;

/** Referencia corta a un usuario (por ejemplo el guía o el evaluador dentro de otra respuesta). */
public record UsuarioResumenDto(Long id, String nombreCompleto, String email) {

    public static UsuarioResumenDto desde(Usuario u) {
        return u == null ? null : new UsuarioResumenDto(u.getId(), u.getNombreCompleto(), u.getEmail());
    }
}

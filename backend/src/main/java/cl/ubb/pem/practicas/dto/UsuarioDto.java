package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.Usuario;

/**
 * Datos públicos de un usuario. Nunca incluye el hash de la contraseña. {@code rolSecundario} es nulo salvo en las
 * cuentas especiales con dos roles (migración 004).
 */
public record UsuarioDto(Long id, String nombre, String apellidos, String email, String rol, boolean activo,
                         String rolSecundario) {

    public static UsuarioDto desde(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getApellidos(), u.getEmail(),
                u.getRol().getNombre(), Boolean.TRUE.equals(u.getActivo()),
                u.getRolSecundario() == null ? null : u.getRolSecundario().getNombre());
    }
}

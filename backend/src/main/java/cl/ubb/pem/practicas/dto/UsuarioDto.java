package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.Usuario;

/** Datos públicos de un usuario. Nunca incluye el hash de la contraseña. */
public record UsuarioDto(Long id, String nombre, String apellidos, String email, String rol, boolean activo) {

    public static UsuarioDto desde(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getApellidos(), u.getEmail(),
                u.getRol().getNombre(), Boolean.TRUE.equals(u.getActivo()));
    }
}

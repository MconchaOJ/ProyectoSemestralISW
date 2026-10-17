package cl.ubb.pem.practicas.security;

import java.util.List;

/**
 * Usuario que hizo la petición, ya validado por el filtro JWT. Se obtiene en los controladores con
 * {@code @AuthenticationPrincipal}. Los roles se leen de la base en cada petición, no del token.
 * {@code rolSecundario} es nulo salvo en cuentas especiales con dos roles (migración 004): sus permisos son la
 * suma de los dos, por eso toda pregunta de "¿tiene este rol?" pasa por {@link #tieneRol(String)}.
 */
public record UsuarioAutenticado(Long id, String email, String rol, String rolSecundario) {

    /** Cuenta normal, con un solo rol. */
    public UsuarioAutenticado(Long id, String email, String rol) {
        this(id, email, rol, null);
    }

    /** El principal primero y, si tiene, el segundo. */
    public List<String> roles() {
        return rolSecundario == null ? List.of(rol) : List.of(rol, rolSecundario);
    }

    public boolean tieneRol(String nombreRol) {
        return rol.equals(nombreRol) || nombreRol.equals(rolSecundario);
    }
}

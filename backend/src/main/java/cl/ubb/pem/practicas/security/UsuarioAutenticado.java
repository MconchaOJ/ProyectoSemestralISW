package cl.ubb.pem.practicas.security;

/**
 * Usuario que hizo la petición, ya validado por el filtro JWT. Se obtiene en los controladores con
 * {@code @AuthenticationPrincipal}. El rol se lee de la base en cada petición, no del token.
 */
public record UsuarioAutenticado(Long id, String email, String rol) {

    public boolean tieneRol(String nombreRol) {
        return rol.equals(nombreRol);
    }
}

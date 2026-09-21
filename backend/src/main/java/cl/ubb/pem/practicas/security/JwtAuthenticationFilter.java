package cl.ubb.pem.practicas.security;

import cl.ubb.pem.practicas.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee "Authorization: Bearer ...", valida el token y deja al usuario autenticado en el contexto de seguridad.
 * Si el token falta o es inválido no autentica a nadie: los endpoints protegidos responden 401 después.
 * No es un @Component a propósito: SecurityConfig lo crea, para que solo corra dentro de la cadena de seguridad.
 * Consulta al usuario en la base en cada petición, así un usuario desactivado pierde el acceso de inmediato.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            jwtService.extraerUsuarioId(cabecera.substring(PREFIJO.length()).trim())
                    .flatMap(usuarioRepository::findById)
                    .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                    .ifPresent(u -> {
                        var principal = new UsuarioAutenticado(u.getId(), u.getEmail(), u.getRol().getNombre());
                        var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + principal.rol()));
                        SecurityContextHolder.getContext()
                                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, autoridades));
                    });
        }
        chain.doFilter(request, response);
    }
}

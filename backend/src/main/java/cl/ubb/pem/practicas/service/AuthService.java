package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.LoginRequest;
import cl.ubb.pem.practicas.dto.LoginResponse;
import cl.ubb.pem.practicas.dto.UsuarioDto;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.CredencialesInvalidasException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.JwtService;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-01: inicio de sesión. Verifica la contraseña con BCrypt y entrega un token con el usuario y su rol. */
@Service
public class AuthService {

    static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    /** Hash de una contraseña al azar: se compara cuando el correo no existe, para tardar lo mismo en ambos casos. */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.hashFicticio = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        // Siempre se ejecuta una comparación BCrypt: así no se puede adivinar qué correos existen midiendo el tiempo.
        boolean claveCorrecta = encoder.matches(request.password(),
                usuario != null ? usuario.getPasswordHash() : hashFicticio);
        // Mismo mensaje para correo inexistente, clave incorrecta y usuario inactivo (HU-01, criterios 2 y 6).
        if (usuario == null || !claveCorrecta || !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
        }
        return new LoginResponse(jwtService.generarToken(usuario), "Bearer",
                jwtService.segundosDeVigencia(), UsuarioDto.desde(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioDto perfil(UsuarioAutenticado actual) {
        return usuarios.findById(actual.id())
                .map(UsuarioDto::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario ya no existe."));
    }
}

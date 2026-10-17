package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.CambioPasswordRequest;
import cl.ubb.pem.practicas.dto.LoginRequest;
import cl.ubb.pem.practicas.dto.LoginResponse;
import cl.ubb.pem.practicas.dto.UsuarioDto;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.CredencialesInvalidasException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.JwtService;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-01: inicio de sesión. Verifica la contraseña con BCrypt y entrega un token con el usuario y su rol.
 * Sprint 2: límite de intentos fallidos (BloqueoLoginService) y HU-26, cambio de la propia contraseña.
 */
@Service
public class AuthService {

    static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final BloqueoLoginService bloqueo;
    /** Hash de una contraseña al azar: se compara cuando el correo no existe, para tardar lo mismo en ambos casos. */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwtService,
                       BloqueoLoginService bloqueo) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.bloqueo = bloqueo;
        this.hashFicticio = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String correo = request.email().trim();
        // Un correo bloqueado no se revisa, aunque traiga la clave correcta (si no, se podría seguir probando claves)
        bloqueo.verificar(correo);
        Usuario usuario = usuarios.findByEmailIgnoreCase(correo).orElse(null);
        // Siempre se ejecuta una comparación BCrypt: así no se puede adivinar qué correos existen midiendo el tiempo.
        boolean claveCorrecta = encoder.matches(request.password(),
                usuario != null ? usuario.getPasswordHash() : hashFicticio);
        // Mismo mensaje para correo inexistente, clave incorrecta y usuario inactivo (HU-01, criterios 2 y 6).
        if (usuario == null || !claveCorrecta || !Boolean.TRUE.equals(usuario.getActivo())) {
            bloqueo.registrarFallo(correo);
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
        }
        bloqueo.registrarExito(correo);
        return new LoginResponse(jwtService.generarToken(usuario), "Bearer",
                jwtService.segundosDeVigencia(), UsuarioDto.desde(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioDto perfil(UsuarioAutenticado actual) {
        return usuarios.findById(actual.id())
                .map(UsuarioDto::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario ya no existe."));
    }

    /**
     * HU-26: el usuario autenticado cambia su propia contraseña. Exige la actual (un token robado no basta) y la
     * nueva debe ser distinta. Una contraseña actual incorrecta cuenta como intento fallido del límite de login.
     * El token en uso sigue valiendo hasta que expire (S-50).
     */
    @Transactional
    public void cambiarPassword(UsuarioAutenticado actual, CambioPasswordRequest request) {
        Usuario usuario = usuarios.findById(actual.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario ya no existe."));
        bloqueo.verificar(usuario.getEmail());
        if (!encoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            bloqueo.registrarFallo(usuario.getEmail());
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        bloqueo.registrarExito(usuario.getEmail());
        if (encoder.matches(request.passwordNueva(), usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña nueva debe ser distinta de la actual.");
        }
        usuario.setPasswordHash(encoder.encode(request.passwordNueva()));
        usuarios.save(usuario);
    }
}

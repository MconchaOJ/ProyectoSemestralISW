package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cl.ubb.pem.practicas.dto.LoginRequest;
import cl.ubb.pem.practicas.dto.LoginResponse;
import cl.ubb.pem.practicas.entity.Rol;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.CredencialesInvalidasException;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.JwtProperties;
import cl.ubb.pem.practicas.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtService jwt = new JwtService(new JwtProperties("", 8));
    @Mock
    private UsuarioRepository usuarios;
    private AuthService servicio;

    // Clave aleatoria de cada ejecución: la prueba no usa ninguna clave real ni la de demostración.
    private final String clave = UUID.randomUUID().toString();
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(usuarios, encoder, jwt);
        Rol rol = new Rol();
        rol.setNombre("PROFESOR_GUIA");
        usuario = new Usuario();
        usuario.setId(5L);
        usuario.setNombre("Marcos");
        usuario.setApellidos("Bravo");
        usuario.setEmail("guia@demo.example");
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setPasswordHash(encoder.encode(clave));
    }

    @Test
    void loginCorrectoEntregaTokenConElUsuario() {
        when(usuarios.findByEmailIgnoreCase("guia@demo.example")).thenReturn(Optional.of(usuario));

        LoginResponse r = servicio.login(new LoginRequest(" guia@demo.example ", clave));

        assertThat(r.tipo()).isEqualTo("Bearer");
        assertThat(r.expiraEnSegundos()).isEqualTo(8 * 3600);
        assertThat(r.usuario().rol()).isEqualTo("PROFESOR_GUIA");
        assertThat(jwt.extraerUsuarioId(r.token())).contains(5L);
    }

    @Test
    void claveIncorrectaLanza401() {
        when(usuarios.findByEmailIgnoreCase("guia@demo.example")).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> servicio.login(new LoginRequest("guia@demo.example", "otra")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void correoInexistenteDaElMismoMensajeQueClaveIncorrecta() {
        when(usuarios.findByEmailIgnoreCase("guia@demo.example")).thenReturn(Optional.of(usuario));
        when(usuarios.findByEmailIgnoreCase("nadie@demo.example")).thenReturn(Optional.empty());

        String conClaveMala = mensajeDe(new LoginRequest("guia@demo.example", "otra"));
        String conCorreoMalo = mensajeDe(new LoginRequest("nadie@demo.example", clave));

        assertThat(conCorreoMalo).isEqualTo(conClaveMala);
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesionAunqueLaClaveSeaCorrecta() {
        usuario.setActivo(false);
        when(usuarios.findByEmailIgnoreCase("guia@demo.example")).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> servicio.login(new LoginRequest("guia@demo.example", clave)))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    private String mensajeDe(LoginRequest req) {
        try {
            servicio.login(req);
            throw new AssertionError("debía fallar");
        } catch (CredencialesInvalidasException e) {
            return e.getMessage();
        }
    }
}

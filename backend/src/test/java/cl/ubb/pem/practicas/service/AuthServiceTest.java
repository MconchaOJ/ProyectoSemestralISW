package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cl.ubb.pem.practicas.dto.CambioPasswordRequest;
import cl.ubb.pem.practicas.dto.LoginRequest;
import cl.ubb.pem.practicas.dto.LoginResponse;
import cl.ubb.pem.practicas.entity.Rol;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.CredencialesInvalidasException;
import cl.ubb.pem.practicas.exception.LoginBloqueadoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.JwtProperties;
import cl.ubb.pem.practicas.security.JwtService;
import cl.ubb.pem.practicas.security.LoginProperties;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.time.Clock;
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
        // Límite de 3 intentos para la prueba (en la aplicación viene de app.login.intentos-maximos)
        servicio = new AuthService(usuarios, encoder, jwt,
                new BloqueoLoginService(new LoginProperties(3, 15), Clock.systemUTC()));
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

    @Test
    void trasLosIntentosFallidosElCorreoSeBloqueaAunqueLuegoLaClaveSeaCorrecta() {
        when(usuarios.findByEmailIgnoreCase("guia@demo.example")).thenReturn(Optional.of(usuario));
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> servicio.login(new LoginRequest("guia@demo.example", "otra")))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }
        assertThatThrownBy(() -> servicio.login(new LoginRequest("guia@demo.example", clave)))
                .isInstanceOf(LoginBloqueadoException.class);
    }

    @Test
    void correoInexistenteTambienSeBloqueaParaNoRevelarQueCorreosExisten() {
        when(usuarios.findByEmailIgnoreCase("nadie@demo.example")).thenReturn(Optional.empty());
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> servicio.login(new LoginRequest("nadie@demo.example", clave)))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }
        assertThatThrownBy(() -> servicio.login(new LoginRequest("nadie@demo.example", clave)))
                .isInstanceOf(LoginBloqueadoException.class);
    }

    @Test
    void cambiarPasswordConLaActualCorrectaGuardaElNuevoHash() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        String nueva = UUID.randomUUID().toString();

        servicio.cambiarPassword(yo(), new CambioPasswordRequest(clave, nueva));

        assertThat(encoder.matches(nueva, usuario.getPasswordHash())).isTrue();
        assertThat(encoder.matches(clave, usuario.getPasswordHash())).isFalse();
    }

    @Test
    void cambiarPasswordConLaActualIncorrectaDa400YNoCambiaNada() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        String hashAntes = usuario.getPasswordHash();

        assertThatThrownBy(() -> servicio.cambiarPassword(yo(), new CambioPasswordRequest("otra", "nueva-clave-123")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("actual no es correcta");
        assertThat(usuario.getPasswordHash()).isEqualTo(hashAntes);
    }

    @Test
    void cambiarPasswordPorLaMismaDa400() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> servicio.cambiarPassword(yo(), new CambioPasswordRequest(clave, clave)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("distinta");
    }

    @Test
    void fallarLaClaveActualAlCambiarlaCuentaParaElLimiteDeIntentos() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> servicio.cambiarPassword(yo(), new CambioPasswordRequest("otra", "nueva-clave-123")))
                    .isInstanceOf(ReglaNegocioException.class);
        }
        assertThatThrownBy(() -> servicio.cambiarPassword(yo(), new CambioPasswordRequest(clave, "nueva-clave-123")))
                .isInstanceOf(LoginBloqueadoException.class);
    }

    private UsuarioAutenticado yo() {
        return new UsuarioAutenticado(5L, "guia@demo.example", "PROFESOR_GUIA");
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

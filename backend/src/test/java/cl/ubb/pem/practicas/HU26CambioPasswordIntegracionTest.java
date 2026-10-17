package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.dto.CambioPasswordRequest;
import cl.ubb.pem.practicas.dto.LoginRequest;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.repository.RolRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-26 (cambiar la propia contraseña) y límite de intentos de login (Sprint 2), contra la base real.
 * Cada prueba usa un correo propio: el registro de intentos vive en memoria y se comparte entre pruebas.
 */
class HU26CambioPasswordIntegracionTest extends IntegracionBase {

    private static final String RUTA = "/api/auth/password";

    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private RolRepository roles;

    private Usuario crearUsuario(String email, String clave) {
        Usuario u = new Usuario();
        u.setRol(roles.findByNombre("ESTUDIANTE").orElseThrow());
        u.setNombre("Prueba");
        u.setApellidos("HU-26");
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(clave));
        u.setActivo(true);
        return usuarios.saveAndFlush(u);
    }

    private ResultActions login(String email, String clave) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(email, clave))));
    }

    @Test
    void cambiaSuContrasenaYSoloLaNuevaSirveParaEntrar() throws Exception {
        String actual = UUID.randomUUID().toString();
        String nueva = UUID.randomUUID().toString();
        crearUsuario("it.hu26a@demo.example", actual);

        reemplazar(RUTA, "it.hu26a@demo.example", new CambioPasswordRequest(actual, nueva))
                .andExpect(status().isNoContent());

        login("it.hu26a@demo.example", nueva).andExpect(status().isOk());
        login("it.hu26a@demo.example", actual).andExpect(status().isUnauthorized());
        String hash = usuarios.findByEmailIgnoreCase("it.hu26a@demo.example").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2a$10$").isNotEqualTo(nueva);
    }

    @Test
    void contrasenaActualIncorrectaDa400YNoCambiaNada() throws Exception {
        String actual = UUID.randomUUID().toString();
        crearUsuario("it.hu26b@demo.example", actual);

        reemplazar(RUTA, "it.hu26b@demo.example", new CambioPasswordRequest("no-es-la-actual", "otra-clave-nueva"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña actual no es correcta."));
        login("it.hu26b@demo.example", actual).andExpect(status().isOk());
    }

    @Test
    void laNuevaSigueLaMismaReglaQueAlCrearUnUsuario() throws Exception {
        String actual = UUID.randomUUID().toString();
        crearUsuario("it.hu26c@demo.example", actual);

        reemplazar(RUTA, "it.hu26c@demo.example", new CambioPasswordRequest(actual, "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("passwordNueva"));
        reemplazar(RUTA, "it.hu26c@demo.example", new CambioPasswordRequest(actual, "x".repeat(73)))
                .andExpect(status().isBadRequest());
        reemplazar(RUTA, "it.hu26c@demo.example", new CambioPasswordRequest(actual, actual))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña nueva debe ser distinta de la actual."));
    }

    @Test
    void sinTokenDa401YCualquierRolPuedeCambiarLaSuya() throws Exception {
        mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CambioPasswordRequest("a-b-c-d-e-f", "g-h-i-j-k-l"))))
                .andExpect(status().isUnauthorized());
        // Los 5 roles llegan al servicio (400 por la clave actual incorrecta, no 403)
        for (String email : new String[] {COORDINADOR, GUIA2, CURSO2, JAVIERA, JEFE_CARRERA}) {
            reemplazar(RUTA, email, new CambioPasswordRequest("no-es-la-actual", "otra-clave-nueva"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void trasCincoIntentosFallidosElLoginSeBloqueaCon429AunqueLaClaveSeaCorrecta() throws Exception {
        String clave = UUID.randomUUID().toString();
        crearUsuario("it.hu26d@demo.example", clave);

        for (int i = 0; i < 5; i++) {   // app.login.intentos-maximos=5 en application.properties
            login("it.hu26d@demo.example", "incorrecta").andExpect(status().isUnauthorized());
        }
        login("it.hu26d@demo.example", clave)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.mensaje").value(containsString("15 minutos")));
        // Otro correo no queda afectado
        String otra = UUID.randomUUID().toString();
        crearUsuario("it.hu26e@demo.example", otra);
        login("it.hu26e@demo.example", otra).andExpect(status().isOk());
    }
}

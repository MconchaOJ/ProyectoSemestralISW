package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.dto.LoginRequest;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.repository.RolRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

/** HU-01: inicio de sesión y roles, contra la base real (cada prueba se revierte). */
class HU01AutenticacionIntegracionTest extends IntegracionBase {

    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private RolRepository roles;

    /** Crea un usuario con una clave aleatoria de la prueba (no se usa la clave de demostración). */
    private Usuario crearUsuario(String email, String clave, boolean activo) {
        Usuario u = new Usuario();
        u.setRol(roles.findByNombre("PROFESOR_GUIA").orElseThrow());
        u.setNombre("Prueba");
        u.setApellidos("Integración");
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(clave));
        u.setActivo(activo);
        return usuarios.saveAndFlush(u);
    }

    private ResultActions login(String email, String clave) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(email, clave))));
    }

    @Test
    void loginCorrectoEntregaTokenYRolYNuncaElHash() throws Exception {
        String clave = UUID.randomUUID().toString();
        crearUsuario("it.hu01@demo.example", clave, true);

        String cuerpo = login("it.hu01@demo.example", clave)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("PROFESOR_GUIA"))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("password").doesNotContain("$2a$");
    }

    @Test
    void elTokenObtenidoSirveParaConsultarElPerfil() throws Exception {
        String clave = UUID.randomUUID().toString();
        crearUsuario("it.hu01b@demo.example", clave, true);
        String token = cuerpo(login("it.hu01b@demo.example", clave)).get("token").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("it.hu01b@demo.example"));
    }

    @Test
    void claveIncorrectaYCorreoInexistenteDan401ConElMismoMensaje() throws Exception {
        String clave = UUID.randomUUID().toString();
        crearUsuario("it.hu01c@demo.example", clave, true);

        String claveMala = login("it.hu01c@demo.example", "incorrecta").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String correoMalo = login("no.existe@demo.example", clave).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(claveMala).get("mensaje")).isEqualTo(json.readTree(correoMalo).get("mensaje"));
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesion() throws Exception {
        String clave = UUID.randomUUID().toString();
        crearUsuario("it.hu01d@demo.example", clave, false);
        login("it.hu01d@demo.example", clave).andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDesactivadoDespuesPierdeElAccesoConSuTokenAnterior() throws Exception {
        Usuario u = crearUsuario("it.hu01e@demo.example", UUID.randomUUID().toString(), true);
        String token = jwtService.generarToken(u);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        u.setActivo(false);
        usuarios.saveAndFlush(u);

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginConCamposVaciosDa400ConDetallePorCampo() throws Exception {
        login("", "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(2));
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get("/api/roles")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenInvalidoDa401() throws Exception {
        mvc.perform(get("/api/roles").header("Authorization", "Bearer esto.no.es.un.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolSinPermisoDa403YRolCorrectoDa200() throws Exception {
        consultar("/api/roles", CAMILA).andExpect(status().isForbidden());
        consultar("/api/roles", GUIA1).andExpect(status().isForbidden());
        consultar("/api/roles", CURSO1).andExpect(status().isForbidden());
        consultar("/api/roles", JEFE_CARRERA).andExpect(status().isForbidden());
        consultar("/api/roles", COORDINADOR).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void existenLosSeisRolesDelSistema() throws Exception {   // TUTOR desde HU-07 (migración 006)
        var nombres = cuerpo(consultar("/api/roles", COORDINADOR)).findValuesAsText("nombre");
        assertThat(nombres).containsExactly("ESTUDIANTE", "PROFESOR_GUIA", "PROFESOR_CURSO", "COORDINADOR", "JEFE_CARRERA", "TUTOR");
    }

    @Test
    void loginEsPublicoYSwaggerNoPideToken() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}

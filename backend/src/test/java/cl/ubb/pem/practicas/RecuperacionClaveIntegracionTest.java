package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.dto.LoginRequest;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.repository.RolRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Recuperación de la clave por correo (S-97 a S-99), contra la base real. El SMTP es un doble de Mockito: las
 * pruebas NUNCA mandan correos. El correo sale en otro hilo, por eso se verifica con timeout(...) y after(...).
 * Cada prueba usa correos propios (el límite cuenta los enlaces de cada usuario).
 */
@TestPropertySource(properties = {"app.correo.host=smtp.prueba.invalid", "app.correo.remitente=sistema@demo.example"})
class RecuperacionClaveIntegracionTest extends IntegracionBase {

    private static final String RECUPERAR = "/api/auth/recuperar-clave";
    private static final String RESTABLECER = "/api/auth/restablecer-clave";
    private static final String MENSAJE_GENERICO = "Si el correo está registrado, te enviamos un enlace para "
            + "restablecer tu clave. Revisa tu bandeja de entrada y la carpeta de spam.";
    private static final String MENSAJE_INVALIDO = "El enlace para restablecer la clave no es válido o ya venció. "
            + "Pide uno nuevo.";
    private static final Pattern TOKEN = Pattern.compile("restablecer-clave\\.html\\?token=([A-Za-z0-9_-]+)");
    private static final long ESPERA_MS = 3000;
    private static final long SIN_CORREO_MS = 400;

    @MockitoBean
    private JavaMailSender mail;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private RolRepository roles;

    private Usuario crearUsuario(String email, String clave, boolean activo) {
        Usuario u = new Usuario();
        u.setRol(roles.findByNombre("ESTUDIANTE").orElseThrow());
        u.setNombre("Prueba");
        u.setApellidos("Recuperación");
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(clave));
        u.setActivo(activo);
        return usuarios.saveAndFlush(u);
    }

    private ResultActions pedir(String email) throws Exception {
        return mvc.perform(post(RECUPERAR).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email))));
    }

    private ResultActions restablecer(String token, String nuevaClave) throws Exception {
        return mvc.perform(post(RESTABLECER).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", token, "nuevaClave", nuevaClave))));
    }

    private ResultActions login(String email, String clave) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(email, clave))));
    }

    /** Los correos mandados hasta ahora (espera a que lleguen los "veces" esperados). */
    private List<SimpleMailMessage> correos(int veces) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, timeout(ESPERA_MS).times(veces)).send(captor.capture());
        return captor.getAllValues();
    }

    private static String token(SimpleMailMessage correo) {
        Matcher m = TOKEN.matcher(correo.getText());
        assertThat(m.find()).as("el correo trae el enlace").isTrue();
        return m.group(1);
    }

    private long enlacesDe(String email) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM token_recuperacion_clave t "
                + "JOIN usuario u ON u.id = t.usuario_id WHERE u.email = ?1").setParameter(1, email)
                .getSingleResult()).longValue();
    }

    @Test
    void casoFelizLlegaElEnlaceYSoloLaClaveNuevaSirve() throws Exception {
        String vieja = UUID.randomUUID().toString();
        String nueva = UUID.randomUUID().toString();
        crearUsuario("it.rec.a@demo.example", vieja, true);

        pedir("it.rec.a@demo.example").andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_GENERICO));

        SimpleMailMessage correo = correos(1).get(0);
        assertThat(correo.getTo()).containsExactly("it.rec.a@demo.example");
        assertThat(correo.getFrom()).isEqualTo("sistema@demo.example");
        assertThat(correo.getText()).contains("http://localhost:8080/restablecer-clave.html?token=")
                .contains("30 minutos");
        String token = token(correo);
        assertThat(token).hasSizeGreaterThanOrEqualTo(43);   // 32 bytes en Base64url

        // En la base queda solo el SHA-256 (64 hexadecimales), nunca el token
        String hash = (String) em.createNativeQuery("SELECT t.token_hash FROM token_recuperacion_clave t "
                + "JOIN usuario u ON u.id = t.usuario_id WHERE u.email = 'it.rec.a@demo.example'").getSingleResult();
        assertThat(hash).matches("[0-9a-f]{64}").isNotEqualTo(token);

        restablecer(token, nueva).andExpect(status().isNoContent());
        login("it.rec.a@demo.example", nueva).andExpect(status().isOk());
        login("it.rec.a@demo.example", vieja).andExpect(status().isUnauthorized());
        assertThat(usuarios.findByEmailIgnoreCase("it.rec.a@demo.example").orElseThrow().getPasswordHash())
                .startsWith("$2a$10$");
    }

    @Test
    void elEnlaceSirveUnaSolaVez() throws Exception {
        crearUsuario("it.rec.b@demo.example", UUID.randomUUID().toString(), true);
        pedir("it.rec.b@demo.example").andExpect(status().isOk());
        String token = token(correos(1).get(0));

        restablecer(token, "primera-clave-nueva").andExpect(status().isNoContent());
        restablecer(token, "segunda-clave-nueva").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_INVALIDO));
        login("it.rec.b@demo.example", "primera-clave-nueva").andExpect(status().isOk());
    }

    @Test
    void elEnlaceVencidoNoSirveYLaClaveNoCambia() throws Exception {
        String vieja = UUID.randomUUID().toString();
        crearUsuario("it.rec.c@demo.example", vieja, true);
        pedir("it.rec.c@demo.example").andExpect(status().isOk());
        String token = token(correos(1).get(0));

        // Se lleva el enlace al pasado: creado hace 61 minutos, venció hace 31
        em.createNativeQuery("UPDATE token_recuperacion_clave t JOIN usuario u ON u.id = t.usuario_id "
                        + "SET t.fecha_creacion = t.fecha_creacion - INTERVAL 61 MINUTE, "
                        + "t.fecha_expiracion = t.fecha_expiracion - INTERVAL 61 MINUTE "
                        + "WHERE u.email = 'it.rec.c@demo.example'").executeUpdate();
        em.clear();   // la entidad del enlace seguía en la caché de JPA con las fechas de antes

        restablecer(token, "clave-nueva-larga").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_INVALIDO));
        login("it.rec.c@demo.example", vieja).andExpect(status().isOk());
    }

    @Test
    void unTokenInexistenteDaElMismoMensaje() throws Exception {
        restablecer("token-que-no-existe-0123456789abcdefghijklmnop", "clave-nueva-larga")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_INVALIDO));
        restablecer("", "clave-nueva-larga").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("token"));
    }

    @Test
    void unCorreoQueNoExisteOInactivoRespondeIgualQueUnoQueExisteYNoMandaNada() throws Exception {
        crearUsuario("it.rec.d@demo.example", UUID.randomUUID().toString(), true);
        crearUsuario("it.rec.inactivo@demo.example", UUID.randomUUID().toString(), false);

        String noExiste = pedir("nadie.registrado@demo.example").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String inactivo = pedir("it.rec.inactivo@demo.example").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        verify(mail, after(SIN_CORREO_MS).never()).send(any(SimpleMailMessage.class));
        assertThat(enlacesDe("it.rec.inactivo@demo.example")).isZero();

        String existe = pedir("IT.REC.D@demo.example ").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(noExiste).isEqualTo(existe).isEqualTo(inactivo);
        assertThat(correos(1).get(0).getTo()).containsExactly("it.rec.d@demo.example");
    }

    @Test
    void trasTresSolicitudesNoSeMandaOtroCorreoPeroLaRespuestaEsLaMisma() throws Exception {
        crearUsuario("it.rec.e@demo.example", UUID.randomUUID().toString(), true);
        for (int i = 0; i < 3; i++) {   // app.recuperacion.solicitudes-maximas=3 cada 15 minutos
            pedir("it.rec.e@demo.example").andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value(MENSAJE_GENERICO));
        }
        correos(3);
        pedir("it.rec.e@demo.example").andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_GENERICO));
        verify(mail, after(SIN_CORREO_MS).times(3)).send(any(SimpleMailMessage.class));
        assertThat(enlacesDe("it.rec.e@demo.example")).isEqualTo(3);

        // Pasada la ventana, puede pedir otro
        em.createNativeQuery("UPDATE token_recuperacion_clave t JOIN usuario u ON u.id = t.usuario_id "
                        + "SET t.fecha_creacion = t.fecha_creacion - INTERVAL 16 MINUTE "
                        + "WHERE u.email = 'it.rec.e@demo.example'").executeUpdate();
        em.clear();
        pedir("it.rec.e@demo.example").andExpect(status().isOk());
        correos(4);
    }

    @Test
    void unEnlaceNuevoDejaSinEfectoElAnterior() throws Exception {
        crearUsuario("it.rec.f@demo.example", UUID.randomUUID().toString(), true);
        pedir("it.rec.f@demo.example").andExpect(status().isOk());
        String primero = token(correos(1).get(0));
        pedir("it.rec.f@demo.example").andExpect(status().isOk());
        String segundo = token(correos(2).get(1));
        assertThat(segundo).isNotEqualTo(primero);

        restablecer(primero, "clave-nueva-larga").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_INVALIDO));
        restablecer(segundo, "clave-nueva-larga").andExpect(status().isNoContent());
    }

    @Test
    void laClaveNuevaSigueLaReglaDeHu26YElEnlaceSigueSirviendo() throws Exception {
        crearUsuario("it.rec.g@demo.example", UUID.randomUUID().toString(), true);
        pedir("it.rec.g@demo.example").andExpect(status().isOk());
        String token = token(correos(1).get(0));

        restablecer(token, "corta").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nuevaClave"));
        restablecer(token, "x".repeat(73)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nuevaClave"));
        restablecer(token, "ocho1234").andExpect(status().isNoContent());   // 8 caracteres: el mínimo
    }

    @Test
    void sirveParaLosSeisRoles() throws Exception {
        List<String> correosRoles = List.of(COORDINADOR, GUIA2, CURSO2, JEFE_CARRERA, "tutor1@demo.example", JAVIERA);
        for (String email : correosRoles) {
            pedir(email).andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").value(MENSAJE_GENERICO));
        }
        assertThat(correos(6)).extracting(c -> c.getTo()[0]).containsExactlyInAnyOrderElementsOf(correosRoles);
    }

    @Test
    void restablecerLevantaElBloqueoDelLogin() throws Exception {
        crearUsuario("it.rec.h@demo.example", UUID.randomUUID().toString(), true);
        for (int i = 0; i < 5; i++) {   // app.login.intentos-maximos=5
            login("it.rec.h@demo.example", "incorrecta").andExpect(status().isUnauthorized());
        }
        pedir("it.rec.h@demo.example").andExpect(status().isOk());
        restablecer(token(correos(1).get(0)), "clave-nueva-larga").andExpect(status().isNoContent());
        login("it.rec.h@demo.example", "clave-nueva-larga").andExpect(status().isOk());
    }

    @Test
    void unSmtpCaidoNoSeVuelveUnErrorParaElUsuario() throws Exception {
        crearUsuario("it.rec.i@demo.example", UUID.randomUUID().toString(), true);
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP caído"))
                .when(mail).send(any(SimpleMailMessage.class));
        pedir("it.rec.i@demo.example").andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_GENERICO));
        verify(mail, timeout(ESPERA_MS).times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void elCorreoSeValida() throws Exception {
        pedir("").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0].campo").value("email"));
        verify(mail, after(SIN_CORREO_MS).never()).send(any(SimpleMailMessage.class));
    }
}

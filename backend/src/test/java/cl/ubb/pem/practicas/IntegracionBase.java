package cl.ubb.pem.practicas;

import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de las pruebas de integración: levanta la aplicación completa contra la base MySQL real (usuario isw_app)
 * y con los datos de db/seed.sql. Cada prueba corre en una transacción que se revierte al terminar, así la base
 * de demostración queda intacta.
 *
 * Los tokens se generan directamente con JwtService para los usuarios del seed: las pruebas no necesitan
 * (ni escriben) la clave de demostración.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegracionBase {

    protected static final String COORDINADOR = "coordinador@demo.example";
    protected static final String GUIA1 = "guia1@demo.example";      // Marcos Bravo (id 2)
    protected static final String GUIA2 = "guia2@demo.example";      // Lorena Vidal (id 3)
    protected static final String CURSO1 = "curso1@demo.example";    // Ricardo Núñez (id 4)
    protected static final String CURSO2 = "curso2@demo.example";    // Patricia Contreras (id 5)
    protected static final String CAMILA = "camila.rojas@demo.example";        // estudiante 1
    protected static final String DIEGO = "diego.munoz@demo.example";          // estudiante 2
    protected static final String VALENTINA = "valentina.perez@demo.example";  // estudiante 3
    protected static final String SEBASTIAN = "sebastian.vargas@demo.example"; // estudiante 4
    protected static final String JAVIERA = "javiera.herrera@demo.example";    // estudiante 5

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected JwtService jwtService;
    @Autowired
    protected UsuarioRepository usuarios;
    @PersistenceContext
    protected EntityManager em;

    protected String token(String email) {
        Usuario u = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        return jwtService.generarToken(u);
    }

    protected ResultActions consultar(String url, String email) throws Exception {
        return mvc.perform(autenticar(MockMvcRequestBuilders.get(url), email));
    }

    protected ResultActions enviar(String url, String email, Object cuerpo) throws Exception {
        return mvc.perform(autenticar(MockMvcRequestBuilders.post(url), email)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)));
    }

    protected ResultActions enviar(String url, String email) throws Exception {
        return mvc.perform(autenticar(MockMvcRequestBuilders.post(url), email));
    }

    protected ResultActions reemplazar(String url, String email, Object cuerpo) throws Exception {
        return mvc.perform(autenticar(MockMvcRequestBuilders.put(url), email)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)));
    }

    private MockHttpServletRequestBuilder autenticar(MockHttpServletRequestBuilder req, String email) {
        return email == null ? req : req.header("Authorization", "Bearer " + token(email));
    }

    /** POST multipart/form-data (subir un archivo), ya autenticado: agrega .file(...) y .param(...) al resultado. */
    protected MockMultipartHttpServletRequestBuilder multipart(String url, String email) {
        MockMultipartHttpServletRequestBuilder req = MockMvcRequestBuilders.multipart(url);
        if (email != null) {
            req.header("Authorization", "Bearer " + token(email));
        }
        return req;
    }

    /** Un archivo de prueba pequeño con el nombre, tipo MIME y contenido dados. */
    protected static MockMultipartFile archivo(String parametro, String nombre, String tipoMime, byte[] contenido) {
        return new MockMultipartFile(parametro, nombre, tipoMime, contenido);
    }

    protected JsonNode cuerpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    /** Los "id" del nivel superior de cada elemento de un arreglo JSON (sin los id anidados). */
    protected static java.util.List<Long> ids(JsonNode arreglo) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        arreglo.forEach(n -> ids.add(n.get("id").asLong()));
        return ids;
    }

    /** Envía los cambios pendientes a la base (así se comprueban sus CHECK y triggers) y vacía la caché de JPA. */
    protected void sincronizar() {
        em.flush();
        em.clear();
    }
}

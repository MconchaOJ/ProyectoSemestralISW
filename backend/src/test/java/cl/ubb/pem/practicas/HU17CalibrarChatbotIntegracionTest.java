package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.repository.CalibracionChatbotRepository;
import cl.ubb.pem.practicas.service.ModeloLenguaje.Solicitud;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * HU-17: el coordinador calibra las instrucciones del chatbot sin tocar código, y el chatbot usa el texto nuevo desde
 * el siguiente mensaje. SUPUESTOS S-65, S-67 y S-68. El modelo es un doble (ChatbotIntegracionBase): se revisa el texto
 * que el sistema le enviaría.
 */
class HU17CalibrarChatbotIntegracionTest extends ChatbotIntegracionBase {

    private static final String INSTRUCCIONES = "/api/chatbot/instrucciones";
    private static final String TEXTO_NUEVO = "Eres el asistente calibrado por la coordinación. "
            + "Responde siempre con un saludo breve y luego con preguntas de reflexión. ".repeat(4);

    @Autowired
    private CalibracionChatbotRepository calibraciones;

    private static String textoBase() throws Exception {
        String t = new ClassPathResource("chatbot/instrucciones-base.txt").getContentAsString(StandardCharsets.UTF_8);
        return t.replace("\r\n", "\n").strip();
    }

    @Test
    void elSeedCargaElTextoBaseSinEditar() throws Exception {   // seed.sql y el archivo base no se desincronizan
        JsonNode r = cuerpo(consultar(INSTRUCCIONES, COORDINADOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.esTextoBase").value(true))
                .andExpect(jsonPath("$.editadoPor").doesNotExist())
                .andExpect(jsonPath("$.maximoCaracteres").value(12000)));
        assertThat(r.get("texto").asText()).isEqualTo(textoBase());
        assertThat(r.get("textoBase").asText()).isEqualTo(textoBase());
        assertThat(textoBase()).contains("Expresiones a evitar", "Respuestas modelo", "Piden diseñar");
    }

    @Test
    void elCoordinadorEditaYElChatbotUsaElTextoNuevoSinReescribirLoGuardado() throws Exception {   // criterios 1 a 3
        escribir(CHAT_VALENTINA, VALENTINA, "Primer mensaje, con el texto base.").andExpect(status().isCreated());

        reemplazar(INSTRUCCIONES, COORDINADOR, Map.of("texto", "  " + TEXTO_NUEVO + "\r\n")).andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value(TEXTO_NUEVO.strip()))
                .andExpect(jsonPath("$.esTextoBase").value(false))
                .andExpect(jsonPath("$.editadoPor.email").value(COORDINADOR))
                .andExpect(jsonPath("$.fechaEdicion").isNotEmpty());
        consultar(INSTRUCCIONES, COORDINADOR).andExpect(jsonPath("$.texto").value(TEXTO_NUEVO.strip()))
                .andExpect(jsonPath("$.editadoPor.email").value(COORDINADOR));

        escribir(CHAT_VALENTINA, VALENTINA, "Segundo mensaje, ya calibrado.").andExpect(status().isCreated());

        var enviadas = solicitudes(2);
        assertThat(enviadas.get(0).instrucciones()).startsWith(textoBase()).doesNotContain(TEXTO_NUEVO.strip());
        Solicitud calibrada = enviadas.get(1);
        assertThat(calibrada.instrucciones()).startsWith(TEXTO_NUEVO.strip())
                .doesNotContain("Expresiones a evitar")
                // el contexto del estudiante lo sigue agregando el sistema
                .contains("## Contexto del estudiante", "Valentina");
        // los mensajes ya guardados no cambian (criterio 3)
        consultar(CHAT_VALENTINA, VALENTINA).andExpect(jsonPath("$.mensajes.length()").value(4))
                .andExpect(jsonPath("$.mensajes[0].contenido").value("Primer mensaje, con el texto base."))
                .andExpect(jsonPath("$.mensajes[1].contenido").value(RESPUESTA));
    }

    @Test
    void volverAlTextoBaseLoMarcaComoBase() throws Exception {
        reemplazar(INSTRUCCIONES, COORDINADOR, Map.of("texto", TEXTO_NUEVO)).andExpect(status().isOk());
        reemplazar(INSTRUCCIONES, COORDINADOR, Map.of("texto", textoBase())).andExpect(status().isOk())
                .andExpect(jsonPath("$.esTextoBase").value(true))
                .andExpect(jsonPath("$.editadoPor.email").value(COORDINADOR));   // queda quién lo restauró
    }

    @Test
    void soloElCoordinadorLeeYEditaLasInstrucciones() throws Exception {
        for (String otro : new String[] {VALENTINA, GUIA2, CURSO2, JEFE_CARRERA, "tutor1@demo.example"}) {
            consultar(INSTRUCCIONES, otro).andExpect(status().isForbidden());
            reemplazar(INSTRUCCIONES, otro, Map.of("texto", TEXTO_NUEVO)).andExpect(status().isForbidden());
        }
        consultar(INSTRUCCIONES, COORDINADOR).andExpect(jsonPath("$.esTextoBase").value(true));   // nada cambió
    }

    @Test
    void unTextoVacioMuyCortoOMuyLargoDa400() throws Exception {   // SUPUESTO S-67
        reemplazar(INSTRUCCIONES, COORDINADOR, Map.of("texto", "   ")).andExpect(status().isBadRequest());
        reemplazar(INSTRUCCIONES, COORDINADOR, Map.of("texto", "Sé amable.")).andExpect(status().isBadRequest());
        reemplazar(INSTRUCCIONES, COORDINADOR, Map.of("texto", "x".repeat(12_001))).andExpect(status().isBadRequest());
        consultar(INSTRUCCIONES, COORDINADOR).andExpect(jsonPath("$.esTextoBase").value(true));
    }

    @Test
    void sinFilaEnLaTablaElChatbotUsaElTextoBaseYElCoordinadorPuedeGuardar() throws Exception {
        calibraciones.deleteAll();   // como una base de producción recién instalada (datos-iniciales.sql no la carga)
        sincronizar();

        consultar(INSTRUCCIONES, COORDINADOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.esTextoBase").value(true))
                .andExpect(jsonPath("$.editadoPor").doesNotExist());
        escribir(CHAT_VALENTINA, VALENTINA, "Hola").andExpect(status().isCreated());
        assertThat(solicitudes(1).get(0).instrucciones()).startsWith(textoBase());

        reemplazar(INSTRUCCIONES, COORDINADOR, Map.of("texto", TEXTO_NUEVO)).andExpect(status().isOk());
        sincronizar();
        assertThat(calibraciones.count()).isEqualTo(1);
    }
}

package cl.ubb.pem.practicas;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HU-30, lo mínimo en el sistema: con la validación abierta el chatbot solo responde a los estudiantes validadores
 * (SUPUESTO S-71). Seed: validación cerrada; Valentina (estudiante 3, inscripción 11) es la validadora de demostración;
 * Sebastián (estudiante 4, inscripción 17 de Práctica Profesional, en curso) no lo es.
 */
class HU30ValidacionChatbotIntegracionTest extends ChatbotIntegracionBase {

    private static final String VALIDACION = "/api/chatbot/validacion";
    private static final String CHAT_SEBASTIAN = "/api/inscripciones/17/chatbot";

    @Test
    void conLaValidacionCerradaTodosConversan() throws Exception {
        consultar(VALIDACION, COORDINADOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.abierta").value(false))
                .andExpect(jsonPath("$.validadores.length()").value(1))
                .andExpect(jsonPath("$.validadores[0].nombreCompleto").value("Valentina Pérez Cortés"));
        escribir(CHAT_SEBASTIAN, SEBASTIAN, "Hola").andExpect(status().isCreated());
    }

    @Test
    void conLaValidacionAbiertaUnNoValidadorRecibe409YUnValidadorConversaNormal() throws Exception {
        reemplazar(VALIDACION, COORDINADOR, Map.of("abierta", true)).andExpect(status().isOk())
                .andExpect(jsonPath("$.abierta").value(true));

        escribir(CHAT_SEBASTIAN, SEBASTIAN, "Hola").andExpect(status().isConflict());
        consultar(CHAT_SEBASTIAN, SEBASTIAN).andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.puedeEscribir").value(false))
                .andExpect(jsonPath("$.motivoNoDisponible").value(org.hamcrest.Matchers.containsString("aún no está disponible")));
        escribir(CHAT_VALENTINA, VALENTINA, "Hola").andExpect(status().isCreated());

        // el coordinador suma a Sebastián al grupo y ya puede conversar
        reemplazar(VALIDACION + "/validadores/4", COORDINADOR, Map.of("validador", true)).andExpect(status().isOk())
                .andExpect(jsonPath("$.validadores.length()").value(2));
        escribir(CHAT_SEBASTIAN, SEBASTIAN, "Hola").andExpect(status().isCreated());

        // al cerrar la validación, todos vuelven a conversar
        reemplazar(VALIDACION + "/validadores/4", COORDINADOR, Map.of("validador", false)).andExpect(status().isOk());
        reemplazar(VALIDACION, COORDINADOR, Map.of("abierta", false)).andExpect(status().isOk());
        escribir(CHAT_SEBASTIAN, SEBASTIAN, "Hola de nuevo").andExpect(status().isCreated());
    }

    @Test
    void soloElCoordinadorManejaLaValidacion() throws Exception {
        for (String otro : new String[] {VALENTINA, GUIA2, CURSO2, JEFE_CARRERA, "tutor1@demo.example"}) {
            consultar(VALIDACION, otro).andExpect(status().isForbidden());
            reemplazar(VALIDACION, otro, Map.of("abierta", true)).andExpect(status().isForbidden());
            reemplazar(VALIDACION + "/validadores/4", otro, Map.of("validador", true)).andExpect(status().isForbidden());
        }
        reemplazar(VALIDACION + "/validadores/99999", COORDINADOR, Map.of("validador", true))
                .andExpect(status().isNotFound());
        reemplazar(VALIDACION, COORDINADOR, Map.of()).andExpect(status().isBadRequest());
        consultar(VALIDACION, COORDINADOR).andExpect(jsonPath("$.abierta").value(false));
    }
}

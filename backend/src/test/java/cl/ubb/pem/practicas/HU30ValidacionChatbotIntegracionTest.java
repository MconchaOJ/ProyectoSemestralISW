package cl.ubb.pem.practicas;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HU-30: con la validación abierta el chatbot solo responde a los estudiantes validadores, y los validadores
 * responden en el sistema la Escala SUS y la rúbrica de juicio experto de la propuesta RetroPlani Mate (S-75). Seed: validación cerrada; Valentina (estudiante 3, inscripción 11) es la validadora de demostración;
 * Sebastián (estudiante 4, inscripción 17 de Práctica Profesional, en curso) no lo es.
 */
class HU30ValidacionChatbotIntegracionTest extends ChatbotIntegracionBase {

    private static final String VALIDACION = "/api/chatbot/validacion";
    private static final String CHAT_SEBASTIAN = "/api/inscripciones/17/chatbot";
    private static final String SUS = VALIDACION + "/sus";
    private static final String RUBRICA = VALIDACION + "/rubrica";
    private static final String RESULTADOS = VALIDACION + "/resultados";
    /** Impares (positivos) 4 o 5 y pares (negativos) 1 o 2: puntaje SUS = (16 + 16) × 2,5 = 80. */
    private static final List<Integer> ITEMS_SUS = List.of(4, 2, 5, 1, 4, 2, 4, 2, 4, 2);

    private static Map<String, Object> rubrica(int puntaje, int criterios) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (int n = 1; n <= criterios; n++) {
            Map<String, Object> c = new HashMap<>(Map.of("puntaje", n == 6 ? 2 : puntaje));
            if (n == 6) {
                c.put("comentario", "En un momento propuso actividades completas.");
            }
            lista.add(c);
        }
        return Map.of("criterios", lista, "fortalezas", "Pregunta antes de retroalimentar.", "recomendacion", "  ");
    }

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

    // ---------- Escala SUS y rúbrica de juicio experto ----------

    @Test
    void unValidadorRespondeLaSusYLaRubricaUnaSolaVezYElCoordinadorVeLosResultados() throws Exception {
        consultar(VALIDACION + "/mi-validacion", VALENTINA).andExpect(status().isOk())
                .andExpect(jsonPath("$.validador").value(true))
                .andExpect(jsonPath("$.susRespondida").doesNotExist())
                .andExpect(jsonPath("$.instrumentos.itemsSus.length()").value(10))
                .andExpect(jsonPath("$.instrumentos.itemsSus[0]").value("Me gustaría usar este chatbot frecuentemente."))
                .andExpect(jsonPath("$.instrumentos.criterios.length()").value(9))
                .andExpect(jsonPath("$.instrumentos.preguntasAbiertas.length()").value(4));

        enviar(SUS, VALENTINA, Map.of("items", ITEMS_SUS)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.susRespondida").exists())
                .andExpect(jsonPath("$.rubricaRespondida").doesNotExist());
        enviar(SUS, VALENTINA, Map.of("items", ITEMS_SUS)).andExpect(status().isConflict());

        enviar(RUBRICA, VALENTINA, rubrica(4, 9)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.rubricaRespondida").exists());
        enviar(RUBRICA, VALENTINA, rubrica(3, 9)).andExpect(status().isConflict());

        consultar(RESULTADOS, COORDINADOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.sus.respuestas").value(1))
                .andExpect(jsonPath("$.sus.promedioPuntaje").value(80.0))
                .andExpect(jsonPath("$.sus.promedioPorItem[0]").value(4.0))
                .andExpect(jsonPath("$.sus.detalle[0].validador.nombreCompleto").value("Valentina Pérez Cortés"))
                .andExpect(jsonPath("$.sus.detalle[0].puntaje").value(80.0))
                .andExpect(jsonPath("$.sus.detalle[0].items[1]").value(2))
                .andExpect(jsonPath("$.rubrica.respuestas").value(1))
                .andExpect(jsonPath("$.rubrica.promedioPorCriterio[0]").value(4.0))
                .andExpect(jsonPath("$.rubrica.promedioPorCriterio[5]").value(2.0))
                .andExpect(jsonPath("$.rubrica.detalle[0].criterios.length()").value(9))
                .andExpect(jsonPath("$.rubrica.detalle[0].criterios[5].comentario").value("En un momento propuso actividades completas."))
                .andExpect(jsonPath("$.rubrica.detalle[0].criterios[0].comentario").doesNotExist())
                .andExpect(jsonPath("$.rubrica.detalle[0].fortalezas").value("Pregunta antes de retroalimentar."))
                .andExpect(jsonPath("$.rubrica.detalle[0].recomendacion").doesNotExist())   // en blanco = sin respuesta
                .andExpect(jsonPath("$.instrumentos.criterios[4].nombre").value(org.hamcrest.Matchers.containsString("DUA")));
    }

    @Test
    void sinRespuestasLosPromediosVanVacios() throws Exception {
        consultar(RESULTADOS, COORDINADOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.sus.respuestas").value(0))
                .andExpect(jsonPath("$.sus.promedioPuntaje").doesNotExist())
                .andExpect(jsonPath("$.rubrica.detalle.length()").value(0));
    }

    @Test
    void unEstudianteQueNoEsValidadorNoPuedeResponder() throws Exception {
        consultar(VALIDACION + "/mi-validacion", SEBASTIAN).andExpect(status().isOk())
                .andExpect(jsonPath("$.validador").value(false));
        enviar(SUS, SEBASTIAN, Map.of("items", ITEMS_SUS)).andExpect(status().isForbidden());
        enviar(RUBRICA, SEBASTIAN, rubrica(4, 9)).andExpect(status().isForbidden());

        // marcado por el coordinador, ya puede
        reemplazar(VALIDACION + "/validadores/4", COORDINADOR, Map.of("validador", true)).andExpect(status().isOk());
        enviar(SUS, SEBASTIAN, Map.of("items", ITEMS_SUS)).andExpect(status().isCreated());
    }

    @Test
    void losDemasRolesNoRespondenNiVenLosResultados() throws Exception {
        for (String otro : new String[] {GUIA2, CURSO2, COORDINADOR, JEFE_CARRERA, "tutor1@demo.example"}) {
            consultar(VALIDACION + "/mi-validacion", otro).andExpect(status().isForbidden());
            enviar(SUS, otro, Map.of("items", ITEMS_SUS)).andExpect(status().isForbidden());
            enviar(RUBRICA, otro, rubrica(4, 9)).andExpect(status().isForbidden());
        }
        for (String otro : new String[] {VALENTINA, GUIA2, CURSO2, JEFE_CARRERA, "tutor1@demo.example"}) {
            consultar(RESULTADOS, otro).andExpect(status().isForbidden());
        }
    }

    @Test
    void respuestasIncompletasOFueraDeEscalaDan400() throws Exception {
        enviar(SUS, VALENTINA, Map.of("items", ITEMS_SUS.subList(0, 9))).andExpect(status().isBadRequest());
        List<Integer> fuera = new ArrayList<>(ITEMS_SUS);
        fuera.set(3, 6);
        enviar(SUS, VALENTINA, Map.of("items", fuera)).andExpect(status().isBadRequest());
        enviar(SUS, VALENTINA, Map.of("items", Collections.nCopies(10, 0))).andExpect(status().isBadRequest());
        enviar(RUBRICA, VALENTINA, rubrica(4, 8)).andExpect(status().isBadRequest());
        enviar(RUBRICA, VALENTINA, rubrica(5, 9)).andExpect(status().isBadRequest());
        // nada quedó guardado: todavía puede responder
        enviar(SUS, VALENTINA, Map.of("items", ITEMS_SUS)).andExpect(status().isCreated());
    }
}

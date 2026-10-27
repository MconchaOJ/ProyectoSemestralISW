package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.service.ModeloLenguaje.Solicitud;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * HU-15, criterio 2 (SUPUESTO S-72): sin planificación adjunta en la conversación, un mensaje que parece una
 * planificación pegada como texto recibe una respuesta fija que pide elegirla en "Planificación a revisar", y el
 * modelo NO se llama. Un mensaje corto sigue llegando al modelo.
 */
class HU15PlanificacionPegadaIntegracionTest extends ChatbotIntegracionBase {

    static final String PLANIFICACION_PEGADA = """
            Planificación de clase: función cuadrática, 1.º medio.
            Objetivo: que los estudiantes reconozcan la función cuadrática y su gráfica, la parábola.
            Recursos: guía de trabajo, GeoGebra en el laboratorio y papel milimetrado.
            Inicio (15 minutos): activación de conocimientos previos sobre la función lineal con una pregunta abierta.
            Desarrollo (50 minutos): actividad en parejas para graficar tres funciones y comparar sus vértices.
            Cierre (15 minutos): plenario y ticket de salida.
            Evaluación: ticket de salida con dos preguntas. DUA: la guía se entrega también en formato digital.""";

    @Test
    void unaPlanificacionPegadaSinAdjuntarRecibeLaRespuestaFijaYElModeloNoSeLlama() throws Exception {
        escribir(CHAT_VALENTINA, VALENTINA, PLANIFICACION_PEGADA).andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensajeEstudiante.contenido").value(PLANIFICACION_PEGADA.strip()))
                .andExpect(jsonPath("$.respuesta.autor").value("CHATBOT"))
                .andExpect(jsonPath("$.respuesta.contenido").value(Matchers.containsString("«Planificación a revisar»")))
                .andExpect(jsonPath("$.respuesta.contenido").value(Matchers.containsString("pegaste tu planificación como texto")));
        verifyNoInteractions(modelo);

        // los dos mensajes quedan en la conversación (el guía ve el intento)
        consultar(CHAT_VALENTINA, VALENTINA).andExpect(jsonPath("$.mensajes.length()").value(2));
    }

    @Test
    void unMensajeCortoSinPlanificacionSigueLlamandoAlModelo() throws Exception {
        // nombra partes de una planificación, pero es una pregunta corta
        escribir(CHAT_VALENTINA, VALENTINA, "¿Cómo reparto el tiempo entre inicio, desarrollo y cierre?")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.contenido").value(RESPUESTA));
        assertThat(solicitudes(1)).hasSize(1);
    }

    @Test
    void conUnaPlanificacionAdjuntaElTextoLargoSiLlegaAlModelo() throws Exception {
        escribir(CHAT_VALENTINA, VALENTINA, PLANIFICACION_PEGADA, 8).andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.contenido").value(RESPUESTA));
        assertThat(solicitudes(1).get(0).mensaje()).contains("PLANIFICACIÓN EN REVISIÓN");
    }

    @Test
    void laPlanificacionPegadaNoSeReenviaAlModeloEnLosMensajesSiguientes() throws Exception {
        escribir(CHAT_VALENTINA, VALENTINA, PLANIFICACION_PEGADA).andExpect(status().isCreated());
        escribir(CHAT_VALENTINA, VALENTINA, "Listo, ahora la elegí.", 8).andExpect(status().isCreated());

        Solicitud s = solicitudes(1).get(0);
        assertThat(s.historia()).isEmpty();   // ni el texto pegado ni la respuesta fija gastan cuota
        assertThat(s.mensaje()).contains("PLANIFICACIÓN EN REVISIÓN").endsWith("Listo, ahora la elegí.");
        consultar(CHAT_VALENTINA, VALENTINA).andExpect(jsonPath("$.mensajes.length()").value(4));
    }
}

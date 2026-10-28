package cl.ubb.pem.practicas;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * HU-16: el profesor guía ve el chat de sus estudiantes asignados, y de nadie más (SUPUESTO S-60). La parte de
 * planificaciones y evaluaciones ya estaba cubierta por HU-04 y HU-05.
 */
class HU16ChatGuiaIntegracionTest extends ChatbotIntegracionBase {

    @Test
    void elGuiaAsignadoLeeLaConversacionPeroNoPuedeEscribir() throws Exception {
        escribir(CHAT_VALENTINA, VALENTINA, "Revisa mi planificación, por favor.", 8).andExpect(status().isCreated());

        consultar(CHAT_VALENTINA, GUIA2).andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiante").value("Valentina Pérez Cortés"))
                .andExpect(jsonPath("$.puedeEscribir").value(false))
                .andExpect(jsonPath("$.mensajes.length()").value(2))
                .andExpect(jsonPath("$.mensajes[0].contenido").value("Revisa mi planificación, por favor."))
                .andExpect(jsonPath("$.mensajes[0].planificacionTitulo").value("Introducción a las funciones cuadráticas"))
                .andExpect(jsonPath("$.mensajes[1].contenido").value(RESPUESTA));
    }

    @Test
    void elEstudianteSabeQueSuGuiaPuedeVerElChat() throws Exception {   // criterio 2
        consultar(CHAT_VALENTINA, VALENTINA).andExpect(status().isOk())
                .andExpect(jsonPath("$.profesorGuia.nombreCompleto").value("Lorena Vidal Pizarro"));
    }

    @Test
    void otroGuiaYLosRolesSinRelacionConElChatReciben403() throws Exception {
        consultar(CHAT_VALENTINA, GUIA1).andExpect(status().isForbidden());          // guía de otros estudiantes
        consultar(CHAT_VALENTINA, CURSO2).andExpect(status().isForbidden());         // su profesor del curso (S-60)
        consultar(CHAT_VALENTINA, DIEGO).andExpect(status().isForbidden());          // otro estudiante
        consultar(CHAT_VALENTINA, COORDINADOR).andExpect(status().isForbidden());    // S-60, privacidad abierta (S-38)
        // el jefe de carrera sí lo lee desde HU-20 (HU20ChatJefeCarreraIntegracionTest)
        consultar("/api/inscripciones/17/chatbot", "tutor1@demo.example").andExpect(status().isForbidden());
    }
}

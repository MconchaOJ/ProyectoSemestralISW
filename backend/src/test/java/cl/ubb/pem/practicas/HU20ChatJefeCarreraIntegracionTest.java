package cl.ubb.pem.practicas;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * HU-20: el jefe de carrera lee, en solo lectura, las conversaciones con el chatbot de cualquier estudiante de
 * Práctica Pedagógica o Profesional, aunque no sea su guía (SUPUESTO S-66). Los roles sin acceso siguen en 403 (S-60).
 * Seed: el jefe (Gonzalo Riquelme) es guía solo de la inscripción 40; Valentina (inscripción 11) tiene de guía a Lorena.
 */
class HU20ChatJefeCarreraIntegracionTest extends ChatbotIntegracionBase {

    @Test
    void elJefeDeCarreraLeeElChatDeUnEstudianteDelQueNoEsGuia() throws Exception {
        escribir(CHAT_VALENTINA, VALENTINA, "Revisa mi planificación, por favor.", 8).andExpect(status().isCreated());

        consultar(CHAT_VALENTINA, JEFE_CARRERA).andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiante").value("Valentina Pérez Cortés"))
                .andExpect(jsonPath("$.puedeEscribir").value(false))
                .andExpect(jsonPath("$.profesorGuia.nombreCompleto").value("Lorena Vidal Pizarro"))
                .andExpect(jsonPath("$.mensajes.length()").value(2))
                .andExpect(jsonPath("$.mensajes[0].planificacionTitulo").value("Introducción a las funciones cuadráticas"))
                .andExpect(jsonPath("$.mensajes[1].contenido").value(RESPUESTA));
    }

    @Test
    void elJefeSoloLeeNoEscribe() throws Exception {
        escribir(CHAT_VALENTINA, JEFE_CARRERA, "Hola").andExpect(status().isForbidden());
        consultar(CHAT_VALENTINA, JEFE_CARRERA).andExpect(jsonPath("$.mensajes.length()").value(0));
    }

    @Test
    void soloEnLasAsignaturasConChatbot() throws Exception {
        consultar("/api/inscripciones/3/chatbot", JEFE_CARRERA).andExpect(status().isForbidden());   // Trabajo Colaborativo
        consultar("/api/inscripciones/17/chatbot", JEFE_CARRERA).andExpect(status().isOk());         // Práctica Profesional
    }

    @Test
    void losRolesSinAccesoSiguenEn403() throws Exception {
        consultar(CHAT_VALENTINA, COORDINADOR).andExpect(status().isForbidden());
        consultar(CHAT_VALENTINA, CURSO2).andExpect(status().isForbidden());
        consultar(CHAT_VALENTINA, GUIA1).andExpect(status().isForbidden());
        consultar(CHAT_VALENTINA, DIEGO).andExpect(status().isForbidden());
        consultar("/api/inscripciones/17/chatbot", "tutor1@demo.example").andExpect(status().isForbidden());
    }
}

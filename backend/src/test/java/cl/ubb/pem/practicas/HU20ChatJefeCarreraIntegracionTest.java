package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.repository.LecturaChatRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HU-20: el jefe de carrera lee, en solo lectura, las conversaciones con el chatbot de cualquier estudiante de
 * Práctica Pedagógica o Profesional, aunque no sea su guía (SUPUESTO S-66). Los roles sin acceso siguen en 403 (S-60).
 * Criterios 3 y 4: el estudiante ve que el jefe puede leer y cuándo lo hizo por última vez, y cada lectura del jefe
 * queda en lectura_chat con su rol (las del guía y las del estudiante no). Desde el 2026-11-22 el coordinador también
 * lee, con el mismo registro (rol COORDINADOR; migración 018).
 * Seed: el jefe (Gonzalo Riquelme) es guía solo de la inscripción 40; Valentina (inscripción 11) tiene de guía a Lorena.
 */
class HU20ChatJefeCarreraIntegracionTest extends ChatbotIntegracionBase {

    @Autowired
    private LecturaChatRepository lecturas;

    @Test
    void cadaLecturaDelJefeQuedaEnLaBaseYElEstudianteLaVe() throws Exception {
        // antes de que lea: el aviso está, pero sin fecha
        consultar(CHAT_VALENTINA, VALENTINA).andExpect(status().isOk())
                .andExpect(jsonPath("$.avisoLectores").value(Matchers.containsString("El jefe de carrera y el coordinador también pueden leer")))
                .andExpect(jsonPath("$.ultimaLecturaJefeCarrera").doesNotExist())
                .andExpect(jsonPath("$.ultimaLecturaCoordinador").doesNotExist());
        assertThat(lecturas.countByInscripcionId(11L)).isZero();

        consultar(CHAT_VALENTINA, JEFE_CARRERA).andExpect(status().isOk());
        consultar(CHAT_VALENTINA, JEFE_CARRERA).andExpect(status().isOk());
        assertThat(lecturas.countByInscripcionId(11L)).isEqualTo(2);
        var ultima = lecturas.findFirstByInscripcionIdAndRolOrderByFechaDescIdDesc(11L, "JEFE_CARRERA").orElseThrow();
        assertThat(ultima.getUsuario().getEmail()).isEqualTo(JEFE_CARRERA);

        consultar(CHAT_VALENTINA, VALENTINA).andExpect(jsonPath("$.ultimaLecturaJefeCarrera").exists())
                .andExpect(jsonPath("$.ultimaLecturaCoordinador").doesNotExist());
    }

    @Test
    void elCoordinadorLeeConElMismoRegistroYElEstudianteLoVe() throws Exception {   // 2026-11-22, decisión del usuario
        escribir(CHAT_VALENTINA, VALENTINA, "Revisa mi planificación, por favor.", 8).andExpect(status().isCreated());
        consultar(CHAT_VALENTINA, COORDINADOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeEscribir").value(false))
                .andExpect(jsonPath("$.mensajes.length()").value(2));
        assertThat(lecturas.countByInscripcionIdAndRol(11L, "COORDINADOR")).isEqualTo(1);
        assertThat(lecturas.countByInscripcionIdAndRol(11L, "JEFE_CARRERA")).isZero();
        var ultima = lecturas.findFirstByInscripcionIdAndRolOrderByFechaDescIdDesc(11L, "COORDINADOR").orElseThrow();
        assertThat(ultima.getUsuario().getEmail()).isEqualTo(COORDINADOR);

        consultar(CHAT_VALENTINA, VALENTINA).andExpect(jsonPath("$.ultimaLecturaCoordinador").exists())
                .andExpect(jsonPath("$.ultimaLecturaJefeCarrera").doesNotExist());
        // lee, pero no escribe; y solo en las prácticas con chatbot
        escribir(CHAT_VALENTINA, COORDINADOR, "Hola").andExpect(status().isForbidden());
        consultar("/api/inscripciones/3/chatbot", COORDINADOR).andExpect(status().isForbidden());
        consultar("/api/inscripciones/17/chatbot", COORDINADOR).andExpect(status().isOk());
    }

    @Test
    void lasLecturasDelGuiaYDelEstudianteNoSeRegistran() throws Exception {
        consultar(CHAT_VALENTINA, GUIA2).andExpect(status().isOk());
        consultar(CHAT_VALENTINA, VALENTINA).andExpect(status().isOk());
        assertThat(lecturas.countByInscripcionId(11L)).isZero();
        // un rol sin acceso (403) tampoco deja fila
        consultar(CHAT_VALENTINA, CURSO2).andExpect(status().isForbidden());
        assertThat(lecturas.count()).isZero();
    }

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
        consultar(CHAT_VALENTINA, CURSO2).andExpect(status().isForbidden());
        consultar(CHAT_VALENTINA, GUIA1).andExpect(status().isForbidden());
        consultar(CHAT_VALENTINA, DIEGO).andExpect(status().isForbidden());
        consultar("/api/inscripciones/17/chatbot", "tutor1@demo.example").andExpect(status().isForbidden());
    }
}

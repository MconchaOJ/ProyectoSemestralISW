package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Cuenta especial con dos roles (migración 004, confirmado por el cliente el 2026-10-11): el jefe de carrera
 * (usuario 32) tiene JEFE_CARRERA como rol principal y PROFESOR_GUIA como segundo rol, y es el guía de la
 * inscripción 40 (Isidora Bustos, Trabajo Colaborativo 2026-2). Sus permisos son la suma de los dos roles:
 * lee todo como el coordinador, pide la cartola, y solo actúa como guía donde de verdad es el guía.
 */
class RolSecundarioIntegracionTest extends IntegracionBase {

    private static Map<String, Object> evaluacion(long inscripcionId) {
        Map<String, Object> m = new HashMap<>();
        m.put("inscripcionId", inscripcionId);
        m.put("pautaId", 1);
        m.put("fechaClase", "2026-10-20");
        m.put("establecimiento", "Liceo Bicentenario Ejemplo");
        m.put("curso", "2° Medio A");
        m.put("horaInicio", "09:00:00");
        m.put("horaTermino", "10:30:00");
        m.put("objetivoClase", "Calcular porcentajes en situaciones cotidianas.");
        return m;
    }

    @Test
    void elPerfilInformaLosDosRoles() throws Exception {
        consultar("/api/auth/me", JEFE_CARRERA).andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("JEFE_CARRERA"))
                .andExpect(jsonPath("$.rolSecundario").value("PROFESOR_GUIA"));
        consultar("/api/auth/me", GUIA1).andExpect(jsonPath("$.rolSecundario").doesNotExist());
    }

    @Test
    void comoJefeDeCarreraLeeTodoComoElCoordinador() throws Exception {
        assertThat(cuerpo(consultar("/api/inscripciones", JEFE_CARRERA).andExpect(status().isOk()))).hasSize(55);
        assertThat(cuerpo(consultar("/api/planificaciones", JEFE_CARRERA).andExpect(status().isOk()))).hasSize(19);
        assertThat(cuerpo(consultar("/api/estudiantes", JEFE_CARRERA).andExpect(status().isOk()))).hasSize(20);
        consultar("/api/estudiantes/1", JEFE_CARRERA).andExpect(status().isOk());
        // historial de cualquier estudiante, también de uno que no es suyo como guía (Sebastián)
        consultar("/api/estudiantes/4/historial", JEFE_CARRERA).andExpect(status().isOk());
        // evaluaciones de todos, incluidos los borradores (mismo alcance que el coordinador)
        assertThat(cuerpo(consultar("/api/evaluaciones", JEFE_CARRERA)).size())
                .isEqualTo(cuerpo(consultar("/api/evaluaciones", COORDINADOR)).size());
        consultar("/api/evaluaciones/4", JEFE_CARRERA).andExpect(status().isOk());   // borrador de Marcos (guía 2)
        // y sigue teniendo la cartola
        consultar("/api/reportes/cartola-practicas?desde=2025-01-01&hasta=2025-12-31", JEFE_CARRERA)
                .andExpect(status().isOk());
    }

    @Test
    void comoJefeDeCarreraNoCreaNiModificaNada() throws Exception {
        enviar("/api/estudiantes", JEFE_CARRERA, Map.of("nombre", "X", "apellidos", "Y", "email", "x@demo.example",
                "password", "clave-de-prueba-1", "rut", "11111111-1", "semestreActual", 2, "anioIngreso", 2026))
                .andExpect(status().isForbidden());
        enviar("/api/inscripciones", JEFE_CARRERA, Map.of("estudianteId", 1, "asignaturaId", 4, "periodo", "2027-1",
                "profesorGuiaId", 32, "profesorCursoId", 4)).andExpect(status().isForbidden());
        reemplazar("/api/inscripciones/40/cierre", JEFE_CARRERA, Map.of("estado", "APROBADA", "notaFinal", 6.0))
                .andExpect(status().isForbidden());
        // no autoriza (no es profesor del curso) ni planifica (no es estudiante)
        enviar("/api/planificaciones/7/revision-curso", JEFE_CARRERA, Map.of("decision", "APROBADA"))
                .andExpect(status().isForbidden());
        enviar("/api/planificaciones/5/enviar", JEFE_CARRERA).andExpect(status().isForbidden());
    }

    @Test
    void comoGuiaSoloActuaEnLaInscripcionQueTieneAsignada() throws Exception {
        // en la suya (40) evalúa con la pauta: crea el borrador y es su autor
        JsonNode creada = cuerpo(enviar("/api/evaluaciones", JEFE_CARRERA, evaluacion(40)).andExpect(status().isCreated()));
        assertThat(creada.get("evaluador").get("email").asText()).isEqualTo(JEFE_CARRERA);

        // en la de otro guía (inscripción 3, guía Marcos) no puede, aunque la vea por ser jefe de carrera
        enviar("/api/evaluaciones", JEFE_CARRERA, evaluacion(3)).andExpect(status().isForbidden());
        reemplazar("/api/evaluaciones/4/respuestas", JEFE_CARRERA, Map.of("respuestas", List.of(Map.of("indicadorId", 1, "puntaje", 5))))
                .andExpect(status().isForbidden());
        // ni revisa una planificación de un estudiante que no guía (planificación 6: Diego, guía Lorena)
        enviar("/api/planificaciones/6/revision-guia", JEFE_CARRERA, Map.of("decision", "APROBADA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCoordinadorPuedeAsignarloComoGuiaPorSuSegundoRol() throws Exception {
        enviar("/api/inscripciones", COORDINADOR, Map.of("estudianteId", 3, "asignaturaId", 6, "periodo", "2027-1",
                "profesorGuiaId", 32, "profesorCursoId", 5)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.profesorGuia.id").value(32));
        // pero no como profesor del curso: ese rol no lo tiene
        enviar("/api/inscripciones", COORDINADOR, Map.of("estudianteId", 4, "asignaturaId", 6, "periodo", "2027-1",
                "profesorGuiaId", 3, "profesorCursoId", 32)).andExpect(status().isBadRequest());
    }

    @Test
    void elRestoDeLosGuiasSigueViendoSoloLoSuyo() throws Exception {
        consultar("/api/inscripciones?periodo=2026-2", GUIA1).andExpect(jsonPath("$.length()").value(1));
        consultar("/api/estudiantes", GUIA1).andExpect(status().isForbidden());
        consultar("/api/estudiantes/17/historial", GUIA1).andExpect(status().isForbidden());   // Isidora
    }
}

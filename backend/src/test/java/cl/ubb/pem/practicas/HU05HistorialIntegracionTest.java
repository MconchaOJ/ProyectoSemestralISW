package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HU-05: historial del estudiante, contra la base real (cada prueba se revierte).
 * Estudiantes del seed: 1 Camila, 2 Diego, 3 Valentina, 4 Sebastián (recorrido completo), 5 Javiera.
 */
class HU05HistorialIntegracionTest extends IntegracionBase {

    private List<String> campo(JsonNode arreglo, String nombre) {
        List<String> valores = new ArrayList<>();
        arreglo.forEach(n -> valores.add(n.get(nombre).asText()));
        return valores;
    }

    // ---------- Contenido ----------

    @Test
    void elRecorridoDeSebastianTiene6InscripcionesEnOrdenCronologico() throws Exception {
        JsonNode h = cuerpo(consultar("/api/estudiantes/4/historial", COORDINADOR).andExpect(status().isOk()));
        JsonNode insc = h.get("inscripciones");
        assertThat(insc).hasSize(6);
        assertThat(campo(insc, "periodo")).containsExactly("2023-2", "2024-1", "2024-2", "2025-1", "2025-2", "2026-2");
        assertThat(insc.get(0).get("asignatura").get("nombre").asText()).isEqualTo("Aproximación al Sistema Escolar");
        assertThat(insc.get(0).get("asignatura").get("ciclo").asText()).isEqualTo("INICIAL");
        assertThat(insc.get(4).get("asignatura").get("ciclo").asText()).isEqualTo("AVANZADO");
        assertThat(campo(insc, "estado")).containsExactly("APROBADA", "APROBADA", "APROBADA", "APROBADA", "APROBADA", "EN_CURSO");
        assertThat(insc.get(0).get("notaFinal").asDouble()).isEqualTo(5.5);
        assertThat(insc.get(5).get("notaFinal").isNull()).isTrue();
    }

    @Test
    void cadaInscripcionTraeSusEvaluacionesFinalizadasYSusPlanificacionesConSuEstado() throws Exception {
        JsonNode insc = cuerpo(consultar("/api/estudiantes/4/historial", COORDINADOR)).get("inscripciones");
        // 2024-2, Trabajo Colaborativo: 1 evaluación (id 1) y 1 planificación (id 1) AUTORIZADA
        JsonNode tc = insc.get(2);
        assertThat(tc.get("evaluaciones")).hasSize(1);
        JsonNode ev = tc.get("evaluaciones").get(0);
        assertThat(ev.get("id").asLong()).isEqualTo(1L);
        assertThat(ev.get("estado").asText()).isEqualTo("FINALIZADA");
        assertThat(ev.get("puntajeTotal").asInt()).isEqualTo(85);
        assertThat(ev.get("promedio").asDouble()).isEqualTo(4.25);
        assertThat(ev.get("pautaNombre").asText()).contains("Pauta de observación de clase");
        assertThat(tc.get("planificaciones").get(0).get("estado").asText()).isEqualTo("AUTORIZADA");
        // 2026-2, Práctica Profesional: sin evaluaciones y con una planificación EN_REVISION_GUIA
        assertThat(insc.get(5).get("evaluaciones")).isEmpty();
        assertThat(insc.get(5).get("planificaciones").get(0).get("estado").asText()).isEqualTo("EN_REVISION_GUIA");
        // las iniciales no tienen guía (no van al colegio)
        assertThat(insc.get(0).get("profesorGuia").isNull()).isTrue();
        assertThat(insc.get(2).get("profesorGuia").get("nombreCompleto").asText()).isEqualTo("Marcos Bravo Tapia");
    }

    @Test
    void elHistorialMuestraLaEvolucionDeValentinaEntreSusDosClases() throws Exception {
        JsonNode insc = cuerpo(consultar("/api/estudiantes/3/historial", COORDINADOR)).get("inscripciones");
        JsonNode evaluaciones = insc.get(2).get("evaluaciones");      // Trabajo Colaborativo 2025-2
        assertThat(evaluaciones).hasSize(2);
        assertThat(campo(evaluaciones, "fechaClase")).containsExactly("2025-10-08", "2025-10-22");
        assertThat(evaluaciones.get(0).get("promedio").asDouble()).isEqualTo(4.8);
        assertThat(evaluaciones.get(1).get("promedio").asDouble()).isEqualTo(5.9);      // el promedio sube
    }

    @Test
    void incluyeElSemestreActualYLasAsignaturasAprobadasParaElChatbot() throws Exception {
        JsonNode h = cuerpo(consultar("/api/estudiantes/3/historial", COORDINADOR));
        assertThat(h.get("estudiante").get("semestreActual").asInt()).isEqualTo(8);
        assertThat(campo(h.get("asignaturasAprobadas"), "nombre")).containsExactly("Aproximación al Sistema Escolar",
                "Ambientes de Aprendizaje", "Trabajo Colaborativo en el Aula", "Investigación en el Aula");
        // Valentina cursa Práctica Pedagógica (EN_CURSO): no cuenta como aprobada
        assertThat(h.get("inscripciones")).hasSize(5);
        assertThat(cuerpo(consultar("/api/estudiantes/4/historial", COORDINADOR)).get("asignaturasAprobadas")).hasSize(5);
        assertThat(cuerpo(consultar("/api/estudiantes/5/historial", COORDINADOR)).get("asignaturasAprobadas")).isEmpty();
    }

    @Test
    void elDetalleDeUnaEvaluacionDelHistorialTraeElPuntajeDeCadaIndicador() throws Exception {
        JsonNode ev = cuerpo(consultar("/api/evaluaciones/3", VALENTINA).andExpect(status().isOk()));
        JsonNode respuestas = ev.get("respuestas");
        assertThat(respuestas).hasSize(20);
        assertThat(respuestas.get(0).get("numero").asInt()).isEqualTo(1);
        assertThat(respuestas.get(0).get("puntaje").asInt()).isEqualTo(6);
        assertThat(respuestas.get(12).get("puntaje").asInt()).isEqualTo(7);     // indicador 13
        assertThat(respuestas.get(19).get("dimension").asText()).isEqualTo("Ambiente de aula y recursos de aprendizaje");
        assertThat(ev.get("puntajeTotal").asInt()).isEqualTo(118);
    }

    @Test
    void lasEvaluacionesEnBorradorNoAparecenPeroLasPlanificacionesNuevasSi() throws Exception {
        // el guía abre una evaluación (BORRADOR) y Camila crea una planificación (BORRADOR)
        enviar("/api/evaluaciones", GUIA1, Map.of("inscripcionId", 3, "pautaId", 1, "fechaClase", "2026-09-22",
                "establecimiento", "Liceo", "curso", "1° Medio", "horaInicio", "10:00:00", "horaTermino", "11:00:00",
                "objetivoClase", "Objetivo")).andExpect(status().isCreated());
        enviar("/api/planificaciones", CAMILA, Map.of("inscripcionId", 3, "titulo", "Nueva idea")).andExpect(status().isCreated());
        sincronizar();

        JsonNode insc3 = cuerpo(consultar("/api/estudiantes/1/historial", CAMILA)).get("inscripciones").get(2);   // Camila: 2025-2, 2026-1, 2026-2
        assertThat(insc3.get("periodo").asText()).isEqualTo("2026-2");
        assertThat(insc3.get("evaluaciones")).isEmpty();
        assertThat(campo(insc3.get("planificaciones"), "estado")).contains("BORRADOR", "AUTORIZADA", "CON_OBSERVACIONES");
        assertThat(insc3.get("planificaciones")).hasSize(4);
    }

    // ---------- Permisos ----------

    @Test
    void elEstudianteVeSoloSuPropioHistorial() throws Exception {
        consultar("/api/estudiantes/1/historial", CAMILA).andExpect(status().isOk());
        consultar("/api/estudiantes/me/historial", CAMILA).andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiante.rut").value("11111111-1"))
                .andExpect(jsonPath("$.inscripciones.length()").value(3));
        consultar("/api/estudiantes/2/historial", CAMILA).andExpect(status().isForbidden());
        consultar("/api/estudiantes/4/historial", DIEGO).andExpect(status().isForbidden());
        consultar("/api/estudiantes/me/historial", COORDINADOR).andExpect(status().isForbidden());
        consultar("/api/estudiantes/me/historial", GUIA1).andExpect(status().isForbidden());
    }

    @Test
    void elGuiaVeElHistorialDeSusEstudiantesAsignadosYDeNadieMas() throws Exception {
        // Marcos (guía 2) está asignado a Camila (1), Valentina (3) y Sebastián (4)
        for (int estudiante : List.of(1, 3, 4)) {
            consultar("/api/estudiantes/" + estudiante + "/historial", GUIA1).andExpect(status().isOk());
        }
        consultar("/api/estudiantes/2/historial", GUIA1).andExpect(status().isForbidden());   // Diego es de Lorena
        consultar("/api/estudiantes/5/historial", GUIA1).andExpect(status().isForbidden());   // Javiera no tiene guía
        // Lorena (guía 3): Diego (2), Valentina (3) y Sebastián (4)
        consultar("/api/estudiantes/2/historial", GUIA2).andExpect(status().isOk());
        consultar("/api/estudiantes/1/historial", GUIA2).andExpect(status().isForbidden());
    }

    @Test
    void elProfesorDelCursoVeElHistorialDeLosEstudiantesDeSusInscripciones() throws Exception {
        // Ricardo (curso 4): Camila, Diego, Valentina y Sebastián; Patricia (curso 5): Diego, Valentina, Sebastián y Javiera
        consultar("/api/estudiantes/1/historial", CURSO1).andExpect(status().isOk());
        consultar("/api/estudiantes/5/historial", CURSO1).andExpect(status().isForbidden());
        consultar("/api/estudiantes/5/historial", CURSO2).andExpect(status().isOk());
        consultar("/api/estudiantes/1/historial", CURSO2).andExpect(status().isForbidden());
    }

    @Test
    void elCoordinadorVeTodosYSinTokenONoExisteDaErrorClaro() throws Exception {
        for (int id = 1; id <= 5; id++) {
            consultar("/api/estudiantes/" + id + "/historial", COORDINADOR).andExpect(status().isOk());
        }
        consultar("/api/estudiantes/99/historial", COORDINADOR).andExpect(status().isNotFound());
        consultar("/api/estudiantes/4/historial", null).andExpect(status().isUnauthorized());
        consultar("/api/estudiantes/abc/historial", COORDINADOR).andExpect(status().isBadRequest());
    }
}

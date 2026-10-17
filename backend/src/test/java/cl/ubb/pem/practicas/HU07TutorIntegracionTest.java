package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HU-07 (primera parte, 2026-10-16): tutor de Práctica Profesional y pautas con escala 2 a 7, contra la base real
 * (cada prueba se revierte). Datos del seed: inscripción 17 = Sebastián (estudiante 4) en Práctica Profesional 2026-2,
 * guía Lorena (usuario 3), profesor del curso Patricia (usuario 5) y tutor Rodrigo (usuario 33). Pauta 2 = Especialidad
 * 1.ª observación del colaborador y pauta 3 = la misma del tutor (asignatura 6, 4 dimensiones A-D, 19 indicadores).
 */
class HU07TutorIntegracionTest extends IntegracionBase {

    private static final String TUTOR1 = "tutor1@demo.example";   // Rodrigo Saavedra (id 33)

    private Map<String, Object> nueva(long inscripcionId, int pautaId) {
        Map<String, Object> m = new HashMap<>();
        m.put("inscripcionId", inscripcionId);
        m.put("pautaId", pautaId);
        m.put("fechaClase", "2026-10-15");
        m.put("establecimiento", "Liceo Bicentenario Ejemplo");
        m.put("curso", "3° Medio A");
        m.put("horaInicio", "08:00:00");
        m.put("horaTermino", "09:30:00");
        m.put("objetivoClase", "Modelar con funciones exponenciales.");
        return m;
    }

    private List<Integer> indicadorIds(int pautaId) throws Exception {
        List<Integer> ids = new ArrayList<>();
        for (JsonNode dim : cuerpo(consultar("/api/pautas/" + pautaId, TUTOR1)).get("dimensiones")) {
            dim.get("indicadores").forEach(i -> ids.add(i.get("id").asInt()));
        }
        return ids;
    }

    @Test
    void lasPautasDelTutorUsanEscalaDe2a7DimensionesAaDYPuntajeMaximoCalculado() throws Exception {   // criterios 1, 2 y 4
        JsonNode p = cuerpo(consultar("/api/pautas/3", TUTOR1).andExpect(status().isOk()));
        assertThat(p.get("tipoEvaluador").asText()).isEqualTo("TUTOR");
        assertThat(p.get("ambito").asText()).isEqualTo("ESPECIALIDAD");
        assertThat(p.get("numeroObservacion").asInt()).isEqualTo(1);
        assertThat(p.get("escalaMin").asInt()).isEqualTo(2);
        assertThat(p.get("escalaMax").asInt()).isEqualTo(7);
        assertThat(p.get("totalIndicadores").asInt()).isEqualTo(19);
        assertThat(p.get("puntajeMaximo").asInt()).isEqualTo(133);   // el total de la pauta impresa
        List<String> letras = new ArrayList<>();
        p.get("dimensiones").forEach(d -> letras.add(d.get("nombre").asText().substring(0, 2)));
        assertThat(letras).containsExactly("A.", "B.", "C.", "D.");
        // la de observación de clase no lleva número de observación
        consultar("/api/pautas/1", TUTOR1).andExpect(jsonPath("$.numeroObservacion").isEmpty());
    }

    @Test
    void elTutorVeSoloLaInscripcionDondeEsTutor() throws Exception {
        assertThat(ids(cuerpo(consultar("/api/inscripciones", TUTOR1).andExpect(status().isOk())))).containsExactly(17L);
        consultar("/api/inscripciones/17", TUTOR1).andExpect(status().isOk())
                .andExpect(jsonPath("$.tutor.id").value(33))
                .andExpect(jsonPath("$.asignatura.llevaTutor").value(true));
        consultar("/api/inscripciones/3", TUTOR1).andExpect(status().isForbidden());
        // historial: el de su estudiante sí (como S-21), el de otro no
        consultar("/api/estudiantes/4/historial", TUTOR1).andExpect(status().isOk());
        consultar("/api/estudiantes/1/historial", TUTOR1).andExpect(status().isForbidden());
        // las planificaciones y evaluaciones que ve son todas de la inscripción 17
        for (JsonNode p : cuerpo(consultar("/api/planificaciones", TUTOR1).andExpect(status().isOk()))) {
            assertThat(p.get("inscripcionId").asLong()).isEqualTo(17L);
        }
        for (JsonNode e : cuerpo(consultar("/api/evaluaciones", TUTOR1).andExpect(status().isOk()))) {
            assertThat(e.get("inscripcionId").asLong()).isEqualTo(17L);
        }
        // no gestiona nada del coordinador
        consultar("/api/estudiantes", TUTOR1).andExpect(status().isForbidden());
        consultar("/api/pautas/gestion", TUTOR1).andExpect(status().isForbidden());
    }

    @Test
    void elTutorEvaluaConSuPautaDe2a7YQuedaLaPrimeraObservacion() throws Exception {
        JsonNode ev = cuerpo(enviar("/api/evaluaciones", TUTOR1, nueva(17, 3)).andExpect(status().isCreated()));
        long id = ev.get("id").asLong();
        assertThat(ev.get("pauta").get("numeroObservacion").asInt()).isEqualTo(1);   // criterio 2
        assertThat(ev.get("pauta").get("tipoEvaluador").asText()).isEqualTo("TUTOR");
        List<Integer> indicadores = indicadorIds(3);

        // 1 está fuera de la escala 2 a 7 (criterio 1)
        reemplazar("/api/evaluaciones/" + id + "/respuestas", TUTOR1,
                Map.of("respuestas", List.of(Map.of("indicadorId", indicadores.get(0), "puntaje", 1))))
                .andExpect(status().isBadRequest());

        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < indicadores.size(); i++) {
            items.add(Map.of("indicadorId", indicadores.get(i), "puntaje", i < 10 ? 7 : 6));
        }
        reemplazar("/api/evaluaciones/" + id + "/respuestas", TUTOR1, Map.of("respuestas", items)).andExpect(status().isOk());
        JsonNode fin = cuerpo(enviar("/api/evaluaciones/" + id + "/finalizar", TUTOR1).andExpect(status().isOk()));
        assertThat(fin.get("puntajeTotal").asInt()).isEqualTo(10 * 7 + 9 * 6);
        assertThat(fin.get("puntajeMaximo").asInt()).isEqualTo(133);

        // la nota de voz también es del evaluador, aunque sea tutor
        mvc.perform(multipart("/api/evaluaciones/" + id + "/nota-voz", TUTOR1)
                        .file(archivo("audio", "nota.webm", "audio/webm", new byte[] {26, 69, -33, -93, 1, 2, 3}))
                        .param("transcripcion", "Buena transposición didáctica.")
                        .param("duracionSegundos", "12"))
                .andExpect(status().isCreated());
        // el estudiante y el guía de la inscripción la ven finalizada
        consultar("/api/evaluaciones/" + id, SEBASTIAN).andExpect(status().isOk());
        consultar("/api/evaluaciones/" + id, GUIA2).andExpect(status().isOk());
        // y el guía no la modifica
        enviar("/api/evaluaciones/" + id + "/finalizar", GUIA2).andExpect(status().isForbidden());
    }

    @Test
    void elTutorSoloEvaluaConPautasDeTutorYElGuiaSoloConLasSuyas() throws Exception {   // criterio 3
        enviar("/api/evaluaciones", TUTOR1, nueva(17, 2)).andExpect(status().isBadRequest());   // pauta del colaborador
        enviar("/api/evaluaciones", GUIA2, nueva(17, 3)).andExpect(status().isBadRequest());    // pauta del tutor
        enviar("/api/evaluaciones", GUIA2, nueva(17, 2)).andExpect(status().isCreated());
        // donde no es tutor, no evalúa
        enviar("/api/evaluaciones", TUTOR1, nueva(3, 1)).andExpect(status().isForbidden());
        // los demás roles siguen sin poder evaluar
        for (String email : List.of(CURSO2, SEBASTIAN, COORDINADOR, JEFE_CARRERA)) {
            enviar("/api/evaluaciones", email, nueva(17, 3)).andExpect(status().isForbidden());
        }
    }

    @Test
    void elCoordinadorAsignaYQuitaElTutorSoloEnPracticaProfesional() throws Exception {
        reemplazar("/api/inscripciones/17/tutor", TUTOR1, Map.of("tutorId", 33)).andExpect(status().isForbidden());
        reemplazar("/api/inscripciones/17/tutor", GUIA2, Map.of("tutorId", 33)).andExpect(status().isForbidden());

        Map<String, Object> sinTutor = new HashMap<>();
        sinTutor.put("tutorId", null);
        reemplazar("/api/inscripciones/17/tutor", COORDINADOR, sinTutor).andExpect(status().isOk())
                .andExpect(jsonPath("$.tutor").isEmpty());
        consultar("/api/inscripciones/17", TUTOR1).andExpect(status().isForbidden());
        enviar("/api/evaluaciones", TUTOR1, nueva(17, 3)).andExpect(status().isForbidden());

        reemplazar("/api/inscripciones/17/tutor", COORDINADOR, Map.of("tutorId", 33)).andExpect(status().isOk())
                .andExpect(jsonPath("$.tutor.id").value(33));
        // solo en asignaturas con tutor, con rol TUTOR, persona distinta del guía y con la inscripción en curso
        reemplazar("/api/inscripciones/3/tutor", COORDINADOR, Map.of("tutorId", 33)).andExpect(status().isBadRequest());
        reemplazar("/api/inscripciones/17/tutor", COORDINADOR, Map.of("tutorId", 3)).andExpect(status().isBadRequest());
        reemplazar("/api/inscripciones/16/tutor", COORDINADOR, Map.of("tutorId", 33)).andExpect(status().isConflict());
        reemplazar("/api/inscripciones/17/tutor", COORDINADOR, Map.of("tutorId", 999)).andExpect(status().isNotFound());
    }

    @Test
    void alInscribirSePuedeIndicarElTutor() throws Exception {
        Map<String, Object> m = new HashMap<>(Map.of("estudianteId", 1, "asignaturaId", 6, "periodo", "2027-1",
                "profesorGuiaId", 2, "profesorCursoId", 4, "tutorId", 33));
        enviar("/api/inscripciones", COORDINADOR, m).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tutor.id").value(33));
        m.put("asignaturaId", 4);   // Investigación en el Aula no lleva tutor
        enviar("/api/inscripciones", COORDINADOR, m).andExpect(status().isBadRequest());
        // el coordinador puede registrar tutores y los ve entre los profesores
        assertThat(cuerpo(consultar("/api/profesores?rol=TUTOR", COORDINADOR).andExpect(status().isOk())).size()).isEqualTo(1);
    }

    @Test
    void lasPautasDeEspecialidadUOrientacionExigenElNumeroDeObservacion() throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("asignaturaId", 6);
        p.put("nombre", "Pauta de Orientación, 2.ª observación - Tutor/a");
        p.put("tipoEvaluador", "TUTOR");
        p.put("ambito", "ORIENTACION");
        p.put("escalaMin", 2);
        p.put("escalaMax", 7);
        p.put("dimensiones", List.of(Map.of("nombre", "A. Orientación", "indicadores", List.of("Acompaña al curso."))));
        enviar("/api/pautas", COORDINADOR, p).andExpect(status().isBadRequest());
        p.put("numeroObservacion", 3);
        enviar("/api/pautas", COORDINADOR, p).andExpect(status().isBadRequest());
        p.put("numeroObservacion", 2);
        enviar("/api/pautas", COORDINADOR, p).andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroObservacion").value(2))
                .andExpect(jsonPath("$.puntajeMaximo").value(7));
        p.put("ambito", "OBSERVACION_CLASE");   // la de observación de clase no lleva número
        p.put("nombre", "Otra pauta");
        enviar("/api/pautas", COORDINADOR, p).andExpect(status().isBadRequest());
    }
}

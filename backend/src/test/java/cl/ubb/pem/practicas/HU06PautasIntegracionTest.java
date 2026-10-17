package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-06: configurar pautas de evaluación, contra la base real (cada prueba se revierte).
 * Datos del seed: la pauta 1 (Trabajo Colaborativo, asignatura 3, 20 indicadores en 3 dimensiones, escala 1 a 7) ya
 * se usó en las 11 evaluaciones de demostración; la evaluación 1 está FINALIZADA (puntaje máximo 20 x 7 = 140) y la 4
 * es un BORRADOR de Marcos (guía 1) sobre la inscripción 3.
 */
class HU06PautasIntegracionTest extends IntegracionBase {

    private ResultActions borrar(String url, String email) throws Exception {
        return mvc.perform(delete(url).header("Authorization", "Bearer " + token(email)));
    }

    /** Pauta de Especialidad con tutor (como las de HU-07): escala 2 a 7, dimensiones A y B. */
    private static Map<String, Object> pautaTutor(String nombre) {
        Map<String, Object> m = new HashMap<>();
        m.put("asignaturaId", 6);
        m.put("nombre", nombre);
        m.put("tipoEvaluador", "TUTOR");
        m.put("ambito", "ESPECIALIDAD");
        m.put("numeroObservacion", 1);
        m.put("escalaMin", 2);
        m.put("escalaMax", 7);
        m.put("dimensiones", List.of(
                Map.of("nombre", "A. Planificación", "indicadores", List.of("Formula objetivos claros.", "Secuencia las actividades.")),
                Map.of("nombre", "B. Gestión del aula", "indicadores", List.of("Gestiona el tiempo de la clase."))));
        return m;
    }

    /** La pauta 1 tal como está, con un indicador más en la última dimensión (un cambio real). */
    private Map<String, Object> pautaUnoConUnIndicadorMas() throws Exception {
        JsonNode p = cuerpo(consultar("/api/pautas/1", COORDINADOR));
        List<Map<String, Object>> dims = new ArrayList<>();
        for (JsonNode d : p.get("dimensiones")) {
            List<String> textos = new ArrayList<>();
            d.get("indicadores").forEach(i -> textos.add(i.get("descripcion").asText()));
            dims.add(new HashMap<>(Map.of("nombre", d.get("nombre").asText(), "indicadores", textos)));
        }
        @SuppressWarnings("unchecked")
        List<String> ultima = (List<String>) dims.get(dims.size() - 1).get("indicadores");
        ultima.add("El profesor/a usa la retroalimentación formativa durante la clase.");
        Map<String, Object> m = new HashMap<>();
        m.put("asignaturaId", 3);
        m.put("nombre", p.get("nombre").asText());
        m.put("tipoEvaluador", "COLABORADOR");
        m.put("ambito", "OBSERVACION_CLASE");
        m.put("escalaMin", 1);
        m.put("escalaMax", 7);
        m.put("dimensiones", dims);
        return m;
    }

    @Test
    void soloElCoordinadorGestionaLasPautas() throws Exception {
        for (String email : List.of(GUIA1, CURSO1, CAMILA, JEFE_CARRERA)) {
            enviar("/api/pautas", email, pautaTutor("X")).andExpect(status().isForbidden());
            reemplazar("/api/pautas/1", email, pautaUnoConUnIndicadorMas()).andExpect(status().isForbidden());
            reemplazar("/api/pautas/1/activa", email, Map.of("activa", false)).andExpect(status().isForbidden());
            borrar("/api/pautas/1", email).andExpect(status().isForbidden());
            consultar("/api/pautas/gestion", email).andExpect(status().isForbidden());
            // leer la pauta sigue abierto a todos (S-27)
            consultar("/api/pautas/1", email).andExpect(status().isOk());
        }
    }

    @Test
    void elCoordinadorCreaUnaPautaConSusDimensionesEIndicadores() throws Exception {
        JsonNode p = cuerpo(enviar("/api/pautas", COORDINADOR, pautaTutor("Pauta de Especialidad - Tutor"))
                .andExpect(status().isCreated()));
        assertThat(p.get("version").asInt()).isEqualTo(1);
        assertThat(p.get("versionAnteriorId").isNull()).isTrue();
        assertThat(p.get("activa").asBoolean()).isTrue();
        assertThat(p.get("escalaMin").asInt()).isEqualTo(2);
        assertThat(p.get("tipoEvaluador").asText()).isEqualTo("TUTOR");
        assertThat(p.get("ambito").asText()).isEqualTo("ESPECIALIDAD");
        assertThat(p.get("totalIndicadores").asInt()).isEqualTo(3);
        // dimensiones en orden y los indicadores numerados en toda la pauta (como la pauta impresa)
        assertThat(p.get("dimensiones").findValuesAsText("nombre")).containsExactly("A. Planificación", "B. Gestión del aula");
        assertThat(p.get("dimensiones").get(1).get("indicadores").get(0).get("numero").asInt()).isEqualTo(3);
        // cualquier rol la ve entre las pautas activas de la asignatura (más las 2 de Especialidad de HU-07)
        consultar("/api/pautas?asignaturaId=6", CAMILA).andExpect(jsonPath("$.length()").value(3));
        JsonNode gestion = cuerpo(consultar("/api/pautas/gestion?asignaturaId=6", COORDINADOR).andExpect(status().isOk()));
        assertThat(gestion.get(0).get("vigente").asBoolean()).isTrue();
        assertThat(gestion.get(0).get("evaluaciones").asLong()).isZero();
        assertThat(gestion.get(0).get("puntajeMaximo").asInt()).isEqualTo(21);
    }

    @Test
    void unaPautaInvalidaSeRechaza() throws Exception {
        Map<String, Object> escala = pautaTutor("Escala al revés");
        escala.put("escalaMax", 2);
        enviar("/api/pautas", COORDINADOR, escala).andExpect(status().isBadRequest());

        Map<String, Object> sinDimensiones = pautaTutor("Sin dimensiones");
        sinDimensiones.put("dimensiones", List.of());
        enviar("/api/pautas", COORDINADOR, sinDimensiones).andExpect(status().isBadRequest());

        Map<String, Object> dimensionVacia = pautaTutor("Dimensión vacía");
        dimensionVacia.put("dimensiones", List.of(Map.of("nombre", "A", "indicadores", List.of())));
        enviar("/api/pautas", COORDINADOR, dimensionVacia).andExpect(status().isBadRequest());

        Map<String, Object> repetidas = pautaTutor("Dimensiones repetidas");
        repetidas.put("dimensiones", List.of(Map.of("nombre", "A", "indicadores", List.of("x")),
                Map.of("nombre", "a", "indicadores", List.of("y"))));
        enviar("/api/pautas", COORDINADOR, repetidas).andExpect(status().isBadRequest());

        Map<String, Object> tipo = pautaTutor("Tipo inventado");
        tipo.put("tipoEvaluador", "DIRECTOR");
        enviar("/api/pautas", COORDINADOR, tipo).andExpect(status().isBadRequest());

        Map<String, Object> asignatura = pautaTutor("Asignatura inexistente");
        asignatura.put("asignaturaId", 99);
        enviar("/api/pautas", COORDINADOR, asignatura).andExpect(status().isNotFound());

        // mismo nombre que otra pauta de la misma asignatura (sin distinguir mayúsculas): 409
        Map<String, Object> repetida = pautaUnoConUnIndicadorMas();
        repetida.put("nombre", repetida.get("nombre").toString().toUpperCase());
        enviar("/api/pautas", COORDINADOR, repetida).andExpect(status().isConflict());
    }

    @Test
    void unaPautaSinUsoSeEditaEnElMismoLugar() throws Exception {
        long id = cuerpo(enviar("/api/pautas", COORDINADOR, pautaTutor("Pauta de Orientación - Tutor"))).get("id").asLong();
        Map<String, Object> cambio = pautaTutor("Pauta de Orientación - Tutor (corregida)");
        cambio.put("dimensiones", List.of(Map.of("nombre", "A. Única", "indicadores", List.of("Uno.", "Dos.", "Tres.", "Cuatro."))));
        JsonNode p = cuerpo(reemplazar("/api/pautas/" + id, COORDINADOR, cambio).andExpect(status().isOk()));
        assertThat(p.get("id").asLong()).isEqualTo(id);
        assertThat(p.get("version").asInt()).isEqualTo(1);
        assertThat(p.get("nombre").asText()).isEqualTo("Pauta de Orientación - Tutor (corregida)");
        assertThat(p.get("totalIndicadores").asInt()).isEqualTo(4);
        assertThat(p.get("dimensiones")).hasSize(1);
        // la asignatura no se cambia
        cambio.put("asignaturaId", 5);
        reemplazar("/api/pautas/" + id, COORDINADOR, cambio).andExpect(status().isBadRequest());
    }

    @Test
    void editarUnaPautaUsadaCreaUnaVersionNuevaYLasEvaluacionesConservanLaSuya() throws Exception {
        JsonNode antes = cuerpo(consultar("/api/evaluaciones/1", COORDINADOR));
        assertThat(antes.get("puntajeMaximo").asInt()).isEqualTo(140);

        JsonNode v2 = cuerpo(reemplazar("/api/pautas/1", COORDINADOR, pautaUnoConUnIndicadorMas()).andExpect(status().isOk()));
        long idV2 = v2.get("id").asLong();
        assertThat(idV2).isNotEqualTo(1L);
        assertThat(v2.get("version").asInt()).isEqualTo(2);
        assertThat(v2.get("versionAnteriorId").asInt()).isEqualTo(1);
        assertThat(v2.get("totalIndicadores").asInt()).isEqualTo(21);
        assertThat(v2.get("activa").asBoolean()).isTrue();

        // la versión 1 queda intacta e inactiva; la evaluación finalizada no cambia su puntaje máximo
        JsonNode v1 = cuerpo(consultar("/api/pautas/1", COORDINADOR));
        assertThat(v1.get("activa").asBoolean()).isFalse();
        assertThat(v1.get("totalIndicadores").asInt()).isEqualTo(20);
        JsonNode despues = cuerpo(consultar("/api/evaluaciones/1", COORDINADOR));
        assertThat(despues.get("pauta").get("id").asInt()).isEqualTo(1);
        assertThat(despues.get("puntajeMaximo").asInt()).isEqualTo(140);
        assertThat(despues.get("puntajeTotal")).isEqualTo(antes.get("puntajeTotal"));
        assertThat(despues.get("respuestas")).hasSize(20);

        // a los guías se les ofrece solo la versión vigente
        JsonNode activas = cuerpo(consultar("/api/pautas?asignaturaId=3", GUIA1));
        assertThat(ids(activas)).containsExactly(idV2);

        // el borrador 4 (de Marcos, con la versión 1) se sigue completando con su propia versión
        long indicadorV1 = cuerpo(consultar("/api/pautas/1", GUIA1)).get("dimensiones").get(0).get("indicadores").get(0).get("id").asLong();
        reemplazar("/api/evaluaciones/4/respuestas", GUIA1, Map.of("respuestas", List.of(Map.of("indicadorId", indicadorV1, "puntaje", 5))))
                .andExpect(status().isOk());

        // la versión 1 ya no se edita, ni se activa, ni se borra
        reemplazar("/api/pautas/1", COORDINADOR, pautaUnoConUnIndicadorMas()).andExpect(status().isConflict());
        reemplazar("/api/pautas/1/activa", COORDINADOR, Map.of("activa", true)).andExpect(status().isConflict());
        borrar("/api/pautas/1", COORDINADOR).andExpect(status().isConflict());

        JsonNode gestion = cuerpo(consultar("/api/pautas/gestion?asignaturaId=3", COORDINADOR));
        assertThat(gestion.findValuesAsText("version")).containsExactly("1", "2");
        assertThat(gestion.findValuesAsText("vigente")).containsExactly("false", "true");
        assertThat(gestion.get(0).get("evaluaciones").asLong()).isEqualTo(11);
    }

    @Test
    void guardarSinCambiosNoCreaUnaVersion() throws Exception {
        Map<String, Object> igual = pautaUnoConUnIndicadorMas();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dims = (List<Map<String, Object>>) igual.get("dimensiones");
        @SuppressWarnings("unchecked")
        List<String> ultima = (List<String>) dims.get(dims.size() - 1).get("indicadores");
        ultima.remove(ultima.size() - 1);   // vuelve a quedar exactamente como está
        JsonNode p = cuerpo(reemplazar("/api/pautas/1", COORDINADOR, igual).andExpect(status().isOk()));
        assertThat(p.get("id").asInt()).isEqualTo(1);
        assertThat(p.get("version").asInt()).isEqualTo(1);
        assertThat(p.get("activa").asBoolean()).isTrue();
    }

    @Test
    void unaPautaEnUsoNoSeBorraYUnaSinUsoSi() throws Exception {
        borrar("/api/pautas/1", COORDINADOR).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("en uso")));
        long id = cuerpo(enviar("/api/pautas", COORDINADOR, pautaTutor("Pauta para borrar"))).get("id").asLong();
        borrar("/api/pautas/" + id, COORDINADOR).andExpect(status().isNoContent());
        consultar("/api/pautas/" + id, COORDINADOR).andExpect(status().isNotFound());
    }

    @Test
    void borrarUnaVersionNuevaSinUsoDevuelveLaAnteriorComoVigente() throws Exception {
        long idV2 = cuerpo(reemplazar("/api/pautas/1", COORDINADOR, pautaUnoConUnIndicadorMas())).get("id").asLong();
        borrar("/api/pautas/" + idV2, COORDINADOR).andExpect(status().isNoContent());
        JsonNode v1 = cuerpo(consultar("/api/pautas/1", COORDINADOR));
        assertThat(v1.get("activa").asBoolean()).isTrue();
        assertThat(ids(cuerpo(consultar("/api/pautas?asignaturaId=3", GUIA1)))).containsExactly(1L);
    }

    @Test
    void unaPautaDesactivadaNoSeOfreceParaEvaluar() throws Exception {
        reemplazar("/api/pautas/1/activa", COORDINADOR, Map.of("activa", false)).andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
        consultar("/api/pautas?asignaturaId=3", GUIA1).andExpect(jsonPath("$.length()").value(0));
        Map<String, Object> nueva = new HashMap<>(Map.of("inscripcionId", 3, "pautaId", 1, "fechaClase", "2026-10-20",
                "establecimiento", "Liceo", "curso", "1° Medio B", "horaInicio", "10:15:00", "horaTermino", "11:45:00",
                "objetivoClase", "Objetivo."));
        enviar("/api/evaluaciones", GUIA1, nueva).andExpect(status().isBadRequest());
        reemplazar("/api/pautas/1/activa", COORDINADOR, Map.of("activa", true)).andExpect(status().isOk());
        enviar("/api/evaluaciones", GUIA1, nueva).andExpect(status().isCreated());
    }
}

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
 * HU-03: pauta digital y evaluaciones, contra la base real (cada prueba se revierte).
 * Datos del seed: inscripción 3 = Camila en Trabajo Colaborativo 2026-2 con guía Marcos (usuario 2) y planificación 3
 * AUTORIZADA; inscripción 6 = Diego con guía Lorena (usuario 3).
 */
class HU03EvaluacionIntegracionTest extends IntegracionBase {

    private Map<String, Object> nueva(long inscripcionId, Integer pautaId, Long planificacionId) {
        Map<String, Object> m = new HashMap<>();
        m.put("inscripcionId", inscripcionId);
        m.put("pautaId", pautaId);
        m.put("planificacionId", planificacionId);
        m.put("fechaClase", "2026-09-22");
        m.put("establecimiento", "Liceo Bicentenario Ejemplo");
        m.put("curso", "1° Medio B");
        m.put("horaInicio", "10:15:00");
        m.put("horaTermino", "11:45:00");
        m.put("objetivoClase", "Resolver ecuaciones lineales.");
        m.put("observacionesGenerales", "Fortalezas: buen clima de aula.");
        return m;
    }

    /** Ids de los 20 indicadores de la pauta 1, en el orden de la pauta. */
    private List<Integer> indicadorIds() throws Exception {
        List<Integer> ids = new ArrayList<>();
        for (JsonNode dim : cuerpo(consultar("/api/pautas/1", GUIA1)).get("dimensiones")) {
            for (JsonNode ind : dim.get("indicadores")) {
                ids.add(ind.get("id").asInt());
            }
        }
        return ids;
    }

    private Map<String, Object> respuestasTodas(int puntaje) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int id : indicadorIds()) {
            items.add(Map.of("indicadorId", id, "puntaje", puntaje));
        }
        return Map.of("respuestas", items);
    }

    private long crearBorrador() throws Exception {
        return cuerpo(enviar("/api/evaluaciones", GUIA1, nueva(3, 1, 3L)).andExpect(status().isCreated())).get("id").asLong();
    }

    // ---------- Pauta ----------

    @Test
    void laPautaTraeSus3DimensionesYSus20IndicadoresEnOrden() throws Exception {
        JsonNode lista = cuerpo(consultar("/api/pautas?asignaturaId=3", GUIA1).andExpect(status().isOk()));
        assertThat(lista).hasSize(1);
        JsonNode pauta = lista.get(0);
        assertThat(pauta.get("escalaMin").asInt()).isEqualTo(1);
        assertThat(pauta.get("escalaMax").asInt()).isEqualTo(7);
        assertThat(pauta.get("totalIndicadores").asInt()).isEqualTo(20);
        assertThat(pauta.get("dimensiones")).hasSize(3);
        List<Integer> numeros = new ArrayList<>();
        pauta.get("dimensiones").forEach(d -> d.get("indicadores").forEach(i -> numeros.add(i.get("numero").asInt())));
        assertThat(numeros).containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 20).boxed().toList());
        consultar("/api/pautas?asignaturaId=1", GUIA1).andExpect(jsonPath("$.length()").value(0));
        consultar("/api/pautas/1", null).andExpect(status().isUnauthorized());
    }

    // ---------- Flujo completo ----------

    @Test
    void flujoCompletoCrearPuntuarFinalizarYLaEvaluacionQuedaCongelada() throws Exception {
        long id = crearBorrador();
        consultar("/api/evaluaciones/" + id, GUIA1).andExpect(jsonPath("$.estado").value("BORRADOR"));

        enviar("/api/evaluaciones/" + id + "/finalizar", GUIA1).andExpect(status().isBadRequest());   // faltan los 20

        reemplazar("/api/evaluaciones/" + id + "/respuestas", GUIA1, respuestasTodas(5))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicadoresRespondidos").value(20));
        sincronizar();   // el trigger de la base valida cada puntaje

        JsonNode fin = cuerpo(enviar("/api/evaluaciones/" + id + "/finalizar", GUIA1).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA")));
        assertThat(fin.get("puntajeTotal").asInt()).isEqualTo(100);      // 20 indicadores x 5
        assertThat(fin.get("puntajeMaximo").asInt()).isEqualTo(140);     // 20 x 7
        assertThat(fin.get("promedio").asDouble()).isEqualTo(5.0);
        sincronizar();   // el CHECK ck_evaluacion_finalizada exige total, promedio y fecha

        // congelada: ni puntajes ni cabecera ni volver a finalizar
        reemplazar("/api/evaluaciones/" + id + "/respuestas", GUIA1, respuestasTodas(6)).andExpect(status().isConflict());
        enviar("/api/evaluaciones/" + id + "/finalizar", GUIA1).andExpect(status().isConflict());
        reemplazar("/api/evaluaciones/" + id, GUIA1, Map.of("fechaClase", "2026-09-22", "establecimiento", "X", "curso", "Y",
                "horaInicio", "10:00:00", "horaTermino", "11:00:00", "objetivoClase", "Z")).andExpect(status().isConflict());

        // el estudiante evaluado y su profesor del curso ya pueden verla, con el puntaje de cada indicador
        JsonNode vista = cuerpo(consultar("/api/evaluaciones/" + id, CAMILA).andExpect(status().isOk()));
        assertThat(vista.get("respuestas")).hasSize(20);
        assertThat(vista.get("respuestas").get(0).get("numero").asInt()).isEqualTo(1);
        assertThat(vista.get("planificacionId").asLong()).isEqualTo(3L);
        consultar("/api/evaluaciones/" + id, CURSO1).andExpect(status().isOk());
    }

    @Test
    void elPuntajeSeCalculaConPuntajesDistintosYElPromedioTieneDosDecimales() throws Exception {
        long id = crearBorrador();
        List<Map<String, Object>> items = new ArrayList<>();
        List<Integer> ids = indicadorIds();
        for (int i = 0; i < ids.size(); i++) {
            items.add(Map.of("indicadorId", ids.get(i), "puntaje", i < 7 ? 7 : 4));   // 7 sietes y 13 cuatros = 101
        }
        reemplazar("/api/evaluaciones/" + id + "/respuestas", GUIA1, Map.of("respuestas", items)).andExpect(status().isOk());
        JsonNode fin = cuerpo(enviar("/api/evaluaciones/" + id + "/finalizar", GUIA1).andExpect(status().isOk()));
        assertThat(fin.get("puntajeTotal").asInt()).isEqualTo(101);
        assertThat(fin.get("promedio").asDouble()).isEqualTo(5.05);     // 101 / 20
        sincronizar();
    }

    // ---------- Validaciones ----------

    @Test
    void puntajeFueraDeRangoOIndicadorAjenoDan400() throws Exception {
        long id = crearBorrador();
        String url = "/api/evaluaciones/" + id + "/respuestas";
        int primero = indicadorIds().get(0);
        reemplazar(url, GUIA1, Map.of("respuestas", List.of(Map.of("indicadorId", primero, "puntaje", 8)))).andExpect(status().isBadRequest());
        reemplazar(url, GUIA1, Map.of("respuestas", List.of(Map.of("indicadorId", primero, "puntaje", 0)))).andExpect(status().isBadRequest());
        reemplazar(url, GUIA1, Map.of("respuestas", List.of(Map.of("indicadorId", 9999, "puntaje", 5)))).andExpect(status().isBadRequest());
        reemplazar(url, GUIA1, Map.of("respuestas", List.of(Map.of("indicadorId", primero, "puntaje", 4.5)))).andExpect(status().isBadRequest());   // no entero
        reemplazar(url, GUIA1, Map.of("respuestas", List.of())).andExpect(status().isBadRequest());
        reemplazar(url, GUIA1, Map.of("respuestas", List.of(Map.of("indicadorId", primero, "puntaje", 7)))).andExpect(status().isOk());
    }

    @Test
    void laCabeceraDebeTenerLosDatosDeLaPautaEnPapel() throws Exception {
        Map<String, Object> sinObjetivo = nueva(3, 1, null);
        sinObjetivo.put("objetivoClase", " ");
        sinObjetivo.put("establecimiento", null);
        JsonNode err = cuerpo(enviar("/api/evaluaciones", GUIA1, sinObjetivo).andExpect(status().isBadRequest()));
        assertThat(err.get("errores").findValuesAsText("campo")).contains("objetivoClase", "establecimiento");

        Map<String, Object> horasInvertidas = nueva(3, 1, null);
        horasInvertidas.put("horaInicio", "12:00:00");
        enviar("/api/evaluaciones", GUIA1, horasInvertidas).andExpect(status().isBadRequest());
    }

    @Test
    void laPlanificacionVinculadaDebeSerDeLaInscripcionYEstarAutorizada() throws Exception {
        enviar("/api/evaluaciones", GUIA1, nueva(3, 1, 4L)).andExpect(status().isConflict());        // plan 4: CON_OBSERVACIONES
        enviar("/api/evaluaciones", GUIA1, nueva(3, 1, 6L)).andExpect(status().isBadRequest());      // plan 6: es de otra inscripción
        enviar("/api/evaluaciones", GUIA1, nueva(3, 1, 999L)).andExpect(status().isNotFound());
        enviar("/api/evaluaciones", GUIA1, nueva(3, 1, null)).andExpect(status().isCreated());       // el vínculo es opcional
    }

    @Test
    void laPautaDebeSerDeLaAsignaturaYLaInscripcionDebeEstarEnCurso() throws Exception {
        enviar("/api/evaluaciones", GUIA2, nueva(11, 1, null)).andExpect(status().isBadRequest());   // inscripción 11 es de Práctica Pedagógica
        enviar("/api/evaluaciones", GUIA1, nueva(9, 1, null)).andExpect(status().isConflict());      // inscripción 9 ya está APROBADA
        enviar("/api/evaluaciones", GUIA1, nueva(3, 99, null)).andExpect(status().isNotFound());
    }

    // ---------- Permisos ----------

    @Test
    void soloElGuiaAsignadoPuedeEvaluar() throws Exception {
        enviar("/api/evaluaciones", GUIA2, nueva(3, 1, 3L)).andExpect(status().isForbidden());        // otro guía
        enviar("/api/evaluaciones", GUIA1, nueva(1, 1, null)).andExpect(status().isForbidden());      // inscripción sin guía
        for (String rol : List.of(CAMILA, CURSO1, COORDINADOR)) {
            enviar("/api/evaluaciones", rol, nueva(3, 1, 3L)).andExpect(status().isForbidden());
        }
        enviar("/api/evaluaciones", null, nueva(3, 1, 3L)).andExpect(status().isUnauthorized());
    }

    @Test
    void otroGuiaNoPuedeModificarNiFinalizarLaEvaluacionAjena() throws Exception {
        long id = crearBorrador();
        reemplazar("/api/evaluaciones/" + id + "/respuestas", GUIA2, respuestasTodas(5)).andExpect(status().isForbidden());
        enviar("/api/evaluaciones/" + id + "/finalizar", GUIA2).andExpect(status().isForbidden());
        enviar("/api/evaluaciones/" + id + "/finalizar", CAMILA).andExpect(status().isForbidden());
    }

    @Test
    void unBorradorEsPrivadoDelEvaluadorYElCoordinador() throws Exception {
        long id = crearBorrador();
        consultar("/api/evaluaciones/" + id, GUIA1).andExpect(status().isOk());
        consultar("/api/evaluaciones/" + id, COORDINADOR).andExpect(status().isOk());
        consultar("/api/evaluaciones/" + id, CAMILA).andExpect(status().isForbidden());
        consultar("/api/evaluaciones/" + id, CURSO1).andExpect(status().isForbidden());
        consultar("/api/evaluaciones/" + id, DIEGO).andExpect(status().isForbidden());
        consultar("/api/evaluaciones/9999", COORDINADOR).andExpect(status().isNotFound());
        // y tampoco aparece en el listado del estudiante
        consultar("/api/evaluaciones?inscripcionId=3", CAMILA).andExpect(jsonPath("$.length()").value(0));
        consultar("/api/evaluaciones?inscripcionId=3", GUIA1).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void lasEvaluacionesFinalizadasDelSeedSeVenSegunElRol() throws Exception {
        // Seed: evaluación 1 (Sebastián, insc. 14), 2 y 3 (Valentina, insc. 9), todas de Marcos y FINALIZADAS
        assertThat(ids(cuerpo(consultar("/api/evaluaciones", GUIA1)))).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(ids(cuerpo(consultar("/api/evaluaciones", COORDINADOR)))).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(ids(cuerpo(consultar("/api/evaluaciones", VALENTINA)))).containsExactlyInAnyOrder(2L, 3L);
        assertThat(ids(cuerpo(consultar("/api/evaluaciones?estado=BORRADOR", COORDINADOR)))).isEmpty();
        consultar("/api/evaluaciones", GUIA2).andExpect(jsonPath("$.length()").value(0));
        consultar("/api/evaluaciones/1", CAMILA).andExpect(status().isForbidden());           // no es de Camila
        JsonNode e2 = cuerpo(consultar("/api/evaluaciones/2", VALENTINA).andExpect(status().isOk()));
        assertThat(e2.get("puntajeTotal").asInt()).isEqualTo(96);
        assertThat(e2.get("respuestas")).hasSize(20);
    }
}

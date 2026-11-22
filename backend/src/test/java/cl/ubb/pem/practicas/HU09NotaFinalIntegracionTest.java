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
 * HU-09: estructura evaluativa y nota final, contra la base real (cada prueba se revierte).
 * Seed: Práctica Pedagógica (asignatura 5) tiene la estructura oficial (componentes 1 a 8: 26 % de pautas 10 a 13 y
 * 74 % externos); Valentina (inscripción 11, en curso, profesora del curso Patricia = CURSO2) ya tiene los productos 1
 * (6,2) y 2 (5,8). Trabajo Colaborativo (asignatura 3) no tiene estructura: la inscripción 3 (Camila, guía GUIA1, curso
 * CURSO1) tiene la evaluación 4 en borrador con la pauta 1 (20 indicadores, escala 1 a 7).
 */
class HU09NotaFinalIntegracionTest extends IntegracionBase {

    private static Map<String, Object> componente(Integer id, String tipo, String nombre, Integer pautaId, int porcentaje) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("tipo", tipo);
        m.put("nombre", nombre);
        m.put("pautaId", pautaId);
        m.put("porcentaje", porcentaje);
        return m;
    }

    private static Map<String, Object> estructura(Map<?, ?>... componentes) {
        return Map.of("componentes", List.of(componentes));
    }

    /** Trabajo Colaborativo: la pauta 1 pesa 40 % y un informe externo 60 %. */
    private JsonNode estructuraDeTrabajoColaborativo(String email) throws Exception {
        return cuerpo(reemplazar("/api/asignaturas/3/estructura-evaluativa", email, estructura(
                componente(null, "PAUTA", null, 1, 40),
                componente(null, "EXTERNO", "Informe final", null, 60))).andExpect(status().isOk()));
    }

    /** Termina la evaluación 4 (borrador de GUIA1 para Camila) con 5 en los 20 indicadores: 100 de 140 = nota 4,9. */
    private void finalizarEvaluacion4() throws Exception {
        JsonNode pauta = cuerpo(consultar("/api/pautas/1", GUIA1));
        List<Map<String, Object>> items = new ArrayList<>();
        pauta.get("dimensiones").forEach(d -> d.get("indicadores")
                .forEach(i -> items.add(Map.of("indicadorId", i.get("id").asInt(), "puntaje", 5))));
        reemplazar("/api/evaluaciones/4/respuestas", GUIA1, Map.of("respuestas", items)).andExpect(status().isOk());
        sincronizar();
        enviar("/api/evaluaciones/4/finalizar", GUIA1).andExpect(status().isOk()).andExpect(jsonPath("$.nota").value(4.9));
    }

    private static JsonNode componenteDe(JsonNode nota, String nombre) {
        for (JsonNode c : nota.get("componentes")) {
            if (c.get("nombre").asText().equals(nombre)) {
                return c;
            }
        }
        throw new AssertionError("No está el componente " + nombre);
    }

    @Test
    void laEstructuraOficialDePracticaPedagogicaSumaCienConPautasYExternos() throws Exception {
        JsonNode e = cuerpo(consultar("/api/asignaturas/5/estructura-evaluativa", CURSO2).andExpect(status().isOk()));
        assertThat(e.get("componentes")).hasSize(8);
        assertThat(e.get("porcentajeTotal").asInt()).isEqualTo(100);
        assertThat(e.get("porcentajePautas").asInt()).isEqualTo(26);
        assertThat(e.get("porcentajeExternos").asInt()).isEqualTo(74);
        assertThat(e.get("puedeEditar").asBoolean()).isTrue();
        assertThat(e.get("componentes").get(2).get("pautaId").asInt()).isEqualTo(10);
        assertThat(e.get("componentes").get(0).get("notasRegistradas").asLong()).isEqualTo(1);
        assertThat(e.get("pautasDisponibles")).hasSize(4);
        // el jefe de carrera la lee pero no la edita; los demás roles no la ven
        consultar("/api/asignaturas/5/estructura-evaluativa", JEFE_CARRERA).andExpect(jsonPath("$.puedeEditar").value(false));
        for (String email : List.of(GUIA1, VALENTINA, "tutor1@demo.example")) {
            consultar("/api/asignaturas/5/estructura-evaluativa", email).andExpect(status().isForbidden());
        }
    }

    @Test
    void conNotasFaltantesSeMuestraLaParcialConElPorcentajeEvaluado() throws Exception {
        JsonNode n = cuerpo(consultar("/api/inscripciones/11/nota-final", VALENTINA).andExpect(status().isOk()));
        assertThat(n.get("tieneEstructura").asBoolean()).isTrue();
        assertThat(n.get("porcentajeEvaluado").asInt()).isEqualTo(27);
        assertThat(n.get("notaParcial").asDouble()).isEqualTo(5.9);   // (6,2 x 10 + 5,8 x 17) / 27 = 5,948
        assertThat(n.get("completa").asBoolean()).isFalse();
        assertThat(n.get("notaPropuesta").isNull()).isTrue();
        assertThat(n.get("puedeEditarNotas").asBoolean()).isFalse();   // la estudiante solo la lee
        assertThat(componenteDe(n, "Producto 1: Texto Expectativas").get("registradaPor").get("email").asText())
                .isEqualTo(CURSO2);
        // otro estudiante no la ve
        consultar("/api/inscripciones/11/nota-final", CAMILA).andExpect(status().isForbidden());
    }

    @Test
    void laNotaFinalAparecePorCompletoConPautaYExternoYSePropone() throws Exception {
        estructuraDeTrabajoColaborativo(CURSO1);
        JsonNode antes = cuerpo(consultar("/api/inscripciones/3/nota-final", CURSO1));
        assertThat(antes.get("porcentajeEvaluado").asInt()).isZero();
        assertThat(antes.get("notaParcial").isNull()).isTrue();

        finalizarEvaluacion4();
        JsonNode conPauta = cuerpo(consultar("/api/inscripciones/3/nota-final", CURSO1));
        JsonNode dePauta = conPauta.get("componentes").get(0);
        assertThat(dePauta.get("nota").asDouble()).isEqualTo(4.9);
        assertThat(dePauta.get("evaluaciones").asInt()).isEqualTo(1);
        assertThat(conPauta.get("porcentajeEvaluado").asInt()).isEqualTo(40);
        assertThat(conPauta.get("notaParcial").asDouble()).isEqualTo(4.9);
        assertThat(conPauta.get("puedeEditarNotas").asBoolean()).isTrue();

        int informe = conPauta.get("componentes").get(1).get("componenteId").asInt();
        JsonNode completa = cuerpo(reemplazar("/api/inscripciones/3/notas-externas/" + informe, CURSO1, Map.of("nota", 6.0))
                .andExpect(status().isOk()));
        assertThat(completa.get("completa").asBoolean()).isTrue();
        assertThat(completa.get("porcentajeEvaluado").asInt()).isEqualTo(100);
        assertThat(completa.get("notaPropuesta").asDouble()).isEqualTo(5.6);   // 4,9 x 0,4 + 6,0 x 0,6 = 5,56

        // el listado de la asignatura muestra lo mismo para el profesor del curso (solo sus inscripciones)
        JsonNode lista = cuerpo(consultar("/api/notas?asignaturaId=3&periodo=2026-2", CURSO1).andExpect(status().isOk()));
        assertThat(lista.get("estructura").get("componentes")).hasSize(2);
        JsonNode fila = null;
        for (JsonNode f : lista.get("inscripciones")) {
            assertThat(f.get("periodo").asText()).isEqualTo("2026-2");
            if (f.get("inscripcionId").asLong() == 3) {
                fila = f;
            }
        }
        assertThat(fila).isNotNull();
        assertThat(fila.get("notaPropuesta").asDouble()).isEqualTo(5.6);
        JsonNode delOtroProfesor = cuerpo(consultar("/api/notas?asignaturaId=3&periodo=2026-2", CURSO2));
        assertThat(delOtroProfesor.get("inscripciones").findValuesAsText("inscripcionId")).doesNotContain("3");

        // borrar la nota externa vuelve a la parcial
        borrar("/api/inscripciones/3/notas-externas/" + informe, CURSO1).andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeEvaluado").value(40)).andExpect(jsonPath("$.completa").value(false));
    }

    @Test
    void soloElProfesorDelCursoDeLaInscripcionOElCoordinadorEscribenNotasExternas() throws Exception {
        estructuraDeTrabajoColaborativo(COORDINADOR);
        int informe = cuerpo(consultar("/api/asignaturas/3/estructura-evaluativa", COORDINADOR))
                .get("componentes").get(1).get("id").asInt();
        int dePauta = cuerpo(consultar("/api/asignaturas/3/estructura-evaluativa", COORDINADOR))
                .get("componentes").get(0).get("id").asInt();
        String url = "/api/inscripciones/3/notas-externas/" + informe;
        reemplazar(url, CURSO2, Map.of("nota", 5.0)).andExpect(status().isForbidden());        // no es su inscripción
        reemplazar(url, JEFE_CARRERA, Map.of("nota", 5.0)).andExpect(status().isForbidden());  // solo lectura
        reemplazar(url, GUIA1, Map.of("nota", 5.0)).andExpect(status().isForbidden());
        reemplazar(url, CAMILA, Map.of("nota", 5.0)).andExpect(status().isForbidden());
        reemplazar(url, COORDINADOR, Map.of("nota", 5.0)).andExpect(status().isOk());          // respaldo
        reemplazar(url, CURSO1, Map.of("nota", 7.5)).andExpect(status().isBadRequest());
        reemplazar(url, CURSO1, Map.of("nota", 0.9)).andExpect(status().isBadRequest());
        reemplazar(url, CURSO1, Map.of("nota", 5.55)).andExpect(status().isBadRequest());      // un decimal
        // un componente de pauta no se escribe a mano; uno de otra asignatura tampoco
        reemplazar("/api/inscripciones/3/notas-externas/" + dePauta, CURSO1, Map.of("nota", 5.0)).andExpect(status().isBadRequest());
        reemplazar("/api/inscripciones/3/notas-externas/1", CURSO1, Map.of("nota", 5.0)).andExpect(status().isBadRequest());
        reemplazar("/api/inscripciones/3/notas-externas/9999", CURSO1, Map.of("nota", 5.0)).andExpect(status().isNotFound());
        // inscripción cerrada (la 9 de Valentina, aprobada, también es de CURSO1): sus notas no cambian
        reemplazar("/api/inscripciones/9/notas-externas/" + informe, CURSO1, Map.of("nota", 5.0)).andExpect(status().isConflict());
        consultar("/api/inscripciones/9/nota-final", CURSO1).andExpect(jsonPath("$.puedeEditarNotas").value(false))
                .andExpect(jsonPath("$.notaFinalGuardada").value(6.4));
    }

    @Test
    void laEstructuraSeValidaAntesDeGuardar() throws Exception {
        String url = "/api/asignaturas/3/estructura-evaluativa";
        JsonNode err = cuerpo(reemplazar(url, CURSO1, estructura(
                componente(null, "PAUTA", null, 1, 30), componente(null, "EXTERNO", "Informe", null, 60)))
                .andExpect(status().isBadRequest()));
        assertThat(err.get("mensaje").asText()).contains("suman 90 %");
        // pauta de otra asignatura, pauta repetida, externo sin nombre o con pauta, pauta de PAUTA faltante
        reemplazar(url, CURSO1, estructura(componente(null, "PAUTA", null, 2, 100))).andExpect(status().isBadRequest());
        reemplazar(url, CURSO1, estructura(componente(null, "PAUTA", null, 1, 50), componente(null, "PAUTA", null, 1, 50)))
                .andExpect(status().isBadRequest());
        reemplazar(url, CURSO1, estructura(componente(null, "EXTERNO", " ", null, 100))).andExpect(status().isBadRequest());
        reemplazar(url, CURSO1, estructura(componente(null, "EXTERNO", "X", 1, 100))).andExpect(status().isBadRequest());
        reemplazar(url, CURSO1, estructura(componente(null, "PAUTA", null, null, 100))).andExpect(status().isBadRequest());
        reemplazar(url, CURSO1, estructura(componente(null, "EXTERNO", "X", null, 0), componente(null, "EXTERNO", "Y", null, 100)))
                .andExpect(status().isBadRequest());
        // un componente de otra asignatura no se usa aquí; uno existente no cambia de tipo
        reemplazar(url, CURSO1, estructura(componente(1, "EXTERNO", "X", null, 100))).andExpect(status().isBadRequest());
        reemplazar("/api/asignaturas/5/estructura-evaluativa", CURSO2, estructura(componente(3, "EXTERNO", "X", null, 100)))
                .andExpect(status().isBadRequest());
        // nada cambió
        consultar(url, CURSO1).andExpect(jsonPath("$.componentes.length()").value(0));
    }

    @Test
    void laEditanElCoordinadorYLosProfesoresDelCursoDeEsaAsignatura() throws Exception {
        Map<String, Object> todoExterno = estructura(componente(null, "EXTERNO", "Informe", null, 100));
        // CURSO1 no tiene inscripciones en Práctica Profesional (asignatura 6)
        reemplazar("/api/asignaturas/6/estructura-evaluativa", CURSO1, todoExterno).andExpect(status().isForbidden());
        consultar("/api/asignaturas/6/estructura-evaluativa", CURSO1).andExpect(jsonPath("$.puedeEditar").value(false));
        for (String email : List.of(JEFE_CARRERA, GUIA1, CAMILA)) {
            reemplazar("/api/asignaturas/6/estructura-evaluativa", email, todoExterno).andExpect(status().isForbidden());
        }
        reemplazar("/api/asignaturas/6/estructura-evaluativa", CURSO2, todoExterno).andExpect(status().isOk());
        reemplazar("/api/asignaturas/6/estructura-evaluativa", COORDINADOR, estructura()).andExpect(status().isOk())
                .andExpect(jsonPath("$.componentes.length()").value(0));   // lista vacía = sin estructura
        reemplazar("/api/asignaturas/99/estructura-evaluativa", COORDINADOR, todoExterno).andExpect(status().isNotFound());
    }

    @Test
    void cambiarLaEstructuraAMitadDeSemestreRecalculaYQuitarUnExternoBorraSusNotas() throws Exception {
        // Se quita el producto 1 (que Valentina ya tenía) y su 10 % pasa al producto 2
        JsonNode actual = cuerpo(consultar("/api/asignaturas/5/estructura-evaluativa", CURSO2));
        List<Map<String, Object>> nuevos = new ArrayList<>();
        for (JsonNode c : actual.get("componentes")) {
            int id = c.get("id").asInt();
            if (id == 1) {
                continue;
            }
            int porcentaje = id == 2 ? 27 : c.get("porcentaje").asInt();
            nuevos.add(componente(id, c.get("tipo").asText(), c.get("nombre").asText(),
                    c.get("pautaId").isNull() ? null : c.get("pautaId").asInt(), porcentaje));
        }
        JsonNode guardada = cuerpo(reemplazar("/api/asignaturas/5/estructura-evaluativa", CURSO2, Map.of("componentes", nuevos))
                .andExpect(status().isOk()));
        assertThat(guardada.get("componentes")).hasSize(7);
        assertThat(guardada.get("componentes").get(0).get("porcentaje").asInt()).isEqualTo(27);
        assertThat(guardada.get("componentes").get(0).get("orden").asInt()).isEqualTo(1);
        JsonNode n = cuerpo(consultar("/api/inscripciones/11/nota-final", CURSO2));
        assertThat(n.get("porcentajeEvaluado").asInt()).isEqualTo(27);
        assertThat(n.get("notaParcial").asDouble()).isEqualTo(5.8);   // solo el producto 2
        assertThat(n.get("componentes")).hasSize(7);
    }

    @Test
    void unaVersionNuevaDeLaPautaMueveElComponenteYLaNotaGuardadaNoCambia() throws Exception {
        estructuraDeTrabajoColaborativo(CURSO1);
        finalizarEvaluacion4();
        // el coordinador edita la pauta 1 (ya usada): se crea la versión 2
        JsonNode p = cuerpo(consultar("/api/pautas/1", COORDINADOR));
        List<Map<String, Object>> dims = new ArrayList<>();
        for (JsonNode d : p.get("dimensiones")) {
            List<String> textos = new ArrayList<>();
            d.get("indicadores").forEach(i -> textos.add(i.get("descripcion").asText()));
            dims.add(new HashMap<>(Map.of("nombre", d.get("nombre").asText(), "indicadores", textos)));
        }
        @SuppressWarnings("unchecked")
        List<String> ultima = (List<String>) dims.get(dims.size() - 1).get("indicadores");
        ultima.add("Indicador nuevo de la versión 2.");
        Map<String, Object> cambio = new HashMap<>();
        cambio.put("asignaturaId", 3);
        cambio.put("nombre", p.get("nombre").asText());
        cambio.put("tipoEvaluador", "COLABORADOR");
        cambio.put("ambito", "OBSERVACION_CLASE");
        cambio.put("escalaMin", 1);
        cambio.put("escalaMax", 7);
        cambio.put("dimensiones", dims);
        int v2 = cuerpo(reemplazar("/api/pautas/1", COORDINADOR, cambio).andExpect(status().isOk())).get("id").asInt();
        assertThat(v2).isNotEqualTo(1);

        JsonNode e = cuerpo(consultar("/api/asignaturas/3/estructura-evaluativa", CURSO1));
        assertThat(e.get("componentes").get(0).get("pautaId").asInt()).isEqualTo(v2);
        assertThat(e.get("componentes").get(0).get("pautaVersion").asInt()).isEqualTo(2);
        // la evaluación hecha con la versión 1 sigue contando, con su nota de siempre
        consultar("/api/evaluaciones/4", CURSO1).andExpect(jsonPath("$.nota").value(4.9));
        JsonNode n = cuerpo(consultar("/api/inscripciones/3/nota-final", CURSO1));
        assertThat(n.get("componentes").get(0).get("nota").asDouble()).isEqualTo(4.9);
        // borrar la versión 2 (sin uso) devuelve el componente a la versión 1
        borrar("/api/pautas/" + v2, COORDINADOR).andExpect(status().is2xxSuccessful());
        consultar("/api/asignaturas/3/estructura-evaluativa", CURSO1).andExpect(jsonPath("$.componentes[0].pautaId").value(1));
    }

    @Test
    void unaPautaDeLaEstructuraNoSeBorraSinQuitarlaAntes() throws Exception {
        // la pauta 10 (sin evaluaciones) es parte de la estructura de Práctica Pedagógica
        JsonNode err = cuerpo(borrar("/api/pautas/10", COORDINADOR).andExpect(status().isConflict()));
        assertThat(err.get("mensaje").asText()).contains("estructura evaluativa");
    }

    @Test
    void elPeriodoDelListadoSeValida() throws Exception {
        consultar("/api/notas?asignaturaId=5&periodo=2026-3", COORDINADOR).andExpect(status().isBadRequest());
        consultar("/api/notas?asignaturaId=99&periodo=2026-2", COORDINADOR).andExpect(status().isNotFound());
        consultar("/api/notas?asignaturaId=5&periodo=2026-2", GUIA1).andExpect(status().isForbidden());
        JsonNode lista = cuerpo(consultar("/api/notas?asignaturaId=5&periodo=2026-2", JEFE_CARRERA).andExpect(status().isOk()));
        assertThat(lista.get("inscripciones").findValuesAsText("inscripcionId")).contains("11");
        for (JsonNode f : lista.get("inscripciones")) {
            assertThat(f.get("puedeEditarNotas").asBoolean()).isFalse();   // el jefe de carrera solo lee
        }
    }
}

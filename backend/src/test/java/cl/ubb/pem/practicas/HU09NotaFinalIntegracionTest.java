package cl.ubb.pem.practicas;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HU-09: nota final ponderada, contra la base real con los datos de db/seed.sql (cada prueba se revierte).
 * Práctica Pedagógica (asignatura 5) tiene 8 componentes que suman 100 (ids 1 a 8: 10, 17, 5, 13, 3, 5, 20 y 27 %).
 * La inscripción 55 (profesor del curso Ricardo Núñez = CURSO1) tiene los 8 y nota calculada 5,9; la 11 (Valentina,
 * profesora del curso Patricia Contreras = CURSO2) tiene 3 de 8 y todavía no tiene nota calculada.
 */
class HU09NotaFinalIntegracionTest extends IntegracionBase {

    private static final double[] PESOS_REALES = {10, 17, 5, 13, 3, 5, 20, 27};

    // ---------- Criterio 1: los pesos se configuran por asignatura ----------

    @Test
    void laEstructuraDePracticaPedagogicaTraeSusOchoComponentesQueSuman100() throws Exception {
        consultar("/api/asignaturas/5/componentes-nota", CAMILA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.componentes.length()").value(8))
                .andExpect(jsonPath("$.componentes[6].nombre").value("Integración curricular: TANEP"))
                .andExpect(jsonPath("$.componentes[6].peso").value(20.0))
                .andExpect(jsonPath("$.sumaPesos").value(100.0))
                .andExpect(jsonPath("$.completa").value(true));
        consultar("/api/asignaturas/3/componentes-nota", COORDINADOR)
                .andExpect(jsonPath("$.componentes.length()").value(0))
                .andExpect(jsonPath("$.completa").value(false));
    }

    @Test
    void elCoordinadorConfiguraLosPesosDeOtraAsignatura() throws Exception {
        Map<String, Object> cuerpo = Map.of("componentes", List.of(
                Map.of("nombre", "Pauta del colaborador", "peso", 60),
                Map.of("nombre", "Informe de la práctica", "peso", 40)));
        reemplazar("/api/asignaturas/3/componentes-nota", COORDINADOR, cuerpo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.componentes.length()").value(2))
                .andExpect(jsonPath("$.componentes[1].orden").value(2))
                .andExpect(jsonPath("$.completa").value(true));
        // Camila (inscripción 3) todavía no tiene notas: sin nota calculada
        consultar("/api/inscripciones/3/nota-final", COORDINADOR)
                .andExpect(jsonPath("$.estructuraCompleta").value(true))
                .andExpect(jsonPath("$.registradas").value(0))
                .andExpect(jsonPath("$.notaCalculada").doesNotExist());
    }

    @Test
    void losPesosDebenSumar100YLosNombresNoSeRepiten() throws Exception {
        reemplazar("/api/asignaturas/3/componentes-nota", COORDINADOR, Map.of("componentes", List.of(
                Map.of("nombre", "A", "peso", 50), Map.of("nombre", "B", "peso", 40))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value(
                        "Los pesos deben sumar 100 % y suman 90 %."));
        reemplazar("/api/asignaturas/3/componentes-nota", COORDINADOR, Map.of("componentes", List.of(
                Map.of("nombre", "Pauta", "peso", 50), Map.of("nombre", "pauta ", "peso", 50))))
                .andExpect(status().isBadRequest());
        reemplazar("/api/asignaturas/3/componentes-nota", COORDINADOR, Map.of("componentes", List.of(
                Map.of("nombre", "A", "peso", 0), Map.of("nombre", "B", "peso", 100))))
                .andExpect(status().isBadRequest());
        reemplazar("/api/asignaturas/3/componentes-nota", COORDINADOR, Map.of("componentes", List.of(
                Map.of("id", 1, "nombre", "De otra asignatura", "peso", 100))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarLosPesosRecalculaLaNotaDeLasInscripcionesEnCurso() throws Exception {
        // 6,0·50 + 5,5·10 + 6,2·5 + 5,8·5 + 6,5·5 + 6,0·5 + 5,7·10 + 6,1·10 = 595,5 → 5,955 → 6,0
        reemplazar("/api/asignaturas/5/componentes-nota", COORDINADOR,
                        estructuraPracticaPedagogica(new double[]{50, 10, 5, 5, 5, 5, 10, 10}))
                .andExpect(status().isOk());
        consultar("/api/inscripciones/55/nota-final", CURSO1).andExpect(jsonPath("$.notaCalculada").value(6.0));
    }

    @Test
    void unComponenteConNotasNoSeQuitaPeroSeRenombra() throws Exception {
        Map<String, Object> sinElPrimero = estructuraPracticaPedagogica(PESOS_REALES);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lista = (List<Map<String, Object>>) sinElPrimero.get("componentes");
        lista.remove(0);
        lista.get(0).put("peso", 27);   // 17 + 10 para que sigan sumando 100
        reemplazar("/api/asignaturas/5/componentes-nota", COORDINADOR, sinElPrimero)
                .andExpect(status().isConflict());

        Map<String, Object> renombrado = estructuraPracticaPedagogica(PESOS_REALES);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> otra = (List<Map<String, Object>>) renombrado.get("componentes");
        otra.get(0).put("nombre", "Producto 1: expectativas de la práctica");
        reemplazar("/api/asignaturas/5/componentes-nota", COORDINADOR, renombrado)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.componentes[0].id").value(1))
                .andExpect(jsonPath("$.componentes[0].nombre").value("Producto 1: expectativas de la práctica"));
    }

    @Test
    void soloElCoordinadorConfiguraLaEstructura() throws Exception {
        Map<String, Object> cuerpo = Map.of("componentes", List.of(Map.of("nombre", "Todo", "peso", 100)));
        for (String email : List.of(JEFE_CARRERA, CURSO1, GUIA1, CAMILA)) {
            reemplazar("/api/asignaturas/3/componentes-nota", email, cuerpo).andExpect(status().isForbidden());
        }
    }

    // ---------- Criterio 2: la nota queda guardada en la inscripción ----------

    @Test
    void laNotaCalculadaQuedaEnLaInscripcion() throws Exception {
        consultar("/api/inscripciones/55/nota-final", CURSO1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registradas").value(8))
                .andExpect(jsonPath("$.notaCalculada").value(5.9))
                .andExpect(jsonPath("$.componentes[7].aporte").value(1.65))   // 6,1 × 27 %
                .andExpect(jsonPath("$.puedeRegistrar").value(true));
        consultar("/api/inscripciones/55", COORDINADOR).andExpect(jsonPath("$.notaCalculada").value(5.9))
                .andExpect(jsonPath("$.notaFinal").doesNotExist());
    }

    @Test
    void alRegistrarElUltimoComponenteSeCalculaLaNota() throws Exception {
        consultar("/api/inscripciones/11/nota-final", CURSO2)
                .andExpect(jsonPath("$.registradas").value(3))
                .andExpect(jsonPath("$.notaCalculada").doesNotExist());
        for (int componente = 4; componente <= 8; componente++) {
            reemplazar("/api/inscripciones/11/nota-final/componentes/" + componente, CURSO2, Map.of("nota", 6.0))
                    .andExpect(status().isOk());
        }
        // 6,5·10 + 6,0·17 + 6,3·5 + 6,0·(13 + 3 + 5 + 20 + 27) = 606,5 → 6,065 → 6,1
        consultar("/api/inscripciones/11", COORDINADOR).andExpect(jsonPath("$.notaCalculada").value(6.1));
        // Corregir una nota recalcula: 7,0 en el TANEP suma 0,2 → 6,265 → 6,3
        reemplazar("/api/inscripciones/11/nota-final/componentes/7", CURSO2, Map.of("nota", 7.0))
                .andExpect(jsonPath("$.notaCalculada").value(6.3))
                .andExpect(jsonPath("$.componentes[6].registradaPor").value("Patricia Contreras Mora"));
    }

    @Test
    void soloElProfesorDelCursoDeEsaInscripcionRegistraNotas() throws Exception {
        Map<String, Object> nota = Map.of("nota", 6.0);
        reemplazar("/api/inscripciones/11/nota-final/componentes/4", CURSO1, nota)
                .andExpect(status().isForbidden());   // es de otra inscripción
        for (String email : List.of(COORDINADOR, JEFE_CARRERA, GUIA2, VALENTINA)) {
            reemplazar("/api/inscripciones/11/nota-final/componentes/4", email, nota).andExpect(status().isForbidden());
        }
        consultar("/api/inscripciones/11/nota-final", VALENTINA).andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeRegistrar").value(false));
        consultar("/api/inscripciones/11/nota-final", DIEGO).andExpect(status().isForbidden());
    }

    @Test
    void notasFueraDeEscalaComponentesAjenosEInscripcionesCerradas() throws Exception {
        reemplazar("/api/inscripciones/11/nota-final/componentes/4", CURSO2, Map.of("nota", 7.5))
                .andExpect(status().isBadRequest());
        reemplazar("/api/inscripciones/11/nota-final/componentes/4", CURSO2, Map.of("nota", 6.25))
                .andExpect(status().isBadRequest());
        // La inscripción 3 es de Trabajo Colaborativo: el componente 1 es de Práctica Pedagógica
        reemplazar("/api/inscripciones/3/nota-final/componentes/1", CURSO1, Map.of("nota", 6.0))
                .andExpect(status().isNotFound());
        // La 16 (Sebastián, Práctica Pedagógica 2025-2) ya está APROBADA
        reemplazar("/api/inscripciones/16/nota-final/componentes/1", CURSO2, Map.of("nota", 6.0))
                .andExpect(status().isConflict());
        consultar("/api/inscripciones/999/nota-final", COORDINADOR).andExpect(status().isNotFound());
    }

    /** La estructura actual de Práctica Pedagógica (ids 1 a 8, mismos nombres) con otros pesos. */
    private Map<String, Object> estructuraPracticaPedagogica(double[] pesos) throws Exception {
        List<Map<String, Object>> lista = new ArrayList<>();
        var actuales = cuerpo(consultar("/api/asignaturas/5/componentes-nota", COORDINADOR)).get("componentes");
        for (int n = 0; n < 8; n++) {
            Map<String, Object> c = new HashMap<>();
            c.put("id", actuales.get(n).get("id").asInt());
            c.put("nombre", actuales.get(n).get("nombre").asText());
            c.put("peso", pesos[n]);
            lista.add(c);
        }
        return new HashMap<>(Map.of("componentes", lista));
    }
}

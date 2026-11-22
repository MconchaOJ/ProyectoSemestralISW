package cl.ubb.pem.practicas;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Usuario;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HU-10: casos de la prueba de Alej (HU10EquiposIntegracionTest, commit 7e9b132) llevados a nuestra API
 * (/api/grupos, grupo_practica/grupo_id) y a los grupos de nuestro seed: 1 = inscripciones 18 a 21 (Aproximación
 * 2026-2) y 2 = 25 y 27 (Ambientes 2026-1). Las inscripciones 22 y 23 (Aproximación 2026-2) no tienen grupo.
 */
class HU10GruposReglasIntegracionTest extends IntegracionBase {

    private static Map<String, Object> grupo(List<Long> ids, String establecimiento, String curso) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("inscripcionIds", ids);
        cuerpo.put("establecimiento", establecimiento);
        cuerpo.put("curso", curso);
        return cuerpo;
    }

    /** En el seed no hay dos inscripciones en curso con el mismo guía en Trabajo Colaborativo 2026-2: se ajusta una. */
    private void mismoGuia(Long inscripcionId, Long guiaId) {
        em.find(Inscripcion.class, inscripcionId).setProfesorGuia(em.find(Usuario.class, guiaId));
        sincronizar();
    }

    @Test
    void elCoordinadorArmaUnGrupoDeAproximacion() throws Exception {
        enviar("/api/grupos", COORDINADOR, grupo(List.of(22L, 23L), null, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.integrantes.length()").value(2))
                .andExpect(jsonPath("$.modalidad").value("GRUPO"));
    }

    @Test
    void cadaIntegranteConservaSuPropiaInscripcionYSusEvaluaciones() throws Exception {   // criterio 2
        mismoGuia(6L, 2L);
        enviar("/api/grupos", COORDINADOR, grupo(List.of(3L, 6L), "Liceo Bicentenario Ejemplo", "1° Medio B"))
                .andExpect(status().isCreated());
        // la evaluación 5 sigue siendo de Diego (inscripción 6), no de la dupla
        consultar("/api/evaluaciones/5", COORDINADOR).andExpect(jsonPath("$.inscripcionId").value(6));
        consultar("/api/inscripciones/3", COORDINADOR)
                .andExpect(jsonPath("$.estudiante.nombreCompleto").value("Camila Rojas Soto"));
    }

    @Test
    void reglasDeTamanoAsignaturaYEstado() throws Exception {
        // Práctica Pedagógica es individual: no forma duplas ni grupos
        enviar("/api/grupos", COORDINADOR, grupo(List.of(45L, 50L), null, null)).andExpect(status().isBadRequest());
        // distinta asignatura
        enviar("/api/grupos", COORDINADOR, grupo(List.of(11L, 17L), null, null)).andExpect(status().isBadRequest());
        // una dupla de 3
        mismoGuia(6L, 2L);
        mismoGuia(34L, 2L);
        enviar("/api/grupos", COORDINADOR, grupo(List.of(3L, 6L, 34L), "Liceo", "1° Medio A"))
                .andExpect(status().isBadRequest());
        // más de 4, menos de 2 o una inscripción repetida
        enviar("/api/grupos", COORDINADOR, grupo(List.of(18L, 19L, 20L, 21L, 22L), null, null))
                .andExpect(status().isBadRequest());
        enviar("/api/grupos", COORDINADOR, grupo(List.of(22L), null, null)).andExpect(status().isBadRequest());
        enviar("/api/grupos", COORDINADOR, grupo(List.of(22L, 22L), null, null)).andExpect(status().isBadRequest());
        // una ya está en otro grupo, o no existe
        enviar("/api/grupos", COORDINADOR, grupo(List.of(18L, 22L), null, null)).andExpect(status().isConflict());
        enviar("/api/grupos", COORDINADOR, grupo(List.of(22L, 999L), null, null)).andExpect(status().isNotFound());
    }

    @Test
    void deshacerUnGrupoInexistenteDa404() throws Exception {
        borrar("/api/grupos/999", COORDINADOR).andExpect(status().isNotFound());
        consultar("/api/grupos/999", COORDINADOR).andExpect(status().isNotFound());
    }
}

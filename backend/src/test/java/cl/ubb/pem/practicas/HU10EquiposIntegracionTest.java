package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Usuario;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-10: prácticas en dupla y en grupo, contra la base real con los datos de db/seed.sql (cada prueba se revierte).
 * Equipos del seed: 1 = inscripciones 25 y 27 (Ambientes 2026-1), 2 = 29 y 31 (Ambientes 2026-1), 3 = 19 a 22
 * (grupo de Aproximación 2026-2) y 4 = 14 y 48 (dupla cerrada de Trabajo Colaborativo 2024-2: Sebastián Vargas y
 * Trinidad Cárdenas, guía Marcos Bravo, Liceo Bicentenario Ejemplo, 1° Medio B).
 */
class HU10EquiposIntegracionTest extends IntegracionBase {

    // ---------- Criterio 3: el historial de cada uno muestra con quién trabajó ----------

    @Test
    void elHistorialMuestraConQuienTrabajo() throws Exception {
        consultar("/api/estudiantes/4/historial", COORDINADOR)
                .andExpect(status().isOk())
                // Sebastián: la inscripción 14 es la tercera de su recorrido (2023-2, 2024-1, 2024-2)
                .andExpect(jsonPath("$.inscripciones[2].inscripcionId").value(14))
                .andExpect(jsonPath("$.inscripciones[2].equipo.establecimiento").value("Liceo Bicentenario Ejemplo"))
                .andExpect(jsonPath("$.inscripciones[2].equipo.companeros.length()").value(1))
                .andExpect(jsonPath("$.inscripciones[2].equipo.companeros[0].nombreCompleto")
                        .value("Trinidad Cárdenas Rojo"))
                .andExpect(jsonPath("$.inscripciones[0].equipo").doesNotExist());

        String pdf = texto(consultar("/api/estudiantes/4/historial/pdf", COORDINADOR));
        assertThat(pdf).contains("Trabajó en dupla con: Trinidad Cárdenas Rojo");
    }

    @Test
    void laInscripcionTraeSuEquipoSinIncluirseASiMisma() throws Exception {
        consultar("/api/inscripciones/25", COORDINADOR)
                .andExpect(jsonPath("$.equipo.id").value(1))
                .andExpect(jsonPath("$.equipo.companeros.length()").value(1))
                .andExpect(jsonPath("$.equipo.companeros[0].inscripcionId").value(27));
        consultar("/api/inscripciones/19", COORDINADOR).andExpect(jsonPath("$.equipo.companeros.length()").value(3));
        consultar("/api/inscripciones/18", COORDINADOR).andExpect(jsonPath("$.equipo").doesNotExist());
    }

    @Test
    void elCoordinadorYElJefeListanLosEquipos() throws Exception {
        consultar("/api/equipos?periodo=2026-1", JEFE_CARRERA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].integrantes.length()").value(2));
        consultar("/api/equipos/4", COORDINADOR)
                .andExpect(jsonPath("$.profesorGuia.nombreCompleto").value("Marcos Bravo Tapia"))
                .andExpect(jsonPath("$.curso").value("1° Medio B"));
        consultar("/api/equipos", GUIA1).andExpect(status().isForbidden());
    }

    // ---------- Armar equipos: criterio 1 y las reglas de la modalidad ----------

    @Test
    void elCoordinadorArmaUnGrupoDeAproximacion() throws Exception {
        // Javiera (18) y la inscripción 23 son de Aproximación 2026-2 y no tienen grupo; sin colegio, se ignora
        enviar("/api/equipos", COORDINADOR, equipo(List.of(18L, 23L), "Colegio que no aplica", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.integrantes.length()").value(2))
                .andExpect(jsonPath("$.establecimiento").doesNotExist())
                .andExpect(jsonPath("$.asignatura.modalidad").value("GRUPO"));
        consultar("/api/inscripciones/18", COORDINADOR).andExpect(jsonPath("$.equipo.companeros[0].inscripcionId").value(23));
    }

    @Test
    void unaDuplaComparteGuiaColegioYCurso() throws Exception {
        // Trabajo Colaborativo 2026-2: Camila (3, guía Marcos Bravo) y Diego (6, guía Lorena Vidal)
        enviar("/api/equipos", COORDINADOR, equipo(List.of(3L, 6L), "Liceo Bicentenario Ejemplo", "1° Medio B"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Los integrantes deben tener el mismo profesor guía (criterio 1)."));

        mismoGuia(6L, 2L);
        enviar("/api/equipos", COORDINADOR, equipo(List.of(3L, 6L), null, null))
                .andExpect(status().isBadRequest());   // va a un colegio: colegio y curso obligatorios
        enviar("/api/equipos", COORDINADOR, equipo(List.of(3L, 6L), "Liceo Bicentenario Ejemplo", "1° Medio B"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profesorGuia.nombreCompleto").value("Marcos Bravo Tapia"))
                .andExpect(jsonPath("$.establecimiento").value("Liceo Bicentenario Ejemplo"));
    }

    @Test
    void cadaIntegranteConservaSuPropiaInscripcionYSusEvaluaciones() throws Exception {
        mismoGuia(6L, 2L);
        enviar("/api/equipos", COORDINADOR, equipo(List.of(3L, 6L), "Liceo Bicentenario Ejemplo", "1° Medio B"))
                .andExpect(status().isCreated());
        // Criterio 2: la evaluación 5 sigue siendo de Diego (inscripción 6), no de la dupla
        consultar("/api/evaluaciones/5", COORDINADOR).andExpect(jsonPath("$.inscripcionId").value(6));
        consultar("/api/inscripciones/3", COORDINADOR).andExpect(jsonPath("$.estudiante.nombreCompleto")
                .value("Camila Rojas Soto"));
    }

    @Test
    void reglasDeTamanoAsignaturaYEstado() throws Exception {
        // Práctica Pedagógica es individual
        enviar("/api/equipos", COORDINADOR, equipo(List.of(45L, 50L), null, null)).andExpect(status().isBadRequest());
        // distinta asignatura
        enviar("/api/equipos", COORDINADOR, equipo(List.of(11L, 17L), null, null)).andExpect(status().isBadRequest());
        // una dupla de 3
        enviar("/api/equipos", COORDINADOR, equipo(List.of(3L, 6L, 34L), "Liceo", "1° Medio A"))
                .andExpect(status().isBadRequest());
        // más de 4 o menos de 2
        enviar("/api/equipos", COORDINADOR, equipo(List.of(18L, 19L, 20L, 21L, 22L), null, null))
                .andExpect(status().isBadRequest());
        enviar("/api/equipos", COORDINADOR, equipo(List.of(18L), null, null)).andExpect(status().isBadRequest());
        enviar("/api/equipos", COORDINADOR, equipo(List.of(18L, 18L), null, null)).andExpect(status().isBadRequest());
        // ya cerradas (Trabajo Colaborativo 2024-2) o ya en otro equipo
        enviar("/api/equipos", COORDINADOR, equipo(List.of(43L, 53L), "Liceo", "1° Medio A"))
                .andExpect(status().isConflict());
        enviar("/api/equipos", COORDINADOR, equipo(List.of(25L, 29L), null, null)).andExpect(status().isConflict());
        enviar("/api/equipos", COORDINADOR, equipo(List.of(18L, 999L), null, null)).andExpect(status().isNotFound());
    }

    @Test
    void soloElCoordinadorArmaYDeshaceEquipos() throws Exception {
        for (String email : List.of(JEFE_CARRERA, GUIA1, CURSO1, CAMILA)) {
            enviar("/api/equipos", email, equipo(List.of(18L, 23L), null, null)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/equipos/1").header("Authorization", "Bearer " + token(email)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void deshacerUnEquipoDejaACadaUnoIndividual() throws Exception {
        borrar("/api/equipos/1").andExpect(status().isNoContent());
        sincronizar();
        consultar("/api/inscripciones/25", COORDINADOR).andExpect(jsonPath("$.equipo").doesNotExist());
        consultar("/api/equipos/1", COORDINADOR).andExpect(status().isNotFound());

        borrar("/api/equipos/4").andExpect(status().isConflict());   // sus inscripciones están cerradas
        borrar("/api/equipos/999").andExpect(status().isNotFound());
    }

    private ResultActions borrar(String url) throws Exception {
        return mvc.perform(delete(url).header("Authorization", "Bearer " + token(COORDINADOR)));
    }

    private static Map<String, Object> equipo(List<Long> ids, String establecimiento, String curso) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("inscripcionIds", ids);
        cuerpo.put("establecimiento", establecimiento);
        cuerpo.put("curso", curso);
        return cuerpo;
    }

    /** En el seed no hay dos inscripciones en curso con el mismo guía en una asignatura en dupla: se ajusta una. */
    private void mismoGuia(Long inscripcionId, Long guiaId) {
        em.find(Inscripcion.class, inscripcionId).setProfesorGuia(em.find(Usuario.class, guiaId));
        sincronizar();
    }

    private static String texto(ResultActions r) throws Exception {
        PdfReader lector = new PdfReader(r.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(new PdfTextExtractor(lector).getTextFromPage(pagina)).append('\n');
        }
        return texto.toString();
    }
}

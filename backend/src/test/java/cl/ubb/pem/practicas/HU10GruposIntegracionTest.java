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
 * HU-10: duplas y grupos, contra la base real con los datos del seed (cada prueba se revierte). Grupo 1 = Aproximación
 * 2026-2 con las inscripciones 18 (Javiera), 19, 20 y 21; grupo 2 = dupla de Ambientes 2026-1 con 25 (Catalina) y 27
 * (Tomás). Sin grupo: 22 y 23 (Aproximación 2026-2), 29 (Josefa) y 31 (Vicente) de Ambientes 2026-1, y Trabajo
 * Colaborativo 2026-2 (3 Camila con guía 2, 6 Diego con guía 3).
 */
class HU10GruposIntegracionTest extends IntegracionBase {

    private static final String TUTOR1 = "tutor1@demo.example";

    private static Map<String, Object> grupo(List<Long> inscripciones) {
        Map<String, Object> m = new HashMap<>();
        m.put("inscripcionIds", inscripciones);
        return m;
    }

    private List<String> companeros(long inscripcionId, String email) throws Exception {
        return cuerpo(consultar("/api/inscripciones/" + inscripcionId, email).andExpect(status().isOk()))
                .get("companeros").findValuesAsText("nombreCompleto");
    }

    @Test
    void losGruposDelSeedSeVenConSusIntegrantes() throws Exception {
        JsonNode lista = cuerpo(consultar("/api/grupos", COORDINADOR).andExpect(status().isOk()));
        assertThat(lista).hasSize(2);
        JsonNode aproximacion = cuerpo(consultar("/api/grupos/1", JEFE_CARRERA).andExpect(status().isOk()));
        assertThat(aproximacion.get("modalidad").asText()).isEqualTo("GRUPO");
        assertThat(aproximacion.get("integrantes")).hasSize(4);
        // cada integrante ve con quién trabaja en su inscripción (el estudiante ve las suyas)
        assertThat(companeros(18, JAVIERA)).hasSize(3).doesNotContain("Javiera Herrera Ibáñez");
        consultar("/api/inscripciones/3", CAMILA).andExpect(jsonPath("$.grupoId").isEmpty())
                .andExpect(jsonPath("$.companeros").isEmpty());
    }

    @Test
    void elCoordinadorFormaUnaDuplaYCadaUnoSigueConLoSuyo() throws Exception {
        JsonNode d = cuerpo(enviar("/api/grupos", COORDINADOR, grupo(List.of(29L, 31L))).andExpect(status().isCreated()));
        assertThat(d.get("asignatura").asText()).isEqualTo("Ambientes de Aprendizaje");
        assertThat(d.get("periodo").asText()).isEqualTo("2026-1");
        assertThat(d.get("integrantes").findValuesAsText("inscripcionId")).containsExactlyInAnyOrder("29", "31");
        sincronizar();
        assertThat(companeros(29, COORDINADOR)).containsExactly("Vicente Carrasco Díaz");
        // S-77: las planificaciones siguen siendo de cada uno (la 13 es de Vicente, inscripción 31)
        consultar("/api/planificaciones/13", COORDINADOR).andExpect(jsonPath("$.inscripcionId").value(31));

        // Ambientes no va a un colegio: no lleva establecimiento ni curso
        long id = d.get("id").asLong();
        Map<String, Object> conColegio = grupo(List.of(29L, 31L));
        conColegio.put("establecimiento", "Liceo Ejemplo");
        reemplazar("/api/grupos/" + id, COORDINADOR, conColegio).andExpect(status().isBadRequest());
    }

    @Test
    void lasReglasDeLaDuplaSeComprueban() throws Exception {   // S-78
        // criterio 1: la dupla comparte guía (Camila y Diego tienen guías distintos)
        JsonNode err = cuerpo(enviar("/api/grupos", COORDINADOR, grupo(List.of(3L, 6L))).andExpect(status().isBadRequest()));
        assertThat(err.get("mensaje").asText()).contains("mismo profesor guía");
        // una dupla lleva exactamente 2
        enviar("/api/grupos", COORDINADOR, grupo(List.of(29L, 31L, 27L))).andExpect(status().isBadRequest());
        // misma asignatura y periodo
        enviar("/api/grupos", COORDINADOR, grupo(List.of(29L, 22L))).andExpect(status().isBadRequest());
        // entre 2 y 4 inscripciones, sin repetir
        enviar("/api/grupos", COORDINADOR, grupo(List.of(29L))).andExpect(status().isBadRequest());
        enviar("/api/grupos", COORDINADOR, grupo(List.of(29L, 29L))).andExpect(status().isBadRequest());
        // ya en otro grupo, o con la práctica cerrada: 409
        enviar("/api/grupos", COORDINADOR, grupo(List.of(22L, 18L))).andExpect(status().isConflict());
        enviar("/api/grupos", COORDINADOR, grupo(List.of(1L, 4L))).andExpect(status().isConflict());   // Aproximación 2025-2, aprobadas
        // inscripción inexistente
        enviar("/api/grupos", COORDINADOR, grupo(List.of(29L, 999L))).andExpect(status().isNotFound());
    }

    @Test
    void sePuedenCambiarLosIntegrantesYDeshacerElGrupo() throws Exception {
        // en el grupo de 4 de Aproximación, Benjamín (21) sale y entra Fernanda (22)
        JsonNode g = cuerpo(reemplazar("/api/grupos/1", COORDINADOR, grupo(List.of(18L, 19L, 20L, 22L))).andExpect(status().isOk()));
        assertThat(g.get("integrantes").findValuesAsText("inscripcionId")).containsExactlyInAnyOrder("18", "19", "20", "22");
        sincronizar();
        assertThat(companeros(21, COORDINADOR)).isEmpty();
        assertThat(companeros(22, COORDINADOR)).hasSize(3);

        // deshacer la dupla de Ambientes: quedan sin grupo y su planificación sigue ahí (la 10 es de Catalina)
        consultar("/api/grupos/2", COORDINADOR).andExpect(status().isOk());
        borrar("/api/grupos/2", COORDINADOR).andExpect(status().isNoContent());
        sincronizar();
        consultar("/api/grupos/2", COORDINADOR).andExpect(status().isNotFound());
        consultar("/api/inscripciones/25", COORDINADOR).andExpect(jsonPath("$.grupoId").isEmpty());
        consultar("/api/planificaciones/10", COORDINADOR).andExpect(status().isOk());
    }

    @Test
    void soloElCoordinadorFormaGruposYElJefeDeCarreraSoloLosLee() throws Exception {
        for (String email : List.of(CAMILA, GUIA1, CURSO1, TUTOR1, JEFE_CARRERA)) {
            enviar("/api/grupos", email, grupo(List.of(29L, 31L))).andExpect(status().isForbidden());
            borrar("/api/grupos/2", email).andExpect(status().isForbidden());
        }
        for (String email : List.of(CAMILA, GUIA1, CURSO1, TUTOR1)) {
            consultar("/api/grupos", email).andExpect(status().isForbidden());
        }
        consultar("/api/grupos", JEFE_CARRERA).andExpect(status().isOk());
    }

    @Test
    void elHistorialMuestraConQuienTrabajo() throws Exception {   // criterio 3, en pantalla y en PDF
        JsonNode h = cuerpo(consultar("/api/estudiantes/5/historial", COORDINADOR).andExpect(status().isOk()));   // Javiera
        JsonNode aproximacion = null;
        for (JsonNode i : h.get("inscripciones")) {
            if (i.get("inscripcionId").asLong() == 18) {
                aproximacion = i;
            }
        }
        assertThat(aproximacion).isNotNull();
        assertThat(aproximacion.get("companeros")).hasSize(3);
        byte[] pdf = consultar("/api/estudiantes/me/historial/pdf", JAVIERA).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        com.lowagie.text.pdf.PdfReader lector = new com.lowagie.text.pdf.PdfReader(pdf);
        var extractor = new com.lowagie.text.pdf.parser.PdfTextExtractor(lector);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(extractor.getTextFromPage(pagina));
        }
        lector.close();
        assertThat(texto.toString()).contains("Trabajó en grupo con:");
    }
}

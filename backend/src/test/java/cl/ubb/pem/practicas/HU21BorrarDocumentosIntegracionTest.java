package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * HU-21/HU-22: borrar o reemplazar un documento adjunto subido por error (cada prueba se revierte).
 * Seed: la planificación 4 de Camila está CON_OBSERVACIONES y la 3 AUTORIZADA; la evaluación 4 (GUIA1) es un borrador
 * de la inscripción 3 (en curso), la 5 (GUIA2) está finalizada en la inscripción 6 (en curso) y la 1 (GUIA1) está
 * finalizada en la inscripción 14 (aprobada).
 */
class HU21BorrarDocumentosIntegracionTest extends IntegracionBase {

    private static MockMultipartFile pdf(String nombre, String contenido) {
        return archivo("archivo", nombre, "application/pdf", ("%PDF-1.4 " + contenido).getBytes());
    }

    private long subir(String base, String email, MockMultipartFile archivo) throws Exception {
        return cuerpo(mvc.perform(multipart(base, email).file(archivo)).andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions cambiar(String url, String email, MockMultipartFile archivo) throws Exception {
        var req = MockMvcRequestBuilders.multipart(HttpMethod.PUT, url).file(archivo);
        if (email != null) {
            req.header("Authorization", "Bearer " + token(email));
        }
        return mvc.perform(req);
    }

    /** Planificación completa de Camila (inscripción 3) para dentro de 10 días, con un documento; devuelve su id. */
    private long planificacionConDocumento() throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("inscripcionId", 3);
        m.put("titulo", "Clase de prueba");
        m.put("establecimiento", "Liceo Bicentenario Ejemplo");
        m.put("curso", "1° Medio B");
        m.put("unidad", "Álgebra");
        m.put("objetivoAprendizaje", "OA.");
        m.put("objetivoActitudinal", "Perseverancia.");
        m.put("objetivoClase", "Objetivo.");
        m.put("fechaClase", LocalDate.now().plusDays(10).toString());
        m.put("duracionMinutos", 90);
        m.put("recursos", "Pizarra.");
        m.put("instrumentosEvaluacion", "Ticket.");
        m.put("inicio", "Inicio.");
        m.put("inicioMinutos", 10);
        m.put("desarrollo", "Desarrollo.");
        m.put("desarrolloMinutos", 60);
        m.put("cierre", "Cierre.");
        m.put("cierreMinutos", 20);
        long id = cuerpo(enviar("/api/planificaciones", CAMILA, m).andExpect(status().isCreated())).get("id").asLong();
        subir("/api/planificaciones/" + id + "/documentos", CAMILA, pdf("plan.pdf", "uno"));
        return id;
    }

    // ---------- Planificación (HU-21) ----------

    @Test
    void elDuenoReemplazaYBorraUnDocumentoDeSuPlanificacion() throws Exception {
        String base = "/api/planificaciones/4/documentos";
        long equivocado = subir(base, CAMILA, pdf("equivocado.pdf", "viejo"));
        long otro = subir(base, CAMILA, pdf("otro.pdf", "otro"));

        JsonNode cambiado = cuerpo(cambiar(base + "/" + equivocado, CAMILA, pdf("correcto.pdf", "nuevo"))
                .andExpect(status().isOk()));
        assertThat(cambiado.get("id").asLong()).isEqualTo(equivocado);           // mismo documento
        assertThat(cambiado.get("nombreArchivo").asText()).isEqualTo("correcto.pdf");
        consultar(base + "/" + equivocado, GUIA1).andExpect(status().isOk())
                .andExpect(content().bytes("%PDF-1.4 nuevo".getBytes()));       // se descarga el archivo nuevo

        borrar(base + "/" + otro, CAMILA).andExpect(status().isNoContent());
        JsonNode lista = cuerpo(consultar(base, CAMILA));
        assertThat(lista.findValuesAsText("nombreArchivo")).containsExactly("correcto.pdf");
        consultar(base + "/" + otro, CAMILA).andExpect(status().isNotFound());
    }

    @Test
    void soloElDuenoBorraOReemplaza() throws Exception {
        String base = "/api/planificaciones/4/documentos";
        long doc = subir(base, CAMILA, pdf("plan.pdf", "x"));
        for (String email : List.of(DIEGO, GUIA1, CURSO1, COORDINADOR, JEFE_CARRERA)) {
            borrar(base + "/" + doc, email).andExpect(status().isForbidden());
            cambiar(base + "/" + doc, email, pdf("y.pdf", "y")).andExpect(status().isForbidden());
        }
        borrar(base + "/" + doc, null).andExpect(status().isUnauthorized());
        consultar(base, CAMILA).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void enRevisionOAutorizadaLosDocumentosNoCambian() throws Exception {
        long id = planificacionConDocumento();
        String base = "/api/planificaciones/" + id + "/documentos";
        long doc = cuerpo(consultar(base, CAMILA)).get(0).get("id").asLong();
        enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isOk());

        borrar(base + "/" + doc, CAMILA).andExpect(status().isConflict());
        cambiar(base + "/" + doc, CAMILA, pdf("otro.pdf", "y")).andExpect(status().isConflict());
        mvc.perform(multipart(base, CAMILA).file(pdf("mas.pdf", "z"))).andExpect(status().isConflict());
        // la planificación 3 ya está autorizada: tampoco se le sube nada
        mvc.perform(multipart("/api/planificaciones/3/documentos", CAMILA).file(pdf("tarde.pdf", "z")))
                .andExpect(status().isConflict());
        consultar(base, CAMILA).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void sePuedeBorrarElUltimoPeroSinDocumentoNoSeEnvia() throws Exception {
        long id = planificacionConDocumento();
        String base = "/api/planificaciones/" + id + "/documentos";
        long doc = cuerpo(consultar(base, CAMILA)).get(0).get("id").asLong();
        borrar(base + "/" + doc, CAMILA).andExpect(status().isNoContent());
        JsonNode err = cuerpo(enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isBadRequest()));
        assertThat(err.toString()).contains("documentoFormal");   // S-47: al enviar se exige al menos uno
    }

    @Test
    void unReemplazoInvalidoDejaElOriginalYUnDocumentoAjenoDa404() throws Exception {
        String base = "/api/planificaciones/4/documentos";
        long doc = subir(base, CAMILA, pdf("plan.pdf", "original"));
        cambiar(base + "/" + doc, CAMILA, archivo("archivo", "virus.exe", "application/octet-stream", "MZ".getBytes()))
                .andExpect(status().isBadRequest());
        consultar(base + "/" + doc, CAMILA).andExpect(content().bytes("%PDF-1.4 original".getBytes()));
        // el documento existe, pero no es de la planificación 5
        borrar("/api/planificaciones/5/documentos/" + doc, CAMILA).andExpect(status().isNotFound());
        borrar(base + "/999999", CAMILA).andExpect(status().isNotFound());
    }

    // ---------- Evaluación (HU-22) ----------

    @Test
    void elEvaluadorReemplazaYBorraEnBorradorYFinalizada() throws Exception {
        String borrador = "/api/evaluaciones/4/documentos";
        long doc = subir(borrador, GUIA1, pdf("pauta.pdf", "viejo"));
        cambiar(borrador + "/" + doc, GUIA1, pdf("pauta-buena.pdf", "nuevo")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doc)).andExpect(jsonPath("$.nombreArchivo").value("pauta-buena.pdf"));
        borrar(borrador + "/" + doc, GUIA1).andExpect(status().isNoContent());
        consultar(borrador, GUIA1).andExpect(jsonPath("$.length()").value(0));

        // S-82: también con la evaluación FINALIZADA, mientras la inscripción siga en curso
        String finalizada = "/api/evaluaciones/5/documentos";
        long otro = subir(finalizada, GUIA2, pdf("pauta.pdf", "x"));
        cambiar(finalizada + "/" + otro, GUIA2, pdf("pauta-2.pdf", "y")).andExpect(status().isOk());
        borrar(finalizada + "/" + otro, GUIA2).andExpect(status().isNoContent());
    }

    @Test
    void soloElEvaluadorYConLaInscripcionEnCurso() throws Exception {
        String base = "/api/evaluaciones/4/documentos";
        long doc = subir(base, GUIA1, pdf("pauta.pdf", "x"));
        borrar(base + "/" + doc, GUIA2).andExpect(status().isForbidden());          // otro guía
        cambiar(base + "/" + doc, GUIA2, pdf("y.pdf", "y")).andExpect(status().isForbidden());
        for (String email : List.of(CAMILA, CURSO1, COORDINADOR, JEFE_CARRERA)) {
            borrar(base + "/" + doc, email).andExpect(status().isForbidden());
        }
        // evaluación 1: inscripción 14 ya aprobada. Subir sigue permitido; borrar o reemplazar no
        String cerrada = "/api/evaluaciones/1/documentos";
        long historico = subir(cerrada, GUIA1, pdf("pauta.pdf", "x"));
        borrar(cerrada + "/" + historico, GUIA1).andExpect(status().isConflict());
        cambiar(cerrada + "/" + historico, GUIA1, pdf("y.pdf", "y")).andExpect(status().isConflict());
        consultar(cerrada, GUIA1).andExpect(jsonPath("$.length()").value(1));
    }
}

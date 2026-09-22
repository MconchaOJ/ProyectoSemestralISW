package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Fase E, punto 3: documentos adjuntos de planificaciones y evaluaciones, contra la base real.
 * Seed: planificación 3 = Camila (inscripción 3, guía Marcos, curso Ricardo, AUTORIZADA).
 *       evaluación 1 = Sebastián (inscripción 14, evaluador Marcos, profesor del curso Ricardo, FINALIZADA).
 */
class DocumentosAdjuntosIntegracionTest extends IntegracionBase {

    private static final byte[] PDF_PEQUENO = "%PDF-1.4 contenido de prueba".getBytes();

    // ---------- Planificaciones ----------

    @Test
    void elDuenoSubeUnDocumentoValidoYQuienVeLaPlanificacionLoDescarga() throws Exception {
        MockMultipartFile pdf = archivo("archivo", "planificacion.pdf", "application/pdf", PDF_PEQUENO);
        JsonNode doc = cuerpo(mvc.perform(multipart("/api/planificaciones/3/documentos", CAMILA).file(pdf))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreArchivo").value("planificacion.pdf"))
                .andExpect(jsonPath("$.subidoPor.email").value(CAMILA)));
        long docId = doc.get("id").asLong();

        consultar("/api/planificaciones/3/documentos", CAMILA).andExpect(jsonPath("$.length()").value(1));
        for (String vidente : new String[] {CAMILA, GUIA1, CURSO1, COORDINADOR}) {
            consultar("/api/planificaciones/3/documentos/" + docId, vidente).andExpect(status().isOk());
        }
        // Diego no tiene relación con la inscripción 3 (es de Lorena/Trabajo Colaborativo, otra inscripción)
        consultar("/api/planificaciones/3/documentos/" + docId, DIEGO).andExpect(status().isForbidden());
        consultar("/api/planificaciones/3/documentos/" + docId, null).andExpect(status().isUnauthorized());
    }

    @Test
    void soloElDuenoDeLaPlanificacionPuedeSubirDocumentos() throws Exception {
        MockMultipartFile pdf = archivo("archivo", "planificacion.pdf", "application/pdf", PDF_PEQUENO);
        mvc.perform(multipart("/api/planificaciones/3/documentos", DIEGO).file(pdf)).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/planificaciones/3/documentos", GUIA1).file(pdf)).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/planificaciones/3/documentos", COORDINADOR).file(pdf)).andExpect(status().isForbidden());
    }

    @Test
    void rechazaUnaExtensionNoPermitidaEnPlanificacion() throws Exception {
        MockMultipartFile exe = archivo("archivo", "virus.exe", "application/octet-stream", PDF_PEQUENO);
        mvc.perform(multipart("/api/planificaciones/3/documentos", CAMILA).file(exe)).andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnArchivoMasGrandeQue10MbEnPlanificacion() throws Exception {
        byte[] grande = new byte[11 * 1024 * 1024];
        MockMultipartFile pdfGrande = archivo("archivo", "grande.pdf", "application/pdf", grande);
        mvc.perform(multipart("/api/planificaciones/3/documentos", CAMILA).file(pdfGrande)).andExpect(status().isBadRequest());
    }

    // ---------- Evaluaciones ----------

    @Test
    void elEvaluadorSubeLaPautaFormalYQuienVeLaEvaluacionLaDescarga() throws Exception {
        MockMultipartFile docx = archivo("archivo", "pauta.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", PDF_PEQUENO);
        JsonNode doc = cuerpo(mvc.perform(multipart("/api/evaluaciones/1/documentos", GUIA1).file(docx))
                .andExpect(status().isCreated()));
        long docId = doc.get("id").asLong();

        for (String vidente : new String[] {GUIA1, SEBASTIAN, CURSO1, COORDINADOR}) {
            consultar("/api/evaluaciones/1/documentos/" + docId, vidente).andExpect(status().isOk());
        }
        consultar("/api/evaluaciones/1/documentos/" + docId, DIEGO).andExpect(status().isForbidden());
    }

    @Test
    void soloElEvaluadorDeEsaEvaluacionPuedeSubirDocumentos() throws Exception {
        MockMultipartFile docx = archivo("archivo", "pauta.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", PDF_PEQUENO);
        mvc.perform(multipart("/api/evaluaciones/1/documentos", GUIA2).file(docx)).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/evaluaciones/1/documentos", SEBASTIAN).file(docx)).andExpect(status().isForbidden());
    }

    @Test
    void rechazaUnaExtensionNoPermitidaEnEvaluacion() throws Exception {
        MockMultipartFile txt = archivo("archivo", "notas.txt", "text/plain", PDF_PEQUENO);
        mvc.perform(multipart("/api/evaluaciones/1/documentos", GUIA1).file(txt)).andExpect(status().isBadRequest());
    }
}

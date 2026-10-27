package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-29: el coordinador carga la base de conocimiento y el chatbot recibe sus fragmentos pertinentes en cada mensaje
 * (SUPUESTOS S-69 y S-70). El modelo es un doble: se revisa lo que el sistema le enviaría.
 */
class HU29BaseConocimientoIntegracionTest extends ChatbotIntegracionBase {

    private static final String BASE = "/api/chatbot/base-conocimiento";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final String RUBRICA_PARABOLA = "Criterio de logro: el estudiante identifica que el coeficiente "
            + "principal determina si la parábola abre hacia arriba o hacia abajo.";
    private static final String RUBRICA_OTRO = "Criterio de redacción: la ortografía y el léxico son adecuados al nivel.";

    private ResultActions subir(String tipo, String email, MockMultipartFile archivo) throws Exception {
        return mvc.perform(multipart(BASE + "/" + tipo, email).file(archivo));
    }

    @Test
    void sinDocumentosLaListaTraeLos10TiposVaciosYElChatbotRespondeNormal() throws Exception {
        consultar(BASE, COORDINADOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[1].tipo").value("RUBRICA_PLANIFICACION"))
                .andExpect(jsonPath("$[1].cargado").value(false));

        escribir(CHAT_VALENTINA, VALENTINA, "Revisa mi planificación de la parábola.", 8).andExpect(status().isCreated());
        assertThat(solicitudes(1).get(0).instrucciones()).doesNotContain("Material de referencia")
                .contains("## Contexto del estudiante");
    }

    @Test
    void elCoordinadorSubeUnDocumentoYElChatbotUsaLoPertinenteEnSuContexto() throws Exception {   // criterios 1 y 2
        subir("RUBRICA_PLANIFICACION", COORDINADOR,
                archivo("archivo", "rubrica.docx", DOCX, docx(RUBRICA_PARABOLA, RUBRICA_OTRO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cargado").value(true))
                .andExpect(jsonPath("$.nombreArchivo").value("rubrica.docx"))
                .andExpect(jsonPath("$.subidoPor.email").value(COORDINADOR))   // criterio 3: quién y cuándo
                .andExpect(jsonPath("$.fechaSubida").isNotEmpty());
        consultar(BASE, COORDINADOR).andExpect(jsonPath("$[1].cargado").value(true))
                .andExpect(jsonPath("$[1].caracteres").value(RUBRICA_PARABOLA.length() + RUBRICA_OTRO.length() + 1));

        escribir(CHAT_VALENTINA, VALENTINA, "¿Mi explicación del coeficiente de la parábola está bien?", 8)
                .andExpect(status().isCreated());
        assertThat(solicitudes(1).get(0).instrucciones())
                .contains("## Material de referencia de la carrera")
                .contains("Rúbrica de planificación de clases (\"rubrica.docx\")")
                .contains(RUBRICA_PARABOLA)
                .contains("## Contexto del estudiante");
    }

    @Test
    void reemplazarUnTipoDejaUnSoloDocumentoYUnPdfTambienSeLee() throws Exception {
        subir("BASES_CURRICULARES_7B_2M", COORDINADOR, archivo("archivo", "v1.docx", DOCX, docx("Versión antigua")))
                .andExpect(status().isCreated());
        subir("bases_curriculares_7b_2m", COORDINADOR, archivo("archivo", "bases.pdf", "application/pdf",
                pdf("OA 3: Mostrar que comprenden la funcion cuadratica y su grafica, la parabola.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreArchivo").value("bases.pdf"));
        consultar(BASE, COORDINADOR).andExpect(jsonPath("$[5].nombreArchivo").value("bases.pdf"));

        escribir(CHAT_VALENTINA, VALENTINA, "Mi clase es sobre la función cuadrática y la parábola.", 8)
                .andExpect(status().isCreated());
        assertThat(solicitudes(1).get(0).instrucciones()).contains("OA 3: Mostrar que comprenden la funcion cuadratica")
                .doesNotContain("Versión antigua");
    }

    @Test
    void quitarUnDocumentoLoSacaDelContexto() throws Exception {
        subir("RUBRICA_PLANIFICACION", COORDINADOR, archivo("archivo", "rubrica.docx", DOCX, docx(RUBRICA_PARABOLA)))
                .andExpect(status().isCreated());
        mvc.perform(delete(BASE + "/RUBRICA_PLANIFICACION").header("Authorization", "Bearer " + token(COORDINADOR)))
                .andExpect(status().isNoContent());
        mvc.perform(delete(BASE + "/RUBRICA_PLANIFICACION").header("Authorization", "Bearer " + token(COORDINADOR)))
                .andExpect(status().isNotFound());

        escribir(CHAT_VALENTINA, VALENTINA, "¿Y la parábola?", 8).andExpect(status().isCreated());
        assertThat(solicitudes(1).get(0).instrucciones()).doesNotContain(RUBRICA_PARABOLA);
    }

    @Test
    void soloElCoordinadorVeYSubeDocumentos() throws Exception {
        for (String otro : new String[] {VALENTINA, GUIA2, CURSO2, JEFE_CARRERA, "tutor1@demo.example"}) {
            consultar(BASE, otro).andExpect(status().isForbidden());
            subir("RUBRICA_PLANIFICACION", otro, archivo("archivo", "r.docx", DOCX, docx(RUBRICA_PARABOLA)))
                    .andExpect(status().isForbidden());
        }
        consultar(BASE, COORDINADOR).andExpect(jsonPath("$[1].cargado").value(false));
    }

    @Test
    void tipoDesconocidoFormatoNoPermitidoODocumentoSinTextoDa400() throws Exception {
        subir("PRUEBA_SUS", COORDINADOR, archivo("archivo", "r.docx", DOCX, docx("x"))).andExpect(status().isBadRequest());
        subir("BANCO_COMENTARIOS", COORDINADOR, archivo("archivo", "comentarios.odt",
                "application/vnd.oasis.opendocument.text", "contenido".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest());
        subir("BANCO_COMENTARIOS", COORDINADOR, archivo("archivo", "vacio.docx", DOCX, docx("")))
                .andExpect(status().isBadRequest());
        consultar(BASE, COORDINADOR).andExpect(jsonPath("$[3].cargado").value(false));
    }

    private static byte[] docx(String... parrafos) throws Exception {
        StringBuilder cuerpo = new StringBuilder();
        for (String p : parrafos) {
            cuerpo.append("<w:p><w:r><w:t>").append(p).append("</w:t></w:r></w:p>");
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("<w:document xmlns:w=\"x\"><w:body>" + cuerpo + "</w:body></w:document>")
                    .getBytes(StandardCharsets.UTF_8));
        }
        return salida.toByteArray();
    }

    private static byte[] pdf(String texto) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        com.lowagie.text.Document documento = new com.lowagie.text.Document();
        com.lowagie.text.pdf.PdfWriter.getInstance(documento, salida);
        documento.open();
        documento.add(new com.lowagie.text.Paragraph(texto));
        documento.close();
        return salida.toByteArray();
    }
}

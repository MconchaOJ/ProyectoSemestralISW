package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

/** HU-15: el chatbot lee el texto de la planificación formal en Word (.docx). */
class TextoDocxTest {

    /** Un .docx mínimo: un ZIP con word/document.xml. */
    static byte[] docx(String cuerpoXml) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:document xmlns:w=\"x\"><w:body>" + cuerpoXml
                    + "</w:body></w:document>").getBytes(StandardCharsets.UTF_8));
        }
        return salida.toByteArray();
    }

    @Test
    void sacaLosParrafosLasCeldasYLosCaracteresEspeciales() throws IOException {
        byte[] archivo = docx("<w:p><w:r><w:t>Objetivo: resolver ecuaciones</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t xml:space=\"preserve\">Inicio </w:t></w:r><w:r><w:t>(15 min)</w:t></w:r></w:p>"
                + "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>Recurso</w:t></w:r></w:p></w:tc>"
                + "<w:tc><w:p><w:r><w:t>Guía &amp; ticket &lt;de salida&gt;</w:t></w:r></w:p></w:tc></w:tr></w:tbl>");

        String texto = TextoDocx.extraer(new ByteArrayInputStream(archivo));

        assertThat(texto).contains("Objetivo: resolver ecuaciones").contains("Inicio (15 min)")
                .contains("Recurso").contains("Guía & ticket <de salida>")
                .doesNotContain("<w:").doesNotContain("&amp;");
        assertThat(texto.lines().filter(l -> l.contains("Objetivo")).count()).isEqualTo(1);
    }

    @Test
    void unArchivoQueNoEsWordDevuelveTextoVacio() throws IOException {
        assertThat(TextoDocx.extraer(new ByteArrayInputStream("%PDF-1.4 no es un zip".getBytes()))).isEmpty();
    }
}

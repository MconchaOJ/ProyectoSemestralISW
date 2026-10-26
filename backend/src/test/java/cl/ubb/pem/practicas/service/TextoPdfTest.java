package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** HU-15: el chatbot lee el texto de un PDF (Groq solo recibe texto). */
class TextoPdfTest {

    @Test
    void sacaElTextoDeCadaPagina() throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document d = new Document();
        PdfWriter.getInstance(d, salida);
        d.open();
        d.add(new Paragraph("Objetivo: resolver ecuaciones de primer grado"));
        d.newPage();
        d.add(new Paragraph("Cierre: ticket de salida con dos ejercicios"));
        d.close();

        String texto = TextoPdf.extraer(salida.toByteArray());

        assertThat(texto).contains("Objetivo: resolver ecuaciones de primer grado")
                .contains("Cierre: ticket de salida con dos ejercicios");
    }

    @Test
    void unArchivoQueNoEsPdfFallaConIoException() {
        assertThatThrownBy(() -> TextoPdf.extraer("no es un pdf".getBytes())).isInstanceOf(IOException.class);
    }
}

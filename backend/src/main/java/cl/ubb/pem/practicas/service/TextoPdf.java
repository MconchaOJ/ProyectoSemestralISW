package cl.ubb.pem.practicas.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;

/**
 * Saca el texto de un PDF para que el chatbot lea la planificación (HU-15): Groq solo recibe texto. Usa OpenPDF, que
 * ya estaba en el proyecto por la cartola (HU-31). Un PDF escaneado (solo imágenes) no tiene texto: queda vacío y el
 * chatbot lo dice (limitación anotada en PROGRESO.md).
 */
final class TextoPdf {

    /** Más páginas que esto no es una planificación de una clase; se leen solo las primeras. */
    static final int MAXIMO_PAGINAS = 30;

    private TextoPdf() {
    }

    static String extraer(byte[] pdf) throws IOException {
        return extraer(pdf, MAXIMO_PAGINAS);
    }

    /** Con otro tope de páginas: los documentos de la base de conocimiento (HU-29) son mucho más largos. */
    static String extraer(byte[] pdf, int maximoPaginas) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder texto = new StringBuilder();
            int paginas = Math.min(lector.getNumberOfPages(), maximoPaginas);
            for (int p = 1; p <= paginas; p++) {
                texto.append(extractor.getTextFromPage(p)).append('\n');
            }
            if (lector.getNumberOfPages() > maximoPaginas) {
                texto.append("[… solo se leyeron las primeras ").append(maximoPaginas).append(" páginas]\n");
            }
            return texto.toString().replaceAll("[ \\t]+\n", "\n").replaceAll("\n{3,}", "\n\n").trim();
        } finally {
            lector.close();
        }
    }
}

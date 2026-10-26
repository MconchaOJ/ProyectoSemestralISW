package cl.ubb.pem.practicas.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Saca el texto de un Word (.docx) para que el chatbot lea la planificación formal (HU-15). Un .docx es un ZIP y el
 * texto está en word/document.xml: se toman los párrafos y las celdas de tabla y se quitan las etiquetas. No conserva
 * formato, imágenes ni ecuaciones de Word (una limitación conocida, anotada en PROGRESO.md).
 * Sin librerías nuevas a propósito: basta con java.util.zip.
 */
final class TextoDocx {

    /** Tope de lo que se descomprime (protege de un "zip bomb"): un document.xml real pesa mucho menos. */
    static final int MAXIMO_XML = 5 * 1024 * 1024;

    private TextoDocx() {
    }

    /** El texto del documento, o vacío si el archivo no es un .docx válido. */
    static String extraer(InputStream archivo) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(archivo, StandardCharsets.UTF_8)) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entrada.getName())) {
                    return aTexto(leerConTope(zip));
                }
            }
        } catch (IllegalArgumentException e) {
            // nombres de entrada mal codificados: no es un .docx que se pueda leer
        }
        return "";
    }

    private static String leerConTope(InputStream in) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            if (salida.size() + n > MAXIMO_XML) {
                throw new IOException("El documento Word es demasiado grande para leerlo.");
            }
            salida.write(buffer, 0, n);
        }
        return salida.toString(StandardCharsets.UTF_8);
    }

    static String aTexto(String xml) {
        String texto = xml
                .replaceAll("<w:tab/>", "\t")
                .replaceAll("<w:br/>|<w:cr/>", "\n")
                .replaceAll("</w:p>", "\n")
                .replaceAll("</w:tc>", " | ")
                .replaceAll("<[^>]+>", "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
                .replace("&amp;", "&");
        // sin líneas vacías repetidas ni espacios al final de cada línea
        return texto.replaceAll("[ \\t]+\n", "\n").replaceAll("\n{3,}", "\n\n").trim();
    }
}

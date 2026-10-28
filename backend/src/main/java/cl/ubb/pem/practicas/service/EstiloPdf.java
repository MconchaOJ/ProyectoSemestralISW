package cl.ubb.pem.practicas.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Colores, fuentes y piezas comunes de los PDF de los informes (HU-08) y del historial (HU-28), con el mismo aspecto
 * que la cartola (HU-31): encabezado con la universidad, tablas con títulos azules y filas alternadas, y el número de
 * página al pie.
 */
final class EstiloPdf {

    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    static final Color AZUL_UBB = new Color(0, 59, 113);
    static final Color GRIS_FILA = new Color(242, 244, 247);

    static final Font TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, AZUL_UBB);
    static final Font SECCION = new Font(Font.HELVETICA, 12, Font.BOLD, AZUL_UBB);
    static final Font SUBTITULO = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL);
    static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD);
    static final Font CABECERA = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
    static final Font CELDA = new Font(Font.HELVETICA, 8, Font.NORMAL);
    static final Font PIE = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);

    static final String INSTITUCION = "Universidad del Bío-Bío · Pedagogía en Educación Matemática";
    static final String SISTEMA = "Sistema de Formación Práctica PEM-UBB";

    private EstiloPdf() {
    }

    /** Tabla de ancho completo con su fila de títulos (que se repite en cada página). */
    static PdfPTable tabla(String[] columnas, float[] anchos) {
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.setSpacingBefore(4);
        tabla.setSpacingAfter(8);
        for (String columna : columnas) {
            PdfPCell celda = new PdfPCell(new Phrase(columna, CABECERA));
            celda.setBackgroundColor(AZUL_UBB);
            celda.setPadding(4);
            celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tabla.addCell(celda);
        }
        return tabla;
    }

    /** Fondo de la fila {@code n} (empieza en 0): blanco y gris alternados. */
    static Color fondo(int n) {
        return n % 2 == 0 ? Color.WHITE : GRIS_FILA;
    }

    static void celda(PdfPTable tabla, String texto, Color fondo, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "—" : texto, CELDA));
        celda.setBackgroundColor(fondo);
        celda.setPadding(3);
        celda.setHorizontalAlignment(alineacion);
        tabla.addCell(celda);
    }

    static Paragraph seccion(String texto) {
        Paragraph p = new Paragraph(texto, SECCION);
        p.setSpacingBefore(10);
        p.setSpacingAfter(2);
        return p;
    }

    /** Notas y promedios con coma decimal, como se escriben en Chile; un guion si no hay valor. */
    static String numero(BigDecimal valor) {
        return valor == null ? "—" : valor.toPlainString().replace('.', ',');
    }

    /** Número de página y nombre del sistema al pie de cada página. */
    static final class PieDePagina extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document documento) {
            Phrase pie = new Phrase(SISTEMA + "  ·  Página " + writer.getPageNumber(), PIE);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, pie,
                    (documento.left() + documento.right()) / 2, documento.bottom() - 20, 0);
        }
    }
}

package cl.ubb.pem.practicas.service;

import com.lowagie.text.Chunk;
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
 * Estilo común de los informes en PDF (HU-08 y HU-28), el mismo de la cartola de HU-31: azul UBB, filas alternadas,
 * títulos de columna repetidos en cada página y número de página al pie. Solo métodos estáticos.
 */
final class PdfEstilo {

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

    private PdfEstilo() {
    }

    /** Institución, título y las líneas de contexto (por ejemplo, la fecha de generación). */
    static void encabezado(Document documento, String titulo, String... lineas) {
        documento.add(new Paragraph("Universidad del Bío-Bío · Pedagogía en Educación Matemática", SUBTITULO));
        documento.add(new Paragraph(titulo, TITULO));
        for (String linea : lineas) {
            documento.add(new Paragraph(linea, SUBTITULO));
        }
        documento.add(Chunk.NEWLINE);
    }

    /** Título de una sección, con un poco de aire arriba. */
    static void seccion(Document documento, String titulo) {
        Paragraph p = new Paragraph(titulo, SECCION);
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        documento.add(p);
    }

    /** Tabla al 100 % del ancho, con la fila de títulos en azul que se repite en cada página. */
    static PdfPTable tabla(String[] columnas, float[] anchos) {
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.setSpacingBefore(6);   // sin esto la línea anterior queda montada sobre la fila de títulos
        tabla.setSpacingAfter(6);
        for (String columna : columnas) {
            PdfPCell celda = new PdfPCell(new Phrase(columna, CABECERA));
            celda.setBackgroundColor(AZUL_UBB);
            celda.setPadding(4);
            celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tabla.addCell(celda);
        }
        return tabla;
    }

    /** Agrega una fila completa; la primera columna a la izquierda y las demás centradas. */
    static void fila(PdfPTable tabla, boolean alterna, String... valores) {
        Color fondo = alterna ? GRIS_FILA : Color.WHITE;
        for (int i = 0; i < valores.length; i++) {
            PdfPCell celda = new PdfPCell(new Phrase(valores[i] == null ? "—" : valores[i], CELDA));
            celda.setBackgroundColor(fondo);
            celda.setPadding(3);
            celda.setHorizontalAlignment(i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            tabla.addCell(celda);
        }
    }

    /** Números con coma decimal, como se escriben en Chile; un guion si no hay valor. */
    static String numero(BigDecimal valor) {
        return valor == null ? "—" : valor.toPlainString().replace('.', ',');
    }

    static String numero(Integer valor) {
        return valor == null ? "—" : valor.toString();
    }

    /** Nombre legible de un estado (de inscripción, planificación o evaluación), como en la página. */
    static String estado(Enum<?> estado) {
        if (estado == null) {
            return "—";
        }
        return switch (estado.name()) {
            case "EN_CURSO" -> "En curso";
            case "APROBADA" -> "Aprobada";
            case "REPROBADA" -> "Reprobada";
            case "BORRADOR" -> "Borrador";
            case "EN_REVISION_GUIA" -> "En revisión del guía";
            case "CON_OBSERVACIONES" -> "Con observaciones";
            case "EN_AUTORIZACION" -> "En autorización";
            case "AUTORIZADA" -> "Autorizada";
            case "FINALIZADA" -> "Finalizada";
            default -> estado.name();
        };
    }

    /** Número de página y nombre del sistema al pie de cada página. */
    static final class PieDePagina extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document documento) {
            Phrase pie = new Phrase("Sistema de Formación Práctica PEM-UBB  ·  Página " + writer.getPageNumber(), PIE);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, pie,
                    (documento.left() + documento.right()) / 2, documento.bottom() - 20, 0);
        }
    }
}

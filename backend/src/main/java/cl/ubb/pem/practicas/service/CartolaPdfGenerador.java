package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.CartolaFilaDto;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Dibuja la cartola de prácticas (HU-31) en PDF con OpenPDF, al estilo de una cartola bancaria: encabezado con el
 * rango y la fecha de generación, y una fila por inscripción cerrada. Página A4 horizontal; si la tabla ocupa más
 * de una página, el encabezado de columnas se repite y cada página lleva su número al pie.
 */
@Component
public class CartolaPdfGenerador {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final String[] COLUMNAS = {"Estudiante", "RUT", "Asignatura", "Periodo", "Profesor guía",
            "Profesor del curso", "Estado", "Fecha de cierre", "Nota de cierre", "Evaluaciones finalizadas",
            "Promedio evaluaciones"};
    private static final float[] ANCHOS = {15, 8, 13, 6, 12, 12, 8, 7.5f, 6, 7.5f, 7.5f};
    private static final Color AZUL_UBB = new Color(0, 59, 113);
    private static final Color GRIS_FILA = new Color(242, 244, 247);

    private static final Font TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, AZUL_UBB);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font CABECERA = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
    private static final Font CELDA = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static final Font PIE = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);

    private final Clock reloj;

    public CartolaPdfGenerador(Clock reloj) {
        this.reloj = reloj;
    }

    public byte[] generar(LocalDate desde, LocalDate hasta, List<CartolaFilaDto> filas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 28, 28, 28, 40);
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new PieDePagina());
            documento.addTitle("Cartola de prácticas " + desde + " a " + hasta);
            documento.addAuthor("Sistema de Formación Práctica PEM-UBB");
            documento.open();
            encabezado(documento, desde, hasta, filas.size());
            if (filas.isEmpty()) {
                documento.add(new Paragraph("No hay inscripciones cerradas en este rango de fechas.", NORMAL));
            } else {
                documento.add(tabla(filas));
            }
            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar la cartola en PDF.", e);
        }
        return salida.toByteArray();
    }

    private void encabezado(Document documento, LocalDate desde, LocalDate hasta, int cantidad) {
        documento.add(new Paragraph("Universidad del Bío-Bío · Pedagogía en Educación Matemática", SUBTITULO));
        documento.add(new Paragraph("Cartola de prácticas", TITULO));
        documento.add(new Paragraph("Inscripciones cerradas entre el " + desde.format(FECHA) + " y el "
                + hasta.format(FECHA) + " (ambos incluidos)", NORMAL));
        documento.add(new Paragraph("Generada el " + LocalDateTime.now(reloj).format(FECHA_HORA)
                + "  ·  Total: " + cantidad + (cantidad == 1 ? " inscripción" : " inscripciones"), SUBTITULO));
        documento.add(Chunk.NEWLINE);
    }

    private PdfPTable tabla(List<CartolaFilaDto> filas) {
        PdfPTable tabla = new PdfPTable(ANCHOS);
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);   // repite los títulos de columna en cada página
        for (String columna : COLUMNAS) {
            PdfPCell celda = new PdfPCell(new Phrase(columna, CABECERA));
            celda.setBackgroundColor(AZUL_UBB);
            celda.setPadding(4);
            celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tabla.addCell(celda);
        }
        boolean alterna = false;
        for (CartolaFilaDto f : filas) {
            Color fondo = alterna ? GRIS_FILA : Color.WHITE;
            celda(tabla, f.estudiante(), fondo, Element.ALIGN_LEFT);
            celda(tabla, f.rut(), fondo, Element.ALIGN_LEFT);
            celda(tabla, f.asignatura(), fondo, Element.ALIGN_LEFT);
            celda(tabla, f.periodo(), fondo, Element.ALIGN_CENTER);
            celda(tabla, f.profesorGuia() == null ? "Sin guía" : f.profesorGuia(), fondo, Element.ALIGN_LEFT);
            celda(tabla, f.profesorCurso(), fondo, Element.ALIGN_LEFT);
            celda(tabla, f.estado().name(), fondo, Element.ALIGN_CENTER);
            celda(tabla, f.fechaCierre().format(FECHA), fondo, Element.ALIGN_CENTER);
            celda(tabla, numero(f.notaFinal()), fondo, Element.ALIGN_CENTER);
            celda(tabla, String.valueOf(f.evaluacionesFinalizadas()), fondo, Element.ALIGN_CENTER);
            celda(tabla, numero(f.promedioEvaluaciones()), fondo, Element.ALIGN_CENTER);
            alterna = !alterna;
        }
        return tabla;
    }

    private static void celda(PdfPTable tabla, String texto, Color fondo, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, CELDA));
        celda.setBackgroundColor(fondo);
        celda.setPadding(3);
        celda.setHorizontalAlignment(alineacion);
        tabla.addCell(celda);
    }

    /** Notas con coma decimal, como se escriben en Chile; un guion si no hay valor. */
    private static String numero(BigDecimal valor) {
        return valor == null ? "—" : valor.toPlainString().replace('.', ',');
    }

    /** Número de página y nombre del sistema al pie de cada página. */
    private static final class PieDePagina extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document documento) {
            Phrase pie = new Phrase("Sistema de Formación Práctica PEM-UBB  ·  Página " + writer.getPageNumber(), PIE);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, pie,
                    (documento.left() + documento.right()) / 2, documento.bottom() - 20, 0);
        }
    }
}

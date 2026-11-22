package cl.ubb.pem.practicas.service;

import static cl.ubb.pem.practicas.service.PdfEstilo.FECHA;
import static cl.ubb.pem.practicas.service.PdfEstilo.FECHA_HORA;
import static cl.ubb.pem.practicas.service.PdfEstilo.NEGRITA;
import static cl.ubb.pem.practicas.service.PdfEstilo.NORMAL;
import static cl.ubb.pem.practicas.service.PdfEstilo.SUBTITULO;
import static cl.ubb.pem.practicas.service.PdfEstilo.estado;
import static cl.ubb.pem.practicas.service.PdfEstilo.fila;
import static cl.ubb.pem.practicas.service.PdfEstilo.numero;

import cl.ubb.pem.practicas.dto.EstudianteDto;
import cl.ubb.pem.practicas.dto.EvaluacionResumenDto;
import cl.ubb.pem.practicas.dto.HistorialDto;
import cl.ubb.pem.practicas.dto.HistorialDto.InscripcionHistorialDto;
import cl.ubb.pem.practicas.dto.PlanificacionResumenDto;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * HU-28 y criterio 1 de HU-08: el historial del estudiante en PDF, con lo mismo que la pantalla de historial
 * (inscripciones en orden, cada una con sus evaluaciones finalizadas y sus planificaciones) y la fecha de generación.
 * Recibe el {@link HistorialDto} ya armado y con el acceso ya comprobado por {@link HistorialService}.
 */
@Component
public class HistorialPdfGenerador {

    private static final String[] COLUMNAS_EVALUACION = {"Fecha de la clase", "Pauta", "Establecimiento y curso",
            "Puntaje total", "Promedio"};
    private static final float[] ANCHOS_EVALUACION = {12, 40, 26, 10, 10};
    private static final String[] COLUMNAS_PLANIFICACION = {"Título", "Fecha de la clase", "Estado"};
    private static final float[] ANCHOS_PLANIFICACION = {56, 18, 26};

    private final Clock reloj;

    public HistorialPdfGenerador(Clock reloj) {
        this.reloj = reloj;
    }

    public byte[] generar(HistorialDto h) {
        EstudianteDto e = h.estudiante();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 36, 44);
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new PdfEstilo.PieDePagina());
            documento.addTitle("Historial de prácticas de " + e.nombre() + " " + e.apellidos());
            documento.addAuthor("Sistema de Formación Práctica PEM-UBB");
            documento.open();
            PdfEstilo.encabezado(documento, "Historial de prácticas",
                    e.nombre() + " " + e.apellidos() + "  ·  RUT " + e.rut() + "  ·  " + e.email(),
                    "Semestre actual: " + numero(e.semestreActual()) + "  ·  Año de ingreso: " + numero(e.anioIngreso())
                            + "  ·  Asignaturas aprobadas: " + h.asignaturasAprobadas().size() + " de 6",
                    "Generado el " + LocalDateTime.now(reloj).format(FECHA_HORA));
            List<InscripcionHistorialDto> recorrido = h.inscripciones().stream()
                    .sorted(Comparator.comparing(InscripcionHistorialDto::periodo)
                            .thenComparing(i -> i.asignatura().orden()))
                    .toList();
            if (recorrido.isEmpty()) {
                documento.add(new Paragraph("El estudiante todavía no tiene inscripciones.", NORMAL));
            }
            for (InscripcionHistorialDto i : recorrido) {
                inscripcion(documento, i);
            }
            documento.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el historial en PDF.", ex);
        }
        return salida.toByteArray();
    }

    private void inscripcion(Document documento, InscripcionHistorialDto i) {
        PdfEstilo.seccion(documento, i.asignatura().orden() + ". " + i.asignatura().nombre() + "  ·  " + i.periodo());
        documento.add(new Paragraph("Estado: " + estado(i.estado()) + "  ·  Nota final: " + numero(i.notaFinal())
                + "  ·  Profesor guía: " + (i.profesorGuia() == null ? "no lleva" : i.profesorGuia().nombreCompleto())
                + "  ·  Profesor del curso: " + i.profesorCurso().nombreCompleto(), SUBTITULO));

        documento.add(new Paragraph("Evaluaciones finalizadas (" + i.evaluaciones().size() + ")", NEGRITA));
        if (i.evaluaciones().isEmpty()) {
            documento.add(new Paragraph("Sin evaluaciones finalizadas.", SUBTITULO));
        } else {
            PdfPTable t = PdfEstilo.tabla(COLUMNAS_EVALUACION, ANCHOS_EVALUACION);
            boolean alterna = false;
            for (EvaluacionResumenDto ev : i.evaluaciones()) {
                fila(t, alterna, fecha(ev.fechaClase()), ev.pautaNombre(),
                        unir(ev.establecimiento(), ev.curso()), numero(ev.puntajeTotal()), numero(ev.promedio()));
                alterna = !alterna;
            }
            documento.add(t);
        }

        if (!i.asignatura().requierePlanificacion()) {
            return;   // Aproximación al Sistema Escolar no entrega planificaciones
        }
        documento.add(new Paragraph("Planificaciones (" + i.planificaciones().size() + ")", NEGRITA));
        if (i.planificaciones().isEmpty()) {
            documento.add(new Paragraph("Sin planificaciones.", SUBTITULO));
        } else {
            PdfPTable t = PdfEstilo.tabla(COLUMNAS_PLANIFICACION, ANCHOS_PLANIFICACION);
            boolean alterna = false;
            for (PlanificacionResumenDto p : i.planificaciones()) {
                fila(t, alterna, p.titulo(), fecha(p.fechaClase()), estado(p.estado()));
                alterna = !alterna;
            }
            documento.add(t);
        }
    }

    private static String fecha(LocalDate d) {
        return d == null ? "—" : d.format(FECHA);
    }

    private static String unir(String establecimiento, String curso) {
        if (establecimiento == null && curso == null) {
            return "—";
        }
        return establecimiento == null ? curso : curso == null ? establecimiento : establecimiento + " · " + curso;
    }
}

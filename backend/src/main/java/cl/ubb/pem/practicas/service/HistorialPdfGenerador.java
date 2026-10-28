package cl.ubb.pem.practicas.service;

import static cl.ubb.pem.practicas.service.EstiloPdf.FECHA;
import static cl.ubb.pem.practicas.service.EstiloPdf.FECHA_HORA;
import static cl.ubb.pem.practicas.service.EstiloPdf.NEGRITA;
import static cl.ubb.pem.practicas.service.EstiloPdf.NORMAL;
import static cl.ubb.pem.practicas.service.EstiloPdf.SUBTITULO;
import static cl.ubb.pem.practicas.service.EstiloPdf.TITULO;
import static cl.ubb.pem.practicas.service.EstiloPdf.celda;
import static cl.ubb.pem.practicas.service.EstiloPdf.fondo;
import static cl.ubb.pem.practicas.service.EstiloPdf.numero;
import static cl.ubb.pem.practicas.service.EstiloPdf.seccion;
import static cl.ubb.pem.practicas.service.EstiloPdf.tabla;

import cl.ubb.pem.practicas.dto.EquipoResumenDto;
import cl.ubb.pem.practicas.dto.EstudianteDto;
import cl.ubb.pem.practicas.dto.EvaluacionResumenDto;
import cl.ubb.pem.practicas.dto.HistorialDto;
import cl.ubb.pem.practicas.dto.HistorialDto.AsignaturaAprobadaDto;
import cl.ubb.pem.practicas.dto.HistorialDto.InscripcionHistorialDto;
import cl.ubb.pem.practicas.dto.PlanificacionResumenDto;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Modalidad;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Dibuja el historial de un estudiante (HU-28) en PDF: lo mismo que la pantalla de historial (HU-05), es decir sus
 * inscripciones en orden cronológico con sus evaluaciones finalizadas y sus planificaciones, más una síntesis por
 * inscripción y la fecha de generación. Es también el informe por estudiante en PDF de HU-08 (S-76).
 */
@Component
public class HistorialPdfGenerador {

    private static final String[] COLUMNAS_EVALUACIONES = {"Fecha de la clase", "Pauta", "Establecimiento", "Curso",
            "Puntaje", "Promedio"};
    private static final float[] ANCHOS_EVALUACIONES = {10, 38, 22, 10, 9, 9};
    private static final String[] COLUMNAS_PLANIFICACIONES = {"Título", "Fecha de la clase", "Estado"};
    private static final float[] ANCHOS_PLANIFICACIONES = {60, 15, 20};
    private static final String[] COLUMNAS_APROBADAS = {"Asignatura", "Periodo", "Nota final"};
    private static final float[] ANCHOS_APROBADAS = {60, 15, 15};

    private final Clock reloj;

    public HistorialPdfGenerador(Clock reloj) {
        this.reloj = reloj;
    }

    public byte[] generar(HistorialDto historial) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 36, 44);
        EstudianteDto e = historial.estudiante();
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new EstiloPdf.PieDePagina());
            documento.addTitle("Historial de prácticas de " + e.nombre() + " " + e.apellidos());
            documento.addAuthor(EstiloPdf.SISTEMA);
            documento.open();
            encabezado(documento, historial);
            aprobadas(documento, historial.asignaturasAprobadas());
            documento.add(seccion("Recorrido por periodo"));
            if (historial.inscripciones().isEmpty()) {
                documento.add(new Paragraph("Este estudiante aún no tiene inscripciones.", NORMAL));
            }
            for (InscripcionHistorialDto i : historial.inscripciones()) {
                inscripcion(documento, i);
            }
            documento.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el historial en PDF.", ex);
        }
        return salida.toByteArray();
    }

    private void encabezado(Document documento, HistorialDto h) {
        EstudianteDto e = h.estudiante();
        long enCurso = h.inscripciones().stream().filter(i -> i.estado() == EstadoInscripcion.EN_CURSO).count();
        documento.add(new Paragraph(EstiloPdf.INSTITUCION, SUBTITULO));
        documento.add(new Paragraph("Historial de prácticas", TITULO));
        documento.add(new Paragraph(e.nombre() + " " + e.apellidos(), NEGRITA));
        documento.add(new Paragraph("RUT " + e.rut() + "  ·  " + e.email() + "  ·  Semestre actual " + e.semestreActual()
                + "  ·  Ingreso " + e.anioIngreso(), NORMAL));
        documento.add(new Paragraph("Prácticas aprobadas: " + h.asignaturasAprobadas().size() + " de 6  ·  En curso: "
                + enCurso, NORMAL));
        documento.add(new Paragraph("Generado el " + LocalDateTime.now(reloj).format(FECHA_HORA), SUBTITULO));
        documento.add(Chunk.NEWLINE);
    }

    private static void aprobadas(Document documento, List<AsignaturaAprobadaDto> aprobadas) {
        if (aprobadas.isEmpty()) {
            return;
        }
        documento.add(seccion("Asignaturas aprobadas"));
        PdfPTable t = tabla(COLUMNAS_APROBADAS, ANCHOS_APROBADAS);
        for (int n = 0; n < aprobadas.size(); n++) {
            AsignaturaAprobadaDto a = aprobadas.get(n);
            celda(t, a.nombre(), fondo(n), Element.ALIGN_LEFT);
            celda(t, a.periodo(), fondo(n), Element.ALIGN_CENTER);
            celda(t, numero(a.notaFinal()), fondo(n), Element.ALIGN_CENTER);
        }
        documento.add(t);
    }

    private static void inscripcion(Document documento, InscripcionHistorialDto i) {
        Paragraph titulo = new Paragraph(i.asignatura().orden() + ". " + i.asignatura().nombre() + " (" + i.periodo()
                + ")", NEGRITA);
        titulo.setSpacingBefore(8);
        documento.add(titulo);
        documento.add(new Paragraph("Estado: " + i.estado().name()
                + (i.notaFinal() == null ? "" : "  ·  Nota final: " + numero(i.notaFinal()))
                + "  ·  Profesor guía: " + (i.profesorGuia() == null ? "no lleva" : i.profesorGuia().nombreCompleto())
                + "  ·  Profesor del curso: " + (i.profesorCurso() == null ? "—" : i.profesorCurso().nombreCompleto()),
                NORMAL));
        if (i.equipo() != null) {
            documento.add(new Paragraph(equipo(i), NORMAL));
        }
        SintesisInscripcion s = SintesisInscripcion.de(i);
        documento.add(new Paragraph("Síntesis: " + s.evaluaciones() + " evaluaciones finalizadas"
                + (s.promedioEvaluaciones() == null ? "" : " (promedio " + numero(s.promedioEvaluaciones()) + ")")
                + "  ·  " + s.planificaciones() + " planificaciones (" + s.planificacionesAutorizadas()
                + " autorizadas)", SUBTITULO));

        if (!i.evaluaciones().isEmpty()) {
            PdfPTable t = tabla(COLUMNAS_EVALUACIONES, ANCHOS_EVALUACIONES);
            for (int n = 0; n < i.evaluaciones().size(); n++) {
                EvaluacionResumenDto ev = i.evaluaciones().get(n);
                celda(t, fecha(ev.fechaClase()), fondo(n), Element.ALIGN_CENTER);
                celda(t, ev.pautaNombre(), fondo(n), Element.ALIGN_LEFT);
                celda(t, ev.establecimiento(), fondo(n), Element.ALIGN_LEFT);
                celda(t, ev.curso(), fondo(n), Element.ALIGN_LEFT);
                celda(t, ev.puntajeTotal() == null ? null : String.valueOf(ev.puntajeTotal()), fondo(n),
                        Element.ALIGN_CENTER);
                celda(t, numero(ev.promedio()), fondo(n), Element.ALIGN_CENTER);
            }
            documento.add(t);
        }
        if (!i.planificaciones().isEmpty()) {
            PdfPTable t = tabla(COLUMNAS_PLANIFICACIONES, ANCHOS_PLANIFICACIONES);
            for (int n = 0; n < i.planificaciones().size(); n++) {
                PlanificacionResumenDto p = i.planificaciones().get(n);
                celda(t, p.titulo(), fondo(n), Element.ALIGN_LEFT);
                celda(t, fecha(p.fechaClase()), fondo(n), Element.ALIGN_CENTER);
                celda(t, p.estado().name(), fondo(n), Element.ALIGN_CENTER);
            }
            documento.add(t);
        }
        if (i.evaluaciones().isEmpty() && i.planificaciones().isEmpty()) {
            documento.add(new Paragraph(i.asignatura().requierePlanificacion()
                    ? "Sin evaluaciones ni planificaciones registradas." : "Esta práctica no entrega planificaciones.",
                    SUBTITULO));
        }
    }

    /** HU-10, criterio 3: "Trabajó en dupla con: Ana Soto · Liceo X, 1° Medio B". */
    static String equipo(InscripcionHistorialDto i) {
        EquipoResumenDto e = i.equipo();
        String tipo = i.asignatura().modalidad() == Modalidad.GRUPO ? "en grupo" : "en dupla";
        String companeros = e.companeros().stream().map(EquipoResumenDto.CompaneroDto::nombreCompleto)
                .collect(Collectors.joining(", "));
        String lugar = e.establecimiento() == null ? ""
                : "  ·  " + e.establecimiento() + (e.curso() == null ? "" : ", " + e.curso());
        return "Trabajó " + tipo + " con: " + (companeros.isEmpty() ? "—" : companeros) + lugar;
    }

    private static String fecha(LocalDate d) {
        return d == null ? null : d.format(FECHA);
    }
}

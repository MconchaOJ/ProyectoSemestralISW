package cl.ubb.pem.practicas.service;

import static cl.ubb.pem.practicas.service.PdfEstilo.FECHA_HORA;
import static cl.ubb.pem.practicas.service.PdfEstilo.NEGRITA;
import static cl.ubb.pem.practicas.service.PdfEstilo.SUBTITULO;
import static cl.ubb.pem.practicas.service.PdfEstilo.fila;
import static cl.ubb.pem.practicas.service.PdfEstilo.numero;

import cl.ubb.pem.practicas.dto.InformeAsignaturaDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.FilaEstudiante;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.Notas;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.PorDimension;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.PorPauta;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.Tramo;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * HU-08, criterio 3: el informe de una asignatura y período en PDF (A4 vertical), con las mismas secciones que la
 * pantalla "Informes": inscripciones, notas finales, evaluaciones por pauta, planificaciones y una fila por estudiante.
 */
@Component
public class InformeAsignaturaPdfGenerador {

    private static final String[] COLUMNAS_TRAMOS = {"Tramo", "Cantidad"};
    private static final float[] ANCHOS_TRAMOS = {70, 30};
    private static final String[] COLUMNAS_ESTUDIANTES = {"Estudiante", "RUT", "Estado", "Nota final",
            "Evaluaciones", "Promedio evaluaciones", "Planificaciones (autorizadas)"};
    private static final float[] ANCHOS_ESTUDIANTES = {25, 12, 10, 9, 14, 13, 17};

    public byte[] generar(InformeAsignaturaDto inf) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 36, 44);
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new PdfEstilo.PieDePagina());
            documento.addTitle("Informe de " + inf.asignatura() + " " + inf.periodo());
            documento.addAuthor("Sistema de Formación Práctica PEM-UBB");
            documento.open();
            PdfEstilo.encabezado(documento, "Informe de " + inf.asignatura(),
                    "Período " + inf.periodo() + "  ·  Código " + inf.asignaturaCodigo(),
                    "Generado el " + inf.generado().format(FECHA_HORA));

            var ins = inf.inscripciones();
            PdfEstilo.seccion(documento, "Inscripciones");
            documento.add(new Paragraph("Total: " + ins.total() + "  ·  En curso: " + ins.enCurso() + "  ·  Aprobadas: "
                    + ins.aprobadas() + "  ·  Reprobadas: " + ins.reprobadas(), PdfEstilo.NORMAL));

            PdfEstilo.seccion(documento, "Notas finales");
            notas(documento, inf.notasFinales(), "Todavía no hay notas finales (las prácticas en curso no tienen nota).");

            PdfEstilo.seccion(documento, "Evaluaciones finalizadas por pauta");
            if (inf.evaluaciones().isEmpty()) {
                documento.add(new Paragraph("No hay evaluaciones finalizadas en este período.", SUBTITULO));
            }
            for (PorPauta p : inf.evaluaciones()) {
                pauta(documento, p);
            }

            var pl = inf.planificaciones();
            PdfEstilo.seccion(documento, "Planificaciones");
            documento.add(new Paragraph("Total: " + pl.total() + "  ·  Autorizadas: " + pl.autorizadas()
                    + "  ·  En autorización: " + pl.enAutorizacion() + "  ·  En revisión del guía: " + pl.enRevisionGuia()
                    + "  ·  Con observaciones: " + pl.conObservaciones() + "  ·  Borrador: " + pl.borrador(), PdfEstilo.NORMAL));
            documento.add(new Paragraph("Devueltas con observaciones al menos una vez: " + pl.devueltasAlMenosUnaVez(),
                    PdfEstilo.NORMAL));

            PdfEstilo.seccion(documento, "Estudiantes (" + inf.estudiantes().size() + ")");
            if (inf.estudiantes().isEmpty()) {
                documento.add(new Paragraph("No hay estudiantes inscritos en este período.", SUBTITULO));
            } else {
                PdfPTable t = PdfEstilo.tabla(COLUMNAS_ESTUDIANTES, ANCHOS_ESTUDIANTES);
                boolean alterna = false;
                for (FilaEstudiante f : inf.estudiantes()) {
                    fila(t, alterna, f.estudiante(), f.rut(), PdfEstilo.estado(estadoInscripcion(f.estado())),
                            numero(f.notaFinal()), String.valueOf(f.evaluacionesFinalizadas()),
                            numero(f.promedioEvaluaciones()),
                            f.planificaciones() + " (" + f.planificacionesAutorizadas() + ")");
                    alterna = !alterna;
                }
                documento.add(t);
            }
            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el informe en PDF.", e);
        }
        return salida.toByteArray();
    }

    private static void pauta(Document documento, PorPauta p) {
        Paragraph titulo = new Paragraph(p.pauta() + (p.version() > 1 ? " (versión " + p.version() + ")" : ""), NEGRITA);
        titulo.setSpacingBefore(6);
        documento.add(titulo);
        documento.add(new Paragraph("Evaluador: " + evaluador(p.tipoEvaluador()) + "  ·  Evaluaciones: " + p.evaluaciones()
                + "  ·  Puntaje promedio: " + numero(p.puntajePromedio()) + " de " + p.puntajeMaximo()
                + "  ·  Logro: " + numero(p.porcentajeLogro()) + " %", SUBTITULO));
        notas(documento, p.promedios(), "Sin promedios.");
        if (!p.dimensiones().isEmpty()) {
            PdfPTable t = PdfEstilo.tabla(new String[] {"Dimensión", "Puntaje promedio por indicador"}, new float[] {70, 30});
            boolean alterna = false;
            for (PorDimension d : p.dimensiones()) {
                fila(t, alterna, d.dimension(), numero(d.promedioPorIndicador()));
                alterna = !alterna;
            }
            documento.add(t);
        }
    }

    /** Promedio, mínimo y máximo, y la tabla de tramos. */
    private static void notas(Document documento, Notas n, String siVacio) {
        if (n.cantidad() == 0) {
            documento.add(new Paragraph(siVacio, SUBTITULO));
            return;
        }
        documento.add(new Paragraph("Cantidad: " + n.cantidad() + "  ·  Promedio: " + numero(n.promedio())
                + "  ·  Mínimo: " + numero(n.minimo()) + "  ·  Máximo: " + numero(n.maximo()), PdfEstilo.NORMAL));
        PdfPTable t = PdfEstilo.tabla(COLUMNAS_TRAMOS, ANCHOS_TRAMOS);
        boolean alterna = false;
        List<Tramo> tramos = n.tramos();
        for (Tramo tr : tramos) {
            fila(t, alterna, tr.tramo(), String.valueOf(tr.cantidad()));
            alterna = !alterna;
        }
        documento.add(t);
    }

    private static String evaluador(String tipo) {
        return switch (tipo) {
            case "COLABORADOR" -> "profesor/a colaborador/a";
            case "TUTOR" -> "tutor/a";
            case "ACADEMICO" -> "académico/a";
            default -> tipo;
        };
    }

    private static cl.ubb.pem.practicas.entity.EstadoInscripcion estadoInscripcion(String estado) {
        return cl.ubb.pem.practicas.entity.EstadoInscripcion.valueOf(estado);
    }
}

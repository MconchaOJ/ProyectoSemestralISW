package cl.ubb.pem.practicas.service;

import static cl.ubb.pem.practicas.service.EstiloPdf.FECHA_HORA;
import static cl.ubb.pem.practicas.service.EstiloPdf.NORMAL;
import static cl.ubb.pem.practicas.service.EstiloPdf.SUBTITULO;
import static cl.ubb.pem.practicas.service.EstiloPdf.TITULO;
import static cl.ubb.pem.practicas.service.EstiloPdf.celda;
import static cl.ubb.pem.practicas.service.EstiloPdf.fondo;
import static cl.ubb.pem.practicas.service.EstiloPdf.numero;
import static cl.ubb.pem.practicas.service.EstiloPdf.seccion;
import static cl.ubb.pem.practicas.service.EstiloPdf.tabla;

import cl.ubb.pem.practicas.dto.InformeAsignaturaDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.FilaEstudianteDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.PautaUsadaDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.ResumenDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.TramoDto;
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
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Dibuja el informe de una asignatura en un periodo (HU-08, criterio 2) en PDF: resumen, distribución de notas
 * finales, una tabla por pauta usada con la distribución de sus puntajes y una fila por estudiante. A4 horizontal.
 */
@Component
public class InformeAsignaturaPdfGenerador {

    private static final String[] COLUMNAS_RESUMEN = {"Inscripciones", "En curso", "Aprobadas", "Reprobadas",
            "Promedio notas finales", "Evaluaciones finalizadas", "Promedio evaluaciones", "Planificaciones",
            "Autorizadas"};
    private static final String[] COLUMNAS_PAUTAS = {"Pauta", "Versión", "Puntaje máximo", "Evaluaciones",
            "Puntaje promedio", "Mínimo", "Más alto", "Promedio"};
    private static final float[] ANCHOS_PAUTAS = {40, 7, 9, 9, 9, 7, 7, 8};
    private static final String[] COLUMNAS_ESTUDIANTES = {"Estudiante", "RUT", "Profesor guía", "Estado",
            "Nota final", "Evaluaciones finalizadas", "Promedio evaluaciones", "Planificaciones", "Autorizadas"};
    private static final float[] ANCHOS_ESTUDIANTES = {20, 10, 17, 10, 7, 9, 9, 9, 8};

    private final Clock reloj;

    public InformeAsignaturaPdfGenerador(Clock reloj) {
        this.reloj = reloj;
    }

    public byte[] generar(InformeAsignaturaDto informe) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 28, 28, 28, 40);
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new EstiloPdf.PieDePagina());
            documento.addTitle("Informe de " + informe.asignatura().nombre() + " " + informe.periodo());
            documento.addAuthor(EstiloPdf.SISTEMA);
            documento.open();
            documento.add(new Paragraph(EstiloPdf.INSTITUCION, SUBTITULO));
            documento.add(new Paragraph("Informe por asignatura", TITULO));
            documento.add(new Paragraph(informe.asignatura().nombre() + " (" + informe.asignatura().codigo()
                    + ")  ·  Periodo " + informe.periodo(), NORMAL));
            documento.add(new Paragraph("Generado el " + LocalDateTime.now(reloj).format(FECHA_HORA)
                    + "  ·  Solo cuenta evaluaciones finalizadas", SUBTITULO));
            documento.add(Chunk.NEWLINE);

            if (informe.estudiantes().isEmpty()) {
                documento.add(new Paragraph("No hay inscripciones en esta asignatura en el periodo "
                        + informe.periodo() + ".", NORMAL));
            } else {
                resumen(documento, informe.resumen());
                distribucionNotas(documento, informe.distribucionNotasFinales());
                pautas(documento, informe.pautas());
                estudiantes(documento, informe.estudiantes());
            }
            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el informe en PDF.", e);
        }
        return salida.toByteArray();
    }

    private static void resumen(Document documento, ResumenDto r) {
        documento.add(seccion("Resumen"));
        float[] anchos = new float[COLUMNAS_RESUMEN.length];
        java.util.Arrays.fill(anchos, 1);
        PdfPTable t = tabla(COLUMNAS_RESUMEN, anchos);
        for (String valor : new String[]{String.valueOf(r.inscripciones()), String.valueOf(r.enCurso()),
                String.valueOf(r.aprobadas()), String.valueOf(r.reprobadas()), numero(r.promedioNotaFinal()),
                String.valueOf(r.evaluacionesFinalizadas()), numero(r.promedioEvaluaciones()),
                String.valueOf(r.planificaciones()), String.valueOf(r.planificacionesAutorizadas())}) {
            celda(t, valor, fondo(0), Element.ALIGN_CENTER);
        }
        documento.add(t);
    }

    private static void distribucionNotas(Document documento, List<TramoDto> tramos) {
        documento.add(seccion("Distribución de notas finales"));
        documento.add(tablaTramos(tramos, "Inscripciones con nota"));
    }

    private static void pautas(Document documento, List<PautaUsadaDto> pautas) {
        documento.add(seccion("Puntajes por pauta"));
        if (pautas.isEmpty()) {
            documento.add(new Paragraph("No hay evaluaciones finalizadas en este periodo.", NORMAL));
            return;
        }
        PdfPTable t = tabla(COLUMNAS_PAUTAS, ANCHOS_PAUTAS);
        for (int n = 0; n < pautas.size(); n++) {
            PautaUsadaDto p = pautas.get(n);
            celda(t, p.nombre(), fondo(n), Element.ALIGN_LEFT);
            celda(t, String.valueOf(p.version()), fondo(n), Element.ALIGN_CENTER);
            celda(t, String.valueOf(p.puntajeMaximo()), fondo(n), Element.ALIGN_CENTER);
            celda(t, String.valueOf(p.evaluaciones()), fondo(n), Element.ALIGN_CENTER);
            celda(t, numero(p.puntajePromedio()), fondo(n), Element.ALIGN_CENTER);
            celda(t, p.puntajeMinimo() == null ? null : String.valueOf(p.puntajeMinimo()), fondo(n), Element.ALIGN_CENTER);
            celda(t, p.puntajeMasAlto() == null ? null : String.valueOf(p.puntajeMasAlto()), fondo(n), Element.ALIGN_CENTER);
            celda(t, numero(p.promedio()), fondo(n), Element.ALIGN_CENTER);
        }
        documento.add(t);
        for (PautaUsadaDto p : pautas) {
            documento.add(new Paragraph("Distribución de puntajes (% del máximo): " + p.nombre() + ", versión "
                    + p.version(), SUBTITULO));
            documento.add(tablaTramos(p.distribucion(), "Evaluaciones"));
        }
    }

    private static PdfPTable tablaTramos(List<TramoDto> tramos, String etiqueta) {
        String[] columnas = new String[tramos.size() + 1];
        float[] anchos = new float[tramos.size() + 1];
        columnas[0] = "Tramo";
        anchos[0] = 2;
        for (int n = 0; n < tramos.size(); n++) {
            columnas[n + 1] = tramos.get(n).tramo();
            anchos[n + 1] = 1;
        }
        PdfPTable t = tabla(columnas, anchos);
        celda(t, etiqueta, fondo(0), Element.ALIGN_LEFT);
        tramos.forEach(tr -> celda(t, String.valueOf(tr.cantidad()), fondo(0), Element.ALIGN_CENTER));
        return t;
    }

    private static void estudiantes(Document documento, List<FilaEstudianteDto> filas) {
        documento.add(seccion("Detalle por estudiante"));
        PdfPTable t = tabla(COLUMNAS_ESTUDIANTES, ANCHOS_ESTUDIANTES);
        for (int n = 0; n < filas.size(); n++) {
            FilaEstudianteDto f = filas.get(n);
            celda(t, f.estudiante(), fondo(n), Element.ALIGN_LEFT);
            celda(t, f.rut(), fondo(n), Element.ALIGN_LEFT);
            celda(t, f.profesorGuia() == null ? "No lleva" : f.profesorGuia(), fondo(n), Element.ALIGN_LEFT);
            celda(t, f.estado().name(), fondo(n), Element.ALIGN_CENTER);
            celda(t, numero(f.notaFinal()), fondo(n), Element.ALIGN_CENTER);
            celda(t, String.valueOf(f.evaluacionesFinalizadas()), fondo(n), Element.ALIGN_CENTER);
            celda(t, numero(f.promedioEvaluaciones()), fondo(n), Element.ALIGN_CENTER);
            celda(t, String.valueOf(f.planificaciones()), fondo(n), Element.ALIGN_CENTER);
            celda(t, String.valueOf(f.planificacionesAutorizadas()), fondo(n), Element.ALIGN_CENTER);
        }
        documento.add(t);
    }
}

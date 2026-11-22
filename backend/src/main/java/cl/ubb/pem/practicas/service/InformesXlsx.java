package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.EstudianteDto;
import cl.ubb.pem.practicas.dto.EvaluacionResumenDto;
import cl.ubb.pem.practicas.dto.HistorialDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto;
import cl.ubb.pem.practicas.dto.PlanificacionResumenDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * HU-08: los informes en Excel (.xlsx), con el mismo contenido que sus PDF. Integrado desde el trabajo de Alej
 * (commit 7e9b132, 28-oct): {@link LibroXlsx} es su generador sin librerías nuevas; aquí se arma con nuestros DTO.
 * - Informe por asignatura y período: hojas Resumen, Distribución, Pautas y Estudiantes.
 * - Informe por estudiante (su historial, HU-28): hojas Estudiante, Prácticas, Evaluaciones y Planificaciones.
 */
@Component
public class InformesXlsx {

    public static final String TIPO_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public byte[] asignatura(InformeAsignaturaDto inf) {
        var ins = inf.inscripciones();
        var notas = inf.notasFinales();
        var pl = inf.planificaciones();
        List<List<Object>> resumen = new ArrayList<>();
        resumen.add(fila("Dato", "Valor"));
        resumen.add(fila("Asignatura", inf.asignatura()));
        resumen.add(fila("Período", inf.periodo()));
        resumen.add(fila("Inscritos", ins.total()));
        resumen.add(fila("En curso", ins.enCurso()));
        resumen.add(fila("Aprobadas", ins.aprobadas()));
        resumen.add(fila("Reprobadas", ins.reprobadas()));
        resumen.add(fila("Notas finales", notas.cantidad()));
        resumen.add(fila("Promedio de notas finales", notas.promedio()));
        resumen.add(fila("Nota mínima", notas.minimo()));
        resumen.add(fila("Nota máxima", notas.maximo()));
        resumen.add(fila("Planificaciones", pl.total()));
        resumen.add(fila("Planificaciones autorizadas", pl.autorizadas()));
        resumen.add(fila("Devueltas al menos una vez", pl.devueltasAlMenosUnaVez()));
        resumen.add(fila("Generado el", inf.generado()));

        List<List<Object>> distribucion = new ArrayList<>();
        distribucion.add(fila("Qué se distribuye", "Tramo", "Cantidad"));
        notas.tramos().forEach(t -> distribucion.add(fila("Notas finales", t.tramo(), t.cantidad())));
        inf.evaluaciones().forEach(p -> p.promedios().tramos().forEach(t -> distribucion.add(
                fila("Promedio de evaluaciones: " + p.pauta() + " (v" + p.version() + ")", t.tramo(), t.cantidad()))));

        List<List<Object>> pautas = new ArrayList<>();
        pautas.add(fila("Pauta", "Versión", "Evaluador", "Evaluaciones", "Puntaje máximo", "Puntaje promedio",
                "% de logro", "Promedio"));
        inf.evaluaciones().forEach(p -> pautas.add(fila(p.pauta(), p.version(), p.tipoEvaluador(), p.evaluaciones(),
                p.puntajeMaximo(), p.puntajePromedio(), p.porcentajeLogro(), p.promedios().promedio())));

        List<List<Object>> estudiantes = new ArrayList<>();
        estudiantes.add(fila("Estudiante", "RUT", "Estado", "Nota final", "Evaluaciones finalizadas",
                "Promedio de evaluaciones", "Planificaciones", "Planificaciones autorizadas"));
        inf.estudiantes().forEach(f -> estudiantes.add(fila(f.estudiante(), f.rut(), f.estado(), f.notaFinal(),
                f.evaluacionesFinalizadas(), f.promedioEvaluaciones(), f.planificaciones(), f.planificacionesAutorizadas())));

        return new LibroXlsx().hoja("Resumen", resumen).hoja("Distribución", distribucion).hoja("Pautas", pautas)
                .hoja("Estudiantes", estudiantes).generar();
    }

    public byte[] historial(HistorialDto h) {
        EstudianteDto e = h.estudiante();
        List<List<Object>> datos = new ArrayList<>();
        datos.add(fila("Dato", "Valor"));
        datos.add(fila("Estudiante", e.nombre() + " " + e.apellidos()));
        datos.add(fila("RUT", e.rut()));
        datos.add(fila("Correo", e.email()));
        datos.add(fila("Semestre actual", e.semestreActual()));
        datos.add(fila("Año de ingreso", e.anioIngreso()));
        datos.add(fila("Prácticas aprobadas", h.asignaturasAprobadas().size()));

        List<List<Object>> practicas = new ArrayList<>();
        practicas.add(fila("Orden", "Práctica", "Período", "Estado", "Nota final", "Profesor guía", "Profesor del curso",
                "Trabajó con"));
        List<List<Object>> evaluaciones = new ArrayList<>();
        evaluaciones.add(fila("Práctica", "Período", "Fecha de la clase", "Pauta", "Establecimiento", "Curso",
                "Puntaje", "Promedio", "Nota"));
        List<List<Object>> planes = new ArrayList<>();
        planes.add(fila("Práctica", "Período", "Título", "Fecha de la clase", "Estado"));
        for (HistorialDto.InscripcionHistorialDto i : h.inscripciones()) {
            practicas.add(fila(i.asignatura().orden(), i.asignatura().nombre(), i.periodo(), i.estado().name(),
                    i.notaFinal(), i.profesorGuia() == null ? null : i.profesorGuia().nombreCompleto(),
                    i.profesorCurso() == null ? null : i.profesorCurso().nombreCompleto(),
                    i.companeros() == null || i.companeros().isEmpty() ? null : String.join(", ", i.companeros())));
            for (EvaluacionResumenDto ev : i.evaluaciones()) {
                evaluaciones.add(fila(i.asignatura().nombre(), i.periodo(), ev.fechaClase(), ev.pautaNombre(),
                        ev.establecimiento(), ev.curso(), ev.puntajeTotal(), ev.promedio(), ev.nota()));
            }
            for (PlanificacionResumenDto p : i.planificaciones()) {
                planes.add(fila(i.asignatura().nombre(), i.periodo(), p.titulo(), p.fechaClase(), p.estado().name()));
            }
        }
        return new LibroXlsx().hoja("Estudiante", datos).hoja("Prácticas", practicas).hoja("Evaluaciones", evaluaciones)
                .hoja("Planificaciones", planes).generar();
    }

    private static List<Object> fila(Object... celdas) {
        return new ArrayList<>(Arrays.asList(celdas));
    }
}

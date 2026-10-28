package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.AsignaturaDto;
import cl.ubb.pem.practicas.dto.EvaluacionResumenDto;
import cl.ubb.pem.practicas.dto.HistorialDto;
import cl.ubb.pem.practicas.dto.HistorialDto.InscripcionHistorialDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.FilaEstudianteDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.PautaUsadaDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.ResumenDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.TramoDto;
import cl.ubb.pem.practicas.dto.PlanificacionResumenDto;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.AsignaturaPracticaRepository;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-08: informes por estudiante y por asignatura, en PDF o Excel. Solo lectura. El rol (coordinador o jefe de
 * carrera) lo restringe @PreAuthorize en el controlador; el informe por estudiante además pasa por las reglas del
 * historial (HU-05), que es de donde sale.
 */
@Service
@Transactional(readOnly = true)
public class InformeService {

    /** Formatos de descarga (criterio 3). */
    public enum Formato {
        PDF("application/pdf", "pdf"),
        XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

        public final String tipoMime;
        public final String extension;

        Formato(String tipoMime, String extension) {
            this.tipoMime = tipoMime;
            this.extension = extension;
        }

        /** "pdf", "PDF", "xlsx"... Cualquier otro valor da 400. */
        public static Formato de(String texto) {
            try {
                return valueOf(texto.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new ReglaNegocioException("Formato no válido: '" + texto + "'. Usa 'pdf' o 'xlsx'.");
            }
        }
    }

    /** Un informe listo para descargar. */
    public record Informe(byte[] contenido, String nombreArchivo, String tipoMime) {
    }

    private static final Pattern PERIODO = Pattern.compile("^[0-9]{4}-[12]$");

    /** Tramos de la nota final (escala chilena 1,0 a 7,0; el 4,0 aprueba). */
    private static final String[] TRAMOS_NOTA = {"1,0 a 3,9", "4,0 a 4,9", "5,0 a 5,9", "6,0 a 7,0"};
    /** Tramos del puntaje como porcentaje del máximo de la pauta; el 60 % es la exigencia habitual para el 4,0 (S-77). */
    private static final String[] TRAMOS_PUNTAJE = {"Menos de 60 %", "60 a 69 %", "70 a 79 %", "80 a 89 %", "90 % o más"};

    private final AsignaturaPracticaRepository asignaturas;
    private final InscripcionRepository inscripciones;
    private final EvaluacionRepository evaluaciones;
    private final PlanificacionRepository planificaciones;
    private final HistorialService historial;
    private final HistorialPdfGenerador historialPdf;
    private final InformeAsignaturaPdfGenerador asignaturaPdf;
    private final Clock reloj;

    public InformeService(AsignaturaPracticaRepository asignaturas, InscripcionRepository inscripciones,
                          EvaluacionRepository evaluaciones, PlanificacionRepository planificaciones,
                          HistorialService historial, HistorialPdfGenerador historialPdf,
                          InformeAsignaturaPdfGenerador asignaturaPdf, Clock reloj) {
        this.asignaturas = asignaturas;
        this.inscripciones = inscripciones;
        this.evaluaciones = evaluaciones;
        this.planificaciones = planificaciones;
        this.historial = historial;
        this.historialPdf = historialPdf;
        this.asignaturaPdf = asignaturaPdf;
        this.reloj = reloj;
    }

    // ---------- Criterio 1: informe por estudiante ----------

    public Informe informeEstudiante(Long estudianteId, Formato formato, UsuarioAutenticado actor) {
        HistorialDto h = historial.historialDe(estudianteId, actor);
        String nombre = "informe-estudiante-" + estudianteId + "." + formato.extension;
        byte[] contenido = formato == Formato.PDF ? historialPdf.generar(h) : estudianteXlsx(h);
        return new Informe(contenido, nombre, formato.tipoMime);
    }

    private byte[] estudianteXlsx(HistorialDto h) {
        var e = h.estudiante();
        List<List<Object>> datos = new ArrayList<>();
        datos.add(List.of("Dato", "Valor"));
        datos.add(fila("Estudiante", e.nombre() + " " + e.apellidos()));
        datos.add(fila("RUT", e.rut()));
        datos.add(fila("Correo", e.email()));
        datos.add(fila("Semestre actual", e.semestreActual()));
        datos.add(fila("Año de ingreso", e.anioIngreso()));
        datos.add(fila("Prácticas aprobadas", h.asignaturasAprobadas().size()));
        datos.add(fila("Generado el", LocalDateTime.now(reloj)));

        List<List<Object>> practicas = new ArrayList<>();
        practicas.add(List.of("Orden", "Asignatura", "Periodo", "Estado", "Nota final", "Profesor guía",
                "Profesor del curso", "Evaluaciones finalizadas", "Promedio evaluaciones", "Planificaciones",
                "Planificaciones autorizadas", "Equipo (HU-10)"));
        List<List<Object>> evals = new ArrayList<>();
        evals.add(List.of("Asignatura", "Periodo", "Fecha de la clase", "Pauta", "Establecimiento", "Curso",
                "Puntaje total", "Promedio"));
        List<List<Object>> planes = new ArrayList<>();
        planes.add(List.of("Asignatura", "Periodo", "Título", "Fecha de la clase", "Estado"));

        for (InscripcionHistorialDto i : h.inscripciones()) {
            SintesisInscripcion s = SintesisInscripcion.de(i);
            practicas.add(fila(i.asignatura().orden(), i.asignatura().nombre(), i.periodo(), i.estado().name(),
                    i.notaFinal(), i.profesorGuia() == null ? "No lleva" : i.profesorGuia().nombreCompleto(),
                    i.profesorCurso() == null ? null : i.profesorCurso().nombreCompleto(), s.evaluaciones(),
                    s.promedioEvaluaciones(), s.planificaciones(), s.planificacionesAutorizadas(),
                    i.equipo() == null ? null : HistorialPdfGenerador.equipo(i)));
            for (EvaluacionResumenDto ev : i.evaluaciones()) {
                evals.add(fila(i.asignatura().nombre(), i.periodo(), ev.fechaClase(), ev.pautaNombre(),
                        ev.establecimiento(), ev.curso(), ev.puntajeTotal(), ev.promedio()));
            }
            for (PlanificacionResumenDto p : i.planificaciones()) {
                planes.add(fila(i.asignatura().nombre(), i.periodo(), p.titulo(), p.fechaClase(), p.estado().name()));
            }
        }
        return new LibroXlsx().hoja("Estudiante", datos).hoja("Prácticas", practicas).hoja("Evaluaciones", evals)
                .hoja("Planificaciones", planes).generar();
    }

    // ---------- Criterio 2: informe por asignatura y periodo ----------

    public Informe informeAsignatura(Integer asignaturaId, String periodo, Formato formato) {
        InformeAsignaturaDto informe = datosAsignatura(asignaturaId, periodo);
        String nombre = "informe-" + informe.asignatura().codigo() + "_" + periodo + "." + formato.extension;
        byte[] contenido = formato == Formato.PDF ? asignaturaPdf.generar(informe) : asignaturaXlsx(informe);
        return new Informe(contenido, nombre, formato.tipoMime);
    }

    public InformeAsignaturaDto datosAsignatura(Integer asignaturaId, String periodo) {
        if (periodo == null || !PERIODO.matcher(periodo).matches()) {
            throw new ReglaNegocioException("El periodo debe tener el formato AAAA-S, por ejemplo 2026-2.");
        }
        AsignaturaPractica asignatura = asignaturas.findById(asignaturaId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la asignatura con id " + asignaturaId + "."));
        List<Inscripcion> lista = inscripciones.findDeAsignaturaEnPeriodo(asignaturaId, periodo);
        List<Long> ids = lista.stream().map(Inscripcion::getId).toList();
        List<Evaluacion> finalizadas = ids.isEmpty() ? List.of()
                : evaluaciones.findPorInscripciones(ids, EstadoEvaluacion.FINALIZADA);
        List<Planificacion> planes = ids.isEmpty() ? List.of() : planificaciones.findPorInscripciones(ids);

        Map<Long, List<Evaluacion>> evalsPorInscripcion = finalizadas.stream()
                .collect(Collectors.groupingBy(ev -> ev.getInscripcion().getId()));
        Map<Long, List<Planificacion>> planesPorInscripcion = planes.stream()
                .collect(Collectors.groupingBy(p -> p.getInscripcion().getId()));

        List<FilaEstudianteDto> filas = lista.stream().map(i -> {
            List<Evaluacion> evs = evalsPorInscripcion.getOrDefault(i.getId(), List.of());
            List<Planificacion> ps = planesPorInscripcion.getOrDefault(i.getId(), List.of());
            return new FilaEstudianteDto(i.getId(), i.getEstudiante().getUsuario().getNombreCompleto(),
                    i.getEstudiante().getRut(), i.getProfesorGuia() == null ? null : i.getProfesorGuia().getNombreCompleto(),
                    i.getEstado(), i.getNotaFinal(), evs.size(),
                    SintesisInscripcion.promedio(evs.stream().map(Evaluacion::getPromedio).toList()), ps.size(),
                    (int) ps.stream().filter(p -> p.getEstado() == EstadoPlanificacion.AUTORIZADA).count());
        }).toList();

        List<BigDecimal> notas = lista.stream().map(Inscripcion::getNotaFinal).filter(Objects::nonNull).toList();
        ResumenDto resumen = new ResumenDto(lista.size(), contar(lista, EstadoInscripcion.EN_CURSO),
                contar(lista, EstadoInscripcion.APROBADA), contar(lista, EstadoInscripcion.REPROBADA),
                SintesisInscripcion.promedio(notas), finalizadas.size(),
                SintesisInscripcion.promedio(finalizadas.stream().map(Evaluacion::getPromedio).toList()), planes.size(),
                (int) planes.stream().filter(p -> p.getEstado() == EstadoPlanificacion.AUTORIZADA).count());

        return new InformeAsignaturaDto(AsignaturaDto.desde(asignatura), periodo, resumen, distribucionNotas(notas),
                pautasUsadas(finalizadas), filas);
    }

    private static int contar(List<Inscripcion> lista, EstadoInscripcion estado) {
        return (int) lista.stream().filter(i -> i.getEstado() == estado).count();
    }

    private static List<TramoDto> distribucionNotas(List<BigDecimal> notas) {
        int[] cuenta = new int[TRAMOS_NOTA.length];
        for (BigDecimal n : notas) {
            double v = n.doubleValue();
            cuenta[v < 4.0 ? 0 : v < 5.0 ? 1 : v < 6.0 ? 2 : 3]++;
        }
        return tramos(TRAMOS_NOTA, cuenta);
    }

    /** Una entrada por pauta usada (cada versión por separado, porque su puntaje máximo puede cambiar). */
    private static List<PautaUsadaDto> pautasUsadas(List<Evaluacion> finalizadas) {
        Map<Integer, List<Evaluacion>> porPauta = finalizadas.stream()
                .sorted(Comparator.comparing(ev -> ev.getPauta().getId()))
                .collect(Collectors.groupingBy(ev -> ev.getPauta().getId(), LinkedHashMap::new, Collectors.toList()));
        return porPauta.values().stream().map(evs -> {
            Pauta pauta = evs.get(0).getPauta();
            int maximo = pauta.indicadores().size() * pauta.getEscalaMax();
            List<Integer> totales = evs.stream().map(Evaluacion::getPuntajeTotal).filter(Objects::nonNull).toList();
            int[] cuenta = new int[TRAMOS_PUNTAJE.length];
            for (int total : totales) {
                double porcentaje = maximo == 0 ? 0 : total * 100.0 / maximo;
                cuenta[porcentaje < 60 ? 0 : porcentaje < 70 ? 1 : porcentaje < 80 ? 2 : porcentaje < 90 ? 3 : 4]++;
            }
            return new PautaUsadaDto(pauta.getId(), pauta.getNombre(), pauta.getVersion(), maximo, evs.size(),
                    SintesisInscripcion.promedio(totales.stream().map(BigDecimal::valueOf).toList()),
                    totales.stream().min(Integer::compare).orElse(null),
                    totales.stream().max(Integer::compare).orElse(null),
                    SintesisInscripcion.promedio(evs.stream().map(Evaluacion::getPromedio).toList()),
                    tramos(TRAMOS_PUNTAJE, cuenta));
        }).toList();
    }

    private static List<TramoDto> tramos(String[] nombres, int[] cuenta) {
        List<TramoDto> lista = new ArrayList<>();
        for (int n = 0; n < nombres.length; n++) {
            lista.add(new TramoDto(nombres[n], cuenta[n]));
        }
        return lista;
    }

    private byte[] asignaturaXlsx(InformeAsignaturaDto informe) {
        ResumenDto r = informe.resumen();
        List<List<Object>> resumen = new ArrayList<>();
        resumen.add(List.of("Dato", "Valor"));
        resumen.add(fila("Asignatura", informe.asignatura().nombre()));
        resumen.add(fila("Periodo", informe.periodo()));
        resumen.add(fila("Inscripciones", r.inscripciones()));
        resumen.add(fila("En curso", r.enCurso()));
        resumen.add(fila("Aprobadas", r.aprobadas()));
        resumen.add(fila("Reprobadas", r.reprobadas()));
        resumen.add(fila("Promedio de notas finales", r.promedioNotaFinal()));
        resumen.add(fila("Evaluaciones finalizadas", r.evaluacionesFinalizadas()));
        resumen.add(fila("Promedio de las evaluaciones", r.promedioEvaluaciones()));
        resumen.add(fila("Planificaciones", r.planificaciones()));
        resumen.add(fila("Planificaciones autorizadas", r.planificacionesAutorizadas()));
        resumen.add(fila("Generado el", LocalDateTime.now(reloj)));

        List<List<Object>> distribucion = new ArrayList<>();
        distribucion.add(List.of("Distribución", "Tramo", "Cantidad"));
        informe.distribucionNotasFinales().forEach(t -> distribucion.add(fila("Notas finales", t.tramo(), t.cantidad())));
        for (PautaUsadaDto p : informe.pautas()) {
            String nombre = p.nombre() + " (v" + p.version() + ", máximo " + p.puntajeMaximo() + ")";
            p.distribucion().forEach(t -> distribucion.add(fila(nombre, t.tramo(), t.cantidad())));
        }

        List<List<Object>> pautas = new ArrayList<>();
        pautas.add(List.of("Pauta", "Versión", "Puntaje máximo", "Evaluaciones", "Puntaje promedio", "Puntaje mínimo",
                "Puntaje más alto", "Promedio"));
        informe.pautas().forEach(p -> pautas.add(fila(p.nombre(), p.version(), p.puntajeMaximo(), p.evaluaciones(),
                p.puntajePromedio(), p.puntajeMinimo(), p.puntajeMasAlto(), p.promedio())));

        List<List<Object>> estudiantes = new ArrayList<>();
        estudiantes.add(List.of("Estudiante", "RUT", "Profesor guía", "Estado", "Nota final",
                "Evaluaciones finalizadas", "Promedio evaluaciones", "Planificaciones", "Planificaciones autorizadas"));
        informe.estudiantes().forEach(f -> estudiantes.add(fila(f.estudiante(), f.rut(),
                f.profesorGuia() == null ? "No lleva" : f.profesorGuia(), f.estado().name(), f.notaFinal(),
                f.evaluacionesFinalizadas(), f.promedioEvaluaciones(), f.planificaciones(),
                f.planificacionesAutorizadas())));

        return new LibroXlsx().hoja("Resumen", resumen).hoja("Distribución", distribucion).hoja("Pautas", pautas)
                .hoja("Estudiantes", estudiantes).generar();
    }

    /** Una fila de Excel que admite celdas nulas (List.of no las acepta). */
    private static List<Object> fila(Object... celdas) {
        return Arrays.asList(celdas);
    }
}

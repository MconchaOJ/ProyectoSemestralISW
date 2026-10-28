package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.ubb.pem.practicas.Datos;
import cl.ubb.pem.practicas.dto.HistorialDto;
import cl.ubb.pem.practicas.entity.AmbitoPauta;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.entity.TipoEvaluador;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.EstudianteRepository;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistorialServiceTest {

    @Mock
    private EstudianteRepository estudiantes;
    @Mock
    private InscripcionRepository inscripciones;
    @Mock
    private EvaluacionRepository evaluaciones;
    @Mock
    private PlanificacionRepository planificaciones;
    @Mock
    private AccesoService acceso;

    private HistorialService servicio;

    private final Usuario usuarioEstudiante = Datos.usuario(6, "ESTUDIANTE");
    private final Usuario guia = Datos.usuario(2, "PROFESOR_GUIA");
    private final Usuario curso = Datos.usuario(4, "PROFESOR_CURSO");
    private final Estudiante estudiante = Datos.estudiante(1, usuarioEstudiante);
    private final UsuarioAutenticado actor = new UsuarioAutenticado(2L, "g@demo.example", "PROFESOR_GUIA");

    @BeforeEach
    void preparar() {
        servicio = new HistorialService(estudiantes, inscripciones, evaluaciones, planificaciones, acceso,
                new HistorialPdfGenerador(java.time.Clock.systemDefaultZone()));
    }

    private Inscripcion inscripcion(long id, int asignaturaId, String periodo, EstadoInscripcion estado, String nota) {
        Inscripcion i = Datos.inscripcion(id, estudiante, Datos.asignatura(asignaturaId, asignaturaId > 2), asignaturaId > 2 ? guia : null, curso);
        i.setPeriodo(periodo);
        i.setEstado(estado);
        i.setNotaFinal(nota == null ? null : new BigDecimal(nota));
        return i;
    }

    private Evaluacion evaluacion(long id, Inscripcion i, int total, String promedio) {
        Pauta p = new Pauta();
        p.setId(1);
        p.setNombre("Pauta");
        p.setTipoEvaluador(TipoEvaluador.COLABORADOR);
        p.setAmbito(AmbitoPauta.OBSERVACION_CLASE);
        Evaluacion e = new Evaluacion();
        e.setId(id);
        e.setInscripcion(i);
        e.setPauta(p);
        e.setFechaClase(LocalDate.of(2025, 10, 8));
        e.setEstado(EstadoEvaluacion.FINALIZADA);
        e.setPuntajeTotal(total);
        e.setPromedio(new BigDecimal(promedio));
        return e;
    }

    private Planificacion planificacion(long id, Inscripcion i, EstadoPlanificacion estado) {
        Planificacion p = new Planificacion();
        p.setId(id);
        p.setInscripcion(i);
        p.setTitulo("Plan " + id);
        p.setEstado(estado);
        return p;
    }

    @Test
    void armaElRecorridoConEvaluacionesPlanificacionesYAsignaturasAprobadas() {
        Inscripcion i1 = inscripcion(10, 1, "2025-2", EstadoInscripcion.APROBADA, "5.5");
        Inscripcion i3 = inscripcion(11, 3, "2026-1", EstadoInscripcion.APROBADA, "6.0");
        Inscripcion i5 = inscripcion(12, 5, "2026-2", EstadoInscripcion.EN_CURSO, null);
        when(estudiantes.findById(1L)).thenReturn(Optional.of(estudiante));
        when(acceso.puedeVerHistorial(estudiante, actor)).thenReturn(true);
        when(inscripciones.findRecorrido(1L)).thenReturn(List.of(i1, i3, i5));
        when(evaluaciones.findPorInscripciones(List.of(10L, 11L, 12L), EstadoEvaluacion.FINALIZADA))
                .thenReturn(List.of(evaluacion(100, i3, 96, "4.80")));
        when(planificaciones.findPorInscripciones(List.of(10L, 11L, 12L)))
                .thenReturn(List.of(planificacion(200, i3, EstadoPlanificacion.AUTORIZADA), planificacion(201, i5, EstadoPlanificacion.BORRADOR)));

        HistorialDto h = servicio.historialDe(1L, actor);

        assertThat(h.estudiante().semestreActual()).isEqualTo(6);
        assertThat(h.inscripciones()).extracting(x -> x.periodo()).containsExactly("2025-2", "2026-1", "2026-2");
        assertThat(h.asignaturasAprobadas()).extracting(a -> a.codigo()).containsExactly("PEM-P1", "PEM-P3");
        assertThat(h.inscripciones().get(0).evaluaciones()).isEmpty();
        assertThat(h.inscripciones().get(1).evaluaciones()).hasSize(1);
        assertThat(h.inscripciones().get(1).evaluaciones().get(0).promedio()).isEqualByComparingTo("4.80");
        assertThat(h.inscripciones().get(1).planificaciones().get(0).estado()).isEqualTo(EstadoPlanificacion.AUTORIZADA);
        assertThat(h.inscripciones().get(2).planificaciones().get(0).estado()).isEqualTo(EstadoPlanificacion.BORRADOR);
        assertThat(h.inscripciones().get(0).profesorGuia()).isNull();     // ciclo inicial: sin guía
        assertThat(h.inscripciones().get(1).profesorGuia().id()).isEqualTo(2L);
    }

    @Test
    void soloPideLasEvaluacionesFinalizadas() {
        Inscripcion i = inscripcion(10, 3, "2026-2", EstadoInscripcion.EN_CURSO, null);
        when(estudiantes.findById(1L)).thenReturn(Optional.of(estudiante));
        when(acceso.puedeVerHistorial(estudiante, actor)).thenReturn(true);
        when(inscripciones.findRecorrido(1L)).thenReturn(List.of(i));
        when(evaluaciones.findPorInscripciones(List.of(10L), EstadoEvaluacion.FINALIZADA)).thenReturn(List.of());
        when(planificaciones.findPorInscripciones(List.of(10L))).thenReturn(List.of());

        servicio.historialDe(1L, actor);

        verify(evaluaciones).findPorInscripciones(List.of(10L), EstadoEvaluacion.FINALIZADA);
    }

    @Test
    void unEstudianteSinInscripcionesDevuelveUnHistorialVacioSinConsultarMas() {
        when(estudiantes.findById(1L)).thenReturn(Optional.of(estudiante));
        when(acceso.puedeVerHistorial(estudiante, actor)).thenReturn(true);
        when(inscripciones.findRecorrido(1L)).thenReturn(List.of());

        HistorialDto h = servicio.historialDe(1L, actor);

        assertThat(h.inscripciones()).isEmpty();
        assertThat(h.asignaturasAprobadas()).isEmpty();
        verify(evaluaciones, never()).findPorInscripciones(any(), any());
        verify(planificaciones, never()).findPorInscripciones(any());
    }

    @Test
    void sinPermisoDa403YNoSeConsultaNada() {
        when(estudiantes.findById(1L)).thenReturn(Optional.of(estudiante));
        when(acceso.puedeVerHistorial(estudiante, actor)).thenReturn(false);
        assertThatThrownBy(() -> servicio.historialDe(1L, actor)).isInstanceOf(AccesoDenegadoException.class);
        verify(inscripciones, never()).findRecorrido(any());
    }

    @Test
    void estudianteInexistenteDa404() {
        when(estudiantes.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.historialDe(99L, actor)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}

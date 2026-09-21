package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import cl.ubb.pem.practicas.Datos;
import cl.ubb.pem.practicas.config.PlanificacionProperties;
import cl.ubb.pem.practicas.dto.PlanificacionActualizarRequest;
import cl.ubb.pem.practicas.dto.PlanificacionCrearRequest;
import cl.ubb.pem.practicas.dto.PlanificacionDto;
import cl.ubb.pem.practicas.dto.RevisionRequest;
import cl.ubb.pem.practicas.entity.DecisionRevision;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.EtapaRevision;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Máquina de estados de la planificación, sin base de datos. */
@ExtendWith(MockitoExtension.class)
class PlanificacionServiceTest {

    @Mock
    private PlanificacionRepository planificaciones;
    @Mock
    private InscripcionRepository inscripciones;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private AccesoService acceso;

    private PlanificacionService servicio;

    private final Usuario usuarioEstudiante = Datos.usuario(6, "ESTUDIANTE");
    private final Usuario guia = Datos.usuario(2, "PROFESOR_GUIA");
    private final Usuario curso = Datos.usuario(4, "PROFESOR_CURSO");
    private final UsuarioAutenticado estudiante = new UsuarioAutenticado(6L, "e@demo.example", "ESTUDIANTE");
    private final UsuarioAutenticado guiaActor = new UsuarioAutenticado(2L, "g@demo.example", "PROFESOR_GUIA");
    private final UsuarioAutenticado cursoActor = new UsuarioAutenticado(4L, "c@demo.example", "PROFESOR_CURSO");
    private Inscripcion inscripcion;

    @BeforeEach
    void preparar() {
        // "Hoy" fijo: 20 de septiembre de 2026, con 3 días mínimos de anticipación -> la primera clase válida es el 23
        Clock hoy = Clock.fixed(Instant.parse("2026-09-20T15:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new PlanificacionService(planificaciones, inscripciones, usuarios, acceso, hoy, new PlanificacionProperties(3));
        inscripcion = Datos.inscripcion(3, Datos.estudiante(1, usuarioEstudiante), Datos.asignatura(3, true), guia, curso);
        lenient().when(inscripciones.findById(3L)).thenReturn(Optional.of(inscripcion));
        lenient().when(usuarios.findById(2L)).thenReturn(Optional.of(guia));
        lenient().when(usuarios.findById(4L)).thenReturn(Optional.of(curso));
        lenient().when(planificaciones.save(any(Planificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(planificaciones.saveAndFlush(any(Planificacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Planificacion planificacion(EstadoPlanificacion estado, boolean completa) {
        Planificacion p = new Planificacion();
        p.setId(10L);
        p.setInscripcion(inscripcion);
        p.setTitulo("Ecuaciones");
        p.setEstado(estado);
        if (completa) {
            p.setFechaClase(LocalDate.of(2026, 10, 7));
            p.setObjetivo("obj");
            p.setRecursos("rec");
            p.setTiempos("tiem");
            p.setDuracionMinutos(90);
            p.setEvaluacionAprendizaje("eval");
            p.setInteraccionParticipantes("inter");
        }
        lenient().when(planificaciones.findById(10L)).thenReturn(Optional.of(p));
        return p;
    }

    private static RevisionRequest aprobar() {
        return new RevisionRequest(DecisionRevision.APROBADA, null);
    }

    private static RevisionRequest devolver(String comentario) {
        return new RevisionRequest(DecisionRevision.OBSERVADA, comentario);
    }

    // ---------- crear y editar ----------

    @Test
    void crearDejaUnBorradorAunqueEsteIncompleto() {
        PlanificacionDto dto = servicio.crear(new PlanificacionCrearRequest(3L, "Mi clase", null, "  ", null, null, null, null, null), estudiante);
        assertThat(dto.estado()).isEqualTo(EstadoPlanificacion.BORRADOR);
        assertThat(dto.objetivo()).isNull();      // el texto en blanco cuenta como vacío
        assertThat(dto.pendienteDe()).isEqualTo("ESTUDIANTE");
    }

    @Test
    void soloElEstudianteDeLaInscripcionPuedeCrearYSoloSiEstaEnCurso() {
        UsuarioAutenticado otro = new UsuarioAutenticado(7L, "o@demo.example", "ESTUDIANTE");
        assertThatThrownBy(() -> servicio.crear(new PlanificacionCrearRequest(3L, "X", null, null, null, null, null, null, null), otro))
                .isInstanceOf(AccesoDenegadoException.class);
        inscripcion.setEstado(EstadoInscripcion.APROBADA);
        assertThatThrownBy(() -> servicio.crear(new PlanificacionCrearRequest(3L, "X", null, null, null, null, null, null, null), estudiante))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void soloSeEditaEnBorradorOConObservaciones() {
        var req = new PlanificacionActualizarRequest("Nuevo título", null, null, null, null, null, null, null);
        for (EstadoPlanificacion bloqueado : new EstadoPlanificacion[]{EstadoPlanificacion.EN_REVISION_GUIA,
                EstadoPlanificacion.EN_AUTORIZACION, EstadoPlanificacion.AUTORIZADA}) {
            planificacion(bloqueado, true);
            assertThatThrownBy(() -> servicio.actualizar(10L, req, estudiante)).isInstanceOf(ConflictoException.class);
        }
        for (EstadoPlanificacion editable : new EstadoPlanificacion[]{EstadoPlanificacion.BORRADOR, EstadoPlanificacion.CON_OBSERVACIONES}) {
            planificacion(editable, false);
            assertThat(servicio.actualizar(10L, req, estudiante).titulo()).isEqualTo("Nuevo título");
        }
    }

    @Test
    void otroEstudianteNoPuedeEditarNiEnviar() {
        planificacion(EstadoPlanificacion.BORRADOR, true);
        UsuarioAutenticado otro = new UsuarioAutenticado(7L, "o@demo.example", "ESTUDIANTE");
        assertThatThrownBy(() -> servicio.enviar(10L, otro)).isInstanceOf(AccesoDenegadoException.class);
    }

    // ---------- enviar ----------

    @Test
    void paraEnviarTodosLosElementosSonObligatoriosYElMensajeDiceCuales() {
        planificacion(EstadoPlanificacion.BORRADOR, false);
        assertThatThrownBy(() -> servicio.enviar(10L, estudiante)).isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("fechaClase").hasMessageContaining("objetivo").hasMessageContaining("interaccionParticipantes");
    }

    @Test
    void enviarUnaPlanificacionCompletaLaPasaAEnRevisionGuia() {
        Planificacion p = planificacion(EstadoPlanificacion.BORRADOR, true);
        assertThat(servicio.enviar(10L, estudiante).estado()).isEqualTo(EstadoPlanificacion.EN_REVISION_GUIA);
        assertThat(p.getRevisiones()).isEmpty();     // el envío del estudiante no es una revisión
        assertThatThrownBy(() -> servicio.enviar(10L, estudiante)).isInstanceOf(ConflictoException.class);   // ya enviada
    }

    @Test
    void sinProfesorGuiaVaDirectoAlProfesorDelCurso() {
        // Ciclo inicial (p. ej. Ambientes de Aprendizaje): no hay colegio ni colaborador (S-34)
        inscripcion.setProfesorGuia(null);
        planificacion(EstadoPlanificacion.BORRADOR, true);
        PlanificacionDto dto = servicio.enviar(10L, estudiante);
        assertThat(dto.estado()).isEqualTo(EstadoPlanificacion.EN_AUTORIZACION);
        assertThat(dto.pendienteDe()).isEqualTo("PROFESOR_CURSO");
    }

    @Test
    void sinGuiaElProfesorDelCursoAutorizaPeroNingunGuiaPuedeRevisar() {
        inscripcion.setProfesorGuia(null);
        planificacion(EstadoPlanificacion.EN_AUTORIZACION, true);
        assertThatThrownBy(() -> servicio.revisarComoGuia(10L, aprobar(), guiaActor)).isInstanceOf(AccesoDenegadoException.class);
        assertThat(servicio.revisarComoProfesorDelCurso(10L, aprobar(), cursoActor).estado()).isEqualTo(EstadoPlanificacion.AUTORIZADA);
    }

    @Test
    void laPlanificacionSeEntregaConAlMenos3DiasDeAnticipacion() {
        Planificacion p = planificacion(EstadoPlanificacion.BORRADOR, true);
        p.setFechaClase(LocalDate.of(2026, 9, 22));       // solo 2 días después de "hoy" (20-sep)
        assertThatThrownBy(() -> servicio.enviar(10L, estudiante)).isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("3 días de anticipación").hasMessageContaining("2026-09-23");
        assertThat(p.getEstado()).isEqualTo(EstadoPlanificacion.BORRADOR);

        p.setFechaClase(LocalDate.of(2026, 9, 23));       // justo 3 días: se acepta
        assertThat(servicio.enviar(10L, estudiante).estado()).isEqualTo(EstadoPlanificacion.EN_REVISION_GUIA);
    }

    @Test
    void aproximacionNoEntregaPlanificaciones() {
        inscripcion.getAsignatura().setRequierePlanificacion(false);
        assertThatThrownBy(() -> servicio.crear(new PlanificacionCrearRequest(3L, "X", null, null, null, null, null, null, null), estudiante))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("no se entregan planificaciones");
    }

    // ---------- revisión del guía ----------

    @Test
    void elGuiaAprueba_pasaAEnAutorizacionYQuedaRegistrado() {
        Planificacion p = planificacion(EstadoPlanificacion.EN_REVISION_GUIA, true);
        PlanificacionDto dto = servicio.revisarComoGuia(10L, aprobar(), guiaActor);
        assertThat(dto.estado()).isEqualTo(EstadoPlanificacion.EN_AUTORIZACION);
        assertThat(dto.pendienteDe()).isEqualTo("PROFESOR_CURSO");
        assertThat(p.getRevisiones()).hasSize(1);
        assertThat(p.getRevisiones().get(0).getEtapa()).isEqualTo(EtapaRevision.GUIA);
        assertThat(p.getRevisiones().get(0).getRevisor().getId()).isEqualTo(2L);
    }

    @Test
    void elGuiaDevuelveConComentario_yVuelveAlEstudiante() {
        planificacion(EstadoPlanificacion.EN_REVISION_GUIA, true);
        PlanificacionDto dto = servicio.revisarComoGuia(10L, devolver("Precisar el cierre"), guiaActor);
        assertThat(dto.estado()).isEqualTo(EstadoPlanificacion.CON_OBSERVACIONES);
        assertThat(dto.revisiones().get(0).comentario()).isEqualTo("Precisar el cierre");
    }

    @Test
    void devolverSinComentarioONadaMasQueEspaciosEs400() {
        Planificacion p = planificacion(EstadoPlanificacion.EN_REVISION_GUIA, true);
        assertThatThrownBy(() -> servicio.revisarComoGuia(10L, devolver(null), guiaActor)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.revisarComoGuia(10L, devolver("   "), guiaActor)).isInstanceOf(ReglaNegocioException.class);
        assertThat(p.getEstado()).isEqualTo(EstadoPlanificacion.EN_REVISION_GUIA);
        assertThat(p.getRevisiones()).isEmpty();
    }

    @Test
    void otroGuiaNoPuedeRevisar() {
        planificacion(EstadoPlanificacion.EN_REVISION_GUIA, true);
        UsuarioAutenticado otro = new UsuarioAutenticado(3L, "g3@demo.example", "PROFESOR_GUIA");
        assertThatThrownBy(() -> servicio.revisarComoGuia(10L, aprobar(), otro)).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void elGuiaSoloRevisaCuandoEstaEnRevisionGuia() {
        for (EstadoPlanificacion estado : new EstadoPlanificacion[]{EstadoPlanificacion.BORRADOR, EstadoPlanificacion.CON_OBSERVACIONES,
                EstadoPlanificacion.EN_AUTORIZACION, EstadoPlanificacion.AUTORIZADA}) {
            planificacion(estado, true);
            assertThatThrownBy(() -> servicio.revisarComoGuia(10L, aprobar(), guiaActor)).isInstanceOf(ConflictoException.class);
        }
    }

    // ---------- autorización del profesor del curso ----------

    @Test
    void elProfesorDelCursoAutoriza() {
        Planificacion p = planificacion(EstadoPlanificacion.EN_AUTORIZACION, true);
        PlanificacionDto dto = servicio.revisarComoProfesorDelCurso(10L, aprobar(), cursoActor);
        assertThat(dto.estado()).isEqualTo(EstadoPlanificacion.AUTORIZADA);
        assertThat(dto.pendienteDe()).isNull();
        assertThat(p.getRevisiones().get(0).getEtapa()).isEqualTo(EtapaRevision.CURSO);
    }

    @Test
    void elProfesorDelCursoNoPuedeSaltarseAlGuiaNiRevisarUnaAutorizada() {
        planificacion(EstadoPlanificacion.EN_REVISION_GUIA, true);
        assertThatThrownBy(() -> servicio.revisarComoProfesorDelCurso(10L, aprobar(), cursoActor)).isInstanceOf(ConflictoException.class);
        planificacion(EstadoPlanificacion.AUTORIZADA, true);
        assertThatThrownBy(() -> servicio.revisarComoProfesorDelCurso(10L, devolver("tarde"), cursoActor)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void otroProfesorDelCursoNoPuedeAutorizar() {
        planificacion(EstadoPlanificacion.EN_AUTORIZACION, true);
        UsuarioAutenticado otro = new UsuarioAutenticado(5L, "c5@demo.example", "PROFESOR_CURSO");
        assertThatThrownBy(() -> servicio.revisarComoProfesorDelCurso(10L, aprobar(), otro)).isInstanceOf(AccesoDenegadoException.class);
    }

    // ---------- flujo completo (S-10) ----------

    @Test
    void siElProfesorDelCursoDevuelve_elFlujoSeRepiteDesdeElGuiaYTodoQuedaEnElHistorial() {
        Planificacion p = planificacion(EstadoPlanificacion.BORRADOR, true);

        servicio.enviar(10L, estudiante);
        servicio.revisarComoGuia(10L, aprobar(), guiaActor);
        servicio.revisarComoProfesorDelCurso(10L, devolver("Falta la evaluación del aprendizaje"), cursoActor);
        assertThat(p.getEstado()).isEqualTo(EstadoPlanificacion.CON_OBSERVACIONES);

        servicio.actualizar(10L, new PlanificacionActualizarRequest("Ecuaciones v2", LocalDate.of(2026, 10, 7), "obj", "rec", "tiem", 90, "eval mejorada", "inter"), estudiante);
        servicio.enviar(10L, estudiante);
        assertThat(p.getEstado()).isEqualTo(EstadoPlanificacion.EN_REVISION_GUIA);   // vuelve al guía, no directo al curso
        servicio.revisarComoGuia(10L, aprobar(), guiaActor);
        servicio.revisarComoProfesorDelCurso(10L, aprobar(), cursoActor);

        assertThat(p.getEstado()).isEqualTo(EstadoPlanificacion.AUTORIZADA);
        assertThat(p.getRevisiones()).extracting(r -> r.getEtapa() + ":" + r.getDecision())
                .containsExactly("GUIA:APROBADA", "CURSO:OBSERVADA", "GUIA:APROBADA", "CURSO:APROBADA");
        var req = new PlanificacionActualizarRequest("x", null, null, null, null, null, null, null);
        assertThatThrownBy(() -> servicio.actualizar(10L, req, estudiante)).isInstanceOf(ConflictoException.class);
    }
}

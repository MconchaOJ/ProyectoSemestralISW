package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import cl.ubb.pem.practicas.repository.DocumentoPlanificacionRepository;
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
    @Mock
    private DocumentoPlanificacionRepository documentos;

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
        servicio = new PlanificacionService(planificaciones, inscripciones, usuarios, acceso, hoy, new PlanificacionProperties(3), documentos);
        inscripcion = Datos.inscripcion(3, Datos.estudiante(1, usuarioEstudiante), Datos.asignatura(3, true), guia, curso);
        lenient().when(inscripciones.findById(3L)).thenReturn(Optional.of(inscripcion));
        lenient().when(usuarios.findById(2L)).thenReturn(Optional.of(guia));
        lenient().when(usuarios.findById(4L)).thenReturn(Optional.of(curso));
        lenient().when(planificaciones.save(any(Planificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(planificaciones.saveAndFlush(any(Planificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        // Por defecto la planificación de prueba (id 10) ya tiene su documento formal adjunto (S-47);
        // las pruebas que quieren probar justo esa falta la pisan explícitamente.
        lenient().when(documentos.existsByPlanificacionId(10L)).thenReturn(true);
    }

    private Planificacion planificacion(EstadoPlanificacion estado, boolean completa) {
        Planificacion p = new Planificacion();
        p.setId(10L);
        p.setInscripcion(inscripcion);
        p.setTitulo("Ecuaciones");
        p.setEstado(estado);
        if (completa) {
            // los campos del formato oficial de la carrera (S-45)
            p.setEstablecimiento("Liceo");
            p.setCurso("1° Medio B");
            p.setUnidad("Álgebra");
            p.setObjetivoAprendizaje("oa");
            p.setObjetivoActitudinal("act");
            p.setObjetivoClase("obj");
            p.setFechaClase(LocalDate.of(2026, 10, 7));
            p.setDuracionMinutos(90);
            p.setRecursos("rec");
            p.setInstrumentosEvaluacion("ins");
            p.setInicio("ini");
            p.setInicioMinutos(10);
            p.setDesarrollo("des");
            p.setDesarrolloMinutos(60);
            p.setCierre("cie");
            p.setCierreMinutos(20);
        }
        lenient().when(planificaciones.findById(10L)).thenReturn(Optional.of(p));
        return p;
    }

    /** Pedido de creación con solo título y objetivo de la clase (los demás campos vacíos). */
    private static PlanificacionCrearRequest crearReq(String titulo, String objetivoClase) {
        return new PlanificacionCrearRequest(3L, titulo, null, null, null, null, null, objetivoClase, null, null, null,
                null, null, null, null, null, null, null);
    }

    /** Pedido de edición con todos los campos del formato completos (10 + 60 + 20 = 90 minutos). */
    private static PlanificacionActualizarRequest actualizarCompleta(String titulo, String instrumentos) {
        return new PlanificacionActualizarRequest(titulo, "Liceo", "1° Medio B", "Álgebra", "oa", "act", "obj",
                LocalDate.of(2026, 10, 7), 90, "rec", instrumentos, "ini", 10, "des", 60, "cie", 20);
    }

    /** Pedido de creación con duración y minutos de las tres partes (S-76). */
    private static PlanificacionCrearRequest crearConMinutos(int duracion, int inicio, int desarrollo, int cierre) {
        return new PlanificacionCrearRequest(3L, "Con minutos", null, null, null, null, null, null, null, duracion, null,
                null, null, inicio, null, desarrollo, null, cierre);
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
        PlanificacionDto dto = servicio.crear(crearReq("Mi clase", "  "), estudiante);
        assertThat(dto.estado()).isEqualTo(EstadoPlanificacion.BORRADOR);
        assertThat(dto.objetivoClase()).isNull();      // el texto en blanco cuenta como vacío
        assertThat(dto.pendienteDe()).isEqualTo("ESTUDIANTE");
    }

    @Test
    void lasTresPartesDebenSumarLaDuracionYSiNoNoSeGuardaNada() {   // supuesto S-76
        assertThatThrownBy(() -> servicio.crear(crearConMinutos(90, 10, 60, 30), estudiante))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("100").hasMessageContaining("90");
        verify(planificaciones, never()).save(any(Planificacion.class));
        assertThat(servicio.crear(crearConMinutos(90, 10, 60, 20), estudiante).inicioMinutos()).isEqualTo(10);
    }

    @Test
    void soloElEstudianteDeLaInscripcionPuedeCrearYSoloSiEstaEnCurso() {
        UsuarioAutenticado otro = new UsuarioAutenticado(7L, "o@demo.example", "ESTUDIANTE");
        assertThatThrownBy(() -> servicio.crear(crearReq("X", null), otro))
                .isInstanceOf(AccesoDenegadoException.class);
        inscripcion.setEstado(EstadoInscripcion.APROBADA);
        assertThatThrownBy(() -> servicio.crear(crearReq("X", null), estudiante))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void soloSeEditaEnBorradorOConObservaciones() {
        var req = new PlanificacionActualizarRequest("Nuevo título", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
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
                .hasMessageContaining("establecimiento").hasMessageContaining("objetivoClase").hasMessageContaining("fechaClase")
                .hasMessageContaining("cierreMinutos");
    }

    @Test
    void sinDocumentoFormalAdjuntoNoSePuedeEnviarAunqueLosCamposEstenCompletos() {
        planificacion(EstadoPlanificacion.BORRADOR, true);
        lenient().when(documentos.existsByPlanificacionId(10L)).thenReturn(false);
        assertThatThrownBy(() -> servicio.enviar(10L, estudiante)).isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("documentoFormal").hasMessageNotContaining("objetivo");   // los demás campos sí están completos

        lenient().when(documentos.existsByPlanificacionId(10L)).thenReturn(true);   // el estudiante lo sube
        assertThat(servicio.enviar(10L, estudiante).estado()).isEqualTo(EstadoPlanificacion.EN_REVISION_GUIA);
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
        assertThatThrownBy(() -> servicio.crear(crearReq("X", null), estudiante))
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

        servicio.actualizar(10L, actualizarCompleta("Ecuaciones v2", "instrumentos mejorados"), estudiante);
        servicio.enviar(10L, estudiante);
        assertThat(p.getEstado()).isEqualTo(EstadoPlanificacion.EN_REVISION_GUIA);   // vuelve al guía, no directo al curso
        servicio.revisarComoGuia(10L, aprobar(), guiaActor);
        servicio.revisarComoProfesorDelCurso(10L, aprobar(), cursoActor);

        assertThat(p.getEstado()).isEqualTo(EstadoPlanificacion.AUTORIZADA);
        assertThat(p.getRevisiones()).extracting(r -> r.getEtapa() + ":" + r.getDecision())
                .containsExactly("GUIA:APROBADA", "CURSO:OBSERVADA", "GUIA:APROBADA", "CURSO:APROBADA");
        var req = actualizarCompleta("x", "ins");
        assertThatThrownBy(() -> servicio.actualizar(10L, req, estudiante)).isInstanceOf(ConflictoException.class);
    }
}

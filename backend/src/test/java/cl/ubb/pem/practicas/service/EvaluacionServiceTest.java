package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import cl.ubb.pem.practicas.Datos;
import cl.ubb.pem.practicas.dto.EvaluacionCrearRequest;
import cl.ubb.pem.practicas.dto.EvaluacionDto;
import cl.ubb.pem.practicas.dto.RespuestasRequest;
import cl.ubb.pem.practicas.entity.AmbitoPauta;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.DimensionPauta;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Indicador;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.entity.TipoEvaluador;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.PautaRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluacionServiceTest {

    @Mock
    private EvaluacionRepository evaluaciones;
    @Mock
    private InscripcionRepository inscripciones;
    @Mock
    private PautaRepository pautas;
    @Mock
    private PlanificacionRepository planificaciones;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private AccesoService acceso;

    private EvaluacionService servicio;

    private final Usuario guia = Datos.usuario(2, "PROFESOR_GUIA");
    private final Usuario curso = Datos.usuario(4, "PROFESOR_CURSO");
    private final Estudiante estudiante = Datos.estudiante(1, Datos.usuario(6, "ESTUDIANTE"));
    private final AsignaturaPractica asignatura = Datos.asignatura(3, true);
    private final UsuarioAutenticado actor = new UsuarioAutenticado(2L, "u2@demo.example", "PROFESOR_GUIA");
    private Inscripcion inscripcion;
    private Pauta pauta;      // 3 indicadores (ids 11, 12, 13; números 1, 2, 3), escala 1 a 7

    @BeforeEach
    void preparar() {
        servicio = new EvaluacionService(evaluaciones, inscripciones, pautas, planificaciones, usuarios, acceso);
        inscripcion = Datos.inscripcion(3, estudiante, asignatura, guia, curso);
        pauta = pautaDePrueba();
        lenient().when(inscripciones.findById(3L)).thenReturn(Optional.of(inscripcion));
        lenient().when(pautas.findById(1)).thenReturn(Optional.of(pauta));
        lenient().when(usuarios.findById(2L)).thenReturn(Optional.of(guia));
        lenient().when(evaluaciones.save(any(Evaluacion.class))).thenAnswer(inv -> {
            Evaluacion e = inv.getArgument(0);
            if (e.getId() == null) e.setId(50L);
            return e;
        });
    }

    private Pauta pautaDePrueba() {
        Pauta p = new Pauta();
        p.setId(1);
        p.setAsignatura(asignatura);
        p.setNombre("Pauta de prueba");
        p.setTipoEvaluador(TipoEvaluador.COLABORADOR);
        p.setAmbito(AmbitoPauta.OBSERVACION_CLASE);
        p.setEscalaMin(1);
        p.setEscalaMax(7);
        p.setActiva(true);
        DimensionPauta d1 = new DimensionPauta();
        d1.setId(1);
        d1.setPauta(p);
        d1.setNombre("Dimensión A");
        d1.setOrden(1);
        for (int n = 1; n <= 3; n++) {
            Indicador i = new Indicador();
            i.setId(10 + n);
            i.setNumero(n);
            i.setDescripcion("Indicador " + n);
            i.setDimension(d1);
            d1.getIndicadores().add(i);
        }
        p.getDimensiones().add(d1);
        return p;
    }

    private EvaluacionCrearRequest crearRequest(LocalTime inicio, LocalTime termino) {
        return new EvaluacionCrearRequest(3L, 1, null, LocalDate.of(2026, 9, 22), "Liceo Ejemplo", "1° Medio B",
                inicio, termino, "Objetivo de la clase", null);
    }

    private EvaluacionDto crearBorrador() {
        return servicio.crear(crearRequest(LocalTime.of(10, 0), LocalTime.of(11, 30)), actor);
    }

    private Evaluacion borradorEnMemoria() {
        crearBorrador();
        Evaluacion e = new Evaluacion();
        e.setId(50L);
        e.setInscripcion(inscripcion);
        e.setPauta(pauta);
        e.setEvaluador(guia);
        e.setFechaClase(LocalDate.of(2026, 9, 22));
        lenient().when(evaluaciones.findById(50L)).thenReturn(Optional.of(e));
        return e;
    }

    private static RespuestasRequest respuestas(int... idYPuntaje) {
        var items = new java.util.ArrayList<RespuestasRequest.Item>();
        for (int i = 0; i < idYPuntaje.length; i += 2) {
            items.add(new RespuestasRequest.Item(idYPuntaje[i], idYPuntaje[i + 1], null));
        }
        return new RespuestasRequest(items);
    }

    // ---------- crear ----------

    @Test
    void crearDejaLaEvaluacionEnBorrador() {
        EvaluacionDto dto = crearBorrador();
        assertThat(dto.estado()).isEqualTo(EstadoEvaluacion.BORRADOR);
        assertThat(dto.puntajeTotal()).isNull();
        assertThat(dto.indicadoresTotales()).isEqualTo(3);
    }

    @Test
    void soloElGuiaAsignadoALaInscripcionPuedeEvaluarla() {
        UsuarioAutenticado otro = new UsuarioAutenticado(3L, "u3@demo.example", "PROFESOR_GUIA");
        assertThatThrownBy(() -> servicio.crear(crearRequest(LocalTime.of(10, 0), LocalTime.of(11, 0)), otro))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void laPautaDebeSerDeLaAsignaturaDeLaInscripcion() {
        pauta.setAsignatura(Datos.asignatura(5, true));
        assertThatThrownBy(this::crearBorrador).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("asignatura");
    }

    @Test
    void horaDeTerminoDebeSerPosteriorALaDeInicio() {
        assertThatThrownBy(() -> servicio.crear(crearRequest(LocalTime.of(11, 0), LocalTime.of(10, 0)), actor))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("hora");
    }

    @Test
    void soloSeEvaluaUnaInscripcionEnCurso() {
        inscripcion.setEstado(EstadoInscripcion.APROBADA);
        assertThatThrownBy(this::crearBorrador).isInstanceOf(ConflictoException.class);
    }

    // ---------- respuestas ----------

    @Test
    void puntajeFueraDeLaEscalaSeRechaza() {
        Evaluacion e = borradorEnMemoria();
        assertThatThrownBy(() -> servicio.registrarRespuestas(50L, respuestas(11, 8), actor))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("entre 1 y 7");
        assertThatThrownBy(() -> servicio.registrarRespuestas(50L, respuestas(11, 0), actor))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(e.getRespuestas()).isEmpty();   // no se guardó nada
    }

    @Test
    void indicadorDeOtraPautaSeRechaza() {
        borradorEnMemoria();
        assertThatThrownBy(() -> servicio.registrarRespuestas(50L, respuestas(999, 5), actor))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no pertenece a la pauta");
    }

    @Test
    void unPuntajeInvalidoImpideGuardarTambienLosValidosDeLaMismaPeticion() {
        Evaluacion e = borradorEnMemoria();
        assertThatThrownBy(() -> servicio.registrarRespuestas(50L, respuestas(11, 5, 12, 9), actor))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(e.getRespuestas()).isEmpty();
    }

    @Test
    void volverAPuntuarUnIndicadorReemplazaElValor() {
        Evaluacion e = borradorEnMemoria();
        servicio.registrarRespuestas(50L, respuestas(11, 3), actor);
        servicio.registrarRespuestas(50L, respuestas(11, 6, 12, 4), actor);
        assertThat(e.getRespuestas()).hasSize(2);
        assertThat(e.getRespuestas().stream().filter(r -> r.getIndicador().getId() == 11).findFirst().orElseThrow().getPuntaje())
                .isEqualTo(6);
    }

    @Test
    void otroGuiaNoPuedeModificarLaEvaluacion() {
        borradorEnMemoria();
        UsuarioAutenticado otro = new UsuarioAutenticado(3L, "u3@demo.example", "PROFESOR_GUIA");
        assertThatThrownBy(() -> servicio.registrarRespuestas(50L, respuestas(11, 5), otro))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    // ---------- finalizar ----------

    @Test
    void noSeFinalizaSiFaltanIndicadoresYElMensajeDiceCuales() {
        Evaluacion e = borradorEnMemoria();
        servicio.registrarRespuestas(50L, respuestas(11, 5), actor);
        assertThatThrownBy(() -> servicio.finalizar(50L, actor))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("faltan 2 de 3").hasMessageContaining("[2, 3]");
        assertThat(e.getEstado()).isEqualTo(EstadoEvaluacion.BORRADOR);
    }

    @Test
    void finalizarCalculaTotalYPromedioYCongelaLaEvaluacion() {
        Evaluacion e = borradorEnMemoria();
        servicio.registrarRespuestas(50L, respuestas(11, 5, 12, 5, 13, 6), actor);

        EvaluacionDto dto = servicio.finalizar(50L, actor);

        assertThat(dto.estado()).isEqualTo(EstadoEvaluacion.FINALIZADA);
        assertThat(dto.puntajeTotal()).isEqualTo(16);
        assertThat(dto.puntajeMaximo()).isEqualTo(21);
        assertThat(dto.promedio()).isEqualByComparingTo("5.33");   // 16 / 3 redondeado a 2 decimales
        assertThat(dto.fechaFinalizacion()).isNotNull();
        assertThat(e.estaFinalizada()).isTrue();

        assertThatThrownBy(() -> servicio.registrarRespuestas(50L, respuestas(11, 1), actor)).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> servicio.finalizar(50L, actor)).isInstanceOf(ConflictoException.class);
    }

    // ---------- visibilidad ----------

    @Test
    void unBorradorSoloLoVenElEvaluadorYElCoordinador() {
        Evaluacion e = borradorEnMemoria();
        lenient().when(acceso.puedeVerInscripcion(any(), any())).thenReturn(true);

        assertThat(servicio.puedeVer(e, actor)).isTrue();
        assertThat(servicio.puedeVer(e, new UsuarioAutenticado(1L, "c@demo.example", "COORDINADOR"))).isTrue();
        assertThat(servicio.puedeVer(e, new UsuarioAutenticado(6L, "e@demo.example", "ESTUDIANTE"))).isFalse();
        assertThat(servicio.puedeVer(e, new UsuarioAutenticado(4L, "p@demo.example", "PROFESOR_CURSO"))).isFalse();

        servicio.registrarRespuestas(50L, respuestas(11, 5, 12, 5, 13, 5), actor);
        servicio.finalizar(50L, actor);
        assertThat(servicio.puedeVer(e, new UsuarioAutenticado(6L, "e@demo.example", "ESTUDIANTE"))).isTrue();
        assertThat(servicio.puedeVer(e, new UsuarioAutenticado(4L, "p@demo.example", "PROFESOR_CURSO"))).isTrue();
    }
}

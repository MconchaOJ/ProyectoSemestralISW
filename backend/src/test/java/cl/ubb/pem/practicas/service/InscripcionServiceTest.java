package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import cl.ubb.pem.practicas.Datos;
import cl.ubb.pem.practicas.dto.InscripcionCierreRequest;
import cl.ubb.pem.practicas.dto.InscripcionCrearRequest;
import cl.ubb.pem.practicas.dto.InscripcionDto;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.AsignaturaPracticaRepository;
import cl.ubb.pem.practicas.repository.EstudianteRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import java.math.BigDecimal;
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

@ExtendWith(MockitoExtension.class)
class InscripcionServiceTest {

    @Mock
    private InscripcionRepository inscripciones;
    @Mock
    private EstudianteRepository estudiantes;
    @Mock
    private AsignaturaPracticaRepository asignaturas;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private AccesoService acceso;

    private InscripcionService servicio;
    private final Usuario guia = Datos.usuario(2, "PROFESOR_GUIA");
    private final Usuario curso = Datos.usuario(4, "PROFESOR_CURSO");
    private final Estudiante estudiante = Datos.estudiante(1, Datos.usuario(6, "ESTUDIANTE"));
    private final AsignaturaPractica conColegio = Datos.asignatura(3, true);
    private final AsignaturaPractica inicial = Datos.asignatura(1, false);

    @BeforeEach
    void preparar() {
        Clock hoy = Clock.fixed(Instant.parse("2026-10-10T15:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new InscripcionService(inscripciones, estudiantes, asignaturas, usuarios, acceso, hoy);
        lenient().when(estudiantes.findById(1L)).thenReturn(Optional.of(estudiante));
        lenient().when(asignaturas.findById(3)).thenReturn(Optional.of(conColegio));
        lenient().when(asignaturas.findById(1)).thenReturn(Optional.of(inicial));
        lenient().when(usuarios.findById(2L)).thenReturn(Optional.of(guia));
        lenient().when(usuarios.findById(4L)).thenReturn(Optional.of(curso));
        lenient().when(inscripciones.save(any(Inscripcion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void inscribeEnCursoConGuiaYProfesorDelCurso() {
        InscripcionDto dto = servicio.crear(new InscripcionCrearRequest(1L, 3, "2026-2", 2L, 4L, null));

        assertThat(dto.estado()).isEqualTo(EstadoInscripcion.EN_CURSO);
        assertThat(dto.profesorGuia().id()).isEqualTo(2L);
        assertThat(dto.profesorCurso().id()).isEqualTo(4L);
        assertThat(dto.notaFinal()).isNull();
    }

    @Test
    void inscripcionDuplicadaDaConflicto() {
        when(inscripciones.existsByEstudianteIdAndAsignaturaIdAndPeriodo(1L, 3, "2026-2")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(new InscripcionCrearRequest(1L, 3, "2026-2", 2L, 4L, null)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void elGuiaDebeTenerRolProfesorGuia() {
        // el usuario 4 es PROFESOR_CURSO: no puede ser guía
        assertThatThrownBy(() -> servicio.crear(new InscripcionCrearRequest(1L, 3, "2026-2", 4L, 4L, null)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("PROFESOR_GUIA");
    }

    @Test
    void elProfesorDelCursoDebeTenerRolProfesorCurso() {
        // el usuario 2 es PROFESOR_GUIA: no puede ser profesor del curso
        assertThatThrownBy(() -> servicio.crear(new InscripcionCrearRequest(1L, 3, "2026-2", 2L, 2L, null)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("PROFESOR_CURSO");
    }

    @Test
    void unProfesorInactivoNoSePuedeAsignar() {
        guia.setActivo(false);
        assertThatThrownBy(() -> servicio.crear(new InscripcionCrearRequest(1L, 3, "2026-2", 2L, 4L, null)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("inactivo");
    }

    @Test
    void asignaturaConColegioExigeGuiaPeroElCicloInicialNo() {
        assertThatThrownBy(() -> servicio.crear(new InscripcionCrearRequest(1L, 3, "2026-2", null, 4L, null)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("profesor guía");

        InscripcionDto dto = servicio.crear(new InscripcionCrearRequest(1L, 1, "2026-2", null, 4L, null));
        assertThat(dto.profesorGuia()).isNull();
    }

    @Test
    void enUnaAsignaturaSinColegioNoExisteProfesorGuia() {
        // Aproximación / Ambientes: no van a un colegio, así que no hay colaborador (S-25)
        assertThatThrownBy(() -> servicio.crear(new InscripcionCrearRequest(1L, 1, "2026-2", 2L, 4L, null)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no tiene profesor guía");
    }

    @Test
    void cierreAsignaEstadoNotaYFechaDeCierre() {
        Inscripcion i = Datos.inscripcion(7, estudiante, conColegio, guia, curso);
        when(inscripciones.findById(7L)).thenReturn(Optional.of(i));

        InscripcionDto dto = servicio.cerrar(7L, new InscripcionCierreRequest(EstadoInscripcion.APROBADA, new BigDecimal("5.8")));

        assertThat(dto.estado()).isEqualTo(EstadoInscripcion.APROBADA);
        assertThat(dto.notaFinal()).isEqualByComparingTo("5.8");
        assertThat(dto.fechaCierre()).isEqualTo(LocalDate.of(2026, 10, 10));   // HU-31: la cartola filtra por esta fecha
    }

    @Test
    void noSePuedeCerrarConEnCursoNiCerrarDosVeces() {
        Inscripcion i = Datos.inscripcion(7, estudiante, conColegio, guia, curso);
        i.setEstado(EstadoInscripcion.APROBADA);
        i.setNotaFinal(new BigDecimal("6.0"));
        lenient().when(inscripciones.findById(7L)).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> servicio.cerrar(7L, new InscripcionCierreRequest(EstadoInscripcion.EN_CURSO, new BigDecimal("5.0"))))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.cerrar(7L, new InscripcionCierreRequest(EstadoInscripcion.REPROBADA, new BigDecimal("3.0"))))
                .isInstanceOf(ConflictoException.class);
    }
}

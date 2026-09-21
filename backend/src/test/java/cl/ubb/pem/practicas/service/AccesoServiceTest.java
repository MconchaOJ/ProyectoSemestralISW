package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import cl.ubb.pem.practicas.Datos;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reglas de "quién puede ver qué" que dependen de los datos (no solo del rol). */
@ExtendWith(MockitoExtension.class)
class AccesoServiceTest {

    @Mock
    private InscripcionRepository inscripciones;
    private AccesoService acceso;

    private final Usuario usuarioEstudiante = Datos.usuario(6, "ESTUDIANTE");
    private final Estudiante estudiante = Datos.estudiante(1, usuarioEstudiante);
    private Inscripcion inscripcion;

    private static UsuarioAutenticado actor(long id, String rol) {
        return new UsuarioAutenticado(id, "u" + id + "@demo.example", rol);
    }

    @BeforeEach
    void preparar() {
        acceso = new AccesoService(inscripciones);
        inscripcion = Datos.inscripcion(3, estudiante, Datos.asignatura(3, true),
                Datos.usuario(2, "PROFESOR_GUIA"), Datos.usuario(4, "PROFESOR_CURSO"));
    }

    @Test
    void elAlcanceDeCadaRolLimitaLosListados() {
        assertThat(acceso.alcanceDe(actor(1, "COORDINADOR"))).isEqualTo(new AccesoService.Alcance(null, null, null));
        assertThat(acceso.alcanceDe(actor(2, "PROFESOR_GUIA"))).isEqualTo(new AccesoService.Alcance(2L, null, null));
        assertThat(acceso.alcanceDe(actor(4, "PROFESOR_CURSO"))).isEqualTo(new AccesoService.Alcance(null, 4L, null));
        assertThat(acceso.alcanceDe(actor(6, "ESTUDIANTE"))).isEqualTo(new AccesoService.Alcance(null, null, 6L));
    }

    @Test
    void unRolDesconocidoNoVeNadaEnLugarDeVerTodo() {
        assertThatThrownBy(() -> acceso.alcanceDe(actor(9, "TUTOR"))).isInstanceOf(AccesoDenegadoException.class);
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(9, "TUTOR"))).isFalse();
    }

    @Test
    void soloLosInvolucradosVenLaInscripcion() {
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(1, "COORDINADOR"))).isTrue();
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(6, "ESTUDIANTE"))).isTrue();
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(2, "PROFESOR_GUIA"))).isTrue();
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(4, "PROFESOR_CURSO"))).isTrue();

        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(7, "ESTUDIANTE"))).isFalse();
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(3, "PROFESOR_GUIA"))).isFalse();
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(5, "PROFESOR_CURSO"))).isFalse();
        assertThatThrownBy(() -> acceso.exigirVerInscripcion(inscripcion, actor(7, "ESTUDIANTE")))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void unaInscripcionSinGuiaNoLaVeNingunGuia() {
        inscripcion.setProfesorGuia(null);
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(2, "PROFESOR_GUIA"))).isFalse();
    }

    @Test
    void elHistorialLoVenElPropioEstudianteElCoordinadorYLosProfesoresAsignados() {
        lenient().when(inscripciones.existsByEstudianteIdAndProfesorGuiaId(1L, 2L)).thenReturn(true);
        lenient().when(inscripciones.existsByEstudianteIdAndProfesorCursoId(1L, 4L)).thenReturn(true);

        assertThat(acceso.puedeVerHistorial(estudiante, actor(1, "COORDINADOR"))).isTrue();
        assertThat(acceso.puedeVerHistorial(estudiante, actor(6, "ESTUDIANTE"))).isTrue();
        assertThat(acceso.puedeVerHistorial(estudiante, actor(2, "PROFESOR_GUIA"))).isTrue();
        assertThat(acceso.puedeVerHistorial(estudiante, actor(4, "PROFESOR_CURSO"))).isTrue();

        assertThat(acceso.puedeVerHistorial(estudiante, actor(7, "ESTUDIANTE"))).isFalse();
        assertThat(acceso.puedeVerHistorial(estudiante, actor(3, "PROFESOR_GUIA"))).isFalse();
        assertThat(acceso.puedeVerHistorial(estudiante, actor(5, "PROFESOR_CURSO"))).isFalse();
        assertThat(acceso.puedeVerHistorial(estudiante, actor(9, "TUTOR"))).isFalse();
    }
}

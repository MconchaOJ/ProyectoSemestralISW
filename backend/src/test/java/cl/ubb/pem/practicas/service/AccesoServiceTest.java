package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

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
    void elTutorVeSoloLasInscripcionesDondeEsTutor() {   // HU-07
        // La consulta no filtra por tutor: se pide sin filtro y el servicio deja las filas que puedeVerInscripcion acepta.
        assertThat(acceso.alcanceDe(actor(8, "TUTOR"))).isEqualTo(new AccesoService.Alcance(null, null, null));
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(8, "TUTOR"))).isFalse();
        inscripcion.setTutor(Datos.usuario(8, "TUTOR"));
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(8, "TUTOR"))).isTrue();
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(9, "TUTOR"))).isFalse();   // otro tutor
        when(inscripciones.existsByEstudianteIdAndTutorId(1L, 8L)).thenReturn(true);
        assertThat(acceso.puedeVerHistorial(estudiante, actor(8, "TUTOR"))).isTrue();
        assertThat(acceso.puedeVerHistorial(estudiante, actor(9, "TUTOR"))).isFalse();
    }

    @Test
    void unRolDesconocidoNoVeNadaEnLugarDeVerTodo() {
        assertThatThrownBy(() -> acceso.alcanceDe(actor(9, "INVITADO"))).isInstanceOf(AccesoDenegadoException.class);
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(9, "INVITADO"))).isFalse();
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
        assertThat(acceso.puedeVerHistorial(estudiante, actor(9, "INVITADO"))).isFalse();
    }

    // ---------- Cuentas con dos roles (migración 004) y jefe de carrera con lectura total (2026-10-11) ----------

    private static UsuarioAutenticado actor(long id, String rol, String rolSecundario) {
        return new UsuarioAutenticado(id, "u" + id + "@demo.example", rol, rolSecundario);
    }

    @Test
    void elJefeDeCarreraLeeTodoComoElCoordinador() {
        UsuarioAutenticado jefe = actor(32, "JEFE_CARRERA");
        assertThat(acceso.alcanceDe(jefe)).isEqualTo(new AccesoService.Alcance(null, null, null));
        assertThat(acceso.puedeVerInscripcion(inscripcion, jefe)).isTrue();
        assertThat(acceso.puedeVerHistorial(estudiante, jefe)).isTrue();
        assertThat(AccesoService.veTodo(jefe)).isTrue();
        // y no consulta la base para decidirlo
        verifyNoInteractions(inscripciones);
    }

    @Test
    void conDosRolesVeLaSumaDeLoQueVeCadaUno() {
        // guía 2 de esta inscripción y, además, profesor del curso de otras: la ve por ser su guía
        UsuarioAutenticado guiaYCurso = actor(2, "PROFESOR_GUIA", "PROFESOR_CURSO");
        assertThat(acceso.puedeVerInscripcion(inscripcion, guiaYCurso)).isTrue();
        // usuario 4 como profesor del curso más un rol desconocido: la ve por el rol que sí conoce
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(4, "INVITADO", "PROFESOR_CURSO"))).isTrue();
        // alguien sin relación con la inscripción no la ve aunque tenga dos roles
        assertThat(acceso.puedeVerInscripcion(inscripcion, actor(3, "PROFESOR_GUIA", "PROFESOR_CURSO"))).isFalse();
        // dos roles restringidos: la consulta no puede expresar el "o", se pide sin filtro y se filtra fila por fila
        assertThat(acceso.alcanceDe(guiaYCurso)).isEqualTo(new AccesoService.Alcance(null, null, null));
        // un rol conocido y uno desconocido: queda el alcance del conocido
        assertThat(acceso.alcanceDe(actor(4, "INVITADO", "PROFESOR_CURSO"))).isEqualTo(new AccesoService.Alcance(null, 4L, null));
    }

    @Test
    void elJefeDeCarreraQueTambienEsGuiaLeeTodo() {
        UsuarioAutenticado jefeGuia = actor(32, "JEFE_CARRERA", "PROFESOR_GUIA");
        assertThat(acceso.alcanceDe(jefeGuia)).isEqualTo(new AccesoService.Alcance(null, null, null));
        assertThat(acceso.puedeVerInscripcion(inscripcion, jefeGuia)).isTrue();
        assertThat(jefeGuia.tieneRol("PROFESOR_GUIA")).isTrue();
        assertThat(jefeGuia.tieneRol("COORDINADOR")).isFalse();
    }

    @Test
    void elChatLoLeenElEstudianteYSuGuiaPeroNoElCursoNiElTutorNiElCoordinador() {   // HU-15/HU-16, S-60
        assertThat(acceso.puedeVerChat(inscripcion, actor(6, "ESTUDIANTE"))).isTrue();
        assertThat(acceso.puedeVerChat(inscripcion, actor(2, "PROFESOR_GUIA"))).isTrue();
        assertThat(acceso.puedeVerChat(inscripcion, actor(7, "ESTUDIANTE"))).isFalse();      // otro estudiante
        assertThat(acceso.puedeVerChat(inscripcion, actor(3, "PROFESOR_GUIA"))).isFalse();   // otro guía
        // ven la inscripción, pero no el chat: la privacidad de los chats sigue abierta con el cliente (S-38)
        inscripcion.setTutor(Datos.usuario(8, "TUTOR"));
        inscripcion.getAsignatura().setChatbotHabilitado(true);
        for (UsuarioAutenticado sinChat : new UsuarioAutenticado[] {actor(4, "PROFESOR_CURSO"), actor(8, "TUTOR"),
                actor(1, "COORDINADOR")}) {
            assertThat(acceso.puedeVerInscripcion(inscripcion, sinChat)).isTrue();
            assertThat(acceso.puedeVerChat(inscripcion, sinChat)).isFalse();
        }
        // el jefe de carrera que además es el guía de ESTA inscripción lo lee por su segundo rol
        assertThat(acceso.puedeVerChat(inscripcion, actor(2, "JEFE_CARRERA", "PROFESOR_GUIA"))).isTrue();
        assertThatThrownBy(() -> acceso.exigirVerChat(inscripcion, actor(4, "PROFESOR_CURSO")))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void elJefeDeCarreraLeeLosChatsDeLasPracticasConChatbot() {   // HU-20, SUPUESTO S-66
        UsuarioAutenticado jefe = actor(32, "JEFE_CARRERA");
        assertThat(acceso.puedeVerChat(inscripcion, jefe)).isFalse();     // asignatura sin chatbot
        inscripcion.getAsignatura().setChatbotHabilitado(true);
        assertThat(acceso.puedeVerChat(inscripcion, jefe)).isTrue();      // sin ser su guía
        assertThat(acceso.leeChatComoJefe(inscripcion, jefe)).isTrue();
        assertThat(acceso.leeChatComoJefe(inscripcion, actor(1, "COORDINADOR"))).isFalse();
    }
}

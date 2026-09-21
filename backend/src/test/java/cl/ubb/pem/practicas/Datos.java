package cl.ubb.pem.practicas;

import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.Ciclo;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Modalidad;
import cl.ubb.pem.practicas.entity.Rol;
import cl.ubb.pem.practicas.entity.Usuario;

/** Fábricas de entidades en memoria para las pruebas unitarias (sin base de datos). */
public final class Datos {

    private Datos() {
    }

    public static Usuario usuario(long id, String rol) {
        Rol r = new Rol();
        r.setNombre(rol);
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(r);
        u.setNombre("Nombre" + id);
        u.setApellidos("Apellido" + id);
        u.setEmail("u" + id + "@demo.example");
        u.setActivo(true);
        u.setPasswordHash("$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        return u;
    }

    public static Estudiante estudiante(long id, Usuario usuario) {
        Estudiante e = new Estudiante();
        e.setId(id);
        e.setUsuario(usuario);
        e.setRut("11111111-1");
        e.setSemestreActual(6);
        e.setAnioIngreso(2024);
        return e;
    }

    public static AsignaturaPractica asignatura(int id, boolean requiereCentro) {
        AsignaturaPractica a = new AsignaturaPractica();
        a.setId(id);
        a.setCodigo("PEM-P" + id);
        a.setNombre("Asignatura " + id);
        a.setOrden(id);
        a.setCiclo(requiereCentro ? Ciclo.INTERMEDIO : Ciclo.INICIAL);
        a.setModalidad(Modalidad.INDIVIDUAL);
        a.setRequiereCentroEducativo(requiereCentro);
        a.setChatbotHabilitado(false);
        a.setRequierePlanificacion(true);
        return a;
    }

    public static Inscripcion inscripcion(long id, Estudiante e, AsignaturaPractica a, Usuario guia, Usuario curso) {
        Inscripcion i = new Inscripcion();
        i.setId(id);
        i.setEstudiante(e);
        i.setAsignatura(a);
        i.setPeriodo("2026-2");
        i.setProfesorGuia(guia);
        i.setProfesorCurso(curso);
        return i;
    }
}

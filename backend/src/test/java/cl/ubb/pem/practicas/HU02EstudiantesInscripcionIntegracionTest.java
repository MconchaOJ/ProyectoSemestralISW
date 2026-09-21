package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** HU-02: estudiantes, profesores, asignaturas e inscripciones, contra la base real (cada prueba se revierte). */
class HU02EstudiantesInscripcionIntegracionTest extends IntegracionBase {

    private Map<String, Object> estudianteNuevo(String email, String rut) {
        return Map.of("nombre", "Ana", "apellidos", "Soto Reyes", "email", email, "password", "clave-de-prueba-1",
                "rut", rut, "semestreActual", 6, "anioIngreso", 2024);
    }

    // ---------- Estudiantes ----------

    @Test
    void coordinadorRegistraUnEstudianteYSeCreanUsuarioYFicha() throws Exception {
        JsonNode e = cuerpo(enviar("/api/estudiantes", COORDINADOR, estudianteNuevo("ana.soto@demo.example", "12.345.678-5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rut").value("12345678-5"))
                .andExpect(jsonPath("$.semestreActual").value(6)));

        sincronizar();   // fuerza a la base a validar CHECK y UNIQUE
        assertThat(e.toString()).doesNotContain("password").doesNotContain("$2a$");
        // el usuario quedó con rol ESTUDIANTE y una clave cifrada
        var usuario = usuarios.findByEmailIgnoreCase("ana.soto@demo.example").orElseThrow();
        assertThat(usuario.getRol().getNombre()).isEqualTo("ESTUDIANTE");
        assertThat(usuario.getPasswordHash()).startsWith("$2a$10$");
        consultar("/api/estudiantes/" + e.get("id").asLong(), COORDINADOR).andExpect(status().isOk());
    }

    @Test
    void correoDuplicadoYRutDuplicadoDan409() throws Exception {
        enviar("/api/estudiantes", COORDINADOR, estudianteNuevo(CAMILA, "12345678-5")).andExpect(status().isConflict());
        enviar("/api/estudiantes", COORDINADOR, estudianteNuevo("otro@demo.example", "11111111-1")).andExpect(status().isConflict());
    }

    @Test
    void rutInvalidoOCamposInvalidosDan400() throws Exception {
        enviar("/api/estudiantes", COORDINADOR, estudianteNuevo("x@demo.example", "12345678-4")).andExpect(status().isBadRequest());

        Map<String, Object> malo = Map.of("nombre", "", "apellidos", "S", "email", "no-es-correo", "password", "corta",
                "rut", "12345678-5", "semestreActual", 13, "anioIngreso", 2024);
        JsonNode err = cuerpo(enviar("/api/estudiantes", COORDINADOR, malo).andExpect(status().isBadRequest()));
        List<String> campos = err.get("errores").findValuesAsText("campo");
        assertThat(campos).contains("nombre", "email", "password", "semestreActual");
    }

    @Test
    void soloElCoordinadorPuedeRegistrarYListarEstudiantes() throws Exception {
        for (String rol : List.of(GUIA1, CURSO1, CAMILA)) {
            enviar("/api/estudiantes", rol, estudianteNuevo("x@demo.example", "12345678-5")).andExpect(status().isForbidden());
            consultar("/api/estudiantes", rol).andExpect(status().isForbidden());
        }
        enviar("/api/estudiantes", null, estudianteNuevo("x@demo.example", "12345678-5")).andExpect(status().isUnauthorized());
    }

    @Test
    void seBuscaPorNombreSinTildesOPorRut() throws Exception {
        consultar("/api/estudiantes?buscar=sebastian", COORDINADOR)          // en la base es "Sebastián"
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rut").value("44444444-4"));
        consultar("/api/estudiantes?buscar=33333333", COORDINADOR).andExpect(jsonPath("$[0].nombre").value("Valentina"));
        consultar("/api/estudiantes", COORDINADOR).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void elEstudianteVeSuPropiaFicha() throws Exception {
        consultar("/api/estudiantes/me", DIEGO).andExpect(status().isOk()).andExpect(jsonPath("$.rut").value("22222222-2"));
        consultar("/api/estudiantes/me", COORDINADOR).andExpect(status().isForbidden());
    }

    // ---------- Profesores ----------

    @Test
    void coordinadorRegistraProfesoresGuiaYDelCurso() throws Exception {
        for (String rol : List.of("PROFESOR_GUIA", "PROFESOR_CURSO")) {
            enviar("/api/profesores", COORDINADOR, Map.of("nombre", "Camilo", "apellidos", "Araya",
                    "email", rol.toLowerCase() + "@demo.example", "password", "clave-de-prueba-1", "rol", rol))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value(rol));
        }
        // un profesor no puede crearse con rol ESTUDIANTE ni COORDINADOR
        enviar("/api/profesores", COORDINADOR, Map.of("nombre", "X", "apellidos", "Y", "email", "z@demo.example",
                "password", "clave-de-prueba-1", "rol", "COORDINADOR")).andExpect(status().isBadRequest());
        enviar("/api/profesores", GUIA1, Map.of("nombre", "X", "apellidos", "Y", "email", "z@demo.example",
                "password", "clave-de-prueba-1", "rol", "PROFESOR_GUIA")).andExpect(status().isForbidden());
    }

    @Test
    void seListanLosProfesoresConFiltroPorRol() throws Exception {
        consultar("/api/profesores?rol=PROFESOR_GUIA", COORDINADOR).andExpect(jsonPath("$.length()").value(2));
        consultar("/api/profesores?rol=PROFESOR_CURSO", COORDINADOR).andExpect(jsonPath("$.length()").value(2));
        consultar("/api/profesores", COORDINADOR).andExpect(jsonPath("$.length()").value(4));
        consultar("/api/profesores?rol=ESTUDIANTE", COORDINADOR).andExpect(status().isBadRequest());
    }

    // ---------- Asignaturas ----------

    @Test
    void existenLasSeisAsignaturasYElChatbotSoloAplicaEnLasDosFinales() throws Exception {
        JsonNode lista = cuerpo(consultar("/api/asignaturas", CAMILA).andExpect(status().isOk()));
        assertThat(lista).hasSize(6);
        assertThat(lista.findValuesAsText("chatbotHabilitado")).containsExactly("false", "false", "false", "false", "true", "true");
        assertThat(lista.get(0).get("ciclo").asText()).isEqualTo("INICIAL");
        assertThat(lista.findValuesAsText("modalidad")).containsExactly("GRUPO", "DUPLA", "DUPLA", "DUPLA", "INDIVIDUAL", "INDIVIDUAL");
        // solo Aproximación al Sistema Escolar no entrega planificaciones (solo trabajos hechos en clase)
        assertThat(lista.findValuesAsText("requierePlanificacion")).containsExactly("false", "true", "true", "true", "true", "true");
        consultar("/api/asignaturas", null).andExpect(status().isUnauthorized());
    }

    // ---------- Inscripciones ----------

    private Map<String, Object> inscripcion(long estudianteId, int asignaturaId, String periodo, Long guia, long curso) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("estudianteId", estudianteId);
        m.put("asignaturaId", asignaturaId);
        m.put("periodo", periodo);
        m.put("profesorGuiaId", guia);
        m.put("profesorCursoId", curso);
        return m;
    }

    @Test
    void coordinadorInscribeYLaInscripcionQuedaEnCurso() throws Exception {
        // Javiera (5) en Trabajo Colaborativo (3), periodo 2026-2, guía 2 y profesor del curso 4
        cuerpo(enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 3, "2026-2", 2L, 4))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.profesorGuia.id").value(2))
                .andExpect(jsonPath("$.asignatura.codigo").value("PEM-P3")));
        sincronizar();
    }

    @Test
    void inscribirDosVecesEnLaMismaAsignaturaYPeriodoDa409() throws Exception {
        // Camila (1) ya está en Trabajo Colaborativo 2026-2
        enviar("/api/inscripciones", COORDINADOR, inscripcion(1, 3, "2026-2", 2L, 4)).andExpect(status().isConflict());
        // en otro periodo sí se puede
        enviar("/api/inscripciones", COORDINADOR, inscripcion(1, 3, "2027-1", 2L, 4)).andExpect(status().isCreated());
    }

    @Test
    void guiaYProfesorDelCursoDebenTenerSuRol() throws Exception {
        enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 3, "2026-2", 4L, 4)).andExpect(status().isBadRequest());  // guía con rol de curso
        enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 3, "2026-2", 2L, 2)).andExpect(status().isBadRequest());  // curso con rol de guía
        enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 3, "2026-2", 6L, 4)).andExpect(status().isBadRequest());  // guía = estudiante
    }

    @Test
    void elGuiaPuedeQuedarVacioSoloEnElCicloInicial() throws Exception {
        enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 2, "2026-2", null, 4))       // Ambientes de Aprendizaje: inicial
                .andExpect(status().isCreated());
        enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 3, "2026-2", null, 4))       // Trabajo Colaborativo va al colegio
                .andExpect(status().isBadRequest());
    }

    @Test
    void dondeNoSeVaAUnColegioNoExisteProfesorGuia() throws Exception {
        // Aproximación (1) y Ambientes (2) no tienen colaborador: asignarle un guía es un error
        enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 2, "2026-2", 2L, 4)).andExpect(status().isBadRequest());
        enviar("/api/inscripciones", COORDINADOR, inscripcion(2, 1, "2027-1", 2L, 4)).andExpect(status().isBadRequest());
        enviar("/api/inscripciones", COORDINADOR, inscripcion(2, 1, "2027-1", null, 4)).andExpect(status().isCreated());
    }

    @Test
    void inscripcionConIdsInexistentesDa404() throws Exception {
        enviar("/api/inscripciones", COORDINADOR, inscripcion(999, 3, "2026-2", 2L, 4)).andExpect(status().isNotFound());
        enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 99, "2026-2", 2L, 4)).andExpect(status().isNotFound());
        enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 3, "2026-2", 2L, 999)).andExpect(status().isNotFound());
    }

    @Test
    void periodoConFormatoInvalidoDa400() throws Exception {
        enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 3, "2026-3", 2L, 4)).andExpect(status().isBadRequest());
        enviar("/api/inscripciones", COORDINADOR, inscripcion(5, 3, "26-2", 2L, 4)).andExpect(status().isBadRequest());
    }

    @Test
    void soloElCoordinadorInscribe() throws Exception {
        for (String rol : List.of(GUIA1, CURSO1, CAMILA)) {
            enviar("/api/inscripciones", rol, inscripcion(5, 3, "2026-2", 2L, 4)).andExpect(status().isForbidden());
        }
        enviar("/api/inscripciones", null, inscripcion(5, 3, "2026-2", 2L, 4)).andExpect(status().isUnauthorized());
    }

    @Test
    void cadaRolVeSoloSusInscripciones() throws Exception {
        assertThat(cuerpo(consultar("/api/inscripciones", COORDINADOR))).hasSize(18);
        // Camila tiene 3 inscripciones (ids 1, 2 y 3)
        assertThat(ids(cuerpo(consultar("/api/inscripciones", CAMILA)))).containsExactlyInAnyOrder(1L, 2L, 3L);
        // el guía 2 (Marcos) figura en las inscripciones 3, 9, 10 y 14
        assertThat(ids(cuerpo(consultar("/api/inscripciones", GUIA1)))).containsExactlyInAnyOrder(3L, 9L, 10L, 14L);
        // filtro por periodo dentro del alcance
        consultar("/api/inscripciones?periodo=2026-2", GUIA1).andExpect(jsonPath("$.length()").value(1));
        consultar("/api/inscripciones?asignaturaId=3&periodo=2026-2", COORDINADOR).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void elDetalleDeUnaInscripcionAjenaDa403() throws Exception {
        consultar("/api/inscripciones/3", CAMILA).andExpect(status().isOk());       // la suya
        consultar("/api/inscripciones/6", CAMILA).andExpect(status().isForbidden()); // de Diego
        consultar("/api/inscripciones/6", GUIA1).andExpect(status().isForbidden());  // Diego tiene de guía a Lorena (3)
        consultar("/api/inscripciones/6", GUIA2).andExpect(status().isOk());
        consultar("/api/inscripciones/999", COORDINADOR).andExpect(status().isNotFound());
    }

    @Test
    void elCoordinadorCierraUnaInscripcionConSuNota() throws Exception {
        enviar("/api/inscripciones/18", COORDINADOR, Map.of()).andExpect(status().isMethodNotAllowed());   // la ruta es PUT
        reemplazar("/api/inscripciones/18/cierre", COORDINADOR, Map.of("estado", "APROBADA", "notaFinal", 5.8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.notaFinal").value(5.8));
        sincronizar();
        reemplazar("/api/inscripciones/18/cierre", COORDINADOR, Map.of("estado", "REPROBADA", "notaFinal", 3.0))
                .andExpect(status().isConflict());
        reemplazar("/api/inscripciones/3/cierre", COORDINADOR, Map.of("estado", "APROBADA", "notaFinal", 8.0))
                .andExpect(status().isBadRequest());
        reemplazar("/api/inscripciones/3/cierre", COORDINADOR, Map.of("estado", "EN_CURSO", "notaFinal", 5.0))
                .andExpect(status().isBadRequest());
        reemplazar("/api/inscripciones/3/cierre", GUIA1, Map.of("estado", "APROBADA", "notaFinal", 5.0))
                .andExpect(status().isForbidden());
    }
}

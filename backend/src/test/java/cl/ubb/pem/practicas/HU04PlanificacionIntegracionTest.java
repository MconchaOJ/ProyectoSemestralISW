package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * HU-04: planificación con flujo de aprobación, contra la base real (cada prueba se revierte).
 * Seed: inscripción 3 = Camila (guía Marcos, profesor del curso Ricardo); planificaciones de Camila: 3 AUTORIZADA,
 * 4 CON_OBSERVACIONES y 5 BORRADOR incompleto. Diego (inscripción 6, guía Lorena, curso Ricardo): 6 EN_REVISION_GUIA
 * y 7 EN_AUTORIZACION.
 * Desde S-47, enviar a revisión también exige al menos un documento adjunto (la planificación formal en Word o
 * PDF, HU-21): {@link #adjuntarDocumentoFormal} lo sube para dejar la planificación lista para enviarse.
 */
class HU04PlanificacionIntegracionTest extends IntegracionBase {

    private static final byte[] PDF_PEQUENO = "%PDF-1.4 planificación formal de prueba".getBytes();

    /** S-47: sube el documento formal para que la planificación quede lista para "enviar" (una vez basta). */
    private void adjuntarDocumentoFormal(long planificacionId, String estudiante) throws Exception {
        MockMultipartFile pdf = archivo("archivo", "planificacion.pdf", "application/pdf", PDF_PEQUENO);
        mvc.perform(multipart("/api/planificaciones/" + planificacionId + "/documentos", estudiante).file(pdf))
                .andExpect(status().isCreated());
    }

    private Map<String, Object> completa(long inscripcionId) {
        Map<String, Object> m = new HashMap<>();
        m.put("inscripcionId", inscripcionId);
        m.put("titulo", "Sistemas de ecuaciones 2x2");
        m.put("fechaClase", LocalDate.now().plusDays(10).toString());   // relativa a hoy: la regla exige 3 días de anticipación
        m.put("objetivo", "Resolver sistemas de ecuaciones por sustitución.");
        m.put("recursos", "Guía de problemas y pizarra.");
        m.put("tiempos", "Inicio 10 min; desarrollo 65 min; cierre 15 min.");
        m.put("duracionMinutos", 90);
        m.put("evaluacionAprendizaje", "Problema de cierre resuelto por cada estudiante.");
        m.put("interaccionParticipantes", "Los estudiantes comparan estrategias en grupos.");
        return m;
    }

    private Map<String, Object> sinInscripcion(Map<String, Object> m) {
        Map<String, Object> copia = new HashMap<>(m);
        copia.remove("inscripcionId");
        return copia;
    }

    private long crearCompleta() throws Exception {
        long id = cuerpo(enviar("/api/planificaciones", CAMILA, completa(3)).andExpect(status().isCreated())).get("id").asLong();
        adjuntarDocumentoFormal(id, CAMILA);
        return id;
    }

    private Map<String, Object> decision(String d, String comentario) {
        Map<String, Object> m = new HashMap<>();
        m.put("decision", d);
        m.put("comentario", comentario);
        return m;
    }

    private void estadoEs(long id, String esperado) throws Exception {
        sincronizar();   // la base valida CHECK (planificación completa, comentario obligatorio) en cada paso
        consultar("/api/planificaciones/" + id, COORDINADOR).andExpect(jsonPath("$.estado").value(esperado));
    }

    // ---------- Flujo completo ----------

    @Test
    void flujoCompletoConIdaYVueltaHastaQuedarAutorizadaYConTodoElHistorial() throws Exception {
        long id = crearCompleta();
        String base = "/api/planificaciones/" + id;
        estadoEs(id, "BORRADOR");

        enviar(base + "/enviar", CAMILA).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_REVISION_GUIA"))
                .andExpect(jsonPath("$.pendienteDe").value("PROFESOR_GUIA"));
        estadoEs(id, "EN_REVISION_GUIA");

        // nadie puede saltarse al guía, ni otro guía, ni el estudiante
        enviar(base + "/revision-curso", CURSO1, decision("APROBADA", null)).andExpect(status().isConflict());
        enviar(base + "/revision-guia", GUIA2, decision("APROBADA", null)).andExpect(status().isForbidden());
        enviar(base + "/revision-guia", CAMILA, decision("APROBADA", null)).andExpect(status().isForbidden());
        reemplazar(base, CAMILA, sinInscripcion(completa(3))).andExpect(status().isConflict());   // en revisión ya no se edita

        // el guía la devuelve: el comentario es obligatorio
        enviar(base + "/revision-guia", GUIA1, decision("OBSERVADA", null)).andExpect(status().isBadRequest());
        enviar(base + "/revision-guia", GUIA1, decision("OBSERVADA", "Incluir el uso de los errores como devolución."))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CON_OBSERVACIONES"));
        estadoEs(id, "CON_OBSERVACIONES");

        // el estudiante corrige y reenvía
        Map<String, Object> mejorada = sinInscripcion(completa(3));
        mejorada.put("evaluacionAprendizaje", "Ticket de salida y análisis de errores frecuentes.");
        reemplazar(base, CAMILA, mejorada).andExpect(status().isOk());
        enviar(base + "/enviar", CAMILA).andExpect(status().isOk());
        enviar(base + "/revision-guia", GUIA1, decision("APROBADA", "Ahora sí.")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_AUTORIZACION")).andExpect(jsonPath("$.pendienteDe").value("PROFESOR_CURSO"));
        estadoEs(id, "EN_AUTORIZACION");

        // el profesor del curso: otro profesor no puede; el asignado la devuelve y el flujo vuelve al guía (S-10)
        enviar(base + "/revision-curso", CURSO2, decision("APROBADA", null)).andExpect(status().isForbidden());
        enviar(base + "/revision-guia", GUIA1, decision("APROBADA", null)).andExpect(status().isConflict());
        enviar(base + "/revision-curso", CURSO1, decision("OBSERVADA", "Falta justificar los tiempos.")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CON_OBSERVACIONES"));
        enviar(base + "/enviar", CAMILA).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_REVISION_GUIA"));
        enviar(base + "/revision-guia", GUIA1, decision("APROBADA", null)).andExpect(status().isOk());
        enviar(base + "/revision-curso", CURSO1, decision("APROBADA", "Autorizada.")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AUTORIZADA")).andExpect(jsonPath("$.pendienteDe").doesNotExist());
        estadoEs(id, "AUTORIZADA");

        // AUTORIZADA: no se modifica ni se reenvía ni se revisa otra vez (409)
        reemplazar(base, CAMILA, sinInscripcion(completa(3))).andExpect(status().isConflict());
        enviar(base + "/enviar", CAMILA).andExpect(status().isConflict());
        enviar(base + "/revision-curso", CURSO1, decision("OBSERVADA", "tarde")).andExpect(status().isConflict());
        enviar(base + "/revision-guia", GUIA1, decision("APROBADA", null)).andExpect(status().isConflict());

        // historial: autor, etapa, decisión y comentario, en orden cronológico
        JsonNode revisiones = cuerpo(consultar(base, GUIA1).andExpect(status().isOk())).get("revisiones");
        List<String> resumen = new java.util.ArrayList<>();
        revisiones.forEach(r -> resumen.add(r.get("etapa").asText() + ":" + r.get("decision").asText()));
        assertThat(resumen).containsExactly("GUIA:OBSERVADA", "GUIA:APROBADA", "CURSO:OBSERVADA", "GUIA:APROBADA", "CURSO:APROBADA");
        assertThat(revisiones.get(0).get("revisor").get("email").asText()).isEqualTo(GUIA1);
        assertThat(revisiones.get(0).get("comentario").asText()).contains("errores");
        assertThat(revisiones.get(4).get("revisor").get("email").asText()).isEqualTo(CURSO1);
    }

    @Test
    void elFlujoTambienFuncionaConLasPlanificacionesDelSeed() throws Exception {
        // Diego: 6 está EN_REVISION_GUIA con guía Lorena; 7 está EN_AUTORIZACION con profesor del curso Ricardo
        enviar("/api/planificaciones/6/revision-guia", GUIA2, decision("APROBADA", null)).andExpect(jsonPath("$.estado").value("EN_AUTORIZACION"));
        enviar("/api/planificaciones/7/revision-curso", CURSO1, decision("APROBADA", null)).andExpect(jsonPath("$.estado").value("AUTORIZADA"));
        sincronizar();
        enviar("/api/planificaciones/6/revision-guia", GUIA1, decision("APROBADA", null)).andExpect(status().isForbidden());  // Marcos no es su guía
    }

    // ---------- Borradores y validaciones ----------

    @Test
    void unBorradorPuedeEstarIncompletoPeroNoSePuedeEnviarAsi() throws Exception {
        long id = cuerpo(enviar("/api/planificaciones", CAMILA, Map.of("inscripcionId", 3, "titulo", "Solo título"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("BORRADOR"))).get("id").asLong();
        sincronizar();
        JsonNode err = cuerpo(enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isBadRequest()));
        assertThat(err.get("mensaje").asText()).contains("objetivo", "recursos", "tiempos", "evaluacionAprendizaje", "interaccionParticipantes");
        // el borrador del seed (id 5) tampoco se puede enviar
        enviar("/api/planificaciones/5/enviar", CAMILA).andExpect(status().isBadRequest());
    }

    @Test
    void sinDocumentoFormalAdjuntoNoSePuedeEnviarAunqueLosCamposEstenCompletos() throws Exception {
        long id = cuerpo(enviar("/api/planificaciones", CAMILA, completa(3)).andExpect(status().isCreated())).get("id").asLong();
        JsonNode err = cuerpo(enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isBadRequest()));
        assertThat(err.get("mensaje").asText()).contains("documentoFormal");
        assertThat(err.get("mensaje").asText()).doesNotContain("objetivo", "recursos");   // los demás campos sí están completos

        adjuntarDocumentoFormal(id, CAMILA);
        enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION_GUIA"));
    }

    @Test
    void elTituloEsObligatorioYLosTextosVaciosCuentanComoNoCompletados() throws Exception {
        enviar("/api/planificaciones", CAMILA, Map.of("inscripcionId", 3, "titulo", " ")).andExpect(status().isBadRequest());
        Map<String, Object> conVacios = completa(3);
        conVacios.put("recursos", "   ");
        long id = cuerpo(enviar("/api/planificaciones", CAMILA, conVacios).andExpect(status().isCreated())).get("id").asLong();
        enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isBadRequest());
        Map<String, Object> duracionInvalida = completa(3);
        duracionInvalida.put("duracionMinutos", 0);
        enviar("/api/planificaciones", CAMILA, duracionInvalida).andExpect(status().isBadRequest());
    }

    @Test
    void unaPlanificacionAutorizadaDelSeedNoSePuedeModificar() throws Exception {
        reemplazar("/api/planificaciones/3", CAMILA, sinInscripcion(completa(3))).andExpect(status().isConflict());
        enviar("/api/planificaciones/3/enviar", CAMILA).andExpect(status().isConflict());
        // una CON_OBSERVACIONES (id 4) sí se corrige y se reenvía (S-47: el seed no trae documentos, hay que adjuntar uno)
        reemplazar("/api/planificaciones/4", CAMILA, sinInscripcion(completa(3))).andExpect(status().isOk());
        adjuntarDocumentoFormal(4, CAMILA);
        enviar("/api/planificaciones/4/enviar", CAMILA).andExpect(jsonPath("$.estado").value("EN_REVISION_GUIA"));
        sincronizar();
    }

    @Test
    void soloSePlanificaEnInscripcionesPropiasYEnCurso() throws Exception {
        enviar("/api/planificaciones", CAMILA, completa(6)).andExpect(status().isForbidden());      // inscripción de Diego
        enviar("/api/planificaciones", CAMILA, completa(2)).andExpect(status().isConflict());       // inscripción 2 ya APROBADA
        enviar("/api/planificaciones", CAMILA, completa(999)).andExpect(status().isNotFound());
        enviar("/api/planificaciones/9999/enviar", CAMILA).andExpect(status().isNotFound());
    }

    // ---------- Reglas según las respuestas del estudiante (S-34, S-35, S-36) ----------

    @Test
    void laPlanificacionSeEntregaConAlMenos3DiasDeAnticipacion() throws Exception {
        Map<String, Object> muyPronto = completa(3);
        muyPronto.put("fechaClase", LocalDate.now().plusDays(2).toString());
        long id = cuerpo(enviar("/api/planificaciones", CAMILA, muyPronto).andExpect(status().isCreated())).get("id").asLong();
        adjuntarDocumentoFormal(id, CAMILA);   // S-47: con el documento ya adjunto, el único problema es la fecha
        JsonNode err = cuerpo(enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isBadRequest()));
        assertThat(err.get("mensaje").asText()).contains("3 días de anticipación").contains(LocalDate.now().plusDays(3).toString());

        Map<String, Object> aTiempo = sinInscripcion(completa(3));
        aTiempo.put("fechaClase", LocalDate.now().plusDays(3).toString());
        reemplazar("/api/planificaciones/" + id, CAMILA, aTiempo).andExpect(status().isOk());
        enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION_GUIA"));
        sincronizar();
    }

    @Test
    void enAmbientesDeAprendizajeNoHayGuiaYLaPlanificacionVaDirectoAlProfesorDelCurso() throws Exception {
        // Javiera (estudiante 5) se inscribe en Ambientes de Aprendizaje (asignatura 2): no va a un colegio, no hay colaborador
        long inscripcion = cuerpo(enviar("/api/inscripciones", COORDINADOR, Map.of("estudianteId", 5, "asignaturaId", 2,
                "periodo", "2026-2", "profesorCursoId", 5)).andExpect(status().isCreated())).get("id").asLong();
        long id = cuerpo(enviar("/api/planificaciones", JAVIERA, completa(inscripcion)).andExpect(status().isCreated())).get("id").asLong();
        String base = "/api/planificaciones/" + id;
        adjuntarDocumentoFormal(id, JAVIERA);

        enviar(base + "/enviar", JAVIERA).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_AUTORIZACION")).andExpect(jsonPath("$.pendienteDe").value("PROFESOR_CURSO"));
        sincronizar();
        enviar(base + "/revision-guia", GUIA1, decision("APROBADA", null)).andExpect(status().isForbidden());   // no hay guía en esta inscripción
        enviar(base + "/revision-curso", CURSO1, decision("APROBADA", null)).andExpect(status().isForbidden()); // Ricardo no es su profesor
        enviar(base + "/revision-curso", CURSO2, decision("OBSERVADA", "Falta la evaluación del cierre.")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CON_OBSERVACIONES"));
        enviar(base + "/enviar", JAVIERA).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_AUTORIZACION"));
        enviar(base + "/revision-curso", CURSO2, decision("APROBADA", null)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AUTORIZADA"));
        sincronizar();
    }

    @Test
    void aproximacionAlSistemaEscolarNoEntregaPlanificaciones() throws Exception {
        // inscripción 18 = Javiera en Aproximación al Sistema Escolar (grupos de 4, solo trabajos en clase)
        enviar("/api/planificaciones", JAVIERA, completa(18)).andExpect(status().isConflict());
    }

    // ---------- Permisos y visibilidad ----------

    @Test
    void soloElEstudiantePuedeCrearPlanificaciones() throws Exception {
        for (String rol : List.of(GUIA1, CURSO1, COORDINADOR)) {
            enviar("/api/planificaciones", rol, completa(3)).andExpect(status().isForbidden());
        }
        enviar("/api/planificaciones", null, completa(3)).andExpect(status().isUnauthorized());
        enviar("/api/planificaciones/4/enviar", GUIA1).andExpect(status().isForbidden());
        enviar("/api/planificaciones/6/revision-guia", CAMILA, decision("APROBADA", null)).andExpect(status().isForbidden());
    }

    @Test
    void otroEstudianteNoPuedeVerNiTocarLaPlanificacionAjena() throws Exception {
        consultar("/api/planificaciones/3", DIEGO).andExpect(status().isForbidden());
        reemplazar("/api/planificaciones/4", DIEGO, sinInscripcion(completa(3))).andExpect(status().isForbidden());
        enviar("/api/planificaciones/4/enviar", DIEGO).andExpect(status().isForbidden());
        consultar("/api/planificaciones/3", CAMILA).andExpect(status().isOk());
    }

    @Test
    void cadaRolVeLasPlanificacionesDeSusInscripcionesEnCualquierEstado() throws Exception {
        // guía Marcos: inscripciones 3, 9, 10, 14 y 48 (Fase F) -> planificaciones 1, 2, 3, 4, 5 y 18
        assertThat(ids(cuerpo(consultar("/api/planificaciones", GUIA1)))).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 18L);
        // guía Lorena también quedó con la inscripción 53 (Fase F) -> se suma la planificación 19
        assertThat(ids(cuerpo(consultar("/api/planificaciones", GUIA2)))).containsExactlyInAnyOrder(6L, 7L, 8L, 9L, 19L);
        assertThat(ids(cuerpo(consultar("/api/planificaciones", CAMILA)))).containsExactlyInAnyOrder(3L, 4L, 5L);
        assertThat(cuerpo(consultar("/api/planificaciones", COORDINADOR))).hasSize(19);
        // bandejas por estado
        assertThat(ids(cuerpo(consultar("/api/planificaciones?estado=EN_REVISION_GUIA", GUIA2)))).containsExactlyInAnyOrder(6L, 9L);
        // Ricardo también quedó con la planificación 11 (Tomás, Fase F) en EN_AUTORIZACION
        assertThat(ids(cuerpo(consultar("/api/planificaciones?estado=EN_AUTORIZACION", CURSO1)))).containsExactlyInAnyOrder(7L, 11L);
        assertThat(ids(cuerpo(consultar("/api/planificaciones?inscripcionId=3&estado=BORRADOR", GUIA1)))).containsExactlyInAnyOrder(5L);
        consultar("/api/planificaciones?estado=INVENTADO", GUIA1).andExpect(status().isBadRequest());
        // el guía ve el detalle de cualquier estado de sus estudiantes; no el de otro guía
        consultar("/api/planificaciones/5", GUIA1).andExpect(status().isOk());
        consultar("/api/planificaciones/6", GUIA1).andExpect(status().isForbidden());
    }

    @Test
    void elHistorialDeUnaPlanificacionDelSeedTraeSusRevisionesEnOrden() throws Exception {
        // planificación 2 (Valentina, 2025): guía observa, guía aprueba, curso autoriza
        JsonNode p = cuerpo(consultar("/api/planificaciones/2", VALENTINA).andExpect(status().isOk()));
        List<String> resumen = new java.util.ArrayList<>();
        p.get("revisiones").forEach(r -> resumen.add(r.get("etapa").asText() + ":" + r.get("decision").asText()));
        assertThat(resumen).containsExactly("GUIA:OBSERVADA", "GUIA:APROBADA", "CURSO:APROBADA");
        assertThat(p.get("estado").asText()).isEqualTo("AUTORIZADA");
    }
}

package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.exception.ServicioExternoException;
import cl.ubb.pem.practicas.service.ModeloLenguaje.Solicitud;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** HU-15: chatbot de retroalimentación con el historial del estudiante como contexto (modelo simulado). */
class HU15ChatbotIntegracionTest extends ChatbotIntegracionBase {

    @Test
    void casoFelizElEstudianteEscribeYElChatbotRespondeConSuHistorialComoContexto() throws Exception {
        JsonNode r = cuerpo(escribir(CHAT_VALENTINA, VALENTINA, "  Hola, ¿me ayudas con mi clase?  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensajeEstudiante.autor").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.mensajeEstudiante.contenido").value("Hola, ¿me ayudas con mi clase?"))
                .andExpect(jsonPath("$.respuesta.autor").value("CHATBOT"))
                .andExpect(jsonPath("$.respuesta.contenido").value(RESPUESTA)));
        assertThat(r.get("respuesta").get("id").asLong()).isGreaterThan(r.get("mensajeEstudiante").get("id").asLong());

        // criterio 1: el contexto sale del historial (HU-05): semestre actual y prácticas aprobadas
        Solicitud s = solicitudes(1).get(0);
        assertThat(s.instrucciones())
                .contains("Valentina")
                .contains("Semestre actual de la carrera:")
                .contains("Práctica en curso: Práctica Pedagógica, período 2026-2")
                .contains("Trabajo Colaborativo en el Aula").contains("Investigación en el Aula")
                // criterios 3, 5, 6 y 7 viajan como reglas del chatbot
                .contains("Nunca diseñas una planificación").contains("como máximo 5 preguntas")
                .contains("nunca asignes puntaje").contains("como máximo dos retroalimentaciones");
        assertThat(s.historia()).isEmpty();
        assertThat(s.mensaje()).contains("no ha adjuntado ninguna planificación").endsWith("Hola, ¿me ayudas con mi clase?");

        // criterio 9: la conversación queda guardada por inscripción, en orden
        consultar(CHAT_VALENTINA, VALENTINA).andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.puedeEscribir").value(true))
                .andExpect(jsonPath("$.mensajes.length()").value(2))
                .andExpect(jsonPath("$.mensajes[0].autor").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.mensajes[1].autor").value("CHATBOT"));
    }

    @Test
    void elSegundoMensajeLlevaLaConversacionAnteriorYSigueConLaMismaPlanificacion() throws Exception {
        escribir(CHAT_VALENTINA, VALENTINA, "Revisa mi planificación, por favor.", 8).andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensajeEstudiante.planificacionId").value(8))
                .andExpect(jsonPath("$.mensajeEstudiante.planificacionTitulo").value("Introducción a las funciones cuadráticas"));
        escribir(CHAT_VALENTINA, VALENTINA, "Quiero que los estudiantes grafiquen la parábola.")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensajeEstudiante.planificacionId").doesNotExist());

        Solicitud segunda = solicitudes(2).get(1);
        assertThat(segunda.historia()).hasSize(2);
        assertThat(segunda.historia().get(0).delEstudiante()).isTrue();
        assertThat(segunda.historia().get(1).texto()).isEqualTo(RESPUESTA);
        // sin elegir otra, el chatbot sigue leyendo la última planificación adjunta
        assertThat(segunda.mensaje()).contains("PLANIFICACIÓN EN REVISIÓN").contains("Introducción a las funciones cuadráticas");
    }

    @Test
    void sinHistorialUnEstudianteNuevoTambienPuedeConversar() throws Exception {
        // un estudiante recién registrado, inscrito directo en Práctica Pedagógica: sin prácticas aprobadas,
        // sin evaluaciones, sin planificaciones y sin mensajes
        Map<String, Object> e = new HashMap<>(Map.of("nombre", "Ana", "apellidos", "Prueba Chat",
                "email", "ana.chat@demo.example", "password", "clave-de-prueba-123", "rut", "30000001-0",
                "semestreActual", 9, "anioIngreso", 2022));
        long estudianteId = cuerpo(enviar("/api/estudiantes", COORDINADOR, e).andExpect(status().isCreated())).get("id").asLong();
        long inscripcionId = cuerpo(enviar("/api/inscripciones", COORDINADOR, Map.of("estudianteId", estudianteId,
                "asignaturaId", 5, "periodo", "2026-2", "profesorGuiaId", 2, "profesorCursoId", 4))
                .andExpect(status().isCreated())).get("id").asLong();
        String chat = "/api/inscripciones/" + inscripcionId + "/chatbot";

        consultar(chat, "ana.chat@demo.example").andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajes.length()").value(0))
                .andExpect(jsonPath("$.puedeEscribir").value(true));
        escribir(chat, "ana.chat@demo.example", "¿Cómo empiezo?").andExpect(status().isCreated());

        Solicitud s = solicitudes(1).get(0);
        assertThat(s.instrucciones()).contains("Semestre actual de la carrera: 9")
                .contains("Sin prácticas aprobadas registradas en el sistema")
                .contains("Aún no tiene evaluaciones finalizadas")
                .contains("Planificaciones de la práctica en curso: ninguna todavía");
    }

    @Test
    void sinAccesoOtroEstudianteElGuiaYLosDemasRolesNoEscriben() throws Exception {
        escribir(CHAT_VALENTINA, DIEGO, "Hola").andExpect(status().isForbidden());        // otro estudiante
        escribir(CHAT_VALENTINA, GUIA2, "Hola").andExpect(status().isForbidden());        // su guía solo lee (HU-16)
        escribir(CHAT_VALENTINA, COORDINADOR, "Hola").andExpect(status().isForbidden());
        escribir(CHAT_VALENTINA, JEFE_CARRERA, "Hola").andExpect(status().isForbidden());
        mvc.perform(MockMvcRequestBuilders.post(CHAT_VALENTINA + "/mensajes")
                        .contentType("application/json").content("{\"texto\":\"Hola\"}"))
                .andExpect(status().isUnauthorized());
        escribir("/api/inscripciones/99999/chatbot", VALENTINA, "Hola").andExpect(status().isNotFound());
        verifyNoInteractions(modelo);
    }

    @Test
    void soloEnLasPracticasFinalesYConLaInscripcionEnCurso() throws Exception {
        // Camila está en Trabajo Colaborativo (sin chatbot)
        escribir("/api/inscripciones/3/chatbot", CAMILA, "Hola").andExpect(status().isConflict());
        consultar("/api/inscripciones/3/chatbot", CAMILA).andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.puedeEscribir").value(false))
                .andExpect(jsonPath("$.motivoNoDisponible").value(org.hamcrest.Matchers.containsString("Práctica Pedagógica")));
        // Sebastián ya aprobó Práctica Pedagógica (inscripción 16): el chat queda solo para consulta
        escribir("/api/inscripciones/16/chatbot", SEBASTIAN, "Hola").andExpect(status().isConflict());
        consultar("/api/inscripciones/16/chatbot", SEBASTIAN).andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoNoDisponible").value(org.hamcrest.Matchers.containsString("cerrada")));
        verifyNoInteractions(modelo);
    }

    @Test
    void siLaApiExternaFallaResponde503NoGuardaNadaYLaAplicacionSigueFuncionando() throws Exception {
        when(modelo.responder(any())).thenThrow(new ServicioExternoException(
                "El asistente de planificación no pudo responder en este momento. Tu mensaje no se guardó: inténtalo de nuevo en unos minutos."));

        escribir(CHAT_VALENTINA, VALENTINA, "Hola").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("no se guardó")));

        // nada quedó a medias y el resto del sistema responde normal
        consultar(CHAT_VALENTINA, VALENTINA).andExpect(status().isOk()).andExpect(jsonPath("$.mensajes.length()").value(0));
        consultar("/api/auth/me", VALENTINA).andExpect(status().isOk());
        consultar("/api/estudiantes/me/historial", VALENTINA).andExpect(status().isOk());

        // cuando el proveedor vuelve, el mismo mensaje funciona (doReturn: when(...) ejecutaría el stub que lanza)
        doReturn(RESPUESTA).when(modelo).responder(any());
        escribir(CHAT_VALENTINA, VALENTINA, "Hola").andExpect(status().isCreated());
    }

    @Test
    void laPlanificacionAdjuntaDebeSerDeLaPracticaYElChatbotLeeSusDocumentos() throws Exception {
        // la planificación 9 es de Sebastián: no se puede adjuntar en el chat de Valentina
        escribir(CHAT_VALENTINA, VALENTINA, "Revisa esta", 9).andExpect(status().isBadRequest());
        escribir(CHAT_VALENTINA, VALENTINA, "Revisa esta", 999999).andExpect(status().isNotFound());

        // Valentina sube su planificación formal en Word y una guía en PDF (HU-21)
        mvc.perform(multipart("/api/planificaciones/8/documentos", VALENTINA)
                        .file(archivo("archivo", "planificacion.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                docx("Inicio: activar conocimientos previos sobre funciones lineales"))))
                .andExpect(status().isCreated());
        mvc.perform(multipart("/api/planificaciones/8/documentos", VALENTINA)
                        .file(archivo("archivo", "guia.pdf", "application/pdf",
                                pdf("Guia de trabajo: graficar y = x^2 con una tabla de valores"))))
                .andExpect(status().isCreated());

        escribir(CHAT_VALENTINA, VALENTINA, "¿Mi inicio está bien?", 8).andExpect(status().isCreated());

        // el proveedor solo recibe texto: el Word y el PDF llegan convertidos dentro del mensaje
        Solicitud s = solicitudes(1).get(0);
        assertThat(s.mensaje()).contains("Título: Introducción a las funciones cuadráticas")
                .contains("Texto del documento Word \"planificacion.docx\"")
                .contains("Inicio: activar conocimientos previos sobre funciones lineales")
                .contains("Texto del documento PDF \"guia.pdf\"")
                .contains("Guia de trabajo: graficar y = x^2 con una tabla de valores");
    }

    /** Un PDF real de una página (con OpenPDF, la misma librería de la cartola). */
    private static byte[] pdf(String texto) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        com.lowagie.text.Document documento = new com.lowagie.text.Document();
        com.lowagie.text.pdf.PdfWriter.getInstance(documento, salida);
        documento.open();
        documento.add(new com.lowagie.text.Paragraph(texto));
        documento.close();
        return salida.toByteArray();
    }

    @Test
    void unMensajeVacioODemasiadoLargoDa400() throws Exception {
        escribir(CHAT_VALENTINA, VALENTINA, "   ").andExpect(status().isBadRequest());
        escribir(CHAT_VALENTINA, VALENTINA, "a".repeat(4001)).andExpect(status().isBadRequest());
        verifyNoInteractions(modelo);
    }

    private static byte[] docx(String parrafo) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("<w:document xmlns:w=\"x\"><w:body><w:p><w:r><w:t>" + parrafo
                    + "</w:t></w:r></w:p></w:body></w:document>").getBytes(StandardCharsets.UTF_8));
        }
        return salida.toByteArray();
    }
}

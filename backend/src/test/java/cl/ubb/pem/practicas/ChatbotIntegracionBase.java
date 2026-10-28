package cl.ubb.pem.practicas;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.ubb.pem.practicas.service.ModeloLenguaje;
import cl.ubb.pem.practicas.service.ModeloLenguaje.Solicitud;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Base de las pruebas del chatbot (HU-15 y HU-16): la aplicación completa contra la base real, pero el modelo de
 * lenguaje es un doble de Mockito. Las pruebas NUNCA llaman a la API del proveedor ni usan una clave real.
 *
 * Seed: inscripción 11 = Valentina Pérez en Práctica Pedagógica 2026-2 (EN_CURSO), guía Lorena Vidal (GUIA2),
 * profesor del curso Patricia Contreras (CURSO2); su planificación 8. Inscripción 17 = Sebastián Vargas en Práctica
 * Profesional (planificación 9). Inscripción 3 = Camila en Trabajo Colaborativo (sin chatbot).
 */
abstract class ChatbotIntegracionBase extends IntegracionBase {

    protected static final String CHAT_VALENTINA = "/api/inscripciones/11/chatbot";
    protected static final String RESPUESTA = "Gracias por compartir tu planificación. ¿Qué quieres que logren tus estudiantes al cierre?";

    @MockitoBean
    protected ModeloLenguaje modelo;

    @BeforeEach
    void modeloQueResponde() {
        when(modelo.responder(any())).thenReturn(RESPUESTA);
    }

    protected ResultActions escribir(String chat, String email, String texto) throws Exception {
        return enviar(chat + "/mensajes", email, Map.of("texto", texto));
    }

    protected ResultActions escribir(String chat, String email, String texto, long planificacionId) throws Exception {
        return enviar(chat + "/mensajes", email, Map.of("texto", texto, "planificacionId", planificacionId));
    }

    /** Las solicitudes que recibió el modelo, en orden. */
    protected List<Solicitud> solicitudes(int veces) {
        ArgumentCaptor<Solicitud> captor = ArgumentCaptor.forClass(Solicitud.class);
        verify(modelo, org.mockito.Mockito.times(veces)).responder(captor.capture());
        return captor.getAllValues();
    }
}

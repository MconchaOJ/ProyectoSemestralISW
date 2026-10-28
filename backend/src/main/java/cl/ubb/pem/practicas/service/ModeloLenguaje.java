package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.exception.ServicioExternoException;
import java.util.List;

/**
 * El modelo de lenguaje que responde en el chat (HU-15). Es una interfaz para que el resto del sistema no dependa de
 * un proveedor: hoy la implementa {@link ModeloLenguajeGroq} (SUPUESTO S-59) y en las pruebas un doble.
 * Todo viaja como texto: los documentos de la planificación (Word y PDF) ya llegan convertidos dentro del mensaje.
 */
public interface ModeloLenguaje {

    /** Un turno anterior de la conversación, tal como quedó guardado. */
    record Turno(boolean delEstudiante, String texto) {
    }

    /**
     * Todo lo que el modelo necesita para responder.
     *
     * @param instrucciones reglas del chatbot y contexto del estudiante (historial, HU-05)
     * @param historia      la conversación anterior, en orden
     * @param mensaje       el mensaje nuevo del estudiante, ya con el texto de la planificación si la hay
     */
    record Solicitud(String instrucciones, List<Turno> historia, String mensaje) {
    }

    /**
     * Respuesta del modelo en texto.
     *
     * @throws ServicioExternoException si no está configurado o el proveedor falla (red, límite de uso, error del
     *                                  servidor, respuesta vacía)
     */
    String responder(Solicitud solicitud);
}

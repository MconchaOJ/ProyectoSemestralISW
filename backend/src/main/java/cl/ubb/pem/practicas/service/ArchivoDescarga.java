package cl.ubb.pem.practicas.service;

import org.springframework.core.io.Resource;

/** Lo que necesita el controlador para responder la descarga de un archivo (documento o audio). */
public record ArchivoDescarga(Resource recurso, String nombreArchivo, String tipoMime) {
}

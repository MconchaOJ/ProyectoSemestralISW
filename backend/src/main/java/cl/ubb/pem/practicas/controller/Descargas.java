package cl.ubb.pem.practicas.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Respuesta de un PDF o un Excel descargable (HU-08, HU-28 y HU-31): tipo MIME y nombre del archivo. */
final class Descargas {

    private Descargas() {
    }

    static ResponseEntity<byte[]> pdf(byte[] contenido, String nombre) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(contenido);
    }

    /** HU-08: un libro de Excel (.xlsx). */
    static ResponseEntity<byte[]> xlsx(byte[] contenido, String nombre) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cl.ubb.pem.practicas.service.InformesXlsx.TIPO_MIME))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(contenido);
    }
}

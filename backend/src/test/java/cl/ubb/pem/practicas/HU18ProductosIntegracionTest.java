package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * HU-18: informes y productos de la práctica (cada prueba se revierte). Seed: la inscripción 3 es de Camila (en curso,
 * guía Marcos = GUIA1, curso Ricardo = CURSO1); la 1 también es de Camila, pero APROBADA; la 17 es de Sebastián (en
 * curso, guía Lorena = GUIA2, tutor Rodrigo = tutor1).
 */
class HU18ProductosIntegracionTest extends IntegracionBase {

    private static final String TUTOR1 = "tutor1@demo.example";
    private static final String BASE = "/api/inscripciones/3/productos";

    private static MockMultipartFile pdf(String nombre, String contenido) {
        return archivo("archivo", nombre, "application/pdf", ("%PDF-1.4 " + contenido).getBytes());
    }

    private ResultActions subir(String base, String email, String tipo, MockMultipartFile archivo) throws Exception {
        var req = multipart(base, email).file(archivo);
        if (tipo != null) {
            req.param("tipo", tipo);
        }
        return mvc.perform(req);
    }

    private long subirOk(String base, String email, String tipo, String nombre) throws Exception {
        return cuerpo(subir(base, email, tipo, pdf(nombre, nombre)).andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions cambiar(String url, String email, MockMultipartFile archivo, String tipo) throws Exception {
        var req = MockMvcRequestBuilders.multipart(HttpMethod.PUT, url).file(archivo);
        if (tipo != null) {
            req.param("tipo", tipo);
        }
        if (email != null) {
            req.header("Authorization", "Bearer " + token(email));
        }
        return mvc.perform(req);
    }

    @Test
    void elEstudianteSubeSusProductosYQuienVeLaInscripcionLosDescarga() throws Exception {
        JsonNode p = cuerpo(subir(BASE, CAMILA, "INFORME_FINAL", pdf("informe.pdf", "informe")).andExpect(status().isCreated()));
        assertThat(p.get("tipo").asText()).isEqualTo("INFORME_FINAL");
        assertThat(p.get("tipoNombre").asText()).isEqualTo("Informe final");
        assertThat(p.get("subidoPor").get("email").asText()).isEqualTo(CAMILA);
        long id = p.get("id").asLong();
        // S-94: varios por tipo y cualquier tipo
        subirOk(BASE, CAMILA, "INFORME_FINAL", "informe-anexo.pdf");
        subirOk(BASE, CAMILA, "DIARIO_REFLEXION", "diario.pdf");

        for (String email : List.of(CAMILA, GUIA1, CURSO1, COORDINADOR, JEFE_CARRERA)) {
            consultar(BASE, email).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
            consultar(BASE + "/" + id, email).andExpect(status().isOk())
                    .andExpect(content().bytes("%PDF-1.4 informe".getBytes()));
        }
        // quien no ve la inscripción tampoco ve sus productos
        for (String email : List.of(DIEGO, GUIA2, CURSO2, TUTOR1)) {
            consultar(BASE, email).andExpect(status().isForbidden());
            consultar(BASE + "/" + id, email).andExpect(status().isForbidden());
        }
        consultar(BASE, null).andExpect(status().isUnauthorized());
    }

    @Test
    void elTutorVeLosProductosDeSusInscripciones() throws Exception {
        long id = subirOk("/api/inscripciones/17/productos", SEBASTIAN, "PROYECTO_INTERVENCION", "proyecto.pdf");
        consultar("/api/inscripciones/17/productos", TUTOR1).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoNombre").value("Proyecto de intervención"));
        consultar("/api/inscripciones/17/productos/" + id, TUTOR1).andExpect(status().isOk());
    }

    @Test
    void soloElEstudianteDeLaInscripcionSubeReemplazaOBorra() throws Exception {
        long id = subirOk(BASE, CAMILA, "OTRO", "autoevaluacion.pdf");
        for (String email : List.of(DIEGO, GUIA1, CURSO1, COORDINADOR, JEFE_CARRERA)) {
            subir(BASE, email, "OTRO", pdf("x.pdf", "x")).andExpect(status().isForbidden());
            cambiar(BASE + "/" + id, email, pdf("y.pdf", "y"), null).andExpect(status().isForbidden());
            borrar(BASE + "/" + id, email).andExpect(status().isForbidden());
        }
        subir(BASE, null, "OTRO", pdf("x.pdf", "x")).andExpect(status().isUnauthorized());
        consultar(BASE, CAMILA).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void elEstudianteReemplazaYBorraUnProducto() throws Exception {
        long id = subirOk(BASE, CAMILA, "OTRO", "equivocado.pdf");
        long otro = subirOk(BASE, CAMILA, "DIARIO_REFLEXION", "diario.pdf");

        // sin tipo se mantiene; con tipo cambia
        JsonNode r = cuerpo(cambiar(BASE + "/" + id, CAMILA, pdf("correcto.pdf", "nuevo"), null).andExpect(status().isOk()));
        assertThat(r.get("id").asLong()).isEqualTo(id);
        assertThat(r.get("tipo").asText()).isEqualTo("OTRO");
        assertThat(r.get("nombreArchivo").asText()).isEqualTo("correcto.pdf");
        r = cuerpo(cambiar(BASE + "/" + id, CAMILA, pdf("informe.pdf", "final"), "INFORME_FINAL").andExpect(status().isOk()));
        assertThat(r.get("tipo").asText()).isEqualTo("INFORME_FINAL");
        consultar(BASE + "/" + id, GUIA1).andExpect(content().bytes("%PDF-1.4 final".getBytes()));

        borrar(BASE + "/" + otro, CAMILA).andExpect(status().isNoContent());
        assertThat(cuerpo(consultar(BASE, CAMILA)).findValuesAsText("nombreArchivo")).containsExactly("informe.pdf");
        consultar(BASE + "/" + otro, CAMILA).andExpect(status().isNotFound());
    }

    @Test
    void conLaInscripcionCerradaNoSeSubeNiSeCambiaNada() throws Exception {
        subir("/api/inscripciones/1/productos", CAMILA, "INFORME_FINAL", pdf("tarde.pdf", "x")).andExpect(status().isConflict());
        // un producto de otra inscripción no se toca por la ruta de esta
        long id = subirOk(BASE, CAMILA, "INFORME_FINAL", "informe.pdf");
        consultar("/api/inscripciones/1/productos/" + id, CAMILA).andExpect(status().isNotFound());
        consultar("/api/inscripciones/99999/productos", CAMILA).andExpect(status().isNotFound());
    }

    @Test
    void tipoYFormatoSeValidan() throws Exception {
        subir(BASE, CAMILA, "TESIS", pdf("x.pdf", "x")).andExpect(status().isBadRequest());
        subir(BASE, CAMILA, null, pdf("x.pdf", "x")).andExpect(status().isBadRequest());
        subir(BASE, CAMILA, "OTRO", archivo("archivo", "planilla.xlsx", "application/vnd.ms-excel", "x".getBytes()))
                .andExpect(status().isBadRequest());
        subir(BASE, CAMILA, "OTRO", archivo("archivo", "vacio.pdf", "application/pdf", new byte[0]))
                .andExpect(status().isBadRequest());
        consultar(BASE, CAMILA).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void losProductosQuedanEnElRecorridoEnPantallaPdfYExcel() throws Exception {
        subirOk(BASE, CAMILA, "INFORME_FINAL", "mi-informe-final.pdf");
        sincronizar();

        JsonNode h = cuerpo(consultar("/api/estudiantes/me/historial", CAMILA).andExpect(status().isOk()));
        JsonNode insc3 = null;
        for (JsonNode i : h.get("inscripciones")) {
            if (i.get("inscripcionId").asLong() == 3) {
                insc3 = i;
            } else {
                assertThat(i.get("productos")).isEmpty();
            }
        }
        assertThat(insc3).isNotNull();
        assertThat(insc3.get("productos").findValuesAsText("nombreArchivo")).containsExactly("mi-informe-final.pdf");
        consultar("/api/estudiantes/1/historial", GUIA1).andExpect(status().isOk());

        consultar("/api/estudiantes/1/historial/pdf", COORDINADOR).andExpect(status().isOk());
        byte[] xlsx = consultar("/api/estudiantes/1/historial/xlsx", COORDINADOR).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String todo = textoDelZip(xlsx);
        assertThat(todo).contains("Productos", "mi-informe-final.pdf", "Informe final");
    }

    private static String textoDelZip(byte[] zip) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e = z.getNextEntry(); e != null; e = z.getNextEntry()) {
                sb.append(new String(z.readAllBytes(), StandardCharsets.UTF_8)).append('\n');
            }
        }
        return sb.toString();
    }
}

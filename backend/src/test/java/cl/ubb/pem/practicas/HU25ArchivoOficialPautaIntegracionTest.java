package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-25: archivos oficiales de las pautas (cada prueba se revierte). Seed: la pauta 1 (Trabajo Colaborativo) ya se
 * usó en evaluaciones, así que editarla crea la versión 2 (HU-06); ninguna pauta trae archivo oficial.
 */
class HU25ArchivoOficialPautaIntegracionTest extends IntegracionBase {

    private static final String TUTOR1 = "tutor1@demo.example";
    private static final String BASE = "/api/pautas/1/archivo-oficial";

    private static MockMultipartFile pdf(String nombre, String contenido) {
        return archivo("archivo", nombre, "application/pdf", ("%PDF-1.4 " + contenido).getBytes());
    }

    private ResultActions subir(String base, String email, MockMultipartFile archivo) throws Exception {
        return mvc.perform(multipart(base, email).file(archivo));
    }

    private JsonNode subirOk(String base, String nombre) throws Exception {
        return cuerpo(subir(base, COORDINADOR, pdf(nombre, nombre)).andExpect(status().isCreated()));
    }

    @Test
    void elCoordinadorSubeYReemplazaYQuedaElHistorial() throws Exception {
        JsonNode v1 = subirOk(BASE, "pauta-firmada.pdf");
        assertThat(v1.get("version").asInt()).isEqualTo(1);
        assertThat(v1.get("subidoPor").get("email").asText()).isEqualTo(COORDINADOR);
        JsonNode v2 = subirOk(BASE, "pauta-firmada-2026.pdf");
        assertThat(v2.get("version").asInt()).isEqualTo(2);

        // criterio 1: el vigente es el último
        consultar(BASE, CAMILA).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.nombreArchivo").value("pauta-firmada-2026.pdf"));
        consultar(BASE + "/descargar", GUIA1).andExpect(status().isOk())
                .andExpect(content().bytes("%PDF-1.4 pauta-firmada-2026.pdf".getBytes()));

        // criterio 2: historial con quién y cuándo, del más reciente al primero; el anterior se sigue descargando
        for (String email : List.of(COORDINADOR, JEFE_CARRERA)) {
            JsonNode h = cuerpo(consultar(BASE + "/historial", email).andExpect(status().isOk()));
            assertThat(h.findValuesAsText("version")).containsExactly("2", "1");
            assertThat(h.get(1).get("fechaSubida").isNull()).isFalse();
            assertThat(h.get(1).get("subidoPor").get("nombreCompleto").asText()).isNotBlank();
            consultar(BASE + "/" + v1.get("id").asLong(), email).andExpect(status().isOk())
                    .andExpect(content().bytes("%PDF-1.4 pauta-firmada.pdf".getBytes()));
        }
    }

    @Test
    void soloElCoordinadorSubeYQuienVeLaPautaDescargaElVigente() throws Exception {
        JsonNode v1 = subirOk(BASE, "pauta.pdf");
        for (String email : List.of(GUIA1, CURSO1, CAMILA, JEFE_CARRERA, TUTOR1)) {
            subir(BASE, email, pdf("otra.pdf", "x")).andExpect(status().isForbidden());
            // criterio 3: quien ve la pauta (todos, S-27) descarga el archivo vigente
            consultar(BASE + "/descargar", email).andExpect(status().isOk());
        }
        // el historial y las versiones antiguas: solo coordinador y jefe de carrera
        for (String email : List.of(GUIA1, CURSO1, CAMILA, TUTOR1)) {
            consultar(BASE + "/historial", email).andExpect(status().isForbidden());
            consultar(BASE + "/" + v1.get("id").asLong(), email).andExpect(status().isForbidden());
        }
        subir(BASE, null, pdf("x.pdf", "x")).andExpect(status().isUnauthorized());
        consultar(BASE + "/descargar", null).andExpect(status().isUnauthorized());
        consultar(BASE + "/historial", COORDINADOR).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void sinArchivoOficialOConDatosInvalidosResponde404o400() throws Exception {
        consultar(BASE, CAMILA).andExpect(status().isNotFound());
        consultar(BASE + "/descargar", CAMILA).andExpect(status().isNotFound());
        consultar(BASE + "/historial", COORDINADOR).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        subir("/api/pautas/9999/archivo-oficial", COORDINADOR, pdf("x.pdf", "x")).andExpect(status().isNotFound());
        // S-95: PDF o Word
        subir(BASE, COORDINADOR, archivo("archivo", "pauta.xlsx", "application/vnd.ms-excel", "x".getBytes()))
                .andExpect(status().isBadRequest());
        subir(BASE, COORDINADOR, archivo("archivo", "pauta.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "PK x".getBytes()))
                .andExpect(status().isCreated());
        // un archivo de la pauta 1 no se descarga por la ruta de otra pauta
        long id = cuerpo(consultar(BASE, COORDINADOR)).get("id").asLong();
        consultar("/api/pautas/2/archivo-oficial/" + id, COORDINADOR).andExpect(status().isNotFound());
    }

    @Test
    void laPautaMuestraSuArchivoOficialEnElDetalleYEnLaGestion() throws Exception {
        consultar("/api/pautas/1", GUIA1).andExpect(jsonPath("$.archivoOficial").doesNotExist());
        subirOk(BASE, "pauta.pdf");
        sincronizar();
        consultar("/api/pautas/1", GUIA1).andExpect(jsonPath("$.archivoOficial.nombreArchivo").value("pauta.pdf"));
        JsonNode gestion = cuerpo(consultar("/api/pautas/gestion", COORDINADOR));
        for (JsonNode p : gestion) {
            if (p.get("id").asInt() == 1) {
                assertThat(p.get("archivoOficial").get("version").asInt()).isEqualTo(1);
            } else {
                assertThat(p.get("archivoOficial").isNull()).isTrue();
            }
        }
    }

    @Test
    void unaVersionNuevaDeLaPautaNoHeredaElArchivo() throws Exception {   // S-96
        subirOk(BASE, "pauta-v1.pdf");
        JsonNode nueva = cuerpo(reemplazar("/api/pautas/1", COORDINADOR, pautaUnoConUnIndicadorMas())
                .andExpect(status().isOk()));
        assertThat(nueva.get("version").asInt()).isEqualTo(2);
        assertThat(nueva.get("archivoOficial").isNull()).isTrue();
        consultar("/api/pautas/" + nueva.get("id").asInt() + "/archivo-oficial", COORDINADOR).andExpect(status().isNotFound());
        consultar(BASE, COORDINADOR).andExpect(status().isOk()).andExpect(jsonPath("$.nombreArchivo").value("pauta-v1.pdf"));
    }

    @Test
    void borrarUnaPautaSinUsoBorraSusArchivosOficiales() throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("asignaturaId", 6);
        m.put("nombre", "Pauta de prueba HU-25");
        m.put("tipoEvaluador", "TUTOR");
        m.put("ambito", "ESPECIALIDAD");
        m.put("numeroObservacion", 1);
        m.put("escalaMin", 2);
        m.put("escalaMax", 7);
        m.put("dimensiones", List.of(Map.of("nombre", "A. Planificación", "indicadores", List.of("Formula objetivos."))));
        int id = cuerpo(enviar("/api/pautas", COORDINADOR, m).andExpect(status().isCreated())).get("id").asInt();
        String base = "/api/pautas/" + id + "/archivo-oficial";
        subirOk(base, "a.pdf");
        subirOk(base, "b.pdf");

        borrar("/api/pautas/" + id, COORDINADOR).andExpect(status().isNoContent());
        sincronizar();
        Number filas = (Number) em.createNativeQuery("SELECT COUNT(*) FROM archivo_oficial_pauta WHERE pauta_id = " + id)
                .getSingleResult();
        assertThat(filas.intValue()).isZero();
        consultar("/api/pautas/" + id, COORDINADOR).andExpect(status().isNotFound());
    }

    /** La pauta 1 tal como está, con un indicador más (un cambio real: como ya se usó, crea la versión 2). */
    private Map<String, Object> pautaUnoConUnIndicadorMas() throws Exception {
        JsonNode p = cuerpo(consultar("/api/pautas/1", COORDINADOR));
        List<Map<String, Object>> dims = new ArrayList<>();
        for (JsonNode d : p.get("dimensiones")) {
            List<String> textos = new ArrayList<>();
            d.get("indicadores").forEach(i -> textos.add(i.get("descripcion").asText()));
            dims.add(new HashMap<>(Map.of("nombre", d.get("nombre").asText(), "indicadores", textos)));
        }
        @SuppressWarnings("unchecked")
        List<String> ultima = (List<String>) dims.get(dims.size() - 1).get("indicadores");
        ultima.add("El profesor/a usa la retroalimentación formativa durante la clase.");
        Map<String, Object> m = new HashMap<>();
        m.put("asignaturaId", 3);
        m.put("nombre", p.get("nombre").asText());
        m.put("tipoEvaluador", "COLABORADOR");
        m.put("ambito", "OBSERVACION_CLASE");
        m.put("escalaMin", 1);
        m.put("escalaMax", 7);
        m.put("dimensiones", dims);
        return m;
    }
}

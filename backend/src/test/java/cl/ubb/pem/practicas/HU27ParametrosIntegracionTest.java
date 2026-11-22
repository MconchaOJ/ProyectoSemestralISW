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

/**
 * HU-27: parámetros del sistema, contra la base real (cada prueba se revierte). Valores del seed: anticipación 3
 * días (0 a 14), panel 3 días (1 a 30), documento 10 MB (1 a 10), audio 25 MB (1 a 25).
 */
class HU27ParametrosIntegracionTest extends IntegracionBase {

    private static final String TUTOR1 = "tutor1@demo.example";

    private static Map<String, Object> valor(Object v) {
        Map<String, Object> m = new HashMap<>();
        m.put("valor", v);
        return m;
    }

    /** Planificación completa de Camila (inscripción 3) para la fecha dada, con su documento formal adjunto. */
    private long planificacionLista(LocalDate fecha) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("inscripcionId", 3);
        m.put("titulo", "Clase para mañana");
        m.put("establecimiento", "Liceo Bicentenario Ejemplo");
        m.put("curso", "1° Medio B");
        m.put("unidad", "Álgebra y funciones");
        m.put("objetivoAprendizaje", "OA.");
        m.put("objetivoActitudinal", "Perseverancia.");
        m.put("objetivoClase", "Objetivo.");
        m.put("fechaClase", fecha.toString());
        m.put("duracionMinutos", 90);
        m.put("recursos", "Pizarra.");
        m.put("instrumentosEvaluacion", "Ticket.");
        m.put("inicio", "Inicio.");
        m.put("inicioMinutos", 10);
        m.put("desarrollo", "Desarrollo.");
        m.put("desarrolloMinutos", 60);
        m.put("cierre", "Cierre.");
        m.put("cierreMinutos", 20);
        long id = cuerpo(enviar("/api/planificaciones", CAMILA, m).andExpect(status().isCreated())).get("id").asLong();
        mvc.perform(multipart("/api/planificaciones/" + id + "/documentos", CAMILA)
                .file(archivo("archivo", "plan.pdf", "application/pdf", "%PDF-1.4 prueba".getBytes())))
                .andExpect(status().isCreated());
        return id;
    }

    @Test
    void cualquieraLeeLosParametrosConSuRango() throws Exception {
        JsonNode lista = cuerpo(consultar("/api/parametros", CAMILA).andExpect(status().isOk()));
        assertThat(lista.findValuesAsText("clave")).containsExactly(
                "archivos.audio_mb", "archivos.documento_mb", "panel.dias_atencion", "planificacion.dias_anticipacion");
        JsonNode anticipacion = lista.get(3);
        assertThat(anticipacion.get("valor").asInt()).isEqualTo(3);
        assertThat(anticipacion.get("minimo").asInt()).isZero();
        assertThat(anticipacion.get("maximo").asInt()).isEqualTo(14);
        assertThat(anticipacion.get("editadoPor").isNull()).isTrue();   // valor inicial
        consultar("/api/parametros", null).andExpect(status().isUnauthorized());
    }

    @Test
    void elCambioDeLaAnticipacionRigeDesdeLaPeticionSiguiente() throws Exception {   // criterios 1 y 2
        long id = planificacionLista(LocalDate.now().plusDays(1));
        JsonNode err = cuerpo(enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isBadRequest()));
        assertThat(err.get("mensaje").asText()).contains("3 días");

        JsonNode p = cuerpo(reemplazar("/api/parametros/planificacion.dias_anticipacion", COORDINADOR, valor(0))
                .andExpect(status().isOk()));
        assertThat(p.get("valor").asInt()).isZero();
        assertThat(p.get("editadoPor").get("email").asText()).isEqualTo(COORDINADOR);
        assertThat(p.get("fechaEdicion").isNull()).isFalse();

        enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION_GUIA"));
    }

    @Test
    void elTamanoMaximoDeLosDocumentosSePuedeBajar() throws Exception {
        long id = planificacionLista(LocalDate.now().plusDays(10));
        byte[] dosMegas = new byte[2 * 1024 * 1024];
        reemplazar("/api/parametros/archivos.documento_mb", COORDINADOR, valor(1)).andExpect(status().isOk());
        mvc.perform(multipart("/api/planificaciones/" + id + "/documentos", CAMILA)
                .file(archivo("archivo", "grande.pdf", "application/pdf", dosMegas)))
                .andExpect(status().isBadRequest());
        reemplazar("/api/parametros/archivos.documento_mb", COORDINADOR, valor(10)).andExpect(status().isOk());
        mvc.perform(multipart("/api/planificaciones/" + id + "/documentos", CAMILA)
                .file(archivo("archivo", "grande.pdf", "application/pdf", dosMegas)))
                .andExpect(status().isCreated());
    }

    @Test
    void elUmbralDelPanelSeLeeDelParametro() throws Exception {
        reemplazar("/api/parametros/panel.dias_atencion", COORDINADOR, valor(30)).andExpect(status().isOk());
        consultar("/api/reportes/panel", COORDINADOR).andExpect(jsonPath("$.diasAtencion").value(30));
    }

    @Test
    void elRangoSeRespetaYSoloElCoordinadorLosCambia() throws Exception {   // criterio 3 y S-80
        reemplazar("/api/parametros/planificacion.dias_anticipacion", COORDINADOR, valor(15)).andExpect(status().isBadRequest());
        reemplazar("/api/parametros/archivos.documento_mb", COORDINADOR, valor(11)).andExpect(status().isBadRequest());
        reemplazar("/api/parametros/archivos.documento_mb", COORDINADOR, valor(0)).andExpect(status().isBadRequest());
        reemplazar("/api/parametros/archivos.documento_mb", COORDINADOR, valor(null)).andExpect(status().isBadRequest());
        reemplazar("/api/parametros/no.existe", COORDINADOR, valor(1)).andExpect(status().isNotFound());
        for (String email : List.of(JEFE_CARRERA, GUIA1, CURSO1, TUTOR1, CAMILA)) {
            reemplazar("/api/parametros/panel.dias_atencion", email, valor(5)).andExpect(status().isForbidden());
        }
        consultar("/api/parametros", COORDINADOR).andExpect(jsonPath("$[2].valor").value(3));   // no cambió
    }
}

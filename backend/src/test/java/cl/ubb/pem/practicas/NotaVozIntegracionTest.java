package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Fase E, punto 4 (HU-11 y HU-12): nota de voz del profesor evaluador con transcripción a texto, contra la base real.
 * Seed: evaluación 1 = Sebastián (inscripción 14), evaluador Marcos (GUIA1), FINALIZADA.
 */
class NotaVozIntegracionTest extends IntegracionBase {

    private static final byte[] AUDIO = "audio de prueba".getBytes();

    private MockMultipartFile audio() {
        return archivo("audio", "grabacion.webm", "audio/webm", AUDIO);
    }

    @Test
    void grabarYConsultarLaNotaDeVozConSuTranscripcion() throws Exception {
        JsonNode creada = cuerpo(mvc.perform(multipart("/api/evaluaciones/1/nota-voz", GUIA1)
                        .file(audio())
                        .param("transcripcion", "Buen manejo del contenido, faltó cerrar verificando el objetivo.")
                        .param("duracionSegundos", "42"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracionSegundos").value(42))
                .andExpect(jsonPath("$.transcripcionCorregida").value(false)));
        assertThat(creada.get("transcripcion").asText()).contains("Buen manejo del contenido");
        assertThat(creada.get("audioUrl").asText()).isEqualTo("/api/evaluaciones/1/nota-voz/audio");

        JsonNode consultada = cuerpo(consultar("/api/evaluaciones/1/nota-voz", COORDINADOR).andExpect(status().isOk()));
        assertThat(consultada.get("id").asLong()).isEqualTo(creada.get("id").asLong());

        // el estudiante evaluado, su profesor del curso y el coordinador pueden escucharla y leerla
        for (String vidente : new String[] {GUIA1, SEBASTIAN, CURSO1, COORDINADOR}) {
            consultar("/api/evaluaciones/1/nota-voz", vidente).andExpect(status().isOk());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/api/evaluaciones/1/nota-voz/audio")
                            .header("Authorization", "Bearer " + token(vidente)))
                    .andExpect(status().isOk());
        }
        consultar("/api/evaluaciones/1/nota-voz", DIEGO).andExpect(status().isForbidden());
    }

    @Test
    void unaSegundaGrabacionParaLaMismaEvaluacionDa409YHayQueUsarPut() throws Exception {
        mvc.perform(multipart("/api/evaluaciones/1/nota-voz", GUIA1).file(audio())
                        .param("transcripcion", "Primer intento.").param("duracionSegundos", "20"))
                .andExpect(status().isCreated());
        mvc.perform(multipart("/api/evaluaciones/1/nota-voz", GUIA1).file(audio())
                        .param("transcripcion", "Segundo intento.").param("duracionSegundos", "25"))
                .andExpect(status().isConflict());
    }

    /** PUT multipart/form-data, ya autenticado (volver a grabar, HU-11). */
    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder multipartPut(String url, String email) {
        var req = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(org.springframework.http.HttpMethod.PUT, url);
        req.header("Authorization", "Bearer " + token(email));
        return req;
    }

    @Test
    void elEvaluadorVuelveAGrabarYElAudioSeReemplaza() throws Exception {   // HU-11, criterio 2
        mvc.perform(multipart("/api/evaluaciones/1/nota-voz", GUIA1).file(audio())
                        .param("transcripcion", "Primer intento.").param("duracionSegundos", "20"))
                .andExpect(status().isCreated());
        mvc.perform(multipartPut("/api/evaluaciones/1/nota-voz/audio", GUIA1)
                        .file(archivo("audio", "otra.webm", "audio/webm", "otro audio".getBytes()))
                        .param("transcripcion", "Segundo intento, mas claro.").param("duracionSegundos", "31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracionSegundos").value(31))
                .andExpect(jsonPath("$.transcripcion").value("Segundo intento, mas claro."))
                .andExpect(jsonPath("$.transcripcionCorregida").value(false));
        byte[] escuchado = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/evaluaciones/1/nota-voz/audio").header("Authorization", "Bearer " + token(SEBASTIAN)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(escuchado)).isEqualTo("otro audio");

        // solo el evaluador vuelve a grabar; sin nota de voz previa, 404 (se usa POST)
        mvc.perform(multipartPut("/api/evaluaciones/1/nota-voz/audio", GUIA2).file(audio())
                        .param("transcripcion", "x").param("duracionSegundos", "5"))
                .andExpect(status().isForbidden());
        mvc.perform(multipartPut("/api/evaluaciones/2/nota-voz/audio", GUIA1).file(audio())
                        .param("transcripcion", "x").param("duracionSegundos", "5"))
                .andExpect(status().isNotFound());
        mvc.perform(multipartPut("/api/evaluaciones/1/nota-voz/audio", GUIA1).file(audio())
                        .param("transcripcion", "x").param("duracionSegundos", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elEvaluadorCorrigeLaTranscripcion() throws Exception {
        mvc.perform(multipart("/api/evaluaciones/1/nota-voz", GUIA1).file(audio())
                        .param("transcripcion", "texto automatico con errores").param("duracionSegundos", "30"))
                .andExpect(status().isCreated());

        JsonNode corregida = cuerpo(reemplazar("/api/evaluaciones/1/nota-voz", GUIA1,
                Map.of("transcripcion", "Texto corregido por el evaluador."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcripcionCorregida").value(true)));
        assertThat(corregida.get("transcripcion").asText()).isEqualTo("Texto corregido por el evaluador.");

        reemplazar("/api/evaluaciones/1/nota-voz", GUIA1, Map.of("transcripcion", " ")).andExpect(status().isBadRequest());
    }

    @Test
    void seGrabaTantoConLaEvaluacionEnBorradorComoFinalizada() throws Exception {
        // evaluación 1 del seed ya está FINALIZADA: grabar de todas formas no rompe su inmutabilidad
        mvc.perform(multipart("/api/evaluaciones/1/nota-voz", GUIA1).file(audio())
                        .param("transcripcion", "Retroalimentación complementaria.").param("duracionSegundos", "15"))
                .andExpect(status().isCreated());
        consultar("/api/evaluaciones/1", GUIA1).andExpect(jsonPath("$.estado").value("FINALIZADA"));
    }

    @Test
    void otroGuiaQueNoEsElEvaluadorRecibe403AlGrabarOCorregir() throws Exception {
        mvc.perform(multipart("/api/evaluaciones/1/nota-voz", GUIA2).file(audio())
                        .param("transcripcion", "No debería poder.").param("duracionSegundos", "10"))
                .andExpect(status().isForbidden());

        mvc.perform(multipart("/api/evaluaciones/1/nota-voz", GUIA1).file(audio())
                        .param("transcripcion", "Grabación original.").param("duracionSegundos", "10"))
                .andExpect(status().isCreated());
        reemplazar("/api/evaluaciones/1/nota-voz", GUIA2, Map.of("transcripcion", "Intento ajeno."))
                .andExpect(status().isForbidden());
        consultar("/api/evaluaciones/1/nota-voz", GUIA2).andExpect(status().isForbidden());
    }

    @Test
    void consultarUnaEvaluacionSinNotaDeVozDa404() throws Exception {
        consultar("/api/evaluaciones/2/nota-voz", GUIA1).andExpect(status().isNotFound());
    }
}

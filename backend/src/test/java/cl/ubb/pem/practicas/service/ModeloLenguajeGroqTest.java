package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.ubb.pem.practicas.config.ChatbotProperties;
import cl.ubb.pem.practicas.exception.ServicioExternoException;
import cl.ubb.pem.practicas.service.ModeloLenguaje.Solicitud;
import cl.ubb.pem.practicas.service.ModeloLenguaje.Turno;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * HU-15: el cliente de Groq contra un servidor HTTP local que imita a Groq (formato "chat completions" de OpenAI).
 * Prueba el código HTTP de verdad (cabeceras, cuerpo, errores, reintentos) sin llamar a la API real ni usar su clave.
 */
class ModeloLenguajeGroqTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Solicitud SOLICITUD = new Solicitud("instrucciones", List.of(), "Hola");
    private static final String OK = """
            {"choices":[{"message":{"role":"assistant","content":"  ¿Qué quieres revisar de tu planificación?  "},
              "finish_reason":"stop"}],"usage":{"prompt_tokens":120,"completion_tokens":12}}""";

    /** Respuestas que dará el servidor falso, en orden: {estado, cuerpo, retry-after}. */
    private final Deque<String[]> guion = new ArrayDeque<>();
    private final List<String> cuerposRecibidos = new ArrayList<>();
    private final List<String> autorizaciones = new ArrayList<>();
    private HttpServer servidor;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/openai/v1/chat/completions", intercambio -> {
            cuerposRecibidos.add(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            autorizaciones.add(intercambio.getRequestHeaders().getFirst("Authorization"));
            String[] r = guion.isEmpty() ? new String[] {"200", OK, null} : guion.poll();
            if (r[2] != null) {
                intercambio.getResponseHeaders().add("retry-after", r[2]);
            }
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            byte[] cuerpo = r[1].getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(Integer.parseInt(r[0]), cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidor.start();
    }

    @AfterEach
    void bajarServidor() {
        servidor.stop(0);
    }

    private ModeloLenguajeGroq modelo(String clave, int reintentos) {
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/openai/v1";
        return new ModeloLenguajeGroq(new ChatbotProperties(clave, "openai/gpt-oss-120b", 1500, 0.3, 5, reintentos, url), JSON);
    }

    @Test
    void sinClaveNoLlamaAlProveedorYAvisaQueNoEstaConfigurado() {
        assertThatThrownBy(() -> modelo("", 0).responder(SOLICITUD))
                .isInstanceOf(ServicioExternoException.class).hasMessage(ModeloLenguajeGroq.NO_CONFIGURADO);
        assertThat(cuerposRecibidos).isEmpty();
    }

    @Test
    void enviaInstruccionesYTurnosAlternadosYDevuelveElTextoDeLaRespuesta() throws IOException {
        Solicitud s = new Solicitud("reglas y contexto",
                List.of(new Turno(false, "respuesta suelta"),   // se descarta: la conversación empieza por el usuario
                        new Turno(true, "primera pregunta"), new Turno(false, "primera respuesta"),
                        new Turno(true, "segunda"), new Turno(true, "repetida")),
                "mensaje nuevo");

        assertThat(modelo("clave-de-prueba-no-real", 0).responder(s)).isEqualTo("¿Qué quieres revisar de tu planificación?");

        assertThat(autorizaciones).containsExactly("Bearer clave-de-prueba-no-real");
        JsonNode cuerpo = JSON.readTree(cuerposRecibidos.get(0));
        assertThat(cuerpo.path("model").asText()).isEqualTo("openai/gpt-oss-120b");
        assertThat(cuerpo.path("max_completion_tokens").asInt()).isEqualTo(1500);
        List<String> roles = new ArrayList<>();
        cuerpo.path("messages").forEach(m -> roles.add(m.path("role").asText()));
        assertThat(roles).containsExactly("system", "user", "assistant", "user", "assistant", "user");
        assertThat(cuerpo.path("messages").get(0).path("content").asText()).isEqualTo("reglas y contexto");
        assertThat(cuerpo.path("messages").get(3).path("content").asText()).isEqualTo("segunda\n\nrepetida");
        assertThat(cuerpo.path("messages").get(4).path("content").asText()).isEqualTo("(sin respuesta)");
        assertThat(cuerpo.path("messages").get(5).path("content").asText()).isEqualTo("mensaje nuevo");
    }

    @Test
    void elLimiteDeUsoDa503ConLaEsperaYNoSeReintenta() {
        guion.add(new String[] {"429", "{\"error\":{\"message\":\"Rate limit reached\"}}", "42"});
        assertThatThrownBy(() -> modelo("clave-de-prueba-no-real", 2).responder(SOLICITUD))
                .isInstanceOf(ServicioExternoException.class)
                .hasMessageContaining("límite de uso").hasMessageContaining("42 segundos");
        assertThat(cuerposRecibidos).hasSize(1);   // reintentar solo agotaría más la cuota
        assertThat(ModeloLenguajeGroq.espera("125")).isEqualTo("3 minutos");
        assertThat(ModeloLenguajeGroq.espera(null)).isEqualTo("unos minutos");
    }

    @Test
    void unErrorDelServidorSeReintentaYUnaClaveInvalidaNo() {
        guion.add(new String[] {"503", "{}", null});
        guion.add(new String[] {"500", "{}", null});
        assertThat(modelo("clave-de-prueba-no-real", 2).responder(SOLICITUD)).isNotBlank();
        assertThat(cuerposRecibidos).hasSize(3);

        cuerposRecibidos.clear();
        guion.add(new String[] {"401", "{\"error\":{\"message\":\"Invalid API Key\"}}", null});
        assertThatThrownBy(() -> modelo("clave-de-prueba-no-real", 2).responder(SOLICITUD))
                .isInstanceOf(ServicioExternoException.class).hasMessage(ModeloLenguajeGroq.NO_RESPONDE);
        assertThat(cuerposRecibidos).hasSize(1);
    }

    @Test
    void sinConexionConElProveedorDa503() {
        ModeloLenguajeGroq caido = new ModeloLenguajeGroq(new ChatbotProperties("clave-de-prueba-no-real",
                "openai/gpt-oss-120b", 1500, 0.3, 5, 0, "http://127.0.0.1:9/openai/v1"), JSON);
        assertThatThrownBy(() -> caido.responder(SOLICITUD))
                .isInstanceOf(ServicioExternoException.class).hasMessage(ModeloLenguajeGroq.NO_RESPONDE);
    }
}

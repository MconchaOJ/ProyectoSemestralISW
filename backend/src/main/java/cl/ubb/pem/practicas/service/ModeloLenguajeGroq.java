package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.config.ChatbotProperties;
import cl.ubb.pem.practicas.exception.ServicioExternoException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link ModeloLenguaje} con Groq (SUPUESTO S-59: el proveedor no está confirmado por el cliente). Groq expone la API
 * "chat completions" de OpenAI en https://api.groq.com/openai/v1; se llama por HTTP con el RestClient de Spring, sin
 * un SDK. La clave sale solo de la variable de entorno ISW_GROQ_API_KEY (ver {@link ChatbotProperties}); si falta, la
 * aplicación arranca igual y cada mensaje al chatbot responde 503.
 *
 * Cualquier falla (sin red, clave inválida, límite de uso de la capa gratuita, error del servidor, respuesta vacía)
 * se convierte en {@link ServicioExternoException}: nunca llega a la aplicación como un error 500.
 */
@Component
public class ModeloLenguajeGroq implements ModeloLenguaje {

    private static final Logger log = LoggerFactory.getLogger(ModeloLenguajeGroq.class);

    static final String NO_CONFIGURADO = "El asistente de planificación no está configurado en este servidor. "
            + "Avisa al coordinador de prácticas.";
    static final String NO_RESPONDE = "El asistente de planificación no pudo responder en este momento. "
            + "Tu mensaje no se guardó: inténtalo de nuevo en unos minutos.";
    static final String LIMITE_DE_USO = "El asistente de planificación alcanzó su límite de uso por ahora. "
            + "Tu mensaje no se guardó: inténtalo de nuevo en %s.";

    private final ChatbotProperties props;
    private final ObjectMapper json;
    private volatile RestClient cliente;

    public ModeloLenguajeGroq(ChatbotProperties props, ObjectMapper json) {
        this.props = props;
        this.json = json;
    }

    /** Lo que devolvió el proveedor, antes de interpretarlo. */
    private record Respuesta(int estado, HttpHeaders cabeceras, String cuerpo) {
    }

    @Override
    public String responder(Solicitud s) {
        if (!props.configurado()) {
            throw new ServicioExternoException(NO_CONFIGURADO);
        }
        Map<String, Object> cuerpo = cuerpo(s);
        for (int intento = 0; ; intento++) {
            Respuesta r;
            try {
                r = cliente().post().uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo)
                        .exchange((req, res) -> new Respuesta(res.getStatusCode().value(), res.getHeaders(),
                                new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)), true);
            } catch (RestClientException e) {   // sin red, tiempo agotado, DNS
                if (intento < props.reintentos()) {
                    esperar(intento);
                    continue;
                }
                log.warn("Groq no respondió: {}", e.toString());
                throw new ServicioExternoException(NO_RESPONDE, e);
            }
            if (r.estado() == 200) {
                return interpretar(r);
            }
            if (r.estado() == 429) {
                // límite de la capa gratuita (por minuto o por día): reintentar solo lo agota más
                log.warn("Groq: límite de uso alcanzado ({}). {}", r.cabeceras().getFirst("retry-after"), detalle(r.cuerpo()));
                throw new ServicioExternoException(LIMITE_DE_USO.formatted(espera(r.cabeceras().getFirst("retry-after"))));
            }
            if (r.estado() >= 500 && intento < props.reintentos()) {
                esperar(intento);
                continue;
            }
            // 401 (clave mala), 400 (petición inválida, por ejemplo un modelo que ya no existe), 5xx sin más reintentos
            log.warn("Groq respondió {}: {}", r.estado(), detalle(r.cuerpo()));
            throw new ServicioExternoException(NO_RESPONDE);
        }
    }

    /** El cuerpo de "chat completions": instrucciones como mensaje de sistema y luego los turnos alternados. */
    Map<String, Object> cuerpo(Solicitud s) {
        List<Map<String, String>> mensajes = new ArrayList<>();
        mensajes.add(Map.of("role", "system", "content", s.instrucciones()));
        // Turnos alternados que empiezan por el usuario: se juntan dos seguidos del mismo autor (no debería pasar,
        // porque los mensajes se guardan de a pares) y se descarta una respuesta inicial suelta.
        List<Turno> turnos = new ArrayList<>();
        for (Turno t : s.historia()) {
            if (turnos.isEmpty() && !t.delEstudiante()) {
                continue;
            }
            Turno ultimo = turnos.isEmpty() ? null : turnos.get(turnos.size() - 1);
            if (ultimo != null && ultimo.delEstudiante() == t.delEstudiante()) {
                turnos.set(turnos.size() - 1, new Turno(t.delEstudiante(), ultimo.texto() + "\n\n" + t.texto()));
            } else {
                turnos.add(t);
            }
        }
        if (!turnos.isEmpty() && turnos.get(turnos.size() - 1).delEstudiante()) {
            turnos.add(new Turno(false, "(sin respuesta)"));
        }
        for (Turno t : turnos) {
            mensajes.add(Map.of("role", t.delEstudiante() ? "user" : "assistant", "content", t.texto()));
        }
        mensajes.add(Map.of("role", "user", "content", s.mensaje()));

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", props.modelo());
        cuerpo.put("messages", mensajes);
        cuerpo.put("temperature", props.temperatura());
        cuerpo.put("max_completion_tokens", props.maxTokens());
        return cuerpo;
    }

    private String interpretar(Respuesta r) {
        try {
            JsonNode raiz = json.readTree(r.cuerpo());
            JsonNode eleccion = raiz.path("choices").path(0);
            String texto = eleccion.path("message").path("content").asText("").trim();
            JsonNode uso = raiz.path("usage");
            log.info("Groq {}: {} tokens de entrada, {} de salida, fin '{}'; quedan {} tokens este minuto y {} "
                            + "peticiones hoy", props.modelo(), uso.path("prompt_tokens").asInt(),
                    uso.path("completion_tokens").asInt(), eleccion.path("finish_reason").asText(),
                    r.cabeceras().getFirst("x-ratelimit-remaining-tokens"),
                    r.cabeceras().getFirst("x-ratelimit-remaining-requests"));
            if (texto.isEmpty()) {
                throw new ServicioExternoException(NO_RESPONDE);
            }
            if ("length".equals(eleccion.path("finish_reason").asText())) {
                texto += "\n\n(La respuesta quedó cortada por su largo. Pídeme que continúe.)";
            }
            return texto;
        } catch (IOException e) {
            log.warn("Groq devolvió una respuesta que no es JSON: {}", e.toString());
            throw new ServicioExternoException(NO_RESPONDE, e);
        }
    }

    /** "retry-after" en segundos, en palabras ("1 minuto", "40 segundos"). */
    static String espera(String retryAfter) {
        try {
            long segundos = (long) Math.ceil(Double.parseDouble(retryAfter));
            if (segundos >= 60) {
                long minutos = (segundos + 59) / 60;
                return minutos + (minutos == 1 ? " minuto" : " minutos");
            }
            return Math.max(segundos, 1) + " segundos";
        } catch (NumberFormatException | NullPointerException e) {
            return "unos minutos";
        }
    }

    /** El mensaje de error del proveedor (nunca trae la clave), recortado para el log. */
    private String detalle(String cuerpo) {
        try {
            String m = json.readTree(cuerpo).path("error").path("message").asText();
            return m.length() > 300 ? m.substring(0, 300) + "…" : m;
        } catch (IOException e) {
            return "(sin detalle)";
        }
    }

    private static void esperar(int intento) {
        try {
            Thread.sleep(500L * (intento + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private RestClient cliente() {
        RestClient c = cliente;
        if (c == null) {
            synchronized (this) {
                if (cliente == null) {
                    JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
                    fabrica.setReadTimeout(Duration.ofSeconds(props.timeoutSegundos()));
                    cliente = RestClient.builder()
                            .baseUrl(props.urlBase())
                            .requestFactory(fabrica)
                            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                            .build();
                }
                c = cliente;
            }
        }
        return c;
    }
}

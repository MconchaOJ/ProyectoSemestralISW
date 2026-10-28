package cl.ubb.pem.practicas.service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HU-15, criterio 2: reconoce un mensaje que parece una planificación pegada como texto, para que el sistema pida
 * elegir la planificación desde "Planificación a revisar" sin llamar al modelo (regla en código, no por instrucciones).
 *
 * SUPUESTO S-72: parece una planificación el mensaje de más de {@value #LARGO_MINIMO} caracteres que nombra al menos
 * {@value #MARCADORES_MINIMOS} partes distintas de una planificación (objetivo, recursos, actividad, evaluación, DUA,
 * inicio, desarrollo, cierre, indicador, habilidad, OA, minutos). Una pregunta corta, aunque nombre esas partes
 * ("¿cómo reparto inicio, desarrollo y cierre?"), sigue llegando al modelo.
 */
final class PlanificacionPegada {

    static final int LARGO_MINIMO = 400;
    static final int MARCADORES_MINIMOS = 2;

    /** Cada grupo es una parte distinta de la planificación (sin tildes, en minúscula). */
    private static final Pattern MARCADOR = Pattern.compile("\\b(objetivo|recurso|actividad|evaluacion|dua|inicio"
            + "|desarrollo|cierre|indicador|habilidad|oa ?\\d|minutos)");

    private PlanificacionPegada() {
    }

    static boolean pareceUna(String texto) {
        String t = texto.strip();
        return t.length() > LARGO_MINIMO && marcadores(t) >= MARCADORES_MINIMOS;
    }

    /** Cuántas partes distintas de una planificación nombra el texto. */
    static int marcadores(String texto) {
        String limpio = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        Set<String> vistos = new HashSet<>();
        Matcher m = MARCADOR.matcher(limpio);
        while (m.find()) {
            vistos.add(m.group(1).startsWith("oa") ? "oa" : m.group(1));
        }
        return vistos.size();
    }
}

package cl.ubb.pem.practicas.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Elige qué partes de la base de conocimiento (HU-29) viajan en cada mensaje al modelo. No cabe todo: las Bases
 * Curriculares solas tienen cientos de páginas y la capa gratuita de Groq limita los tokens por minuto.
 *
 * SUPUESTO S-70: búsqueda por palabras, sin embeddings (la capa gratuita de Groq no los ofrece y no hay presupuesto
 * para otro servicio). Cada documento se corta en fragmentos de unos 700 caracteres y se ordenan por TF-IDF (S-74,
 * desde el 26-oct; antes se sumaban las palabras en común):
 * - TF: cuántas veces aparece cada palabra en el fragmento (y en la consulta), amortiguado con 1 + ln(veces);
 * - IDF: cuánto distingue la palabra según en cuántos fragmentos de toda la base aparece (la fórmula de BM25: casi
 *   cero para una palabra que está en todos, alto para una que está en pocos);
 * - el puntaje es el coseno entre la consulta y el fragmento con esos pesos, así que un fragmento que repite palabras
 *   comunes no le gana a uno que trata el tema de la consulta.
 * Se toman los de más puntaje hasta el tope y se devuelven en el orden de sus documentos. Sigue sin ser búsqueda por
 * significado (un sinónimo no coincide): es la versión simple de la "recuperación" del notebook de la profesora
 * (FAISS + embeddings); cambiarla no toca nada más.
 */
final class FragmentosBaseConocimiento {

    static final int LARGO_FRAGMENTO = 700;
    /** 3 letras: así cuentan siglas como DUA; las de 3 letras sin contenido van en {@link #VACIAS}. */
    private static final int LARGO_MINIMO_PALABRA = 3;

    /** Palabras frecuentes que no sirven para elegir (ya sin tildes). */
    private static final Set<String> VACIAS = Set.of("los", "las", "del", "con", "una", "uno", "que", "por", "sus",
            "son", "mis", "tus", "hay", "fue", "era", "ser", "han", "les", "nos", "ese", "esa", "eso", "asi", "sin",
            "ver", "vez", "dos", "tan", "cual", "para", "como", "esta", "este", "estos", "estas", "pero", "porque",
            "sobre", "entre", "desde", "hasta", "cuando", "donde", "tambien", "puede", "pueden", "debe", "deben", "todo",
            "todos", "todas", "cada", "otro", "otra", "otros", "otras", "tiene", "tienen", "hace", "hacer", "segun",
            "sera", "solo", "muy", "mas", "menos", "algo", "bien", "aqui", "estudiante", "estudiantes", "clase",
            "clases", "planificacion", "revision", "documento", "texto", "sistema", "mensaje", "adjuntada", "completar");

    record Documento(String titulo, String texto) {
    }

    private record Fragmento(int documento, int posicion, String texto, Map<String, Integer> veces) {
    }

    private FragmentosBaseConocimiento() {
    }

    /** Los fragmentos más pertinentes para la consulta, agrupados por documento; vacío si nada coincide. */
    static String elegir(List<Documento> documentos, String consulta, int maximoCaracteres) {
        Map<String, Integer> buscadas = frecuencias(consulta);
        if (documentos.isEmpty() || buscadas.isEmpty()) {
            return "";
        }
        List<Fragmento> fragmentos = new ArrayList<>();
        for (int d = 0; d < documentos.size(); d++) {
            List<String> partes = cortar(documentos.get(d).texto());
            for (int p = 0; p < partes.size(); p++) {
                fragmentos.add(new Fragmento(d, p, partes.get(p), frecuencias(partes.get(p))));
            }
        }
        // en cuántos fragmentos está cada palabra (para pesar las raras más que las comunes)
        Map<String, Integer> enCuantos = new HashMap<>();
        for (Fragmento f : fragmentos) {
            f.veces().keySet().forEach(w -> enCuantos.merge(w, 1, Integer::sum));
        }
        int total = fragmentos.size();
        Map<String, Double> pesoConsulta = pesos(buscadas, enCuantos, total);
        double normaConsulta = norma(pesoConsulta);
        Map<Fragmento, Double> puntaje = new HashMap<>();
        for (Fragmento f : fragmentos) {
            Map<String, Double> peso = pesos(f.veces(), enCuantos, total);
            double producto = 0;
            for (Map.Entry<String, Double> e : pesoConsulta.entrySet()) {
                producto += e.getValue() * peso.getOrDefault(e.getKey(), 0.0);
            }
            if (producto > 0) {
                puntaje.put(f, producto / (normaConsulta * norma(peso)));
            }
        }
        List<Fragmento> elegidos = new ArrayList<>();
        int usados = 0;
        for (Fragmento f : ordenados(puntaje)) {
            if (usados + f.texto().length() > maximoCaracteres) {
                continue;
            }
            elegidos.add(f);
            usados += f.texto().length();
        }
        if (elegidos.isEmpty()) {
            return "";
        }
        elegidos.sort(Comparator.comparingInt(Fragmento::documento).thenComparingInt(Fragmento::posicion));
        StringBuilder s = new StringBuilder();
        int actual = -1;
        for (Fragmento f : elegidos) {
            if (f.documento() != actual) {
                actual = f.documento();
                s.append("### ").append(documentos.get(actual).titulo()).append('\n');
            } else {
                s.append("[…]\n");
            }
            s.append(f.texto().strip()).append('\n');
        }
        return s.toString();
    }

    /** Los fragmentos con puntaje, del más pertinente al menos (a igual puntaje, en el orden de la base). */
    private static List<Fragmento> ordenados(Map<Fragmento, Double> puntaje) {
        return puntaje.keySet().stream()
                .sorted(Comparator.comparingDouble((Fragmento f) -> -puntaje.get(f))
                        .thenComparingInt(Fragmento::documento).thenComparingInt(Fragmento::posicion))
                .toList();
    }

    /** Peso TF-IDF de cada palabra: (1 + ln veces) × IDF de BM25, ln(1 + (N - n + 0,5) / (n + 0,5)). */
    private static Map<String, Double> pesos(Map<String, Integer> veces, Map<String, Integer> enCuantos, int total) {
        Map<String, Double> salida = new HashMap<>();
        veces.forEach((w, v) -> {
            int n = enCuantos.getOrDefault(w, 0);
            salida.put(w, (1 + Math.log(v)) * Math.log(1 + (total - n + 0.5) / (n + 0.5)));
        });
        return salida;
    }

    private static double norma(Map<String, Double> pesos) {
        return Math.sqrt(pesos.values().stream().mapToDouble(p -> p * p).sum());
    }

    /** Corta el texto en fragmentos de hasta LARGO_FRAGMENTO caracteres, respetando párrafos y espacios. */
    static List<String> cortar(String texto) {
        List<String> partes = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String parrafo : texto.split("\\n\\s*\\n|\\n")) {
            String p = parrafo.strip();
            if (p.isEmpty()) {
                continue;
            }
            while (p.length() > LARGO_FRAGMENTO) {
                int corte = p.lastIndexOf(' ', LARGO_FRAGMENTO);
                corte = corte < LARGO_FRAGMENTO / 2 ? LARGO_FRAGMENTO : corte;
                cerrar(partes, actual);
                partes.add(p.substring(0, corte).strip());
                p = p.substring(corte).strip();
            }
            if (actual.length() + p.length() + 1 > LARGO_FRAGMENTO) {
                cerrar(partes, actual);
            }
            actual.append(actual.isEmpty() ? "" : "\n").append(p);
        }
        cerrar(partes, actual);
        return partes;
    }

    private static void cerrar(List<String> partes, StringBuilder actual) {
        if (!actual.isEmpty()) {
            partes.add(actual.toString());
            actual.setLength(0);
        }
    }

    /** Las palabras distintas del texto (ver {@link #frecuencias}). */
    static Set<String> palabras(String texto) {
        return frecuencias(texto).keySet();
    }

    /** Cuántas veces está cada palabra de al menos 3 letras, en minúscula y sin tildes, sin las palabras vacías. */
    static Map<String, Integer> frecuencias(String texto) {
        String limpio = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        Map<String, Integer> salida = new HashMap<>();
        for (String w : limpio.split("[^a-z0-9ñ]+")) {
            if (w.length() >= LARGO_MINIMO_PALABRA && !VACIAS.contains(w)) {
                salida.merge(w, 1, Integer::sum);
            }
        }
        return salida;
    }
}

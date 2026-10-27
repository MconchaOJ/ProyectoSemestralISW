package cl.ubb.pem.practicas.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Elige qué partes de la base de conocimiento (HU-29) viajan en cada mensaje al modelo. No cabe todo: las Bases
 * Curriculares solas tienen cientos de páginas y la capa gratuita de Groq limita los tokens por minuto.
 *
 * SUPUESTO S-70: búsqueda por palabras, sin embeddings. Cada documento se corta en fragmentos de unos 700
 * caracteres; cada fragmento suma un puntaje por cada palabra de la consulta (el mensaje y la planificación) que
 * contiene, pesada por lo rara que es en toda la base (una palabra que está en todos los fragmentos no ayuda a
 * elegir). Se toman los de más puntaje hasta el tope y se devuelven en el orden de sus documentos. Es la versión
 * simple de la "recuperación" del notebook de la profesora (FAISS + embeddings); cambiarla no toca nada más.
 */
final class FragmentosBaseConocimiento {

    static final int LARGO_FRAGMENTO = 700;
    private static final int LARGO_MINIMO_PALABRA = 4;

    /** Palabras frecuentes que no sirven para elegir (ya sin tildes). */
    private static final Set<String> VACIAS = Set.of("para", "como", "esta", "este", "estos", "estas", "pero", "porque",
            "sobre", "entre", "desde", "hasta", "cuando", "donde", "tambien", "puede", "pueden", "debe", "deben", "todo",
            "todos", "todas", "cada", "otro", "otra", "otros", "otras", "tiene", "tienen", "hace", "hacer", "segun",
            "sera", "solo", "muy", "mas", "menos", "algo", "bien", "aqui", "estudiante", "estudiantes", "clase",
            "clases", "planificacion", "revision", "documento", "texto", "sistema", "mensaje", "adjuntada", "completar");

    record Documento(String titulo, String texto) {
    }

    private record Fragmento(int documento, int posicion, String texto, Set<String> palabras) {
    }

    private FragmentosBaseConocimiento() {
    }

    /** Los fragmentos más pertinentes para la consulta, agrupados por documento; vacío si nada coincide. */
    static String elegir(List<Documento> documentos, String consulta, int maximoCaracteres) {
        Set<String> buscadas = palabras(consulta);
        if (documentos.isEmpty() || buscadas.isEmpty()) {
            return "";
        }
        List<Fragmento> fragmentos = new ArrayList<>();
        for (int d = 0; d < documentos.size(); d++) {
            List<String> partes = cortar(documentos.get(d).texto());
            for (int p = 0; p < partes.size(); p++) {
                fragmentos.add(new Fragmento(d, p, partes.get(p), palabras(partes.get(p))));
            }
        }
        // cuántos fragmentos tienen cada palabra buscada (para pesar las raras más que las comunes)
        Map<String, Integer> enCuantos = new HashMap<>();
        for (Fragmento f : fragmentos) {
            for (String w : f.palabras()) {
                if (buscadas.contains(w)) {
                    enCuantos.merge(w, 1, Integer::sum);
                }
            }
        }
        Map<Fragmento, Double> puntaje = new HashMap<>();
        for (Fragmento f : fragmentos) {
            double p = 0;
            for (String w : f.palabras()) {
                Integer n = enCuantos.get(w);
                if (n != null) {
                    p += Math.log(1.0 + (double) fragmentos.size() / n);
                }
            }
            if (p > 0) {
                puntaje.put(f, p);
            }
        }
        List<Fragmento> elegidos = new ArrayList<>();
        int usados = 0;
        for (Fragmento f : puntaje.keySet().stream()
                .sorted(Comparator.comparingDouble((Fragmento f) -> -puntaje.get(f))
                        .thenComparingInt(Fragmento::documento).thenComparingInt(Fragmento::posicion))
                .toList()) {
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

    /** Palabras de al menos 4 letras, en minúscula y sin tildes, sin las palabras vacías. */
    static Set<String> palabras(String texto) {
        String limpio = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        Set<String> salida = new HashSet<>();
        for (String w : limpio.split("[^a-z0-9ñ]+")) {
            if (w.length() >= LARGO_MINIMO_PALABRA && !VACIAS.contains(w)) {
                salida.add(w);
            }
        }
        return salida;
    }
}

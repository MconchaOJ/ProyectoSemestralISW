package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;

import cl.ubb.pem.practicas.service.FragmentosBaseConocimiento.Documento;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-29, SUPUESTO S-70: la elección de fragmentos de la base de conocimiento, sin base de datos ni modelo. */
class FragmentosBaseConocimientoTest {

    private static final String RELLENO = "Texto general de la carrera sin relación con el tema de la consulta. ";

    @Test
    void eligeElFragmentoQueCoincideConLaConsultaAunqueEsteAlFinalDeUnDocumentoLargo() {
        String largo = RELLENO.repeat(200) + "\n\nOA 3: mostrar que comprenden la función cuadrática y la parábola.";
        String r = FragmentosBaseConocimiento.elegir(List.of(new Documento("Bases Curriculares", largo)),
                "Mi clase es sobre la funcion CUADRATICA y su parabola", 800);
        assertThat(r).startsWith("### Bases Curriculares").contains("OA 3: mostrar que comprenden la función cuadrática")
                .hasSizeLessThan(900);
    }

    @Test
    void respetaElTopeDeCaracteresYOrdenaPorDocumento() {
        List<Documento> docs = List.of(new Documento("Rúbrica", "La parábola y el vértice.\n\n" + RELLENO.repeat(30)),
                new Documento("DUA", "Representación de la parábola con material concreto."));
        String r = FragmentosBaseConocimiento.elegir(docs, "parabola", 5_000);
        assertThat(r.indexOf("### Rúbrica")).isLessThan(r.indexOf("### DUA"));
        assertThat(FragmentosBaseConocimiento.elegir(docs, "parabola", 10)).isEmpty();   // ningún fragmento cabe
    }

    @Test
    void sinDocumentosOSinCoincidenciasDevuelveVacio() {
        assertThat(FragmentosBaseConocimiento.elegir(List.of(), "parabola", 5_000)).isEmpty();
        assertThat(FragmentosBaseConocimiento.elegir(List.of(new Documento("X", RELLENO)), "logaritmos", 5_000)).isEmpty();
        // las palabras vacías ("para", "estudiantes", "clase") no cuentan como coincidencia
        assertThat(FragmentosBaseConocimiento.elegir(List.of(new Documento("X", "para los estudiantes de la clase")),
                "para estudiantes clase", 5_000)).isEmpty();
    }

    /**
     * S-74 (TF-IDF): la consulta nombra las partes de su planificación (palabras que están en casi toda la base) y
     * pregunta por el DUA. El fragmento del DUA comparte 2 palabras con la consulta; el genérico, 6. Con el conteo
     * anterior ganaba el genérico; ahora gana el del tema, porque sus palabras son raras en la base y las repite.
     */
    @Test
    void unFragmentoDelTemaLeGanaAUnoConMasPalabrasComunesEnComun() {
        String dua = "Acciones DUA para la diversidad: el DUA ofrece múltiples formas de representación; el DUA "
                + "atiende la diversidad del grupo con apoyos accesibles.";
        String generico = "Objetivos, actividades, evaluación, aprendizaje, matemática y recursos del curso: los "
                + "objetivos guían las actividades y la evaluación del aprendizaje de matemática con recursos.";
        List<Documento> docs = new java.util.ArrayList<>();
        docs.add(new Documento("Generalidades", generico));
        for (int k = 1; k <= 6; k++) {
            docs.add(new Documento("Orientaciones " + k, "Orientaciones de la práctica número " + k
                    + ": objetivos, actividades, evaluación, aprendizaje, matemática y recursos."));
        }
        docs.add(new Documento("Acciones DUA", dua));
        String consulta = "Mi clase tiene objetivos, actividades, evaluación del aprendizaje de matemática y recursos. "
                + "¿Cómo uso el DUA para la diversidad?";

        var buscadas = FragmentosBaseConocimiento.palabras(consulta);
        assertThat(FragmentosBaseConocimiento.palabras(generico).stream().filter(buscadas::contains).count()).isEqualTo(6);
        assertThat(FragmentosBaseConocimiento.palabras(dua).stream().filter(buscadas::contains).count()).isEqualTo(2);

        // con el tope solo cabe un fragmento: el primero del ranking
        String r = FragmentosBaseConocimiento.elegir(docs, consulta, dua.length() + 20);
        assertThat(r).startsWith("### Acciones DUA").doesNotContain("Generalidades").doesNotContain("Orientaciones");
    }

    /** S-74: a igualdad de palabras en común, gana el fragmento que más trata el tema (TF), no el que va primero. */
    @Test
    void entreDosFragmentosConLaMismaPalabraGanaElQueLaTrataMas() {
        String dePaso = "La parábola aparece en la unidad junto con funciones lineales, tablas de valores y gráficos de datos.";
        String delTema = "La parábola: vértice de la parábola, eje de simetría de la parábola y concavidad.";
        List<Documento> docs = List.of(new Documento("Programa", dePaso), new Documento("Rúbrica", delTema));
        String r = FragmentosBaseConocimiento.elegir(docs, "parabola", Math.max(dePaso.length(), delTema.length()) + 5);
        assertThat(r).startsWith("### Rúbrica").doesNotContain("Programa");
    }

    @Test
    void cortaLosParrafosLargosSinPasarseDelLargo() {
        List<String> partes = FragmentosBaseConocimiento.cortar("palabra ".repeat(400));
        assertThat(partes).hasSizeGreaterThan(3)
                .allSatisfy(p -> assertThat(p.length()).isLessThanOrEqualTo(FragmentosBaseConocimiento.LARGO_FRAGMENTO));
    }
}

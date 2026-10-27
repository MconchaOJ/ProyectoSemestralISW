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

    @Test
    void cortaLosParrafosLargosSinPasarseDelLargo() {
        List<String> partes = FragmentosBaseConocimiento.cortar("palabra ".repeat(400));
        assertThat(partes).hasSizeGreaterThan(3)
                .allSatisfy(p -> assertThat(p.length()).isLessThanOrEqualTo(FragmentosBaseConocimiento.LARGO_FRAGMENTO));
    }
}

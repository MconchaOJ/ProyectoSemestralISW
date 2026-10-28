package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;

import cl.ubb.pem.practicas.entity.ComponenteNota;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** HU-09: el cálculo de la nota ponderada, sin base de datos. */
class NotaFinalCalculoTest {

    private static ComponenteNota componente(int id, String peso) {
        ComponenteNota c = new ComponenteNota();
        c.setId(id);
        c.setNombre("C" + id);
        c.setPeso(new BigDecimal(peso));
        c.setOrden(id);
        return c;
    }

    private static final List<ComponenteNota> PRACTICA_PEDAGOGICA = List.of(componente(1, "10"), componente(2, "17"),
            componente(3, "5"), componente(4, "13"), componente(5, "3"), componente(6, "5"), componente(7, "20"),
            componente(8, "27"));

    private static Map<Integer, BigDecimal> notas(String... valores) {
        java.util.HashMap<Integer, BigDecimal> mapa = new java.util.HashMap<>();
        for (int n = 0; n < valores.length; n++) {
            mapa.put(n + 1, new BigDecimal(valores[n]));
        }
        return mapa;
    }

    @Test
    void ponderaConLosPesosYRedondeaAUnDecimal() {
        // (6,0·10 + 5,5·17 + 6,2·5 + 5,8·13 + 6,5·3 + 6,0·5 + 5,7·20 + 6,1·27) / 100 = 5,881
        assertThat(NotaFinalService.calcular(PRACTICA_PEDAGOGICA,
                notas("6.0", "5.5", "6.2", "5.8", "6.5", "6.0", "5.7", "6.1"))).isEqualByComparingTo("5.9");
    }

    @Test
    void elCincoHaciaArribaSube() {
        List<ComponenteNota> mitades = List.of(componente(1, "50"), componente(2, "50"));
        assertThat(NotaFinalService.calcular(mitades, notas("3.9", "4.0"))).isEqualByComparingTo("4.0");   // 3,95
        assertThat(NotaFinalService.calcular(mitades, notas("3.8", "4.0"))).isEqualByComparingTo("3.9");   // 3,90
    }

    @Test
    void sinTodasLasNotasOConPesosQueNoSuman100NoHayNota() {
        assertThat(NotaFinalService.calcular(PRACTICA_PEDAGOGICA, notas("6.0", "5.5", "6.2"))).isNull();
        assertThat(NotaFinalService.calcular(List.of(componente(1, "60"), componente(2, "30")), notas("6.0", "6.0")))
                .isNull();
        assertThat(NotaFinalService.calcular(List.of(), Map.of())).isNull();
    }
}

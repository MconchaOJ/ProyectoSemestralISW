package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-09, S-81: escala de 1,0 a 7,0 con 60 % de exigencia sobre el puntaje máximo. */
class EscalaNotasTest {

    private static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void elCuatroQuedaEnElSesentaPorCientoDelMaximo() {
        // pauta de observación de clase: 20 indicadores x 7 = 140 puntos; el 60 % son 84
        assertThat(EscalaNotas.nota(84, 140)).isEqualByComparingTo("4.0");
        assertThat(EscalaNotas.nota(0, 140)).isEqualByComparingTo("1.0");
        assertThat(EscalaNotas.nota(140, 140)).isEqualByComparingTo("7.0");
    }

    @Test
    void bajoYSobreElCorteLaNotaSubeEnLineaRecta() {
        assertThat(EscalaNotas.nota(42, 140)).isEqualByComparingTo("2.5");    // mitad del tramo bajo
        assertThat(EscalaNotas.nota(112, 140)).isEqualByComparingTo("5.5");   // mitad del tramo alto
        assertThat(EscalaNotas.nota(100, 140)).isEqualByComparingTo("4.9");   // 4 + 3 x 16 / 56 = 4,857
        assertThat(EscalaNotas.nota(76, 140)).isEqualByComparingTo("3.7");    // 1 + 3 x 76 / 84 = 3,714
        assertThat(EscalaNotas.nota(85, 140)).isEqualByComparingTo("4.1");    // 4,054: se redondea hacia arriba
        // pauta de Especialidad 1.ª: 19 indicadores x 7 = 133; el 60 % son 79,8
        assertThat(EscalaNotas.nota(80, 133)).isEqualByComparingTo("4.0");
        assertThat(EscalaNotas.nota(79, 133)).isEqualByComparingTo("4.0");   // 3,970
    }

    @Test
    void unPuntajeImposibleNoTieneNota() {
        assertThatThrownBy(() -> EscalaNotas.nota(141, 140)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EscalaNotas.nota(-1, 140)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EscalaNotas.nota(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elPromedioDeVariasNotasSeRedondeaAUnDecimal() {
        assertThat(EscalaNotas.promedio(List.of(n("5.5"), n("6.0")))).isEqualByComparingTo("5.8");   // 5,75
        assertThat(EscalaNotas.promedio(List.of(n("4.1")))).isEqualByComparingTo("4.1");
        assertThat(EscalaNotas.redondear(n("5.949"))).isEqualByComparingTo("5.9");
    }
}

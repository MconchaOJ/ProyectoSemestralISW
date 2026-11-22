package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * HU-09, S-81: escala con 60 % de exigencia sobre el puntaje máximo y la nota mínima de cada pauta (su escala mínima):
 * 1,0 en la pauta de observación de clase (escala 1 a 7), 2,0 en las de Práctica Pedagógica y Profesional (2 a 7).
 */
class EscalaNotasTest {

    private static final BigDecimal UNO = EscalaNotas.notaMinima(1);
    private static final BigDecimal DOS = EscalaNotas.notaMinima(2);

    private static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void elCuatroQuedaEnElSesentaPorCientoDelMaximo() {
        // pauta de observación de clase: 20 indicadores x 7 = 140 puntos; el 60 % son 84
        assertThat(EscalaNotas.nota(84, 140, UNO)).isEqualByComparingTo("4.0");
        assertThat(EscalaNotas.nota(0, 140, UNO)).isEqualByComparingTo("1.0");
        assertThat(EscalaNotas.nota(140, 140, UNO)).isEqualByComparingTo("7.0");
    }

    @Test
    void conNotaMinimaUnoBajoYSobreElCorteSubeEnLineaRecta() {
        assertThat(EscalaNotas.nota(42, 140, UNO)).isEqualByComparingTo("2.5");    // mitad del tramo bajo
        assertThat(EscalaNotas.nota(112, 140, UNO)).isEqualByComparingTo("5.5");   // mitad del tramo alto
        assertThat(EscalaNotas.nota(100, 140, UNO)).isEqualByComparingTo("4.9");   // 4 + 3 x 16 / 56 = 4,857
        assertThat(EscalaNotas.nota(76, 140, UNO)).isEqualByComparingTo("3.7");    // 1 + 3 x 76 / 84 = 3,714
        assertThat(EscalaNotas.nota(85, 140, UNO)).isEqualByComparingTo("4.1");    // 4,054: se redondea hacia arriba
    }

    @Test
    void conNotaMinimaDosCoincideConLaEscalaImpresaEnLaPauta() {
        // escaladenotas.cl en la pauta de Especialidad 1.ª: mínimo 2,0, aprobación 4,0, exigencia 60 %, máximo 133
        assertThat(EscalaNotas.nota(0, 133, DOS)).isEqualByComparingTo("2.0");
        assertThat(EscalaNotas.nota(20, 133, DOS)).isEqualByComparingTo("2.5");
        assertThat(EscalaNotas.nota(40, 133, DOS)).isEqualByComparingTo("3.0");   // con mínimo 1,0 daba 2,5
        assertThat(EscalaNotas.nota(59, 133, DOS)).isEqualByComparingTo("3.5");
        assertThat(EscalaNotas.nota(78, 133, DOS)).isEqualByComparingTo("4.0");
        assertThat(EscalaNotas.nota(81, 133, DOS)).isEqualByComparingTo("4.1");
        assertThat(EscalaNotas.nota(100, 133, DOS)).isEqualByComparingTo("5.1");
        assertThat(EscalaNotas.nota(119, 133, DOS)).isEqualByComparingTo("6.2");
        assertThat(EscalaNotas.nota(133, 133, DOS)).isEqualByComparingTo("7.0");
    }

    @Test
    void laNotaMinimaEsLaEscalaMinimaAcotadaEntreUnoYTres() {
        assertThat(EscalaNotas.notaMinima(0)).isEqualByComparingTo("1.0");
        assertThat(EscalaNotas.notaMinima(1)).isEqualByComparingTo("1.0");
        assertThat(EscalaNotas.notaMinima(2)).isEqualByComparingTo("2.0");
        assertThat(EscalaNotas.notaMinima(5)).isEqualByComparingTo("3.0");
    }

    @Test
    void unPuntajeImposibleNoTieneNota() {
        assertThatThrownBy(() -> EscalaNotas.nota(141, 140, UNO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EscalaNotas.nota(-1, 140, UNO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EscalaNotas.nota(0, 0, UNO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elPromedioDeVariasNotasSeRedondeaAUnDecimal() {
        assertThat(EscalaNotas.promedio(List.of(n("5.5"), n("6.0")))).isEqualByComparingTo("5.8");   // 5,75
        assertThat(EscalaNotas.promedio(List.of(n("4.1")))).isEqualByComparingTo("4.1");
        assertThat(EscalaNotas.redondear(n("5.949"))).isEqualByComparingTo("5.9");
    }
}

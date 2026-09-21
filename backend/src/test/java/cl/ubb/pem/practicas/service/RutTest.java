package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RutTest {

    @ParameterizedTest
    @ValueSource(strings = {"12345678-5", "11111111-1", "22222222-2", "33333333-3", "44444444-4", "55555555-5"})
    void rutsConDigitoVerificadorCorrectoSonValidos(String rut) {
        assertThat(Rut.esValido(rut)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678-4", "12345678-K", "ABC", "1234-5", "123456789012-3", "", "12345678"})
    void rutsInvalidosSeRechazan(String rut) {
        assertThat(Rut.esValido(rut)).isFalse();
    }

    @Test
    void normalizaPuntosMayusculasYGuion() {
        assertThat(Rut.normalizar("12.345.678-5")).isEqualTo("12345678-5");
        assertThat(Rut.normalizar("123456785")).isEqualTo("12345678-5");
        assertThat(Rut.normalizar(" 7.000.013-k ")).isEqualTo("7000013-K");
    }

    @Test
    void calculaDigitoVerificadorCeroYK() {
        assertThat(Rut.digitoVerificador("7000013")).isEqualTo('K');
        assertThat(Rut.digitoVerificador("7000004")).isEqualTo('0');
        assertThat(Rut.esValido("7000013-K")).isTrue();
    }
}

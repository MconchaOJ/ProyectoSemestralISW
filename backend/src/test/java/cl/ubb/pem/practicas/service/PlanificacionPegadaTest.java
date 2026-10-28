package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** HU-15, criterio 2 (SUPUESTO S-72): cuándo un mensaje parece una planificación pegada como texto. */
class PlanificacionPegadaTest {

    private static final String RELLENO = "Quisiera contarte cómo me fue en el colegio esta semana con mi curso. ";

    @Test
    void largoYConVariasPartesDeUnaPlanificacionSiLoEs() {
        String texto = RELLENO.repeat(5) + "Objetivo: graficar la parábola. Recursos: GeoGebra. Evaluación: ticket.";
        assertThat(texto.length()).isGreaterThan(PlanificacionPegada.LARGO_MINIMO);
        assertThat(PlanificacionPegada.marcadores(texto)).isEqualTo(3);
        assertThat(PlanificacionPegada.pareceUna(texto)).isTrue();
    }

    @Test
    void unaPreguntaCortaNoLoEsAunqueNombreLasPartes() {
        assertThat(PlanificacionPegada.pareceUna("¿Cómo reparto inicio, desarrollo y cierre? ¿Y el objetivo?")).isFalse();
    }

    @Test
    void unTextoLargoSinPartesDeUnaPlanificacionNoLoEs() {
        String texto = RELLENO.repeat(8);
        assertThat(texto.length()).isGreaterThan(PlanificacionPegada.LARGO_MINIMO);
        assertThat(PlanificacionPegada.pareceUna(texto)).isFalse();
        // una sola parte nombrada tampoco basta
        assertThat(PlanificacionPegada.pareceUna(texto + " Mi objetivo era otro.")).isFalse();
    }

    @Test
    void reconoceLasPartesSinTildesNiMayusculasYCuentaCadaUnaUnaVez() {
        assertThat(PlanificacionPegada.marcadores("EVALUACIÓN, evaluacion, Actividades, actividad, OA 3, oa5, DUA"))
                .isEqualTo(4);
    }
}

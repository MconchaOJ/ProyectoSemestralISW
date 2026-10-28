package cl.ubb.pem.practicas.dto;

import java.util.List;

/**
 * Los textos de la Escala SUS y de la rúbrica de juicio experto (HU-30), para que el formulario del validador y la
 * vista de resultados del coordinador muestren lo mismo que guarda el sistema.
 *
 * @param itemsSus          los 10 ítems, en orden (1 a 10)
 * @param escalaSus         qué significa cada valor de 1 a 5
 * @param criterios         los 9 criterios de la rúbrica, con el descriptor del nivel 4
 * @param nivelesRubrica    qué significa cada valor de 1 a 4
 * @param preguntasAbiertas las 4 preguntas abiertas que siguen a la rúbrica
 */
public record InstrumentosValidacionDto(List<String> itemsSus, List<String> escalaSus, List<CriterioRubrica> criterios,
                                        List<String> nivelesRubrica, List<String> preguntasAbiertas) {

    public record CriterioRubrica(int numero, String nombre, String logroAlto) {
    }
}

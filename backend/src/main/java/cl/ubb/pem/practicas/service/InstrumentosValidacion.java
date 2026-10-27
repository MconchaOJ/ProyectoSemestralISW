package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.InstrumentosValidacionDto;
import cl.ubb.pem.practicas.dto.InstrumentosValidacionDto.CriterioRubrica;
import java.util.List;

/**
 * Los instrumentos de validación del chatbot (HU-30), copiados de la propuesta "RetroPlani Mate" V3 del coordinador
 * de prácticas (Documents/ChatBot (1).rar, apartado I: carta a los jueces expertos). No son del equipo:
 * - Escala SUS adaptada al chatbot: 10 ítems de 1 a 5, versión validada en español de Sevilla-González et al. (2020),
 *   adaptada de Brooke (1986). Los ítems impares son positivos y los pares negativos (fórmula de Brooke en la base).
 * - Rúbrica de juicio experto: 9 criterios de 1 (logro insuficiente) a 4 (logro alto); aquí va el descriptor del
 *   nivel 4 de cada uno, como guía para quien responde.
 * - Las 4 preguntas abiertas de observación cualitativa que siguen a la rúbrica.
 */
final class InstrumentosValidacion {

    static final List<String> ITEMS_SUS = List.of(
            "Me gustaría usar este chatbot frecuentemente.",
            "Considero que este chatbot es innecesariamente complejo.",
            "Considero que el chatbot es fácil de usar.",
            "Considero necesario el apoyo de personal experto para poder utilizar este chatbot.",
            "Considero que las funciones del chatbot están bien integradas.",
            "Considero que el chatbot presenta muchas contradicciones.",
            "Imagino que la mayoría de las personas aprenderían a usar este chatbot rápidamente.",
            "Considero que el uso de este chatbot es tedioso.",
            "Me sentí muy confiado al usar el chatbot.",
            "Necesité saber bastantes cosas antes de poder empezar a usar este chatbot.");

    static final List<String> ESCALA_SUS = List.of("Totalmente en desacuerdo", "En desacuerdo",
            "Ni de acuerdo ni en desacuerdo", "De acuerdo", "Totalmente de acuerdo");

    static final List<CriterioRubrica> CRITERIOS = List.of(
            new CriterioRubrica(1, "Uso de planificación adjunta como base de análisis",
                    "Solicita, reconoce y utiliza una planificación adjunta como condición para entregar "
                            + "retroalimentación. Sus respuestas se basan claramente en el documento revisado."),
            new CriterioRubrica(2, "Pertinencia pedagógica y contextual",
                    "La retroalimentación se relaciona directamente con los objetivos, actividades, recursos, "
                            + "evaluación, nivel escolar y contexto de la planificación."),
            new CriterioRubrica(3, "Coherencia didáctica matemática",
                    "Las sugerencias son didácticamente adecuadas para la enseñanza de la matemática, consideran el "
                            + "contenido, el nivel escolar y posibles dificultades de aprendizaje."),
            new CriterioRubrica(4, "Calidad formativa de la retroalimentación",
                    "Entrega comentarios claros, específicos, constructivos y aplicables, que ayudan al profesor en "
                            + "formación a mejorar autónomamente su planificación."),
            new CriterioRubrica(5, "Integración del Diseño Universal para el Aprendizaje (DUA), inclusión y accesibilidad",
                    "Identifica barreras para el aprendizaje y la participación, y propone apoyos diversificados, "
                            + "accesibles y contextualizados desde el DUA."),
            new CriterioRubrica(6, "Coherencia con el rol del chatbot de retroalimentación",
                    "Retroalimenta, orienta, pregunta y sugiere sin diseñar, completar ni reemplazar la planificación "
                            + "elaborada por el usuario."),
            new CriterioRubrica(7, "Manejo de información insuficiente y límites de la respuesta",
                    "Identifica vacíos relevantes, solicita antecedentes cuando corresponde y evita emitir juicios "
                            + "fuertes sin evidencia suficiente."),
            new CriterioRubrica(8, "Claridad, organización y utilidad comunicativa",
                    "Presenta la información de manera clara, ordenada, breve y fácil de usar; jerarquiza las ideas y "
                            + "facilita la toma de decisiones."),
            new CriterioRubrica(9, "Consistencia conversacional y confiabilidad pedagógica",
                    "Mantiene coherencia durante la interacción, no se contradice, evita errores conceptuales y "
                            + "fundamenta sus observaciones en la planificación."));

    static final List<String> NIVELES_RUBRICA = List.of("Logro insuficiente", "Logro parcial", "Logro adecuado",
            "Logro alto");

    static final List<String> PREGUNTAS_ABIERTAS = List.of(
            "¿Cuáles son las principales fortalezas de RetroPlani Mate como herramienta de retroalimentación "
                    + "pedagógica para profesores en formación?",
            "¿Qué debilidades, riesgos o aspectos críticos deberían corregirse antes de aplicarlo con estudiantes?",
            "¿Qué mejoras concretas sugiere para fortalecer la calidad de la retroalimentación, la integración del "
                    + "DUA, la inclusión o la accesibilidad?",
            "¿Recomendaría el uso de RetroPlani Mate en la formación inicial docente? Fundamente brevemente su respuesta.");

    static final InstrumentosValidacionDto DTO = new InstrumentosValidacionDto(ITEMS_SUS, ESCALA_SUS, CRITERIOS,
            NIVELES_RUBRICA, PREGUNTAS_ABIERTAS);

    private InstrumentosValidacion() {
    }
}

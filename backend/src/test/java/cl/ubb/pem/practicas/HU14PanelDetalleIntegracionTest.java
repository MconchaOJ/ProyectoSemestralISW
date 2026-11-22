package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HU-14: el detalle del panel que viene del trabajo de Alej (HU14PanelIntegracionTest de su commit 7e9b132), llevado a
 * nuestro endpoint /api/reportes/panel. En 2026-2 hay 13 inscripciones, todas en curso; Trabajo Colaborativo tiene 5,
 * con 3 evaluaciones finalizadas (promedios 4,70, 4,15 y 5,70), 2 borradores (Camila e inscripción 34) y las
 * planificaciones 6 (EN_REVISION_GUIA), 7 (EN_AUTORIZACION), 4 y 15 (CON_OBSERVACIONES); la 9 (Práctica Profesional)
 * espera al guía.
 */
class HU14PanelDetalleIntegracionTest extends IntegracionBase {

    private JsonNode panel(String filtros) throws Exception {
        return cuerpo(consultar("/api/reportes/panel" + filtros, COORDINADOR).andExpect(status().isOk()));
    }

    private static JsonNode buscar(JsonNode lista, String campo, long valor) {
        for (JsonNode n : lista) {
            if (n.get(campo).asLong() == valor) {
                return n;
            }
        }
        throw new AssertionError("No está " + campo + " = " + valor);
    }

    @Test
    void lasPendientesDicenAQuienEsperanYHaceCuantosDias() throws Exception {
        JsonNode pendientes = panel("?periodo=2026-2").get("pendientes");
        assertThat(pendientes.findValuesAsText("planificacionId")).containsExactlyInAnyOrder("4", "15", "6", "7", "9");
        JsonNode devuelta = buscar(pendientes, "planificacionId", 4);
        assertThat(devuelta.get("rolQueDebeActuar").asText()).isEqualTo("ESTUDIANTE");
        assertThat(devuelta.get("esperaA").asText()).isEqualTo("Camila Rojas Soto");
        assertThat(devuelta.get("requiereAtencion").asBoolean()).isFalse();   // espera al estudiante, no a un profesor
        JsonNode seis = buscar(pendientes, "planificacionId", 6);
        assertThat(seis.get("estudiante").asText()).isEqualTo("Diego Muñoz Araya");
        assertThat(seis.get("esperaA").asText()).isEqualTo("Lorena Vidal Pizarro");          // su guía
        assertThat(seis.get("requiereAtencion").asBoolean()).isTrue();                        // más de 3 días
        assertThat(buscar(pendientes, "planificacionId", 7).get("esperaA").asText()).isEqualTo("Ricardo Núñez Salas");
        // la que lleva más tiempo esperando va primero
        long anterior = Long.MAX_VALUE;
        for (JsonNode p : pendientes) {
            assertThat(p.get("diasEsperando").asLong()).isLessThanOrEqualTo(anterior);
            anterior = p.get("diasEsperando").asLong();
        }
    }

    @Test
    void cadaAsignaturaTraeDevueltasAutorizadasBorradoresYPromedio() throws Exception {
        JsonNode p = panel("?periodo=2026-2");
        JsonNode tc = buscar(p.get("filas"), "asignaturaId", 3);
        assertThat(tc.get("planificacionesConObservaciones").asInt()).isEqualTo(2);
        assertThat(tc.get("planificacionesAutorizadas").asInt()).isEqualTo(3);
        assertThat(tc.get("evaluacionesBorrador").asInt()).isEqualTo(2);
        assertThat(tc.get("promedioEvaluaciones").decimalValue()).isEqualByComparingTo("4.85");   // (4,70 + 4,15 + 5,70) / 3
        assertThat(p.get("totales").get("planificacionesConObservaciones").asInt()).isEqualTo(2);
        assertThat(p.get("totales").get("evaluacionesBorrador").asInt()).isEqualTo(2);
        assertThat(buscar(p.get("sinEvaluar"), "inscripcionId", 3).get("borradores").asInt()).isEqualTo(1);
    }

    @Test
    void cadaEstudianteTieneSuAvance() throws Exception {
        JsonNode avance = panel("?periodo=2026-2").get("avance");
        assertThat(avance).hasSize(13);
        JsonNode diego = buscar(avance, "inscripcionId", 6);
        assertThat(diego.get("estudiante").asText()).isEqualTo("Diego Muñoz Araya");
        assertThat(diego.get("planificaciones").asInt()).isEqualTo(2);
        assertThat(diego.get("evaluacionesFinalizadas").asInt()).isEqualTo(1);
        assertThat(diego.get("promedioEvaluaciones").decimalValue()).isEqualByComparingTo("4.70");
        assertThat(panel("?periodo=2020-1").get("avance")).isEmpty();
    }

    @Test
    void lasPlanificacionesDePracticasCerradasNoCuentanComoPendientes() throws Exception {
        // La 17 (con observaciones) y la 18 (en autorización) quedaron así en inscripciones de 2024-2 ya aprobadas
        JsonNode p = panel("?periodo=2024-2");
        assertThat(p.get("pendientes")).isEmpty();
        assertThat(p.get("planificacionesEsperando")).isEmpty();
        assertThat(p.get("totales").get("planificacionesEnAutorizacion").asInt()).isZero();
        assertThat(p.get("totales").get("planificacionesConObservaciones").asInt()).isZero();
        assertThat(p.get("totales").get("evaluacionesFinalizadas").asInt()).isPositive();
    }

    @Test
    void reflejaLosCambiosAlMomento() throws Exception {
        // la guía de Diego aprueba la planificación 6: deja de esperarla a ella y pasa al profesor del curso
        enviar("/api/planificaciones/6/revision-guia", GUIA2, Map.of("decision", "APROBADA", "comentario", "Bien estructurada."))
                .andExpect(status().isOk());
        sincronizar();
        JsonNode seis = buscar(panel("?periodo=2026-2").get("pendientes"), "planificacionId", 6);
        assertThat(seis.get("rolQueDebeActuar").asText()).isEqualTo("PROFESOR_CURSO");
        assertThat(seis.get("diasEsperando").asLong()).isZero();
        assertThat(seis.get("requiereAtencion").asBoolean()).isFalse();
    }
}

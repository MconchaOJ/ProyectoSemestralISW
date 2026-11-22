package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HU-14: panel de monitoreo, contra la base real con los datos del seed (cada prueba se revierte). Trabajo
 * Colaborativo 2026-2: 5 inscritos en curso, la planificación 6 espera al guía y la 7 al profesor del curso, 3
 * evaluaciones finalizadas, y Camila (3) y Martina (34) sin evaluar. En toda la carrera esperan revisión 4
 * planificaciones (11, 9, 6 y 7, todas hace más de 3 días; la 18 quedó en autorización en la inscripción 48, ya
 * aprobada, y por eso no cuenta) y hay 7 estudiantes en curso sin evaluar.
 */
class HU14PanelIntegracionTest extends IntegracionBase {

    private static final String TUTOR1 = "tutor1@demo.example";

    private JsonNode panel(String filtros, String email) throws Exception {
        return cuerpo(consultar("/api/reportes/panel" + filtros, email).andExpect(status().isOk()));
    }

    @Test
    void elPanelMuestraPendientesEvaluacionesYSinEvaluarPorAsignatura() throws Exception {   // criterio 1
        JsonNode p = panel("", COORDINADOR);
        assertThat(p.get("diasAtencion").asInt()).isEqualTo(3);
        assertThat(p.get("planificacionesEsperando").findValuesAsText("planificacionId"))
                .containsExactly("11", "9", "6", "7");   // la que más espera primero (la 18 es de una práctica cerrada)
        assertThat(p.get("planificacionesEsperando").get(0).get("diasEsperando").asLong()).isGreaterThan(3);
        assertThat(p.get("sinEvaluar")).hasSize(7);
        assertThat(p.get("totales").get("estudiantesSinEvaluar").asInt()).isEqualTo(7);

        JsonNode tc = null;
        for (JsonNode f : p.get("filas")) {
            if (f.get("asignaturaId").asInt() == 3 && f.get("periodo").asText().equals("2026-2")) {
                tc = f;
            }
        }
        assertThat(tc).isNotNull();
        assertThat(tc.get("inscritos").asInt()).isEqualTo(5);
        assertThat(tc.get("enCurso").asInt()).isEqualTo(5);
        assertThat(tc.get("planificacionesEnRevisionGuia").asInt()).isEqualTo(1);
        assertThat(tc.get("planificacionesEnAutorizacion").asInt()).isEqualTo(1);
        assertThat(tc.get("evaluacionesFinalizadas").asInt()).isEqualTo(3);
        assertThat(tc.get("estudiantesSinEvaluar").asInt()).isEqualTo(2);
        // en una práctica sin colegio no hay guía que evalúe: no se cuentan "sin evaluar"
        for (JsonNode f : p.get("filas")) {
            if (f.get("asignaturaId").asInt() == 1) {
                assertThat(f.get("estudiantesSinEvaluar").isNull()).isTrue();
            }
        }
    }

    @Test
    void sePuedeFiltrarPorAsignaturaYPeriodo() throws Exception {   // criterio 2
        JsonNode p = panel("?asignaturaId=3&periodo=2026-2", JEFE_CARRERA);
        assertThat(p.get("filas")).hasSize(1);
        assertThat(p.get("planificacionesEsperando").findValuesAsText("planificacionId")).containsExactly("6", "7");
        assertThat(p.get("planificacionesEsperando").get(0).get("rolQueDebeActuar").asText()).isEqualTo("PROFESOR_GUIA");
        assertThat(p.get("planificacionesEsperando").get(0).get("esperaA").asText()).isEqualTo("Lorena Vidal Pizarro");
        assertThat(p.get("sinEvaluar").findValuesAsText("estudiante")).containsExactly("Martina Aravena Soto", "Camila Rojas Soto");

        assertThat(panel("?periodo=2026-1", COORDINADOR).get("filas").findValuesAsText("periodo")).containsOnly("2026-1");
        assertThat(panel("?asignaturaId=6", COORDINADOR).get("filas")).hasSize(1);
        consultar("/api/reportes/panel?periodo=2026", COORDINADOR).andExpect(status().isBadRequest());
    }

    @Test
    void unaPlanificacionRecienEnviadaNoRequiereAtencionHastaPasarElUmbral() throws Exception {   // S-79
        Map<String, Object> m = new HashMap<>();
        m.put("inscripcionId", 3);
        m.put("titulo", "Recién enviada");
        m.put("establecimiento", "Liceo Bicentenario Ejemplo");
        m.put("curso", "1° Medio B");
        m.put("unidad", "Álgebra y funciones");
        m.put("objetivoAprendizaje", "OA de prueba.");
        m.put("objetivoActitudinal", "Perseverancia.");
        m.put("objetivoClase", "Objetivo de prueba.");
        m.put("fechaClase", LocalDate.now().plusDays(10).toString());
        m.put("duracionMinutos", 90);
        m.put("recursos", "Pizarra.");
        m.put("instrumentosEvaluacion", "Ticket de salida.");
        m.put("inicio", "Inicio.");
        m.put("inicioMinutos", 10);
        m.put("desarrollo", "Desarrollo.");
        m.put("desarrolloMinutos", 60);
        m.put("cierre", "Cierre.");
        m.put("cierreMinutos", 20);
        long id = cuerpo(enviar("/api/planificaciones", CAMILA, m).andExpect(status().isCreated())).get("id").asLong();
        mvc.perform(multipart("/api/planificaciones/" + id + "/documentos", CAMILA)
                .file(archivo("archivo", "plan.pdf", "application/pdf", "%PDF-1.4 prueba".getBytes())))
                .andExpect(status().isCreated());
        enviar("/api/planificaciones/" + id + "/enviar", CAMILA).andExpect(status().isOk());
        sincronizar();

        JsonNode p = panel("?asignaturaId=3&periodo=2026-2", COORDINADOR);
        assertThat(p.get("filas").get(0).get("planificacionesEnRevisionGuia").asInt()).isEqualTo(2);   // se cuenta
        assertThat(p.get("planificacionesEsperando").findValuesAsText("planificacionId"))
                .doesNotContain(String.valueOf(id));   // pero lleva 0 días: todavía no requiere atención
    }

    @Test
    void soloElCoordinadorYElJefeDeCarreraVenElPanel() throws Exception {
        for (String email : List.of(CAMILA, GUIA1, CURSO1, TUTOR1)) {
            consultar("/api/reportes/panel", email).andExpect(status().isForbidden());
        }
        consultar("/api/reportes/panel", null).andExpect(status().isUnauthorized());
    }
}

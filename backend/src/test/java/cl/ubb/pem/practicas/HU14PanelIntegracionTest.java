package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.dto.PanelDto;
import cl.ubb.pem.practicas.dto.PanelDto.AsignaturaPanelDto;
import cl.ubb.pem.practicas.dto.PanelDto.PendienteDto;
import cl.ubb.pem.practicas.dto.PanelDto.SinEvaluarDto;
import cl.ubb.pem.practicas.service.PanelService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HU-14: panel de monitoreo del coordinador, contra la base real con los datos de db/seed.sql (cada prueba se
 * revierte). En 2026-2 hay 13 inscripciones, todas EN_CURSO; Trabajo Colaborativo (asignatura 3) tiene 5, con 3
 * evaluaciones finalizadas (4,70, 4,15 y 5,70), 2 borradores (Camila e inscripción 34) y las planificaciones 6
 * (EN_REVISION_GUIA), 7 (EN_AUTORIZACION) y 4 y 15 (CON_OBSERVACIONES). Solo Trabajo Colaborativo y Práctica
 * Profesional tienen pauta en el sistema.
 */
class HU14PanelIntegracionTest extends IntegracionBase {

    @Autowired
    private PanelService panel;

    // ---------- Criterio 1: planificaciones pendientes, evaluaciones hechas y estudiantes sin evaluar ----------

    @Test
    void elResumenDelPeriodoTraeLasTresCosasQuePideLaHistoria() {
        PanelDto p = panel.panel("2026-2", null);

        assertThat(p.resumen().inscripciones()).isEqualTo(13);
        assertThat(p.resumen().enCurso()).isEqualTo(13);
        assertThat(p.resumen().pendientesGuia()).isEqualTo(2);        // planificaciones 6 y 9
        assertThat(p.resumen().pendientesCurso()).isEqualTo(1);       // la 7
        assertThat(p.resumen().conObservaciones()).isEqualTo(2);      // la 4 y la 15
        assertThat(p.resumen().evaluacionesFinalizadas()).isEqualTo(3);
        assertThat(p.resumen().evaluacionesBorrador()).isEqualTo(2);
        assertThat(p.resumen().estudiantesSinEvaluar()).isEqualTo(3);
    }

    @Test
    void lasPendientesDicenAQuienEsperanYVanLasMasAntiguasPrimero() {
        List<PendienteDto> pendientes = panel.panel("2026-2", null).planificacionesPendientes();

        assertThat(pendientes).extracting(PendienteDto::planificacionId).containsExactly(4L, 15L, 6L, 7L, 9L);
        assertThat(pendientes.get(0).esperaA()).isEqualTo("Estudiante");
        PendienteDto seis = pendientes.get(2);
        assertThat(seis.estudiante()).isEqualTo("Diego Muñoz Araya");
        assertThat(seis.esperaA()).isEqualTo("Lorena Vidal Pizarro");                 // su guía
        assertThat(pendientes.get(3).esperaA()).isEqualTo("Ricardo Núñez Salas");    // su profesor del curso
        assertThat(pendientes.get(0).diasEsperando()).isGreaterThanOrEqualTo(seis.diasEsperando());
    }

    @Test
    void sinEvaluarSonLasEnCursoSinEvaluacionFinalizadaEnAsignaturasConPauta() {
        List<SinEvaluarDto> sinEvaluar = panel.panel("2026-2", null).sinEvaluar();

        // Camila (3, con un borrador), Sebastián en Práctica Profesional (17) y la inscripción 34 (con un borrador).
        // Valentina en Práctica Pedagógica (11) no entra: esa asignatura todavía no tiene pauta (S-81).
        assertThat(sinEvaluar).extracting(SinEvaluarDto::inscripcionId).containsExactlyInAnyOrder(3L, 17L, 34L);
        assertThat(sinEvaluar).filteredOn(s -> s.inscripcionId() == 3L).singleElement()
                .satisfies(s -> assertThat(s.borradores()).isEqualTo(1));
    }

    @Test
    void lasPlanificacionesDePracticasCerradasNoCuentanComoPendientes() {
        // Las planificaciones 17 y 18 quedaron sin resolver en inscripciones de 2024-2 ya APROBADAS
        PanelDto p = panel.panel("2024-2", null);

        assertThat(p.planificacionesPendientes()).isEmpty();
        assertThat(p.resumen().pendientesCurso()).isZero();
        assertThat(p.resumen().conObservaciones()).isZero();
        assertThat(p.resumen().evaluacionesFinalizadas()).isPositive();
    }

    @Test
    void cadaAsignaturaTieneSuFilaYCadaEstudianteSuAvance() {
        PanelDto p = panel.panel("2026-2", null);

        assertThat(p.asignaturas()).extracting(AsignaturaPanelDto::orden).containsExactly(1, 3, 5, 6);
        AsignaturaPanelDto tc = p.asignaturas().get(1);
        assertThat(tc.nombre()).isEqualTo("Trabajo Colaborativo en el Aula");
        assertThat(tc.inscripciones()).isEqualTo(5);
        assertThat(tc.autorizadas()).isEqualTo(3);
        assertThat(tc.evaluacionesFinalizadas()).isEqualTo(3);
        assertThat(tc.evaluacionesBorrador()).isEqualTo(2);
        assertThat(tc.sinEvaluar()).isEqualTo(2);
        assertThat(tc.promedioEvaluaciones()).isEqualByComparingTo("4.85");   // (4,70 + 4,15 + 5,70) / 3

        assertThat(p.avance()).hasSize(13);
        assertThat(p.avance()).filteredOn(a -> a.inscripcionId() == 6L).singleElement().satisfies(a -> {
            assertThat(a.estudiante()).isEqualTo("Diego Muñoz Araya");
            assertThat(a.planificaciones()).isEqualTo(2);
            assertThat(a.evaluacionesFinalizadas()).isEqualTo(1);
            assertThat(a.promedioEvaluaciones()).isEqualByComparingTo("4.70");
        });
    }

    // ---------- Criterio 2: filtrar por periodo y asignatura ----------

    @Test
    void seFiltraPorPeriodoYAsignatura() throws Exception {
        consultar("/api/panel?periodo=2026-2&asignaturaId=3", COORDINADOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("2026-2"))
                .andExpect(jsonPath("$.asignaturaId").value(3))
                .andExpect(jsonPath("$.resumen.inscripciones").value(5))
                .andExpect(jsonPath("$.resumen.estudiantesSinEvaluar").value(2))
                .andExpect(jsonPath("$.asignaturas.length()").value(1))
                .andExpect(jsonPath("$.planificacionesPendientes.length()").value(4));

        // Sin filtros: toda la carrera (las 2026-1 en curso también aportan pendientes: la 11 y la 13)
        PanelDto todo = panel.panel(null, null);
        assertThat(todo.resumen().inscripciones()).isEqualTo(55);
        assertThat(todo.resumen().enCurso()).isEqualTo(20);
        assertThat(todo.planificacionesPendientes()).hasSize(7);
        assertThat(panel.panel("  ", null).resumen().inscripciones()).isEqualTo(55);   // vacío = todos
        assertThat(panel.panel("2020-1", null).avance()).isEmpty();
    }

    @Test
    void unPeriodoMalEscritoDa400() throws Exception {
        consultar("/api/panel?periodo=2026-3", COORDINADOR)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
        consultar("/api/panel?asignaturaId=abc", COORDINADOR).andExpect(status().isBadRequest());
    }

    @Test
    void loVenElCoordinadorYElJefeDeCarreraYNadieMas() throws Exception {
        consultar("/api/panel", JEFE_CARRERA).andExpect(status().isOk());
        for (String email : List.of(GUIA1, CURSO1, CAMILA, "tutor1@demo.example")) {
            consultar("/api/panel", email).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/panel")).andExpect(status().isUnauthorized());
    }

    @Test
    void reflejaLosCambiosAlMomento() throws Exception {
        // El guía de Diego (Lorena Vidal) aprueba la planificación 6: deja de esperarlo a él y pasa al profesor del curso
        enviar("/api/planificaciones/6/revision-guia", GUIA2,
                Map.of("decision", "APROBADA", "comentario", "Bien estructurada."))
                .andExpect(status().isOk());
        sincronizar();

        PanelDto p = panel.panel("2026-2", null);
        assertThat(p.resumen().pendientesGuia()).isEqualTo(1);
        assertThat(p.resumen().pendientesCurso()).isEqualTo(2);
    }
}

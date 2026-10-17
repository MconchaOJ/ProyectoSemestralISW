package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.dto.CartolaFilaDto;
import cl.ubb.pem.practicas.service.CartolaPracticasService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * HU-31: cartola de prácticas y rol JEFE_CARRERA, contra la base real con los datos de db/seed.sql (cada prueba se
 * revierte). En el seed, las inscripciones cerradas tienen fecha de cierre deducida del periodo (S-48): 16 cierran
 * en 2025, y la 9 (Valentina Pérez, Trabajo Colaborativo 2025-2) tiene 2 evaluaciones finalizadas (4,80 y 5,90).
 */
class HU31CartolaPracticasIntegracionTest extends IntegracionBase {

    private static final String CARTOLA_2025 = "/api/reportes/cartola-practicas?desde=2025-01-01&hasta=2025-12-31";

    @Autowired
    private CartolaPracticasService cartola;

    @Test
    void jefeDeCarreraDescargaLaCartolaEnPdf() throws Exception {
        byte[] pdf = consultar(CARTOLA_2025, JEFE_CARRERA)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"cartola-practicas_2025-01-01_2025-12-31.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        PdfReader lector = new PdfReader(pdf);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(new PdfTextExtractor(lector).getTextFromPage(pagina));
        }
        assertThat(texto.toString()).contains("Cartola de prácticas", "Total: 16 inscripciones",
                "Valentina Pérez Cortés", "33333333-3", "5,35");
    }

    @Test
    void laCartolaTraeSoloLasInscripcionesCerradasEnElRangoConSuResumen() {
        List<CartolaFilaDto> filas = cartola.filas(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        assertThat(filas).hasSize(16);
        assertThat(filas).allSatisfy(f -> assertThat(f.fechaCierre()).hasYear(2025));
        CartolaFilaDto valentina = filas.stream().filter(f -> f.inscripcionId() == 9L).findFirst().orElseThrow();
        assertThat(valentina.estudiante()).isEqualTo("Valentina Pérez Cortés");
        assertThat(valentina.profesorGuia()).isEqualTo("Marcos Bravo Tapia");
        assertThat(valentina.profesorCurso()).isEqualTo("Ricardo Núñez Salas");
        assertThat(valentina.notaFinal()).isEqualByComparingTo("6.4");
        assertThat(valentina.evaluacionesFinalizadas()).isEqualTo(2);
        assertThat(valentina.promedioEvaluaciones()).isEqualByComparingTo("5.35");
    }

    @Test
    void lasInscripcionesEnCursoNoEntranYUnRangoSinCierresDaUnPdfSinFilas() throws Exception {
        assertThat(cartola.filas(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31))).isEmpty();
        consultar("/api/reportes/cartola-practicas?desde=2026-08-01&hasta=2026-12-31", JEFE_CARRERA)
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void cerrarUnaInscripcionGuardaLaFechaDelDiaYApareceEnLaCartola() throws Exception {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Santiago"));
        reemplazar("/api/inscripciones/3/cierre", COORDINADOR, Map.of("estado", "APROBADA", "notaFinal", 6.2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaCierre").value(hoy.toString()));

        assertThat(cartola.filas(hoy, hoy)).extracting(CartolaFilaDto::inscripcionId).contains(3L);
    }

    @Test
    void losOtrosCuatroRolesRecibenEl403() throws Exception {
        for (String email : List.of(COORDINADOR, GUIA1, CURSO1, CAMILA)) {
            consultar(CARTOLA_2025, email).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get(CARTOLA_2025)).andExpect(status().isUnauthorized());
    }

    @Test
    void fechasInvertidasOMalEscritasDan400() throws Exception {
        consultar("/api/reportes/cartola-practicas?desde=2025-12-31&hasta=2025-01-01", JEFE_CARRERA)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
        consultar("/api/reportes/cartola-practicas?desde=31-12-2025&hasta=2025-12-31", JEFE_CARRERA)
                .andExpect(status().isBadRequest());
        consultar("/api/reportes/cartola-practicas?desde=2025-01-01", JEFE_CARRERA)
                .andExpect(status().isBadRequest());
    }

    // El 2026-10-11 el cliente amplió el rol: ahora lee todo como el coordinador (RolSecundarioIntegracionTest).
    // Lo que sigue sin poder hacer es administrar: roles, profesores, ni cerrar una inscripción.
    @Test
    void elJefeDeCarreraNoAdministraNada() throws Exception {
        for (String url : List.of("/api/roles", "/api/profesores")) {
            consultar(url, JEFE_CARRERA).andExpect(status().isForbidden());
        }
        reemplazar("/api/inscripciones/3/cierre", JEFE_CARRERA, Map.of("estado", "APROBADA", "notaFinal", 6.2))
                .andExpect(status().isForbidden());
    }
}

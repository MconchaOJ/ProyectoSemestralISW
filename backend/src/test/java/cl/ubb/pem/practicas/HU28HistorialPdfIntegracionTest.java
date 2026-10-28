package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * HU-28: historial del estudiante en PDF, contra la base real con los datos de db/seed.sql. Valentina Pérez
 * (estudiante 3) tiene 5 inscripciones; en la 9 (Trabajo Colaborativo 2025-2, guía Marcos Bravo) hay 2 evaluaciones
 * finalizadas (4,80 y 5,90) y la planificación "Razones y proporciones" AUTORIZADA.
 */
class HU28HistorialPdfIntegracionTest extends IntegracionBase {

    @Test
    void elPdfTraeLasMismasInscripcionesEvaluacionesYPlanificacionesQueLaPantalla() throws Exception {
        String texto = textoPdf(consultar("/api/estudiantes/3/historial/pdf", COORDINADOR)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"historial-estudiante-3.pdf\""))
                .andReturn().getResponse().getContentAsByteArray());

        assertThat(texto).contains("Historial de prácticas", "Valentina Pérez Cortés", "33333333-3",
                "Aproximación al Sistema Escolar", "Ambientes de Aprendizaje", "Trabajo Colaborativo en el Aula",
                "Investigación en el Aula", "Práctica Pedagógica",          // las 5 inscripciones
                "4,8", "5,9",                                                 // sus evaluaciones finalizadas
                "Razones y proporciones", "AUTORIZADA");                      // y sus planificaciones
    }

    @Test
    void incluyeLaFechaDeGeneracion() throws Exception {
        String hoy = LocalDate.now(ZoneId.of("America/Santiago")).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String texto = textoPdf(consultar("/api/estudiantes/3/historial/pdf", GUIA1)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());

        assertThat(texto).contains("Generado el " + hoy);
    }

    @Test
    void elEstudianteDescargaElSuyo() throws Exception {
        String texto = textoPdf(consultar("/api/estudiantes/me/historial/pdf", CAMILA)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"mi-historial.pdf\""))
                .andReturn().getResponse().getContentAsByteArray());

        assertThat(texto).contains("Camila Rojas");
        consultar("/api/estudiantes/1/historial/pdf", CAMILA).andExpect(status().isOk());
    }

    @Test
    void respetaLasMismasReglasDeAccesoQueVerElHistorial() throws Exception {
        consultar("/api/estudiantes/2/historial/pdf", CAMILA).andExpect(status().isForbidden());   // otro estudiante
        consultar("/api/estudiantes/2/historial/pdf", GUIA1).andExpect(status().isForbidden());    // Diego es de Lorena
        consultar("/api/estudiantes/2/historial/pdf", GUIA2).andExpect(status().isOk());
        consultar("/api/estudiantes/1/historial/pdf", CURSO1).andExpect(status().isOk());
        consultar("/api/estudiantes/4/historial/pdf", "tutor1@demo.example").andExpect(status().isOk());
        consultar("/api/estudiantes/1/historial/pdf", JEFE_CARRERA).andExpect(status().isOk());
        consultar("/api/estudiantes/me/historial/pdf", COORDINADOR).andExpect(status().isForbidden());
        consultar("/api/estudiantes/999/historial/pdf", COORDINADOR).andExpect(status().isNotFound());
        mvc.perform(get("/api/estudiantes/1/historial/pdf")).andExpect(status().isUnauthorized());
    }

    @Test
    void unEstudianteSinEvaluacionesNiPlanificacionesTambienTieneSuPdf() throws Exception {
        // Javiera (estudiante 5): solo Aproximación 2026-2, que no entrega planificaciones
        String texto = textoPdf(consultar("/api/estudiantes/5/historial/pdf", COORDINADOR)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());

        assertThat(texto).contains("Esta práctica no entrega planificaciones.", "Prácticas aprobadas: 0 de 6");
    }

    private static String textoPdf(byte[] pdf) throws Exception {
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        PdfReader lector = new PdfReader(pdf);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(new PdfTextExtractor(lector).getTextFromPage(pagina)).append('\n');
        }
        return texto.toString();
    }
}

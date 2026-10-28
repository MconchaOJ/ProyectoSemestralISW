package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.dto.InformeAsignaturaDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.PautaUsadaDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.TramoDto;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.service.InformeService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * HU-08: informes por estudiante y por asignatura, contra la base real con los datos de db/seed.sql (cada prueba se
 * revierte). En el seed, Trabajo Colaborativo (asignatura 3) en 2025-2 tiene una sola inscripción: la 9 de Valentina
 * Pérez, APROBADA con 6,4, con 2 evaluaciones finalizadas de la pauta 1 (96 y 118 de 140 puntos; promedios 4,80 y
 * 5,90) y una planificación AUTORIZADA.
 */
class HU08InformesIntegracionTest extends IntegracionBase {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private InformeService informes;

    // ---------- Criterio 2: informe por asignatura y periodo ----------

    @Test
    void elInformePorAsignaturaTraeElResumenLosPromediosYLaDistribucionDePuntajes() {
        InformeAsignaturaDto informe = informes.datosAsignatura(3, "2025-2");

        assertThat(informe.asignatura().nombre()).isEqualTo("Trabajo Colaborativo en el Aula");
        assertThat(informe.resumen().inscripciones()).isEqualTo(1);
        assertThat(informe.resumen().aprobadas()).isEqualTo(1);
        assertThat(informe.resumen().promedioNotaFinal()).isEqualByComparingTo("6.4");
        assertThat(informe.resumen().evaluacionesFinalizadas()).isEqualTo(2);
        assertThat(informe.resumen().promedioEvaluaciones()).isEqualByComparingTo("5.35");
        assertThat(informe.resumen().planificacionesAutorizadas()).isEqualTo(1);
        assertThat(informe.distribucionNotasFinales()).extracting(TramoDto::cantidad).containsExactly(0, 0, 0, 1);

        PautaUsadaDto pauta = informe.pautas().get(0);
        assertThat(informe.pautas()).hasSize(1);
        assertThat(pauta.puntajeMaximo()).isEqualTo(140);
        assertThat(pauta.evaluaciones()).isEqualTo(2);
        assertThat(pauta.puntajeMinimo()).isEqualTo(96);
        assertThat(pauta.puntajeMasAlto()).isEqualTo(118);
        assertThat(pauta.puntajePromedio()).isEqualByComparingTo("107");
        // 96/140 = 68,6 % (tramo 60-69) y 118/140 = 84,3 % (tramo 80-89)
        assertThat(pauta.distribucion()).extracting(TramoDto::cantidad).containsExactly(0, 1, 0, 1, 0);

        assertThat(informe.estudiantes()).singleElement().satisfies(f -> {
            assertThat(f.estudiante()).isEqualTo("Valentina Pérez Cortés");
            assertThat(f.profesorGuia()).isEqualTo("Marcos Bravo Tapia");
            assertThat(f.estado()).isEqualTo(EstadoInscripcion.APROBADA);
            assertThat(f.evaluacionesFinalizadas()).isEqualTo(2);
        });
    }

    @Test
    void losBorradoresNoCuentanYLasEnCursoNoTienenNota() {
        // Trabajo Colaborativo 2026-2: 5 inscripciones EN_CURSO; la evaluación 4 (Camila) sigue en BORRADOR
        InformeAsignaturaDto informe = informes.datosAsignatura(3, "2026-2");

        assertThat(informe.resumen().inscripciones()).isEqualTo(5);
        assertThat(informe.resumen().enCurso()).isEqualTo(5);
        assertThat(informe.resumen().promedioNotaFinal()).isNull();
        assertThat(informe.distribucionNotasFinales()).extracting(TramoDto::cantidad).containsOnly(0);
        assertThat(informe.estudiantes()).filteredOn(f -> f.inscripcionId() == 3L).singleElement()
                .satisfies(f -> assertThat(f.evaluacionesFinalizadas()).isZero());
    }

    @Test
    void elCoordinadorDescargaElInformePorAsignaturaEnPdf() throws Exception {
        byte[] pdf = consultar("/api/reportes/asignaturas/3?periodo=2025-2", COORDINADOR)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"informe-PEM-P3_2025-2.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(textoPdf(pdf)).contains("Informe por asignatura", "Trabajo Colaborativo en el Aula",
                "Periodo 2025-2", "Valentina Pérez Cortés", "5,35", "Generado el");
    }

    @Test
    void elJefeDeCarreraDescargaElInformePorAsignaturaEnExcel() throws Exception {
        byte[] xlsx = consultar("/api/reportes/asignaturas/3?periodo=2025-2&formato=xlsx", JEFE_CARRERA)
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"informe-PEM-P3_2025-2.xlsx\""))
                .andReturn().getResponse().getContentAsByteArray();

        Map<String, String> partes = descomprimir(xlsx);
        assertThat(partes).containsKeys("[Content_Types].xml", "xl/workbook.xml", "xl/styles.xml",
                "xl/worksheets/sheet1.xml", "xl/worksheets/sheet4.xml");
        assertThat(partes.get("xl/workbook.xml")).contains("Resumen", "Distribución", "Pautas", "Estudiantes");
        assertThat(partes.get("xl/worksheets/sheet4.xml")).contains("Valentina Pérez Cortés", "<v>5.35</v>");
    }

    @Test
    void unPeriodoSinInscripcionesDaUnInformeVacio() throws Exception {
        assertThat(informes.datosAsignatura(6, "2020-1").estudiantes()).isEmpty();
        consultar("/api/reportes/asignaturas/6?periodo=2020-1", COORDINADOR)
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void periodoFormatoOAsignaturaInvalidosDan400O404() throws Exception {
        consultar("/api/reportes/asignaturas/3?periodo=2025-3", COORDINADOR)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
        consultar("/api/reportes/asignaturas/3?periodo=2025-2&formato=docx", COORDINADOR)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
        consultar("/api/reportes/asignaturas/3", COORDINADOR).andExpect(status().isBadRequest());
        consultar("/api/reportes/asignaturas/99?periodo=2025-2", COORDINADOR).andExpect(status().isNotFound());
    }

    // ---------- Criterio 1: informe por estudiante ----------

    @Test
    void elInformePorEstudianteEnPdfTraeTodasSusPracticasConSuSintesis() throws Exception {
        byte[] pdf = consultar("/api/reportes/estudiantes/4", COORDINADOR)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"informe-estudiante-4.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        // Sebastián Vargas: las 6 prácticas, desde Aproximación (2023-2) hasta Práctica Profesional (2026-2)
        assertThat(textoPdf(pdf)).contains("Historial de prácticas", "44444444-4", "Aproximación al Sistema Escolar",
                "Práctica Profesional", "Síntesis:", "Prácticas aprobadas: 5 de 6");
    }

    @Test
    void elInformePorEstudianteEnExcelTraeUnaHojaPorTema() throws Exception {
        byte[] xlsx = consultar("/api/reportes/estudiantes/3?formato=XLSX", JEFE_CARRERA)
                .andExpect(status().isOk()).andExpect(content().contentType(XLSX))
                .andReturn().getResponse().getContentAsByteArray();

        Map<String, String> partes = descomprimir(xlsx);
        assertThat(partes.get("xl/workbook.xml")).contains("Estudiante", "Prácticas", "Evaluaciones", "Planificaciones");
        assertThat(partes.get("xl/worksheets/sheet2.xml")).contains("Trabajo Colaborativo en el Aula", "<v>5.35</v>");
        assertThat(partes.get("xl/worksheets/sheet4.xml")).contains("Razones y proporciones", "AUTORIZADA");
    }

    @Test
    void unEstudianteQueNoExisteDa404() throws Exception {
        consultar("/api/reportes/estudiantes/999", COORDINADOR).andExpect(status().isNotFound());
    }

    // ---------- Acceso: solo coordinador y jefe de carrera ----------

    @Test
    void losDemasRolesRecibenEl403YSinTokenEl401() throws Exception {
        for (String email : List.of(GUIA1, CURSO1, CAMILA, "tutor1@demo.example")) {
            consultar("/api/reportes/asignaturas/3?periodo=2025-2", email).andExpect(status().isForbidden());
            consultar("/api/reportes/estudiantes/1", email).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/reportes/asignaturas/3?periodo=2025-2")).andExpect(status().isUnauthorized());
    }

    private static String textoPdf(byte[] pdf) throws IOException {
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        PdfReader lector = new PdfReader(pdf);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(new PdfTextExtractor(lector).getTextFromPage(pagina)).append('\n');
        }
        return texto.toString();
    }

    /** Cada archivo del .xlsx (un ZIP) con su contenido como texto. */
    private static Map<String, String> descomprimir(byte[] xlsx) throws IOException {
        Map<String, String> partes = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx), StandardCharsets.UTF_8)) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                partes.put(entrada.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return partes;
    }
}

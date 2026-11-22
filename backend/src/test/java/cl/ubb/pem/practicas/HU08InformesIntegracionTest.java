package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * HU-08 (informes) y HU-28 (historial en PDF), contra la base real con los datos exactos del seed (cada prueba se
 * revierte). Trabajo Colaborativo en el Aula (asignatura 3) 2024-2: 4 inscripciones aprobadas (notas 6,1 · 6,3 · 5,9 ·
 * 6,0), 4 evaluaciones finalizadas con la pauta 1 (puntajes 96, 113, 76 y 85 de 140; promedios 4,80 · 5,65 · 3,80 ·
 * 4,25) y 4 planificaciones (2 autorizadas, 1 con observaciones, 1 en autorización; solo la 17 fue devuelta).
 */
class HU08InformesIntegracionTest extends IntegracionBase {

    private static final String TUTOR1 = "tutor1@demo.example";
    private static final String INFORME = "/api/reportes/asignatura?asignaturaId=3&periodo=2024-2";

    /** Texto de un PDF descargado (todas sus páginas), para comprobar lo que dice. */
    private static String textoPdf(byte[] pdf) throws Exception {
        PdfReader lector = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder s = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                s.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return s.toString();
        } finally {
            lector.close();
        }
    }

    private byte[] pdf(String url, String email) throws Exception {
        byte[] contenido = consultar(url, email).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(contenido, 0, 5)).isEqualTo("%PDF-");
        return contenido;
    }

    // ---------- HU-08, criterio 2: informe por asignatura y período ----------

    @Test
    void elInformeDeUnaAsignaturaTraePromediosYDistribucion() throws Exception {
        JsonNode inf = cuerpo(consultar(INFORME, COORDINADOR).andExpect(status().isOk()));
        assertThat(inf.get("asignatura").asText()).isEqualTo("Trabajo Colaborativo en el Aula");
        assertThat(inf.get("inscripciones").get("total").asInt()).isEqualTo(4);
        assertThat(inf.get("inscripciones").get("aprobadas").asInt()).isEqualTo(4);

        JsonNode notas = inf.get("notasFinales");
        assertThat(notas.get("promedio").decimalValue()).isEqualByComparingTo("6.08");   // (6,1 + 6,3 + 5,9 + 6,0) / 4
        assertThat(notas.get("minimo").decimalValue()).isEqualByComparingTo("5.9");
        assertThat(notas.get("maximo").decimalValue()).isEqualByComparingTo("6.3");
        assertThat(cantidades(notas.get("tramos"))).containsExactly(0, 0, 1, 3);

        assertThat(inf.get("evaluaciones")).hasSize(1);   // todas con la pauta 1
        JsonNode p = inf.get("evaluaciones").get(0);
        assertThat(p.get("pautaId").asInt()).isEqualTo(1);
        assertThat(p.get("evaluaciones").asInt()).isEqualTo(4);
        assertThat(p.get("puntajeMaximo").asInt()).isEqualTo(140);                          // 20 indicadores x 7
        assertThat(p.get("puntajePromedio").decimalValue()).isEqualByComparingTo("92.50");   // (96 + 113 + 76 + 85) / 4
        assertThat(p.get("porcentajeLogro").decimalValue()).isEqualByComparingTo("66.1");
        assertThat(p.get("promedios").get("promedio").decimalValue()).isEqualByComparingTo("4.63");
        assertThat(cantidades(p.get("promedios").get("tramos"))).containsExactly(1, 2, 1, 0);
        assertThat(p.get("dimensiones")).hasSize(3);
        assertThat(p.get("dimensiones").get(2).get("promedioPorIndicador").decimalValue()).isEqualByComparingTo("5.03");

        JsonNode pl = inf.get("planificaciones");
        assertThat(pl.get("total").asInt()).isEqualTo(4);
        assertThat(pl.get("autorizadas").asInt()).isEqualTo(2);
        assertThat(pl.get("conObservaciones").asInt()).isEqualTo(1);
        assertThat(pl.get("enAutorizacion").asInt()).isEqualTo(1);
        assertThat(pl.get("devueltasAlMenosUnaVez").asInt()).isEqualTo(1);

        // una fila por estudiante, por apellido
        assertThat(inf.get("estudiantes").findValuesAsText("estudiante"))
                .containsExactly("Cárdenas Rojo, Trinidad", "Navarro Iturra, Agustín", "Pizarro Vega, Joaquín", "Vargas Leiva, Sebastián");
    }

    @Test
    void unPeriodoSinInscripcionesDaUnInformeVacioYLosErroresSonClaros() throws Exception {
        JsonNode vacio = cuerpo(consultar("/api/reportes/asignatura?asignaturaId=6&periodo=2023-1", COORDINADOR)
                .andExpect(status().isOk()));
        assertThat(vacio.get("inscripciones").get("total").asInt()).isZero();
        assertThat(vacio.get("notasFinales").get("promedio").isNull()).isTrue();
        assertThat(vacio.get("evaluaciones")).isEmpty();
        pdf("/api/reportes/asignatura/pdf?asignaturaId=6&periodo=2023-1", COORDINADOR);   // el PDF vacío también sale

        consultar("/api/reportes/asignatura?asignaturaId=3&periodo=2024", COORDINADOR).andExpect(status().isBadRequest());
        consultar("/api/reportes/asignatura?asignaturaId=99&periodo=2024-2", COORDINADOR).andExpect(status().isNotFound());
    }

    @Test
    void soloElCoordinadorYElJefeDeCarreraVenElInformePorAsignatura() throws Exception {
        consultar(INFORME, JEFE_CARRERA).andExpect(status().isOk());   // tiene PROFESOR_GUIA como segundo rol: entra por JEFE_CARRERA
        for (String email : List.of(CAMILA, GUIA1, CURSO1, TUTOR1)) {
            consultar(INFORME, email).andExpect(status().isForbidden());
            consultar("/api/reportes/asignatura/pdf?asignaturaId=3&periodo=2024-2", email).andExpect(status().isForbidden());
        }
        consultar(INFORME, null).andExpect(status().isUnauthorized());
    }

    @Test
    void elInformePorAsignaturaSeDescargaEnPdf() throws Exception {   // criterio 3
        String texto = textoPdf(pdf("/api/reportes/asignatura/pdf?asignaturaId=3&periodo=2024-2", JEFE_CARRERA));
        assertThat(texto).contains("Informe de Trabajo Colaborativo en el Aula", "2024-2", "Generado el",
                "Notas finales", "Evaluaciones finalizadas por pauta", "92,50 de 140", "Vargas Leiva, Sebastián");
    }

    // ---------- HU-28 y HU-08, criterio 1: el historial (informe por estudiante) en PDF ----------

    @Test
    void elHistorialEnPdfTraeTodasLasPracticasYLaFechaDeGeneracion() throws Exception {
        String texto = textoPdf(pdf("/api/estudiantes/4/historial/pdf", COORDINADOR));   // Sebastián Vargas
        assertThat(texto).contains("Historial de prácticas", "Sebastián Vargas Leiva", "Generado el",
                "Trabajo Colaborativo en el Aula", "2024-2", "Práctica Profesional", "2026-2",
                "Ecuaciones lineales con una incógnita");   // su planificación 1
        // el estudiante descarga el suyo
        assertThat(textoPdf(pdf("/api/estudiantes/me/historial/pdf", CAMILA))).contains("Camila Rojas Soto");
    }

    @Test
    void elHistorialEnPdfTieneLasMismasReglasDeAccesoQueElHistorial() throws Exception {   // HU-28, criterio 2
        pdf("/api/estudiantes/1/historial/pdf", CAMILA);
        pdf("/api/estudiantes/1/historial/pdf", GUIA1);          // su guía
        pdf("/api/estudiantes/4/historial/pdf", JEFE_CARRERA);
        pdf("/api/estudiantes/4/historial/pdf", TUTOR1);         // tutor de la inscripción 17 de Sebastián
        consultar("/api/estudiantes/2/historial/pdf", CAMILA).andExpect(status().isForbidden());
        consultar("/api/estudiantes/2/historial/pdf", GUIA1).andExpect(status().isForbidden());   // Diego es de Lorena
        consultar("/api/estudiantes/1/historial/pdf", TUTOR1).andExpect(status().isForbidden());
        consultar("/api/estudiantes/me/historial/pdf", COORDINADOR).andExpect(status().isForbidden());
        consultar("/api/estudiantes/999/historial/pdf", COORDINADOR).andExpect(status().isNotFound());
    }

    private static List<Integer> cantidades(JsonNode tramos) {
        List<Integer> c = new ArrayList<>();
        tramos.forEach(t -> c.add(t.get("cantidad").asInt()));
        return c;
    }
}

package cl.ubb.pem.practicas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.ubb.pem.practicas.service.InformesXlsx;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-08: los informes en Excel (integrados del trabajo de Alej, LibroXlsx): por asignatura y período
 * (/api/reportes/asignatura/xlsx, coordinador y jefe de carrera) y por estudiante (/api/estudiantes/{id|me}/historial/xlsx,
 * mismas reglas de acceso que el historial). Un .xlsx es un ZIP: se lee su XML para comprobar hojas y contenido.
 */
class HU08InformesExcelIntegracionTest extends IntegracionBase {

    private static final String TUTOR1 = "tutor1@demo.example";

    /** Las partes del .xlsx (ruta dentro del ZIP -> XML). */
    private Map<String, String> libro(ResultActions r) throws Exception {
        byte[] bytes = r.andExpect(status().isOk())
                .andExpect(header().string("Content-Type", InformesXlsx.TIPO_MIME))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 2, StandardCharsets.US_ASCII)).isEqualTo("PK");
        Map<String, String> partes = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                partes.put(e.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return partes;
    }

    @Test
    void elInformeDeUnaAsignaturaSeDescargaEnExcelConSusCuatroHojas() throws Exception {
        Map<String, String> x = libro(consultar("/api/reportes/asignatura/xlsx?asignaturaId=3&periodo=2024-2", COORDINADOR)
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx"))));
        assertThat(x.get("xl/workbook.xml")).contains("Resumen", "Distribución", "Pautas", "Estudiantes");
        String todo = String.join("\n", x.values());
        assertThat(todo).contains("Trabajo Colaborativo en el Aula", "2024-2", "44444444-4");   // RUT de Sebastián
        libro(consultar("/api/reportes/asignatura/xlsx?asignaturaId=3&periodo=2024-2", JEFE_CARRERA));
    }

    @Test
    void elExcelDeAsignaturaTieneLasMismasReglasQueElPdf() throws Exception {
        for (String email : List.of(GUIA1, CURSO1, CAMILA, TUTOR1)) {
            consultar("/api/reportes/asignatura/xlsx?asignaturaId=3&periodo=2024-2", email).andExpect(status().isForbidden());
        }
        consultar("/api/reportes/asignatura/xlsx?asignaturaId=3&periodo=2024-3", COORDINADOR).andExpect(status().isBadRequest());
        consultar("/api/reportes/asignatura/xlsx?asignaturaId=99&periodo=2024-2", COORDINADOR).andExpect(status().isNotFound());
    }

    @Test
    void elInformePorEstudianteEnExcelTraeSusPracticasEvaluacionesYPlanificaciones() throws Exception {
        Map<String, String> x = libro(consultar("/api/estudiantes/4/historial/xlsx", COORDINADOR));
        assertThat(x.get("xl/workbook.xml")).contains("Estudiante", "Prácticas", "Evaluaciones", "Planificaciones");
        assertThat(String.join("\n", x.values())).contains("Sebastián Vargas", "44444444-4", "Práctica Profesional");
        // el estudiante descarga el suyo
        assertThat(String.join("\n", libro(consultar("/api/estudiantes/me/historial/xlsx", CAMILA)).values()))
                .contains("Camila Rojas");
    }

    @Test
    void elExcelPorEstudianteRespetaLasReglasDelHistorial() throws Exception {
        consultar("/api/estudiantes/2/historial/xlsx", CAMILA).andExpect(status().isForbidden());   // otro estudiante
        consultar("/api/estudiantes/2/historial/xlsx", GUIA1).andExpect(status().isForbidden());    // Diego es de Lorena
        consultar("/api/estudiantes/2/historial/xlsx", GUIA2).andExpect(status().isOk());
        consultar("/api/estudiantes/1/historial/xlsx", CURSO1).andExpect(status().isOk());
        consultar("/api/estudiantes/4/historial/xlsx", TUTOR1).andExpect(status().isOk());
        consultar("/api/estudiantes/1/historial/xlsx", JEFE_CARRERA).andExpect(status().isOk());
        consultar("/api/estudiantes/me/historial/xlsx", COORDINADOR).andExpect(status().isForbidden());
        consultar("/api/estudiantes/999/historial/xlsx", COORDINADOR).andExpect(status().isNotFound());
    }
}

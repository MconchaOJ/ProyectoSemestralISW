package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/** HU-08: el Excel de los informes se escribe a mano (un ZIP con XML), sin librerías. */
class LibroXlsxTest {

    @Test
    void escribeLasPartesDeUnXlsxConNumerosComoNumerosYTextoEscapado() throws IOException {
        byte[] libro = new LibroXlsx()
                .hoja("Notas", List.of(List.of("Estudiante", "Nota", "Fecha"),
                        Arrays.asList("Ana <\"Pérez\"> & Cía", new BigDecimal("6.40"), LocalDate.of(2025, 12, 31)),
                        Arrays.asList("Sin nota", null, null)))
                .hoja("Otra", List.of(List.of("A"), List.of(7)))
                .generar();

        Map<String, String> partes = descomprimir(libro);
        assertThat(partes).containsOnlyKeys("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml");
        String hoja = partes.get("xl/worksheets/sheet1.xml");
        assertThat(hoja).contains("<c r=\"A1\" s=\"1\" t=\"inlineStr\">")                // títulos en negrita
                .contains("Ana &lt;&quot;Pérez&quot;&gt; &amp; Cía")
                .contains("<c r=\"B2\"><v>6.40</v></c>")                                  // número, no texto
                .contains("31-12-2025")
                .doesNotContain("r=\"B3\"");                                             // nulo = celda vacía
        assertThat(partes.get("xl/worksheets/sheet2.xml")).contains("<c r=\"A2\"><v>7</v></c>");
        assertThat(partes.get("xl/workbook.xml")).contains("name=\"Notas\"", "name=\"Otra\"");
    }

    @Test
    void losNombresDeHojaRespetanLasReglasDeExcel() throws IOException {
        byte[] libro = new LibroXlsx().hoja("Pautas: 1/2 [borrador] con un nombre muy largo", List.of(List.of("x")))
                .generar();

        String nombre = descomprimir(libro).get("xl/workbook.xml").replaceAll("(?s).*name=\"([^\"]*)\".*", "$1");
        assertThat(nombre).hasSizeLessThanOrEqualTo(31).doesNotContain(":", "/", "[", "]");
    }

    @Test
    void nombraLasColumnasComoExcel() {
        assertThat(LibroXlsx.columna(0)).isEqualTo("A");
        assertThat(LibroXlsx.columna(25)).isEqualTo("Z");
        assertThat(LibroXlsx.columna(26)).isEqualTo("AA");
        assertThat(LibroXlsx.columna(701)).isEqualTo("ZZ");
        assertThat(LibroXlsx.columna(702)).isEqualTo("AAA");
    }

    @Test
    void unLibroSinHojasNoSeGenera() {
        assertThatThrownBy(() -> new LibroXlsx().generar()).isInstanceOf(IllegalStateException.class);
    }

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

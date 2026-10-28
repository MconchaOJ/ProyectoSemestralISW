package cl.ubb.pem.practicas.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Escribe un libro de Excel (.xlsx) sencillo para los informes (HU-08): una o más hojas, la primera fila de cada una
 * en negrita como títulos de columna, números como números y el resto como texto. Un .xlsx es un ZIP con XML
 * (SpreadsheetML), así que basta con java.util.zip: sin librerías nuevas a propósito, como TextoDocx al leer Word.
 * No hay fórmulas, colores ni anchos de columna: Excel, LibreOffice y Google Sheets lo abren tal cual.
 */
final class LibroXlsx {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private record Hoja(String nombre, List<List<Object>> filas) {
    }

    private final List<Hoja> hojas = new ArrayList<>();

    /**
     * Agrega una hoja. {@code filas.get(0)} son los títulos de columna. Las celdas nulas quedan vacías; los números
     * (Integer, Long, BigDecimal...) se guardan como números y las fechas como texto dd-MM-yyyy.
     */
    LibroXlsx hoja(String nombre, List<List<Object>> filas) {
        hojas.add(new Hoja(nombreValido(nombre), filas));
        return this;
    }

    byte[] generar() {
        if (hojas.isEmpty()) {
            throw new IllegalStateException("Un libro de Excel necesita al menos una hoja.");
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida, StandardCharsets.UTF_8)) {
            escribir(zip, "[Content_Types].xml", tiposDeContenido());
            escribir(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>\
                    </Relationships>""");
            escribir(zip, "xl/workbook.xml", libro());
            escribir(zip, "xl/_rels/workbook.xml.rels", relacionesDelLibro());
            escribir(zip, "xl/styles.xml", ESTILOS);
            for (int n = 0; n < hojas.size(); n++) {
                escribir(zip, "xl/worksheets/sheet" + (n + 1) + ".xml", hojaXml(hojas.get(n)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel.", e);
        }
        return salida.toByteArray();
    }

    private static void escribir(ZipOutputStream zip, String ruta, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(ruta));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String tiposDeContenido() {
        StringBuilder x = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">\
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>\
                <Default Extension="xml" ContentType="application/xml"/>\
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>\
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""");
        for (int n = 1; n <= hojas.size(); n++) {
            x.append("<Override PartName=\"/xl/worksheets/sheet").append(n)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return x.append("</Types>").toString();
    }

    private String libro() {
        StringBuilder x = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" \
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""");
        for (int n = 1; n <= hojas.size(); n++) {
            x.append("<sheet name=\"").append(escapar(hojas.get(n - 1).nombre())).append("\" sheetId=\"").append(n)
                    .append("\" r:id=\"rId").append(n).append("\"/>");
        }
        return x.append("</sheets></workbook>").toString();
    }

    private String relacionesDelLibro() {
        StringBuilder x = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""");
        for (int n = 1; n <= hojas.size(); n++) {
            x.append("<Relationship Id=\"rId").append(n)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(n).append(".xml\"/>");
        }
        int estilos = hojas.size() + 1;
        x.append("<Relationship Id=\"rId").append(estilos)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return x.append("</Relationships>").toString();
    }

    /** Estilo 0 = normal, estilo 1 = negrita (para los títulos de columna). */
    private static final String ESTILOS = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">\
            <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>\
            <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>\
            <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>\
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>\
            <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>\
            <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>\
            </styleSheet>""";

    private static String hojaXml(Hoja hoja) {
        StringBuilder x = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""");
        for (int f = 0; f < hoja.filas().size(); f++) {
            x.append("<row r=\"").append(f + 1).append("\">");
            List<Object> fila = hoja.filas().get(f);
            for (int c = 0; c < fila.size(); c++) {
                celda(x, columna(c) + (f + 1), fila.get(c), f == 0);
            }
            x.append("</row>");
        }
        return x.append("</sheetData></worksheet>").toString();
    }

    private static void celda(StringBuilder x, String ref, Object valor, boolean titulo) {
        if (valor == null) {
            return;
        }
        String estilo = titulo ? " s=\"1\"" : "";
        if (valor instanceof Number numero) {
            String texto = numero instanceof BigDecimal bd ? bd.toPlainString() : numero.toString();
            x.append("<c r=\"").append(ref).append('"').append(estilo).append("><v>").append(texto).append("</v></c>");
            return;
        }
        String texto;
        if (valor instanceof LocalDate d) {
            texto = d.format(FECHA);
        } else if (valor instanceof LocalDateTime d) {
            texto = d.format(FECHA_HORA);
        } else {
            texto = valor.toString();
        }
        x.append("<c r=\"").append(ref).append('"').append(estilo).append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                .append(escapar(texto)).append("</t></is></c>");
    }

    /** 0 → A, 25 → Z, 26 → AA... */
    static String columna(int indice) {
        StringBuilder nombre = new StringBuilder();
        for (int n = indice + 1; n > 0; n = (n - 1) / 26) {
            nombre.insert(0, (char) ('A' + (n - 1) % 26));
        }
        return nombre.toString();
    }

    /** Excel exige nombres de hoja de 1 a 31 caracteres y sin : \ / ? * [ ]. */
    private static String nombreValido(String nombre) {
        String limpio = nombre == null ? "" : nombre.replaceAll("[:\\\\/?*\\[\\]]", " ").strip();
        if (limpio.isEmpty()) {
            limpio = "Hoja";
        }
        return limpio.length() > 31 ? limpio.substring(0, 31) : limpio;
    }

    /** Escapa lo que rompe el XML y quita los caracteres de control que XML 1.0 no admite. */
    private static String escapar(String texto) {
        StringBuilder x = new StringBuilder(texto.length());
        for (char ch : texto.toCharArray()) {
            switch (ch) {
                case '&' -> x.append("&amp;");
                case '<' -> x.append("&lt;");
                case '>' -> x.append("&gt;");
                case '"' -> x.append("&quot;");
                default -> {
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        x.append(ch);
                    }
                }
            }
        }
        return x.toString();
    }
}

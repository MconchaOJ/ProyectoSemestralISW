package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.ubb.pem.practicas.Datos;
import cl.ubb.pem.practicas.dto.CartolaFilaDto;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** HU-31: cartola de prácticas (rango de fechas, resumen de evaluaciones y PDF), sin base de datos. */
@ExtendWith(MockitoExtension.class)
class CartolaPracticasServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2025, 12, 31);

    @Mock
    private InscripcionRepository inscripciones;
    @Mock
    private EvaluacionRepository evaluaciones;

    private CartolaPracticasService servicio;

    private final Usuario guia = Datos.usuario(2, "PROFESOR_GUIA");
    private final Usuario curso = Datos.usuario(4, "PROFESOR_CURSO");
    private final Estudiante estudiante = Datos.estudiante(1, Datos.usuario(6, "ESTUDIANTE"));

    @BeforeEach
    void preparar() {
        Clock hoy = Clock.fixed(Instant.parse("2026-10-10T15:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new CartolaPracticasService(inscripciones, evaluaciones, new CartolaPdfGenerador(hoy));
    }

    private Inscripcion cerrada(long id, boolean conGuia, String nota, LocalDate fechaCierre) {
        Inscripcion i = Datos.inscripcion(id, estudiante, Datos.asignatura(conGuia ? 3 : 1, conGuia),
                conGuia ? guia : null, curso);
        i.setPeriodo("2025-2");
        i.setEstado(EstadoInscripcion.APROBADA);
        i.setNotaFinal(new BigDecimal(nota));
        i.setFechaCierre(fechaCierre);
        return i;
    }

    private static Evaluacion finalizada(Inscripcion i, String promedio) {
        Evaluacion e = new Evaluacion();
        e.setInscripcion(i);
        e.finalizar(80, new BigDecimal(promedio), null);
        return e;
    }

    @Test
    void rangoSinInscripcionesCerradasDaListaVaciaYNoConsultaEvaluaciones() {
        when(inscripciones.findCerradasEntre(DESDE, HASTA)).thenReturn(List.of());

        assertThat(servicio.filas(DESDE, HASTA)).isEmpty();
        verifyNoInteractions(evaluaciones);
    }

    @Test
    void conResultadosCuentaLasEvaluacionesFinalizadasYPromediaSusPromedios() {
        Inscripcion conDos = cerrada(9, true, "6.4", LocalDate.of(2025, 12, 31));
        Inscripcion sinEvaluaciones = cerrada(1, false, "5.8", LocalDate.of(2025, 7, 31));
        when(inscripciones.findCerradasEntre(DESDE, HASTA)).thenReturn(List.of(sinEvaluaciones, conDos));
        when(evaluaciones.findPorInscripciones(List.of(1L, 9L), EstadoEvaluacion.FINALIZADA))
                .thenReturn(List.of(finalizada(conDos, "4.80"), finalizada(conDos, "5.90")));

        List<CartolaFilaDto> filas = servicio.filas(DESDE, HASTA);

        assertThat(filas).extracting(CartolaFilaDto::inscripcionId).containsExactly(1L, 9L);
        CartolaFilaDto sinEval = filas.get(0);
        assertThat(sinEval.profesorGuia()).isNull();                 // ciclo inicial: no va a un colegio
        assertThat(sinEval.evaluacionesFinalizadas()).isZero();
        assertThat(sinEval.promedioEvaluaciones()).isNull();
        CartolaFilaDto conEval = filas.get(1);
        assertThat(conEval.estudiante()).isEqualTo(estudiante.getUsuario().getNombreCompleto());
        assertThat(conEval.rut()).isEqualTo("11111111-1");
        assertThat(conEval.profesorGuia()).isEqualTo(guia.getNombreCompleto());
        assertThat(conEval.profesorCurso()).isEqualTo(curso.getNombreCompleto());
        assertThat(conEval.estado()).isEqualTo(EstadoInscripcion.APROBADA);
        assertThat(conEval.notaFinal()).isEqualByComparingTo("6.4");
        assertThat(conEval.fechaCierre()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(conEval.evaluacionesFinalizadas()).isEqualTo(2);
        assertThat(conEval.promedioEvaluaciones()).isEqualByComparingTo("5.35");
    }

    @Test
    void fechasInvertidasDan400SinConsultarLaBase() {
        assertThatThrownBy(() -> servicio.filas(HASTA, DESDE))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no puede ser posterior");
        assertThatThrownBy(() -> servicio.pdf(HASTA, DESDE)).isInstanceOf(ReglaNegocioException.class);
        verify(inscripciones, never()).findCerradasEntre(any(), any());
    }

    @Test
    void unSoloDiaEsUnRangoValido() {
        LocalDate dia = LocalDate.of(2025, 12, 31);
        when(inscripciones.findCerradasEntre(dia, dia)).thenReturn(List.of());

        assertThat(servicio.filas(dia, dia)).isEmpty();
    }

    @Test
    void elPdfTraeEncabezadoConRangoYFechaDeGeneracionYUnaFilaPorInscripcion() throws Exception {
        Inscripcion i = cerrada(9, true, "6.4", LocalDate.of(2025, 12, 31));
        when(inscripciones.findCerradasEntre(DESDE, HASTA)).thenReturn(List.of(i));
        when(evaluaciones.findPorInscripciones(List.of(9L), EstadoEvaluacion.FINALIZADA))
                .thenReturn(List.of(finalizada(i, "4.80"), finalizada(i, "5.90")));

        byte[] pdf = servicio.pdf(DESDE, HASTA);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String texto = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        assertThat(texto).contains("Cartola de prácticas", "01-01-2025", "31-12-2025",
                "Generada el 10-10-2026 12:00", "Total: 1 inscripción",
                estudiante.getRut(), "6,4", "5,35");
    }

    @Test
    void elPdfDeUnRangoVacioLoDiceEnVezDeMostrarUnaTablaVacia() throws Exception {
        when(inscripciones.findCerradasEntre(DESDE, HASTA)).thenReturn(List.of());

        String texto = new PdfTextExtractor(new PdfReader(servicio.pdf(DESDE, HASTA))).getTextFromPage(1);

        assertThat(texto).contains("Total: 0 inscripciones", "No hay inscripciones cerradas en este rango");
    }
}

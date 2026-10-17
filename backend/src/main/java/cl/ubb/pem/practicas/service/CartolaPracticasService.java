package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.CartolaFilaDto;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-31: cartola de prácticas para el jefe de carrera. Es solo lectura: junta las inscripciones cerradas en un
 * rango de fechas con el resumen de sus evaluaciones finalizadas, como evidencia para el criterio 3 de acreditación.
 * El rol lo restringe @PreAuthorize en el controlador; aquí no hay filtro por alcance porque el jefe de carrera ve
 * toda la carrera, pero solo en forma agregada.
 */
@Service
public class CartolaPracticasService {

    private final InscripcionRepository inscripciones;
    private final EvaluacionRepository evaluaciones;
    private final CartolaPdfGenerador generador;

    public CartolaPracticasService(InscripcionRepository inscripciones, EvaluacionRepository evaluaciones,
                                   CartolaPdfGenerador generador) {
        this.inscripciones = inscripciones;
        this.evaluaciones = evaluaciones;
        this.generador = generador;
    }

    /** Una fila por inscripción cuya fecha de cierre está entre {@code desde} y {@code hasta} (ambas incluidas). */
    @Transactional(readOnly = true)
    public List<CartolaFilaDto> filas(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha 'desde' (" + desde + ") no puede ser posterior a 'hasta' ("
                    + hasta + ").");
        }
        List<Inscripcion> cerradas = inscripciones.findCerradasEntre(desde, hasta);
        if (cerradas.isEmpty()) {
            return List.of();
        }
        List<Long> ids = cerradas.stream().map(Inscripcion::getId).toList();
        Map<Long, List<BigDecimal>> promediosPorInscripcion = evaluaciones
                .findPorInscripciones(ids, EstadoEvaluacion.FINALIZADA).stream()
                .collect(Collectors.groupingBy(e -> e.getInscripcion().getId(),
                        Collectors.mapping(Evaluacion::getPromedio, Collectors.toList())));
        return cerradas.stream()
                .map(i -> CartolaFilaDto.desde(i, promediosPorInscripcion.getOrDefault(i.getId(), List.of())))
                .toList();
    }

    /** La misma cartola, como PDF descargable. */
    @Transactional(readOnly = true)
    public byte[] pdf(LocalDate desde, LocalDate hasta) {
        return generador.generar(desde, hasta, filas(desde, hasta));
    }
}

package cl.ubb.pem.practicas.dto;

import java.time.LocalDate;

/**
 * Los campos del formato oficial de planificación (S-45) que el estudiante escribe, comunes a crear y a editar.
 * Los dos request son records con estos mismos componentes, así el servicio los copia con un solo método.
 */
public interface CamposPlanificacion {
    String titulo();
    String establecimiento();
    String curso();
    String unidad();
    String objetivoAprendizaje();
    String objetivoActitudinal();
    String objetivoClase();
    LocalDate fechaClase();
    Integer duracionMinutos();
    String recursos();
    String instrumentosEvaluacion();
    String inicio();
    Integer inicioMinutos();
    String desarrollo();
    Integer desarrolloMinutos();
    String cierre();
    Integer cierreMinutos();
}

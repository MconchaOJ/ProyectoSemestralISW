package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.AsignaturaDto;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.AsignaturaPracticaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-02: consulta de las 6 asignaturas de práctica (vienen precargadas por db/seed.sql). */
@Service
@Transactional(readOnly = true)
public class AsignaturaService {

    private final AsignaturaPracticaRepository asignaturas;

    public AsignaturaService(AsignaturaPracticaRepository asignaturas) {
        this.asignaturas = asignaturas;
    }

    public List<AsignaturaDto> listar() {
        return asignaturas.findAllByOrderByOrdenAsc().stream().map(AsignaturaDto::desde).toList();
    }

    public AsignaturaDto obtener(Integer id) {
        return asignaturas.findById(id).map(AsignaturaDto::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la asignatura con id " + id + "."));
    }
}

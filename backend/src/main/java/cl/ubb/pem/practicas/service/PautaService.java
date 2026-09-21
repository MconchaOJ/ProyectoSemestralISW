package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.PautaDto;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.PautaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-03, criterio 1: consulta de las pautas con sus dimensiones e indicadores, en orden. */
@Service
@Transactional(readOnly = true)
public class PautaService {

    private final PautaRepository pautas;

    public PautaService(PautaRepository pautas) {
        this.pautas = pautas;
    }

    public List<PautaDto> listar(Integer asignaturaId) {
        var lista = asignaturaId == null
                ? pautas.findByActivaTrueOrderByIdAsc()
                : pautas.findByAsignaturaIdAndActivaTrueOrderByIdAsc(asignaturaId);
        return lista.stream().map(PautaDto::desde).toList();
    }

    public PautaDto obtener(Integer id) {
        return pautas.findById(id).map(PautaDto::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la pauta con id " + id + "."));
    }
}

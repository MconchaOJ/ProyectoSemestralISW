package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.PautaDto;
import cl.ubb.pem.practicas.service.PautaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pautas")
@Tag(name = "HU-03 Pauta digital y evaluaciones")
public class PautaController {

    private final PautaService pautas;

    public PautaController(PautaService pautas) {
        this.pautas = pautas;
    }

    @GetMapping
    @Operation(summary = "Consultar las pautas de una asignatura",
            description = "Cualquier rol autenticado (S-27: el estudiante también puede ver con qué criterios lo evalúan). "
                    + "Devuelve dimensiones e indicadores en orden, con la escala. La pauta de observación de clase "
                    + "es de la asignatura 3 (Trabajo Colaborativo).")
    public List<PautaDto> listar(@RequestParam(required = false) Integer asignaturaId) {
        return pautas.listar(asignaturaId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una pauta", description = "Cualquier rol autenticado.")
    public PautaDto obtener(@PathVariable Integer id) {
        return pautas.obtener(id);
    }
}

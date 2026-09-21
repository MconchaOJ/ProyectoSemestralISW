package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.AsignaturaDto;
import cl.ubb.pem.practicas.service.AsignaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asignaturas")
@Tag(name = "HU-02 Estudiantes e inscripciones")
public class AsignaturaController {

    private final AsignaturaService asignaturas;

    public AsignaturaController(AsignaturaService asignaturas) {
        this.asignaturas = asignaturas;
    }

    @GetMapping
    @Operation(summary = "Las 6 asignaturas de práctica",
            description = "Cualquier rol autenticado. Incluye ciclo, modalidad y si el chatbot aplica (solo las dos finales).")
    public List<AsignaturaDto> listar() {
        return asignaturas.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una asignatura", description = "Cualquier rol autenticado.")
    public AsignaturaDto obtener(@PathVariable Integer id) {
        return asignaturas.obtener(id);
    }
}

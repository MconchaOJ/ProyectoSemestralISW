package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.ParametroDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.ParametroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parametros")
@Tag(name = "HU-27 Parámetros del sistema")
public class ParametroController {

    private final ParametroService parametros;

    public ParametroController(ParametroService parametros) {
        this.parametros = parametros;
    }

    @GetMapping
    @Operation(summary = "Parámetros del sistema",
            description = "Cualquier usuario autenticado los lee (los formularios muestran, por ejemplo, los días de "
                    + "anticipación y el tamaño máximo de archivo). Cada uno trae su rango y quién lo cambió por última vez.")
    public List<ParametroDto> listar() {
        return parametros.listar();
    }

    @PutMapping("/{clave}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Cambiar un parámetro (HU-27)",
            description = "Solo COORDINADOR. Rige desde la petición siguiente. 400 si el valor está fuera del rango del "
                    + "parámetro (S-80); 404 si la clave no existe. Los tamaños de archivo solo se pueden bajar del techo "
                    + "actual (10 MB los documentos, 25 MB el audio).")
    public ParametroDto actualizar(@PathVariable String clave, @Valid @RequestBody ParametroDto.Cambio cambio,
                                   @AuthenticationPrincipal UsuarioAutenticado actor) {
        return parametros.actualizar(clave, cambio.valor(), actor);
    }
}

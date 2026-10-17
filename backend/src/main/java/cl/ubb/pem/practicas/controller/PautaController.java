package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.PautaActivaRequest;
import cl.ubb.pem.practicas.dto.PautaDto;
import cl.ubb.pem.practicas.dto.PautaGestionDto;
import cl.ubb.pem.practicas.dto.PautaGuardarRequest;
import cl.ubb.pem.practicas.service.PautaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pautas")
@Tag(name = "HU-03 y HU-06 Pautas de evaluación")
public class PautaController {

    private final PautaService pautas;

    public PautaController(PautaService pautas) {
        this.pautas = pautas;
    }

    @GetMapping
    @Operation(summary = "Consultar las pautas de una asignatura",
            description = "Cualquier rol autenticado (S-27: el estudiante también puede ver con qué criterios lo evalúan). "
                    + "Solo las ACTIVAS. Devuelve dimensiones e indicadores en orden, con la escala y la versión. La pauta "
                    + "de observación de clase es de la asignatura 3 (Trabajo Colaborativo).")
    public List<PautaDto> listar(@RequestParam(required = false) Integer asignaturaId) {
        return pautas.listar(asignaturaId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una pauta", description = "Cualquier rol autenticado. Sirve para cualquier versión, "
            + "también una antigua: así cada evaluación se ve con la versión de la pauta con que se hizo.")
    public PautaDto obtener(@PathVariable Integer id) {
        return pautas.obtener(id);
    }

    // ---------------------------------------------------------------------------------------- HU-06

    @GetMapping("/gestion")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "HU-06: todas las pautas y versiones",
            description = "Solo COORDINADOR. Activas e inactivas, con su versión, si es la vigente y cuántas evaluaciones la usan.")
    public List<PautaGestionDto> gestion(@RequestParam(required = false) Integer asignaturaId) {
        return pautas.gestion(asignaturaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "HU-06: crear una pauta",
            description = "Solo COORDINADOR. Queda como versión 1 y activa. Los indicadores se numeran solos en el orden "
                    + "enviado. 400 si la escala máxima no supera a la mínima o hay dimensiones repetidas; 409 si ya hay "
                    + "otra pauta con ese nombre en la asignatura.")
    public PautaDto crear(@Valid @RequestBody PautaGuardarRequest request) {
        return pautas.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "HU-06: editar una pauta",
            description = "Solo COORDINADOR y solo la versión vigente (409 si ya fue reemplazada). Se envía la pauta "
                    + "completa. Si NINGUNA evaluación la usa, se modifica en el mismo lugar (mismo id). Si ya se usó, se "
                    + "crea la VERSIÓN SIGUIENTE (id nuevo, `version` + 1, `versionAnteriorId` = la de antes) y la "
                    + "anterior queda inactiva: sus evaluaciones conservan sus indicadores y su puntaje máximo. Si no "
                    + "cambió nada, no se crea ninguna versión. La asignatura no se cambia (400).")
    public PautaDto actualizar(@PathVariable Integer id, @Valid @RequestBody PautaGuardarRequest request) {
        return pautas.actualizar(id, request);
    }

    @PutMapping("/{id}/activa")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "HU-06: activar o desactivar una pauta",
            description = "Solo COORDINADOR y solo la versión vigente. Una pauta inactiva no se ofrece para evaluaciones "
                    + "nuevas; las evaluaciones ya hechas con ella no cambian.")
    public PautaDto cambiarActiva(@PathVariable Integer id, @Valid @RequestBody PautaActivaRequest request) {
        return pautas.cambiarActiva(id, request.activa());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "HU-06: borrar una pauta sin uso",
            description = "Solo COORDINADOR y solo la versión vigente. 409 si alguna evaluación la usa (una pauta en uso "
                    + "no se borra: se desactiva o se edita y se versiona). Borrar una versión 2 o posterior sin uso "
                    + "la deshace: la anterior vuelve a ser la vigente.")
    public void eliminar(@PathVariable Integer id) {
        pautas.eliminar(id);
    }
}

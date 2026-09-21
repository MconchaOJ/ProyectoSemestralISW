package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.LoginRequest;
import cl.ubb.pem.practicas.dto.LoginResponse;
import cl.ubb.pem.practicas.dto.UsuarioDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "HU-01 Autenticación", description = "Inicio de sesión y perfil del usuario autenticado")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements   // público: no exige token
    @Operation(summary = "Iniciar sesión",
            description = "Público. Devuelve un token JWT (8 horas) con el usuario y su rol. Credenciales incorrectas o "
                    + "usuario inactivo: 401 con el mismo mensaje.")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado", description = "Cualquier rol autenticado.")
    public UsuarioDto perfil(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return authService.perfil(actual);
    }
}

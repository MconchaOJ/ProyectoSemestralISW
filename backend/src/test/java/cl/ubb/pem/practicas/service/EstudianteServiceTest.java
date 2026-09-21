package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.ubb.pem.practicas.Datos;
import cl.ubb.pem.practicas.dto.EstudianteCrearRequest;
import cl.ubb.pem.practicas.dto.EstudianteDto;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.EstudianteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock
    private EstudianteRepository estudiantes;
    @Mock
    private UsuarioService usuarioService;
    @InjectMocks
    private EstudianteService servicio;

    private EstudianteCrearRequest request(String rut) {
        return new EstudianteCrearRequest("Ana", "Soto", "ana@demo.example", "clave-larga-1", rut, 6, 2024);
    }

    @Test
    void creaElUsuarioConRolEstudianteYLaFichaConElRutNormalizado() {
        when(usuarioService.crear(any(), any(), any(), any(), eq("ESTUDIANTE"))).thenReturn(Datos.usuario(9, "ESTUDIANTE"));
        when(estudiantes.save(any(Estudiante.class))).thenAnswer(inv -> {
            Estudiante e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EstudianteDto dto = servicio.crear(request("12.345.678-5"));

        assertThat(dto.rut()).isEqualTo("12345678-5");
        assertThat(dto.semestreActual()).isEqualTo(6);
        verify(usuarioService).crear("Ana", "Soto", "ana@demo.example", "clave-larga-1", "ESTUDIANTE");
    }

    @Test
    void rutConDigitoVerificadorIncorrectoSeRechazaSinCrearNada() {
        assertThatThrownBy(() -> servicio.crear(request("12345678-4"))).isInstanceOf(ReglaNegocioException.class);
        verify(usuarioService, never()).crear(any(), any(), any(), any(), any());
    }

    @Test
    void rutDuplicadoDaConflicto() {
        when(estudiantes.existsByRut("12345678-5")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(request("12345678-5"))).isInstanceOf(ConflictoException.class);
        verify(usuarioService, never()).crear(any(), any(), any(), any(), any());
    }
}

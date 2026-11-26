package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bo.aportaya.identidad.aplicacion.CU04StepUp;
import bo.aportaya.identidad.dominio.puertos.DesafioDeFactor;
import bo.aportaya.identidad.infraestructura.AccesoRepositorio;
import bo.aportaya.identidad.infraestructura.EmisorDeAcceso;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ADR-038 §5: el factor recien reinscrito por recuperacion asistida no habilita el step-up (AP-CU04-11). */
@org.junit.jupiter.api.Timeout(60)
class CU04EnfriamientoDelFactorTest {
    private final UUID usuario = UUID.randomUUID();
    private AccesoRepositorio accesos;
    private CU04StepUp stepUp;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void armar() {
        var factor = mock(DesafioDeFactor.class);
        when(factor.emitir(eq(usuario), any())).thenReturn(UUID.randomUUID());
        var datos = mock(Datos.class);
        when(datos.conContexto(any(), any()))
                .thenAnswer(i -> ((Function<Object, Object>) i.getArgument(1)).apply(null));
        accesos = mock(AccesoRepositorio.class);
        when(accesos.factorActivo(any(), eq(usuario))).thenReturn(Optional.of("TOTP"));
        stepUp = new CU04StepUp(
                datos,
                accesos,
                factor,
                new EmisorDeAcceso("", ""),
                Duration.ofMinutes(5),
                "aportaya-nucleo-financiero",
                Duration.ofHours(24));
    }

    private ContextoSesion como(UUID u) {
        return ContextoSesion.de(u, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName(
            "Dado un factor reinscrito dentro del enfriamiento · Cuando abre un step-up · Entonces se rechaza con AP-CU04-11")
    void rechazaPorFactorReinscritoReciente() {
        when(accesos.factorReinscritoDesde(any(), eq(usuario), any())).thenReturn(true);

        assertThatThrownBy(() -> stepUp.abrir("RETIRO", como(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("reinscribir");
    }

    @Test
    @DisplayName(
            "Dado un factor reinscrito hace más que el enfriamiento · Cuando abre un step-up · Entonces se emite el desafío")
    void abreSiElEnfriamientoPaso() {
        when(accesos.factorReinscritoDesde(any(), eq(usuario), any())).thenReturn(false);

        assertThat(stepUp.abrir("RETIRO", como(usuario)).id()).isNotNull();
    }
}

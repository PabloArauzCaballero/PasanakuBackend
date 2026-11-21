package bo.aportaya.entregas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bo.aportaya.entregas.dominio.CuentaEnmascarada;
import bo.aportaya.entregas.infraestructura.CuentaDestinoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CifradorSimetrico;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * SEG-007: el numero de cuenta se cifra SOLO en el servidor, desde el mismo numero del que salen el enmascarado y el
 * hash. Antes el caso de uso aceptaba un cifrado del cliente, que podia ser de otro numero: se mostraba una cuenta y se
 * le pagaba a otra.
 */
class CifradoDelNumeroDeCuentaTest {

    private static final String PIMIENTA = "pimienta-de-prueba-no-es-la-de-produccion";
    private static final String NUMERO = "4012345678";

    @SuppressWarnings("unchecked")
    private static Datos datosQueEjecutan(DSLContext dsl) {
        Datos datos = mock(Datos.class);
        when(datos.conContexto(any(), any()))
                .thenAnswer(i -> ((Function<DSLContext, Object>) i.getArgument(1)).apply(dsl));
        return datos;
    }

    @Test
    @DisplayName("descifrar lo que se guarda devuelve exactamente el numero del que salen el enmascarado y el hash")
    void idaYVuelta() {
        DSLContext dsl = mock(DSLContext.class);
        CuentaDestinoRepositorio cuentas = mock(CuentaDestinoRepositorio.class);
        UUID usuario = UUID.randomUUID();
        when(cuentas.porHash(any(), any(), anyString())).thenReturn(Optional.empty());
        when(cuentas.cuantasTiene(any(), any())).thenReturn(0);
        when(cuentas.registrar(any(), any(), any(), any(), anyString(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());
        var cu = new CU18RegistrarCuentaDestino(
                datosQueEjecutan(dsl),
                cuentas,
                mock(Outbox.class),
                Reloj.delSistema(),
                PIMIENTA,
                1,
                3,
                Duration.ofHours(24));

        var salida = cu.registrar(
                new CU18RegistrarCuentaDestino.EntradaRegistro(
                        "AHORRO", "Banco de Prueba", NUMERO, "Ana Prueba", "1000001", "Ana Prueba", "1000001", "BOB"),
                ContextoSesion.de(
                        usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString())));

        ArgumentCaptor<String> cifrado = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CuentaEnmascarada> enmascarada = ArgumentCaptor.forClass(CuentaEnmascarada.class);
        verify(cuentas)
                .registrar(
                        eq(dsl),
                        eq(usuario),
                        eq("AHORRO"),
                        eq("Banco de Prueba"),
                        cifrado.capture(),
                        enmascarada.capture(),
                        any(),
                        any(),
                        eq("BOB"),
                        eq(true));
        assertThat(cifrado.getValue()).startsWith(CifradorSimetrico.PREFIJO).doesNotContain(NUMERO);
        assertThat(CifradorSimetrico.descifrar(cifrado.getValue(), PIMIENTA)).isEqualTo(NUMERO);
        assertThat(enmascarada.getValue()).isEqualTo(CuentaEnmascarada.de(NUMERO, PIMIENTA, 1));
        assertThat(salida.numeroEnmascarado()).isEqualTo(enmascarada.getValue().enmascarado());
    }
}

package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-01 · AP-CU01-03 dice CUAL dato ya tiene cuenta.
 *
 * <p>Vivia en {@code CU01Test}; se separo porque ese archivo pasaba las 300 lineas del
 * barrido. Misma base, mismo contenedor.
 */
class CU01CuentaYaExisteTest extends BaseDeCU01 {

    @Test
    @DisplayName(
            "Dado un correo que ya tiene cuenta · Cuando otra persona se registra con ese correo · Entonces se rechaza con AP-CU01-03 y campo correo, sin crear nada")
    void correoQueYaTieneCuenta() {
        // Regresion (2026-10-15): no se preguntaba por el correo, el insert rebotaba en
        // uq_usuario_correo y la persona veia «algo salio mal de nuestro lado».
        var primera = entrada("+59177000031", "3131313", true, true);
        transaccion.execute(e -> registrarUsuario.ejecutar(conCorreo(primera, "repetido@ejemplo.test"), contexto()));
        long usuariosAntes = usuariosTotales();

        var segunda = conCorreo(entrada("+59177000032", "3232323", true, true), "repetido@ejemplo.test");
        assertThatThrownBy(() -> transaccion.execute(e -> registrarUsuario.ejecutar(segunda, contexto())))
                .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> {
                    assertThat(e.codigo().valor()).isEqualTo("AP-CU01-03");
                    assertThat(e.detalle()).containsEntry("campo", "correo");
                });
        assertThat(usuariosTotales()).isEqualTo(usuariosAntes);
    }

    @Test
    @DisplayName(
            "Dado un telefono que ya tiene cuenta · Cuando otra persona se registra con ese telefono · Entonces se rechaza con AP-CU01-03 y campo telefono")
    void telefonoQueYaTieneCuenta() {
        registrar("+59177000033", "3333331");
        assertThatThrownBy(() -> registrar("+59177000033", "3333332"))
                .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> {
                    assertThat(e.codigo().valor()).isEqualTo("AP-CU01-03");
                    assertThat(e.detalle()).containsEntry("campo", "telefono");
                });
    }

    private static bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario.EntradaRegistro conCorreo(
            bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario.EntradaRegistro e, String correo) {
        return new bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario.EntradaRegistro(
                e.telefonoE164(),
                e.nombres(),
                e.apellidos(),
                e.fechaNacimiento(),
                correo,
                e.canalVerificacion(),
                e.verificacionCorreoId(),
                e.documento(),
                e.numeroCifrado(),
                e.contrasena(),
                e.aceptaContratos(),
                e.licenciaHabilitaBilletera(),
                e.ip(),
                e.agente());
    }
}

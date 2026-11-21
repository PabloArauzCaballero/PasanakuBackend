package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code Datos.comoSistema} desde una sesion de participante: dentro del bloque el libro se
 * puede escribir y, al salir, el rol de la sesion vuelve a ser el de antes. Separada de
 * {@link AutorizacionNegativaTest} para que cada archivo quede bajo el tope de 300 lineas.
 */
class LibroComoSistemaTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    @Test
    @DisplayName(
            "Datos.comoSistema: dentro del bloque el libro se puede escribir, y el rol de la sesion se restaura despues")
    void comoSistemaEscribeYRestaura() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("RECARGA", ESTANDAR, "MES", new BigDecimal("10000.00"), null);
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("50.00"));
        String[] rolDespues = {null};
        int[] filas = {-1};

        // Datos.comoSistema exige una transaccion de Spring (07956b99: SET LOCAL fuera de una
        // transaccion no fija nada). La app siempre lo llama asi, de modo que la prueba lo
        // ejercita igual: la misma sesion de participante que arma comoUsuario (rol de la base
        // svc_nucleo_financiero + set_config), pero dentro de transaccion.execute. SET LOCAL ROLE
        // muere con la transaccion, y el rollback deja la base como estaba.
        transaccion.execute(estado -> {
            dsl.execute("SET LOCAL ROLE svc_nucleo_financiero");
            dsl.execute(
                    """
                    SELECT set_config('app.usuario_id', ?, true),
                           set_config('app.rol', 'PARTICIPANTE', true),
                           set_config('app.traza', ?, true)
                    """,
                    usuario.toString(),
                    UUID.randomUUID().toString());
            filas[0] = bo.aportaya.plataforma.datos.Datos.comoSistema(
                    dsl,
                    () -> dsl.execute(
                            AutorizacionNegativaTest.COPIA_DE_UN_MOVIMIENTO.replace("?", "'" + cuenta + "'::uuid")));
            rolDespues[0] =
                    dsl.fetchOne("select current_setting('app.rol', true)").get(0, String.class);
            estado.setRollbackOnly();
            return null;
        });

        assertThat(filas[0]).isEqualTo(1);
        assertThat(rolDespues[0])
                .as("el privilegio de sistema no queda abierto")
                .isEqualTo("PARTICIPANTE");
    }
}

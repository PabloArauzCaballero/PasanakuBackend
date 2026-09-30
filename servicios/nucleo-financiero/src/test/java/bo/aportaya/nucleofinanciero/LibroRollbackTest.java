package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.SalidaTransferencia;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LibroRollbackTest extends BaseDeLibroInvariantes {
    // ------------------------------- 10 · excepcion tras el debito, en contexto --
    /**
     * Escenario 10 del plan madre §H8.S2: una excepción DESPUÉS de que el débito ya
     * ocurrió, dentro del mismo {@code datos.conContexto(...)} de
     * {@code CU12TransferirSaldo.ejecutar}, tiene que revertir TODO — el débito
     * incluido — porque {@code conContexto} corre dentro de la transacción de Spring
     * ({@code @Transactional}) del método, no en una transacción propia.
     *
     * <p>No se fuerza la excepción con un doble ni tocando código de producción: se
     * usa una violación real de {@code fk_transferencia_p2p_grupo_id}
     * (`sql/20_claves/10_billetera_custodia.sql:302-305`) pasando un {@code grupoId}
     * que no existe. Esa comprobación ocurre en
     * {@code TransferenciaRepositorio.registrar}, que corre DESPUÉS de
     * {@code libro.registrar} (el que hace el débito y el crédito) — exactamente el
     * orden que el escenario pide: excepción tras el débito, misma transacción.
     */
    @Test
    @DisplayName(
            "escenario 10: excepcion despues del debito (FK invalida de grupoId) dentro de Datos.conContexto — revierte TODO, ni el debito sobrevive")
    void excepcionTrasElDebitoRevierteTodoDentroDeConContexto() {
        Par p = par("1000.00");
        int totalAntes = saldoTotalDelSistema();
        UUID grupoInexistente = UUID.randomUUID();

        assertThatThrownBy(() -> transaccion.execute(t -> transferenciaCU.ejecutar(
                        new EntradaTransferencia(
                                "inv-10",
                                p.origen(),
                                p.destino(),
                                bob("500.00"),
                                "prueba",
                                Optional.of(grupoInexistente),
                                Optional.empty()),
                        p.ctx())))
                .as("un grupoId inexistente tiene que violar fk_transferencia_p2p_grupo_id DESPUES del debito")
                .isInstanceOf(RuntimeException.class);

        assertThat(saldoTotalDelSistema())
                .as("el debito hecho antes de la excepcion tiene que revertirse entero")
                .isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.origen()))
                .as("la cuenta de origen queda exactamente como antes: el debito no sobrevive")
                .isEqualTo(1000);
        assertThat(
                        contar(
                                "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE clave_idempotencia = 'inv-10'"))
                .as("ni la cabecera de la transaccion (escrita por libro.registrar, antes de la FK invalida) sobrevive")
                .isZero();
    }

    // ------------------------------------------------------------- 9 · rollback --
    @Test
    @DisplayName("escenario 9: una transferencia que revierte (rollback explicito) no deja rastro")
    void rollbackNoDejaRastro() {
        Par p = par("1000.00");
        int totalAntes = saldoTotalDelSistema();

        UUID[] transaccionId = new UUID[1];
        transaccion.execute(t -> {
            SalidaTransferencia salida = transferenciaCU.ejecutar(
                    new EntradaTransferencia(
                            "inv-9",
                            p.origen(),
                            p.destino(),
                            bob("500.00"),
                            "revertida",
                            Optional.empty(),
                            Optional.empty()),
                    p.ctx());
            transaccionId[0] = salida.transaccionId();
            t.setRollbackOnly();
            return null;
        });

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.origen()))
                .isEqualTo(1000);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE id = ?",
                        transaccionId[0]))
                .as("el rollback no deja ni la cabecera de la transaccion")
                .isZero();
    }
}

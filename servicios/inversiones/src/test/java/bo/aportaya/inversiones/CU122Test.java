package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.aplicacion.VistaOrden;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M3 · confirmar la posicion SIN duplicar el saldo.
 *
 * <p>La conciliacion es entre tres cosas: el efectivo (libro), la posicion y la operacion
 * del aliado. El libro y el aliado son dobles declarados; la base es PostgreSQL real.
 */
class CU122Test extends BaseDeInversiones {

    private String codigoDe(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    @Test
    @DisplayName(
            "Dada la confirmacion del aliado · Cuando se procesa · Entonces nace la posicion, se elimina la disponibilidad del importe y efectivo + posicion = saldo inicial")
    void criterio1() {
        conSaldo("5000.00");

        VistaOrden orden = ordenar(PRODUCTO_DPF, "1000.00");

        assertThat(orden.estado()).isEqualTo("CONFIRMADA");
        var pos = tx.en(() -> datos.conContexto(ctx, d -> posiciones.porId(d, orden.posicionId())))
                .orElseThrow();
        assertThat(pos.principal()).isEqualByComparingTo("1000.00");
        assertThat(pos.estado()).isEqualTo("ABIERTA");
        assertThat(pos.posicionExterna()).isEqualTo("POS-" + orden.ordenId());
        // Efectivo: el importe ya no esta disponible NI retenido, y no se conto dos veces.
        assertThat(libro.disponible(cuenta)).isEqualByComparingTo("4000.00");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");
        // Cuadre: lo que queda en la billetera + el costo de la posicion, DESDE el mayor de comprobantes, es lo que
        // habia.
        BigDecimal costoDeLaPosicion =
                tx.en(() -> datos.conContexto(ctx, d -> comprobantes.costoBaseVigente(d, pos.id())));
        assertThat(costoDeLaPosicion).isEqualByComparingTo("1000.00");
        assertThat(libro.total(cuenta).add(costoDeLaPosicion)).isEqualByComparingTo("5000.00");
        // Asiento del libro (del doble): una pata contra la otra, por el mismo importe.
        assertThat(libro.asientos()).hasSize(1);
        var asiento = libro.asientos().get(0);
        assertThat(asiento.monto()).isEqualByComparingTo("1000.00");
        assertThat(asiento.cuentaDebe()).isEqualTo(LibroDoble.CUSTODIA);
        assertThat(asiento.cuentaHaber()).isEqualTo("billetera:" + cuenta);
    }

    @Test
    @DisplayName(
            "reintento: confirmar dos veces la misma orden (sincronizar x2) no duplica posicion, comprobante ni debito")
    void confirmarDosVeces() {
        conSaldo("5000.00");
        aliado.modo(AliadoDoble.Modo.PENDIENTE);
        VistaOrden pendiente = ordenar(PRODUCTO_DPF, "1000.00");
        aliado.confirmarPendientes();

        VistaOrden a = cu122.sincronizar(pendiente.ordenId(), ctx);
        VistaOrden b = cu122.sincronizar(pendiente.ordenId(), ctx);

        assertThat(a.estado()).isEqualTo("CONFIRMADA");
        assertThat(b.posicionId()).isEqualTo(a.posicionId());
        assertThat(contar("select count(*)::int from inversiones.posicion_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
        assertThat(contar("select count(*)::int from inversiones.comprobante_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
        assertThat(libro.asientos()).hasSize(1);
        assertThat(libro.total(cuenta)).isEqualByComparingTo("4000.00");
    }

    @Test
    @DisplayName(
            "Dado un aliado que confirma un importe DISTINTO · Cuando se procesa la confirmacion · Entonces AP-CU122-02: no hay posicion y el importe sigue retenido")
    void confirmacionQueNoCoincide() {
        conSaldo("5000.00");
        aliado.distorsionDeMonto("0.01");

        assertThatThrownBy(() -> ordenar(PRODUCTO_DPF, "1000.00"))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU122-02"));

        assertThat(contar("select count(*)::int from inversiones.posicion_inversion where usuario_id = ?", usuario))
                .isZero();
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("1000.00");
        assertThat(libro.asientos()).isEmpty();
        assertThat(contar(
                        "select count(*)::int from inversiones.orden_inversion where usuario_id = ? and estado = 'ENVIADA'",
                        usuario))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un aliado que rechaza · Cuando se procesa la orden · Entonces la orden queda RECHAZADA y lo retenido se libera exactamente una vez")
    void rechazoDelAliado() {
        conSaldo("5000.00");
        aliado.modo(AliadoDoble.Modo.RECHAZA);

        VistaOrden orden = ordenar(PRODUCTO_DPF, "1000.00");

        assertThat(orden.estado()).isEqualTo("RECHAZADA");
        assertThat(orden.motivoRechazo()).isEqualTo("RECHAZO_DE_PRUEBA");
        assertThat(libro.disponible(cuenta)).isEqualByComparingTo("5000.00");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");
        assertThat(libro.veces("LIBERAR:" + orden.ordenId())).isEqualTo(1);
        assertThat(contar("select count(*)::int from inversiones.posicion_inversion where usuario_id = ?", usuario))
                .isZero();
        // Repetir la sincronizacion de una orden rechazada no libera de nuevo.
        cu122.sincronizar(orden.ordenId(), ctx);
        assertThat(libro.veces("LIBERAR:" + orden.ordenId())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada la respuesta PERDIDA (el aliado la registro y a nosotros nos llega un corte) · Cuando se procesa la orden · Entonces INCIERTA, el saldo sigue retenido, se CONSULTA y no se reenvia a ciegas")
    void respuestaPerdida() {
        conSaldo("5000.00");
        aliado.modo(AliadoDoble.Modo.PERDIDA);

        VistaOrden incierta = ordenar(PRODUCTO_DPF, "1000.00");

        assertThat(incierta.estado()).isEqualTo("INCIERTA");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("1000.00");
        assertThat(libro.veces("LIBERAR:" + incierta.ordenId())).isZero();

        aliado.modo(AliadoDoble.Modo.NORMAL);
        VistaOrden resuelta = cu122.sincronizar(incierta.ordenId(), ctx);

        assertThat(resuelta.estado()).isEqualTo("CONFIRMADA");
        assertThat(aliado.envios(incierta.ordenId()))
                .as("la orden se envio una sola vez")
                .isEqualTo(1);
        assertThat(libro.total(cuenta)).isEqualByComparingTo("4000.00");
        assertThat(contar("select count(*)::int from inversiones.posicion_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado el aliado CAIDO antes de recibirla · Cuando se procesa la orden · Entonces INCIERTA; al volver, la consulta dice que no la conoce y recien ahi se reenvia con la misma referencia")
    void aliadoCaidoYRecuperacion() {
        conSaldo("5000.00");
        aliado.modo(AliadoDoble.Modo.CAIDO);

        VistaOrden incierta = ordenar(PRODUCTO_DPF, "1000.00");
        assertThat(incierta.estado()).isEqualTo("INCIERTA");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("1000.00");
        assertThat(aliado.operacion(incierta.ordenId())).isEmpty();

        aliado.modo(AliadoDoble.Modo.NORMAL);
        VistaOrden resuelta = cu122.sincronizar(incierta.ordenId(), ctx);

        assertThat(resuelta.estado()).isEqualTo("CONFIRMADA");
        assertThat(aliado.operacion(incierta.ordenId())).isPresent();
        assertThat(libro.total(cuenta)).isEqualByComparingTo("4000.00");
        assertThat(contar("select count(*)::int from inversiones.posicion_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado el libro caido AL DEBITAR · Cuando se procesa la confirmacion · Entonces la posicion existe pero el importe sigue retenido (no hay doble disponibilidad); al volver el libro, se debita una vez")
    void libroCaidoAlDebitar() {
        conSaldo("5000.00");
        libro.fallarEn("debitar", true);

        VistaOrden orden = ordenar(PRODUCTO_DPF, "1000.00");

        assertThat(orden.estado()).isEqualTo("CONFIRMADA");
        assertThat(libro.disponible(cuenta))
                .as("el importe NO vuelve a estar disponible")
                .isEqualByComparingTo("4000.00");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("1000.00");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("5000.00");
        assertThat(contar(
                        "select count(*)::int from inversiones.instruccion_libro where tipo = 'DEBITAR' and estado = 'PENDIENTE' and origen_id = ?",
                        orden.ordenId()))
                .isEqualTo(1);

        UUID instruccion = dslFixtura
                .fetchOne(
                        "select id from inversiones.instruccion_libro where tipo = 'DEBITAR' and origen_id = ?",
                        orden.ordenId())
                .get(0, UUID.class);
        libro.fallarEn("debitar", false);
        assertThat(aplicador.aplicar(instruccion, sistema())).isEqualTo("APLICADA");
        assertThat(aplicador.aplicar(instruccion, sistema()))
                .as("la segunda pasada no vuelve a tocar el libro")
                .isEqualTo("APLICADA");

        assertThat(libro.total(cuenta)).isEqualByComparingTo("4000.00");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");
        assertThat(libro.asientos()).hasSize(1);
    }

    @Test
    @DisplayName(
            "Dada una orden de OTRA persona · Cuando intenta verla o sincronizarla · Entonces AP-CU122-01: no existe para ella")
    void ordenAjena() {
        conSaldo("5000.00");
        VistaOrden propia = ordenar(PRODUCTO_DPF, "1000.00");
        var otraSesion = contextoDe(UUID.randomUUID());

        assertThatThrownBy(() -> cu122.ver(propia.ordenId(), otraSesion))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU122-01"));
        assertThatThrownBy(() -> cu122.sincronizar(propia.ordenId(), otraSesion))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU122-01"));
        assertThatThrownBy(() -> cu122.ver(UUID.randomUUID(), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU122-01"));
    }

    @Test
    @DisplayName("rechaza por R-INV-04: una posicion DPF con cuotas o sin vencimiento")
    void rechazaRINV04() {
        // Una posicion es coherente con su tipo: la base lo exige aunque alguien llegue por fuera del
        // caso de uso. Un DPF tiene vencimiento y no tiene cuotas; la fila correcta se acepta.
        conSaldo("5000.00");
        UUID dpf = posicionConfirmada(PRODUCTO_DPF, "1000.00");

        assertThat(rechazaLaBase("update inversiones.posicion_inversion set cuotas = 5 where id = ?", dpf))
                .contains("ck_posicion_inv_tipo");
        assertThat(rechazaLaBase(
                        "update inversiones.posicion_inversion set fecha_vencimiento = null where id = ?", dpf))
                .contains("ck_posicion_inv_tipo");
        assertThat(rechazaLaBase("update inversiones.posicion_inversion set version = version + 1 where id = ?", dpf))
                .as("control negativo: el mismo update, coherente, entra")
                .isEmpty();
    }

    @Test
    @DisplayName(
            "rechaza por R-INV-06: una instruccion al libro aplicada sin su referencia o con una clave de idempotencia repetida")
    void rechazaRINV06() {
        // Toda instruccion al libro es unica por clave y no se da por aplicada sin la referencia que
        // devolvio el libro: sin ella no hay forma de probar que el efecto ocurrio.
        conSaldo("5000.00");
        VistaOrden orden = ordenar(PRODUCTO_DPF, "1000.00");
        String copia =
                "insert into inversiones.instruccion_libro (origen_tipo, origen_id, tipo, usuario_id, cuenta_billetera_id,"
                        + " monto, moneda, clave_idempotencia, estado)"
                        + " select origen_tipo, origen_id, 'LIBERAR', usuario_id, cuenta_billetera_id, monto, moneda,"
                        + " clave_idempotencia || ?, 'PENDIENTE'"
                        + " from inversiones.instruccion_libro where origen_id = ? and tipo = 'RETENER'";

        assertThat(rechazaLaBase(
                        "update inversiones.instruccion_libro set referencia_libro = null where origen_id = ? and tipo = 'RETENER'",
                        orden.ordenId()))
                .contains("ck_instruccion_libro_aplicada");
        assertThat(rechazaLaBase(copia, "", orden.ordenId())).contains("uq_instruccion_libro_clave_idempotencia");
        assertThat(rechazaLaBase(copia, "-otra", orden.ordenId()))
                .as("control negativo: con otra clave la instruccion entra")
                .isEmpty();
    }
}

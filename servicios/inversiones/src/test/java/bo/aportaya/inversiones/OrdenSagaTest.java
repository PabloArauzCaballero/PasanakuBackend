package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.inversiones.aplicacion.CU124SolicitarRescate.EntradaRescate;
import bo.aportaya.inversiones.aplicacion.VistaOrden;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La saga completa (orden -> retencion -> envio -> confirmacion -> debito; rescate ->
 * liquidacion -> credito) rompiendo A PROPOSITO cada dependencia, en el orden en que la
 * regla 98.8 lo pide: caida del dependido, duplicado, traza y que queda inconsistente.
 *
 * <p>El libro y el aliado son dobles declarados con tres niveles (correcto, limite,
 * invalido); la base es PostgreSQL real. Un doble no sustituye la integracion real: sustituye
 * la espera (los contratos reales estan pendientes del productor, ver el reporte).
 */
class OrdenSagaTest extends BaseDeInversiones {

    private int pendientes(UUID origen) {
        return contar(
                "select count(*)::int from inversiones.instruccion_libro where origen_id = ? and estado = 'PENDIENTE'",
                origen);
    }

    @Test
    @DisplayName(
            "Libro caido al RETENER: la orden queda CREADA con su intencion persistida, el aliado ni se entera, y al volver el libro se completa sin duplicar")
    void libroCaidoAlRetener() {
        conSaldo("5000.00");
        libro.fallarEn("retener", true);

        VistaOrden creada = ordenar(PRODUCTO_DPF, "1000.00");

        assertThat(creada.estado()).isEqualTo("CREADA");
        assertThat(pendientes(creada.ordenId())).isEqualTo(1);
        assertThat(aliado.operacion(creada.ordenId()))
                .as("el aliado no recibio nada: no hay plata comprometida sin retener")
                .isEmpty();
        assertThat(libro.total(cuenta)).isEqualByComparingTo("5000.00");

        libro.fallarEn("retener", false);
        VistaOrden resuelta = cu122.sincronizar(creada.ordenId(), ctx);
        cu122.sincronizar(creada.ordenId(), ctx);

        assertThat(resuelta.estado()).isEqualTo("CONFIRMADA");
        assertThat(libro.veces("DEBITAR:" + creada.ordenId())).isEqualTo(1);
        assertThat(contar("select count(*)::int from inversiones.posicion_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
        assertThat(libro.total(cuenta)).isEqualByComparingTo("4000.00");
        assertThat(pendientes(creada.ordenId())).isZero();
    }

    @Test
    @DisplayName(
            "Libro caido al LIBERAR tras un rechazo: la orden queda RECHAZADA, lo retenido sigue visible, y la liberacion se completa despues exactamente una vez")
    void libroCaidoAlLiberar() {
        conSaldo("5000.00");
        aliado.modo(AliadoDoble.Modo.RECHAZA);
        libro.fallarEn("liberar", true);

        VistaOrden rechazada = ordenar(PRODUCTO_DPF, "1000.00");

        assertThat(rechazada.estado()).isEqualTo("RECHAZADA");
        assertThat(libro.retenido(cuenta))
                .as("inconsistencia visible y acotada: plata reservada hasta que el libro vuelva")
                .isEqualByComparingTo("1000.00");
        assertThat(pendientes(rechazada.ordenId())).isEqualTo(1);

        libro.fallarEn("liberar", false);
        assertThat(aplicador.reintentarPendientes(500, sistema())).isGreaterThanOrEqualTo(0);
        cu122.sincronizar(rechazada.ordenId(), ctx);

        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");
        assertThat(libro.disponible(cuenta)).isEqualByComparingTo("5000.00");
        assertThat(pendientes(rechazada.ordenId())).isZero();
    }

    @Test
    @DisplayName(
            "Todo roto a la vez: aliado caido Y libro caido. Nada se pierde, nada se duplica y todo queda reintentable")
    void todoCaidoYRecuperacionTotal() {
        conSaldo("5000.00");
        libro.caido(true);
        // Con el libro caido ni siquiera se puede verificar el saldo: no se crea nada.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ordenar(PRODUCTO_DPF, "1000.00"))
                .isInstanceOf(bo.aportaya.plataforma.dominio.ErrorDeNegocio.class);
        libro.caido(false);

        aliado.modo(AliadoDoble.Modo.CAIDO);
        VistaOrden incierta = ordenar(PRODUCTO_DPF, "1000.00");
        assertThat(incierta.estado()).isEqualTo("INCIERTA");

        libro.caido(true);
        VistaOrden sigue = cu122.sincronizar(incierta.ordenId(), ctx);
        assertThat(sigue.estado()).isEqualTo("INCIERTA");

        libro.caido(false);
        aliado.modo(AliadoDoble.Modo.NORMAL);
        VistaOrden resultado = cu122.sincronizar(incierta.ordenId(), ctx);

        assertThat(resultado.estado()).isEqualTo("CONFIRMADA");
        assertThat(libro.asientos()).hasSize(1);
        assertThat(libro.total(cuenta)).isEqualByComparingTo("4000.00");
        assertThat(contar("select count(*)::int from inversiones.orden_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
        assertThat(contar("select count(*)::int from inversiones.posicion_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Rescate de punta a punta con el libro caido al acreditar y el aliado caido al consultar: el dinero llega UNA vez cuando todo vuelve")
    void rescateConTodoCaido() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        AHORA.set(Instant.parse("2027-04-11T12:00:00Z")); // pasado el vencimiento (180 dias)
        aliado.liquidacionDpf("197.26", "19.73", 180);
        libro.fallarEn("acreditar", true);

        var rescate = cu124.solicitar(
                new EntradaRescate(posicion, Optional.empty(), UUID.randomUUID().toString()), ctx);
        assertThat(rescate.estado()).isEqualTo("POR_ACREDITAR");

        aliado.modo(AliadoDoble.Modo.CAIDO); // cae el aliado y tambien el libro: no hay nada que pueda avanzar
        assertThat(cu125.sincronizar(rescate.rescateId(), ctx).estado()).isEqualTo("POR_ACREDITAR");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("10000.00");

        aliado.modo(AliadoDoble.Modo.NORMAL);
        libro.fallarEn("acreditar", false);
        assertThat(cu125.sincronizar(rescate.rescateId(), ctx).estado()).isEqualTo("LIQUIDADO");
        assertThat(cu125.sincronizar(rescate.rescateId(), ctx).estado()).isEqualTo("LIQUIDADO");

        assertThat(libro.total(cuenta)).isEqualByComparingTo("20177.53");
        assertThat(libro.asientos().stream()
                        .filter(a -> a.concepto().equals("CREDITO_RESCATE"))
                        .toList())
                .hasSize(1);
    }

    @Test
    @DisplayName("Traza: todos los eventos de una orden llevan el MISMO correlationId, el de la sesion que la origino")
    void trazaDePuntaAPunta() {
        conSaldo("5000.00");
        VistaOrden orden = ordenar(PRODUCTO_DPF, "1000.00");

        List<String> correlaciones = dslFixtura
                .fetch(
                        "select distinct correlation_id::text from inversiones.evento_dominio where agregado_id = ? or agregado_id = ?",
                        orden.ordenId(),
                        orden.posicionId())
                .getValues(0, String.class);
        List<String> tipos = dslFixtura
                .fetch(
                        "select tipo from inversiones.evento_dominio where agregado_id = ? or agregado_id = ? order by tipo",
                        orden.ordenId(),
                        orden.posicionId())
                .getValues(0, String.class);

        assertThat(correlaciones).hasSize(1).containsExactly(ctx.traza().id());
        assertThat(tipos)
                .contains(
                        "inversiones.orden_creada", "inversiones.saldo_reservado", "inversiones.posicion_constituida");
    }

    @Test
    @DisplayName(
            "Cuadre global desde el mayor: sea cual sea el camino (ordenes, rescates, rechazos), billetera + costo de las posiciones abiertas = saldo inicial + resultado realizado")
    void cuadreGlobal() {
        conSaldo("20000.00");
        // Una orden confirmada, una rechazada, y un fondo parcialmente rescatado con ganancia.
        posicionConfirmada(PRODUCTO_DPF, "3000.00");
        aliado.modo(AliadoDoble.Modo.RECHAZA);
        ordenar(PRODUCTO_DPF, "2000.00");
        aliado.modo(AliadoDoble.Modo.NORMAL);
        UUID fondo = posicionConfirmada(productoFondo, "1000.00");
        aliado.valorCuotaAplicado("102.000000").rescateFondoInmediato(true);
        cu124.solicitar(
                new EntradaRescate(
                        fondo,
                        Optional.of(new BigDecimal("4.000000")),
                        UUID.randomUUID().toString()),
                ctx);

        BigDecimal costoDeLasAbiertas = dslFixtura
                .fetchOne(
                        "select coalesce(sum(costo_base_abierto), 0) from ("
                                + " select posicion_inversion_id, sum(case when tipo = 'SUSCRIPCION' then costo_base else -costo_base end) as costo_base_abierto"
                                + " from inversiones.comprobante_inversion where usuario_id = ? group by posicion_inversion_id) t",
                        usuario)
                .get(0, BigDecimal.class);
        BigDecimal resultadoRealizado = dslFixtura
                .fetchOne(
                        "select coalesce(sum(interes - impuesto - comision - perdida_realizada), 0) from inversiones.comprobante_inversion where usuario_id = ?",
                        usuario)
                .get(0, BigDecimal.class);

        // 20000 inicial = billetera + costo abierto - resultado realizado
        assertThat(libro.total(cuenta).add(costoDeLasAbiertas).subtract(resultadoRealizado))
                .isEqualByComparingTo("20000.00");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");
        assertThat(libro.sumaDeDebe())
                .isEqualByComparingTo("4407.20"); // 3000 + 1000 de los debitos y 407,20 del credito
    }
}

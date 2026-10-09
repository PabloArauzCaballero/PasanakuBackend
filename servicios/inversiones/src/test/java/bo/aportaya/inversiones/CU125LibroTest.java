package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.aplicacion.VistaComprobante;
import bo.aportaya.inversiones.aplicacion.VistaRescate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M6 y H11.S1.M5 · rescate demorado de punta a punta, el libro caido al acreditar, la
 * reversa por compensacion y los comprobantes separables que cuadran con el mayor.
 */
class CU125LibroTest extends BaseDeLiquidacion {

    @Test
    @DisplayName(
            "Dado un rescate de cuotas cuyo importe no es cuotas x valor · Cuando el aliado confirma el rescate · Entonces AP-CU125-02: no se acredita y la posicion no cambia")
    void importeDeCuotasIncoherente() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");
        aliado.rescateFondoInmediato(true).valorCuotaAplicado("101.000000").distorsionDeMonto("0.01");

        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, "4.000000"), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU125-02"));

        assertThat(dslFixtura
                        .fetchOne("select cuotas from inversiones.posicion_inversion where id = ?", posicion)
                        .get(0, BigDecimal.class))
                .isEqualByComparingTo("10.000000");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("19000.00");
    }

    @Test
    @DisplayName(
            "Dado un valor del aliado distinto del que ya tenemos publicado para esa fecha · Cuando el aliado confirma el rescate · Entonces AP-CU125-02")
    void valorContradiceLoPublicado() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");
        aliado.publicar(productoFondo, DIA0, "105.000000");
        cu123.sincronizarValores(sistema());
        aliado.rescateFondoInmediato(true).valorCuotaAplicado("110.000000").fechaValorDeRescate(DIA0);

        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, "4.000000"), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU125-02"));
    }

    // ------------------------------------------------------------------ rescate demorado y libro
    @Test
    @DisplayName(
            "Dado un rescate demorado · Cuando el aliado confirma mas tarde · Entonces estuvo PENDIENTE hasta confirmar, despues liquida, descuenta las cuotas y acredita una sola vez")
    void rescateDemoradoDePuntaAPunta() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");
        aliado.valorCuotaAplicado("102.000000");
        VistaRescate pendiente = cu124.solicitar(pedir(posicion, "4.000000"), ctx);
        assertThat(pendiente.estado()).isEqualTo("PENDIENTE");

        VistaRescate aunPendiente = cu125.sincronizar(pendiente.rescateId(), ctx);
        assertThat(aunPendiente.estado())
                .as("si el aliado no confirmo, sigue pendiente")
                .isEqualTo("PENDIENTE");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("19000.00");

        aliado.confirmarPendientes();
        VistaRescate liquidado = cu125.sincronizar(pendiente.rescateId(), ctx);
        VistaRescate otraVez = cu125.sincronizar(pendiente.rescateId(), ctx);

        // A mano: 4 x 102 = 408,00 ; costo liberado 1000 x 4 / 10 = 400,00 ; ganancia 8,00 ; exito (102-100) x 4 x 10 %
        // = 0,80
        assertThat(liquidado.estado()).isEqualTo("LIQUIDADO");
        assertThat(otraVez.estado()).isEqualTo("LIQUIDADO");
        var d = liquidacion(posicion).d();
        assertThat(d.principal()).isEqualByComparingTo("400.00");
        assertThat(d.interes()).isEqualByComparingTo("8.00");
        assertThat(d.comision()).isEqualByComparingTo("0.80");
        assertThat(d.neto()).isEqualByComparingTo("407.20");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("19407.20");
        assertThat(libro.veces("ACREDITAR:" + pendiente.rescateId())).isEqualTo(1);
        assertThat(dslFixtura
                        .fetchOne("select cuotas from inversiones.posicion_inversion where id = ?", posicion)
                        .get(0, BigDecimal.class))
                .isEqualByComparingTo("6.000000");
        assertThat(costoBase(posicion)).isEqualByComparingTo("600.00");
        // Cuadre DESDE el mayor: el efecto neto sobre la billetera es el que dicen los comprobantes.
        assertThat(efectoNetoSegunElMayor())
                .isEqualByComparingTo(libro.total(cuenta).subtract(new BigDecimal("20000.00")));
    }

    @Test
    @DisplayName(
            "Dado el libro caido AL ACREDITAR · Cuando se liquida el rescate · Entonces el rescate queda POR_ACREDITAR (liquidado y descontado, pero no disponible); al volver, se acredita una vez")
    void libroCaidoAlAcreditar() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");
        aliado.rescateFondoInmediato(true).valorCuotaAplicado("100.000000");
        libro.fallarEn("acreditar", true);

        VistaRescate r = cu124.solicitar(pedir(posicion, null), ctx);

        assertThat(r.estado()).isEqualTo("POR_ACREDITAR");
        assertThat(r.mensaje()).contains("falta acreditarlo");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("19000.00");
        assertThat(costoBase(posicion))
                .as("la posicion ya no tiene costo: no se cuenta dos veces")
                .isEqualByComparingTo("0.00");

        libro.fallarEn("acreditar", false);
        VistaRescate despues = cu125.sincronizar(r.rescateId(), ctx);
        cu125.sincronizar(r.rescateId(), ctx);

        assertThat(despues.estado()).isEqualTo("LIQUIDADO");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("20000.00");
        assertThat(libro.asientos().stream()
                        .filter(a -> a.concepto().equals("CREDITO_RESCATE"))
                        .count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "compensa: una orden rechazada deshace la retencion con una operacion nueva (liberar), sin editar nada del historial")
    void reversaPorCompensacion() {
        conSaldo("5000.00");
        aliado.modo(AliadoDoble.Modo.RECHAZA);

        var orden = ordenar(PRODUCTO_DPF, "1000.00");

        assertThat(orden.estado()).isEqualTo("RECHAZADA");
        assertThat(libro.disponible(cuenta)).isEqualByComparingTo("5000.00");
        assertThat(contar(
                        "select count(*)::int from inversiones.instruccion_libro where origen_id = ? and tipo in ('RETENER', 'LIBERAR') and estado = 'APLICADA'",
                        orden.ordenId()))
                .isEqualTo(2);
        assertThat(libro.asientos())
                .as("una retencion y su liberacion no generan asiento: el saldo total nunca se movio")
                .isEmpty();
    }

    // ------------------------------------------------------------------ comprobantes (H11.M5)
    @Test
    @DisplayName(
            "Dado un rescate liquidado con tratamiento definido · Cuando se emite el comprobante · Entonces principal, interes, impuesto y comision son separables y cuadran con el mayor")
    void comprobantesSeparables() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        AHORA.set(LUNES.plusSeconds(180L * 86400));
        aliado.liquidacionDpf("197.26", "19.73", 180);
        cu124.solicitar(pedir(posicion, null), ctx);

        List<VistaComprobante> lista = cu123.comprobantes(posicion, ctx);

        assertThat(lista).extracting(VistaComprobante::tipo).containsExactly("SUSCRIPCION", "LIQUIDACION");
        assertThat(lista).extracting(VistaComprobante::sentidoTitular).containsExactly("DEBITO", "CREDITO");
        for (VistaComprobante c : lista) {
            var d = c.d();
            assertThat(d.principal().add(d.interes()).subtract(d.impuesto()).subtract(d.comision()))
                    .isEqualByComparingTo(d.neto());
            assertThat(c.origen()).isEqualTo("SINTETICO");
        }
        assertThat(efectoNetoSegunElMayor()).isEqualByComparingTo("177.53");
        assertThat(libro.total(cuenta).subtract(new BigDecimal("20000.00"))).isEqualByComparingTo("177.53");
    }

    @Test
    @DisplayName(
            "rechaza por R-INV-03: un comprobante que no descompone sin residuo, y por R-AUD-01 un UPDATE o DELETE (append-only)")
    void baseProtegeLosComprobantes() {
        conSaldo("20000.00");
        var orden = ordenar(PRODUCTO_DPF, "1000.00");
        String insercion =
                "insert into inversiones.comprobante_inversion (id, posicion_inversion_id, usuario_id, tipo, origen_id, sentido_titular,"
                        + " principal, interes, impuesto, comision, perdida_realizada, neto, costo_base, moneda, origen_datos, emitido_en)"
                        + " select gen_random_uuid(), posicion_inversion_id, usuario_id, tipo, gen_random_uuid(), sentido_titular, principal, interes,"
                        + " impuesto, comision, perdida_realizada, neto + ?, costo_base, moneda, origen_datos, emitido_en"
                        + " from inversiones.comprobante_inversion where origen_id = ?";
        assertThat(rechazaLaBase(insercion, new BigDecimal("0.01"), orden.ordenId()))
                .contains("ck_comprobante_inv_neto");
        assertThat(rechazaLaBase(insercion, BigDecimal.ZERO, orden.ordenId()))
                .as("control negativo: el mismo insert, coherente, entra")
                .isEmpty();
        assertThat(rechazaLaBase(
                        "update inversiones.comprobante_inversion set neto = 1 where origen_id = ?", orden.ordenId()))
                .contains("R-AUD-01");
        assertThat(rechazaLaBase("delete from inversiones.comprobante_inversion where origen_id = ?", orden.ordenId()))
                .contains("R-AUD-01");
    }

    @Test
    @DisplayName(
            "rechaza por R-INV-07 y R-INV-08: una conciliacion marcada CONCILIADA con cifras distintas y una comision sobre una base inexistente")
    void baseProtegeConciliacionYComision() {
        conSaldo("20000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "10000.00");
        UUID rescate = tx.en(() -> datos.conContexto(
                ctx,
                d -> rescates.crear(
                        d,
                        posicion,
                        usuario,
                        "VENCIMIENTO",
                        null,
                        UUID.randomUUID().toString(),
                        "h",
                        null,
                        java.time.OffsetDateTime.now())));

        assertThat(rechazaLaBase(
                        "insert into inversiones.conciliacion_interes (id, posicion_inversion_id, rescate_inversion_id, periodo_desde, periodo_hasta,"
                                + " interes_devengado, interes_externo, retencion_calculada, retencion_externa, moneda, estado, conciliada_en)"
                                + " values (gen_random_uuid(), ?, ?, current_date, current_date, 1.00, 2.00, 0, 0, 'BOB', 'CONCILIADA', now())",
                        posicion,
                        rescate))
                .contains("ck_conciliacion_interes_estado");
        assertThat(rechazaLaBase(
                        "insert into inversiones.comision_exito (id, posicion_inversion_id, rescate_inversion_id, cuotas, valor_cuota, marca_maxima_previa,"
                                + " tasa, base_elegible, comision, cuota_neta, marca_maxima_nueva, moneda, origen_datos, calculada_en)"
                                + " values (gen_random_uuid(), ?, ?, 1, 100, 100, 0.1, 0.00, 5.00, 100, 100, 'BOB', 'SINTETICO', now())",
                        posicion,
                        rescate))
                .contains("ck_comision_exito_sin_base");
    }
}

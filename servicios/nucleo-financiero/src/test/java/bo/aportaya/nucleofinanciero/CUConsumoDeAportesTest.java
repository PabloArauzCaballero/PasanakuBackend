package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso;
import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.Resultado;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril I (quinta vuelta) · {@code aportes.aporte_cobrado} MUEVE el dinero (regla 91 y 98).
 *
 * <p>El productor (aportes) es un doble de CONTRATO: el evento se arma con los campos exactos que
 * {@code CU21CobrarAporte} emite. Cubre el camino correcto, el cuadre desde el libro y desde el mayor, el evento duplicado, el
 * fuera de orden y la reversa. Los rechazos (moneda, cuentas, saldo, canal, atomicidad, concurrencia) estan en
 * {@link CUConsumoDeAportesRechazosTest}. El recorrido con aportes, Kafka y el libro reales vive en
 * {@code despliegue/ensayos/integracion}.
 */
class CUConsumoDeAportesTest extends BaseDeAportes {

    @Test
    @DisplayName("Dado un participante con Bs 1.000 y un aporte cobrado de Bs 500 · Cuando nucleo consume el evento ·"
            + " Entonces su billetera baja a 500, la del grupo sube a 500 y el mayor asienta DEBE usuarios / HABER"
            + " grupos por 500")
    void elAporteMueveLaBilleteraYElMayor() {
        var a = aporte("1000.00");
        var e = cobrado(a, "500.00");

        var salida = procesar(e);

        assertThat(salida.resultado()).isEqualTo(Resultado.APLICADO);
        // El LIBRO de billeteras.
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("500.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("500.00");
        // La transaccion: tipo, origen, monto, grupo, clave por pago y el asiento YA enlazado.
        var tx = dsl.fetchOne(
                """
                SELECT tipo, origen_tipo, origen_id, monto_total, moneda, grupo_id, asiento_contable_id, clave_idempotencia, estado
                  FROM nucleo_financiero.transaccion_billetera WHERE id = ?
                """,
                salida.transaccionId().orElseThrow());
        assertThat(tx.get(0)).isEqualTo("APORTE_A_GRUPO");
        assertThat(tx.get(1)).isEqualTo("OBLIGACION_APORTE");
        assertThat(tx.get(2)).isEqualTo(a.obligacion());
        assertThat((BigDecimal) tx.get(3)).isEqualByComparingTo("500.00");
        assertThat(tx.get(4)).isEqualTo("BOB");
        assertThat(tx.get(5)).isEqualTo(a.grupo());
        assertThat(tx.get(6)).isEqualTo(salida.asientoId().orElseThrow());
        assertThat(tx.get(7)).isEqualTo("aporte-cobrado:" + e.pagoId());
        assertThat(tx.get(8)).isEqualTo("APLICADA");
        // El ASIENTO: apunta a la transaccion, y es debe usuarios / haber grupos por el mismo importe.
        var asiento = dsl.fetchOne(
                "SELECT origen_tipo, origen_id, grupo_id, estado FROM nucleo_financiero.asiento_contable WHERE id = ?",
                salida.asientoId().orElseThrow());
        assertThat(asiento.get(0)).isEqualTo("PAGO");
        assertThat(asiento.get(1)).isEqualTo(salida.transaccionId().orElseThrow());
        assertThat(asiento.get(2)).isEqualTo(a.grupo());
        assertThat(asiento.get(3)).isEqualTo("CONFIRMADO");
        var partidas = dsl.fetch(
                """
                SELECT c.codigo, m.debe, m.haber, m.moneda FROM nucleo_financiero.movimiento_contable m
                  JOIN nucleo_financiero.cuenta_contable c ON c.id = m.cuenta_id WHERE m.asiento_id = ? ORDER BY m.debe DESC
                """,
                salida.asientoId().orElseThrow());
        assertThat(partidas).hasSize(2);
        assertThat(partidas.get(0).get(0)).isEqualTo(MAPEO.get("SALDO_USUARIOS.BOB"));
        assertThat((BigDecimal) partidas.get(0).get(1)).isEqualByComparingTo("500.00");
        assertThat(partidas.get(1).get(0)).isEqualTo(MAPEO.get("SALDO_GRUPOS.BOB"));
        assertThat((BigDecimal) partidas.get(1).get(2)).isEqualByComparingTo("500.00");
        assertThat(partidas).allSatisfy(p -> assertThat(p.get(3)).isEqualTo("BOB"));
        // El hecho se publica en la misma transaccion (outbox, regla 98.3).
        assertThat(contar(
                        """
                        SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE tipo = 'nucleo_financiero.aporte_aplicado' AND agregado_id = ?
                        """,
                        a.obligacion()))
                .isEqualTo(1);
        System.out.println("ASIENTO | aporte_cobrado pago=" + e.pagoId() + " | DEBE " + MAPEO.get("SALDO_USUARIOS.BOB")
                + " 500.00 | HABER " + MAPEO.get("SALDO_GRUPOS.BOB") + " 500.00 | billetera usuario 1000.00 -> "
                + saldoDelLibro(a.billetera()) + " | grupo 0.00 -> " + saldoDelLibro(a.billeteraDelGrupo()));
    }

    @Test
    @DisplayName(
            "cuadre: el saldo del mayor (haber - debe) de las dos cuentas del plan coincide con el de las billeteras y"
                    + " con la cache, y el asiento cuadra")
    void cuadreDesdeElMayor() {
        var a = aporte("2000.00");
        // Las cuentas del plan son compartidas por toda la clase: el delta de ESTE aporte es lo que se compara.
        BigDecimal usuariosAntes = saldoDelMayor(MAPEO.get("SALDO_USUARIOS.BOB"));
        BigDecimal gruposAntes = saldoDelMayor(MAPEO.get("SALDO_GRUPOS.BOB"));

        var primero = procesar(cobrado(a, "700.00"));
        var segundo = procesar(cobrado(a, "300.00"));

        BigDecimal deltaUsuarios =
                saldoDelMayor(MAPEO.get("SALDO_USUARIOS.BOB")).subtract(usuariosAntes);
        BigDecimal deltaGrupos = saldoDelMayor(MAPEO.get("SALDO_GRUPOS.BOB")).subtract(gruposAntes);
        // Pasivo acreedor: lo que el usuario debita baja su pasivo (-1000) y el del grupo sube (+1000).
        assertThat(deltaUsuarios).isEqualByComparingTo("-1000.00");
        assertThat(deltaGrupos).isEqualByComparingTo("1000.00");
        assertThat(deltaUsuarios.add(deltaGrupos)).isEqualByComparingTo("0.00");
        // Mayor y libro cuentan lo mismo.
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo(deltaGrupos);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("1000.00");
        // La cache de la cuenta contable (R-CTB-09) coincide con el mayor.
        BigDecimal cache = (BigDecimal) dsl.fetchOne(
                        "SELECT saldo FROM nucleo_financiero.cuenta_contable WHERE codigo = ?",
                        MAPEO.get("SALDO_GRUPOS.BOB"))
                .get(0);
        assertThat(cache).isEqualByComparingTo(saldoDelMayor(MAPEO.get("SALDO_GRUPOS.BOB")));
        // Cada asiento cuadra: SUM(debe) = SUM(haber).
        for (var s : java.util.List.of(primero, segundo)) {
            var sumas = dsl.fetchOne(
                    "SELECT SUM(debe), SUM(haber) FROM nucleo_financiero.movimiento_contable WHERE asiento_id = ?",
                    s.asientoId().orElseThrow());
            assertThat((BigDecimal) sumas.get(0)).isEqualByComparingTo((BigDecimal) sumas.get(1));
        }
        // Cada transaccion de billetera cuadra: debitos = creditos.
        assertThat(contar(
                        """
                        SELECT count(*) FROM (SELECT transaccion_id FROM nucleo_financiero.movimiento_billetera
                         WHERE transaccion_id IN (?, ?) GROUP BY transaccion_id
                        HAVING SUM(CASE WHEN sentido = 'CREDITO' THEN monto ELSE -monto END) <> 0) d
                        """,
                        primero.transaccionId().orElseThrow(),
                        segundo.transaccionId().orElseThrow()))
                .isZero();
        System.out.println("CUADRE | mayor usuarios " + deltaUsuarios + " | mayor grupos " + deltaGrupos
                + " | libro grupo " + saldoDelLibro(a.billeteraDelGrupo()) + " | cache " + cache);
    }

    @Test
    @DisplayName(
            "evento duplicado: el mismo aporte_cobrado entregado dos veces mueve la plata UNA vez (una transaccion, un"
                    + " asiento, una marca)")
    void eventoDuplicado() {
        var a = aporte("1000.00");
        var e = cobrado(a, "500.00");

        var primera = procesar(e);
        var segunda = procesar(e);

        assertThat(primera.resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(segunda.resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(transacciones(a.obligacion())).isEqualTo(1);
        assertThat(asientosDeAportes(a.grupo())).isEqualTo(1);
        assertThat(marcas(e.eventoId())).isEqualTo(1);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("500.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName(
            "evento duplicado: el MISMO pago republicado con OTRO id de evento no mueve la plata de nuevo (la clave"
                    + " del libro es el pago)")
    void mismoPagoConOtroEvento() {
        var a = aporte("1000.00");
        var original = cobrado(a, "500.00");

        var primera = procesar(original);
        var republicada = procesar(reenviado(original));

        assertThat(primera.resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(republicada.resultado()).isEqualTo(Resultado.YA_APLICADO);
        assertThat(republicada.transaccionId()).isEqualTo(primera.transaccionId());
        assertThat(transacciones(a.obligacion())).isEqualTo(1);
        assertThat(asientosDeAportes(a.grupo())).isEqualTo(1);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName(
            "fuera de orden: el segundo cobro parcial de una obligacion consumido ANTES que el primero deja el mismo"
                    + " saldo; y un aporte llegado antes del fondeo se rechaza sin efectos, queda consumido y el fondeo"
                    + " tardio no lo resucita")
    void fueraDeOrden() {
        // (a) Dos pagos parciales de la misma obligacion, en orden invertido: las sumas son conmutativas.
        var a = aporte("1000.00");
        var primero = cobrado(a, "300.00");
        var segundo = cobrado(a, "200.00");
        procesar(segundo);
        procesar(primero);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("500.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("500.00");
        assertThat(transacciones(a.obligacion())).isEqualTo(2);

        // (b) El aporte llega antes que el fondeo de la billetera (otro evento, otra cola). Segun el flujo 3b de
        // CU-21 el rechazo por saldo es DEFINITIVO: no se mueve nada, el evento queda consumido y se avisa una sola
        // vez para que `aportes` compense el pago (CUAporteNoAplicadoTest fija el sobre del aviso). El fondeo que
        // llega despues NO resucita el aporte: reprocesar el mismo evento es un duplicado.
        var sinFondos = aporte("0.00");
        var llegaPrimero = cobrado(sinFondos, "400.00");
        assertThat(procesar(llegaPrimero).resultado()).isEqualTo(Resultado.RECHAZADO_POR_SALDO);
        assertThat(transacciones(sinFondos.obligacion())).isZero();
        assertThat(asientosDeAportes(sinFondos.grupo())).isZero();
        assertThat(marcas(llegaPrimero.eventoId())).isEqualTo(1);

        fixtura.acreditar(sinFondos.billetera(), new BigDecimal("400.00"));
        assertThat(procesar(llegaPrimero).resultado()).isEqualTo(Resultado.DUPLICADO);

        assertThat(saldoDelLibro(sinFondos.billetera())).isEqualByComparingTo("400.00");
        assertThat(saldoDelLibro(sinFondos.billeteraDelGrupo())).isEqualByComparingTo("0.00");
        assertThat(transacciones(sinFondos.obligacion())).isZero();
        assertThat(asientosDeAportes(sinFondos.grupo())).isZero();
    }

    @Test
    @DisplayName("compensa: la reversa del aporte es un CONTRA-ASIENTO y un REVERSO del libro; nada se edita y mayor y"
            + " billeteras vuelven al punto de partida")
    void reversaDelAporte() {
        var a = aporte("1000.00");
        var salida = procesar(cobrado(a, "500.00"));
        BigDecimal usuariosConAporte = saldoDelMayor(MAPEO.get("SALDO_USUARIOS.BOB"));
        BigDecimal gruposConAporte = saldoDelMayor(MAPEO.get("SALDO_GRUPOS.BOB"));

        // El mayor: contra-asiento (CU-24), enlazado al original por asiento_reversa_id.
        var ctx = ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
        var contra = transaccion.execute(t -> new bo.aportaya.plataforma.datos.Datos(dsl)
                .conContexto(ctx, d -> new bo.aportaya.nucleofinanciero.aplicacion.CU24RegistrarAsiento(
                                new bo.aportaya.nucleofinanciero.infraestructura.CuentaContableRepositorio(),
                                new bo.aportaya.nucleofinanciero.infraestructura.AsientoRepositorio(),
                                new bo.aportaya.plataforma.mensajeria.Outbox("nucleo_financiero"),
                                bo.aportaya.plataforma.dominio.Reloj.delSistema())
                        .reversar(d, salida.asientoId().orElseThrow(), "reversa del aporte", ctx)));
        // La billetera: reverso del libro (CU-14), cuatro ojos: quien autoriza no es quien solicita.
        UUID autoriza = fixtura.usuario();
        var reverso = transaccion.execute(t -> reversoCU.ejecutar(
                new EntradaReverso(
                        "rev-aporte-" + UUID.randomUUID(),
                        salida.transaccionId().orElseThrow(),
                        "ANULACION",
                        "Reversa del aporte cobrado por error operativo documentado",
                        autoriza),
                contextoDe(fixtura.usuario())));

        assertThat(reverso.generaObligacionDeRestitucion()).isFalse();
        // El original sigue INTACTO: dos movimientos de billetera y dos contables; el espejo es nuevo.
        assertThat(contar(
                        "SELECT count(*) FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id = ?",
                        salida.transaccionId().orElseThrow()))
                .isEqualTo(2);
        assertThat(contar(
                        "SELECT count(*) FROM nucleo_financiero.movimiento_contable WHERE asiento_id = ?",
                        salida.asientoId().orElseThrow()))
                .isEqualTo(2);
        assertThat(contar(
                        """
                        SELECT count(*) FROM nucleo_financiero.asiento_contable WHERE id = ? AND asiento_reversa_id = ?
                        """,
                        contra.asientoId(),
                        salida.asientoId().orElseThrow()))
                .isEqualTo(1);
        // Saldos de vuelta: el mayor se compensa por el delta del aporte y las billeteras vuelven a 1000 / 0.
        assertThat(saldoDelMayor(MAPEO.get("SALDO_USUARIOS.BOB")).subtract(usuariosConAporte))
                .isEqualByComparingTo("500.00");
        assertThat(saldoDelMayor(MAPEO.get("SALDO_GRUPOS.BOB")).subtract(gruposConAporte))
                .isEqualByComparingTo("-500.00");
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("1000.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("0.00");
        System.out.println("REVERSA | contra-asiento " + contra.asientoId() + " del asiento "
                + salida.asientoId().orElseThrow() + " | reverso del libro " + reverso.transaccionReversoId());
    }
}

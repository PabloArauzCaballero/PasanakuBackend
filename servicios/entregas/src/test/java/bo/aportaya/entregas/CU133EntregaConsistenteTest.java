package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.entregas.aplicacion.CU18RegistrarCuentaDestino.EntradaRegistro;
import bo.aportaya.entregas.aplicacion.CU28EmitirDesembolso.EntradaOrden;
import bo.aportaya.entregas.aplicacion.CU28EmitirDesembolso.EntradaRespuesta;
import bo.aportaya.entregas.dominio.AsientoDeEntrega;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H8.S1.M4 · Entregar el pozo completo: el LIBRO (asiento propuesto), la CONFIRMACION externa (referencia del
 * proveedor) y el COMPROBANTE (la entrega y su orden) tienen que decir lo mismo, y decirlo una sola vez.
 *
 * <p>El mayor es un DOBLE ({@link MayorDeDoble}): nucleo-financiero todavia no consume este evento. Lo que
 * queda verificado es el contrato de la carga y la coincidencia de las tres vistas dentro de este servicio.
 */
class CU133EntregaConsistenteTest extends BaseDePozoCompleto {

    private record Entregada(UUID entregaId, UUID ordenId, ContextoSesion ctx) {}

    /** Pozo 6.000 con 5.000 confirmados + 1.000 de la empresa, autorizado, ordenado y acreditado por el proveedor. */
    private Entregada entregarElPozo(Caso c, String referencia) {
        aportesConfirma(c, "5000.00");
        garantiaDoble.conDisponible(bob("8000.00"));
        var fondeo = fondear(c, "fondeo-" + c.e().turnoId());
        var cuenta = transaccion.execute(t -> cuentaCU.registrar(
                new EntradaRegistro(
                        "AHORRO",
                        "Banco de Prueba",
                        "4012345678",
                        "cifrado:x",
                        "Maria Fernanda Quispe",
                        "8123456",
                        "Maria Fernanda Quispe",
                        "8123456",
                        "BOB"),
                c.ctx()));
        transaccion.execute(t -> cuentaCU.verificar(cuenta.cuentaId(), "MICRODEPOSITO", c.ctx()));
        dsl.execute(
                "UPDATE entregas.cuenta_bancaria_beneficiario SET bloqueada_hasta = now() - interval '1 hour' WHERE id = ?",
                cuenta.cuentaId());
        ContextoSesion supervisor = contextoDe(fixtura.usuario());
        transaccion.execute(t -> entregaCU.autorizar(fondeo.entregaId(), supervisor));
        var orden = transaccion.execute(t -> desembolsoCU.emitir(
                new EntradaOrden(
                        fondeo.entregaId(),
                        fixtura.proveedor(),
                        cuenta.cuentaId(),
                        "Entrega del pozo completo",
                        "des-" + c.e().turnoId(),
                        true),
                c.ctx()));
        transaccion.execute(t -> desembolsoCU.anotarRespuesta(
                new EntradaRespuesta(
                        orden.ordenId(),
                        OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
                        true,
                        referencia,
                        null,
                        "OK"),
                c.ctx()));
        return new Entregada(fondeo.entregaId(), orden.ordenId(), c.ctx());
    }

    @Test
    @DisplayName(
            "Dado fondeo completo (Bs 5.000 confirmados + Bs 1.000 de la empresa) · Cuando el proveedor confirma el desembolso · Entonces ledger propuesto, confirmacion externa y comprobante coinciden en Bs 6.000 de principal, una sola vez")
    void ledgerConfirmacionYComprobanteCoinciden() {
        var c = caso();
        var e = entregarElPozo(c, "REF-BANCO-6000");

        // COMPROBANTE: la entrega entera y la orden, al centavo, sin deducciones.
        var entrega = dsl.fetchOne(
                "SELECT estado, monto_bolsa_bruto, total_deducciones, monto_neto_a_entregar FROM entregas.entrega_fondo WHERE id = ?",
                e.entregaId());
        var orden = dsl.fetchOne(
                "SELECT estado, monto, referencia_proveedor FROM entregas.orden_desembolso WHERE id = ?", e.ordenId());
        assertThat(entrega.get("estado", String.class)).isEqualTo("ENTREGADA");
        assertThat(entrega.get("monto_bolsa_bruto", BigDecimal.class)).isEqualByComparingTo("6000.00");
        assertThat(entrega.get("total_deducciones", BigDecimal.class)).isEqualByComparingTo("0.00");
        assertThat(entrega.get("monto_neto_a_entregar", BigDecimal.class)).isEqualByComparingTo("6000.00");
        assertThat(orden.get("monto", BigDecimal.class)).isEqualByComparingTo("6000.00");
        // CONFIRMACION EXTERNA: la referencia del proveedor, en la orden y en el evento.
        assertThat(orden.get("referencia_proveedor", String.class)).isEqualTo("REF-BANCO-6000");

        // LEDGER: el asiento propuesto viaja en el evento, cuadra y su origen es la entrega.
        var mayor = new MayorDeDoble();
        JsonNode evento = MayorDeDoble.leer(dsl.fetchOne(
                        "SELECT payload::text FROM entregas.evento_dominio WHERE tipo = 'entregas.desembolso_acreditado' AND agregado_id = ?",
                        e.ordenId())
                .get(0, String.class));
        assertThat(evento.get("referenciaProveedor").asText()).isEqualTo("REF-BANCO-6000");
        assertThat(new BigDecimal(evento.get("monto").asText())).isEqualByComparingTo("6000.00");
        assertThat(evento.get("asiento").get("origenTipo").asText()).isEqualTo("ENTREGA");
        assertThat(evento.get("asiento").get("origenId").asText())
                .isEqualTo(e.entregaId().toString());
        mayor.consumirOutbox(dsl, e.ordenId());
        assertThat(mayor.asientos()).isEqualTo(1);
        assertThat(mayor.saldo("POZO_GRUPO")).isEqualByComparingTo("6000.00");
        assertThat(mayor.saldo("DESEMBOLSO_CONFIRMADO")).isEqualByComparingTo("-6000.00");
        assertThat(mayor.sumaDeSaldos()).isEqualByComparingTo("0");

        // UNA SOLA VEZ: el mismo evento otra vez, y el mismo origen bajo otro evento, no se registran de nuevo.
        assertThat(mayor.registrar(UUID.randomUUID(), evento)).isFalse();
        assertThat(mayor.asientos()).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM entregas.evento_dominio WHERE tipo = 'entregas.desembolso_acreditado' AND payload->>'entregaId' = ?",
                        e.entregaId().toString()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: la confirmacion duplicada del proveedor no acredita dos veces ni emite un segundo asiento")
    void confirmacionDuplicada() {
        var c = caso();
        var e = entregarElPozo(c, "REF-BANCO-DUP");

        var otra = transaccion.execute(t -> desembolsoCU.anotarRespuesta(
                new EntradaRespuesta(
                        e.ordenId(),
                        OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
                        true,
                        "REF-BANCO-DUP",
                        null,
                        "OK"),
                c.ctx()));

        assertThat(otra.esNuevo()).isFalse();
        assertThat(contar(
                        "SELECT count(*)::int FROM entregas.evento_dominio WHERE tipo = 'entregas.desembolso_acreditado' AND agregado_id = ?",
                        e.ordenId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "rechaza un asiento de entrega con importe cero o negativo: no sale de este servicio algo que no cuadra")
    void rechazaAsientoSinImporte() {
        assertThatThrownBy(() -> new AsientoDeEntrega(UUID.randomUUID(), bob("0.00")))
                .isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> new AsientoDeEntrega(UUID.randomUUID(), bob("-5.00")))
                .isInstanceOf(ErrorDeDominio.class);
        var valido = new AsientoDeEntrega(UUID.randomUUID(), bob("6000.00")).partidas();
        assertThat(valido.get(0).debe()).isEqualTo(valido.get(1).haber());
    }
}

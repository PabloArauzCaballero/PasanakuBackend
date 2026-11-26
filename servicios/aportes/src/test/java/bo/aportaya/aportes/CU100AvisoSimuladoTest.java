package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU100EmitirAvisoDePagoSimulado;
import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela.Resultado;
import bo.aportaya.aportes.aplicacion.CU21PagarConSaldo;
import bo.aportaya.aportes.infraestructura.AuditoriaRepositorio;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.aportes.infraestructura.pasarela.simulada.PasarelaSimulada;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ciclo completo contra PostgreSQL real y el DOBLE de la pasarela (regla 65): pagar con saldo (CU-21 abre la orden
 * simulada) -> disparar el aviso -> CU-100 real verifica, deduplica y concilia. Tres niveles. No prueba ningun proveedor.
 */
class CU100AvisoSimuladoTest extends BaseDeAportes {

    private static final String PROVEEDOR = "QR_TEST";

    private PasarelaSimulada pasarela;
    private CU100EmitirAvisoDePagoSimulado avisos;

    @BeforeEach
    void preparar() {
        pasarela = new PasarelaSimulada(
                codigo -> Optional.of(SECRETO_DE_PRUEBA), Reloj.delSistema(), new ObjectMapper(), PROVEEDOR);
        avisos = new CU100EmitirAvisoDePagoSimulado(
                pasarela, pasarela, webhookCU, new AuditoriaRepositorio(), new Datos(dsl), Reloj.delSistema());
        fixtura.proveedor(PROVEEDOR, false, 1);
    }

    @AfterEach
    void limpiar() {
        dsl.deleteFrom(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("aportes", "webhook_pasarela")))
                .execute();
        fixtura.limpiar();
    }

    /** CU-21 con la pasarela conectada: devuelve el id del pago, cuya referencia es la de la orden simulada. */
    private UUID pagoConOrden(String clave) {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "250.00", 10);
        var cu = new CU21PagarConSaldo(
                new Datos(dsl),
                new bo.aportaya.aportes.infraestructura.ObligacionRepositorio(),
                new PagoRepositorio(),
                cobroCU,
                pasarela);
        return transaccion
                .execute(t -> cu.pagarConSaldo(clave, obligacion.id(), Optional.empty(), true, contextoDe(usuario)))
                .pagoId();
    }

    private String referenciaDe(UUID pagoId) {
        return (String) dsl.fetchOne("SELECT referencia_proveedor FROM aportes.pago WHERE id = ?", pagoId)
                .get(0);
    }

    private Resultado emitir(String referencia) {
        return transaccion.execute(t -> avisos.emitir(referencia, contextoDe(fixtura.usuario())));
    }

    @Test
    @DisplayName(
            "correcto: orden -> aviso -> CU-100 PROCESADO sobre el pago, con rastro en la bitacora y la orden PAGADA")
    void cicloCompleto() {
        UUID pagoId = pagoConOrden("ciclo-1");
        String referencia = referenciaDe(pagoId);

        Resultado r = emitir(referencia);

        assertThat(r.estado()).isEqualTo(Resultado.Estado.PROCESADO);
        assertThat(r.pagoId()).isEqualTo(pagoId);
        assertThat(contar(
                        "SELECT count(*)::int FROM comun.bitacora_evento WHERE accion = 'CONCILIACION' AND entidad_id = ?",
                        r.webhookId()))
                .isEqualTo(1);
        assertThat(pasarela.consultarEstado(referencia).orElseThrow().estado())
                .isEqualTo(bo.aportaya.aportes.dominio.PasarelaDePagos.EstadoDeCobro.PAGADO);
    }

    @Test
    @DisplayName("limite: disparar dos veces el mismo aviso -> DUPLICADO, una sola fila de webhook y un solo pago")
    void idempotente() {
        UUID pagoId = pagoConOrden("ciclo-2");
        String referencia = referenciaDe(pagoId);

        Resultado primera = emitir(referencia);
        Resultado segunda = emitir(referencia);

        assertThat(primera.estado()).isEqualTo(Resultado.Estado.PROCESADO);
        assertThat(segunda.estado()).isEqualTo(Resultado.Estado.DUPLICADO);
        assertThat(segunda.webhookId()).isEqualTo(primera.webhookId());
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.webhook_pasarela WHERE clave_idempotencia = ?",
                        "sim-evt-" + referencia))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*)::int FROM aportes.pago WHERE referencia_proveedor = ?", referencia))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("invalido: una orden que el doble no emitio -> AP-CU100-11 y no se escribe nada")
    void ordenDesconocida() {
        assertThatThrownBy(() -> emitir("SIM-NO-EXISTE"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no emitio");
        assertThat(contar("SELECT count(*)::int FROM aportes.webhook_pasarela")).isZero();
    }

    @Test
    @DisplayName("invalido: proveedor inactivo o secreto distinto -> AP-CU100-12 explicativo, sin webhook escrito")
    void verificacionFallida() {
        UUID pagoId = pagoConOrden("ciclo-3");
        String referencia = referenciaDe(pagoId);
        dsl.execute("UPDATE aportes.proveedor_pago SET activo = false WHERE codigo = ?", PROVEEDOR);

        assertThatThrownBy(() -> emitir(referencia))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("activo con webhook");
        assertThat(contar("SELECT count(*)::int FROM aportes.webhook_pasarela")).isZero();
    }

    @Test
    @DisplayName("invalido: monto del pago distinto al de la orden queda DESCARTADO, no conciliado")
    void montoDistinto() {
        UUID pagoId = pagoConOrden("ciclo-4");
        String referencia = referenciaDe(pagoId);
        // el pago se concilia por referencia: forzar otro monto en el aviso lo hace el propio doble
        var aviso = pasarela.emitirWebhookDePago(referencia, "evt-otro-monto", Dinero.de("250.01", Moneda.BOB));
        Resultado r = transaccion.execute(
                t -> webhookCU.recibir(new bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela.Entrada(
                        aviso.proveedorCodigo(), aviso.cuerpoCrudo(), aviso.firma(), aviso.timestamp())));

        assertThat(r.estado()).isEqualTo(Resultado.Estado.DESCARTADO);
    }
}

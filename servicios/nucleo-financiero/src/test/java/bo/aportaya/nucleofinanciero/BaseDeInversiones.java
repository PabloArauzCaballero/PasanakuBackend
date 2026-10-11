package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.CU12AcreditarRescate;
import bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino;
import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo.EntradaRetencion;
import bo.aportaya.nucleofinanciero.aplicacion.PagoDeCompraEnMercado;
import bo.aportaya.nucleofinanciero.dominio.puertos.OfertasDelMercado;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentasDeSistemaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.RetencionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;

/**
 * Las operaciones que llaman OTROS SERVICIOS (inversiones y entregas): armadas a mano, con
 * {@code entregas} reemplazado por un doble declarado ({@link Ofertas}).
 */
abstract class BaseDeInversiones extends BaseDeBilletera {

    protected static FixturaDeSistema sistemaFx;
    protected static CU13EjecutarConDestino ejecucionCU;
    protected static CU12AcreditarRescate rescateCU;
    protected static PagoDeCompraEnMercado pagoCU;
    protected static Ofertas ofertas;

    @BeforeAll
    static void armarInversiones() {
        sistemaFx = new FixturaDeSistema(dslFixtura, fixtura);
        var cuentas = new CuentaBilleteraRepositorio();
        ejecucionCU = new CU13EjecutarConDestino(
                new Datos(dsl),
                cuentas,
                new RetencionRepositorio(),
                new CuentasDeSistemaRepositorio(),
                retencionCU,
                new LibroDeBilletera(),
                new Outbox("nucleo_financiero"),
                Reloj.delSistema());
        rescateCU = new CU12AcreditarRescate(
                new Datos(dsl),
                cuentas,
                new CuentasDeSistemaRepositorio(),
                new LibroDeBilletera(),
                new Outbox("nucleo_financiero"),
                Reloj.delSistema());
        ofertas = new Ofertas();
        pagoCU = new PagoDeCompraEnMercado(ejecucionCU, ofertas);
    }

    protected ContextoSesion comoServicio(String servicio) {
        return ContextoSesion.de(
                UUID.randomUUID(), servicio, new Traza(UUID.randomUUID().toString()));
    }

    /** La retencion de una orden de inversion, tal como la pide `inversiones` con el token del titular. */
    protected UUID retenerInversion(UUID usuario, UUID cuenta, String monto, UUID ordenId) {
        return transaccion
                .execute(t -> retencionCU.retener(
                        new EntradaRetencion(
                                cuenta,
                                Dinero.de(monto, Moneda.BOB),
                                "INVERSION_VOLUNTARIA",
                                Optional.empty(),
                                Optional.of("ORDEN_INVERSION"),
                                Optional.of(ordenId),
                                Optional.empty()),
                        "ret-" + ordenId,
                        contextoDe(usuario)))
                .retencionId();
    }

    protected UUID retenerCompra(UUID usuario, UUID cuenta, String monto, UUID ofertaId) {
        return transaccion
                .execute(t -> retencionCU.retener(
                        new EntradaRetencion(
                                cuenta,
                                Dinero.de(monto, Moneda.BOB),
                                "COMPRA_EN_MERCADO",
                                Optional.empty(),
                                Optional.of("OFERTA_MERCADO"),
                                Optional.of(ofertaId),
                                Optional.empty()),
                        "ret-" + ofertaId,
                        contextoDe(usuario)))
                .retencionId();
    }

    /** DOBLE de {@code entregas}: el contrato propuesto {@code GET /entregas/mercado/ofertas/{id}}. */
    static final class Ofertas implements OfertasDelMercado {
        private final Map<UUID, Oferta> porId = new HashMap<>();
        private boolean caido;

        void publica(UUID ofertaId, UUID vendedor, String precio, String estado) {
            porId.put(ofertaId, new Oferta(ofertaId, vendedor, Dinero.de(precio, Moneda.BOB), estado));
        }

        void seCae(boolean caido) {
            this.caido = caido;
        }

        @Override
        public Optional<Oferta> oferta(UUID ofertaId) {
            return caido ? Optional.empty() : Optional.ofNullable(porId.get(ofertaId));
        }
    }
}

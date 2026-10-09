package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RechazarRecarga;
import bo.aportaya.nucleofinanciero.aplicacion.CU11InstruirRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.CotizarOperacion;
import bo.aportaya.nucleofinanciero.aplicacion.OrdenesExistentes;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.aplicacion.RegistrarDiscrepancia;
import bo.aportaya.nucleofinanciero.aplicacion.RetirosConProveedor;
import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRecargas;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiros;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.DiscrepanciaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRecargaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.OrdenRetiroRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.Optional;
import java.util.UUID;

/** Arma el flujo de recarga completo (casos de uso transaccionales reales) sobre un proveedor dado. */
final class FlujosDePrueba {
    private FlujosDePrueba() {}

    /** Costo cero: estas pruebas fijan el comportamiento ante el proveedor, no la tarifa. */
    static RecargasConProveedor recargas(ProveedorDeRecargas proveedor) {
        return recargas(proveedor, new TarifaGratuita(), false);
    }

    /** Tarifario que dice, con todas las letras, que la operacion no cuesta: cero conocido, no un vacio. */
    static final class TarifaGratuita implements CotizadorDeComision {
        @Override
        public Optional<Dinero> costoDe(String hecho, UUID referenciaId, Dinero montoBase, String clave) {
            return Optional.of(Dinero.cero(montoBase.moneda()));
        }

        @Override
        public Optional<Cotizacion> cotizar(String hecho, UUID referenciaId, Dinero montoBase, String clave) {
            Dinero cero = Dinero.cero(montoBase.moneda());
            return Optional.of(new Cotizacion(Optional.empty(), montoBase, cero, cero, cero, Optional.empty(), true));
        }
    }

    /** El flujo con el cotizador que se le pase y, si se pide, la exigencia de cotizacion previa. */
    static RecargasConProveedor recargas(
            ProveedorDeRecargas proveedor, CotizadorDeComision cotizador, boolean exigirPrevia) {
        return recargas(proveedor, new CotizarOperacion(cotizador, Reloj.delSistema(), exigirPrevia));
    }

    /** El flujo con las cotizaciones que se le pasen (por ejemplo, con un reloj que se adelanta). */
    static RecargasConProveedor recargas(ProveedorDeRecargas proveedor, CotizarOperacion cotizaciones) {
        var manager = BaseDeBilletera.transaccion.getTransactionManager();
        var dsl = BaseDeBilletera.dsl;
        return new RecargasConProveedor(
                TransaccionalDePrueba.conTransaccion(BaseDeBilletera.recargaCU, manager),
                TransaccionalDePrueba.conTransaccion(
                        new CU10RechazarRecarga(
                                new Datos(dsl),
                                new OrdenRecargaRepositorio(),
                                new CuentaBilleteraRepositorio(),
                                new Outbox("nucleo_financiero")),
                        manager),
                proveedor,
                cotizaciones,
                existentes(),
                TransaccionalDePrueba.conTransaccion(
                        new RegistrarDiscrepancia(
                                new Datos(dsl),
                                new DiscrepanciaRepositorio(),
                                new Outbox("nucleo_financiero"),
                                Reloj.delSistema()),
                        manager));
    }

    static OrdenesExistentes existentes() {
        return TransaccionalDePrueba.conTransaccion(
                new OrdenesExistentes(
                        new Datos(BaseDeBilletera.dsl),
                        new CuentaBilleteraRepositorio(),
                        new OrdenRecargaRepositorio(),
                        new OrdenRetiroRepositorio()),
                BaseDeBilletera.transaccion.getTransactionManager());
    }

    /** El despacho de retiros: casos de uso transaccionales reales y el proveedor que se le pase. */
    static RetirosConProveedor retiros(ProveedorDeRetiros proveedor) {
        var manager = BaseDeBilletera.transaccion.getTransactionManager();
        var dsl = BaseDeBilletera.dsl;
        return new RetirosConProveedor(
                TransaccionalDePrueba.conTransaccion(
                        new CU11InstruirRetiro(new Datos(dsl), new OrdenRetiroRepositorio(), Reloj.delSistema()),
                        manager),
                TransaccionalDePrueba.conTransaccion(BaseDeBilletera.retiroCU, manager),
                proveedor,
                TransaccionalDePrueba.conTransaccion(
                        new RegistrarDiscrepancia(
                                new Datos(dsl),
                                new DiscrepanciaRepositorio(),
                                new Outbox("nucleo_financiero"),
                                Reloj.delSistema()),
                        manager));
    }
}

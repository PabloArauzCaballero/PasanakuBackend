package bo.aportaya.tarifas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.tarifas.aplicacion.ConsultarCostosDeProveedor;
import bo.aportaya.tarifas.dominio.CostoDeProveedor;
import bo.aportaya.tarifas.infraestructura.CostosDeProveedorRepositorio;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sistemas › Proveedores · el costo real por periodo contra Postgres real: suma DECIMAL exacta, una fila por moneda,
 * otro periodo no entra. Proveedores con id propio por prueba (el contenedor se comparte). Datos sinteticos.
 */
class CostosDeProveedorRepositorioTest extends BaseDeTarifas {

    private final ConsultarCostosDeProveedor consulta =
            new ConsultarCostosDeProveedor(new Datos(dsl), new CostosDeProveedorRepositorio());

    private static ContextoSesion operador() {
        return ContextoSesion.de(
                UUID.randomUUID(), "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private void costo(UUID proveedor, String total, String moneda, String periodo) {
        dslFixtura.execute(
                """
                insert into tarifas.costo_proveedor_operacion (proveedor_id, tipo_operacion, costo_total, moneda, periodo)
                values (?, ?, ?::numeric, ?, ?)
                """,
                proveedor,
                TIPO,
                total,
                moneda,
                periodo);
    }

    /** Uno de los de {@code ck_costo_proveedor_operacion_tipo_operacion}. */
    private static final String TIPO = "COBRO_QR";

    /** {@code proveedor_id} tiene FK a {@code aportes.proveedor_pago}: el proveedor existe de verdad. */
    private static UUID proveedorDePago() {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                insert into aportes.proveedor_pago
                  (id, codigo, nombre, tipo, url_base, referencia_credenciales, comision_fija, comision_porcentual,
                   soporta_webhook, soporta_consulta_estado, activo, prioridad)
                values (?, ?, 'sintetico', 'PASARELA', 'https://pasarela.test', 'vault://x', 0, 0, false, false, true, 99)
                """,
                id,
                "C" + id.toString().substring(0, 8));
        return id;
    }

    private List<CostoDeProveedor> de(UUID proveedor, String periodo) {
        return transaccion.execute(s -> consulta.delPeriodo(periodo, operador())).stream()
                .filter(c -> c.proveedorId().equals(proveedor))
                .toList();
    }

    @Test
    @DisplayName("correcto · 0.10 + 0.20 suma 0.30 exacto; dos monedas son dos filas; otro periodo no entra")
    void sumaExacta() {
        UUID p = proveedorDePago();
        costo(p, "0.10", "BOB", "2099-09");
        costo(p, "0.20", "BOB", "2099-09");
        costo(p, "1.50", "USD", "2099-09");
        costo(p, "9.99", "BOB", "2099-08");

        var filas = de(p, "2099-09");
        assertThat(filas).extracting(CostoDeProveedor::moneda).containsExactly("BOB", "USD");
        assertThat(filas.get(0).costoTotal().toPlainString()).isEqualTo("0.30");
        assertThat(filas.get(0).operaciones()).isEqualTo(2);
        assertThat(filas.get(1).costoTotal()).isEqualByComparingTo("1.50");
    }

    @Test
    @DisplayName("limite · un periodo sin operaciones devuelve lista vacia, no un cero inventado")
    void sinOperaciones() {
        assertThat(de(proveedorDePago(), "2099-12")).isEmpty();
    }
}

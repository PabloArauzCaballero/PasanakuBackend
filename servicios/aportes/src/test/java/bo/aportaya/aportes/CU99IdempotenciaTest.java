package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU99EnrutarProveedor.EntradaAlta;
import bo.aportaya.aportes.aplicacion.CU99EnrutarProveedor.SalidaAlta;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** F1.L1.M09 (ANF-RESILIENCIA-001) · el alta de proveedor con la Idempotency-Key de verdad, contra la base (R-BIL-31). */
class CU99IdempotenciaTest extends BaseDeAportes {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static EntradaAlta alta(String codigo, String clave) {
        return new EntradaAlta(
                codigo,
                "Pasarela " + codigo,
                "PASARELA",
                "https://" + codigo + ".test",
                "vault://pagos/" + codigo,
                new BigDecimal("0.50"),
                new BigDecimal("0.0250"),
                true,
                true,
                1,
                true,
                true,
                clave);
    }

    private SalidaAlta darDeAlta(EntradaAlta entrada, ContextoSesion ctx) {
        return transaccion.execute(t -> proveedorCU.darDeAlta(entrada, ctx));
    }

    private int filasDe(String codigo) {
        return contar("SELECT count(*)::int FROM aportes.proveedor_pago WHERE codigo = ?", codigo);
    }

    @Test
    @DisplayName("reintento: la misma clave dos veces en serie devuelve el mismo proveedor y deja una sola fila")
    void reintentoEnSerie() {
        var ctx = contextoDe(fixtura.usuario());
        String codigo = "IDEM-" + UUID.randomUUID().toString().substring(0, 8);
        String clave = UUID.randomUUID().toString();

        SalidaAlta primera = darDeAlta(alta(codigo, clave), ctx);
        SalidaAlta segunda = darDeAlta(alta(codigo, clave), ctx);

        assertThat(segunda.proveedorId()).isEqualTo(primera.proveedorId());
        assertThat(filasDe(codigo)).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.evento_dominio WHERE tipo = 'aportes.proveedor_activado' AND agregado_id = ?",
                        primera.proveedorId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: cuatro altas simultaneas con la misma clave dejan un solo proveedor y un solo id")
    void concurrenciaMismaClave() throws Exception {
        var ctx = contextoDe(fixtura.usuario());
        String codigo = "CONC-" + UUID.randomUUID().toString().substring(0, 8);
        String clave = UUID.randomUUID().toString();
        Callable<SalidaAlta> intento = () -> darDeAlta(alta(codigo, clave), ctx);

        List<UUID> ids = new ArrayList<>();
        try (var hilos = Executors.newFixedThreadPool(4)) {
            List<Future<SalidaAlta>> futuros = hilos.invokeAll(List.of(intento, intento, intento, intento));
            for (var f : futuros) {
                ids.add(f.get().proveedorId());
            }
        }

        assertThat(ids).hasSize(4).containsOnly(ids.get(0));
        assertThat(filasDe(codigo)).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza reusar la clave de un alta para dar de alta otro proveedor (AP-CU99-01)")
    void rechazaClaveDeOtraAlta() {
        var ctx = contextoDe(fixtura.usuario());
        String clave = UUID.randomUUID().toString();
        String primero = "UNO-" + UUID.randomUUID().toString().substring(0, 8);
        String otro = "OTRO-" + UUID.randomUUID().toString().substring(0, 8);
        darDeAlta(alta(primero, clave), ctx);

        assertThatThrownBy(() -> darDeAlta(alta(otro, clave), ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("otro proveedor");
        assertThat(filasDe(otro)).isZero();
    }

    @Test
    @DisplayName("rechaza la base dos proveedores con la misma clave aunque el codigo no salga del caso de uso")
    void rechazaLaBaseLaClaveRepetida() {
        var ctx = contextoDe(fixtura.usuario());
        String clave = UUID.randomUUID().toString();
        var primero = darDeAlta(alta("BASE-" + UUID.randomUUID().toString().substring(0, 8), clave), ctx);

        assertThatThrownBy(() -> transaccion.execute(t -> dsl.execute(
                        """
                        INSERT INTO aportes.proveedor_pago
                            (id, codigo, nombre, tipo, url_base, referencia_credenciales, comision_fija,
                             comision_porcentual, soporta_webhook, soporta_consulta_estado, activo, prioridad,
                             clave_idempotencia)
                        SELECT gen_random_uuid(), 'COPIA-' || substr(codigo, 1, 20), nombre, tipo, url_base,
                               referencia_credenciales, comision_fija, comision_porcentual, soporta_webhook,
                               soporta_consulta_estado, activo, prioridad, clave_idempotencia
                          FROM aportes.proveedor_pago WHERE id = ?
                        """,
                        primero.proveedorId())))
                .hasMessageContaining("uq_proveedor_pago_clave");
    }
}

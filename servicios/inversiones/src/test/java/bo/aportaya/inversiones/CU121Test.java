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
 * H10.S1.M2 · consentimiento versionado, orden con saldo retenido, idempotencia, concurrencia
 * y H10.S1.M7 · el saldo afectado a un pozo no se invierte.
 *
 * <p>PostgreSQL real; el libro y el aliado son los dobles declarados.
 */
class CU121Test extends BaseDeInversiones {

    private String codigoDe(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    private int ordenesDelTitular() {
        return contar("select count(*)::int from inversiones.orden_inversion where usuario_id = ?", usuario);
    }

    @Test
    @DisplayName(
            "Dado saldo libre · Cuando invierte voluntariamente · Entonces reserva el importe y guarda el consentimiento con la version y el hash que acepto")
    void criterio1() {
        conSaldo("5000.00");
        aliado.modo(AliadoDoble.Modo.PENDIENTE); // el aliado todavia no confirma: se ve el estado intermedio
        var condiciones = producto(PRODUCTO_DPF).condiciones();

        VistaOrden orden = ordenar(PRODUCTO_DPF, "1000.00");

        assertThat(orden.estado()).isEqualTo("ENVIADA");
        // Saldo retenido: no disponible, pero tampoco gastado ni invertido todavia.
        assertThat(libro.disponible(cuenta)).isEqualByComparingTo("4000.00");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("1000.00");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("5000.00");
        // Consentimiento versionado.
        var fila = dslFixtura.fetchOne(
                "select version_condiciones_id, texto_hash from inversiones.consentimiento_inversion where id = ?",
                orden.consentimientoId());
        assertThat(fila.get(0, UUID.class)).isEqualTo(condiciones.versionId()).isEqualTo(orden.versionCondicionesId());
        assertThat(fila.get(1, String.class)).isEqualTo(condiciones.textoHash());
        assertThat(orden.mensaje()).contains("todavia no la confirmo");
    }

    @Test
    @DisplayName(
            "Dada la misma orden enviada dos veces con la misma clave · Cuando se repite · Entonces devuelve la original y no duplica orden, retencion ni posicion")
    void idempotenciaEjecutadaDosVeces() {
        conSaldo("5000.00");
        UUID clave = UUID.randomUUID();
        var entrada = entrada(PRODUCTO_DPF, "1000.00", clave);

        VistaOrden primera = cu121.ordenar(entrada, ctx);
        VistaOrden segunda = cu121.ordenar(entrada, ctx);

        assertThat(segunda.ordenId()).isEqualTo(primera.ordenId());
        assertThat(segunda.posicionId()).isEqualTo(primera.posicionId());
        assertThat(ordenesDelTitular()).isEqualTo(1);
        assertThat(contar(
                        "select count(*)::int from inversiones.consentimiento_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
        assertThat(contar("select count(*)::int from inversiones.posicion_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
        assertThat(libro.veces("RETENER:" + primera.ordenId())).isEqualTo(1);
        assertThat(libro.veces("DEBITAR:" + primera.ordenId())).isEqualTo(1);
        assertThat(libro.total(cuenta)).isEqualByComparingTo("4000.00");
    }

    @Test
    @DisplayName(
            "Dada la misma clave con OTRO monto · Cuando se repite el pedido · Entonces se rechaza AP-CU121-06 y no se crea nada nuevo")
    void mismaClaveOtroContenido() {
        conSaldo("5000.00");
        UUID clave = UUID.randomUUID();
        cu121.ordenar(entrada(PRODUCTO_DPF, "1000.00", clave), ctx);

        assertThatThrownBy(() -> cu121.ordenar(entrada(PRODUCTO_DPF, "2000.00", clave), ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-06"));
        assertThat(ordenesDelTitular()).isEqualTo(1);
        assertThat(libro.total(cuenta)).isEqualByComparingTo("4000.00");
    }

    @Test
    @DisplayName(
            "Dado un saldo afectado a un pozo · Cuando intenta invertirlo · Entonces rechaza AP-CU121-05 y no crea orden; en el borde exacto de lo invertible, pasa")
    void criterioBloqueoDePozo() {
        conSaldo("5000.00");
        libro.comprometerEnPozos(cuenta, "4500.00"); // invertible: 500.00

        assertThatThrownBy(() -> ordenar(PRODUCTO_DPF, "1000.00"))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-05"));
        assertThatThrownBy(() -> ordenar(PRODUCTO_DPF, "500.01"))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-05"));
        assertThat(ordenesDelTitular()).isZero();
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");

        VistaOrden justo = ordenar(PRODUCTO_DPF, "500.00"); // el borde: exactamente lo invertible
        assertThat(justo.estado()).isEqualTo("CONFIRMADA");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("4500.00");
    }

    @Test
    @DisplayName(
            "Dado saldo insuficiente sin pozo de por medio · Cuando ordena mas de lo que tiene · Entonces AP-CU121-04, no AP-CU121-05")
    void saldoInsuficiente() {
        conSaldo("800.00");
        assertThatThrownBy(() -> ordenar(PRODUCTO_DPF, "1000.00"))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-04"));
        assertThat(ordenesDelTitular()).isZero();
        // El borde: todo el disponible, justo.
        assertThat(ordenar(PRODUCTO_DPF, "800.00").estado()).isEqualTo("CONFIRMADA");
        assertThat(libro.disponible(cuenta)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName(
            "Dada una cuenta de otra persona · Cuando se la usa para invertir · Entonces AP-CU121-04: no se puede usar (objeto propio, no solo rol)")
    void cuentaAjena() {
        libro.abrirCuenta(cuenta, UUID.randomUUID(), "5000.00");
        assertThatThrownBy(() -> ordenar(PRODUCTO_DPF, "1000.00"))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-04"));
        assertThat(ordenesDelTitular()).isZero();
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName(
            "Dado un monto bajo el minimo del producto · Cuando ordena · Entonces AP-CU121-03; con el minimo exacto, pasa")
    void montoMinimo() {
        conSaldo("5000.00");
        assertThatThrownBy(() -> ordenar(PRODUCTO_DPF, "499.99"))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-03"));
        assertThat(ordenar(PRODUCTO_DPF, "500.00").estado()).isEqualTo("CONFIRMADA");
    }

    @Test
    @DisplayName(
            "Dadas condiciones que cambiaron (version o hash viejos) · Cuando ordena · Entonces AP-CU121-02: hay que volver a aceptar")
    void condicionesDesactualizadas() {
        conSaldo("5000.00");
        var c = producto(PRODUCTO_DPF).condiciones();
        var conOtraVersion = new bo.aportaya.inversiones.aplicacion.CU121OrdenarInversion.EntradaOrden(
                PRODUCTO_DPF,
                UUID.randomUUID(),
                c.textoHash(),
                cuenta,
                bob("1000.00"),
                UUID.randomUUID().toString());
        var conOtroTexto = new bo.aportaya.inversiones.aplicacion.CU121OrdenarInversion.EntradaOrden(
                PRODUCTO_DPF,
                c.versionId(),
                "0".repeat(64),
                cuenta,
                bob("1000.00"),
                UUID.randomUUID().toString());
        assertThatThrownBy(() -> cu121.ordenar(conOtraVersion, ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-02"));
        assertThatThrownBy(() -> cu121.ordenar(conOtroTexto, ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-02"));
        assertThat(ordenesDelTitular()).isZero();
        assertThat(contar(
                        "select count(*)::int from inversiones.consentimiento_inversion where usuario_id = ?", usuario))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dada una moneda que no es BOB o un producto inexistente · Cuando ordena · Entonces AP-CU121-08 en el primer caso y AP-CU121-01 en el segundo")
    void monedaYProducto() {
        conSaldo("5000.00");
        var c = producto(PRODUCTO_DPF).condiciones();
        var enDolares = new bo.aportaya.inversiones.aplicacion.CU121OrdenarInversion.EntradaOrden(
                PRODUCTO_DPF,
                c.versionId(),
                c.textoHash(),
                cuenta,
                bo.aportaya.plataforma.dominio.Dinero.de("1000.00", bo.aportaya.plataforma.dominio.Moneda.USD),
                UUID.randomUUID().toString());
        assertThatThrownBy(() -> cu121.ordenar(enDolares, ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-08"));
        var inexistente = new bo.aportaya.inversiones.aplicacion.CU121OrdenarInversion.EntradaOrden(
                "NO-EXISTE",
                c.versionId(),
                c.textoHash(),
                cuenta,
                bob("1000.00"),
                UUID.randomUUID().toString());
        assertThatThrownBy(() -> cu121.ordenar(inexistente, ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-01"));
    }

    @Test
    @DisplayName(
            "Dado el libro caido al verificar el saldo · Cuando ordena · Entonces AP-CU121-07 y no se reserva ni se crea nada (falla cerrada)")
    void libroCaidoAlVerificar() {
        conSaldo("5000.00");
        libro.caido(true);
        assertThatThrownBy(() -> ordenar(PRODUCTO_DPF, "1000.00"))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-07"));
        assertThat(ordenesDelTitular()).isZero();
        libro.caido(false);
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza por R-INV-02: una orden cuyo consentimiento es de otra persona")
    void baseRechazaConsentimientoAjeno() {
        conSaldo("5000.00");
        var c = producto(PRODUCTO_DPF);
        UUID otraPersona = UUID.randomUUID();
        UUID consentimiento = UUID.randomUUID();
        assertThatThrownBy(() -> tx.en(() -> datos.conContexto(ctx, d -> {
                    ordenes.insertarConsentimiento(
                            d,
                            consentimiento,
                            otraPersona,
                            c.condiciones().versionId(),
                            c.condiciones().textoHash(),
                            UUID.randomUUID().toString(),
                            "h",
                            java.time.OffsetDateTime.now());
                    ordenes.insertarOrden(
                            d,
                            UUID.randomUUID(),
                            usuario,
                            c.id(),
                            c.condiciones().versionId(),
                            consentimiento,
                            cuenta,
                            new BigDecimal("1000.00"),
                            "BOB",
                            UUID.randomUUID().toString(),
                            "h",
                            java.time.OffsetDateTime.now());
                    return null;
                })))
                .hasStackTraceContaining("R-INV-02");
    }
}

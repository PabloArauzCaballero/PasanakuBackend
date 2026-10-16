package bo.aportaya.erp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.erp.aplicacion.CU104CobrarCuenta.EntradaCobro;
import bo.aportaya.erp.aplicacion.CU104CobrarCuenta.EntradaCuenta;
import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp.Filtro;
import bo.aportaya.erp.aplicacion.FilasDeErp;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · A6 · el listado de cuentas por cobrar, contra PostgreSQL real.
 *
 * <p>Vencimientos del rango 2560, que ninguna otra prueba reparte, y cursor inicial en el anio siguiente: la
 * primera pagina es de la prueba. Lo cobrado, el saldo y el estado salen de los COBROS (la cuenta es
 * append-only y sus columnas de saldo se quedan como nacieron).
 */
class CU104ListadoTest extends BaseDeErp {

    private static final AtomicInteger ANIO = new AtomicInteger(2560);
    private static final UUID MAXIMO = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private int anio;
    private UUID cliente;
    private ContextoSesion tesoreria;

    @BeforeEach
    void escenario() {
        anio = ANIO.incrementAndGet();
        tesoreria = contextoDe(fixtura.usuario());
        cliente = fixtura.tercero("CLIENTE", "NIT-L104-" + anio);
    }

    private UUID cuenta(String monto, LocalDate vence) {
        return transaccion.execute(t -> cobroCU.abrir(
                new EntradaCuenta("VENTA_SERVICIO", UUID.randomUUID(), cliente, new BigDecimal(monto), "BOB", vence),
                tesoreria));
    }

    /** Nace INCOBRABLE: la tabla es append-only y no hay operacion para declararla despues (dato sintetico). */
    private UUID incobrable(LocalDate vence) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO erp.cuenta_por_cobrar
                    (id, origen_tipo, origen_id, tercero_comercial_id, monto, moneda, monto_cobrado,
                     fecha_vencimiento, estado)
                VALUES (?, 'OTRO', gen_random_uuid(), ?, 75.50, 'BOB', 0, ?, 'INCOBRABLE')
                """,
                id,
                cliente,
                vence);
        return id;
    }

    private Filtro<LocalDate> desdeMiAnio(List<String> estados, int limite) {
        return new Filtro<>(estados, Optional.of(LocalDate.of(anio + 1, 1, 1)), Optional.of(MAXIMO), limite);
    }

    @Test
    @DisplayName("Dada una cuenta por cobrar cobrada en parte y otra incobrable · Cuando Tesoreria lista las cuentas"
            + " por cobrar · Entonces ve COBRADA_PARCIAL con el saldo derivado de sus cobros, y la incobrable sigue"
            + " INCOBRABLE")
    void cobradaEnParteEIncobrable() {
        UUID parcial = cuenta("500.00", LocalDate.of(anio, 6, 30));
        transaccion.execute(
                t -> cobroCU.cobrar(new EntradaCobro(parcial, new BigDecimal("120.40"), "TRANSFERENCIA"), tesoreria));
        UUID perdida = incobrable(LocalDate.of(anio, 3, 31));

        var pagina = transaccion.execute(t -> listadosCU.cuentasPorCobrar(desdeMiAnio(List.of(), 2), tesoreria));

        assertThat(pagina.items())
                .extracting(FilasDeErp.CuentaPorCobrar::cuentaPorCobrarId)
                .containsExactly(parcial, perdida);
        var primera = pagina.items().get(0);
        assertThat(primera.estado()).isEqualTo("COBRADA_PARCIAL");
        assertThat(primera.cobrado().toString()).isEqualTo("120.40");
        assertThat(primera.saldoPendiente().toString()).isEqualTo("379.60");
        assertThat(primera.terceroRazonSocial()).contains("Tercero NIT-L104-" + anio);
        assertThat(pagina.items().get(1).estado()).isEqualTo("INCOBRABLE");
        assertThat(pagina.items().get(1).saldoPendiente().toString()).isEqualTo("75.50");
    }

    @Test
    @DisplayName("Dada una cuenta cobrada entera y otra pendiente · Cuando Tesoreria filtra por estado o corta la"
            + " pagina en la cobrada · Entonces la cobrada figura COBRADA con saldo cero, la pendiente sigue"
            + " PENDIENTE y el cursor no repite la fila en la que se corto")
    void cobradaEntera() {
        UUID entera = cuenta("40.00", LocalDate.of(anio, 2, 1));
        UUID pendiente = cuenta("41.00", LocalDate.of(anio, 1, 15));
        transaccion.execute(
                t -> cobroCU.cobrar(new EntradaCobro(entera, new BigDecimal("40.00"), "TRANSFERENCIA"), tesoreria));

        var cobradas =
                transaccion.execute(t -> listadosCU.cuentasPorCobrar(desdeMiAnio(List.of("COBRADA"), 1), tesoreria));
        var pendientes =
                transaccion.execute(t -> listadosCU.cuentasPorCobrar(desdeMiAnio(List.of("PENDIENTE"), 1), tesoreria));
        var corta = transaccion.execute(t -> listadosCU.cuentasPorCobrar(
                new Filtro<>(List.of(), Optional.of(LocalDate.of(anio, 2, 1)), Optional.of(entera), 1), tesoreria));

        assertThat(cobradas.items().get(0).cuentaPorCobrarId()).isEqualTo(entera);
        assertThat(cobradas.items().get(0).saldoPendiente().toString()).isEqualTo("0.00");
        assertThat(pendientes.items().get(0).cuentaPorCobrarId()).isEqualTo(pendiente);
        assertThat(corta.items().get(0).cuentaPorCobrarId()).isEqualTo(pendiente);
        assertThat(corta.proximoAntesDeId()).contains(pendiente);
    }

    @Test
    @DisplayName("rechaza el listado de cuentas por cobrar con cursor a medias o estado desconocido: AP-CU104-04")
    void rechazaFiltroInvalido() {
        assertThatThrownBy(() -> transaccion.execute(t -> listadosCU.cuentasPorCobrar(
                        new Filtro<>(List.of(), Optional.empty(), Optional.of(MAXIMO), 5), tesoreria)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU104-04"));
        assertThatThrownBy(() -> transaccion.execute(t -> listadosCU.cuentasPorCobrar(
                        new Filtro<>(List.of("VENCIDA"), Optional.empty(), Optional.empty(), 5), tesoreria)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU104-04"));
    }
}

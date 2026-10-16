package bo.aportaya.erp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.erp.aplicacion.CU103FacturaDeProveedor.EntradaPago;
import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp.Filtro;
import bo.aportaya.erp.aplicacion.FilasDeErp;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · A6 · el listado de cuentas por pagar, contra PostgreSQL real.
 *
 * <p>Cada prueba tiene su propio ejercicio (rango 2300 de {@link EscenarioDeFactura}) y arranca el cursor en el
 * primer dia del anio siguiente: las facturas de otros anios quedan fuera de la primera pagina. Lo que se prueba
 * es que lo pagado, el saldo y el estado salen de los PAGOS, no de las columnas congeladas de la factura.
 */
class CU103ListadoTest extends EscenarioDeFactura {

    private static final UUID MAXIMO = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private Filtro<LocalDate> desdeMiAnio(List<String> estados) {
        return new Filtro<>(estados, Optional.of(LocalDate.of(anio + 1, 1, 1)), Optional.of(MAXIMO), 10);
    }

    private List<FilasDeErp.Factura> listarComo(ContextoSesion quien, List<String> estados) {
        return transaccion.execute(t -> listadosCU.facturas(desdeMiAnio(estados), quien)).items().stream()
                .filter(f -> f.fechaEmision().getYear() == anio)
                .toList();
    }

    @Test
    @DisplayName("Dada una factura aprobada y pagada en parte · Cuando Tesoreria lista las cuentas por pagar ·"
            + " Entonces la ve PAGADA_PARCIAL con lo pagado y el saldo derivados de sus pagos, y no aprobada por ella")
    void pagadaEnParte() {
        var factura = transaccion.execute(t -> facturaCU.registrar(factura("1000.00", enPeriodo(2)), aprobador));
        transaccion.execute(t -> facturaCU.pagar(
                new EntradaPago(
                        factura.facturaId(), new BigDecimal("400.25"), "BOB", "TRANSFERENCIA", UUID.randomUUID()),
                tesoreria));

        var filas = listarComo(tesoreria, List.of());

        assertThat(filas).hasSize(1);
        var fila = filas.get(0);
        assertThat(fila.facturaProveedorId()).isEqualTo(factura.facturaId());
        assertThat(fila.estado()).isEqualTo("PAGADA_PARCIAL");
        assertThat(fila.monto().toString()).isEqualTo("1000.00");
        assertThat(fila.pagado().toString()).isEqualTo("400.25");
        assertThat(fila.saldoPendiente().toString()).isEqualTo("599.75");
        assertThat(fila.aprobadaPorLaSesion()).isFalse();
    }

    @Test
    @DisplayName("Dada una factura aprobada por quien consulta · Cuando lista las cuentas por pagar · Entonces la fila"
            + " dice que la aprobo la sesion, para que no se le ofrezca pagarla")
    void aprobadaPorLaSesion() {
        transaccion.execute(t -> facturaCU.registrar(factura("80.00", enPeriodo(3)), aprobador));

        assertThat(listarComo(aprobador, List.of())).singleElement().satisfies(f -> assertThat(f.aprobadaPorLaSesion())
                .isTrue());
        assertThat(listarComo(tesoreria, List.of())).singleElement().satisfies(f -> assertThat(f.aprobadaPorLaSesion())
                .isFalse());
    }

    @Test
    @DisplayName("Dadas dos facturas del mismo dia y una de ellas pagada entera · Cuando Tesoreria las recorre de a"
            + " una o filtra por PAGADA · Entonces no se repiten y la pagada figura PAGADA con saldo cero")
    void pagadaEnteraYEmpate() {
        LocalDate dia = enPeriodo(4);
        var pagada = transaccion.execute(t -> facturaCU.registrar(factura("10.00", dia), aprobador));
        transaccion.execute(t -> facturaCU.registrar(factura("20.00", dia), aprobador));
        transaccion.execute(t -> facturaCU.pagar(
                new EntradaPago(pagada.facturaId(), new BigDecimal("10.00"), "BOB", "TRANSFERENCIA", UUID.randomUUID()),
                tesoreria));

        var soloPagadas = listarComo(tesoreria, List.of("PAGADA"));
        var primera = transaccion.execute(t -> listadosCU.facturas(
                new Filtro<>(List.of(), Optional.of(LocalDate.of(anio + 1, 1, 1)), Optional.of(MAXIMO), 1), tesoreria));
        var segunda = transaccion.execute(t -> listadosCU.facturas(
                new Filtro<>(List.of(), primera.proximoAntesDe(), primera.proximoAntesDeId(), 1), tesoreria));

        assertThat(soloPagadas).singleElement().satisfies(f -> {
            assertThat(f.facturaProveedorId()).isEqualTo(pagada.facturaId());
            assertThat(f.saldoPendiente().toString()).isEqualTo("0.00");
        });
        assertThat(primera.items().get(0).facturaProveedorId())
                .isNotEqualTo(segunda.items().get(0).facturaProveedorId());
        assertThat(segunda.items().get(0).fechaEmision()).isEqualTo(dia);
    }

    @Test
    @DisplayName("rechaza el listado de facturas con cursor a medias o estado desconocido: AP-CU103-06")
    void rechazaFiltroInvalido() {
        assertThatThrownBy(() -> transaccion.execute(t -> listadosCU.facturas(
                        new Filtro<>(List.of(), Optional.of(LocalDate.of(anio, 1, 1)), Optional.empty(), 5),
                        tesoreria)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU103-06"));
        assertThatThrownBy(() -> transaccion.execute(t -> listadosCU.facturas(
                        new Filtro<>(List.of("VENCIDA"), Optional.empty(), Optional.empty(), 5), tesoreria)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU103-06"));
    }
}

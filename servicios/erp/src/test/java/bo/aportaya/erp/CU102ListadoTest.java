package bo.aportaya.erp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.erp.aplicacion.CU102AltaDeTercero.EntradaOrden;
import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp.Filtro;
import bo.aportaya.erp.aplicacion.FilasDeErp;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · A6 · el listado de ordenes de compra, contra PostgreSQL real.
 *
 * <p>Las pruebas de erp corren en serie dentro del modulo: las ordenes que crea cada prueba son las mas
 * recientes de la base en ese momento, asi que la primera pagina sin cursor es suya.
 */
class CU102ListadoTest extends BaseDeErp {

    private ContextoSesion compras;
    private UUID proveedor;

    @BeforeEach
    void escenario() {
        compras = contextoDe(fixtura.usuario());
        proveedor = fixtura.tercero(
                "PROVEEDOR", "NIT-L102-" + UUID.randomUUID().toString().substring(0, 8));
    }

    private UUID orden(String monto) {
        var entrada = new EntradaOrden(
                proveedor,
                null,
                "OC-L-" + UUID.randomUUID().toString().substring(0, 8),
                "Descripcion libre que no viaja al listado",
                new BigDecimal(monto),
                "BOB");
        return transaccion.execute(t -> terceroCU.crearOrden(entrada, compras)).ordenId();
    }

    private Filtro<OffsetDateTime> filtro(List<String> estados, Optional<OffsetDateTime> antesDe, Optional<UUID> id) {
        return new Filtro<>(estados, antesDe, id, 2);
    }

    @Test
    @DisplayName("Dadas tres ordenes de compra creadas una tras otra · Cuando Contabilidad las recorre de a dos ·"
            + " Entonces van de la mas reciente a la mas antigua, con proveedor y monto en cadena, sin repetir ninguna")
    void ordenesPaginadas() {
        UUID primera = orden("100.01");
        UUID segunda = orden("200.02");
        UUID tercera = orden("300.03");

        var pagina1 = transaccion.execute(
                t -> listadosCU.ordenes(filtro(List.of(), Optional.empty(), Optional.empty()), compras));
        var pagina2 = transaccion.execute(t ->
                listadosCU.ordenes(filtro(List.of(), pagina1.proximoAntesDe(), pagina1.proximoAntesDeId()), compras));

        assertThat(pagina1.items()).extracting(FilasDeErp.Orden::ordenCompraId).containsExactly(tercera, segunda);
        assertThat(pagina2.items().get(0).ordenCompraId()).isEqualTo(primera);
        var ultima = pagina1.items().get(0);
        assertThat(ultima.terceroRazonSocial()).startsWith("Tercero NIT-L102-");
        assertThat(ultima.monto().toString()).isEqualTo("300.03");
        assertThat(ultima.estado()).isEqualTo("BORRADOR");
        assertThat(ultima.centroCostoId()).isEmpty();
    }

    @Test
    @DisplayName("Dada una orden aprobada y otra en borrador · Cuando Compras filtra por BORRADOR · Entonces solo ve la"
            + " que falta aprobar")
    void filtraPorEstado() {
        UUID aprobada = orden("50.00");
        transaccion.execute(t -> terceroCU.aprobarOrden(aprobada, contextoDe(fixtura.usuario())));
        UUID borrador = orden("60.00");

        var borradores = transaccion.execute(
                t -> listadosCU.ordenes(filtro(List.of("BORRADOR"), Optional.empty(), Optional.empty()), compras));
        var aprobadas = transaccion.execute(
                t -> listadosCU.ordenes(filtro(List.of("APROBADA"), Optional.empty(), Optional.empty()), compras));

        assertThat(borradores.items())
                .extracting(FilasDeErp.Orden::ordenCompraId)
                .contains(borrador);
        assertThat(borradores.items())
                .extracting(FilasDeErp.Orden::ordenCompraId)
                .doesNotContain(aprobada);
        assertThat(aprobadas.items().get(0).ordenCompraId()).isEqualTo(aprobada);
        assertThat(aprobadas.items()).allMatch(o -> o.estado().equals("APROBADA"));
    }

    @Test
    @DisplayName("rechaza el listado de ordenes con cursor a medias o estado desconocido: AP-CU102-04")
    void rechazaFiltroInvalido() {
        assertThatThrownBy(() -> transaccion.execute(t -> listadosCU.ordenes(
                        filtro(List.of(), Optional.empty(), Optional.of(UUID.randomUUID())), compras)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU102-04"));
        assertThatThrownBy(() -> transaccion.execute(t ->
                        listadosCU.ordenes(filtro(List.of("RECIBIDA"), Optional.empty(), Optional.empty()), compras)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU102-04"));
    }
}

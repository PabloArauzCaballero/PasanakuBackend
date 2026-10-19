package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.BandejaDeRetiros;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaRetiro;
import bo.aportaya.nucleofinanciero.infraestructura.BandejaDeRetirosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · H2.S2 · la bandeja de la segunda aprobacion (ADR-049) contra PostgreSQL real.
 *
 * <p>Lo que importa: que la cola no repita ni salte ordenes entre paginas, que una orden que ya
 * salio de revision deje de aparecer, y que el cursor a medias se rechace con el codigo del
 * contrato en vez de devolver cualquier cosa. El aprobador consulta con el rol que identidad le
 * pone a todo operador ({@code BACKOFFICE}, {@code SesionesController.ROL_DE_OPERADOR}).
 */
class CU11BandejaTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static ContextoSesion operador(UUID usuarioId) {
        return ContextoSesion.de(
                usuarioId, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private record Solicitante(UUID usuario, UUID cuenta, UUID instrumento, ContextoSesion ctx) {}

    private Solicitante solicitante() {
        fixtura.tipoDeCambioDeHoy();
        custodia.cumpleEncaje();
        fixtura.limite("RETIRO", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("50000.00"));
        UUID instrumento = custodia.instrumentoDestino(usuario, true, true, null);
        return new Solicitante(usuario, cuenta, instrumento, contextoDe(usuario));
    }

    /** Por encima de {@code aportaya.retiro.doble-aprobacion-desde}: nace EN_REVISION. */
    private SalidaRetiro pedirGrande(Solicitante s, String clave) {
        return transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(clave, s.cuenta(), bob("6000.00"), bob("5.00"), s.instrumento(), true, true, true),
                s.ctx()));
    }

    private BandejaDeRetiros.Pagina pagina(
            ContextoSesion ctx, Optional<OffsetDateTime> fecha, Optional<UUID> id, int limite) {
        var bandeja = new BandejaDeRetiros(new Datos(dsl), new BandejaDeRetirosRepositorio());
        return transaccion.execute(t -> bandeja.enRevision(fecha, id, limite, ctx));
    }

    @Test
    @DisplayName("Dadas tres ordenes EN_REVISION · Cuando el aprobador recorre la bandeja de a dos · Entonces ve las"
            + " tres una sola vez, de la mas antigua a la mas nueva, y la segunda pagina cierra sin cursor")
    void recorreSinRepetirNiSaltar() {
        Solicitante s = solicitante();
        List<UUID> pedidas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            pedidas.add(pedirGrande(s, "bandeja-" + i).ordenRetiroId());
        }
        ContextoSesion aprobador = operador(fixtura.usuario());

        BandejaDeRetiros.Pagina primera = pagina(aprobador, Optional.empty(), Optional.empty(), 2);
        assertThat(primera.items()).hasSize(2);
        assertThat(primera.proximoFecha()).isPresent();
        assertThat(primera.proximoId()).isPresent();

        BandejaDeRetiros.Pagina segunda = pagina(aprobador, primera.proximoFecha(), primera.proximoId(), 2);
        assertThat(segunda.items()).hasSize(1);
        assertThat(segunda.proximoId()).isEmpty();

        List<UUID> vistas = new ArrayList<>();
        primera.items().forEach(f -> vistas.add(f.ordenRetiroId()));
        segunda.items().forEach(f -> vistas.add(f.ordenRetiroId()));
        assertThat(vistas).containsExactlyElementsOf(pedidas);
        assertThat(primera.items().get(0).solicitado().monto()).isEqualByComparingTo("6000.00");
        assertThat(primera.items().get(0).solicitado().moneda()).isEqualTo(Moneda.BOB);
        assertThat(primera.items()).allMatch(BandejaDeRetiros.Fila::puedeDecidir);
    }

    @Test
    @DisplayName("Dada una orden que otro aprobador ya autorizo · Cuando se consulta la bandeja · Entonces ya no"
            + " aparece: la cola muestra solo lo que todavia espera decision")
    void loResueltoSaleDeLaCola() {
        Solicitante s = solicitante();
        SalidaRetiro resuelta = pedirGrande(s, "bandeja-resuelta");
        SalidaRetiro pendiente = pedirGrande(s, "bandeja-pendiente");
        transaccion.execute(t -> retiroCU.aprobar(resuelta.ordenRetiroId(), operador(fixtura.usuario())));

        BandejaDeRetiros.Pagina bandeja = pagina(operador(fixtura.usuario()), Optional.empty(), Optional.empty(), 25);

        assertThat(bandeja.items()).extracting(f -> f.ordenRetiroId()).containsExactly(pendiente.ordenRetiroId());
    }

    @Test
    @DisplayName("Dada una bandeja sin ordenes EN_REVISION · Cuando el aprobador la consulta · Entonces recibe una"
            + " pagina vacia y sin cursor, nunca un error")
    void colaVacia() {
        BandejaDeRetiros.Pagina bandeja = pagina(operador(fixtura.usuario()), Optional.empty(), Optional.empty(), 25);

        assertThat(bandeja.items()).isEmpty();
        assertThat(bandeja.proximoFecha()).isEmpty();
        assertThat(bandeja.proximoId()).isEmpty();
    }

    @Test
    @DisplayName("Dada una orden EN_REVISION que pidio el mismo operador · Cuando la consulta en la bandeja · Entonces"
            + " aparece marcada como no decidible (R-SEG-04)")
    void laPropiaNoSeOfreceParaDecidir() {
        Solicitante s = solicitante();
        pedirGrande(s, "bandeja-propia");

        BandejaDeRetiros.Pagina bandeja = pagina(operador(s.usuario()), Optional.empty(), Optional.empty(), 25);

        assertThat(bandeja.items()).hasSize(1);
        assertThat(bandeja.items().get(0).puedeDecidir()).isFalse();
    }

    @Test
    @DisplayName("rechaza un cursor a medias: despuesDeFecha sin despuesDeId es AP-CU11-14, no una pagina inventada")
    void cursorIncompleto() {
        ContextoSesion aprobador = operador(fixtura.usuario());

        assertThatThrownBy(() -> pagina(aprobador, Optional.of(OffsetDateTime.now()), Optional.empty(), 25))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU11-14"));
        assertThatThrownBy(() -> pagina(aprobador, Optional.empty(), Optional.of(UUID.randomUUID()), 25))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU11-14"));
    }
}

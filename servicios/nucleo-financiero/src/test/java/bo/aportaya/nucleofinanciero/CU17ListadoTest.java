package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad.EntradaBloqueo;
import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad.SalidaBloqueo;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-17 · la bandeja de bloqueos del backoffice de Cumplimiento, contra PostgreSQL real: orden determinista con
 * desempate estable, filtro por estado, cursor que no repite ni salta filas, el importe con la moneda de la cuenta y
 * la marca {@code puedeLevantar} (R-BIL-32).
 */
class CU17ListadoTest extends BaseDeBilletera {

    private static final String HASH = "c".repeat(64);

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private UUID cuentaCon(UUID usuario, String saldo) {
        fixtura.tipoDeCambioDeHoy();
        UUID cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal(saldo));
        return cuenta;
    }

    private SalidaBloqueo bloquear(UUID cuenta, String monto, ContextoSesion quien) {
        return transaccion.execute(t -> bloqueoCU.ejecutar(
                new EntradaBloqueo(
                        cuenta,
                        "UIF",
                        "INMOVILIZACION",
                        "OF-" + UUID.randomUUID().toString().substring(0, 8),
                        Optional.of(Dinero.de(monto, Moneda.BOB)),
                        "PARCIAL",
                        "https://oficios/sintetico.pdf",
                        HASH),
                quien));
    }

    private bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad.PaginaDeBloqueos listar(
            Optional<String> estado,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        return transaccion.execute(t -> bloqueoCU.listar(estado, antesDe, antesDeId, limite, ctx));
    }

    private static List<UUID> ids(
            bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad.PaginaDeBloqueos pagina) {
        return pagina.items().stream().map(f -> f.bloqueoId()).toList();
    }

    @Test
    @DisplayName(
            "cuadre: tres bloqueos en paginas de dos salen del mas reciente al mas antiguo, sin repetir ni saltar ninguno")
    void cuadrePaginado() {
        UUID oficial = fixtura.usuario();
        ContextoSesion registra = contextoDe(oficial);
        UUID a = bloquear(cuentaCon(fixtura.usuario(), "1000.00"), "100.00", registra)
                .bloqueoId();
        UUID b = bloquear(cuentaCon(fixtura.usuario(), "1000.00"), "200.00", registra)
                .bloqueoId();
        UUID c = bloquear(cuentaCon(fixtura.usuario(), "1000.00"), "300.00", registra)
                .bloqueoId();
        var todos = List.of(a, b, c);

        var primera = listar(Optional.empty(), Optional.empty(), Optional.empty(), 2, registra);
        var segunda = listar(Optional.empty(), primera.proximoAntesDe(), primera.proximoAntesDeId(), 2, registra);

        var vistos = new java.util.ArrayList<UUID>(ids(primera));
        vistos.addAll(ids(segunda));
        assertThat(vistos.stream().filter(todos::contains).toList()).containsExactlyInAnyOrderElementsOf(todos);
        assertThat(vistos).doesNotHaveDuplicates();
        assertThat(primera.items()).hasSize(2);
        // El orden es (recibido_en, id) descendente: el desempate por id lo hace estable aunque compartan el instante.
        var orden = vistos.stream()
                .map(id -> dsl.fetchOne("SELECT recibido_en, id FROM nucleo_financiero.bloqueo_saldo WHERE id = ?", id))
                .toList();
        for (int i = 1; i < orden.size(); i++) {
            var antes = orden.get(i - 1);
            var despues = orden.get(i);
            int porFecha = antes.get(0, OffsetDateTime.class).compareTo(despues.get(0, OffsetDateTime.class));
            assertThat(porFecha > 0
                            || (porFecha == 0 && antes.get(1, UUID.class).compareTo(despues.get(1, UUID.class)) > 0))
                    .as("fila %d antes que %d", i - 1, i)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("cuadre: el importe sale con la moneda de la cuenta y quien registro no ve el boton de levantar")
    void cuadreMarcaYMoneda() {
        UUID oficial = fixtura.usuario();
        UUID otro = fixtura.usuario();
        SalidaBloqueo bloqueo = bloquear(cuentaCon(fixtura.usuario(), "1000.00"), "250.00", contextoDe(oficial));

        var delQueRegistro =
                listar(Optional.of("VIGENTE"), Optional.empty(), Optional.empty(), 100, contextoDe(oficial));
        var deOtro = listar(Optional.of("VIGENTE"), Optional.empty(), Optional.empty(), 100, contextoDe(otro));

        var suya = delQueRegistro.items().stream()
                .filter(f -> f.bloqueoId().equals(bloqueo.bloqueoId()))
                .findFirst()
                .orElseThrow();
        assertThat(suya.puedeLevantar()).isFalse();
        assertThat(suya.registradoPor()).contains(oficial);
        assertThat(suya.montoBloqueado()).contains(Dinero.de("250.00", Moneda.BOB));
        assertThat(deOtro.items().stream()
                        .filter(f -> f.bloqueoId().equals(bloqueo.bloqueoId()))
                        .findFirst()
                        .orElseThrow()
                        .puedeLevantar())
                .isTrue();
    }

    @Test
    @DisplayName(
            "cuadre: levantado por otra persona, sale con LEVANTADO, quien lo levanto y sin boton; el filtro lo separa")
    void cuadreLevantado() {
        UUID oficial = fixtura.usuario();
        UUID jefe = fixtura.usuario();
        SalidaBloqueo bloqueo = bloquear(cuentaCon(fixtura.usuario(), "1000.00"), "100.00", contextoDe(oficial));
        transaccion.execute(t -> bloqueoCU.levantar(bloqueo.bloqueoId(), contextoDe(jefe)));

        var levantados = listar(Optional.of("LEVANTADO"), Optional.empty(), Optional.empty(), 100, contextoDe(jefe));
        var vigentes = listar(Optional.of("VIGENTE"), Optional.empty(), Optional.empty(), 100, contextoDe(jefe));

        var fila = levantados.items().stream()
                .filter(f -> f.bloqueoId().equals(bloqueo.bloqueoId()))
                .findFirst()
                .orElseThrow();
        assertThat(fila.estado()).isEqualTo("LEVANTADO");
        assertThat(fila.levantadoPor()).contains(jefe);
        assertThat(fila.puedeLevantar()).isFalse();
        assertThat(ids(vigentes)).doesNotContain(bloqueo.bloqueoId());
    }

    @Test
    @DisplayName("rechaza un cursor incompleto (AP-CU17-06): antesDe y antesDeId van juntos o ninguno")
    void rechazaCursorIncompleto() {
        ContextoSesion ctx = contextoDe(fixtura.usuario());

        assertThatThrownBy(() -> listar(Optional.empty(), Optional.of(OffsetDateTime.now()), Optional.empty(), 10, ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("cursor");
        assertThatThrownBy(() -> listar(Optional.empty(), Optional.empty(), Optional.of(UUID.randomUUID()), 10, ctx))
                .isInstanceOf(ErrorDeNegocio.class);
    }
}

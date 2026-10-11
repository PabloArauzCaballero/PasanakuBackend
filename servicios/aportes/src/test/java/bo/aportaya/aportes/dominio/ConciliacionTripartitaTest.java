package bo.aportaya.aportes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.dominio.ConciliacionTripartita.Causa;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.LineaDelProveedor;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.MovimientoDelBanco;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.PagoDelLibro;
import bo.aportaya.aportes.dominio.ConciliacionTripartita.Responsable;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H13.S1.M1 · Libro, proveedor y banco: cada diferencia con su causa y su responsable, sin fabricar abonos. */
class ConciliacionTripartitaTest {

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static PagoDelLibro libro(String ref, String monto) {
        return new PagoDelLibro(UUID.randomUUID(), ref, bob(monto));
    }

    private static LineaDelProveedor prov(String ref, String monto) {
        return new LineaDelProveedor(ref, bob(monto));
    }

    private static MovimientoDelBanco banco(String ref, String monto) {
        return new MovimientoDelBanco(UUID.randomUUID(), ref, bob(monto));
    }

    @Test
    @DisplayName(
            "CORRECTO · Dadas las tres fuentes coincidentes · Cuando se concilia · Entonces todo queda conciliado y no hay hallazgos")
    void todoCoincide() {
        var r = ConciliacionTripartita.conciliar(
                List.of(libro("R1", "500.00"), libro("R2", "300.50")),
                List.of(prov("R1", "500.00"), prov("R2", "300.50")),
                List.of(banco("R1", "500.00"), banco("R2", "300.50")));

        assertThat(r.conciliados()).hasSize(2);
        assertThat(r.hallazgos()).isEmpty();
    }

    @Test
    @DisplayName(
            "Dado un lote con una diferencia de cada tipo · Cuando se concilia · Entonces cada una sale con su causa y su responsable (lote de tres fuentes)")
    void unaDeCadaTipo() {
        var r = ConciliacionTripartita.conciliar(
                List.of(
                        libro("OK", "100.00"),
                        libro("SINPROV", "200.00"),
                        libro("MONTO", "300.00"),
                        libro("SINBANCO", "400.00"),
                        libro("DUP", "500.00"),
                        libro("DUP", "500.00"),
                        libro("BANCOMAL", "600.00")),
                List.of(
                        prov("OK", "100.00"),
                        prov("MONTO", "290.00"),
                        prov("SINBANCO", "400.00"),
                        prov("DUP", "500.00"),
                        prov("BANCOMAL", "600.00"),
                        prov("HUERFANAPROV", "700.00")),
                List.of(
                        banco("OK", "100.00"),
                        banco("SINPROV", "200.00"),
                        banco("MONTO", "290.00"),
                        banco("DUP", "500.00"),
                        banco("BANCOMAL", "599.00"),
                        banco("HUERFANABANCO", "800.00")));

        assertThat(r.conciliados()).extracting(c -> c.referencia()).containsExactly("OK");
        var porRef = r.hallazgos().stream().collect(Collectors.toMap(h -> h.referencia(), h -> h));
        assertThat(porRef.get("SINPROV").causa()).isEqualTo(Causa.SIN_CONFIRMACION_DEL_PROVEEDOR);
        assertThat(porRef.get("MONTO").causa()).isEqualTo(Causa.MONTO_DISTINTO);
        assertThat(porRef.get("MONTO").diferencia()).isEqualTo(bob("10.00"));
        assertThat(porRef.get("SINBANCO").causa()).isEqualTo(Causa.SIN_MOVIMIENTO_BANCARIO);
        assertThat(porRef.get("SINBANCO").responsable()).isEqualTo(Responsable.BANCO);
        assertThat(porRef.get("DUP").causa()).isEqualTo(Causa.PAGO_DUPLICADO);
        assertThat(porRef.get("DUP").pagos()).hasSize(2);
        assertThat(porRef.get("BANCOMAL").causa()).isEqualTo(Causa.MONTO_DISTINTO);
        assertThat(porRef.get("BANCOMAL").responsable()).isEqualTo(Responsable.BANCO);
        assertThat(porRef.get("BANCOMAL").diferencia()).isEqualTo(bob("1.00"));
        assertThat(porRef.get("HUERFANAPROV").causa()).isEqualTo(Causa.CONFIRMACION_SIN_PAGO);
        assertThat(porRef.get("HUERFANABANCO").causa()).isEqualTo(Causa.MOVIMIENTO_SIN_PAGO);
    }

    @Test
    @DisplayName(
            "Dado cualquier lote · Cuando se concilia · Entonces CADA referencia de entrada aparece exactamente una vez en el resultado (conservacion)")
    void conservacion() {
        var libro = List.of(libro("A", "10.00"), libro("B", "20.00"), libro("B", "20.00"), libro("C", "30.00"));
        var prov = List.of(prov("A", "10.00"), prov("C", "31.00"), prov("D", "40.00"));
        var banco = List.of(banco("A", "10.00"), banco("E", "50.00"), banco("E", "50.00"));

        var r = ConciliacionTripartita.conciliar(libro, prov, banco);

        Set<String> entrada = Stream.of(
                        libro.stream().map(x -> x.referencia()),
                        prov.stream().map(x -> x.referencia()),
                        banco.stream().map(x -> x.referencia()))
                .flatMap(s -> s)
                .collect(Collectors.toSet());
        List<String> salida = Stream.concat(
                        r.conciliados().stream().map(c -> c.referencia()),
                        r.hallazgos().stream().map(h -> h.referencia()))
                .toList();
        assertThat(salida).hasSize(entrada.size()).containsExactlyInAnyOrderElementsOf(entrada);
    }

    @Test
    @DisplayName(
            "INVALIDO · Dada una moneda distinta entre libro y proveedor · Cuando se concilia · Entonces MONEDA_DISTINTA a contabilidad, sin comparar importes")
    void monedaDistinta() {
        var r = ConciliacionTripartita.conciliar(
                List.of(libro("R", "100.00")),
                List.of(new LineaDelProveedor("R", Dinero.de("100.00", Moneda.USD))),
                List.of());

        assertThat(r.hallazgos()).hasSize(1);
        assertThat(r.hallazgos().get(0).causa()).isEqualTo(Causa.MONEDA_DISTINTA);
        assertThat(r.hallazgos().get(0).responsable()).isEqualTo(Responsable.CONTABILIDAD);
    }

    @Test
    @DisplayName(
            "LIMITE · Dado un lote vacio · Cuando se concilia · Entonces no hay nada; y un solo centavo de diferencia ya es un hallazgo")
    void vacioYUnCentavo() {
        assertThat(ConciliacionTripartita.conciliar(List.of(), List.of(), List.of())
                        .hallazgos())
                .isEmpty();

        var r = ConciliacionTripartita.conciliar(
                List.of(libro("R", "100.00")), List.of(prov("R", "99.99")), List.of(banco("R", "99.99")));
        assertThat(r.hallazgos().get(0).causa()).isEqualTo(Causa.MONTO_DISTINTO);
        assertThat(r.hallazgos().get(0).diferencia()).isEqualTo(bob("0.01"));
    }

    @Test
    @DisplayName(
            "SIN FABRICAR ABONOS · Dado cualquier hallazgo · Cuando se lee la sugerencia · Entonces nunca manda acreditar: investiga, consulta o espera")
    void nuncaSugiereAcreditar() {
        var r = ConciliacionTripartita.conciliar(
                List.of(libro("A", "10.00")), List.of(prov("B", "20.00")), List.of(banco("C", "30.00")));

        assertThat(r.hallazgos()).hasSize(3);
        assertThat(r.hallazgos()).allSatisfy(h -> {
            assertThat(h.que().toLowerCase()).doesNotStartWith("acreditar");
            assertThat(h.que().toLowerCase()).doesNotContain("acreditar ahora").doesNotContain("abonar");
        });
        assertThat(r.hallazgos().stream()
                        .filter(h -> h.causa() == Causa.MOVIMIENTO_SIN_PAGO)
                        .findFirst()
                        .orElseThrow()
                        .que())
                .contains("NO acreditar");
        assertThat(r.conciliados()).isEmpty();
    }
}

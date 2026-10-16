package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarTesoreria;
import bo.aportaya.nucleofinanciero.infraestructura.TesoreriaRepositorio;
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
 * CU-14 · el listado de reversos que leen Contabilidad y Auditoria en el backoffice (carril A · H8).
 *
 * <p>{@code reverso_transaccion} es append-only: las filas de otras pruebas quedan en el contenedor.
 * Por eso se recorren TODAS las paginas y se afirma sobre las filas propias, no sobre posiciones.
 */
class CU14ReversosTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";
    private static final String MOTIVO = "Cobro duplicado por un reintento del proveedor de pagos";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private static ConsultarTesoreria consultas() {
        return new ConsultarTesoreria(new Datos(dsl), new TesoreriaRepositorio());
    }

    private static ContextoSesion operador(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    /** Una transferencia aplicada de {@code monto}: es lo que se reversa. */
    private UUID transferencia(String monto) {
        UUID quienPaga = fixtura.usuario();
        UUID origen = fixtura.billetera(quienPaga, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal("1000.00"));
        UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
        return transaccion
                .execute(x -> transferenciaCU.ejecutar(
                        new EntradaTransferencia(
                                "mov-" + UUID.randomUUID(),
                                origen,
                                destino,
                                Dinero.de(monto, Moneda.BOB),
                                "cobro",
                                Optional.empty(),
                                Optional.empty()),
                        contextoDe(quienPaga)))
                .transaccionId();
    }

    private List<ConsultarTesoreria.Reverso> todos(List<String> estados, ContextoSesion ctx) {
        var filas = new ArrayList<ConsultarTesoreria.Reverso>();
        Optional<OffsetDateTime> antesDe = Optional.empty();
        Optional<UUID> antesDeId = Optional.empty();
        do {
            var a = antesDe;
            var b = antesDeId;
            var pagina = transaccion.execute(t -> consultas().reversos(estados, a, b, 2, ctx));
            filas.addAll(pagina.items());
            antesDe = pagina.proximoAntesDe();
            antesDeId = pagina.proximoAntesDeId();
        } while (antesDeId.isPresent());
        return filas;
    }

    @Test
    @DisplayName(
            "Dados reversos en estado SOLICITADO y EJECUTADO · Cuando Auditoría lista los reversos filtrando por EJECUTADO · Entonces solo recibe los ejecutados, con el monto y quien lo autorizó")
    void filtraPorEstado() {
        // El catalogo una sola vez: el limite tiene exclusion por vigencia.
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID original = transferencia("300.00");
        UUID autoriza = fixtura.usuario();
        UUID ejecutado = transaccion
                .execute(x -> reversoCU.ejecutar(
                        new EntradaReverso(
                                "rev-lst-" + UUID.randomUUID(), original, "ERROR_OPERATIVO", MOTIVO, autoriza),
                        contextoDe(fixtura.usuario())))
                .reversoId();
        UUID otraOriginal = transferencia("40.00");
        UUID solicitado = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO nucleo_financiero.reverso_transaccion
                    (id, transaccion_original_id, autorizada_por, tipo, motivo,
                     monto_reversado, estado, solicitada_en)
                VALUES (?, ?, ?, 'ANULACION', 'pendiente de ejecutar', 40.00, 'SOLICITADO', now())
                """,
                solicitado,
                otraOriginal,
                autoriza);
        ContextoSesion ctx = operador(fixtura.usuario());

        var ejecutados = todos(List.of("EJECUTADO"), ctx);
        assertThat(ejecutados).allSatisfy(r -> assertThat(r.estado()).isEqualTo("EJECUTADO"));
        assertThat(ejecutados)
                .filteredOn(r -> r.reversoId().equals(ejecutado))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.montoReversado()).isEqualByComparingTo("300.00");
                    assertThat(r.autorizadaPor()).isEqualTo(autoriza);
                    assertThat(r.transaccionOriginalId()).isEqualTo(original);
                    assertThat(r.transaccionReversoId()).isPresent();
                });
        assertThat(ejecutados).noneMatch(r -> r.reversoId().equals(solicitado));

        // Sin filtro van todos los estados: el SOLICITADO aparece, sin reverso aplicado todavia.
        assertThat(todos(List.of(), ctx))
                .filteredOn(r -> r.reversoId().equals(solicitado))
                .singleElement()
                .satisfies(r -> assertThat(r.transaccionReversoId()).isEmpty());
    }

    @Test
    @DisplayName("rechaza un estado que el contrato no conoce con AP-CU14-04 en vez de devolver una lista vacia")
    void rechazaEstadoDesconocido() {
        ContextoSesion ctx = operador(fixtura.usuario());
        assertThatThrownBy(() -> transaccion.execute(
                        t -> consultas().reversos(List.of("INVENTADO"), Optional.empty(), Optional.empty(), 10, ctx)))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU14-04"));
    }

    @Test
    @DisplayName("rechaza un cursor a medias en el listado de reversos con AP-CU14-04")
    void rechazaCursorAMedias() {
        ContextoSesion ctx = operador(fixtura.usuario());
        assertThatThrownBy(() -> transaccion.execute(t ->
                        consultas().reversos(List.of(), Optional.of(OffsetDateTime.now()), Optional.empty(), 10, ctx)))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU14-04"));
    }
}

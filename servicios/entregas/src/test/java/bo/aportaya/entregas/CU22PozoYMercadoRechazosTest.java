package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-22 · las pruebas de RECHAZO de las restricciones del pozo completo y del mercado de
 * turnos (R-DES-03, R-DES-04 y R-DES-05), una por restriccion citada, mas el pago en efectivo al organizador.
 *
 * <p>Lo que se prueba aca es que la BASE los rechaza aunque alguien llegue por fuera del caso
 * de uso. El mercado y la subasta de turnos se retiraron (D-L2-1): las filas de oferta y de
 * cesion se escriben por SQL, porque ningun caso de uso las crea ya, y las tablas con sus
 * restricciones siguen vivas hasta la contraccion del esquema (regla 97.3.5).
 */
class CU22PozoYMercadoRechazosTest extends BaseDeEntregas {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private record Caso(UUID vendedor, FixturaDeEntregas.Escenario e, FixturaDeEntregas.Miembro comprador) {}

    private Caso caso() {
        UUID vendedor = fixtura.usuario();
        var e = fixtura.escenario(vendedor);
        return new Caso(vendedor, e, fixtura.miembro(e.grupoId()));
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    /** Una oferta del turno del caso, escrita directo en la base. */
    private UUID oferta(Caso c, String estado) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO entregas.oferta_turno
                    (id, grupo_id, turno_id, cupo_id, participante_origen_id, vendedor_usuario_id, moneda,
                     monto_derecho, monto_precio, monto_cargos, estado, vigente_hasta, publicada_en,
                     clave_idempotencia, version)
                VALUES (?, ?, ?, ?, ?, ?, 'BOB', 6000.00, 5500.00, 50.00, ?, now() + interval '1 day', now(), ?, 0)
                """,
                id,
                c.e().grupoId(),
                c.e().turnoId(),
                c.e().cupoId(),
                c.e().participanteId(),
                c.vendedor(),
                estado,
                "oferta-" + id);
        return id;
    }

    /** Una cesion LIQUIDADA de la oferta, con retencion, titulo y liquidacion como las dejaba la compra. */
    private UUID cesionLiquidada(Caso c, UUID ofertaId) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO entregas.cesion_derecho
                    (id, oferta_turno_id, turno_id, participante_origen_id, participante_destino_id,
                     comprador_usuario_id, moneda, monto_precio, estado, retencion_ref, liquidacion_ref,
                     clave_idempotencia, creada_en, titulo_asignado_en, liquidada_en, version)
                VALUES (?, ?, ?, ?, ?, ?, 'BOB', 5500.00, 'LIQUIDADA', gen_random_uuid(), gen_random_uuid(),
                        ?, now(), now(), now(), 0)
                """,
                id,
                ofertaId,
                c.e().turnoId(),
                c.e().participanteId(),
                c.comprador().participanteId(),
                c.comprador().usuarioId(),
                "cesion-" + id);
        return id;
    }

    @Test
    @DisplayName("rechaza por R-DES-03: un fondeo que no cuadra o un pendiente sin deuda")
    void rechazaRDES03() {
        var c = caso();
        aportesDoble.responde(new RecaudoDelPozo.Recaudo(
                c.e().periodoId(), c.e().grupoId(), OffsetDateTime.now(), bob("6000.00"), bob("6000.00"), bob("0.00")));
        var salida = pozoCU.fondear(
                new CU22EntregarPozoCompleto.Entrada(
                        c.e().grupoId(),
                        c.e().periodoId(),
                        c.e().turnoId(),
                        c.e().cupoId(),
                        c.e().participanteId(),
                        "BILLETERA_MOVIL",
                        LocalDate.now(),
                        "fondeo-" + c.e().turnoId()),
                contextoDe(c.vendedor()));

        assertThat(rechazaLaBase(
                        "UPDATE entregas.fondeo_entrega SET monto_confirmado = monto_confirmado - 1 WHERE id = ?",
                        salida.fondeoId()))
                .contains("ck_fondeo_entrega_cuadra");
        assertThat(rechazaLaBase(
                        "UPDATE entregas.fondeo_entrega SET estado = 'CON_PENDIENTE' WHERE id = ?", salida.fondeoId()))
                .contains("ck_fondeo_entrega_estado");
    }

    @Test
    @DisplayName("rechaza una entrega pagada en efectivo al organizador: el organizador no es caja (RN-18)")
    void rechazaEfectivoAlOrganizador() {
        var c = caso();
        aportesDoble.responde(new RecaudoDelPozo.Recaudo(
                c.e().periodoId(), c.e().grupoId(), OffsetDateTime.now(), bob("6000.00"), bob("6000.00"), bob("0.00")));
        var salida = pozoCU.fondear(
                new CU22EntregarPozoCompleto.Entrada(
                        c.e().grupoId(),
                        c.e().periodoId(),
                        c.e().turnoId(),
                        c.e().cupoId(),
                        c.e().participanteId(),
                        "BILLETERA_MOVIL",
                        LocalDate.now(),
                        "fondeo-" + c.e().turnoId()),
                contextoDe(c.vendedor()));

        // El CHECK del enum del modelo ya no tiene EFECTIVO_ORGANIZADOR (F1.L2.M09). Rige en toda base creada desde
        // este modelo, que es la del contenedor de la prueba.
        assertThat(rechazaLaBase(
                        "UPDATE entregas.entrega_fondo SET metodo_desembolso = 'EFECTIVO_ORGANIZADOR' WHERE id = ?",
                        salida.entregaId()))
                .contains("ck_entrega_fondo_metodo_desembolso");
    }

    @Test
    @DisplayName("rechaza por R-DES-04: una segunda oferta viva del mismo turno")
    void rechazaRDES04() {
        var c = caso();
        UUID primera = oferta(c, "PUBLICADA");

        String error = rechazaLaBase(
                """
                INSERT INTO entregas.oferta_turno
                    (id, grupo_id, turno_id, cupo_id, participante_origen_id, vendedor_usuario_id, moneda,
                     monto_derecho, monto_precio, monto_cargos, estado, vigente_hasta, publicada_en,
                     clave_idempotencia, version)
                SELECT gen_random_uuid(), grupo_id, turno_id, cupo_id, participante_origen_id, vendedor_usuario_id,
                       moneda, monto_derecho, monto_precio, monto_cargos, 'PUBLICADA', vigente_hasta, now(),
                       'directa-' || gen_random_uuid(), 0
                  FROM entregas.oferta_turno WHERE id = ?""",
                primera);

        assertThat(error).contains("uq_oferta_turno_activa");
    }

    @Test
    @DisplayName(
            "rechaza por R-DES-05: una cesion liquidada sin referencias, entre la misma parte o repetida en el turno")
    void rechazaRDES05() {
        var c = caso();
        UUID cesion = cesionLiquidada(c, oferta(c, "VENDIDA"));

        assertThat(rechazaLaBase("UPDATE entregas.cesion_derecho SET liquidacion_ref = NULL WHERE id = ?", cesion))
                .contains("ck_cesion_derecho_liquidada");
        assertThat(rechazaLaBase(
                        """
                        INSERT INTO entregas.cesion_derecho (id, oferta_turno_id, turno_id, participante_origen_id, participante_destino_id, comprador_usuario_id, moneda, monto_precio, estado, clave_idempotencia, creada_en, version)
                        SELECT gen_random_uuid(), oferta_turno_id, turno_id, participante_origen_id, participante_destino_id, comprador_usuario_id, moneda, monto_precio, 'VALIDADA', 'otra-' || gen_random_uuid(), now(), 0
                        FROM entregas.cesion_derecho WHERE id = ?""",
                        cesion))
                .contains("uq_cesion_derecho_turno_viva");
        assertThat(rechazaLaBase(
                        "UPDATE entregas.cesion_derecho SET participante_destino_id = participante_origen_id WHERE id = ?",
                        cesion))
                .contains("ck_cesion_derecho_partes");
    }

    @Test
    @DisplayName("rechaza por R-DES-06: una incidencia de persona sin autor, o una del sistema con autor")
    void rechazaRDES06() {
        var c = caso();
        UUID entrega = entregaFondeada(c.e(), "BILLETERA_MOVIL", contextoDe(c.vendedor()));
        String alta =
                """
                INSERT INTO entregas.incidencia_entrega
                    (id, entrega_id, tipo, severidad, descripcion, reportada_por, origen, estado, sla_horas,
                     fecha_limite_sla, evidencias, abierta_en)
                VALUES (gen_random_uuid(), ?, 'FONDO_INCOMPLETO', 'CRITICA', 'prueba', CAST(? AS uuid),
                        CAST(? AS varchar), 'ABIERTA', 24,
                        now() + interval '24 hours', '[]'::jsonb, now())
                """;

        assertThat(rechazaLaBase(alta, entrega, null, "PERSONA")).contains("ck_incidencia_entrega_autor");
        assertThat(rechazaLaBase(alta, entrega, c.vendedor(), "SISTEMA")).contains("ck_incidencia_entrega_autor");
        assertThat(rechazaLaBase(alta, entrega, null, null)).contains("ck_incidencia_entrega_autor");
        assertThat(rechazaLaBase(alta, entrega, null, "SISTEMA")).isEmpty();
        assertThat(rechazaLaBase(alta, entrega, c.vendedor(), "PERSONA")).isEmpty();
    }
}

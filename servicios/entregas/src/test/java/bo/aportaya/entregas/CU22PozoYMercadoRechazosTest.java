package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-22 · las pruebas de RECHAZO de las restricciones del pozo completo y del mercado de
 * turnos (R-DES-03, R-DES-04 y R-DES-05), una por restriccion citada.
 *
 * <p>La aplicacion ya valida estos casos; lo que se prueba aca es que la BASE los rechaza
 * aunque alguien llegue por fuera del caso de uso. El escenario completo de cada camino
 * vive en {@code CU133PozoCompletoTest} y en {@code CU130OfertasTest} / {@code CU130CompraTest}.
 */
class CU22PozoYMercadoRechazosTest extends BaseDeMercado {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
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
                c.ctxVendedor());

        assertThat(rechazaLaBase(
                        "UPDATE entregas.fondeo_entrega SET monto_confirmado = monto_confirmado - 1 WHERE id = ?",
                        salida.fondeoId()))
                .contains("ck_fondeo_entrega_cuadra");
        assertThat(rechazaLaBase(
                        "UPDATE entregas.fondeo_entrega SET estado = 'CON_PENDIENTE' WHERE id = ?", salida.fondeoId()))
                .contains("ck_fondeo_entrega_estado");
    }

    @Test
    @DisplayName("rechaza por R-DES-04: una segunda oferta viva del mismo turno")
    void rechazaRDES04() {
        var c = caso();
        var primera = publicar(c);

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
                primera.ofertaId());

        assertThat(error).contains("uq_oferta_turno_activa");
    }

    @Test
    @DisplayName(
            "rechaza por R-DES-05: una cesion liquidada sin referencias, entre la misma parte o repetida en el turno")
    void rechazaRDES05() {
        var c = caso();
        var oferta = publicar(c);
        var r = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());

        assertThat(rechazaLaBase(
                        "UPDATE entregas.cesion_derecho SET liquidacion_ref = NULL WHERE id = ?", r.cesionId()))
                .contains("ck_cesion_derecho_liquidada");
        assertThat(rechazaLaBase(
                        """
                        INSERT INTO entregas.cesion_derecho (id, oferta_turno_id, turno_id, participante_origen_id, participante_destino_id, comprador_usuario_id, moneda, monto_precio, estado, clave_idempotencia, creada_en, version)
                        SELECT gen_random_uuid(), oferta_turno_id, turno_id, participante_origen_id, participante_destino_id, comprador_usuario_id, moneda, monto_precio, 'VALIDADA', 'otra-' || gen_random_uuid(), now(), 0
                        FROM entregas.cesion_derecho WHERE id = ?""",
                        r.cesionId()))
                .contains("uq_cesion_derecho_turno_viva");
        assertThat(rechazaLaBase(
                        "UPDATE entregas.cesion_derecho SET participante_destino_id = participante_origen_id WHERE id = ?",
                        r.cesionId()))
                .contains("ck_cesion_derecho_partes");
    }
}

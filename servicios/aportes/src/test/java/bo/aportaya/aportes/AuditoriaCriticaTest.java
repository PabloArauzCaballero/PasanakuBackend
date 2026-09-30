package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago.EntradaReembolso;
import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago.SalidaSolicitud;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.SalidaCobro;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H5/H7.S6 del carril PR4-seguridad: toda operación crítica deja rastro append-only
 * en {@code comun.bitacora_evento}.
 *
 * <p><b>Hallazgo, declarado antes de este test</b>: {@code comun.bitacora_evento}
 * existía en SQL (cadena de hash, bloqueo consultivo) desde antes de este carril,
 * pero ningún servicio la escribía (`grep -rn "bitacora_evento" --include=*.java
 * servicios` → cero en código de producción). En {@code aportes} se corrigió: la
 * aprobación de un reembolso (la única operación de "aprobación" de este servicio)
 * ahora escribe la bitácora (ver {@code AuditoriaRepositorio},
 * {@code CU19ReembolsarPago.aprobar}). En {@code identidad} (login, MFA) y
 * {@code nucleo-financiero} (retiro, transferencia) el mismo hueco existe y sigue
 * abierto — hallazgo entregado a sus dueños (Richard, Justin) en el daily de este
 * carril; no se edita código ajeno.
 */
class AuditoriaCriticaTest extends BaseDeAportes {

    // `comun.bitacora_evento` es APPEND-ONLY de verdad (R-AUD-01, disparador
    // `fn_aud_bloquear_mutacion`): un `DELETE` la rechaza, tal como tiene que
    // hacerlo — encontrado corriendo este test contra PostgreSQL real. No se
    // limpia entre pruebas a proposito; cada caso usa UUIDs propios (el
    // `entidad_id` es el `reembolsoId`, siempre nuevo) asi que no hay colision.
    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName("aprobar un reembolso (operación crítica) deja una fila append-only en la bitácora encadenada")
    void aprobarReembolsoQuedaAuditado() {
        UUID solicitante = fixtura.usuario();
        var obligacion = fixtura.obligacion(solicitante, "500.00", 10);
        ContextoSesion ctxSolicitante = contextoDe(solicitante);
        SalidaCobro cobro = transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        "cob-audit-1",
                        obligacion.id(),
                        bob("500.00"),
                        bob("0.00"),
                        "BILLETERA_MOVIL",
                        "ref-audit-1",
                        Optional.empty(),
                        false,
                        true),
                ctxSolicitante));
        SalidaSolicitud solicitud = transaccion.execute(t -> reembolsoCU.solicitar(
                new EntradaReembolso(cobro.pagoId(), bob("500.00"), "DUPLICADO"), ctxSolicitante));

        UUID aprobadorId = fixtura.usuario();
        ContextoSesion ctxAprobador = contextoDe(aprobadorId);
        transaccion.execute(t -> reembolsoCU.aprobar(solicitud.reembolsoId(), ctxAprobador));

        assertThat(contar(
                        """
                        SELECT count(*)::int FROM comun.bitacora_evento
                         WHERE entidad = 'reembolso' AND entidad_id = ? AND accion = 'APROBACION'
                           AND actor_usuario_id = ?
                        """,
                        solicitud.reembolsoId(),
                        aprobadorId))
                .isEqualTo(1);

        // La cadena de hash no se puede saltar: la fila tiene su hash_registro
        // calculado por la base (R-AUD-09), no vacío ni una cadena de la aplicación.
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM comun.bitacora_evento
                         WHERE entidad_id = ? AND hash_registro IS NOT NULL AND length(hash_registro) = 64
                        """,
                        solicitud.reembolsoId()))
                .isEqualTo(1);

        // Sin secretos ni datos de terceros: el valor_nuevo solo lleva estado y el id
        // de la obligación, nunca un monto de otro pago ni un token.
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM comun.bitacora_evento
                         WHERE entidad_id = ? AND valor_nuevo::text ILIKE '%token%'
                        """,
                        solicitud.reembolsoId()))
                .isZero();
    }
}

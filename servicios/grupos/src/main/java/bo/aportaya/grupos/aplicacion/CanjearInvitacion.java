package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios.ConsumoInvitacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Una sola transacción local: invitación aceptada + solicitud pendiente + outbox. */
@Service
public class CanjearInvitacion {
    private final Datos datos;
    private final CU69Invitar invitaciones;
    private final CU68Postular postulaciones;

    public CanjearInvitacion(Datos datos, CU69Invitar invitaciones, CU68Postular postulaciones) {
        this.datos = datos;
        this.invitaciones = invitaciones;
        this.postulaciones = postulaciones;
    }

    @Transactional
    public UUID ejecutar(
            UUID invitacionId, ConsumoInvitacion recibo, CU68Postular.EntradaPostulacion entrada, ContextoSesion ctx) {
        if (recibo == null
                || !ctx.usuarioId().equals(recibo.usuarioId())
                || !entrada.grupoId().equals(recibo.grupoId())) throw invalida();
        String huella = huella(entrada.cuposSolicitados() + ":" + entrada.mensaje());
        return datos.conContexto(ctx, dsl -> {
            // Orden de bloqueos: grupo antes de invitación y solicitud.
            dsl.fetchOne("SELECT id FROM grupos.grupo WHERE id=? FOR UPDATE", entrada.grupoId());
            var fila = dsl.fetchOne("SELECT * FROM grupos.invitacion WHERE id=? FOR UPDATE", invitacionId);
            if (fila == null
                    || !recibo.tokenId().equals(fila.get("token_id", UUID.class))
                    || !recibo.grupoId().equals(fila.get("grupo_id", UUID.class))) throw invalida();
            UUID anterior = fila.get("solicitud_ingreso_id", UUID.class);
            if (anterior != null) {
                if (!recibo.clave().equals(fila.get("clave_consumo", UUID.class))
                        || !ctx.usuarioId().equals(fila.get("consumidor_id", UUID.class))
                        || !huella.equals(fila.get("huella_canje", String.class))) throw invalida();
                return anterior;
            }
            invitaciones.aceptar(invitacionId, recibo, ctx);
            var salida = postulaciones.postular(entrada, ctx);
            dsl.execute(
                    """
                UPDATE grupos.invitacion SET solicitud_ingreso_id=?,clave_consumo=?,consumidor_id=?,huella_canje=?
                WHERE id=?
                """,
                    salida.solicitudId(),
                    recibo.clave(),
                    ctx.usuarioId(),
                    huella,
                    invitacionId);
            return salida.solicitudId();
        });
    }

    private String huella(String valor) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private ErrorDeNegocio invalida() {
        return new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
    }
}

package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.FirmaDeOrganizadorRepositorio;
import bo.aportaya.identidad.infraestructura.clientes.OrganizadorPorHttp;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CU-90 vía básica, lado identidad: firmar el contrato de organizador Aprendiz y conceder el
 * rol ORGANIZADOR (GRUPO_CREAR, GRUPO_ADMINISTRAR).
 *
 * <p>Pasos, cada uno repetible sin duplicar: (1) KYC aprobado; (2) token FIRMA_CONTRATO como
 * evidencia, confirmado; (3) organizador firma el contrato con ese token; (4) organizador
 * confirma que la persona quedó habilitada; (5) recién ahí se concede el rol. Si algo falla en
 * el medio, repetir el pedido completa lo que faltaba.
 *
 * <p>Nadie se asigna un rol a sí mismo ({@code ck_asignacion_no_autoasignada}): el otorgante es
 * quien aprobó su KYC. El rol se apoya en esa aprobación más el contrato firmado.
 */
@Service
public class ActivarRolOrganizador {

    private final FirmaDeOrganizadorRepositorio firmas;
    private final OrganizadorPorHttp organizador;
    private final Reloj reloj;

    public ActivarRolOrganizador(FirmaDeOrganizadorRepositorio firmas, OrganizadorPorHttp organizador, Reloj reloj) {
        this.firmas = firmas;
        this.organizador = organizador;
        this.reloj = reloj;
    }

    /** @return {@code true} si el rol se concedió ahora; {@code false} si ya lo tenía. */
    public boolean ejecutar(
            UUID usuarioId, UUID contratoId, String contenidoHash, String ip, String agente, ContextoSesion ctx) {
        if (!usuarioId.equals(ctx.usuarioId())) {
            throw new ErrorDeNegocio(CodigoError.de(90, 12), "Solo podés firmar tu propio contrato de organizador.");
        }
        UUID otorgante = firmas.otorganteDelKyc(ctx)
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(90, 14),
                        "Tu cuenta todavía está en verificación. Cuando se apruebe vas a poder organizar grupos."));
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        UUID token = firmas.tokenDeFirma(ctx, contratoId, contenidoHash, ip, agente, ahora);
        organizador.firmar(contratoId, token);
        if (!organizador.habilitadoConContrato()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(90, 13),
                    "No pudimos firmar tu contrato de organizador. Volvé a intentarlo en un momento.");
        }
        return firmas.asignarOrganizador(ctx, otorgante, ahora);
    }
}

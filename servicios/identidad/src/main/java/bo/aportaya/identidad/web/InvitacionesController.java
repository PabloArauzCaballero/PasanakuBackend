package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.ConsumirInvitacion;
import bo.aportaya.identidad.web.generado.UsuariosApi;
import bo.aportaya.identidad.web.generado.modelo.CanjeInvitacion;
import bo.aportaya.identidad.web.generado.modelo.ConsumoInvitacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public abstract class InvitacionesController implements UsuariosApi {
    private final ConsumirInvitacion consumo;
    private final SesionDeLaPeticion sesion;
    private final HttpServletRequest peticion;

    public InvitacionesController(ConsumirInvitacion consumo, SesionDeLaPeticion sesion, HttpServletRequest peticion) {
        this.consumo = consumo;
        this.sesion = sesion;
        this.peticion = peticion;
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<ConsumoInvitacion> consumirInvitacion(
            UUID tokenId, UUID idempotencyKey, CanjeInvitacion cuerpo) {
        var origen = new ConsumirInvitacion.Origen(
                Optional.ofNullable(peticion.getRemoteAddr()).orElse("0.0.0.0"),
                Optional.ofNullable(peticion.getHeader("User-Agent")).orElse("desconocido"));
        var resultado = consumo.ejecutar(
                        tokenId, cuerpo.getGrupoId(), idempotencyKey, cuerpo.getToken(), origen, sesion.actual())
                .orElseThrow(this::invalida);
        return ResponseEntity.ok(new ConsumoInvitacion(
                resultado.tokenId(),
                resultado.grupoId(),
                resultado.usuarioId(),
                resultado.clave(),
                resultado.consumidoEn()));
    }

    @Override
    @Permiso("GRUPO_ADMINISTRAR")
    public ResponseEntity<Void> revocarInvitacion(UUID tokenId) {
        if (!consumo.revocar(tokenId, sesion.actual())) throw invalida();
        return ResponseEntity.noContent().build();
    }

    private ErrorDeNegocio invalida() {
        return new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
    }
}

package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU69Enlace;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.DetalleEnlaceInvitacion;
import bo.aportaya.grupos.web.generado.modelo.EntradaAceptacionInvitacion;
import bo.aportaya.grupos.web.generado.modelo.EntradaEnlaceInvitacion;
import bo.aportaya.grupos.web.generado.modelo.EntradaInvitacion;
import bo.aportaya.grupos.web.generado.modelo.SalidaAceptacionInvitacion;
import bo.aportaya.grupos.web.generado.modelo.SalidaInvitacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Coordina identidad y grupos antes de las transacciones del enlace de CU-69. */
@Component
public class InvitacionesWeb {
    private final CU69Invitar cu69;
    private final CU69Enlace enlaces;
    private final Consultas consultas;
    private final HechosDeOtrosServicios afuera;
    private final SesionDeLaPeticion sesion;
    private final HttpServletRequest peticion;
    private final int topeDeReenvios;

    public InvitacionesWeb(
            CU69Invitar cu69,
            CU69Enlace enlaces,
            Consultas consultas,
            HechosDeOtrosServicios afuera,
            SesionDeLaPeticion sesion,
            HttpServletRequest peticion,
            @Value("${aportaya.grupo.tope-de-reenvios-de-invitacion}") int topeDeReenvios) {
        this.cu69 = cu69;
        this.enlaces = enlaces;
        this.consultas = consultas;
        this.afuera = afuera;
        this.sesion = sesion;
        this.peticion = peticion;
        this.topeDeReenvios = topeDeReenvios;
    }

    /**
     * Resuelve identidad fuera de la transaccion y emite el enlace con la clave del cliente: reintentar
     * recupera el mismo enlace. Si la invitacion no llega a existir, el enlace emitido se revoca.
     */
    public SalidaInvitacion invitar(UUID grupoId, UUID clave, EntradaInvitacion cuerpo) {
        var ctx = sesion.actual();
        cu69.comprobarEmisor(grupoId, ctx);
        String telefono = cuerpo.getTelefonoInvitado();
        boolean suprimido = afuera.contactoSuprimido(telefono, "INVITACION_GRUPO");
        boolean yaEsta = afuera.usuarioDelTelefono(telefono)
                .map(usuario -> consultas.yaEsParticipante(grupoId, usuario, ctx))
                .orElse(false);
        var token = suprimido || yaEsta
                ? null
                : afuera.tokenDeInvitacion(clave, grupoId, cuerpo.getCanal().getValue(), telefono);
        CU69Invitar.Resultado salida;
        try {
            salida = cu69.invitar(
                    new CU69Invitar.EntradaInvitacion(
                            grupoId,
                            telefono,
                            cuerpo.getNombreSugerido(),
                            cuerpo.getCanal().getValue(),
                            suprimido,
                            yaEsta,
                            topeDeReenvios,
                            token == null ? null : token.tokenId(),
                            token == null ? null : token.expiraEn()),
                    ctx);
        } catch (ErrorDeNegocio rechazada) {
            // Un enlace emitido que no llego a ser invitacion no debe quedar vivo sin dueno.
            // El 69-06 es otra invitacion con esa clave: ese enlace SI tiene dueno.
            if (token != null && !"AP-CU69-06".equals(rechazada.codigo().valor())) {
                afuera.revocarTokenDeInvitacion(token.tokenId());
            }
            throw rechazada;
        }
        var respuesta = new SalidaInvitacion();
        salida.invitacionId().ifPresent(respuesta::setInvitacionId);
        respuesta.setMensaje(salida.mensaje());
        if (salida.invitacionId().isPresent() && token != null) {
            respuesta.setToken(token.token());
            respuesta.setExpiraEn(token.expiraEn());
            respuesta.setEnlace("aportaya://unirse/" + token.tokenId() + "." + token.token());
        }
        return respuesta;
    }

    public DetalleEnlaceInvitacion consultar(EntradaEnlaceInvitacion cuerpo) {
        var datos = enlaces.datosDe(cuerpo.getTokenId(), sesion.actual());
        exigirEnlaceValido(cuerpo.getTokenId(), cuerpo.getToken(), datos.telefono(), "NINGUNO");
        return new DetalleEnlaceInvitacion()
                .grupoId(datos.grupoId())
                .nombre(datos.nombre())
                .montoAporte(datos.montoAporte().toPlainString())
                .moneda(datos.moneda())
                .periodicidad(datos.periodicidad())
                .reglamento(datos.reglamento())
                .hashReglamento(datos.hashReglamento());
    }

    public SalidaAceptacionInvitacion aceptar(EntradaAceptacionInvitacion cuerpo) {
        var ctx = sesion.actual();
        var datos = enlaces.datosDe(cuerpo.getTokenId(), ctx);
        exigirEnlaceValido(cuerpo.getTokenId(), cuerpo.getToken(), datos.telefono(), datos.kycMinimo());
        if (afuera.restriccion(ctx.usuarioId()).vigente()) {
            throw new ErrorDeNegocio(CodigoError.de(68, 1), "Tenés una restricción vigente.");
        }
        var reputacion = afuera.reputacion(ctx.usuarioId());
        if (reputacion.puntaje().compareTo(datos.reputacionMinima()) < 0) {
            throw new ErrorDeNegocio(CodigoError.de(68, 3), "Tu reputación todavía no alcanza para este grupo.");
        }
        if (!Boolean.TRUE.equals(cuerpo.getAceptaReglamento())) {
            throw new ErrorDeNegocio(CodigoError.de(69, 5), "Tenés que aceptar el reglamento del grupo.");
        }
        var salida = enlaces.aceptar(
                cuerpo.getTokenId(), cuerpo.getHashReglamento(), peticion.getRemoteAddr(), reputacion.puntaje(), ctx);
        return new SalidaAceptacionInvitacion().grupoId(salida.grupoId()).participanteId(salida.participanteId());
    }

    private void exigirEnlaceValido(UUID tokenId, String token, String telefono, String kycMinimo) {
        if (!afuera.enlaceDeInvitacionValido(tokenId, token, telefono, kycMinimo)) {
            throw new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
        }
    }
}

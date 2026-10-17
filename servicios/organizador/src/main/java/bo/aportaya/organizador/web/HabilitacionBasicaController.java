package bo.aportaya.organizador.web;

import bo.aportaya.organizador.aplicacion.CU90ConsultarBandeja;
import bo.aportaya.organizador.aplicacion.CU90HabilitacionBasica;
import bo.aportaya.organizador.aplicacion.CU90ResolverHabilitacion;
import bo.aportaya.organizador.web.generado.modelo.EntradaHabilitacionBasica;
import bo.aportaya.organizador.web.generado.modelo.HabilitacionBasica;
import bo.aportaya.organizador.web.generado.modelo.SalidaHabilitacionBasica;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/**
 * CU-90 vía básica: el contrato de organizador Aprendiz que la persona acepta desde la app.
 * Rutas propias para que {@link OrganizadoresController} no pase el límite de tamaño.
 */
abstract class HabilitacionBasicaController extends RutasDeHabilitacion {
    private final CU90HabilitacionBasica basica;
    private final SesionDeLaPeticion sesion;

    protected HabilitacionBasicaController(
            CU90ResolverHabilitacion resolutor,
            CU90ConsultarBandeja bandeja,
            CU90HabilitacionBasica basica,
            SesionDeLaPeticion sesion) {
        super(resolutor, bandeja, sesion);
        this.basica = basica;
        this.sesion = sesion;
    }

    /** El contrato Aprendiz y si la persona ya lo aceptó. Solo lo propio: el usuario sale del token. */
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<HabilitacionBasica> consultarHabilitacionBasica() {
        var contexto = sesion.actual();
        Traza.marcarCasoDeUso("CU-90", contexto.usuarioId().toString());
        var contrato = basica.contrato();
        var estado = basica.estado(contexto);
        var salida = new HabilitacionBasica();
        salida.setVersion(contrato.version());
        salida.setTitulo(contrato.titulo());
        salida.setObligaciones(contrato.obligaciones());
        salida.setCausalesRescision(contrato.causalesRescision());
        salida.setLimiteDeGrupos(contrato.limiteDeGrupos());
        salida.setLimiteDeMonto(contrato.limiteDeMonto()
                .setScale(2, java.math.RoundingMode.HALF_EVEN)
                .toPlainString());
        salida.setHabilitado(estado.habilitado());
        salida.setOrganizadorId(estado.organizadorId());
        salida.setNivel(estado.nivel());
        return ResponseEntity.ok(salida);
    }

    /** Aceptar el contrato y quedar habilitado como Aprendiz (KYC aprobado = rol PARTICIPANTE). */
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaHabilitacionBasica> aceptarHabilitacionBasica(
            UUID idempotencyKey, EntradaHabilitacionBasica cuerpo) {
        var contexto = sesion.actual();
        Traza.marcarCasoDeUso("CU-90", contexto.usuarioId().toString());
        var resultado =
                basica.habilitar(Boolean.TRUE.equals(cuerpo.getAceptaContrato()), cuerpo.getVersion(), contexto);
        var salida = new SalidaHabilitacionBasica();
        salida.setOrganizadorId(resultado.organizadorId());
        salida.setContratoId(resultado.contratoId());
        salida.setContenidoHash(resultado.contenidoHash());
        salida.setNivel(resultado.nivel());
        salida.setEsNueva(resultado.esNueva());
        return ResponseEntity.ok(salida);
    }

    /** La firma del contrato Aprendiz, con el token FIRMA_CONTRATO que emitió identidad. */
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<bo.aportaya.organizador.web.generado.modelo.SalidaFirmaBasica> firmarHabilitacionBasica(
            bo.aportaya.organizador.web.generado.modelo.EntradaFirmaBasica cuerpo) {
        var contexto = sesion.actual();
        Traza.marcarCasoDeUso("CU-91", cuerpo.getContratoId().toString());
        var salida = new bo.aportaya.organizador.web.generado.modelo.SalidaFirmaBasica();
        salida.setFirmadoAhora(basica.firmar(cuerpo.getContratoId(), cuerpo.getTokenFirmaId(), contexto));
        return ResponseEntity.ok(salida);
    }
}

package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.TraspasoAdmisible;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.EntradaPostulacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Como se arma la entrada de CU-68 con los cuatro hechos que vienen de afuera.
 *
 * <p>Aceptar a alguien en un grupo mira su restriccion (de {@code garantia}), su
 * reputacion (de {@code transparencia}), cuantos morosos ya tiene el grupo (de
 * {@code aportes}) y si su KYC alcanza. Los cuatro se preguntan antes de la
 * transaccion.
 *
 * <p>Las afinidades son **del emparejamiento**, no de nadie: miden que tan parecido es
 * este grupo a lo que la persona busca. Cuando no hay historial con que compararlas
 * valen lo que diga {@code aportaya.grupo.afinidad-neutra} —el punto medio, ni a favor
 * ni en contra—, y ese numero esta en configuracion porque mueve a quien entra en que
 * grupo: es una decision de producto, no una constante (invariante 10).
 */
final class MapeoDePostulacion {

    private MapeoDePostulacion() {}

    static CU68Postular.EntradaPostulacion entrada(
            UUID grupoId,
            EntradaPostulacion cuerpo,
            Consultas consultas,
            HechosDeOtrosServicios afuera,
            BigDecimal afinidadNeutra,
            ContextoSesion ctx) {

        var politica = consultas
                .politicaDelGrupo(grupoId, ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(68, 4), "Ese grupo no existe."));
        var restriccion = afuera.restriccion(ctx.usuarioId());
        var reputacion = afuera.reputacion(ctx.usuarioId());
        int moroso = afuera.morososDelGrupo(grupoId);

        return new CU68Postular.EntradaPostulacion(
                grupoId,
                cuerpo.getCuposSolicitados().shortValue(),
                cuerpo.getMensaje(),
                restriccion.vigente(),
                restriccion.montoQueLaLevanta(),
                // El nivel lo dice identidad (fuera de la transaccion); aca solo se compara con
                // el minimo del grupo. Sin respuesta vale NINGUNO: no se deja pasar por omision.
                TraspasoAdmisible.NivelDeKyc.suficiente(afuera.nivelDeKyc(ctx.usuarioId()), politica.kycMinimo()),
                reputacion.puntaje().intValue(),
                moroso,
                afinidadNeutra,
                afinidadNeutra,
                afinidadNeutra,
                reputacion.tieneHistorial() ? BigDecimal.ONE : afinidadNeutra,
                // Sin historial (o sin respuesta del servicio) no es reputacion cero: es SIN_DATOS y lo ve una persona.
                !reputacion.tieneHistorial(),
                moroso == HechosDeOtrosServicios.SIN_DATO_DE_MOROSOS);
    }
}

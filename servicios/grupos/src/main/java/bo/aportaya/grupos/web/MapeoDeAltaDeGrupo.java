package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.dominio.GrupoNuevo;
import bo.aportaya.grupos.dominio.TraspasoAdmisible;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.EntradaGrupo;
import java.util.Optional;
import java.util.UUID;

/**
 * Como se arma la entrada de CU-20 con los tres hechos que no son de este servicio.
 *
 * <p>Un grupo no se abre si el organizador no esta habilitado, si no hay tarifario
 * vigente o si la licencia no cubre el servicio. Los tres los contestan otros
 * —{@code organizador}, {@code tarifas} y {@code cumplimiento}— y los tres se preguntan
 * **antes** de abrir la transaccion (invariante 6).
 *
 * <p>Cuando alguno no responde, la respuesta es la que no deja pasar. Abrir un pasanaku
 * porque el servicio que valida estaba caido es abrirlo sin precio congelado o sin
 * licencia, y eso lo paga la gente que entra.
 */
final class MapeoDeAltaDeGrupo {

    /** El mismo minimo con el que nace todo grupo (CreacionRepositorio). */
    private static final String KYC_MINIMO_DEL_CREADOR = "BASICO";

    private MapeoDeAltaDeGrupo() {}

    static CU20CrearGrupo.EntradaCreacion entrada(
            EntradaGrupo cuerpo,
            HechosDeOtrosServicios afuera,
            String codigoTarifario,
            String servicioDeLicencia,
            UUID usuarioId,
            UUID clave) {

        Optional<UUID> organizador = afuera.organizadorHabilitadoDelUsuario(usuarioId);
        if (organizador.isEmpty()
                || (cuerpo.getOrganizadorId() != null && !organizador.get().equals(cuerpo.getOrganizadorId()))) {
            throw new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                    bo.aportaya.plataforma.dominio.CodigoError.de(20, 2),
                    "El creador debe estar habilitado por backoffice y administrar su propio grupo.");
        }

        // Quien abre un grupo tiene que cumplir el minimo de verificacion que el grupo le va a exigir a
        // los demas (BASICO por omision, como lo fija el alta). Nivel dicho por identidad; sin
        // respuesta vale NINGUNO. Supuesto a confirmar con el oficial de cumplimiento.
        if (!TraspasoAdmisible.NivelDeKyc.suficiente(afuera.nivelDeKyc(usuarioId), KYC_MINIMO_DEL_CREADOR)) {
            throw new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                    bo.aportaya.plataforma.dominio.CodigoError.de(20, 5),
                    "Necesitas elevar tu nivel de verificacion antes de crear un grupo.");
        }

        return new CU20CrearGrupo.EntradaCreacion(
                new GrupoNuevo(
                        cuerpo.getNombre(),
                        MapeoDeGrupos.dinero(cuerpo.getMontoAporte()),
                        cuerpo.getPeriodicidad().getValue(),
                        cuerpo.getDiaCobro(),
                        cuerpo.getCupos(),
                        cuerpo.getFechaDeInicio()),
                organizador,
                true,
                afuera.tarifarioVigente(codigoTarifario),
                afuera.licenciaHabilita(servicioDeLicencia),
                Boolean.TRUE.equals(cuerpo.getPermitePermutaDeTurnos()),
                clave);
    }
}

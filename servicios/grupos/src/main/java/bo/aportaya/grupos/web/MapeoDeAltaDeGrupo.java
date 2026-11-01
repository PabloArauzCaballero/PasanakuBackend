package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.dominio.Etapas;
import bo.aportaya.grupos.dominio.GrupoNuevo;
import bo.aportaya.grupos.dominio.TraspasoAdmisible;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.EntradaGrupo;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
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

    /**
     * Si {@code repeticion} dice que la clave ya creo un grupo de quien llama, no se le pregunta la etapa a
     * {@code transparencia}: el caso de uso devuelve el alta original si la huella coincide, y la etapa de hoy no
     * puede cambiar esa respuesta (regla 96.3.3). Se pregunta recien antes de la etapa, no al principio.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    static CU20CrearGrupo.EntradaCreacion entrada(
            EntradaGrupo cuerpo,
            HechosDeOtrosServicios afuera,
            String codigoTarifario,
            String servicioDeLicencia,
            UUID usuarioId,
            UUID clave,
            java.util.function.BooleanSupplier repeticion) {

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

        // Cuantos grupos puede organizar a la vez (limite_grupos_simultaneos de su habilitacion). Sin
        // respuesta de organizador no se abre nada: abrir de mas porque el que controla estaba caido es
        // justo lo que el limite existe para impedir.
        var limite = afuera.limiteDeGruposDelOrganizador(organizador.get());
        if (limite.isEmpty()) {
            throw new bo.aportaya.plataforma.dominio.ErrorDeNegocio(
                    bo.aportaya.plataforma.dominio.CodigoError.de(20, 2),
                    "No pudimos confirmar cuantos grupos podes organizar. Proba de nuevo en un momento.");
        }

        var aporte = MapeoDeGrupos.dinero(cuerpo.getMontoAporte());
        return new CU20CrearGrupo.EntradaCreacion(
                new GrupoNuevo(
                        cuerpo.getNombre(),
                        aporte,
                        cuerpo.getPeriodicidad().getValue(),
                        cuerpo.getDiaCobro(),
                        cuerpo.getCupos(),
                        cuerpo.getFechaDeInicio()),
                organizador,
                true,
                afuera.tarifarioVigente(codigoTarifario),
                afuera.licenciaHabilita(servicioDeLicencia),
                Boolean.TRUE.equals(cuerpo.getPermitePermutaDeTurnos()),
                clave,
                limite.getAsInt(),
                afuera.contactoDeLaSesion()
                        .map(HechosDeOtrosServicios.Contacto::nombre)
                        .orElse(null),
                repeticion.getAsBoolean() ? BigDecimal.ZERO : etapaMinima(cuerpo, afuera, usuarioId, aporte),
                cuerpo.getEtapaMinima());
    }

    /**
     * Etapa minima y desbloqueo al crear (2026-10-30). La etapa del organizador la dice {@code transparencia}; sin
     * respuesta no se crea (falla cerrado, regla 98.2.4). La minima pedida tiene que estar en el catalogo y no por
     * encima de la propia (AP-CU20-08); omitida, es la propia. El desbloqueo VIGENTE de su etapa limita los cupos y el
     * aporte del grupo (AP-CU20-07). Devuelve el {@code desde} de la etapa minima: lo que guarda {@code reputacion_minima}.
     */
    private static BigDecimal etapaMinima(
            EntradaGrupo cuerpo, HechosDeOtrosServicios afuera, UUID usuarioId, Dinero aporte) {
        Etapas.DeLaPersona organizador = afuera.etapaDe(usuarioId).orElseThrow(EtapaDelIngreso::sinEtapa);
        Etapas.excedido(
                        organizador.etapa(),
                        cuerpo.getCupos(),
                        aporte.monto(),
                        aporte.moneda().name())
                .ifPresent(d -> {
                    throw EtapaDelIngreso.excedido(
                            CodigoError.de(20, 7),
                            d,
                            "Tu etapa todavia no desbloquea un grupo de ese tamaño o ese aporte.");
                });
        if (cuerpo.getEtapaMinima() == null)
            return EtapaDelIngreso.comoMinima(organizador.etapa().desde());
        Etapas.Etapa pedida = Etapas.porCodigo(organizador.catalogo(), cuerpo.getEtapaMinima())
                .filter(a -> a.orden() <= organizador.etapa().orden())
                .orElseThrow(() ->
                        new ErrorDeNegocio(CodigoError.de(20, 8), "Esa etapa no existe o esta por encima de la tuya."));
        return EtapaDelIngreso.comoMinima(pedida.desde());
    }
}

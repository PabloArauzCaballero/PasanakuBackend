package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU69FirmarReglamento;
import bo.aportaya.grupos.aplicacion.CU69IniciarGrupo;
import bo.aportaya.grupos.aplicacion.ConsultarDetalleDelGrupo;
import bo.aportaya.grupos.dominio.Etapas;
import bo.aportaya.grupos.dominio.InicioDelGrupo;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.DetalleDelGrupo;
import bo.aportaya.grupos.web.generado.modelo.EntradaFirmaDeReglamento;
import bo.aportaya.grupos.web.generado.modelo.EtapaDelIntegrante;
import bo.aportaya.grupos.web.generado.modelo.GrupoIniciado;
import bo.aportaya.grupos.web.generado.modelo.IntegranteDelGrupo;
import bo.aportaya.grupos.web.generado.modelo.ReglamentoParaFirmar;
import bo.aportaya.grupos.web.generado.modelo.SalidaFirmaDeReglamento;
import bo.aportaya.grupos.web.generado.modelo.SorteoDelDetalle;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El grupo visto por dentro (2026-10-20): su detalle con una fila por cupo y la firma del reglamento. Las dos
 * comprueban la pertenencia del lado del caso de uso, nunca por lo que diga el cliente.
 */
@Component
class IntegrantesDelGrupo {

    private final ConsultarDetalleDelGrupo detalle;
    private final CU69FirmarReglamento firma;
    private final HttpServletRequest peticion;
    private final bo.aportaya.grupos.aplicacion.ConsultarMisLimites limites;
    private final CU69IniciarGrupo inicio;
    private final HechosDeOtrosServicios afuera;
    private final int porcentajeParaIniciar;

    @SuppressWarnings("checkstyle:ParameterNumber")
    IntegrantesDelGrupo(
            ConsultarDetalleDelGrupo detalle,
            CU69FirmarReglamento firma,
            HttpServletRequest peticion,
            bo.aportaya.grupos.aplicacion.ConsultarMisLimites limites,
            CU69IniciarGrupo inicio,
            HechosDeOtrosServicios afuera,
            @Value("${aportaya.grupos.inicio.porcentaje}") int porcentajeParaIniciar) {
        this.detalle = detalle;
        this.firma = firma;
        this.peticion = peticion;
        this.limites = limites;
        this.inicio = inicio;
        this.afuera = afuera;
        this.porcentajeParaIniciar = porcentajeParaIniciar;
    }

    /** CU-69 · el organizador inicia el grupo al 75 %; la regla y la autorizacion sobre el grupo las decide el caso. */
    GrupoIniciado iniciar(UUID grupoId, UUID clave, ContextoSesion ctx) {
        var i = inicio.iniciar(grupoId, clave, ctx);
        return new GrupoIniciado(
                i.grupoId(), GrupoIniciado.EstadoEnum.fromValue(i.estado()), i.cuposTotales(), i.numPeriodos());
    }

    bo.aportaya.grupos.web.generado.modelo.MisLimites misLimites(ContextoSesion ctx) {
        var l = limites.consultar(ctx);
        var salida = new bo.aportaya.grupos.web.generado.modelo.MisLimites(
                l.organizados(), l.participaciones(), l.limiteDeParticipar());
        salida.setLimiteDeOrganizar(l.limiteDeOrganizar());
        return salida;
    }

    DetalleDelGrupo detalle(UUID grupoId, ContextoSesion ctx) {
        // Primero la pertenencia (AP-CU69-14, dentro del caso de uso): quien no integra el grupo no llega a que se le
        // pregunte a transparencia la etapa de nadie.
        var d = detalle.consultar(grupoId, ctx);
        var salida = new DetalleDelGrupo(
                d.grupoId(),
                d.nombre(),
                d.estado(),
                d.montoAporte().setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString(),
                d.moneda(),
                d.periodicidad(),
                d.cuposTotales(),
                d.cuposOcupados(),
                d.soyOrganizador(),
                d.debeFirmar(),
                d.puedeSortear(),
                integrantesConEtapa(d.integrantes()));
        salida.setReglamentoPendienteDeAceptar(d.reglamentoPendienteDeAceptar());
        if (d.reglamentoHash() != null) {
            var reglamento = new ReglamentoParaFirmar(d.reglamentoTexto(), d.reglamentoHash());
            reglamento.setVersion(d.reglamentoVersion());
            salida.setReglamento(reglamento);
        }
        if (d.sorteoId() != null) {
            salida.setSorteo(
                    new SorteoDelDetalle(d.sorteoId(), SorteoDelDetalle.EstadoEnum.fromValue(d.sorteoEstado())));
        }
        if (d.inicio() != null) inicioYEtapa(salida, d);
        return salida;
    }

    /**
     * La etapa de cada ocupante (decision del dueño 2026-10-30): codigo y nombre, nunca el puntaje, y solo a quien
     * integra el grupo (el caso de uso ya lo comprobo). UNA consulta a transparencia por detalle, con la identidad de
     * servicio de grupos, no una por integrante. Si no contesta, el detalle sale igual sin etapas: aca es informativa;
     * para decidir un ingreso se falla cerrado.
     */
    private java.util.List<IntegranteDelGrupo> integrantesConEtapa(
            java.util.List<ConsultarDetalleDelGrupo.Integrante> integrantes) {
        var ocupantes = integrantes.stream()
                .filter(i -> i.usuarioId() != null && !"LIBRE".equals(i.estado()))
                .map(ConsultarDetalleDelGrupo.Integrante::usuarioId)
                .distinct()
                .toList();
        java.util.Map<UUID, Etapas.Visible> etapas = ocupantes.isEmpty()
                ? java.util.Map.of()
                : afuera.etapasDe(ocupantes).orElse(java.util.Map.of());
        return integrantes.stream()
                .map(i -> {
                    var salida = integrante(i);
                    var etapa = "LIBRE".equals(i.estado()) || i.usuarioId() == null ? null : etapas.get(i.usuarioId());
                    if (etapa != null) salida.setEtapa(new EtapaDelIntegrante(etapa.codigo(), etapa.nombre()));
                    return salida;
                })
                .toList();
    }

    /**
     * El inicio al 75 % y la etapa minima (2026-10-30). {@code puedoIniciar} orienta a la app; lo que decide es el
     * servidor al iniciar. La etapa minima se nombra con el catalogo de {@code transparencia}: si no contesta, el
     * detalle sale sin ella (es informativa; el ingreso la exige igual, y ahi si falla cerrado).
     */
    private void inicioYEtapa(DetalleDelGrupo salida, ConsultarDetalleDelGrupo.Detalle d) {
        var i = d.inicio();
        salida.setMinimoParaIniciar(
                Math.max(1, InicioDelGrupo.minimoParaIniciar(d.cuposTotales(), porcentajeParaIniciar)));
        salida.setPuedoIniciar(d.soyOrganizador()
                && InicioDelGrupo.sePuedeIniciar(
                        d.estado(), i.firmados(), i.reservados(), d.cuposTotales(), porcentajeParaIniciar));
        if (i.reputacionMinima() != null && i.reputacionMinima().signum() > 0) {
            afuera.catalogoDeEtapas()
                    .flatMap(catalogo -> Etapas.codigoDeLaMinima(catalogo, i.reputacionMinima()))
                    .ifPresent(salida::setEtapaMinima);
        }
    }

    bo.aportaya.grupos.web.generado.modelo.PertenenciaCompartida comparten(
            UUID grupoId, UUID usuarioId, ContextoSesion ctx) {
        return new bo.aportaya.grupos.web.generado.modelo.PertenenciaCompartida(
                detalle.comparten(grupoId, usuarioId, ctx));
    }

    SalidaFirmaDeReglamento firmar(UUID grupoId, EntradaFirmaDeReglamento cuerpo, ContextoSesion ctx) {
        // Sin la aceptacion expresa no hay firma, aunque el hash sea el correcto.
        if (!Boolean.TRUE.equals(cuerpo.getAceptaReglamento())) {
            throw new ErrorDeNegocio(CodigoError.de(69, 5), "Para firmar hay que aceptar el reglamento.");
        }
        var f = firma.firmar(grupoId, cuerpo.getHashReglamento(), peticion.getRemoteAddr(), ctx);
        return new SalidaFirmaDeReglamento(f.participanteId(), f.estadoDelGrupo(), f.grupoConformado());
    }

    private static IntegranteDelGrupo integrante(ConsultarDetalleDelGrupo.Integrante i) {
        var salida = new IntegranteDelGrupo(
                i.numero(), IntegranteDelGrupo.EstadoEnum.fromValue(i.estado()), i.esOrganizador(), i.soyYo());
        salida.setCupoId(i.cupoId());
        salida.setUsuarioId(i.usuarioId());
        salida.setNombre(i.nombre());
        salida.setTurno(i.turno());
        return salida;
    }
}

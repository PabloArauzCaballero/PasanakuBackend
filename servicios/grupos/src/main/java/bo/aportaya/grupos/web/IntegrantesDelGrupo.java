package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU69FirmarReglamento;
import bo.aportaya.grupos.aplicacion.ConsultarDetalleDelGrupo;
import bo.aportaya.grupos.web.generado.modelo.DetalleDelGrupo;
import bo.aportaya.grupos.web.generado.modelo.EntradaFirmaDeReglamento;
import bo.aportaya.grupos.web.generado.modelo.IntegranteDelGrupo;
import bo.aportaya.grupos.web.generado.modelo.ReglamentoParaFirmar;
import bo.aportaya.grupos.web.generado.modelo.SalidaFirmaDeReglamento;
import bo.aportaya.grupos.web.generado.modelo.SorteoDelDetalle;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
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

    IntegrantesDelGrupo(ConsultarDetalleDelGrupo detalle, CU69FirmarReglamento firma, HttpServletRequest peticion) {
        this.detalle = detalle;
        this.firma = firma;
        this.peticion = peticion;
    }

    DetalleDelGrupo detalle(UUID grupoId, ContextoSesion ctx) {
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
                d.integrantes().stream().map(IntegrantesDelGrupo::integrante).toList());
        if (d.reglamentoHash() != null) {
            salida.setReglamento(new ReglamentoParaFirmar(d.reglamentoTexto(), d.reglamentoHash()));
        }
        if (d.sorteoId() != null) {
            salida.setSorteo(
                    new SorteoDelDetalle(d.sorteoId(), SorteoDelDetalle.EstadoEnum.fromValue(d.sorteoEstado())));
        }
        return salida;
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
        salida.setNombre(i.nombre());
        salida.setTurno(i.turno());
        return salida;
    }
}

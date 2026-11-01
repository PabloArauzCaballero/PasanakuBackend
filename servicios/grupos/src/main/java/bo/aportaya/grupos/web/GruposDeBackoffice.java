package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.ConsultarGruposDeBackoffice;
import bo.aportaya.grupos.web.generado.modelo.CupoDeBackoffice;
import bo.aportaya.grupos.web.generado.modelo.DetalleDeGrupoDeBackoffice;
import bo.aportaya.grupos.web.generado.modelo.GrupoDeBackoffice;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Los grupos vistos desde el backoffice (2026-10-29): mapeo del caso de uso al contrato. Solo lectura. */
@Component
class GruposDeBackoffice {

    private final ConsultarGruposDeBackoffice consultas;

    GruposDeBackoffice(ConsultarGruposDeBackoffice consultas) {
        this.consultas = consultas;
    }

    List<GrupoDeBackoffice> listar(String estado, Integer limite, ContextoSesion ctx) {
        return consultas.listar(estado, limite, ctx).stream()
                .map(GruposDeBackoffice::grupo)
                .toList();
    }

    DetalleDeGrupoDeBackoffice detalle(UUID grupoId, ContextoSesion ctx) {
        var d = consultas.detalle(grupoId, ctx);
        return new DetalleDeGrupoDeBackoffice(
                grupo(d.grupo()),
                d.cupos().stream().map(GruposDeBackoffice::cupo).toList());
    }

    private static GrupoDeBackoffice grupo(ConsultarGruposDeBackoffice.Grupo g) {
        var salida = new GrupoDeBackoffice(
                g.id(),
                g.codigo(),
                g.nombre(),
                g.estado(),
                // Decimal exacto en texto (regla 91.1.1): la escala del contrato es 2.
                g.montoAporte().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                g.moneda(),
                g.periodicidad(),
                g.cuposTotales(),
                g.cuposOcupados());
        salida.setFechaInicio(g.fechaInicio());
        return salida;
    }

    private static CupoDeBackoffice cupo(ConsultarGruposDeBackoffice.Cupo c) {
        var salida = new CupoDeBackoffice(c.numero(), c.estado(), c.esOrganizador());
        salida.setAlias(c.alias());
        salida.setEstadoParticipante(c.estadoParticipante());
        return salida;
    }
}

package bo.aportaya.organizador.aplicacion;

import bo.aportaya.organizador.dominio.DecisionDeHabilitacion;
import bo.aportaya.organizador.dominio.ExpedienteDeHabilitacion;
import bo.aportaya.organizador.infraestructura.HabilitacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-90 · Lectura de la bandeja y del expediente de habilitación.
 *
 * <p>La bandeja y el expediente ajeno son del backoffice; el postulante lee solo el suyo. La
 * paginación es por cursor (fecha, id) con tope configurable.
 */
@Service
public class CU90ConsultarBandeja {
    private final Datos datos;
    private final HabilitacionRepositorio habilitaciones;
    private final int topeDeBandeja;

    public CU90ConsultarBandeja(
            Datos datos,
            HabilitacionRepositorio habilitaciones,
            @Value("${aportaya.organizador.tope-de-bandeja}") int topeDeBandeja) {
        this.datos = datos;
        this.habilitaciones = habilitaciones;
        this.topeDeBandeja = topeDeBandeja;
    }

    /** Cola de revisión del backoffice, paginada por cursor. Solo backoffice. */
    @Transactional(readOnly = true)
    public List<ExpedienteDeHabilitacion> bandeja(
            List<String> estados, OffsetDateTime despuesDeFecha, UUID despuesDeId, int limite, ContextoSesion ctx) {
        Backoffice.exigir(ctx);
        if (estados == null
                || estados.isEmpty()
                || !List.of("PENDIENTE", "EN_REVISION", "APROBADA", "RECHAZADA").containsAll(estados)
                || (despuesDeFecha == null) != (despuesDeId == null)
                || limite < 1) {
            throw error(7, "Filtro de bandeja invalido.");
        }
        return datos.conContexto(
                ctx,
                dsl -> habilitaciones.bandeja(
                        dsl, estados, despuesDeFecha, despuesDeId, Math.min(limite, topeDeBandeja)));
    }

    /** El expediente de una solicitud con su historial: backoffice. */
    @Transactional(readOnly = true)
    public Expediente expediente(UUID solicitudId, ContextoSesion ctx) {
        Backoffice.exigir(ctx);
        return datos.conContexto(ctx, dsl -> {
            var solicitud =
                    habilitaciones.ver(dsl, solicitudId).orElseThrow(() -> error(3, "Esa solicitud no existe."));
            return new Expediente(solicitud, habilitaciones.historial(dsl, solicitudId));
        });
    }

    /** El expediente propio de quien postuló: estado y resoluciones (con su motivo, que le corresponde conocer). */
    @Transactional(readOnly = true)
    public Expediente expedienteDelUsuario(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var solicitud = habilitaciones
                    .ultimaDelUsuario(dsl, ctx.usuarioId())
                    .filter(s -> s.usuarioId().equals(ctx.usuarioId()))
                    .orElseThrow(() -> error(3, "No tenes una postulacion."));
            return new Expediente(solicitud, habilitaciones.historial(dsl, solicitud.solicitudId()));
        });
    }

    private static ErrorDeNegocio error(int numero, String mensaje) {
        return new ErrorDeNegocio(CodigoError.de(90, numero), mensaje);
    }

    public record Expediente(ExpedienteDeHabilitacion solicitud, List<DecisionDeHabilitacion> decisiones) {}
}

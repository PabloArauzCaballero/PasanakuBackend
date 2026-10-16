package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.BandejaDeRetirosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-11 · la bandeja de la segunda aprobacion (ADR-049, backoffice carril A · H2).
 *
 * <p>El permiso ({@code RETIRO_APROBAR}) lo exige la pagina; aca se valida el cursor y se marca la
 * fila propia, que el aprobador no puede decidir (R-SEG-04). {@code aprobar} lo vuelve a exigir:
 * la marca solo evita ofrecer un boton muerto.
 */
@Service
public class BandejaDeRetiros {

    private final Datos datos;
    private final BandejaDeRetirosRepositorio bandeja;

    public BandejaDeRetiros(Datos datos, BandejaDeRetirosRepositorio bandeja) {
        this.datos = datos;
        this.bandeja = bandeja;
    }

    @Transactional(readOnly = true)
    public Pagina enRevision(
            Optional<OffsetDateTime> despuesDeFecha, Optional<UUID> despuesDeId, int limite, ContextoSesion ctx) {
        if (despuesDeFecha.isPresent() != despuesDeId.isPresent()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(11, 14),
                    "El cursor de la bandeja va completo: despuesDeFecha y despuesDeId juntos.");
        }
        var filas = datos.conContexto(ctx, dsl -> bandeja.enRevision(dsl, despuesDeFecha, despuesDeId, limite));
        var items = filas.stream()
                .map(f -> new Fila(
                        f.id(),
                        f.cuentaId(),
                        f.solicitadaPor(),
                        f.solicitado(),
                        f.costo(),
                        f.neto(),
                        f.mfaVerificado(),
                        f.solicitadaEn(),
                        f.enfriamientoHasta(),
                        !f.solicitadaPor().equals(ctx.usuarioId())))
                .toList();
        // Pagina llena => puede haber mas: el cursor es la ultima fila. Pagina corta => se termino.
        Optional<Fila> ultima = items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new Pagina(items, ultima.map(Fila::solicitadaEn), ultima.map(Fila::ordenRetiroId));
    }

    /** Una fila de la bandeja, en terminos del caso de uso: la web no ve el registro del repositorio. */
    public record Fila(
            UUID ordenRetiroId,
            UUID cuentaId,
            UUID solicitadaPor,
            Dinero solicitado,
            Dinero costo,
            Dinero neto,
            boolean mfaVerificado,
            OffsetDateTime solicitadaEn,
            Optional<OffsetDateTime> enfriamientoHasta,
            boolean puedeDecidir) {}

    public record Pagina(List<Fila> items, Optional<OffsetDateTime> proximoFecha, Optional<UUID> proximoId) {}
}

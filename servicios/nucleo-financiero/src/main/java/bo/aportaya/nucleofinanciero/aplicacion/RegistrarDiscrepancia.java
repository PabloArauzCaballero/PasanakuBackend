package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor.Tipo;
import bo.aportaya.nucleofinanciero.infraestructura.DiscrepanciaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la discrepancia y avisa, en una transaccion propia y <b>sin tocar saldos</b>.
 *
 * <p>Vive aparte de quien rechaza la confirmacion a proposito: la peticion que detecta la
 * discrepancia termina en error, y si el registro compartiera su transaccion se
 * revertiria junto con ella. La evidencia de lo que el proveedor dijo tiene que
 * sobrevivir al rechazo de lo que pidio.
 */
@Service
public class RegistrarDiscrepancia {

    private final Datos datos;
    private final DiscrepanciaRepositorio discrepancias;
    private final Outbox outbox;
    private final Reloj reloj;

    public RegistrarDiscrepancia(Datos datos, DiscrepanciaRepositorio discrepancias, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.discrepancias = discrepancias;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    /** @return {@code true} si abrio una discrepancia nueva (y emitio la alerta). */
    @Transactional
    public boolean registrar(EntradaDiscrepancia entrada, ContextoSesion ctx) {
        var ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            UUID correlacion = UUID.fromString(ctx.traza().id());
            boolean nueva = discrepancias.registrar(
                    dsl,
                    entrada.referenciaTipo(),
                    entrada.referenciaId(),
                    entrada.tipo(),
                    entrada.esperado(),
                    entrada.informado(),
                    entrada.detalle(),
                    entrada.huella(),
                    correlacion,
                    ahora);
            if (nueva) {
                // Identificadores y tipo, no importes: el consumidor pregunta lo que necesita.
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "nucleo_financiero.discrepancia_detectada",
                                entrada.referenciaTipo().toLowerCase(Locale.ROOT),
                                entrada.referenciaId(),
                                Map.of("tipo", entrada.tipo().name()),
                                correlacion));
            }
            return nueva;
        });
    }

    public record EntradaDiscrepancia(
            String referenciaTipo,
            UUID referenciaId,
            Tipo tipo,
            Optional<Dinero> esperado,
            Optional<Dinero> informado,
            String detalle,
            String huella) {}
}

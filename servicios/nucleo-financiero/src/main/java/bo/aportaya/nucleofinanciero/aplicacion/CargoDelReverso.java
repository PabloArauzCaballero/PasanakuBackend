package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.ReversoRepositorio;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Si lo que se reversa era una recarga o un retiro con costo, ese costo tambien se devuelve, y no por una
 * edicion: el contra-asiento de CU-14 es nuevo y el original queda intacto. La devolucion fiscal (nota de
 * credito) es de {@code tarifas}, que la ejecuta al recibir este evento; aca solo se avisa, una vez, y con
 * identificadores. El reverso ocurre una sola vez por transaccion (R-BIL-15), asi que el aviso tambien.
 */
@Component
public class CargoDelReverso {

    private final ReversoRepositorio reversos;
    private final Outbox outbox;

    public CargoDelReverso(ReversoRepositorio reversos, Outbox outbox) {
        this.reversos = reversos;
        this.outbox = outbox;
    }

    void revertir(DSLContext dsl, ReversoRepositorio.Original original, UUID reverso, ContextoSesion ctx) {
        reversos.cargoDe(dsl, original).ifPresent(cargo -> {
            if ("ORDEN_RECARGA".equals(cargo.ordenTipo())) {
                reversos.marcarRecargaReversada(dsl, cargo.ordenId());
            }
            if (cargo.costo().esMayorQue(Dinero.cero(cargo.costo().moneda()))) {
                var carga = new HashMap<String, Object>();
                carga.put("ordenTipo", cargo.ordenTipo());
                carga.put("transaccionReversoId", reverso.toString());
                cargo.cotizacionId().ifPresent(id -> carga.put("cotizacionId", id.toString()));
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "nucleo_financiero.cargo_a_revertir",
                                cargo.ordenTipo().toLowerCase(Locale.ROOT),
                                cargo.ordenId(),
                                carga,
                                UUID.fromString(ctx.traza().id())));
            }
        });
    }
}

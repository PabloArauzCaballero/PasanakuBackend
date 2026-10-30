package bo.aportaya.entregas.aplicacion;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;

/** El evento de una subasta, por el outbox: solo identificadores e importes del cupo, nunca datos de personas (90.2.1). */
final class EventosDeSubasta {

    private EventosDeSubasta() {}

    static void emitir(
            Outbox outbox, DSLContext dsl, String tipo, UUID subastaId, Map<String, String> datos, ContextoSesion ctx) {
        Map<String, String> carga = new HashMap<>(datos);
        carga.put("subastaId", subastaId.toString());
        outbox.emitir(
                dsl,
                new EventoDominio(
                        tipo,
                        "subasta_cupo",
                        subastaId,
                        Map.copyOf(carga),
                        UUID.fromString(ctx.traza().id())));
    }
}

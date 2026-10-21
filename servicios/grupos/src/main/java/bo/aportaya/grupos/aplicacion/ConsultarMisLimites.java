package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.OptionalInt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuantos grupos organizo y en cuantos participo, contra su tope (2026-10-20). Para que la app los muestre ANTES de
 * que la persona intente algo que el servidor le va a rechazar. Los topes los aplican CU-20 (AP-CU20-06) y las
 * entradas al grupo (AP-CU69-15); esto solo los informa.
 */
@Service
public class ConsultarMisLimites {

    private final Datos datos;
    private final EmparejamientoRepositorio emparejamientos;
    private final HechosDeOtrosServicios afuera;

    public ConsultarMisLimites(Datos datos, EmparejamientoRepositorio emparejamientos, HechosDeOtrosServicios afuera) {
        this.datos = datos;
        this.emparejamientos = emparejamientos;
        this.afuera = afuera;
    }

    @Transactional(readOnly = true)
    public Limites consultar(ContextoSesion ctx) {
        // El limite de organizar lo dice `organizador`; se pregunta afuera de la transaccion de lectura propia.
        OptionalInt limiteDeOrganizar = afuera.organizadorHabilitadoDelUsuario(ctx.usuarioId())
                .map(afuera::limiteDeGruposDelOrganizador)
                .orElse(OptionalInt.empty());
        return datos.conContexto(ctx, dsl -> {
            int organizados = dsl.fetchOne(
                            """
                            SELECT count(DISTINCT p.grupo_id)::int
                              FROM grupos.participante p
                              JOIN grupos.grupo g ON g.id = p.grupo_id
                             WHERE p.usuario_id = ? AND p.es_organizador
                               AND p.estado NOT IN ('RETIRADO', 'EXPULSADO', 'REEMPLAZADO')
                               AND g.estado NOT IN ('FINALIZADO', 'CANCELADO', 'DISUELTO_ANTICIPADAMENTE')
                            """,
                            ctx.usuarioId())
                    .get(0, Integer.class);
            return new Limites(
                    organizados,
                    limiteDeOrganizar.isPresent() ? limiteDeOrganizar.getAsInt() : null,
                    emparejamientos.participacionesVivas(dsl, ctx.usuarioId()),
                    emparejamientos.maximoDeParticipaciones());
        });
    }

    /** {@code limiteDeOrganizar} es nulo si la persona no esta habilitada como organizadora. */
    public record Limites(int organizados, Integer limiteDeOrganizar, int participaciones, int limiteDeParticipar) {}
}

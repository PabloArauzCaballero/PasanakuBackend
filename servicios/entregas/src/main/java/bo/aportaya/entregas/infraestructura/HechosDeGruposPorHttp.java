package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.HechosDeGrupos;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Lo que {@code grupos} sabe de la membresia de quien compra, por su contrato publicado
 * ({@code GET /grupos/participaciones}: «solo lo propio, nunca el de otros»).
 *
 * <p>Carril G: reemplaza al stub que contestaba siempre «no se pudo saber» y por eso dejaba el mercado entero
 * detenido. Responde SOLO sobre la persona de la sesion en curso: si piden la membresia de otra, o no hay token (un
 * trabajo programado), la respuesta es «no se pudo saber» y quien pregunta deniega. Con {@code grupos} caido,
 * con el circuito abierto o con un cuerpo ilegible ocurre lo mismo: nunca se supone que alguien es miembro.
 *
 * <p><b>Lo que NO se resuelve aca, declarado:</b> {@link #turno(UUID)}. {@code grupos} no publica una consulta de
 * turno (titular, estado, monto del derecho, cierre), asi que publicar una oferta por HTTP sigue denegado hasta
 * que {@code grupos} la exponga (contrato propuesto en {@link HechosDeGrupos}). Inventar una ruta que no existe
 * seria presentar como hecho lo que es una pregunta abierta.
 */
@Component
public class HechosDeGruposPorHttp implements HechosDeGrupos {

    private static final String ACTIVO = "ACTIVO";

    private final ClienteConCircuito grupos;

    @Autowired
    public HechosDeGruposPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.grupos}") String urlDeGrupos,
            ConfiguracionDeCircuito circuito) {
        this.grupos = new ClienteConCircuito(constructor, urlDeGrupos, "grupos", circuito);
    }

    @Override
    public Optional<Turno> turno(UUID turnoId) {
        return Optional.empty();
    }

    @Override
    public Optional<Miembro> miembro(UUID grupoId, UUID usuarioId) {
        return participaciones(usuarioId).flatMap(lista -> Arrays.stream(lista)
                .filter(p -> grupoId.equals(p.grupoId()))
                .findFirst()
                .map(p -> new Miembro(p.participanteId(), ACTIVO.equals(p.estado()))));
    }

    @Override
    public Optional<Set<UUID>> gruposActivosDe(UUID usuarioId) {
        return participaciones(usuarioId).map(lista -> Arrays.stream(lista)
                .filter(p -> ACTIVO.equals(p.estado()))
                .map(Participacion::grupoId)
                .collect(Collectors.toSet()));
    }

    /** Las participaciones de la persona de la sesion; vacio si no se pudo saber o si piden las de otra persona. */
    private Optional<Participacion[]> participaciones(UUID usuarioId) {
        if (ClienteDeServicio.usuarioDeLaSesion()
                .filter(usuarioId.toString()::equals)
                .isEmpty()) {
            return Optional.empty();
        }
        try {
            return grupos.consultar("/grupos/participaciones", Participacion[].class);
        } catch (RuntimeException noSePudo) {
            return Optional.empty();
        }
    }

    private record Participacion(UUID grupoId, UUID participanteId, String estado) {}
}

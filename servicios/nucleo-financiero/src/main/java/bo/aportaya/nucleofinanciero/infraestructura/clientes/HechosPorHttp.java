package bo.aportaya.nucleofinanciero.infraestructura.clientes;

import bo.aportaya.nucleofinanciero.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a {@code grupos} y a {@code aportes}, por sus contratos.
 *
 * <p>Las dos preguntas que frenan un cierre —¿debe algo?, ¿sigue en un pasanaku?— se
 * responden **que si** cuando el servicio no contesta. Cerrar una billetera porque
 * {@code aportes} estaba caido dejaria el aporte impago del lado de los otros del
 * grupo, y esa perdida no es de quien se va: es de los que se quedan (invariante 9).
 *
 * <p>El alias, en cambio, se responde vacio: un destino que no se pudo resolver no es
 * un destino, y mandar la plata «por las dudas» seria peor que rechazar.
 *
 * <p><b>Carril Q · el estado de pagos NO se indexa por usuario.</b> {@code aportes} publica
 * {@code /aportes/participantes/{participanteId}/estado} y su {@code participanteId} es el de {@code grupos}
 * (una persona tiene uno POR GRUPO); preguntarle con el {@code usuarioId} daba siempre «sin obligaciones» y dejaba cerrar la
 * billetera de quien debia. Ahora se resuelve en dos pasos, ambos con el token de la persona: (1) a {@code grupos} sus
 * participaciones ({@code GET /grupos/participaciones}, solo las propias), (2) a {@code aportes} el estado de CADA
 * participacion. Como el paso 1 devuelve las participaciones de QUIEN HACE LA PETICION, si la sesion no es la del titular
 * por el que se pregunta (un operador, un trabajo programado) no se sabe y se contesta lo cerrado: «tiene obligaciones».
 */
@Component
public class HechosPorHttp implements HechosDeOtrosServicios {

    private final ClienteDeServicio grupos;
    private final ClienteDeServicio aportes;

    public HechosPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.grupos}") String urlGrupos,
            @Value("${aportaya.servicios.aportes}") String urlAportes) {
        this.grupos = new ClienteDeServicio(constructor, urlGrupos, "grupos");
        this.aportes = new ClienteDeServicio(constructor, urlAportes, "aportes");
    }

    @Override
    public Optional<UUID> usuarioDelAlias(String alias) {
        return grupos.consultar("/grupos/participantes/alias/" + alias, Alias.class)
                .filter(Alias::existe)
                .map(Alias::usuarioId);
    }

    @Override
    public boolean tieneObligacionesAbiertas(UUID usuarioId) {
        try {
            var estados = estadosDe(usuarioId);
            return estados.isEmpty() || estados.get().stream().anyMatch(e -> e.obligacionesAbiertas() > 0);
        } catch (RuntimeException ilegible) {
            // 5xx de aportes o cuerpo ilegible: no se sabe, y no saber es «si debe» (invariante 9).
            return true;
        }
    }

    @Override
    public Optional<Dinero> comprometidoEnPozos(UUID usuarioId, Moneda moneda) {
        try {
            var estados = estadosDe(usuarioId);
            if (estados.isEmpty()) {
                return Optional.empty();
            }
            Dinero total = Dinero.cero(moneda);
            for (Estado e : estados.get()) {
                if (e.deudaVigente() == null || e.porAportar() == null || e.moneda() == null) {
                    return Optional.empty();
                }
                Moneda deEste = Moneda.valueOf(e.moneda());
                Dinero comprometido = Dinero.de(e.deudaVigente(), deEste).mas(Dinero.de(e.porAportar(), deEste));
                if (deEste == moneda) {
                    total = total.mas(comprometido);
                } else if (!comprometido.esCero()) {
                    // Hay plata comprometida en OTRA moneda: este numero no la puede contar ni inventar su cambio.
                    return Optional.empty();
                }
            }
            return Optional.of(total);
        } catch (RuntimeException ilegible) {
            // 5xx, cuerpo ilegible, importe o moneda mal formados: no se sabe. Omitir, nunca inventar un cero.
            return Optional.empty();
        }
    }

    /**
     * El estado de pagos de CADA participacion de la persona, o vacio si no se pudo saber: la sesion no es la del titular,
     * {@code grupos} no contesto, o {@code aportes} no contesto por alguna participacion. Una persona sin participaciones
     * devuelve la lista vacia (no debe nada porque no se le pidio nada).
     */
    private Optional<List<Estado>> estadosDe(UUID usuarioId) {
        boolean esLaSesionDelTitular = ClienteDeServicio.usuarioDeLaSesion()
                .map(usuarioId.toString()::equals)
                .orElse(false);
        if (!esLaSesionDelTitular) {
            return Optional.empty();
        }
        var participaciones = grupos.consultar("/grupos/participaciones", Participacion[].class);
        if (participaciones.isEmpty()) {
            return Optional.empty();
        }
        List<Estado> estados = new ArrayList<>();
        for (Participacion p : participaciones.get()) {
            var estado = aportes.consultar("/aportes/participantes/" + p.participanteId() + "/estado", Estado.class);
            // Un 200 sin el conteo es un cuerpo ilegible, no «cero obligaciones».
            if (estado.isEmpty() || estado.get().obligacionesAbiertas() == null) {
                return Optional.empty();
            }
            estados.add(estado.get());
        }
        return Optional.of(estados);
    }

    @Override
    public boolean participaEnGrupoActivo(UUID usuarioId) {
        return grupos.consultar("/grupos/participantes/" + usuarioId + "/actividad", Actividad.class)
                .map(a -> a.gruposActivos() > 0)
                .orElse(true);
    }

    private record Alias(boolean existe, UUID usuarioId) {}

    /** Lo unico que se lee de una participacion de {@code grupos}: el identificador con el que {@code aportes} la conoce. */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record Participacion(UUID participanteId) {}

    /**
     * Lo que este servicio lee del estado de pagos. {@code aportes} publica mas campos: se ignoran a
     * proposito, porque con {@code fail-on-unknown-properties} un campo nuevo del otro lado romperia la lectura.
     */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record Estado(Integer obligacionesAbiertas, String deudaVigente, String porAportar, String moneda) {}

    private record Actividad(int gruposActivos) {}
}

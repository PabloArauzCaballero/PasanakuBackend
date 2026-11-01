package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.grupos.dominio.Etapas;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * La etapa de una o varias personas segun {@code transparencia} (2026-10-30), por su contrato y nunca por su base
 * (regla 98.1.1): el catalogo con {@code GET /reputacion/etapas} y la etapa de cada persona con
 * {@code GET /reputacion/etapas/de-personas}, la ruta [CONTRATO · SOLO SERVICIO] que exige la identidad de servicio
 * {@code SERVICIO_GRUPOS}. Grupos ya no lee {@code GET /reputacion/{usuarioId}/puntaje} de otra persona: el puntaje
 * exacto solo lo ve su dueño (decision del dueño 2026-10-30, regla 90.2.8).
 *
 * <p>Quien habla con la ruta de personas es el SERVICIO, no la sesion: la llamada lleva el token de la cuenta de
 * servicio de grupos ({@link CredencialDeServicio}, emitido por identidad, ADR-024; regla 98.6.3), nunca el de quien
 * pregunta. El catalogo, que no dice nada de nadie, sigue viajando con el token de la sesion.
 *
 * <p>Va por el mismo {@link ClienteConCircuito} hacia transparencia: tiempo maximo de salida (1 s de conexion, 3 s de
 * lectura, {@code ConfiguracionDeClientes}) y circuit breaker propio. Degradacion declarada (regla 98.2.4): sin
 * credencial de servicio no sale ninguna peticion; un 403, un 5xx, una caida, un cuerpo ilegible o una etapa que no
 * esta en el catalogo son VACIO. Para decidir un ingreso eso es «no entra»; en el detalle, «sin etapa».
 */
final class EtapasPorHttp {

    private static final Logger BITACORA = LoggerFactory.getLogger(EtapasPorHttp.class);

    private static final String DE_PERSONAS = "/reputacion/etapas/de-personas";

    private final ClienteConCircuito transparencia;
    private final CredencialDeServicio servicio;

    /**
     * Lo que acepta el contrato por peticion ({@code maxItems: 60}). De configuracion
     * ({@code aportaya.grupos.transparencia.maximo-por-consulta}), no una constante (invariante 10).
     */
    private final int maximoPorConsulta;

    EtapasPorHttp(ClienteConCircuito transparencia, CredencialDeServicio servicio, int maximoPorConsulta) {
        this.transparencia = transparencia;
        this.servicio = servicio;
        this.maximoPorConsulta = maximoPorConsulta;
    }

    /** La etapa de una persona, con lo que desbloquea y el catalogo, para decidir un ingreso o un alta. */
    Optional<Etapas.DeLaPersona> de(UUID usuarioId) {
        Optional<List<Etapas.Etapa>> catalogo = catalogo();
        if (catalogo.isEmpty() || catalogo.get().isEmpty()) return Optional.empty();
        return deVarias(List.of(usuarioId))
                .map(etapas -> etapas.get(usuarioId))
                .flatMap(e -> Etapas.porCodigo(catalogo.get(), e.codigo()))
                // El piso de su etapa: transparencia no le cuenta a grupos el puntaje exacto, y no hace falta.
                .map(etapa -> new Etapas.DeLaPersona(etapa.desde(), etapa, catalogo.get()));
    }

    /** Codigo y nombre de la etapa de cada persona; vacio si no hay respuesta completa (nunca un mapa a medias). */
    Optional<Map<UUID, Etapas.Visible>> visibles(Collection<UUID> usuarios) {
        return deVarias(usuarios).map(etapas -> {
            Map<UUID, Etapas.Visible> salida = new LinkedHashMap<>();
            etapas.forEach((id, e) -> salida.put(id, new Etapas.Visible(e.codigo(), e.nombre())));
            return salida;
        });
    }

    Optional<List<Etapas.Etapa>> catalogo() {
        try {
            return transparencia
                    .consultar("/reputacion/etapas", EtapaDelCatalogo[].class)
                    .map(filas -> Arrays.stream(filas)
                            .map(EtapasPorHttp::etapa)
                            .sorted(Comparator.comparingInt(Etapas.Etapa::orden))
                            .toList());
        } catch (RuntimeException ilegible) {
            // Un importe que no es decimal, un campo obligatorio ausente, un 5xx: no es un catalogo, es «sin dato».
            return Optional.empty();
        }
    }

    /**
     * La ruta de servicio, de a {@code maximoPorConsulta} personas: un detalle de 40 cupos es UNA llamada, no
     * cuarenta. Cada persona pedida sin etapa en la respuesta queda afuera del mapa; una respuesta que trae a alguien
     * que no se pidio no se usa (no es la respuesta a esta pregunta).
     */
    private Optional<Map<UUID, EtapaParaServicio>> deVarias(Collection<UUID> usuarios) {
        List<UUID> pedidos = new ArrayList<>(new LinkedHashSet<>(usuarios));
        if (pedidos.isEmpty()) return Optional.of(Map.of());
        Optional<String> token = servicio.vigente();
        if (token.isEmpty()) {
            // Sin credencial de servicio no sale ninguna peticion (falla cerrado). Solo el hecho, sin ids (90.2.1).
            BITACORA.warn("etapas de personas sin credencial de servicio: sin dato");
            return Optional.empty();
        }
        Map<UUID, EtapaParaServicio> salida = new LinkedHashMap<>();
        for (int desde = 0; desde < pedidos.size(); desde += maximoPorConsulta) {
            List<UUID> tanda = pedidos.subList(desde, Math.min(pedidos.size(), desde + maximoPorConsulta));
            Optional<EtapaDePersona[]> respuesta = consultar(tanda, token.get());
            if (respuesta.isEmpty()) return Optional.empty();
            for (EtapaDePersona p : respuesta.get()) {
                if (p == null || p.usuarioId() == null || !tanda.contains(p.usuarioId())) return Optional.empty();
                if (p.etapa() != null && p.etapa().codigo() != null) salida.put(p.usuarioId(), p.etapa());
            }
        }
        return Optional.of(salida);
    }

    private Optional<EtapaDePersona[]> consultar(List<UUID> tanda, String token) {
        try {
            return Optional.ofNullable(transparencia.ejecutar(rest -> rest.get()
                    .uri(b -> b.path(DE_PERSONAS)
                            .queryParam("usuarioId", tanda.toArray())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(EtapaDePersona[].class)));
        } catch (RuntimeException noContesto) {
            // 403 (no somos SERVICIO_GRUPOS para transparencia), 5xx, red caida, circuito abierto o cuerpo ilegible.
            return Optional.empty();
        }
    }

    private static Etapas.Etapa etapa(EtapaDelCatalogo f) {
        Etapas.Desbloqueo desbloqueo = f.desbloqueo() == null
                ? null
                : new Etapas.Desbloqueo(
                        f.desbloqueo().cuposMax(),
                        new BigDecimal(f.desbloqueo().aporteMax()),
                        f.desbloqueo().moneda(),
                        f.desbloqueo().estado());
        return new Etapas.Etapa(
                f.codigo(), f.orden(), new BigDecimal(f.desde()), new BigDecimal(f.hasta()), desbloqueo);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EtapaDePersona(UUID usuarioId, boolean tieneHistorial, EtapaParaServicio etapa) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EtapaParaServicio(String codigo, String nombre, int orden, String desde) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EtapaDelCatalogo(
            String codigo, String nombre, int orden, String desde, String hasta, DesbloqueoDeEtapa desbloqueo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DesbloqueoDeEtapa(int cuposMax, String aporteMax, String moneda, String estado) {}
}

package bo.aportaya.cumplimiento.aplicacion;

import bo.aportaya.cumplimiento.infraestructura.BandejasLftRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-44 · las bandejas del analista y del oficial (backoffice carril A · H5).
 *
 * <p>Los permisos ({@code CUMPLIMIENTO_ALERTAS}, {@code CUMPLIMIENTO_CASOS}) los exige la pagina.
 * Aca se valida el filtro y se marca {@code vencido} comparando el plazo GUARDADO al abrir el caso
 * con la hora (R-UIF-08: no se recalcula ninguno).
 */
@Service
public class ConsultarBandejasLft {

    private static final Set<String> ESTADOS_ALERTA = Set.of("ABIERTA", "EN_ANALISIS", "ESCALADA", "DESCARTADA");
    private static final List<String> ALERTAS_ABIERTAS = List.of("ABIERTA", "EN_ANALISIS");
    private static final List<String> SEVERIDADES = List.of("CRITICA", "ALTA", "MEDIA", "BAJA");
    private static final Set<String> ESTADOS_CASO = Set.of("ABIERTO", "EN_ANALISIS", "EN_REVISION", "CERRADO");
    private static final List<String> CASOS_ABIERTOS = List.of("ABIERTO", "EN_ANALISIS", "EN_REVISION");

    private final Datos datos;
    private final BandejasLftRepositorio bandejas;
    private final Reloj reloj;

    public ConsultarBandejasLft(Datos datos, BandejasLftRepositorio bandejas, Reloj reloj) {
        this.datos = datos;
        this.bandejas = bandejas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PaginaDeAlertas alertas(
            List<String> estados,
            List<String> severidades,
            Optional<OffsetDateTime> despuesDeFecha,
            Optional<UUID> despuesDeId,
            int limite,
            ContextoSesion ctx) {
        List<String> filtroEstados = vacio(estados) ? ALERTAS_ABIERTAS : estados;
        List<String> filtroSeveridades = vacio(severidades) ? SEVERIDADES : severidades;
        if (!ESTADOS_ALERTA.containsAll(filtroEstados)
                || !SEVERIDADES.containsAll(filtroSeveridades)
                || despuesDeFecha.isPresent() != despuesDeId.isPresent()) {
            throw filtroInvalido();
        }
        var items = datos.conContexto(
                ctx,
                dsl -> bandejas.alertas(dsl, filtroEstados, filtroSeveridades, despuesDeFecha, despuesDeId, limite));
        Optional<BandejasLftRepositorio.AlertaEnBandeja> ultima =
                items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new PaginaDeAlertas(
                items.stream().map(ConsultarBandejasLft::alerta).toList(),
                ultima.map(BandejasLftRepositorio.AlertaEnBandeja::detectadaEn),
                ultima.map(BandejasLftRepositorio.AlertaEnBandeja::id));
    }

    @Transactional(readOnly = true)
    public PaginaDeCasos casos(
            List<String> estados,
            Optional<OffsetDateTime> despuesDePlazo,
            Optional<UUID> despuesDeId,
            int limite,
            ContextoSesion ctx) {
        List<String> filtro = vacio(estados) ? CASOS_ABIERTOS : estados;
        if (!ESTADOS_CASO.containsAll(filtro) || despuesDePlazo.isPresent() != despuesDeId.isPresent()) {
            throw filtroInvalido();
        }
        OffsetDateTime ahora = OffsetDateTime.ofInstant(reloj.ahora(), Reloj.ZONA);
        var items = datos.conContexto(ctx, dsl -> bandejas.casos(dsl, filtro, despuesDePlazo, despuesDeId, limite));
        Optional<BandejasLftRepositorio.CasoEnBandeja> ultima =
                items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new PaginaDeCasos(
                items.stream().map(c -> caso(c, ahora)).toList(),
                ultima.map(BandejasLftRepositorio.CasoEnBandeja::plazoLimite),
                ultima.map(BandejasLftRepositorio.CasoEnBandeja::id));
    }

    private static boolean vacio(List<String> lista) {
        return lista == null || lista.isEmpty();
    }

    private static ErrorDeNegocio filtroInvalido() {
        return new ErrorDeNegocio(
                CodigoError.de(44, 5),
                "Filtro de bandeja invalido: estado o severidad desconocidos, o cursor a medias.");
    }

    private static Alerta alerta(BandejasLftRepositorio.AlertaEnBandeja a) {
        return new Alerta(
                a.id(),
                a.reglaCodigo(),
                a.tipologia(),
                a.severidad(),
                a.estado(),
                a.detectadaEn(),
                Optional.ofNullable(a.asignadaA()),
                Optional.ofNullable(a.casoId()),
                Optional.ofNullable(a.cerradaEn()));
    }

    private static Caso caso(BandejasLftRepositorio.CasoEnBandeja c, OffsetDateTime ahora) {
        boolean vencido = !"CERRADO".equals(c.estado()) && c.plazoLimite().isBefore(ahora);
        return new Caso(
                c.id(),
                c.codigo(),
                c.origen(),
                c.estado(),
                c.prioridad(),
                c.analistaId(),
                Optional.ofNullable(c.revisadoPor()),
                c.abiertoEn(),
                c.plazoLimite(),
                vencido,
                Optional.ofNullable(c.decision()),
                Optional.ofNullable(c.cerradoEn()));
    }

    public record Alerta(
            UUID alertaId,
            String reglaCodigo,
            String tipologia,
            String severidad,
            String estado,
            OffsetDateTime detectadaEn,
            Optional<UUID> asignadaA,
            Optional<UUID> casoId,
            Optional<OffsetDateTime> cerradaEn) {}

    public record Caso(
            UUID casoId,
            String codigo,
            String origen,
            String estado,
            String prioridad,
            UUID analistaId,
            Optional<UUID> revisadoPor,
            OffsetDateTime abiertoEn,
            OffsetDateTime plazoLimite,
            boolean vencido,
            Optional<String> decision,
            Optional<OffsetDateTime> cerradoEn) {}

    public record PaginaDeAlertas(
            List<Alerta> items, Optional<OffsetDateTime> proximoFecha, Optional<UUID> proximoId) {}

    public record PaginaDeCasos(List<Caso> items, Optional<OffsetDateTime> proximoPlazo, Optional<UUID> proximoId) {}
}

package bo.aportaya.plataforma.mensajeria;

import bo.aportaya.plataforma.dominio.Indicador;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jooq.DSLContext;

/**
 * Tableros (CU-98) · el lado que CALCULA: un servicio dueño publica sus indicadores para que {@code auditoria} los
 * guarde en {@code indicador_kpi} y los publique. Ningun otro servicio lee la base de este (invariante 11): el numero
 * viaja como hecho.
 *
 * <p>Es la excepcion declarada a «la carga lleva identificadores» de {@link EventoDominio}: aca el valor ES el hecho
 * (lo que medio el dueño en ese momento), y {@code auditoria} no tiene a quien preguntarlo despues.
 *
 * <p>Un indicador sin valor ({@code SIN_DATOS}, {@code MUESTRA_INSUFICIENTE}...) NO se emite: {@code indicador_kpi}
 * no admite valor nulo y CU-98 muestra el indicador ausente, que es lo honesto. Emitir un cero seria mentir.
 */
public final class PublicadorDeIndicadores {

    /** AAAA, AAAA-MM o AAAA-MM-DD: la granularidad de CU-98. */
    private static final Pattern PERIODO = Pattern.compile("^\\d{4}(-\\d{2}(-\\d{2})?)?$");

    private final Outbox outbox;
    private final String modulo;

    public PublicadorDeIndicadores(Outbox outbox, String modulo) {
        this.outbox = outbox;
        this.modulo = modulo;
    }

    /** Emite por outbox, en la transaccion del que llama. Devuelve si emitio. */
    public boolean publicar(
            DSLContext dsl,
            Indicador indicador,
            String nombre,
            String periodo,
            boolean provisorio,
            UUID correlationId) {
        var evento = evento(modulo, indicador, nombre, periodo, provisorio, correlationId);
        evento.ifPresent(e -> outbox.emitir(dsl, e));
        return evento.isPresent();
    }

    /** Puro: el evento que corresponde, o nada si el indicador no tiene valor. */
    public static Optional<EventoDominio> evento(
            String modulo, Indicador indicador, String nombre, String periodo, boolean provisorio, UUID correlationId) {
        if (nombre == null || nombre.isBlank() || nombre.length() > 80) {
            throw new IllegalArgumentException(
                    "El nombre del indicador " + indicador.codigo() + " es obligatorio y" + " de hasta 80 caracteres.");
        }
        if (periodo == null || !PERIODO.matcher(periodo).matches()) {
            throw new IllegalArgumentException("El periodo es AAAA, AAAA-MM o AAAA-MM-DD; llego «" + periodo + "».");
        }
        if (indicador.valor() == null) {
            return Optional.empty();
        }
        var carga = new HashMap<String, Object>();
        carga.put("codigo", indicador.codigo());
        carga.put("nombre", nombre);
        carga.put("valor", indicador.valor().toPlainString());
        carga.put("unidad", unidad(indicador));
        carga.put("periodo", periodo);
        carga.put("casos", indicador.muestra());
        carga.put("provisorio", provisorio);
        carga.put("dimension", "GLOBAL");
        // Determinista por (modulo, codigo, periodo): un recalculo del mismo periodo es el MISMO agregado.
        UUID agregado = UUID.nameUUIDFromBytes(
                (modulo + "|" + indicador.codigo() + "|" + periodo).getBytes(StandardCharsets.UTF_8));
        return Optional.of(
                new EventoDominio(modulo + ".indicador_calculado", "indicador", agregado, carga, correlationId));
    }

    /** La unidad tal como la guarda {@code indicador_kpi} (15 caracteres): el dinero, por su moneda. */
    static String unidad(Indicador i) {
        return switch (i.unidad()) {
            case DINERO -> i.moneda().name();
            case CONTEO -> "CONTEO";
            case PORCENTAJE -> "PORCENTAJE";
            case DIAS -> "DIAS";
            case RAZON -> "VECES";
            case MILISEGUNDOS -> "MILISEGUNDOS";
        };
    }
}

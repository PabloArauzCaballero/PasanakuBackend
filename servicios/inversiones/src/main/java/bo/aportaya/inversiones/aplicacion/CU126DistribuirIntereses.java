package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.Descomposicion;
import bo.aportaya.inversiones.infraestructura.DistribucionRepositorio;
import bo.aportaya.inversiones.infraestructura.DistribucionRepositorio.Distribucion;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio.Posicion;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio.Rescate;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

/**
 * CU-126 · Distribuir el interes que pago el emisor al titular de la posicion.
 *
 * <p>Cuando un DPF se liquida, el interes que paga el emisor se reparte a UN titular (el de
 * la posicion), por UN periodo (de la constitucion al corte) y con SU retencion (la del
 * comprobante). La base lo exige ({@code R-INV-09}). Ademas se concilia contra el devengo
 * propio: el interes que corresponde segun lo que devengamos dia a dia, menos la
 * penalizacion del contrato si se cancelo antes, tiene que ser lo que pago el emisor. Si no
 * coincide NO se corrige en silencio: queda registrada una discrepancia, con su motivo, a la
 * vista del titular y de operaciones.
 *
 * <p>La discrepancia contra el devengo no frena el credito: el pago ya paso el control mas
 * fuerte (el interes y la retencion del emisor coinciden con el calculo propio, CU-125). Lo
 * que dice es que nuestro devengo diario esta atrasado o que algo cambio, y eso se corrige
 * corriendo el devengo, no negandole al titular lo que el emisor ya pago.
 */
@Service
public class CU126DistribuirIntereses {

    public static final String SIN_DEVENGO_AL_CORTE = "SIN_DEVENGO_AL_CORTE";
    public static final String INTERES_DISTINTO_DEL_DEVENGO = "INTERES_DISTINTO_DEL_DEVENGO";

    private final Datos datos;
    private final Transaccionar tx;
    private final DistribucionRepositorio distribuciones;
    private final PosicionRepositorio posiciones;
    private final CU123Posiciones cu123;
    private final Outbox outbox;

    public CU126DistribuirIntereses(
            Datos datos,
            Transaccionar tx,
            DistribucionRepositorio distribuciones,
            PosicionRepositorio posiciones,
            CU123Posiciones cu123,
            Outbox outbox) {
        this.datos = datos;
        this.tx = tx;
        this.distribuciones = distribuciones;
        this.posiciones = posiciones;
        this.cu123 = cu123;
        this.outbox = outbox;
    }

    /** Las distribuciones de una posicion propia, de la mas vieja a la mas nueva. */
    public List<VistaDistribucion> listar(UUID posicionId, ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> {
            cu123.propia(dsl, posicionId, ctx);
            return distribuciones.deLaPosicion(dsl, posicionId).stream()
                    .map(VistaDistribucion::de)
                    .toList();
        }));
    }

    /**
     * Reparte el interes de una liquidacion DPF. Corre dentro de la transaccion de la
     * liquidacion, DESPUES de emitir el comprobante (la base lo cruza con el).
     *
     * @param dias los dias del periodo que se paga (el plazo, o los devengados si se cancelo antes)
     */
    void registrar(
            DSLContext dsl,
            Rescate r,
            Posicion p,
            Condiciones c,
            Descomposicion d,
            int dias,
            OffsetDateTime ahora,
            ContextoSesion ctx) {
        LocalDate hasta = p.fechaConstitucion().plusDays(Math.max(dias, 0));
        var devengo = posiciones.ultimoDevengo(dsl, p.id(), hasta);
        BigDecimal devengado =
                devengo.map(PosicionRepositorio.Devengo::interesAcumulado).orElse(BigDecimal.ZERO.setScale(2));
        BigDecimal penalizacion = "ANTICIPADO".equals(r.tipo())
                ? devengado.subtract(esperadoTrasPenalizar(devengado, c))
                : BigDecimal.ZERO.setScale(2);
        BigDecimal esperado = devengado.subtract(penalizacion);
        boolean coincide = esperado.compareTo(d.interes()) == 0;
        String motivo = null;
        if (!coincide) {
            motivo = devengo.map(x -> x.fecha().isBefore(hasta)).orElse(true)
                    ? SIN_DEVENGO_AL_CORTE
                    : INTERES_DISTINTO_DEL_DEVENGO;
        }
        var fila = new Distribucion(
                UUID.randomUUID(),
                p.id(),
                r.id(),
                p.usuarioId(),
                p.fechaConstitucion(),
                hasta,
                devengado,
                penalizacion,
                esperado,
                d.interes(),
                d.impuesto(),
                d.interes().subtract(d.impuesto()),
                p.moneda(),
                coincide ? "CONCILIADA" : "DISCREPANCIA",
                motivo,
                ahora);
        if (distribuciones.registrar(dsl, fila)) {
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            coincide ? "inversiones.interes_distribuido" : "inversiones.distribucion_con_discrepancia",
                            "distribucion_interes",
                            fila.id(),
                            Map.of("rescateId", r.id().toString()),
                            UUID.fromString(ctx.traza().id())));
        }
    }

    private static BigDecimal esperadoTrasPenalizar(BigDecimal devengado, Condiciones c) {
        return devengado
                .multiply(BigDecimal.ONE.subtract(c.penalizacionAnticipo().orElse(BigDecimal.ZERO)))
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}

package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.ComisionDeExito;
import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.OrigenDatos;
import bo.aportaya.inversiones.dominio.TextoDeCondiciones;
import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio.ValorGuardado;
import bo.aportaya.inversiones.infraestructura.ComprobanteRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio.Posicion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CU-127 · Estimar la comision por resultado de una posicion de fondo, ANTES de rescatar.
 *
 * <p>La cuenta es la misma que hace la liquidacion (CU-125) con el ultimo valor de cuota
 * publicado: solo se cobra el beneficio elegible, es decir lo que supera la marca maxima previa
 * del LOTE. Cada suscripcion es su propia posicion con su propia marca (la de su valor de
 * entrada), asi que una perdida en un lote no se compensa ni se cobra en otro, y una
 * suscripcion nueva no paga por ganancias anteriores a su entrada. La marca sube con cada
 * rescate que cobro comision y no baja nunca ({@code R-INV-07}).
 *
 * <p>Es una estimacion: la comision real sale de lo que el aliado confirme al rescatar. No
 * inventa un valor de cuota —si no hay ninguno publicado, no estima— y la tasa es SINTETICA
 * mientras no la defina el aliado ({@code DR-INV-04}). Un producto sin comision configurada
 * (todo DPF y los fondos sin tasa) responde {@code AP-CU127-01}: no hay nada que estimar.
 */
@Service
public class CU127ComisionPorResultado {

    public record VistaComision(
            UUID posicionId,
            BigDecimal cuotas,
            BigDecimal valorCuota,
            LocalDate fechaValor,
            BigDecimal marcaMaximaPrevia,
            BigDecimal tasa,
            BigDecimal baseElegible,
            BigDecimal comisionEstimada,
            BigDecimal cuotaNetaEstimada,
            BigDecimal marcaMaximaNueva,
            BigDecimal comisionCobradaAntes,
            String moneda,
            OrigenDatos origen,
            String advertencia) {}

    private final Datos datos;
    private final Transaccionar tx;
    private final CU123Posiciones posiciones;
    private final CatalogoRepositorio catalogo;
    private final ComprobanteRepositorio comprobantes;

    public CU127ComisionPorResultado(
            Datos datos,
            Transaccionar tx,
            CU123Posiciones posiciones,
            CatalogoRepositorio catalogo,
            ComprobanteRepositorio comprobantes) {
        this.datos = datos;
        this.tx = tx;
        this.posiciones = posiciones;
        this.catalogo = catalogo;
        this.comprobantes = comprobantes;
    }

    public VistaComision estimar(UUID posicionId, ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> {
            Posicion p = posiciones.propia(dsl, posicionId, ctx);
            Condiciones c = catalogo.version(dsl, p.versionId()).orElseThrow();
            if (p.tipo() != TipoProducto.FONDO || c.tasaComisionExito().isEmpty()) {
                throw Errores.de(127, 1, "Este producto no cobra comision por resultado: no hay nada que estimar.");
            }
            if (p.cuotas() == null || p.cuotas().signum() == 0) {
                throw Errores.de(127, 3, "La posicion no tiene cuotas abiertas: ya no hay comision por estimar.");
            }
            ValorGuardado v = catalogo.ultimoValor(dsl, p.productoId())
                    .orElseThrow(() -> Errores.de(
                            127, 2, "Todavia no hay un valor de cuota publicado: no se inventa una estimacion."));
            BigDecimal tasa = c.tasaComisionExito().get();
            ComisionDeExito e = ComisionDeExito.calcular(p.cuotas(), v.valor(), p.marcaMaxima(), tasa);
            return new VistaComision(
                    p.id(),
                    p.cuotas(),
                    v.valor(),
                    v.fecha(),
                    p.marcaMaxima(),
                    tasa,
                    e.baseElegible(),
                    e.comision(),
                    e.cuotaNeta(),
                    e.marcaNueva(),
                    comprobantes.comisionesCobradas(dsl, p.id()),
                    p.moneda(),
                    OrigenDatos.valueOf(v.origen()),
                    TextoDeCondiciones.ADVERTENCIA);
        }));
    }
}

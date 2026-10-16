package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.PoliticaDeReintento;
import bo.aportaya.inversiones.infraestructura.CorridaRepositorio;
import bo.aportaya.inversiones.infraestructura.InstruccionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Las operaciones que se disparan por API o por el reloj: sincronizar el catalogo, sincronizar
 * los valores de cuota, devengar los DPF y reintentar las instrucciones pendientes del libro.
 *
 * <p>Las cuatro pasan por el mismo envoltorio ({@link #ejecutar}), que les da lo que un trabajo
 * necesita y que antes no tenian:
 * <ul>
 *   <li><b>Idempotencia de verdad.</b> La clave que el contrato exige se reserva en
 *       {@code corrida_trabajo} (UNIQUE por operacion y clave). La misma clave con el mismo
 *       contenido devuelve el resultado guardado sin volver a correr; con otro contenido se
 *       rechaza; una corrida fallida o caida se retoma con la misma clave.
 *   <li><b>Timeout.</b> Cada llamada al aliado o al libro tiene el suyo; ademas el barrido de
 *       reintentos tiene un plazo maximo y, si lo supera, se corta entero: queda {@code PARCIAL}
 *       y el resto sigue pendiente para la proxima vuelta.
 *   <li><b>Politica de reintento.</b> Espera exponencial con tope y un maximo de intentos
 *       ({@link PoliticaDeReintento}). Pasado el maximo la instruccion NO se descarta: queda
 *       pendiente, contada como atascada y a la vista.
 *   <li><b>Visibilidad.</b> Cada corrida deja su fila (cuando, quien la pidio, como termino) y un
 *       contador por operacion y estado; los medidores {@code inversiones.instrucciones.pendientes}
 *       y {@code inversiones.instrucciones.atascadas} se leen de la base al consultarlos.
 * </ul>
 * El contenido de lo que corre son los casos de uso de siempre: este envoltorio no decide nada
 * de negocio.
 */
@Service
public class OperacionesProgramables {

    public static final String CATALOGO = "SINCRONIZAR_CATALOGO";
    public static final String VALORES = "SINCRONIZAR_VALORES";
    public static final String DEVENGAR = "DEVENGAR";
    public static final String REINTENTO = "REINTENTAR_INSTRUCCIONES";
    public static final String API = "API";
    public static final String PROGRAMADO = "PROGRAMADO";

    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000015");

    private final Datos datos;
    private final Transaccionar tx;
    private final CorridaRepositorio corridas;
    private final InstruccionRepositorio instrucciones;
    private final CU120Catalogo catalogo;
    private final CU123Posiciones posiciones;
    private final CU123Devengo devengo;
    private final AplicadorDeInstrucciones aplicador;
    private final Reloj reloj;
    private final MeterRegistry metricas;
    private final PoliticaDeReintento politica;
    private final Duration plazoMaximo;
    private final EjecucionDeCorridas ejecucion;
    private final int porVuelta;
    private final int lectura;

    public OperacionesProgramables(
            Datos datos,
            Transaccionar tx,
            CorridaRepositorio corridas,
            InstruccionRepositorio instrucciones,
            CU120Catalogo catalogo,
            CU123Posiciones posiciones,
            CU123Devengo devengo,
            AplicadorDeInstrucciones aplicador,
            Reloj reloj,
            MeterRegistry metricas,
            @Value("${aportaya.inversiones.instrucciones.reintento.base:PT30S}") Duration base,
            @Value("${aportaya.inversiones.instrucciones.reintento.tope:PT1H}") Duration tope,
            @Value("${aportaya.inversiones.instrucciones.reintento.max-intentos:12}") int maxIntentos,
            @Value("${aportaya.inversiones.trabajos.plazo-maximo:PT45S}") Duration plazoMaximo,
            @Value("${aportaya.inversiones.trabajos.en-curso-caduca:PT10M}") Duration caducidadEnCurso,
            @Value("${aportaya.inversiones.instrucciones.reintento.por-vuelta:200}") int porVuelta,
            @Value("${aportaya.inversiones.instrucciones.reintento.lectura:500}") int lectura) {
        this.datos = datos;
        this.tx = tx;
        this.corridas = corridas;
        this.instrucciones = instrucciones;
        this.catalogo = catalogo;
        this.posiciones = posiciones;
        this.devengo = devengo;
        this.aplicador = aplicador;
        this.reloj = reloj;
        this.metricas = metricas;
        this.politica = new PoliticaDeReintento(base, tope, maxIntentos);
        this.plazoMaximo = plazoMaximo;
        this.ejecucion = new EjecucionDeCorridas(datos, tx, corridas, reloj, metricas, caducidadEnCurso);
        this.porVuelta = porVuelta;
        this.lectura = lectura;
        Gauge.builder("inversiones.instrucciones.pendientes", this, o -> o.contarPendientes(0))
                .description("Instrucciones al libro todavia pendientes")
                .register(metricas);
        Gauge.builder("inversiones.instrucciones.atascadas", this, o -> o.contarPendientes(maxIntentos))
                .description("Pendientes que ya pasaron el maximo de intentos: el trabajo no las toca")
                .register(metricas);
    }

    /** El resultado que una corrida dejo escrito, como mapa (lo usa la vista de las corridas). */
    public static Map<String, String> leerResultado(String json) {
        return EjecucionDeCorridas.leerResultado(json);
    }

    public ContextoSesion sistema() {
        return ContextoSesion.deSistema(PROCESO, new Traza(UUID.randomUUID().toString()));
    }

    // ------------------------------------------------------------------ las cuatro operaciones
    public Map<String, String> sincronizarCatalogo(String clave, String origen, ContextoSesion ctx) {
        return ejecucion.ejecutar(CATALOGO, 120, 4, clave, "", origen, ctx, () -> {
            int nuevos = catalogo.sincronizar(ctx);
            return Map.of(
                    "nuevos",
                    String.valueOf(nuevos),
                    "productos",
                    String.valueOf(catalogo.listar(ctx).size()));
        });
    }

    public Map<String, String> sincronizarValores(String clave, String origen, ContextoSesion ctx) {
        return ejecucion.ejecutar(
                VALORES,
                123,
                3,
                clave,
                "",
                origen,
                ctx,
                () -> Map.of("nuevos", String.valueOf(posiciones.sincronizarValores(ctx)), "productos", "0"));
    }

    public Map<String, String> devengar(LocalDate fecha, String clave, String origen, ContextoSesion ctx) {
        return ejecucion.ejecutar(DEVENGAR, 123, 3, clave, fecha.toString(), origen, ctx, () -> {
            var r = devengo.devengar(fecha, ctx);
            Map<String, String> m = new LinkedHashMap<>();
            m.put("fecha", r.fecha().toString());
            m.put("devengadas", String.valueOf(r.devengadas()));
            m.put("yaDevengadas", String.valueOf(r.yaDevengadas()));
            m.put("total", r.total().toPlainString());
            return m;
        });
    }

    /**
     * @param limite cuantas instrucciones se intentan como mucho; cero o menos = las de una vuelta del reloj
     * @param forzar ignora la espera y el tope de intentos: es lo que pide una persona; el reloj no lo usa
     */
    public Map<String, String> reintentarInstrucciones(
            int limite, boolean forzar, String clave, String origen, ContextoSesion ctx) {
        int tope = limite > 0 ? limite : porVuelta;
        return ejecucion.ejecutar(REINTENTO, 122, 3, clave, limite + "|" + forzar, origen, ctx, () -> {
            var b = aplicador.reintentarPendientes(tope, Math.max(lectura, tope), politica, forzar, plazoMaximo, ctx);
            Map<String, String> m = new LinkedHashMap<>();
            m.put("aplicadas", String.valueOf(b.aplicadas()));
            m.put("rechazadas", String.valueOf(b.rechazadas()));
            m.put("siguenPendientes", String.valueOf(b.siguenPendientes()));
            m.put("esperando", String.valueOf(b.esperando()));
            m.put("atascadas", String.valueOf(b.atascadas()));
            m.put("cortada", String.valueOf(b.cortado()));
            return m;
        });
    }

    /** Lo que hace el reloj en cada vuelta: las que le tocan segun la politica, con el limite de una vuelta. */
    public Map<String, String> reintentarDelReloj(String clave) {
        return reintentarInstrucciones(0, false, clave, PROGRAMADO, sistema());
    }

    /** Hay algo que reintentar ahora? El reloj no abre una corrida (ni una fila) por minuto si no hay nada. */
    public boolean hayPendientes() {
        return contarPendientes(0) > 0;
    }

    public List<VistaCorrida> corridas(String operacion, int limite, ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> corridas.recientes(dsl, operacion, limite).stream()
                .map(VistaCorrida::de)
                .toList()));
    }

    /** Cuantas pendientes hay con al menos {@code desdeIntentos} intentos (0 = todas). */
    public int contarPendientes(int desdeIntentos) {
        try {
            return tx.en(() -> datos.conContexto(sistema(), dsl -> instrucciones.contarPendientes(dsl, desdeIntentos)));
        } catch (RuntimeException e) {
            return -1;
        }
    }
}

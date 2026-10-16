package bo.aportaya.inversiones;

import bo.aportaya.inversiones.aplicacion.CU127ComisionPorResultado;
import bo.aportaya.inversiones.aplicacion.CU128ReporteFiscal;
import bo.aportaya.inversiones.aplicacion.CU129SimularCaja;
import bo.aportaya.inversiones.aplicacion.OperacionesProgramables;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

/**
 * Lo que se arma ademas de {@link BaseDeLiquidacion} para probar la comision estimada, el reporte
 * de comprobantes, la simulacion de caja y las operaciones programables, todo contra PostgreSQL
 * real y con el libro y el aliado dobles.
 */
public abstract class BaseDeAnalisis extends BaseDeLiquidacion {

    protected static final int MAX_INTENTOS = 5;
    protected static final Duration ESPERA_BASE = Duration.ofSeconds(30);
    protected static final int POR_VUELTA = 200;
    protected static final int LECTURA = 500;

    protected CU127ComisionPorResultado cu127;
    protected CU128ReporteFiscal cu128;
    protected CU129SimularCaja cu129;
    protected OperacionesProgramables operaciones;
    protected SimpleMeterRegistry metricas;

    @BeforeEach
    void armarAnalisis() {
        // La base de las pruebas se comparte entre clases: lo que otra prueba dejo pendiente en el libro (con otro
        // doble) no es de esta, y un barrido de sistema lo tocaria. Se lo da por rechazado antes de empezar.
        dslFixtura.execute(
                "update inversiones.instruccion_libro set estado = 'FALLIDA', ultimo_error = 'LIMPIEZA_DE_PRUEBA'"
                        + " where estado = 'PENDIENTE'");
        cu127 = new CU127ComisionPorResultado(datos, tx, cu123, catalogo, comprobantes);
        cu128 = new CU128ReporteFiscal(datos, tx, comprobantes, instrucciones, distribuciones, posiciones, catalogo);
        cu129 = new CU129SimularCaja(
                cu120, List.of(new BigDecimal("-0.10"), new BigDecimal("0.00"), new BigDecimal("0.10")));
        metricas = new SimpleMeterRegistry();
        operaciones = new OperacionesProgramables(
                datos,
                tx,
                corridas,
                instrucciones,
                cu120,
                cu123,
                cu123Devengo,
                aplicador,
                RELOJ,
                metricas,
                ESPERA_BASE,
                Duration.ofHours(1),
                MAX_INTENTOS,
                Duration.ofSeconds(45),
                Duration.ofMinutes(10),
                POR_VUELTA,
                LECTURA);
    }

    /** Una clave de idempotencia nueva, como la que manda un cliente. */
    protected String clave() {
        return UUID.randomUUID().toString();
    }

    /** Una columna de la corrida de esa clave, o nulo si no hay corrida. */
    protected String deLaCorrida(String clave, String columna) {
        var r = dslFixtura.fetch(
                "select " + columna + " from inversiones.corrida_trabajo where clave_idempotencia = ?", clave);
        return r.isEmpty() ? null : String.valueOf(r.get(0).get(0));
    }
}

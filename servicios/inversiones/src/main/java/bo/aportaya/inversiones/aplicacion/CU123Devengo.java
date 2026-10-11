package bo.aportaya.inversiones.aplicacion;

import bo.aportaya.inversiones.dominio.Condiciones;
import bo.aportaya.inversiones.dominio.DevengoDeDpf;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio.Posicion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

/**
 * CU-123 · Devengo diario de los DPF abiertos.
 *
 * <p>Idempotente por posicion y fecha (UNIQUE): correrlo dos veces el mismo dia no duplica
 * nada, y correrlo despues de dias sin correr cubre la diferencia. El devengo del dia es la
 * diferencia de dos acumulados ya redondeados, asi que la suma de todos los dias es el
 * interes del plazo sin un centavo perdido.
 */
@Service
public class CU123Devengo {

    public record SalidaDevengo(LocalDate fecha, int devengadas, int yaDevengadas, BigDecimal total) {}

    private final Datos datos;
    private final Transaccionar tx;
    private final PosicionRepositorio posiciones;
    private final CatalogoRepositorio catalogo;
    private final Reloj reloj;

    public CU123Devengo(
            Datos datos, Transaccionar tx, PosicionRepositorio posiciones, CatalogoRepositorio catalogo, Reloj reloj) {
        this.datos = datos;
        this.tx = tx;
        this.posiciones = posiciones;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ devengo diario
    public SalidaDevengo devengar(LocalDate fecha, ContextoSesion ctx) {
        return tx.en(() -> datos.conContexto(ctx, dsl -> {
            OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
            int nuevos = 0;
            int previos = 0;
            BigDecimal total = BigDecimal.ZERO;
            for (Posicion p : posiciones.dpfAbiertos(dsl)) {
                if (fecha.isBefore(p.fechaConstitucion()) || fecha.isAfter(p.fechaVencimiento())) {
                    continue;
                }
                Condiciones c = catalogo.version(dsl, p.versionId()).orElseThrow();
                int dias = (int) ChronoUnit.DAYS.between(p.fechaConstitucion(), fecha);
                BigDecimal acumulado = DevengoDeDpf.acumulado(
                        p.principal(),
                        c.tasaNominalAnual().orElseThrow(),
                        dias,
                        c.plazoDias().orElseThrow(),
                        c.baseDias().orElseThrow());
                BigDecimal anterior = posiciones
                        .ultimoDevengo(dsl, p.id(), fecha.minusDays(1))
                        .map(PosicionRepositorio.Devengo::interesAcumulado)
                        .orElse(BigDecimal.ZERO);
                // El devengo del dia es la diferencia de dos acumulados ya redondeados: la
                // suma de todos los dias es el interes del plazo, sin un centavo perdido.
                BigDecimal monto = acumulado.subtract(anterior);
                if (posiciones.insertarDevengo(dsl, p.id(), fecha, dias, acumulado, monto, p.moneda(), ahora)) {
                    nuevos++;
                    total = total.add(monto);
                } else {
                    previos++;
                }
            }
            return new SalidaDevengo(fecha, nuevos, previos, total);
        }));
    }
}

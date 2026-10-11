package bo.aportaya.inversiones.trabajos;

import bo.aportaya.inversiones.aplicacion.OperacionesProgramables;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Trae del aliado el catalogo (una vez al dia) y el ultimo valor de cuota de cada fondo (cada
 * hora). <b>No decide nada</b>: llama a {@link OperacionesProgramables}.
 *
 * <p>Las dos son idempotentes por contenido (una version nueva solo nace si cambio algo; un valor
 * publicado no se reescribe) y ademas llevan clave por ventana (dia u hora), asi que una segunda
 * replica no repite la llamada al aliado. Con el aliado caido la corrida queda {@code FALLIDA}
 * con {@code AP-CU120-02} / {@code AP-CU123-02}: las pantallas siguen mostrando la ultima
 * cotizacion y el valor de cuota con su fecha, marcado como desactualizado si es viejo.
 */
@Component
@ConditionalOnProperty(name = "aportaya.inversiones.trabajos.habilitados", havingValue = "true", matchIfMissing = true)
public class SincronizacionDelAliado {

    private static final Logger LOG = LoggerFactory.getLogger(SincronizacionDelAliado.class);
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final OperacionesProgramables operaciones;
    private final Reloj reloj;

    public SincronizacionDelAliado(OperacionesProgramables operaciones, Reloj reloj) {
        this.operaciones = operaciones;
        this.reloj = reloj;
    }

    @Scheduled(cron = "${aportaya.inversiones.trabajos.catalogo.cron:0 0 6 * * *}", zone = "America/La_Paz")
    @SchedulerLock(name = "inversiones.sincronizar-catalogo", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void catalogo() {
        String clave = "CAT:" + DIA.format(ZonedDateTime.ofInstant(reloj.ahora(), Reloj.ZONA));
        try {
            operaciones.sincronizarCatalogo(clave, OperacionesProgramables.PROGRAMADO, operaciones.sistema());
        } catch (RuntimeException fallo) {
            LOG.warn(
                    "inversiones · la sincronizacion del catalogo fallo: {}",
                    fallo.getClass().getSimpleName());
        }
    }

    @Scheduled(cron = "${aportaya.inversiones.trabajos.valores.cron:0 15 * * * *}", zone = "America/La_Paz")
    @SchedulerLock(name = "inversiones.sincronizar-valores", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void valores() {
        String clave = "VAL:" + HORA.format(ZonedDateTime.ofInstant(reloj.ahora(), Reloj.ZONA));
        try {
            operaciones.sincronizarValores(clave, OperacionesProgramables.PROGRAMADO, operaciones.sistema());
        } catch (RuntimeException fallo) {
            LOG.warn(
                    "inversiones · la sincronizacion de valores de cuota fallo: {}",
                    fallo.getClass().getSimpleName());
        }
    }
}

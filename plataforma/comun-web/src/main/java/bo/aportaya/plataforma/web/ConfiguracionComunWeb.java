package bo.aportaya.plataforma.web;

import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.ConfiguracionMensajeria;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.plataforma.web.errores.TraduccionDeRestricciones;
import bo.aportaya.plataforma.web.idempotencia.Idempotencia;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Lo que un servicio da por sentado, cableado de una vez.
 *
 * <p>No hay ningun archivo donde registrar el servicio: cada uno es un proceso y
 * nadie lo anota en una lista. Es el conflicto numero uno del plan de carriles,
 * eliminado por construccion.
 *
 * <p>{@code @Import(ConfiguracionMensajeria.class)} (H2.S1.M2) porque el relevo del
 * outbox vive en {@code comun-mensajeria}, un modulo sin Spring propio a proposito
 * (igual que {@code comun-dominio}): el cableado de {@code @EnableScheduling} y el
 * {@code LockProvider} vive donde ya vive todo el cableado.
 */
@Configuration
@ComponentScan("bo.aportaya.plataforma.web")
@Import(ConfiguracionMensajeria.class)
public class ConfiguracionComunWeb {

    @Bean
    @ConditionalOnMissingBean
    public TraduccionDeRestricciones traduccionDeRestricciones() {
        return TraduccionDeRestricciones.cargar();
    }

    /**
     * El {@link Reloj} inyectado (no {@code OffsetDateTime.now()} adentro): es lo que
     * permite que {@code IdempotenciaRepositorioTest} pare el tiempo para probar
     * "expirada" sin un {@code Thread.sleep}, y lo mismo en cualquier prueba que declare
     * el suyo (gana por {@code @ConditionalOnMissingBean}, ver mas abajo).
     *
     * <p>{@code aportaya.idempotencia.vigencia} (default {@code PT24H}, H1.S2.M1): cuanto
     * dura una reserva antes de poder reemplazarse como si no existiera. Es config y no una
     * constante en {@code Idempotencia} porque el plazo razonable no es el mismo para un
     * pago que para un sorteo de turnos, y cambiar una circular no puede exigir desplegar
     * codigo nuevo (invariante 10, la misma razon de {@code SinUmbralLiteral}).
     */
    @Bean
    @ConditionalOnMissingBean
    public Idempotencia idempotencia(
            @Value("${aportaya.esquema}") String esquema,
            Reloj reloj,
            @Value("${aportaya.idempotencia.vigencia:PT24H}") Duration vigencia) {
        return new Idempotencia(esquema, reloj, vigencia);
    }

    /**
     * El reloj y el azar, inyectados.
     *
     * <p>Los declara esta configuracion y no {@code comun-dominio} porque ese modulo no
     * tiene Spring a proposito: es lo que permite probar los atomos en milisegundos y lo
     * que ArchUnit verifica. Los bordes se cablean donde ya vive el cableado.
     *
     * <p>{@code @ConditionalOnMissingBean} para que una prueba pueda parar el tiempo sin
     * pelear con esto: quien declare el suyo, gana.
     */
    @Bean
    @ConditionalOnMissingBean
    public Reloj reloj() {
        return Reloj.delSistema();
    }

    @Bean
    @ConditionalOnMissingBean
    public Ids ids() {
        return Ids.seguros();
    }

    /**
     * El outbox y el registro de lo consumido, atados al esquema del servicio.
     *
     * <p>Saber en que esquema escribir es la unica diferencia entre los catorce. Sale de
     * {@code aportaya.esquema}: un outbox apuntando al esquema equivocado publicaria los
     * eventos de otro servicio.
     */
    @Bean
    @ConditionalOnMissingBean
    public Outbox outbox(@Value("${aportaya.esquema}") String esquema) {
        return new Outbox(esquema);
    }

    @Bean
    @ConditionalOnMissingBean
    public Consumidos consumidos(@Value("${aportaya.esquema}") String esquema) {
        return new Consumidos(esquema);
    }
}

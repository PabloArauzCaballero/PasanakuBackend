package bo.aportaya.plataforma.mensajeria;

import bo.aportaya.plataforma.dominio.Indicador;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import java.util.List;
import org.jooq.DSLContext;

/**
 * Tableros (CU-98) · lo unico que un servicio dueño escribe para publicar sus indicadores: como se calculan sobre SUS
 * datos. El disparo diario, el relleno, la transaccion, el contexto de sistema y el outbox los pone
 * {@link PublicacionDeIndicadores}, una sola vez para todos.
 */
public interface CalculadorDeIndicadores {

    /** Un indicador calculado con el nombre que vera el tablero (hasta 80 caracteres). */
    record Calculado(Indicador indicador, String nombre) {}

    /** Corre dentro de una transaccion con contexto de sistema. No publica: devuelve lo calculado. */
    List<Calculado> calcular(DSLContext dsl, PeriodoDeTablero periodo);
}

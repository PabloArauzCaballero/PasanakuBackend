package bo.aportaya.plataforma.datos;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * La puerta por la que pasa TODA consulta del proyecto.
 *
 * <p>Fija {@code app.usuario_id}, {@code app.rol} y {@code app.traza} con
 * {@code SET LOCAL} dentro de la transaccion en curso, que es lo que las politicas de
 * fila leen. Tres detalles, y los tres son la diferencia entre que RLS proteja algo o
 * no proteja nada:
 *
 * <ul>
 *   <li><b>{@code set_config(..., true)} es {@code SET LOCAL}</b>: muere en el
 *       {@code COMMIT}. Un {@code SET} plano sobrevive a la peticion y el siguiente
 *       request hereda la identidad del anterior — la fuga mas silenciosa que puede
 *       tener este sistema, y no deja rastro.
 *   <li><b>Sin transaccion no se ejecuta</b>: {@code SET LOCAL} suelto no fija nada y
 *       PostgreSQL solo emite un WARNING.
 *   <li><b>El {@code DSLContext} es el de la transaccion en curso</b>: tomar otra
 *       conexion pierde el contexto en silencio. Es el error mas caro de esta capa, y
 *       lo evita el {@code TransactionAwareDataSourceProxy} de
 *       {@link ConfiguracionDeDatos}.
 * </ul>
 *
 * <p>No lleva {@code @Transactional} a proposito: la transaccion la abre el caso de
 * uso, y una sola vez (invariante 2). Si esta clase la abriera por su cuenta,
 * «una transaccion por caso de uso» seria una frase y no una garantia.
 */
public final class Datos {

    private final DSLContext dsl;

    public Datos(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    public <T> T conContexto(ContextoSesion ctx, Function<DSLContext, T> consulta) {
        Objects.requireNonNull(ctx, "contexto de sesion");
        Objects.requireNonNull(consulta, "consulta");
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new SinTransaccion();
        }
        fijar("app.usuario_id", ctx.usuarioId().toString());
        fijar("app.rol", ctx.rol());
        fijar("app.traza", ctx.traza().id());
        return consulta.apply(dsl);
    }

    /**
     * Ejecuta {@code trabajo} con el rol de {@code sistema} y restaura el rol anterior al terminar.
     *
     * <p>Es la unica forma de escribir lo que las politicas de fila reservan al sistema
     * (el libro de billetera, por ejemplo) desde una peticion de un participante. Es
     * deliberadamente <b>estatico y de alcance estrecho</b>: no cambia {@code app.usuario_id}
     * (quien actuo sigue constando) y el rol vuelve a ser el de la sesion apenas termina el
     * bloque, de modo que no queda un privilegio abierto para el resto de la peticion.
     * Quien lo usa responde de haber autorizado antes la operacion.
     */
    public static <T> T comoSistema(DSLContext dsl, Supplier<T> trabajo) {
        Objects.requireNonNull(dsl, "dsl");
        Objects.requireNonNull(trabajo, "trabajo");
        String rolDeLaSesion = dsl.fetchOne("select coalesce(current_setting('app.rol', true), '')")
                .get(0, String.class);
        dsl.execute("select set_config('app.rol', ?, true)", ContextoSesion.ROL_SISTEMA);
        T resultado;
        try {
            resultado = trabajo.get();
        } catch (RuntimeException fallo) {
            // Si el trabajo aborto la transaccion, restaurar el rol tambien falla: el error que
            // importa es el del trabajo, no el de la restauracion, que se anota aparte.
            try {
                dsl.execute("select set_config('app.rol', ?, true)", rolDeLaSesion);
            } catch (RuntimeException alRestaurar) {
                fallo.addSuppressed(alRestaurar);
            }
            throw fallo;
        }
        dsl.execute("select set_config('app.rol', ?, true)", rolDeLaSesion);
        return resultado;
    }

    private void fijar(String clave, String valor) {
        // El `true` del tercer parametro es lo que lo vuelve LOCAL.
        dsl.execute("select set_config(?, ?, true)", clave, valor);
    }
}

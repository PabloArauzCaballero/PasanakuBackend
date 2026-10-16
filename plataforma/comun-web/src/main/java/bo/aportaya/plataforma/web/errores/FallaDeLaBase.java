package bo.aportaya.plataforma.web.errores;

import java.sql.SQLException;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * Lo que rechaza la base y no tiene regla {@code R-XXX-nn} detras, catalogado por la
 * clase de error de PostgreSQL (SQLSTATE).
 *
 * <p>Existe porque el catalogo de reglas cubre las restricciones de
 * {@code restricciones.sql} y nada mas: un indice unico de {@code sql/30_indices}
 * —{@code uq_usuario_correo}, por ejemplo— no tiene regla, y antes salia como
 * {@code 500 AP-INT-01}, «algo salio mal de nuestro lado», cuando lo que habia pasado
 * era que el correo ya tenia cuenta. El cliente no podia distinguir un duplicado de
 * una caida, y no tenia como decirle a la persona que hacer.
 *
 * <p>El codigo es estable y lo distingue el cliente; el mensaje es generico a
 * proposito: el nombre de la restriccion y los valores que la violaron nunca salen
 * por la API (ver {@link ErrorApi}). Quien tiene que saber cual fue es el equipo, y
 * lo lee en la bitacora junto a la traza.
 */
enum FallaDeLaBase {
    DUPLICADO("AP-BD-01", HttpStatus.CONFLICT, "Ya existe un registro con esos datos."),
    REFERENCIA_INEXISTENTE(
            "AP-BD-02", HttpStatus.CONFLICT, "Uno de los datos hace referencia a algo que no existe o cambio."),
    DATO_INVALIDO("AP-BD-03", HttpStatus.UNPROCESSABLE_ENTITY, "Uno de los datos no cumple las reglas del registro."),
    SUPERPOSICION("AP-BD-04", HttpStatus.CONFLICT, "Se superpone con otro registro vigente."),
    CONCURRENCIA(
            "AP-BD-05", HttpStatus.CONFLICT, "Otra operacion cambio estos datos al mismo tiempo. Volve a intentar."),
    TIEMPO_AGOTADO(
            "AP-BD-06",
            HttpStatus.SERVICE_UNAVAILABLE,
            "La operacion tardo demasiado. Volve a intentar en un momento.");

    private final String codigo;
    private final HttpStatus estado;
    private final String mensaje;

    FallaDeLaBase(String codigo, HttpStatus estado, String mensaje) {
        this.codigo = codigo;
        this.estado = estado;
        this.mensaje = mensaje;
    }

    String codigo() {
        return codigo;
    }

    HttpStatus estado() {
        return estado;
    }

    String mensaje() {
        return mensaje;
    }

    /**
     * La clase de la falla, si la cadena de causas trae un SQLSTATE que el catalogo
     * reconoce. Sin SQLSTATE no hay como saber que paso, y eso sigue siendo un 500.
     */
    static Optional<FallaDeLaBase> de(Throwable e) {
        return sqlState(e).flatMap(FallaDeLaBase::porSqlState);
    }

    static Optional<String> sqlState(Throwable e) {
        Throwable actual = e;
        for (int i = 0; actual != null && i < 32; i++) {
            if (actual instanceof SQLException sql && sql.getSQLState() != null) {
                return Optional.of(sql.getSQLState());
            }
            actual = actual.getCause() == actual ? null : actual.getCause();
        }
        return Optional.empty();
    }

    /** Codigos de https://www.postgresql.org/docs/16/errcodes-appendix.html. */
    static Optional<FallaDeLaBase> porSqlState(String estado) {
        return Optional.ofNullable(
                switch (estado) {
                    case "23505" -> DUPLICADO; // unique_violation
                    case "23503" -> REFERENCIA_INEXISTENTE; // foreign_key_violation
                    case "23514", "23502", "23000", "23001" -> DATO_INVALIDO; // check, not_null, integrity, restrict
                    case "23P01" -> SUPERPOSICION; // exclusion_violation
                    case "40001", "40P01" -> CONCURRENCIA; // serialization_failure, deadlock_detected
                    case "57014", "55P03" -> TIEMPO_AGOTADO; // query_canceled, lock_not_available
                    default -> estado.startsWith("22") ? DATO_INVALIDO : null; // data_exception
                });
    }
}

package bo.aportaya.plataforma.web.idempotencia;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;

/**
 * La clave ya esta reservada por otro intento en curso (o por este mismo, todavia sin
 * respuesta final) y no expiro.
 *
 * <p>Distinta de {@link OperacionRepetida}: ahi ya hay una respuesta para devolver
 * integra. Aca no hay nada que devolver todavia — el cliente tiene que reintentar mas
 * tarde, no confundir esto con "listo". Por eso es {@code 409} y no {@code 200}, y por
 * eso tampoco es {@link IdempotenciaConflicto}: el cuerpo puede coincidir o no, todavia
 * no hay con que compararlo con certeza — lo unico que se sabe es que hay una carrera.
 *
 * <p>{@code AP-CU00-02} igual que {@link IdempotenciaConflicto}: infraestructura, no un
 * caso de uso — va como literal en el manejador y no via {@code CodigoError} porque esta
 * clase no expone {@code detalle()} (no extiende {@code ErrorDeNegocio}: no hay una
 * regla de negocio que explicar, solo una carrera).
 */
public class IdempotenciaEnProceso extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public IdempotenciaEnProceso() {
        super("Esa operacion ya se esta procesando. Intenta de nuevo en unos segundos.");
    }
}

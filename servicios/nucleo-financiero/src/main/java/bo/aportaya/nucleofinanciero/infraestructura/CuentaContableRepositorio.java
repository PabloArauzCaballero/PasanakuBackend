package bo.aportaya.nucleofinanciero.infraestructura;

import static bo.aportaya.nucleofinanciero.generado.Tables.CUENTA_CONTABLE;

import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * El plan de cuentas. Solo lectura: {@code cuenta_contable.saldo} lo deriva el motor
 * desde {@code movimiento_contable} (R-CTB-09), igual que el saldo de billetera desde
 * R-BIL-16. Un método que lo escribiera acá volvería a poner en la aplicación una
 * garantía que ya vive en la base.
 */
@Component
public class CuentaContableRepositorio {

    /**
     * {@code R-CTB-02} vive en la base; acá solo se resuelve el código a su identificador y
     * a su moneda (R-CTB-10: una cuenta es de una sola moneda).
     */
    public record CuentaEncontrada(UUID id, String moneda) {}

    public Optional<CuentaEncontrada> porCodigo(DSLContext dsl, String codigo) {
        return dsl.select(CUENTA_CONTABLE.ID, CUENTA_CONTABLE.MONEDA)
                .from(CUENTA_CONTABLE)
                .where(CUENTA_CONTABLE.CODIGO.eq(codigo))
                .fetchOptional(r -> new CuentaEncontrada(r.value1(), r.value2()));
    }
}

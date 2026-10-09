package bo.aportaya.aportes.dominio;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cuanto del pozo de un periodo ya es caja y cuanto falta.
 *
 * <p>Ejemplo del plan: pozo 6.000 y 5.000 confirmados dejan un faltante de 1.000, que es
 * lo que se le pide al respaldo empresarial. <b>Un aporte pendiente no es caja
 * confirmada</b>: solo cuenta lo que {@code CU-21} acredito despues de la confirmacion del
 * proveedor (invariante de «pagado» = «el banco lo confirmo»).
 *
 * <p>Tres decisiones, todas para no inflar la caja:
 *
 * <ul>
 *   <li>un pago de mas sobre una obligacion no cuenta para el pozo: es credito del
 *       miembro, no caja de este turno;
 *   <li>lo que cubrio el fondo mutual cuenta aparte y no se mezcla con lo confirmado: son
 *       dos mecanismos distintos y el respaldo empresarial solo cubre lo que ninguno de los
 *       dos puso;
 *   <li>una obligacion condonada o exonerada sigue faltando como caja: quien absorbe esa
 *       condonacion es una decision comercial que este calculo no toma (ambiguedad A11).
 * </ul>
 */
public final class RecaudoDelPeriodo {

    private RecaudoDelPeriodo() {}

    /** Una obligacion periodica del periodo, tal como la guarda este servicio. */
    public record Obligacion(UUID id, Dinero esperado, Dinero pagado, Dinero cubiertoMutual) {}

    /** Lo que falta de una obligacion para que el pozo este completo. */
    public record Pendiente(UUID obligacionId, Dinero monto) {}

    public record Resultado(
            Dinero pozo, Dinero confirmado, Dinero cubiertoMutual, Dinero faltante, List<Pendiente> pendientes) {}

    public static Resultado calcular(Moneda moneda, List<Obligacion> obligaciones) {
        Dinero cero = Dinero.cero(moneda);
        Dinero pozo = cero;
        Dinero confirmado = cero;
        Dinero mutual = cero;
        List<Pendiente> pendientes = new ArrayList<>();

        for (Obligacion o : obligaciones) {
            if (o.esperado().moneda() != moneda
                    || o.pagado().moneda() != moneda
                    || o.cubiertoMutual().moneda() != moneda) {
                throw new ErrorDeNegocio(
                        CodigoError.de(21, 5),
                        "El periodo mezcla monedas: el pozo no se puede sumar sin tipo de cambio.");
            }
            Dinero cuentaComoCaja = o.pagado().esMayorQue(o.esperado()) ? o.esperado() : o.pagado();
            Dinero restante = o.esperado().menos(cuentaComoCaja);
            Dinero porMutual = o.cubiertoMutual().esMayorQue(restante) ? restante : o.cubiertoMutual();
            Dinero falta = restante.menos(porMutual);

            pozo = pozo.mas(o.esperado());
            confirmado = confirmado.mas(cuentaComoCaja);
            mutual = mutual.mas(porMutual);
            if (!falta.esCero()) {
                pendientes.add(new Pendiente(o.id(), falta));
            }
        }
        Dinero faltante = pozo.menos(confirmado).menos(mutual);
        return new Resultado(pozo, confirmado, mutual, faltante, List.copyOf(pendientes));
    }
}

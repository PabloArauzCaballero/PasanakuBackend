package bo.aportaya.garantia.dominio;

import bo.aportaya.plataforma.dominio.Dinero;

/**
 * Lo que la empresa puede poner de su bolsillo, y lo que ya puso.
 *
 * <p>Tres cifras gobiernan una reserva y salen del libro de movimientos, no de un
 * contador que alguien suma a mano:
 *
 * <ul>
 *   <li><b>disponible</b> = reservado - aplicado - liberado: lo que todavia puede cubrir.
 *   <li><b>exposicion</b> = aplicado - recuperado: lo que la empresa tiene afuera.
 * </ul>
 *
 * <p>Una recuperacion <b>no repone el disponible</b>: el aporte tardio reduce la perdida
 * final pero no financia el pago de hoy (PLAN §4.2). Capital para absorber perdidas y
 * liquidez para pagar a tiempo son controles distintos.
 */
public final class RespaldoEmpresarial {

    private RespaldoEmpresarial() {}

    public record Reserva(Dinero reservado, Dinero aplicado, Dinero recuperado, Dinero liberado) {

        public Dinero disponible() {
            return reservado.menos(aplicado).menos(liberado);
        }

        public Dinero exposicion() {
            return aplicado.menos(recuperado);
        }
    }

    /** Todo o nada: la cobertura parcial no esta definida (ambiguedad A4). */
    public record Decision(boolean cubre, Dinero faltante, Dinero disponible, Dinero sinCubrir) {}

    public static Decision decidir(Reserva reserva, Dinero faltante) {
        Dinero disponible = reserva.disponible();
        boolean alcanza = !faltante.esMayorQue(disponible);
        Dinero sinCubrir = alcanza ? Dinero.cero(faltante.moneda()) : faltante.menos(disponible);
        return new Decision(alcanza, faltante, disponible, sinCubrir);
    }

    /**
     * Lo que un pago tardio recupera de una linea cubierta, y lo que no es de la empresa.
     *
     * <p>El excedente existe porque el pago puede incluir un recargo que es del grupo:
     * ese dinero nunca fue adelantado por la empresa y no se le devuelve.
     */
    public record Recuperacion(Dinero aplicado, Dinero excedente) {}

    public static Recuperacion recuperar(Dinero cubierto, Dinero yaRecuperado, Dinero pago) {
        Dinero pendiente = cubierto.menos(yaRecuperado);
        Dinero aplicado = pago.esMayorQue(pendiente) ? pendiente : pago;
        return new Recuperacion(aplicado, pago.menos(aplicado));
    }

    /** Lo que se puede devolver a la caja al cerrar el ciclo. */
    public static Dinero liberable(Reserva reserva) {
        return reserva.disponible();
    }
}

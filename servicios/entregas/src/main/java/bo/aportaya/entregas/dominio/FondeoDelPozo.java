package bo.aportaya.entregas.dominio;

import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.Dinero;

/**
 * Como queda fondeado el pozo de un turno: caja confirmada + fondo mutual.
 *
 * <p>El principal es SIEMPRE el pozo completo (ambiguedad A6). Lo que falte queda como <b>deuda
 * conservada</b> ({@code pendiente}) y la entrega no sale hasta que la cubra el fondo del grupo
 * (ADR-055 E7, F3.L2.M05). Nunca se entrega menos «y despues vemos». La empresa no cubre nada
 * (F2.L2.M01): {@code cubiertoEmpresa} queda en cero y se conserva solo porque es parte del contrato
 * de la respuesta y del evento hasta su contraccion.
 */
public final class FondeoDelPozo {

    private FondeoDelPozo() {}

    public record Resultado(
            Dinero pozo,
            Dinero confirmado,
            Dinero cubiertoMutual,
            Dinero faltante,
            Dinero cubiertoEmpresa,
            Dinero pendiente) {

        public boolean completo() {
            return pendiente.esCero();
        }
    }

    public static Resultado calcular(RecaudoDelPozo.Recaudo recaudo) {
        Dinero faltante = recaudo.faltante();
        return new Resultado(
                recaudo.pozo(),
                recaudo.confirmado(),
                recaudo.cubiertoMutual(),
                faltante,
                Dinero.cero(faltante.moneda()),
                faltante);
    }
}

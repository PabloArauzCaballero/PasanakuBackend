package bo.aportaya.entregas.dominio;

import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.Dinero;

/**
 * Como queda fondeado el pozo de un turno: caja confirmada + fondo mutual + respaldo empresarial.
 *
 * <p>El principal es SIEMPRE el pozo completo (ambiguedad A6): lo que falte lo pone el respaldo;
 * si el respaldo no alcanza, el faltante queda como <b>deuda conservada</b> ({@code pendiente})
 * y la entrega no sale. Nunca se entrega menos «y despues vemos».
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

    public static Resultado calcular(RecaudoDelPozo.Recaudo recaudo, Dinero cubiertoEmpresa) {
        Dinero faltante = recaudo.faltante();
        Dinero cubierto = cubiertoEmpresa.esMayorQue(faltante) ? faltante : cubiertoEmpresa;
        return new Resultado(
                recaudo.pozo(),
                recaudo.confirmado(),
                recaudo.cubiertoMutual(),
                faltante,
                cubierto,
                faltante.menos(cubierto));
    }
}

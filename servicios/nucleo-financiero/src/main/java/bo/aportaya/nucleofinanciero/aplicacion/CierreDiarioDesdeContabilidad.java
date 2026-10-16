package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.aplicacion.CU51EjecutarCierreDiario.SalidaCierre;
import bo.aportaya.nucleofinanciero.dominio.puertos.RecaudoDelDia;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * CU-51 desde el backoffice (carril A · H8): Contabilidad pide cerrar un dia y las cifras las pone el
 * dueño del dato, no el operador.
 *
 * <p>Deliberadamente SIN {@code @Transactional}: la pregunta a {@code aportes} es de red y va antes y fuera
 * de la transaccion del cierre (invariante 6). Orden: dia terminado (AP-CU51-06) → recaudo de
 * {@code aportes} (AP-CU51-08 si no se sabe) → una sola moneda (AP-CU51-07, {@code cierre_diario} no
 * modela moneda) → el cierre, idempotente por fecha (el dia ya cerrado devuelve el cierre que hay).
 */
@Service
public class CierreDiarioDesdeContabilidad {

    private final RecaudoDelDia recaudo;
    private final CU51EjecutarCierreDiario cu51;
    private final Reloj reloj;

    public CierreDiarioDesdeContabilidad(RecaudoDelDia recaudo, CU51EjecutarCierreDiario cu51, Reloj reloj) {
        this.recaudo = recaudo;
        this.cu51 = cu51;
        this.reloj = reloj;
    }

    public SalidaCierre cerrar(LocalDate fecha, ContextoSesion ctx) {
        // El disparador del CU es el FIN del dia operativo: hoy todavia entra plata.
        if (!fecha.isBefore(reloj.hoy())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(51, 6), "El " + fecha + " todavia no termino: no se cierra un dia en curso.");
        }
        var resumen = recaudo.consultar(fecha)
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(51, 8), "aportes no informo el recaudo del dia: no se cierra a ciegas."));
        if (resumen.porMoneda().size() > 1) {
            throw new ErrorDeNegocio(
                    CodigoError.de(51, 7),
                    "El " + fecha + " tiene recaudo en mas de una moneda y el cierre diario no modela moneda.");
        }
        var totales = resumen.porMoneda().stream().findFirst();
        return cu51.ejecutarConRecaudo(
                fecha,
                totales.map(RecaudoDelDia.PorMoneda::recaudado).orElse(BigDecimal.ZERO),
                totales.map(RecaudoDelDia.PorMoneda::conciliado).orElse(BigDecimal.ZERO),
                totales.map(RecaudoDelDia.PorMoneda::excepciones).orElse(BigDecimal.ZERO),
                totales.map(RecaudoDelDia.PorMoneda::cantidadPagos).orElse(0),
                resumen.excepcionesAbiertas(),
                ctx);
    }
}

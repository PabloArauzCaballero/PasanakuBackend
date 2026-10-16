package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino.EntradaPagoATercero;
import bo.aportaya.nucleofinanciero.aplicacion.CU13EjecutarConDestino.SalidaEjecucion;
import bo.aportaya.nucleofinanciero.dominio.puertos.OfertasDelMercado;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Pagar al vendedor de una oferta con la retencion del comprador, SIN que quien llama dicte a quien.
 *
 * <p>Tres pasos y la red va en el del medio, <b>fuera de toda transaccion</b> (invariante 6): (1) leer
 * a que oferta apunta la retencion, (2) preguntarle a {@code entregas} quien vende y a que precio, (3)
 * ejecutar. Si {@code entregas} no responde o la oferta no esta en un estado pagable, no se paga.
 * Un reintento de algo ya pagado devuelve el comprobante sin volver a preguntar.
 */
@Service
public class PagoDeCompraEnMercado {

    /** Estados de la cesion en que todavia se debe pagar; una oferta cancelada o publicada no se paga. */
    private static final Set<String> PAGABLES = Set.of("RESERVADA", "LIQUIDANDO");

    private final CU13EjecutarConDestino ejecuciones;
    private final OfertasDelMercado ofertas;

    public PagoDeCompraEnMercado(CU13EjecutarConDestino ejecuciones, OfertasDelMercado ofertas) {
        this.ejecuciones = ejecuciones;
        this.ofertas = ofertas;
    }

    public SalidaEjecucion ejecutar(UUID retencionId, String clave, ContextoSesion ctx) {
        var preparacion = ejecuciones.preparar(retencionId, clave, ctx);
        if (preparacion.yaEjecutada().isPresent()) {
            return preparacion.yaEjecutada().get();
        }
        var oferta = ofertas.oferta(preparacion.ofertaId())
                .filter(o -> PAGABLES.contains(o.estado()))
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(13, 8), "No se pudo confirmar la oferta con entregas: no se paga."));
        return ejecuciones.pagarAlTercero(
                new EntradaPagoATercero(
                        clave,
                        retencionId,
                        oferta.ofertaId(),
                        oferta.vendedorUsuarioId(),
                        oferta.precio(),
                        oferta.cargos()),
                ctx);
    }
}

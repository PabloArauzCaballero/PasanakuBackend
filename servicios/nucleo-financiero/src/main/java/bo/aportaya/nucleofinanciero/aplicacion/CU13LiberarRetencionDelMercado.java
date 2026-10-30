package bo.aportaya.nucleofinanciero.aplicacion;

import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.SERVICIO_ENTREGAS;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.comoOperacion;
import static bo.aportaya.nucleofinanciero.aplicacion.ReglasDeEjecucion.exigirServicio;

import bo.aportaya.nucleofinanciero.dominio.VigenciaDeRetencion.Motivo;
import bo.aportaya.nucleofinanciero.infraestructura.RetencionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-13 · {@code entregas} devuelve el saldo de una puja superada o de una compra que no se completo.
 *
 * <p>La retencion es del postor, pero quien la libera no es el: lo superan en la sesion de OTRA persona, o la
 * subasta cierra en un trabajo programado sin sesion. Por eso existe esta variante, solo para {@code
 * SERVICIO_ENTREGAS} y solo para retenciones {@code COMPRA_EN_MERCADO}: liberar devuelve la plata a su dueno, nunca
 * la mueve a otro. Una retencion ya cerrada responde AP-CU13-03 (concurrencia sana, igual que el cierre del titular).
 */
@Service
public class CU13LiberarRetencionDelMercado {

    private final Datos datos;
    private final RetencionRepositorio retenciones;
    private final CU13RetenerSaldo cu13;

    public CU13LiberarRetencionDelMercado(Datos datos, RetencionRepositorio retenciones, CU13RetenerSaldo cu13) {
        this.datos = datos;
        this.retenciones = retenciones;
        this.cu13 = cu13;
    }

    @Transactional
    public CU13RetenerSaldo.SalidaCierre liberar(UUID retencionId, ContextoSesion llamador) {
        exigirServicio(llamador, SERVICIO_ENTREGAS, CodigoError.de(13, 9));
        ContextoSesion ctx = comoOperacion(llamador);
        return datos.conContexto(ctx, dsl -> {
            var retencion = retenciones
                    .ver(dsl, retencionId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(13, 3), "Esa retencion no existe."));
            if (!Motivo.COMPRA_EN_MERCADO.name().equals(retencion.motivo())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(13, 8), "Solo se liberan asi las retenciones de una compra en el mercado.");
            }
            return cu13.liberarDentroDe(dsl, retencionId, ctx);
        });
    }
}

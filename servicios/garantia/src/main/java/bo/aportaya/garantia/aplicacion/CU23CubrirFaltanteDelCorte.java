package bo.aportaya.garantia.aplicacion;

import bo.aportaya.garantia.dominio.puertos.RecaudoDelPeriodo;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CU-23 (respaldo empresarial) · Cubrir el faltante del corte, con el faltante afirmado por {@code aportes}.
 *
 * <p>Es la unica puerta HTTP hacia {@link CU23CubrirConRespaldo}. El pedido trae solo
 * <b>identificadores</b>: de donde salen el pozo, lo confirmado y las obligaciones que faltan
 * es del recaudo que devuelve {@code aportes}. Un monto que el cliente quiera «confirmar»
 * (faltanteEsperado) solo sirve para detectar que se desactualizo: si no coincide con el del
 * servidor, se rechaza y no se cubre nada.
 *
 * <p>La pregunta a {@code aportes} va FUERA de la transaccion (invariante 6); por eso esta clase
 * no abre ninguna: la abre {@code cubrir}. Si {@code aportes} no responde se rechaza con
 * AP-CU23-12 y no se escribe nada: el reintento es seguro porque la cobertura es idempotente.
 */
@Service
public class CU23CubrirFaltanteDelCorte {

    private final RecaudoDelPeriodo recaudos;
    private final CU23CubrirConRespaldo cobertura;

    public CU23CubrirFaltanteDelCorte(RecaudoDelPeriodo recaudos, CU23CubrirConRespaldo cobertura) {
        this.recaudos = recaudos;
        this.cobertura = cobertura;
    }

    /** @param faltanteEsperado lo que el llamador cree que falta; opcional, solo para detectar desactualizacion */
    public record Pedido(
            UUID grupoId, UUID periodoId, UUID turnoId, Dinero faltanteEsperado, ClaveIdempotencia clave) {}

    public CU23CubrirConRespaldo.SalidaCobertura cubrir(Pedido pedido, ContextoSesion ctx) {
        var recaudo = recaudos.consultar(pedido.periodoId())
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(23, 12),
                        "No se pudo confirmar el recaudo del periodo con aportes: no se cubre nada."));

        if (!pedido.periodoId().equals(recaudo.periodoId()) || !pedido.grupoId().equals(recaudo.grupoId())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(23, 13), "Ese periodo no pertenece a ese grupo: el pedido mezcla ambitos.");
        }
        Dinero faltante = recaudo.pozo().menos(recaudo.confirmado()).menos(recaudo.cubiertoMutual());
        if (pedido.faltanteEsperado() != null && !pedido.faltanteEsperado().equals(faltante)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(23, 13),
                    "El faltante informado no coincide con el que confirma aportes: no se cubre nada.",
                    Map.of("informado", pedido.faltanteEsperado().toString(), "confirmado", faltante.toString()));
        }
        List<CU23CubrirConRespaldo.Linea> lineas = recaudo.pendientes().stream()
                .map(p -> new CU23CubrirConRespaldo.Linea(p.obligacionId(), p.monto()))
                .toList();

        return cobertura.cubrir(
                new CU23CubrirConRespaldo.EntradaCobertura(
                        recaudo.grupoId(),
                        recaudo.periodoId(),
                        pedido.turnoId(),
                        recaudo.pozo(),
                        recaudo.confirmado(),
                        recaudo.cubiertoMutual(),
                        lineas,
                        recaudo.corteEn(),
                        pedido.clave()),
                ctx);
    }
}

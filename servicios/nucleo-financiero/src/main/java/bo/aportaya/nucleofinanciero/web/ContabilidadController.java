package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.ConsultarTesoreria;
import bo.aportaya.nucleofinanciero.web.generado.ContabilidadApi;
import bo.aportaya.nucleofinanciero.web.generado.modelo.CierreDiarioEnLista;
import bo.aportaya.nucleofinanciero.web.generado.modelo.PaginaDeCierresDiarios;
import bo.aportaya.nucleofinanciero.web.generado.modelo.PaginaDeReversos;
import bo.aportaya.nucleofinanciero.web.generado.modelo.ReversoEnLista;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pagina de {@code /contabilidad} de nucleo-financiero: cierres diarios (CU-51) y reversos (CU-14)
 * para Tesoreria, Contabilidad y Auditoria (backoffice carril A · H8). Solo lectura.
 *
 * <p>Los importes van como cadena SIN moneda: {@code cierre_diario} y {@code reverso_transaccion} no
 * la modelan (ambiguedad A-7 del carril). No se supone BOB.
 */
@RestController
public class ContabilidadController implements ContabilidadApi {

    private final ConsultarTesoreria tesoreria;
    private final SesionDeLaPeticion sesion;

    public ContabilidadController(ConsultarTesoreria tesoreria, SesionDeLaPeticion sesion) {
        this.tesoreria = tesoreria;
        this.sesion = sesion;
    }

    @Override
    @Permiso("AUDITORIA_LEER")
    public ResponseEntity<PaginaDeCierresDiarios> listarCierresDiarios(
            Integer limite, LocalDate antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-51", "listado");
        var pagina = tesoreria.cierres(
                Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDeCierresDiarios();
        salida.setItems(pagina.items().stream()
                .map(c -> {
                    var r = new CierreDiarioEnLista();
                    r.setCierreId(c.cierreId());
                    r.setFecha(c.fecha());
                    r.setTotalRecaudado(monto(c.totalRecaudado()));
                    r.setTotalConciliado(monto(c.totalConciliado()));
                    r.setTotalExcepciones(monto(c.totalExcepciones()));
                    r.setCantidadPagos(c.cantidadPagos());
                    r.setCuadrado(c.cuadrado());
                    r.setCerradoPor(c.cerradoPor());
                    r.setCerradoEn(c.cerradoEn());
                    r.setReabiertoEn(c.reabiertoEn().orElse(null));
                    return r;
                })
                .toList());
        salida.setProximoAntesDe(pagina.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(pagina.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    @Override
    @Permiso("AUDITORIA_LEER")
    public ResponseEntity<PaginaDeReversos> listarReversos(
            List<String> estados, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-14", "listado");
        var pagina = tesoreria.reversos(
                estados, Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDeReversos();
        salida.setItems(pagina.items().stream()
                .map(v -> {
                    var r = new ReversoEnLista();
                    r.setReversoId(v.reversoId());
                    r.setTransaccionOriginalId(v.transaccionOriginalId());
                    r.setTransaccionReversoId(v.transaccionReversoId().orElse(null));
                    r.setAutorizadaPor(v.autorizadaPor());
                    r.setTipo(ReversoEnLista.TipoEnum.fromValue(v.tipo()));
                    r.setMotivo(v.motivo());
                    r.setMontoReversado(monto(v.montoReversado()));
                    r.setEstado(ReversoEnLista.EstadoEnum.fromValue(v.estado()));
                    r.setSolicitadaEn(v.solicitadaEn());
                    r.setEjecutadaEn(v.ejecutadaEn().orElse(null));
                    return r;
                })
                .toList());
        salida.setProximoAntesDe(pagina.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(pagina.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    private static String monto(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}

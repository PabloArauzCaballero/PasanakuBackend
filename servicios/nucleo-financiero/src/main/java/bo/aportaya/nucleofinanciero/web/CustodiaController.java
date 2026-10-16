package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.nucleofinanciero.aplicacion.CU50ConciliarCustodia;
import bo.aportaya.nucleofinanciero.aplicacion.ConsultarTesoreria;
import bo.aportaya.nucleofinanciero.web.generado.CustodiaApi;
import bo.aportaya.nucleofinanciero.web.generado.modelo.ConciliacionEnLista;
import bo.aportaya.nucleofinanciero.web.generado.modelo.EntradaConciliacionCustodia;
import bo.aportaya.nucleofinanciero.web.generado.modelo.PaginaDeConciliaciones;
import bo.aportaya.nucleofinanciero.web.generado.modelo.SalidaConciliacionCustodia;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pagina de {@code /custodia}: conciliar la custodia y ver las conciliaciones (CU-50, backoffice
 * carril A · H8). Traduce y delega; el saldo emitido lo calcula el caso de uso, nunca el cliente.
 */
@RestController
public class CustodiaController implements CustodiaApi {

    private final ConsultarTesoreria tesoreria;
    private final CU50ConciliarCustodia cu50;
    private final SesionDeLaPeticion sesion;

    public CustodiaController(ConsultarTesoreria tesoreria, CU50ConciliarCustodia cu50, SesionDeLaPeticion sesion) {
        this.tesoreria = tesoreria;
        this.cu50 = cu50;
        this.sesion = sesion;
    }

    @Override
    @Permiso("AUDITORIA_LEER")
    public ResponseEntity<PaginaDeConciliaciones> listarConciliaciones(
            Integer limite, LocalDate antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-50", "listado");
        var pagina = tesoreria.conciliaciones(
                Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDeConciliaciones();
        salida.setItems(pagina.items().stream().map(CustodiaController::fila).toList());
        salida.setProximoAntesDe(pagina.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(pagina.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    // Tesoreria por rol, como acreditarRecarga: el token lleva el codigo de cada rol vigente.
    @Override
    @Permiso("TESORERIA")
    public ResponseEntity<SalidaConciliacionCustodia> conciliarCustodia(
            UUID idempotencyKey, EntradaConciliacionCustodia cuerpo) {
        Traza.marcarCasoDeUso("CU-50", cuerpo.getCuentaCustodiaId().toString());
        var salida = cu50.conciliarDesdeTesoreria(
                cuerpo.getCuentaCustodiaId(),
                cuerpo.getFecha(),
                MapeoDeBilletera.dinero(cuerpo.getSaldoCustodia()),
                MapeoDeBilletera.dinero(cuerpo.getSaldoEnTransito()),
                sesion.actual());
        // El saldo emitido de la respuesta es el que calculo el servidor:
        // diferencia = custodia + transito - emitido  =>  emitido = custodia + transito - diferencia.
        var respuesta = new SalidaConciliacionCustodia();
        respuesta.setConciliacionId(salida.conciliacionId());
        respuesta.setRatioCobertura(salida.ratioCobertura().toPlainString());
        respuesta.setCumpleEncaje(salida.cumpleEncaje());
        respuesta.setDiferencia(MapeoDeBilletera.dinero(salida.diferencia()));
        respuesta.setSaldoEmitido(MapeoDeBilletera.dinero(MapeoDeBilletera.dinero(cuerpo.getSaldoCustodia())
                .mas(MapeoDeBilletera.dinero(cuerpo.getSaldoEnTransito()))
                .menos(salida.diferencia())));
        respuesta.setEstado(SalidaConciliacionCustodia.EstadoEnum.fromValue(salida.estado()));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    private static ConciliacionEnLista fila(ConsultarTesoreria.Conciliacion c) {
        var r = new ConciliacionEnLista();
        r.setConciliacionId(c.conciliacionId());
        r.setCuentaCustodiaId(c.cuentaCustodiaId());
        r.setFecha(c.fecha());
        r.setSaldoEmitido(MapeoDeBilletera.dinero(c.saldoEmitido()));
        r.setSaldoCustodia(MapeoDeBilletera.dinero(c.saldoCustodia()));
        r.setSaldoEnTransito(MapeoDeBilletera.dinero(c.saldoEnTransito()));
        r.setDiferencia(MapeoDeBilletera.dinero(c.diferencia()));
        r.setRatioCobertura(c.ratioCobertura().setScale(6).toPlainString());
        r.setCumpleEncaje(c.cumpleEncaje());
        r.setEstado(ConciliacionEnLista.EstadoEnum.fromValue(c.estado()));
        r.setEjecutadaPor(c.ejecutadaPor().orElse(null));
        r.setEjecutadaEn(c.ejecutadaEn());
        r.setDescuadresAbiertos(c.descuadresAbiertos());
        return r;
    }
}

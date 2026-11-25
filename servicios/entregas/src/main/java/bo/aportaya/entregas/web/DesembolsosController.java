package bo.aportaya.entregas.web;

import bo.aportaya.entregas.aplicacion.CU28EmitirDesembolso;
import bo.aportaya.entregas.aplicacion.ConsultarDesembolsos;
import bo.aportaya.entregas.dominio.puertos.MedidaDeCuenta;
import bo.aportaya.entregas.web.generado.DesembolsosApi;
import bo.aportaya.entregas.web.generado.modelo.DesembolsoEnLista;
import bo.aportaya.entregas.web.generado.modelo.EntradaOrdenDesembolso;
import bo.aportaya.entregas.web.generado.modelo.EntradaRespuestaDesembolso;
import bo.aportaya.entregas.web.generado.modelo.PaginaDeDesembolsos;
import bo.aportaya.entregas.web.generado.modelo.SalidaIntentoDesembolso;
import bo.aportaya.entregas.web.generado.modelo.SalidaOrdenDesembolso;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.OperacionRetirada;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de {@code /desembolsos}: la orden y la respuesta del proveedor.
 *
 * <p>La respuesta se anota como un intento mas y nunca sobreescribe al anterior: lo que
 * un proveedor contesto —y cuando— es lo unico que permite reclamarle despues.
 */
@RestController
public class DesembolsosController implements DesembolsosApi {

    private final CU28EmitirDesembolso cu28;
    private final ConsultarDesembolsos consultas;
    private final SesionDeLaPeticion sesion;
    private final MedidaDeCuenta medidaDeCuenta;

    public DesembolsosController(
            CU28EmitirDesembolso cu28,
            ConsultarDesembolsos consultas,
            SesionDeLaPeticion sesion,
            MedidaDeCuenta medidaDeCuenta) {
        this.cu28 = cu28;
        this.consultas = consultas;
        this.sesion = sesion;
        this.medidaDeCuenta = medidaDeCuenta;
    }

    // Tesoreria la opera (ENTREGA_EJECUTAR) y Auditoria la lee: ninguno de los dos mueve plata desde aca.
    @Override
    @Permiso("ENTREGA_EJECUTAR")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<PaginaDeDesembolsos> listarDesembolsos(
            List<String> estados, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        Traza.marcarCasoDeUso("CU-28", "bandeja");
        var pagina = consultas.listar(
                estados, Optional.ofNullable(antesDe), Optional.ofNullable(antesDeId), limite, sesion.actual());
        var salida = new PaginaDeDesembolsos();
        salida.setItems(pagina.items().stream()
                .map(d -> {
                    var r = new DesembolsoEnLista();
                    r.setOrdenId(d.ordenId());
                    r.setEntregaId(d.entregaId());
                    r.setProveedorId(d.proveedorId());
                    r.setMonto(MapeoDeEntregas.dinero(d.monto()));
                    r.setEstado(DesembolsoEnLista.EstadoEnum.fromValue(d.estado()));
                    r.setIntentos(d.intentos());
                    r.setUltimoResultado(d.ultimoResultado()
                            .map(DesembolsoEnLista.UltimoResultadoEnum::fromValue)
                            .orElse(null));
                    r.setUltimoCodigoError(d.ultimoCodigoError().orElse(null));
                    r.setReintentableEn(d.reintentableEn().orElse(null));
                    r.setCreadaEn(d.creadaEn());
                    r.setAcreditadaEn(d.acreditadaEn().orElse(null));
                    return r;
                })
                .toList());
        salida.setProximoAntesDe(pagina.proximoAntesDe().orElse(null));
        salida.setProximoAntesDeId(pagina.proximoAntesDeId().orElse(null));
        return ResponseEntity.ok(salida);
    }

    @Override
    @Permiso("ENTREGA_EJECUTAR")
    public ResponseEntity<SalidaOrdenDesembolso> emitirOrdenDesembolso(
            UUID idempotencyKey, EntradaOrdenDesembolso cuerpo) {
        Traza.marcarCasoDeUso("CU-28", cuerpo.getEntregaId().toString());

        var ctx = sesion.actual();
        // CU-134 (D-11): se pregunta a identidad FUERA de la transaccion de emitir (invariante 6).
        boolean retenida = cu28.titularDeLaCuenta(cuerpo.getCuentaDestinoId(), ctx)
                .map(medidaDeCuenta::retieneEntrega)
                .orElse(false);
        var salida = cu28.emitir(
                new CU28EmitirDesembolso.EntradaOrden(
                        cuerpo.getEntregaId(),
                        cuerpo.getProveedorId(),
                        cuerpo.getCuentaDestinoId(),
                        cuerpo.getGlosa(),
                        idempotencyKey.toString(),
                        Boolean.TRUE.equals(cuerpo.getSaldoRetenido()),
                        retenida),
                ctx);

        var respuesta = new SalidaOrdenDesembolso();
        respuesta.setOrdenId(salida.ordenId());
        respuesta.setEstado(SalidaOrdenDesembolso.EstadoEnum.fromValue(salida.estado()));
        respuesta.setClaveIdempotencia(salida.claveIdempotencia());
        respuesta.setIntentos(salida.intentos());
        respuesta.setEsNueva(salida.esNueva());

        var estado = salida.esNueva() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(respuesta);
    }

    /**
     * F1.L2.M05 (SEG-005): una persona ya no puede anotar la respuesta del proveedor. Con {@code exitoso=true} marcaba
     * ACREDITADA y publicaba el asiento sin firma de nadie. La unica via es el webhook firmado del proveedor (F3.L2.M02,
     * que reutiliza {@code CU28EmitirDesembolso.anotarRespuesta}); mientras no exista, la orden queda en el proveedor
     * (P-14: no se mueve dinero real). Responde 410 hasta que el backoffice suelte la operacion (regla 98.1.3).
     */
    @Deprecated
    @Override
    @Permiso("ENTREGA_EJECUTAR")
    public ResponseEntity<SalidaIntentoDesembolso> anotarRespuestaDesembolso(
            UUID ordenId, EntradaRespuestaDesembolso cuerpo) {
        throw new OperacionRetirada(
                CodigoError.de(28, 8),
                "La respuesta del proveedor ya no se anota a mano: la confirma el proveedor con su aviso firmado.");
    }
}

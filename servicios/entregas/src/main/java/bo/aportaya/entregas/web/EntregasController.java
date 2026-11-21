package bo.aportaya.entregas.web;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.aplicacion.CU22LiquidarEntrega;
import bo.aportaya.entregas.dominio.LiquidacionDeEntrega;
import bo.aportaya.entregas.web.generado.EntregasApi;
import bo.aportaya.entregas.web.generado.modelo.EjecutarEntregaRequest;
import bo.aportaya.entregas.web.generado.modelo.EntradaFondeoDelPozo;
import bo.aportaya.entregas.web.generado.modelo.EntradaLiquidacion;
import bo.aportaya.entregas.web.generado.modelo.EntradaOferta;
import bo.aportaya.entregas.web.generado.modelo.EntradaPuja;
import bo.aportaya.entregas.web.generado.modelo.EntradaSubasta;
import bo.aportaya.entregas.web.generado.modelo.OfertaParaPago;
import bo.aportaya.entregas.web.generado.modelo.PaginaDeOfertas;
import bo.aportaya.entregas.web.generado.modelo.PaginaDeSubastas;
import bo.aportaya.entregas.web.generado.modelo.SalidaAutorizacion;
import bo.aportaya.entregas.web.generado.modelo.SalidaCancelacionDeOferta;
import bo.aportaya.entregas.web.generado.modelo.SalidaCompra;
import bo.aportaya.entregas.web.generado.modelo.SalidaEjecucionEntrega;
import bo.aportaya.entregas.web.generado.modelo.SalidaFondeoDelPozo;
import bo.aportaya.entregas.web.generado.modelo.SalidaLiquidacion;
import bo.aportaya.entregas.web.generado.modelo.SalidaOferta;
import bo.aportaya.entregas.web.generado.modelo.SalidaPuja;
import bo.aportaya.entregas.web.generado.modelo.VistaDeSubasta;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de {@code /entregas}: liquidar, autorizar y ejecutar.
 *
 * <p>Los tres pasos son tres operaciones y no una a proposito: **quien autoriza no
 * ejecuta** (R-SEG-04), y eso solo se puede sostener si son actos separados, cada uno
 * con su sesion y su permiso.
 */
@RestController
public class EntregasController implements EntregasApi {

    private final CU22LiquidarEntrega cu22;
    private final CU22EntregarPozoCompleto pozo;
    private final SesionDeLaPeticion sesion;

    public EntregasController(CU22LiquidarEntrega cu22, CU22EntregarPozoCompleto pozo, SesionDeLaPeticion sesion) {
        this.cu22 = cu22;
        this.pozo = pozo;
        this.sesion = sesion;
    }

    @Override
    @Permiso("ENTREGA_EJECUTAR")
    public ResponseEntity<SalidaLiquidacion> liquidarEntrega(UUID idempotencyKey, EntradaLiquidacion cuerpo) {
        Traza.marcarCasoDeUso("CU-22", cuerpo.getTurnoId().toString());

        var salida = cu22.liquidar(
                new CU22LiquidarEntrega.EntradaLiquidacion(
                        cuerpo.getGrupoId(),
                        cuerpo.getPeriodoId(),
                        cuerpo.getTurnoId(),
                        cuerpo.getCupoId(),
                        cuerpo.getBeneficiarioId(),
                        MapeoDeEntregas.dinero(cuerpo.getBruto()),
                        MapeoDeEntregas.dinero(cuerpo.getRecaudado()),
                        cuerpo.getDeducciones().stream()
                                .map(d -> new LiquidacionDeEntrega.Deduccion(
                                        d.getTipo().getValue(),
                                        d.getDescripcion(),
                                        MapeoDeEntregas.dinero(d.getMonto()),
                                        d.getReferenciaOrigenId(),
                                        Boolean.TRUE.equals(d.getEsObligatoria())))
                                .toList(),
                        cuerpo.getMetodoDesembolso().getValue(),
                        cuerpo.getFechaProgramada()),
                sesion.actual());

        var respuesta = new SalidaLiquidacion();
        respuesta.setEntregaId(salida.entregaId());
        respuesta.setBruto(MapeoDeEntregas.dinero(salida.bruto()));
        respuesta.setTotalDeducciones(MapeoDeEntregas.dinero(salida.totalDeducciones()));
        respuesta.setNeto(MapeoDeEntregas.dinero(salida.neto()));
        respuesta.setEstado(salida.estado());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Override
    @Permiso("ENTREGA_AUTORIZAR")
    public ResponseEntity<SalidaAutorizacion> autorizarEntrega(UUID entregaId) {
        Traza.marcarCasoDeUso("CU-22", entregaId.toString());

        var salida = cu22.autorizar(entregaId, sesion.actual());

        var respuesta = new SalidaAutorizacion();
        respuesta.setEntregaId(salida.entregaId());
        respuesta.setEstado(SalidaAutorizacion.EstadoEnum.fromValue(salida.estado()));
        respuesta.setAutorizadaPor(salida.autorizadaPor());
        return ResponseEntity.ok(respuesta);
    }

    @Override
    @Permiso("ENTREGA_EJECUTAR")
    public ResponseEntity<SalidaEjecucionEntrega> ejecutarEntrega(UUID entregaId, EjecutarEntregaRequest cuerpo) {
        Traza.marcarCasoDeUso("CU-22", entregaId.toString());

        var salida = cu22.ejecutar(entregaId, MapeoDeEntregas.dinero(cuerpo.getMontoEntregado()), sesion.actual());

        var respuesta = new SalidaEjecucionEntrega();
        respuesta.setEntregaId(salida.entregaId());
        respuesta.setEstado(SalidaEjecucionEntrega.EstadoEnum.fromValue(salida.estado()));
        respuesta.setMontoEntregado(MapeoDeEntregas.dinero(salida.montoEntregado()));
        return ResponseEntity.ok(respuesta);
    }

    /** Recibir mesa es recibir el pozo completo: el importe lo afirman aportes y garantia, no el cliente. */
    @Override
    @Permiso("ENTREGA_EJECUTAR")
    public ResponseEntity<SalidaFondeoDelPozo> fondearPozo(UUID idempotencyKey, EntradaFondeoDelPozo cuerpo) {
        Traza.marcarCasoDeUso("CU-22", cuerpo.getTurnoId().toString());

        var salida = pozo.fondear(
                new CU22EntregarPozoCompleto.Entrada(
                        cuerpo.getGrupoId(),
                        cuerpo.getPeriodoId(),
                        cuerpo.getTurnoId(),
                        cuerpo.getCupoId(),
                        cuerpo.getBeneficiarioId(),
                        cuerpo.getMetodoDesembolso().getValue(),
                        cuerpo.getFechaProgramada(),
                        ClaveIdempotencia.deHecho("fondeo", idempotencyKey).valor()),
                sesion.actual());

        var c = salida.cifras();
        var respuesta = new SalidaFondeoDelPozo();
        respuesta.setEntregaId(salida.entregaId());
        respuesta.setFondeoId(salida.fondeoId());
        respuesta.setEstado(SalidaFondeoDelPozo.EstadoEnum.fromValue(salida.estadoDelFondeo()));
        respuesta.setPozo(MapeoDeEntregas.dinero(c.pozo()));
        respuesta.setConfirmado(MapeoDeEntregas.dinero(c.confirmado()));
        respuesta.setCubiertoMutual(MapeoDeEntregas.dinero(c.cubiertoMutual()));
        respuesta.setCubiertoEmpresa(MapeoDeEntregas.dinero(c.cubiertoEmpresa()));
        respuesta.setPendiente(MapeoDeEntregas.dinero(c.pendiente()));
        respuesta.setEsNuevo(salida.esNuevo());
        return ResponseEntity.status(salida.esNuevo() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(respuesta);
    }

    // --- Cesion de turno retirada (D-L2-1, DECISIONES tanda 7: «subasta y cesion de turno: se elimina») ---
    // El orden lo fijan el nivel de puntualidad y el sorteo; un turno no se vende ni se subasta. Las operaciones siguen
    // publicadas (deprecated) solo hasta que la app y nucleo-financiero las suelten (regla 98.1.3): responden 410 sin
    // efecto. El permiso se sigue exigiendo para no contarle a un anonimo que existieron.

    @Deprecated
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaOferta> publicarOferta(UUID idempotencyKey, EntradaOferta cuerpo) {
        throw mercadoRetirado();
    }

    @Deprecated
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<PaginaDeOfertas> listarOfertas(Integer limite, Integer pagina) {
        throw mercadoRetirado();
    }

    /**
     * El hecho que `nucleo-financiero` pide antes de pagarle al vendedor (pacto `nucleo-financiero-entregas.json`). Sin
     * mercado ya no hay oferta que pagar: responde lo que el pacto define para una oferta inexistente, y nucleo no
     * paga (falla cerrado). Se retira cuando nucleo retire su consumidor.
     */
    @Override
    @Permiso("SERVICIO_ENTREGAS")
    public ResponseEntity<OfertaParaPago> consultarOfertaParaPago(UUID ofertaId) {
        throw new ErrorDeNegocio(CodigoError.de(130, 5), "Esa oferta no esta disponible.");
    }

    @Deprecated
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaCancelacionDeOferta> cancelarOferta(UUID ofertaId) {
        throw mercadoRetirado();
    }

    @Deprecated
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaCompra> comprarOferta(UUID ofertaId, UUID idempotencyKey) {
        throw mercadoRetirado();
    }

    @Deprecated
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<VistaDeSubasta> abrirSubastaDeCupo(UUID idempotencyKey, EntradaSubasta cuerpo) {
        throw subastaRetirada();
    }

    @Deprecated
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<PaginaDeSubastas> listarSubastasDeCupo(UUID cupoId, Integer limite, Integer pagina) {
        throw subastaRetirada();
    }

    @Deprecated
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<VistaDeSubasta> consultarSubastaDeCupo(UUID subastaId) {
        throw subastaRetirada();
    }

    @Deprecated
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaPuja> pujarEnSubasta(UUID subastaId, UUID idempotencyKey, EntradaPuja cuerpo) {
        throw subastaRetirada();
    }

    @Deprecated
    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<VistaDeSubasta> cancelarSubastaDeCupo(UUID subastaId) {
        throw subastaRetirada();
    }

    private static OperacionRetirada mercadoRetirado() {
        return new OperacionRetirada(
                CodigoError.de(130, 10),
                "La venta de turnos ya no existe: el orden lo fijan la puntualidad y el sorteo.");
    }

    private static OperacionRetirada subastaRetirada() {
        return new OperacionRetirada(
                CodigoError.de(136, 9),
                "La subasta de cupos ya no existe: el orden lo fijan la puntualidad y el sorteo.");
    }
}

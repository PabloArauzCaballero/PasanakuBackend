package bo.aportaya.aportes.web;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte;
import bo.aportaya.aportes.aplicacion.CU21PagarConSaldo;
import bo.aportaya.aportes.aplicacion.HechosDeGrupos;
import bo.aportaya.aportes.web.generado.AportesApi;
import bo.aportaya.aportes.web.generado.modelo.EntradaCobro;
import bo.aportaya.aportes.web.generado.modelo.SalidaCobro;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * CU-21, el cobro de la cuota (F1.L1.M11). Nada del cobro sale del cuerpo: el monto es el pendiente que lee el
 * servidor, el canal es el saldo de la billetera (P-06) y la referencia la fija el servidor. {@code cobrarAporte} se
 * retira (expandir y contraer, regla 98.1.3): mientras la app no pase a {@code pagarCuotaConSaldo}, solo acepta
 * exactamente lo que el servidor habria fijado.
 */
abstract class RutasDelCobroDeAportes implements AportesApi {

    private final CU21CobrarAporte cu21;
    private final SesionDeLaPeticion sesion;
    private final HechosDeGrupos grupos;
    private final CU21PagarConSaldo conSaldo;

    protected RutasDelCobroDeAportes(
            CU21CobrarAporte cu21, SesionDeLaPeticion sesion, HechosDeGrupos grupos, CU21PagarConSaldo conSaldo) {
        this.cu21 = cu21;
        this.conSaldo = conSaldo;
        this.sesion = sesion;
        this.grupos = grupos;
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaCobro> pagarCuotaConSaldo(UUID idempotencyKey, UUID obligacionId, Object cuerpo) {
        Traza.marcarCasoDeUso("CU-21", obligacionId.toString());
        // El generador traduce el esquema sin campos a Object, y Object acepta cualquier JSON. La lista blanca
        // (regla 96.2.2) se aplica aca: solo un cuerpo ausente o {} pasa; lo demas es el mismo 400 AP-VAL-02
        // que da un campo no declarado en cualquier otro cuerpo estricto.
        if (cuerpo != null && !(cuerpo instanceof Map<?, ?> campos && campos.isEmpty())) {
            throw new ConstraintViolationException("El pago con saldo no lleva campos.", Set.of());
        }
        var ctx = sesion.actual();
        boolean periodoAbierto = admisible(obligacionId);
        return respuesta(
                conSaldo.pagarConSaldo(idempotencyKey.toString(), obligacionId, Optional.empty(), periodoAbierto, ctx));
    }

    // Contrato legado que la app movil publicada sigue usando: el OpenAPI lo marca obsoleto y la
    // implementacion lo declara igual, para que el retiro se vea en el codigo y no solo en el YAML.
    @Override
    @Deprecated
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<SalidaCobro> cobrarAporte(UUID idempotencyKey, UUID obligacionId, EntradaCobro cuerpo) {
        Traza.marcarCasoDeUso("CU-21", obligacionId.toString());
        // Lo que el servidor fija no lo dicta el cliente: se rechaza antes de mirar la obligacion.
        if (cuerpo.getCanal() != EntradaCobro.CanalEnum.BILLETERA_MOVIL
                || (cuerpo.getRecargo() != null
                        && !MapeoDeAportes.dinero(cuerpo.getRecargo()).esCero())
                || cuerpo.getProveedorId() != null
                || Boolean.TRUE.equals(cuerpo.getEsManual())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(21, 10),
                    "El canal, el recargo y el proveedor los fija el servidor: solo se paga con el saldo.");
        }
        var ctx = sesion.actual();
        boolean periodoAbierto = admisible(obligacionId);
        // El monto tiene que ser el pendiente (AP-CU21-02): el pago parcial ya no lo decide el cliente.
        return respuesta(conSaldo.pagarConSaldo(
                idempotencyKey.toString(),
                obligacionId,
                Optional.of(MapeoDeAportes.dinero(cuerpo.getMonto())),
                periodoAbierto,
                ctx));
    }

    /**
     * La obligacion tiene que ser de quien paga (AP-CU21-06); devuelve si el periodo sigue abierto. Se pregunta a
     * {@code grupos} FUERA de la transaccion del cobro (invariante 6).
     */
    private boolean admisible(UUID obligacionId) {
        var ctx = sesion.actual();
        var contexto = cu21.contextoDe(obligacionId, ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(21, 1), "Esa obligacion no existe."));
        var admisibilidad = grupos.admisibilidad(contexto.participanteId(), contexto.periodoId())
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(21, 7), "No pudimos verificar esa obligacion: intentalo de nuevo."));
        if (!admisibilidad.esDelUsuario()) {
            throw new ErrorDeNegocio(CodigoError.de(21, 6), "Esa obligacion no es tuya.");
        }
        return admisibilidad.periodoAbierto();
    }

    private static ResponseEntity<SalidaCobro> respuesta(CU21CobrarAporte.SalidaCobro salida) {
        var respuesta = new SalidaCobro();
        respuesta.setPagoId(salida.pagoId());
        respuesta.setObligacionId(salida.obligacionId());
        respuesta.setEstadoObligacion(SalidaCobro.EstadoObligacionEnum.fromValue(salida.estadoObligacion()));
        respuesta.setPendiente(MapeoDeAportes.dinero(salida.pendiente()));
        respuesta.setEsNuevo(salida.esNuevo());
        // 201 cuando el cobro se acredito ahora; 200 cuando el reintento devolvio el que ya existia.
        return ResponseEntity.status(salida.esNuevo() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(respuesta);
    }
}

package bo.aportaya.cumplimiento.aplicacion;

import bo.aportaya.cumplimiento.aplicacion.CU43RemitirReportes.EntradaEnvio;
import bo.aportaya.cumplimiento.aplicacion.CU43RemitirReportes.EntradaReporte;
import bo.aportaya.cumplimiento.aplicacion.CU43RemitirReportes.SalidaReporte;
import bo.aportaya.cumplimiento.aplicacion.CU45AtenderRequerimiento.EntradaRequerimiento;
import bo.aportaya.cumplimiento.aplicacion.CU45AtenderRequerimiento.EntradaRespuesta;
import bo.aportaya.cumplimiento.aplicacion.CU56EjecutarPruebaDeContinuidad.EntradaPrueba;
import bo.aportaya.cumplimiento.aplicacion.CU56EjecutarPruebaDeContinuidad.SalidaPrueba;
import bo.aportaya.cumplimiento.infraestructura.AccionesDeCumplimientoRepositorio;
import bo.aportaya.cumplimiento.infraestructura.ReporteRegulatorioRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las acciones de cumplimiento que el backoffice ejecuta por HTTP (carril A · H9): CU-43, CU-45 y CU-56.
 *
 * <p>Dos reglas que los casos de uso no traian y aca se imponen: (1) <b>el actor sale de la sesion</b> —
 * quien aprueba un reporte es quien lo pide, nunca un id del cuerpo—, y el plazo y el numero de un oficio
 * son los guardados, nunca los del cliente (un plazo falso evitaria el hallazgo por respuesta tardia);
 * (2) <b>el reintento es seguro por estado</b>: repetir lo mismo devuelve lo que ya hay ({@code esNuevo =
 * false}); algo distinto sobre lo mismo es el rechazo del CU.
 */
@Service
public class AccionesDeCumplimiento {

    private final Datos datos;
    private final CU43RemitirReportes cu43;
    private final CU45AtenderRequerimiento cu45;
    private final CU56EjecutarPruebaDeContinuidad cu56;
    private final ReporteRegulatorioRepositorio reportes;
    private final AccionesDeCumplimientoRepositorio guardado;

    public AccionesDeCumplimiento(
            Datos datos,
            CU43RemitirReportes cu43,
            CU45AtenderRequerimiento cu45,
            CU56EjecutarPruebaDeContinuidad cu56,
            ReporteRegulatorioRepositorio reportes,
            AccionesDeCumplimientoRepositorio guardado) {
        this.datos = datos;
        this.cu43 = cu43;
        this.cu45 = cu45;
        this.cu56 = cu56;
        this.reportes = reportes;
        this.guardado = guardado;
    }

    public record Resultado<T>(T salida, boolean esNuevo) {}

    /** CU-43 · generar ya es idempotente por (catalogo, periodo): el caso de uso devuelve el que hay. */
    @Transactional
    public Resultado<SalidaReporte> generarReporte(
            String periodo, String catalogoCodigo, String formulario, ContextoSesion ctx) {
        boolean existia = datos.conContexto(ctx, dsl -> reportes.catalogoPorCodigo(dsl, catalogoCodigo)
                .flatMap(c -> reportes.reporteDe(dsl, c.id(), periodo))
                .isPresent());
        var salida = cu43.generar(new EntradaReporte(periodo, catalogoCodigo, formulario), ctx);
        return new Resultado<>(salida, !existia);
    }

    /** CU-43 · aprobar y registrar el envio. El aprobador es la sesion; el reintento del mismo aprobador no duplica. */
    @Transactional
    public Resultado<SalidaReporte> aprobarYEnviar(
            String periodo,
            String catalogoCodigo,
            String canal,
            String numeroConstancia,
            boolean aceptado,
            int reintentos,
            ContextoSesion ctx) {
        var previo = datos.conContexto(ctx, dsl -> reportes.catalogoPorCodigo(dsl, catalogoCodigo)
                .flatMap(c -> reportes.reporteDe(dsl, c.id(), periodo)));
        if (previo.isPresent()
                && !"GENERADO".equals(previo.get().estado())
                && ctx.usuarioId().equals(previo.get().aprobadoPor())) {
            var r = previo.get();
            return new Resultado<>(
                    new SalidaReporte(r.id(), r.cantidadRegistros(), r.enCero(), null, r.hashArchivo(), null, false),
                    false);
        }
        var salida = cu43.aprobarYEnviar(
                new EntradaEnvio(
                        periodo, catalogoCodigo, ctx.usuarioId(), canal, numeroConstancia, aceptado, reintentos),
                ctx);
        return new Resultado<>(salida, true);
    }

    /** CU-45 · el mismo oficio con el mismo documento es un reintento; con otro documento, AP-CU45-01. */
    @Transactional
    public Resultado<UUID> registrarRequerimiento(EntradaRequerimiento entrada, ContextoSesion ctx) {
        var previo = datos.conContexto(ctx, dsl -> guardado.oficioPorNumero(dsl, entrada.numeroOficio()));
        if (previo.isPresent() && previo.get().hashDocumento().equals(entrada.hashDocumento())) {
            return new Resultado<>(previo.get().id(), false);
        }
        return new Resultado<>(cu45.registrar(entrada, ctx).requerimientoId(), true);
    }

    /** CU-45 · se responde con el plazo y el numero GUARDADOS; el mismo respondedor con la misma respuesta es reintento. */
    @Transactional
    public Resultado<UUID> responderRequerimiento(UUID requerimientoId, String respuestaUrl, ContextoSesion ctx) {
        var oficio = datos.conContexto(ctx, dsl -> guardado.oficio(dsl, requerimientoId))
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(45, 1), "Ese requerimiento no existe."));
        if ("RESPONDIDO".equals(oficio.estado())
                && ctx.usuarioId().equals(oficio.respondidoPor())
                && respuestaUrl.equals(oficio.respuestaUrl())) {
            return new Resultado<>(oficio.id(), false);
        }
        cu45.responder(
                new EntradaRespuesta(oficio.id(), oficio.numeroOficio(), oficio.plazoRespuesta(), respuestaUrl), ctx);
        return new Resultado<>(oficio.id(), true);
    }

    /** CU-56 · la misma prueba (plan, fecha, tipo, quien la ejecuto) registrada dos veces es una sola. */
    @Transactional
    public Resultado<SalidaPrueba> registrarPrueba(EntradaPrueba entrada, ContextoSesion ctx) {
        var previa = datos.conContexto(
                ctx,
                dsl -> guardado.prueba(
                        dsl, entrada.planContinuidadId(), entrada.fecha(), entrada.tipo(), entrada.ejecutadaPor()));
        if (previa.isPresent()) {
            var p = previa.get();
            return new Resultado<>(
                    new SalidaPrueba(p.id(), p.rtoMinutos(), p.rpoMinutos(), p.resultado(), null), false);
        }
        return new Resultado<>(cu56.registrar(entrada, ctx), true);
    }
}

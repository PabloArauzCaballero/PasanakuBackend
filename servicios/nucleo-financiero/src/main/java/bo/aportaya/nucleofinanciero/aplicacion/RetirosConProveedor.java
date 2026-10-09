package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.aplicacion.CU11InstruirRetiro.EnvioDeRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.RegistrarDiscrepancia.EntradaDiscrepancia;
import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor;
import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor.Tipo;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiros;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiros.Confirmacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * H4.S1.M4 · El pago de un retiro con resultado desconocido.
 *
 * <p>La regla que organiza todo: <b>un timeout despues de liquidar es indistinguible de uno
 * antes de liquidar</b>. Por eso nunca se reenvia a ciegas. Antes de cada envio se consulta
 * la referencia de la orden; despues de un envio que no obtuvo respuesta se consulta de
 * nuevo; y recien con lo que el proveedor firmo se mueve el libro: pagado (la retencion se
 * ejecuta y se escribe la salida), rechazado (la retencion se libera) o todavia en proceso
 * (no se toca nada, el dinero sigue retenido).
 *
 * <p>La referencia es la de la orden, asi que aun si dos despachos se cruzan el proveedor
 * liquida una sola vez. Las llamadas de red van fuera de toda transaccion.
 */
@Service
public class RetirosConProveedor {

    private static final String REFERENCIA = "ORDEN_RETIRO";
    private static final String MOTIVO_RECHAZO = "PROVEEDOR_RECHAZO";

    /** Como termino el intento: {@code EN_PROCESO} incluye el resultado desconocido. */
    public enum Desenlace {
        PAGADO,
        RECHAZADO,
        EN_PROCESO
    }

    public record ResultadoDeRetiro(Desenlace desenlace, UUID transaccionId) {}

    private final CU11InstruirRetiro instrucciones;
    private final CU11RetirarSaldo retiros;
    private final ProveedorDeRetiros proveedor;
    private final RegistrarDiscrepancia discrepancias;

    public RetirosConProveedor(
            CU11InstruirRetiro instrucciones,
            CU11RetirarSaldo retiros,
            ProveedorDeRetiros proveedor,
            RegistrarDiscrepancia discrepancias) {
        this.instrucciones = instrucciones;
        this.retiros = retiros;
        this.proveedor = proveedor;
        this.discrepancias = discrepancias;
    }

    /** Pone la orden en camino: consulta, envia solo si el proveedor no la conoce, y consulta de nuevo. */
    public ResultadoDeRetiro despachar(UUID ordenId, ContextoSesion ctx) {
        var envio = instrucciones.preparar(ordenId, ctx);
        if (esTerminal(envio)) {
            return resolver(ordenId, ctx);
        }
        Optional<Confirmacion> conocida = consultar(envio, ctx);
        if (conocida == null) {
            return enProceso();
        }
        if (conocida.isEmpty()) {
            try {
                proveedor.instruir(ordenId, envio.neto());
            } catch (DiscrepanciaDelProveedor d) {
                return discrepancia(envio, d.tipo(), d.huella(), Optional.empty(), d.getMessage(), ctx);
            } catch (ErrorDeNegocio sinRespuesta) {
                // Pudo haber liquidado: no se asume nada, se pregunta abajo.
            }
            conocida = consultar(envio, ctx);
            if (conocida == null || conocida.isEmpty()) {
                return enProceso();
            }
        }
        return aplicar(envio, conocida.get(), ctx);
    }

    /** Solo pregunta: lo que se hace despues de un resultado desconocido, o cuando llega el aviso. */
    public ResultadoDeRetiro resolver(UUID ordenId, ContextoSesion ctx) {
        var envio = instrucciones.preparar(ordenId, ctx);
        if ("PAGADA".equals(envio.estado()) || "RECHAZADA".equals(envio.estado())) {
            // Ya cerrada: igual se contrasta con el proveedor, para detectar si se contradice.
            var conocida = consultar(envio, ctx);
            if (conocida != null && conocida.isPresent()) {
                return aplicar(envio, conocida.get(), ctx);
            }
            return cerrada(envio);
        }
        var conocida = consultar(envio, ctx);
        return conocida == null || conocida.isEmpty() ? enProceso() : aplicar(envio, conocida.get(), ctx);
    }

    private ResultadoDeRetiro aplicar(EnvioDeRetiro envio, Confirmacion c, ContextoSesion ctx) {
        if (!envio.ordenId().equals(c.referencia())) {
            return discrepancia(
                    envio, Tipo.REFERENCIA_DISTINTA, huellaDe(c), Optional.empty(), "Respondio otra referencia.", ctx);
        }
        if (!envio.neto().equals(c.monto())) {
            return discrepancia(
                    envio,
                    Tipo.MONTO_DISTINTO,
                    huellaDe(c),
                    Optional.of(c.monto()),
                    "El proveedor informa un importe distinto del neto de la orden.",
                    ctx);
        }
        boolean pagada = "PAGADA".equals(envio.estado());
        boolean rechazada = "RECHAZADA".equals(envio.estado());
        return switch (c.estado()) {
            case "CONFIRMADO" -> {
                if (rechazada) {
                    yield contradice(envio, c, "El libro rechazo esta orden y el proveedor dice que la pago.", ctx);
                }
                yield pagada ? cerrada(envio) : pagar(envio, ctx);
            }
            case "RECHAZADO" -> {
                if (pagada) {
                    yield contradice(envio, c, "El libro pago esta orden y el proveedor dice que la rechazo.", ctx);
                }
                yield rechazada ? cerrada(envio) : rechazar(envio, ctx);
            }
            default -> {
                if (pagada || rechazada) {
                    yield contradice(
                            envio, c, "La orden esta cerrada en el libro y el proveedor la tiene pendiente.", ctx);
                }
                yield enProceso();
            }
        };
    }

    private ResultadoDeRetiro pagar(EnvioDeRetiro envio, ContextoSesion ctx) {
        try {
            var pago = retiros.confirmarPago(envio.ordenId(), ctx);
            return new ResultadoDeRetiro(Desenlace.PAGADO, pago.transaccionId());
        } catch (ErrorDeNegocio carrera) {
            // Otro despacho gano entre la lectura y el cierre: se informa lo que quedo escrito.
            var actual = instrucciones.ver(envio.ordenId(), ctx);
            if ("PAGADA".equals(actual.estado())) {
                return cerrada(actual);
            }
            throw carrera;
        }
    }

    private ResultadoDeRetiro rechazar(EnvioDeRetiro envio, ContextoSesion ctx) {
        try {
            retiros.rechazar(envio.ordenId(), MOTIVO_RECHAZO, ctx);
        } catch (ErrorDeNegocio carrera) {
            if (!"RECHAZADA".equals(instrucciones.ver(envio.ordenId(), ctx).estado())) {
                throw carrera;
            }
        }
        return new ResultadoDeRetiro(Desenlace.RECHAZADO, null);
    }

    /** {@code null} = no se pudo preguntar (resultado desconocido); vacio = el proveedor no conoce la referencia. */
    private Optional<Confirmacion> consultar(EnvioDeRetiro envio, ContextoSesion ctx) {
        try {
            return proveedor.consultar(envio.ordenId());
        } catch (DiscrepanciaDelProveedor d) {
            discrepancia(envio, d.tipo(), d.huella(), Optional.empty(), d.getMessage(), ctx);
            return null;
        } catch (ErrorDeNegocio sinRespuesta) {
            return null;
        }
    }

    private ResultadoDeRetiro contradice(EnvioDeRetiro envio, Confirmacion c, String detalle, ContextoSesion ctx) {
        return discrepancia(envio, Tipo.ESTADO_CONTRADICTORIO, huellaDe(c), Optional.empty(), detalle, ctx);
    }

    /** Registra la evidencia (sin tocar el libro) y rechaza la operacion. Siempre lanza. */
    private ResultadoDeRetiro discrepancia(
            EnvioDeRetiro envio,
            Tipo tipo,
            String huella,
            Optional<Dinero> informado,
            String detalle,
            ContextoSesion ctx) {
        discrepancias.registrar(
                new EntradaDiscrepancia(
                        REFERENCIA, envio.ordenId(), tipo, Optional.of(envio.neto()), informado, detalle, huella),
                ctx);
        throw new ErrorDeNegocio(
                CodigoError.de(11, 9),
                "El proveedor respondio algo que no coincide con la orden: queda para revision.");
    }

    private static boolean esTerminal(EnvioDeRetiro envio) {
        return "PAGADA".equals(envio.estado()) || "RECHAZADA".equals(envio.estado());
    }

    private static ResultadoDeRetiro cerrada(EnvioDeRetiro envio) {
        return "PAGADA".equals(envio.estado())
                ? new ResultadoDeRetiro(Desenlace.PAGADO, envio.transaccionId())
                : new ResultadoDeRetiro(Desenlace.RECHAZADO, null);
    }

    private static ResultadoDeRetiro enProceso() {
        return new ResultadoDeRetiro(Desenlace.EN_PROCESO, null);
    }

    private static String huellaDe(Confirmacion c) {
        String material = c.referencia() + "|" + c.transaccionProveedor() + "|" + c.monto() + "|" + c.estado();
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("Toda JVM trae SHA-256", imposible);
        }
    }
}

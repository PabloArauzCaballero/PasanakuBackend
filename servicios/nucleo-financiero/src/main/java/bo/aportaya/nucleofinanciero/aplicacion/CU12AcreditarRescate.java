package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio.Cuenta;
import bo.aportaya.nucleofinanciero.infraestructura.CuentasDeSistemaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera.Pata;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-12 · Acreditar un rescate de inversion: de la custodia de inversion a la billetera del titular.
 *
 * <p>Es una transferencia entre dos bolsillos del mismo sistema —la plata vuelve a casa— y por eso
 * vive junto a CU-12: el total del sistema no cambia. Lo que la distingue es QUIEN la puede pedir:
 * acreditar es confirmar que la plata volvio, y eso no lo hace el titular que la espera (91.3). La
 * ruta exige la identidad de servicio {@code SERVICIO_INVERSIONES}; sin esa barrera cualquier titular podria
 * acreditarse importes.
 *
 * <p>Idempotente por clave (R-BIL-06) y por hecho: un rescate se acredita una sola vez aunque llegue
 * con otra clave (R-BIL-24). La custodia no puede quedar en negativo: la base lo rechaza
 * ({@code ck_cuenta_saldo_no_negativo}) y aca se adelanta con {@code AP-CU12-01}.
 */
@Service
public class CU12AcreditarRescate {

    private static final String ORIGEN = "RESCATE_INVERSION";

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final CuentasDeSistemaRepositorio sistema;
    private final LibroDeBilletera libro;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU12AcreditarRescate(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            CuentasDeSistemaRepositorio sistema,
            LibroDeBilletera libro,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.sistema = sistema;
        this.libro = libro;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Transactional
    public SalidaRescate ejecutar(EntradaRescate e, ContextoSesion llamador) {
        // Solo el servicio de inversiones: acreditar es confirmar que la plata volvio (91.3), no algo que
        // pida el titular que la espera. La ruta lo exige y aca se exige de nuevo.
        ReglasDeEjecucion.exigirServicio(llamador, ReglasDeEjecucion.SERVICIO_INVERSIONES, CodigoError.de(12, 8));
        ContextoSesion ctx = ReglasDeEjecucion.comoOperacion(llamador);
        return datos.conContexto(ctx, dsl -> {
            Optional<UUID> quien = Optional.empty();
            libro.bloquearIdempotencia(dsl, quien, ORIGEN, e.claveIdempotencia());
            // El hecho tambien se serializa: dos pedidos del mismo rescate con claves distintas se turnan.
            libro.bloquearIdempotencia(dsl, Optional.empty(), ORIGEN, "origen:" + e.referenciaRescateId());
            Optional<UUID> previa = libro.porClaveIdempotencia(dsl, quien, ORIGEN, e.claveIdempotencia())
                    .or(() -> libro.porOrigen(dsl, ORIGEN, e.referenciaRescateId()));
            if (previa.isPresent()) {
                return repetida(dsl, previa.get(), e);
            }

            // El destino es la billetera de OTRA persona: se lee con el rol de sistema, sin exponer la fila.
            Cuenta destino = Datos.comoSistema(dsl, () -> cuentas.ver(dsl, e.cuentaBilleteraId()))
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(12, 2), "Esa billetera no existe."));
            if (!destino.operativa()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(12, 3), "La billetera de destino esta " + destino.estado() + ".");
            }
            if (destino.usuarioId() == null) {
                throw new ErrorDeNegocio(
                        CodigoError.de(12, 2), "Un rescate se acredita a la billetera de una persona.");
            }
            if (destino.moneda() != e.monto().moneda()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(12, 2),
                        "La billetera es en " + destino.moneda() + " y el rescate en "
                                + e.monto().moneda() + ".");
            }
            UUID custodia = sistema.custodiaDeInversion(dsl, destino.moneda())
                    .orElseThrow(() -> new ErrorDeNegocio(
                            CodigoError.de(12, 7),
                            "No hay una custodia de inversion activa en " + destino.moneda() + "."));

            // Bloqueo en orden de identificador: un rescate y un debito del mismo titular se turnan.
            List<UUID> orden = List.of(custodia, destino.id()).stream().sorted().toList();
            Datos.comoSistema(dsl, () -> {
                orden.forEach(id -> cuentas.bloquear(dsl, id));
                return null;
            });
            Cuenta enCustodia =
                    Datos.comoSistema(dsl, () -> cuentas.ver(dsl, custodia)).orElseThrow();
            if (e.monto().esMayorQue(enCustodia.disponible())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(12, 1),
                        "La custodia de inversion no cubre el rescate: quedan " + enCustodia.disponible() + ".");
            }

            UUID transaccionId = libro.registrar(
                    dsl,
                    ORIGEN,
                    ORIGEN,
                    e.referenciaRescateId(),
                    ctx.esSistema() ? "BATCH" : "API",
                    e.monto(),
                    e.claveIdempotencia(),
                    quien,
                    List.of(
                            Pata.debito(custodia, e.monto(), "Rescate de inversion"),
                            Pata.credito(destino.id(), e.monto(), "Rescate de inversion")),
                    reloj.ahora().atOffset(ZoneOffset.UTC));

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "nucleo_financiero.rescate_acreditado",
                            "transaccion_billetera",
                            transaccionId,
                            Map.of(
                                    "cuentaBilleteraId", destino.id().toString(),
                                    "rescateId", e.referenciaRescateId().toString(),
                                    "monto", e.monto().toString(),
                                    "moneda", e.monto().moneda().name(),
                                    "servicio", llamador.rol()),
                            UUID.fromString(ctx.traza().id())));
            return salida(dsl, transaccionId, destino.id(), false);
        });
    }

    /** Mismo rescate, mismo importe y misma billetera: devuelve el comprobante. Otra cosa: la clave es de otra operacion. */
    private SalidaRescate repetida(DSLContext dsl, UUID transaccionId, EntradaRescate e) {
        var previa = libro.resumen(dsl, transaccionId).orElseThrow();
        boolean mismo = previa.origenId().equals(e.referenciaRescateId())
                && previa.monto().equals(e.monto())
                && libro.tocaLaCuenta(dsl, transaccionId, e.cuentaBilleteraId(), "CREDITO");
        if (!mismo) {
            throw new ErrorDeNegocio(CodigoError.de(12, 6), "La clave de idempotencia ya identifica otro rescate.");
        }
        return salida(dsl, transaccionId, e.cuentaBilleteraId(), true);
    }

    private SalidaRescate salida(DSLContext dsl, UUID transaccionId, UUID cuentaId, boolean repetida) {
        Cuenta despues =
                Datos.comoSistema(dsl, () -> cuentas.ver(dsl, cuentaId)).orElseThrow();
        return new SalidaRescate(transaccionId, despues.disponible(), repetida);
    }

    public record EntradaRescate(
            String claveIdempotencia, UUID cuentaBilleteraId, Dinero monto, UUID referenciaRescateId) {}

    public record SalidaRescate(UUID transaccionId, Dinero saldoDespues, boolean repetida) {}
}

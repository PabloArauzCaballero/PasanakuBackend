package bo.aportaya.garantia.aplicacion;

import bo.aportaya.garantia.infraestructura.FondoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tableros · H1.S2.M1c — el fondo de garantia se entera de lo que entro a su billetera.
 *
 * <p>{@code nucleo-financiero} mueve el dinero del recargo por mora a la billetera de sistema del fondo y lo publica en
 * {@code aporte_aplicado} con {@code destino = FONDO_GARANTIA}. Aca se anota el ingreso en el fondo DEL GRUPO: un
 * {@code movimiento_fondo} {@code RECARGO_MORA} (append-only) y el saldo disponible. Sin esto el dinero estaria en la
 * billetera pero el fondo no lo podria usar para cubrir a nadie.
 *
 * <p>Idempotente por evento ({@code evento_consumido}): la entrega es al menos una vez. El recargo NO suma a
 * {@code total_aportado}: no es un aporte del participante y no se le devuelve al cerrar el fondo (CU-29 reparte solo
 * {@code APORTE_PERIODICO} y {@code CONSTITUCION}).
 *
 * <p>Un grupo sin fondo propio es un faltante, no un rechazo de negocio: se lanza sin marcar y el reproceso lo aplica
 * cuando el fondo exista (supuesto A6 del plan de tableros, mismo criterio que la apertura de billeteras).
 */
@Service
public class IngresoAlFondo {

    public static final String CONSUMIDOR = "garantia.aportes-aplicados";

    /** El identificador de proceso del actor Sistema; no es una persona ni una fila de usuario. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-0000000000f0");

    public enum Resultado {
        ANOTADO,
        DUPLICADO,
        NO_ES_DEL_FONDO
    }

    /** Lo que {@code nucleo_financiero.aporte_aplicado} trae, ya con tipos: el borde con Kafka valida y convierte. */
    public record AporteAplicado(
            UUID eventoId, UUID correlationId, UUID transaccionId, UUID grupoId, Dinero monto, String destino) {}

    private final Datos datos;
    private final FondoRepositorio fondos;
    private final Reloj reloj;
    private final Consumidos consumidos = new Consumidos("garantia");

    public IngresoAlFondo(Datos datos, FondoRepositorio fondos, Reloj reloj) {
        this.datos = datos;
        this.fondos = fondos;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado anotar(AporteAplicado e) {
        if (!"FONDO_GARANTIA".equals(e.destino())) {
            // Un aporte al pozo del grupo no es asunto del fondo: ni se marca ni se escribe nada.
            return Resultado.NO_ES_DEL_FONDO;
        }
        ContextoSesion ctx =
                ContextoSesion.deSistema(PROCESO, new Traza(e.correlationId().toString()));
        return datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, e.eventoId(), CONSUMIDOR)) {
                return Resultado.DUPLICADO;
            }
            UUID fondoId = fondos.delGrupo(dsl, e.grupoId())
                    .orElseThrow(() -> new IllegalStateException(
                            "El grupo " + e.grupoId() + " no tiene fondo de garantia: el ingreso queda pendiente."))
                    .id();
            var fondo = fondos.bloquear(dsl, fondoId).orElseThrow();
            if (fondo.moneda() != e.monto().moneda()) {
                throw new ErrorDeDominio("El ingreso viene en " + e.monto().moneda() + " y el fondo es en "
                        + fondo.moneda() + ": no se suma sin tipo de cambio (regla 91.1.2).");
            }
            Dinero saldoDespues = fondo.saldoDisponible().mas(e.monto());
            if (!fondos.moverSaldo(dsl, fondo.id(), e.monto(), Dinero.cero(fondo.moneda()), fondo.version())) {
                // Imposible con el candado de arriba; si pasa, que se reintente entero y no a medias.
                throw new IllegalStateException("El fondo " + fondo.id() + " cambio de version bajo candado.");
            }
            OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
            fondos.registrarMovimiento(
                    dsl,
                    fondo.id(),
                    "RECARGO_MORA",
                    e.monto(),
                    saldoDespues,
                    "TRANSACCION_BILLETERA",
                    e.transaccionId(),
                    "Recargo por mora cobrado",
                    null,
                    ahora);
            return Resultado.ANOTADO;
        });
    }
}

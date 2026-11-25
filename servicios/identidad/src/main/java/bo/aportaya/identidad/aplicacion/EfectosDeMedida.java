package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.CausalDeMedida;
import bo.aportaya.identidad.dominio.PlazosDeMedida;
import bo.aportaya.identidad.dominio.RegistroDeMedida.Apelacion;
import bo.aportaya.identidad.dominio.RegistroDeMedida.Expediente;
import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.MedidasDeCuentaRepositorio;
import bo.aportaya.plataforma.dominio.CalendarioHabil;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.security.access.AccessDeniedException;

/**
 * CU-134 · los efectos y las reglas del expediente que comparten las transiciones de
 * {@link CU134GestionarMedida}: aplicar y revertir la medida sobre la cuenta, vencer plazos, cargar el
 * expediente abierto y emitir su evento. Corre siempre dentro de la transaccion de quien la llama; no es
 * un bean ni abre transacciones propias.
 */
final class EfectosDeMedida {

    private static final String APELACION_VENCIDA = "Venció el plazo de la segunda instancia";

    private final MedidasDeCuentaRepositorio medidas;
    private final AccesosRepositorio accesos;
    private final EvidenciaDeStepUp evidencias;
    private final Outbox outbox;

    EfectosDeMedida(
            MedidasDeCuentaRepositorio medidas,
            AccesosRepositorio accesos,
            EvidenciaDeStepUp evidencias,
            Outbox outbox) {
        this.medidas = medidas;
        this.accesos = accesos;
        this.evidencias = evidencias;
        this.outbox = outbox;
    }

    void vencerExpediente(DSLContext dsl, Expediente e, ContextoSesion ctx, OffsetDateTime ahora) {
        if ("EN_APELACION".equals(e.estado())) {
            medidas.apelacion(dsl, e.id())
                    .filter(a -> vencioApelacion(a, ahora))
                    .ifPresent(a -> aceptarPorVencimiento(dsl, e, a, ctx, ahora));
        } else if ("DECIDIDA".equals(e.estado())) {
            quedaFirme(dsl, e, ctx, ahora);
        } else if ("FIRME".equals(e.estado())) {
            medidas.transicion(dsl, e, "CUMPLIDA", null, "Vencio la suspension", ahora);
            revertir(dsl, e, null, ahora);
        } else {
            medidas.transicion(dsl, e, "PRESCRITO", null, "Prescribio sin decision", ahora);
            revertir(dsl, e, null, ahora);
        }
    }

    void aceptarPorVencimiento(
            DSLContext dsl, Expediente e, Apelacion apelacion, ContextoSesion ctx, OffsetDateTime ahora) {
        medidas.resolverApelacion(dsl, apelacion.id(), "ACEPTADA_POR_VENCIMIENTO", null, APELACION_VENCIDA, ahora);
        medidas.transicion(dsl, e, "REVOCADA", null, APELACION_VENCIDA, ahora);
        revertir(dsl, e, null, ahora);
        emitir(dsl, "identidad.medida_cuenta_revocada", e.id(), e.usuarioId(), ctx);
    }

    void quedaFirme(DSLContext dsl, Expediente e, ContextoSesion ctx, OffsetDateTime ahora) {
        medidas.firme(dsl, e.id(), ahora);
        medidas.transicion(dsl, e, "FIRME", ctx.esSistema() ? null : ctx.usuarioId(), "Medida firme", ahora);
        if (!e.cautelar()) {
            // D-7: sin cautelar, la suspension rige recien al quedar firme.
            aplicarMedida(dsl, e.usuarioId(), CausalDeMedida.valueOf(e.causal()), e.medida(), ahora);
        }
        emitir(dsl, "identidad.medida_cuenta_firme", e.id(), e.usuarioId(), ctx);
    }

    void aplicarMedida(DSLContext dsl, UUID usuarioId, CausalDeMedida causal, String medida, OffsetDateTime ahora) {
        medidas.estadoDeCuenta(
                dsl, usuarioId, "BLOQUEO_CAUTELAR".equals(medida) ? "BLOQUEADO_SEGURIDAD" : "SUSPENDIDO");
        if ("BLOQUEO_CAUTELAR".equals(medida)) {
            medidas.bloquear(
                    dsl,
                    usuarioId,
                    causal == CausalDeMedida.CUENTA_COMPROMETIDA ? "CUENTA_COMPROMETIDA" : "FRAUDE",
                    ahora);
        }
        accesos.cerrarSesionesDe(dsl, usuarioId, "Medida sobre la cuenta (CU-134)", ahora);
    }

    /** Cierra el expediente y revierte los efectos solo si la medida llegó a aplicarse. */
    void revertir(DSLContext dsl, Expediente e, UUID por, OffsetDateTime ahora) {
        if (!e.cautelar() && e.firmeEn() == null) {
            medidas.cerrar(dsl, e.id(), ahora);
            return;
        }
        medidas.estadoDeCuenta(dsl, e.usuarioId()).ifPresent(actual -> {
            if (actual.equals("SUSPENDIDO") || actual.equals("BLOQUEADO_SEGURIDAD")) {
                medidas.estadoDeCuenta(dsl, e.usuarioId(), e.estadoCuentaPrevio());
            }
        });
        if ("BLOQUEO_CAUTELAR".equals(e.medida())) {
            medidas.liberarBloqueosDeMedida(dsl, e.usuarioId(), por, ahora);
        }
        medidas.cerrar(dsl, e.id(), ahora);
    }

    boolean vencioApelacion(Apelacion apelacion, OffsetDateTime ahora) {
        return apelacion.fechaLimiteResolucion() != null && ahora.isAfter(apelacion.fechaLimiteResolucion());
    }

    /** El plazo de la segunda instancia corre desde la recepcion, en dias habiles de La Paz (A-3). */
    OffsetDateTime limiteResolucionApelacion(DSLContext dsl, Expediente e, OffsetDateTime ahora) {
        OffsetDateTime recepcionLocal = ahora.atZoneSameInstant(Reloj.ZONA).toOffsetDateTime();
        int dias = diasResolucionApelacion(dsl, e, ahora);
        return PlazosDeMedida.limiteResolucionApelacion(recepcionLocal, dias, calendario(dsl, recepcionLocal));
    }

    private int diasResolucionApelacion(DSLContext dsl, Expediente e, OffsetDateTime ahora) {
        // Los expedientes anteriores a A-3 conservan sus plazos. Para una nueva apelación
        // en uno de ellos se toma solo el plazo nuevo, que no existía cuando se abrió.
        return medidas.politica(dsl, e.politicaId())
                .resolucionApelacionDiasHabiles()
                .or(() -> medidas.politicaVigente(dsl, ahora)
                        .flatMap(MedidasDeCuentaRepositorio.PoliticaVigente::resolucionApelacionDiasHabiles))
                .orElseThrow(() -> new IllegalStateException("Sin plazo vigente para resolver apelaciones CU-134"));
    }

    Expediente abierto(DSLContext dsl, UUID id) {
        Expediente e = medidas.paraActualizar(dsl, id)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(134, 10), "No existe ese expediente."));
        if (e.cerradaEn() != null) {
            throw new ErrorDeNegocio(CodigoError.de(134, 10), "Ese expediente ya esta cerrado.");
        }
        return e;
    }

    void noPrescrito(Expediente e, OffsetDateTime ahora) {
        if (!ahora.isBefore(e.prescribeEn())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(134, 10), "El expediente prescribio: no se reabre por el mismo hecho.");
        }
    }

    void soloElTitular(Expediente e, ContextoSesion ctx) {
        if (!e.usuarioId().equals(ctx.usuarioId())) {
            throw new AccessDeniedException("solo el titular presenta descargo o apelacion");
        }
    }

    CalendarioHabil calendario(DSLContext dsl, OffsetDateTime desde) {
        Set<java.time.LocalDate> no =
                medidas.noHabiles(dsl, desde.toLocalDate(), desde.toLocalDate().plusDays(90));
        return no::contains;
    }

    CU134GestionarMedida.Vista vista(DSLContext dsl, UUID id) {
        Expediente e = medidas.consultar(dsl, id).orElseThrow();
        return new CU134GestionarMedida.Vista(
                e,
                medidas.lineaDeTiempo(dsl, id),
                medidas.apelacion(dsl, id),
                medidas.estadoDeCuenta(dsl, e.usuarioId()).orElse("ACTIVO"));
    }

    void emitir(DSLContext dsl, String tipo, UUID expedienteId, UUID usuarioId, ContextoSesion ctx) {
        outbox.emitir(
                dsl,
                new EventoDominio(
                        tipo,
                        "expediente_medida_cuenta",
                        expedienteId,
                        Map.of("usuarioId", usuarioId.toString()),
                        UUID.fromString(ctx.traza().id())));
    }

    void exigirSegundoFactor(ContextoSesion ctx, String evidenciaMfa) {
        if (!evidencias.consumir(ctx.usuarioId(), evidenciaMfa)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(134, 3),
                    "Confirma esta decision con tu segundo factor y volve a intentarlo (R-SEG-12).");
        }
    }
}

package bo.aportaya.cumplimiento.aplicacion;

import bo.aportaya.cumplimiento.infraestructura.CasoLftRepositorio;
import bo.aportaya.cumplimiento.infraestructura.MonitoreoLftRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-44 · De alerta de monitoreo a reporte de operacion sospechosa.
 *
 * <p>**Rige el deber de reserva: el titular no se entera de nada.** Ni pantalla, ni
 * aviso, ni un cambio de estado que se le note. Avisarle a alguien que esta siendo
 * investigado por lavado es, literalmente, el delito de tipping-off.
 *
 * <p>Y tres controles que sostienen que la investigacion valga:
 *
 * <ul>
 *   <li>**Ninguna alerta se cierra sin conclusion** (R-UIF-07). «Revisado» no le sirve a
 *       nadie dos años despues.
 *   <li>**Quien revisa no es quien analizo** ({@code ck_caso_revision}). Revisarse a uno
 *       mismo no es revisar.
 *   <li>**El caso nace con plazo** (R-UIF-08). Una investigacion sin fecha limite se
 *       convierte en una investigacion sin fin.
 * </ul>
 */
@Service
public class CU44InvestigarYReportar {

    private final Datos datos;
    private final MonitoreoLftRepositorio monitoreo;
    private final CasoLftRepositorio casos;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Map<String, Duration> plazoPorSeveridad;

    public CU44InvestigarYReportar(
            Datos datos,
            MonitoreoLftRepositorio monitoreo,
            CasoLftRepositorio casos,
            Outbox outbox,
            Reloj reloj,
            Map<String, Duration> plazoPorSeveridad) {
        this.datos = datos;
        this.monitoreo = monitoreo;
        this.casos = casos;
        this.outbox = outbox;
        this.reloj = reloj;
        this.plazoPorSeveridad = plazoPorSeveridad;
    }

    /** Agrupa alertas del mismo cliente en un caso y le pone plazo. */
    @Transactional
    public SalidaCaso abrirCaso(EntradaCaso entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var alertas = monitoreo.alertas(dsl, entrada.alertaIds());
            if (alertas.isEmpty()
                    || alertas.size() != entrada.alertaIds().stream().distinct().count()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(44, 8), "No hay alertas para investigar con esos identificadores.");
            }
            // Reintento (backoffice carril A · H5.S2): si TODAS ya estan en un mismo caso de este
            // analista, es el caso que se abrio la primera vez; se devuelve, no se abre otro vacio.
            var enCaso = monitoreo.casosDe(dsl, entrada.alertaIds());
            if (enCaso.size() == alertas.size()
                    && enCaso.values().stream().distinct().count() == 1) {
                UUID existente = enCaso.values().iterator().next();
                var caso = casos.porId(dsl, existente)
                        .filter(c -> c.analistaId().equals(ctx.usuarioId()))
                        .orElseThrow(() -> alertasInvalidas());
                return new SalidaCaso(caso.id(), caso.estado(), caso.plazoLimite(), null);
            }
            if (!enCaso.isEmpty()) {
                throw alertasInvalidas();
            }
            // Un caso reune alertas de UN cliente. Mezclar clientes en un expediente
            // hace que la decision sobre uno contamine al otro.
            var titulares = alertas.stream()
                    .map(MonitoreoLftRepositorio.Alerta::usuarioId)
                    .distinct()
                    .toList();
            if (titulares.size() > 1) {
                throw new ErrorDeNegocio(
                        CodigoError.de(44, 8), "Las alertas son de titulares distintos: van en casos separados.");
            }

            String severidad = alertas.stream()
                    .map(MonitoreoLftRepositorio.Alerta::severidad)
                    .max(java.util.Comparator.comparingInt(CU44InvestigarYReportar::peso))
                    .orElse("MEDIA");
            // R-UIF-08 · el plazo se guarda al abrir, no se recalcula al consultar.
            OffsetDateTime plazo = ahora.plus(plazoPorSeveridad.getOrDefault(severidad, Duration.ofDays(30)));

            UUID casoId = casos.abrir(
                    dsl,
                    titulares.get(0),
                    ctx.usuarioId(),
                    "ALERTA",
                    prioridadDe(severidad),
                    entrada.resumen(),
                    ahora,
                    plazo);

            for (var alerta : alertas) {
                // Si otra transaccion la tomo primero (ya no esta ABIERTA), este caso no se abre:
                // seguir dejaria un caso vacio, sin alertas que decidir.
                if (!monitoreo.asignar(dsl, alerta.id(), ctx.usuarioId(), casoId)) {
                    throw alertasInvalidas();
                }
            }

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "cumplimiento.caso_abierto",
                            "caso_investigacion_lft",
                            casoId,
                            Map.of(
                                    "analistaId", ctx.usuarioId().toString(),
                                    "alertas", Integer.toString(alertas.size()),
                                    "plazoLimite", plazo.toString(),
                                    // Sin usuarioId en el payload: el deber de reserva
                                    // alcanza tambien a la bandeja de eventos.
                                    "severidad", severidad),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaCaso(casoId, "ABIERTO", plazo, null);
        });
    }

    private static ErrorDeNegocio alertasInvalidas() {
        return new ErrorDeNegocio(CodigoError.de(44, 8), "Alguna alerta ya esta en otro caso o ya no esta abierta.");
    }

    private static int peso(String severidad) {
        return switch (severidad) {
            case "CRITICA" -> 4;
            case "ALTA" -> 3;
            case "MEDIA" -> 2;
            default -> 1;
        };
    }

    private static String prioridadDe(String severidad) {
        return switch (severidad) {
            case "CRITICA" -> "URGENTE";
            case "ALTA" -> "ALTA";
            case "MEDIA" -> "MEDIA";
            default -> "BAJA";
        };
    }

    public record EntradaCaso(List<UUID> alertaIds, String resumen) {}

    public record SalidaCaso(UUID casoId, String estado, OffsetDateTime plazoLimite, UUID reporteSospechosoId) {}
}

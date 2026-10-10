package bo.aportaya.garantia.dominio;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * H8.S1.M6 · El plan de contingencia ante una falta extraordinaria de caja: respaldo insuficiente sobre un
 * compromiso que ya existe (el pozo de un turno).
 *
 * <p>Lo que el plan DECIDE sin pedirle permiso a nadie es la contencion: no se cancela ninguna obligacion ni se
 * borra la deuda, la entrega queda en espera (la bloquea {@code entregas}), no se prometen ciclos nuevos
 * mientras dure y se escala a quien puede reponer el respaldo. Lo que NO decide es de donde sale la plata ni
 * si el grupo cambia sus reglas: eso es una decision humana (ambiguedad A11 del carril O2).
 *
 * <p>Los responsables son <b>roles y el responsable de la reserva</b>, no personas inventadas: quien ocupa
 * cada rol lo resuelve la operacion. La comunicacion al grupo dice hechos (cuanto falta, que se conserva,
 * que sigue en espera), nunca una probabilidad ni una promesa de fecha.
 */
public record PlanDeCaja(
        UUID grupoId,
        UUID turnoId,
        Dinero faltante,
        Dinero disponible,
        Dinero sinCubrir,
        String motivo,
        Optional<UUID> responsableDeLaReserva,
        List<Pendiente> obligaciones) {

    /** Una obligacion que sigue sin cobrarse y por cuanto: la deuda que se conserva. */
    public record Pendiente(UUID obligacionId, Dinero monto) {}

    /** Los roles a los que se escala, en orden. Son nombres del catalogo de permisos, no personas. */
    public static final List<String> ROLES_A_ESCALAR = List.of("TESORERIA", "RESPONSABLE_RIESGOS");

    /** Las medidas de contencion, con nombre estable para que cualquier consumidor las reconozca. */
    public static final List<String> MEDIDAS = List.of(
            "CONSERVAR_DEUDA",
            "ENTREGA_EN_ESPERA",
            "SUSPENDER_NUEVAS_RESERVAS",
            "ESCALAR_A_TESORERIA",
            "COMUNICAR_AL_GRUPO",
            "REPONER_RESPALDO");

    public PlanDeCaja {
        obligaciones = List.copyOf(obligaciones);
    }

    /** Lo que se guarda en {@code plan_contingencia.descripcion}: hechos, sin datos personales. */
    public String descripcion() {
        return "Falta extraordinaria de caja en el turno %s: faltan %s, el respaldo disponible es %s y quedan sin cubrir %s (%s). "
                        .formatted(turnoId, faltante, disponible, sinCubrir, motivo)
                + "La deuda de %d obligacion(es) se conserva, la entrega queda en espera y no se reservan ciclos nuevos hasta reponer el respaldo."
                        .formatted(obligaciones.size());
    }

    /** El texto para el grupo: cuanto falta, que se conserva y que sigue en espera. Ni una probabilidad ni una fecha. */
    public String comunicacionAlGrupo() {
        return "La entrega de este turno todavia no puede completarse: al pozo le faltan %s y el respaldo disponible no lo cubre. "
                        .formatted(faltante)
                + "Ninguna obligacion se cancela y la deuda se conserva; la entrega sigue en espera hasta que se reponga el respaldo."
                + " Te avisamos apenas cambie.";
    }

    /** La carga del evento de escalamiento: lo que backoffice y notificaciones necesitan, y nada mas. */
    public Map<String, Object> comoCarga(UUID planId) {
        return Map.ofEntries(
                Map.entry("planId", planId.toString()),
                Map.entry("grupoId", grupoId.toString()),
                Map.entry("turnoId", turnoId.toString()),
                Map.entry("motivo", motivo),
                Map.entry("faltante", faltante.toString()),
                Map.entry("disponible", disponible.toString()),
                Map.entry("sinCubrir", sinCubrir.toString()),
                Map.entry("moneda", faltante.moneda().name()),
                Map.entry(
                        "responsableDeLaReserva",
                        responsableDeLaReserva.map(UUID::toString).orElse("")),
                Map.entry("rolesAEscalar", ROLES_A_ESCALAR),
                Map.entry("medidas", MEDIDAS),
                Map.entry(
                        "obligaciones",
                        obligaciones.stream()
                                .map(o -> Map.of(
                                        "obligacionId",
                                        o.obligacionId().toString(),
                                        "monto",
                                        o.monto().toString()))
                                .toList()),
                Map.entry("comunicacionAlGrupo", comunicacionAlGrupo()));
    }
}

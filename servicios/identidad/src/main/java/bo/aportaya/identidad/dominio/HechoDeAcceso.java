package bo.aportaya.identidad.dominio;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Un acto de gobierno de accesos ya ocurrido: quien, que, a quien, por que y cuando (GU-07, decision P-5).
 *
 * <p>Es la forma v1 del evento de bitacora que consume {@code auditoria} (CU-08 § Contrato de eventos). Lleva
 * identificadores y el motivo que escribio quien actuo; nunca correo, telefono ni documento del afectado
 * (regla 90.2.1): quien necesita el nombre lo pide por la API con su permiso.
 *
 * @param accion lo que paso
 * @param actorId quien actuo ({@code null} solo para el trabajo de vencimiento, que no tiene persona)
 * @param afectadoId a quien le paso
 * @param motivo la justificacion o el motivo que escribio quien actuo
 * @param ocurridoEn cuando
 * @param detalle datos propios de la accion (asignacionId, rolCodigo, ambito, sesionesCerradas...)
 */
public record HechoDeAcceso(
        Accion accion,
        Optional<UUID> actorId,
        UUID afectadoId,
        String motivo,
        OffsetDateTime ocurridoEn,
        Map<String, Object> detalle) {

    /** Version de la forma de la carga: un cambio incompatible sube este numero (regla 98.1.3). */
    public static final String VERSION = "1";

    public HechoDeAcceso {
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(afectadoId, "afectadoId");
        Objects.requireNonNull(motivo, "motivo");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        detalle = Map.copyOf(Objects.requireNonNull(detalle, "detalle"));
    }

    /** La carga del evento: sin nulos, porque el outbox la copia con {@code Map.copyOf}. */
    public Map<String, Object> carga() {
        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("version", VERSION);
        carga.put("accion", accion.name());
        actorId.ifPresent(actor -> carga.put("actorId", actor.toString()));
        carga.put("afectadoId", afectadoId.toString());
        carga.put("motivo", motivo);
        carga.put("ocurridoEn", ocurridoEn.toString());
        carga.putAll(detalle);
        return carga;
    }

    /** Los actos que CU-08 manda dejar en la bitacora y avisar. El tipo del evento sale de aca. */
    public enum Accion {
        ROL_ASIGNADO("identidad.rol_asignado"),
        ROL_REVOCADO("identidad.rol_revocado"),
        ROL_VENCIDO("identidad.rol_vencido"),
        OPERADOR_DADO_DE_ALTA("identidad.usuario_interno_creado"),
        OPERADOR_SUSPENDIDO("identidad.usuario_interno_suspendido"),
        OPERADOR_REACTIVADO("identidad.usuario_interno_reactivado"),
        OPERADOR_DADO_DE_BAJA("identidad.usuario_interno_dado_de_baja"),
        OPERADOR_DESBLOQUEADO("identidad.usuario_interno_desbloqueado");

        private final String tipoDeEvento;

        Accion(String tipoDeEvento) {
            this.tipoDeEvento = tipoDeEvento;
        }

        public String tipoDeEvento() {
            return tipoDeEvento;
        }

        /** Las de una asignacion se agregan por la asignacion; las de la cuenta, por el usuario. */
        public boolean esDeAsignacion() {
            return this == ROL_ASIGNADO || this == ROL_REVOCADO || this == ROL_VENCIDO;
        }
    }
}

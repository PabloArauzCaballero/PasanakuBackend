package bo.aportaya.auditoria.dominio;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * CU-131 · el veredicto de una persona sobre lo que el catalogo dice de una columna: aprobada, rechazada o
 * pendiente, con que confianza y por que. Un rechazo sin motivo no le sirve a quien tiene que corregir.
 */
public record RevisionDeColumna(
        String decision,
        Optional<String> confianza,
        Optional<String> motivo,
        Optional<UUID> revisadoPor,
        Optional<OffsetDateTime> revisadoEn,
        int version) {

    public static final String RECHAZADA = "RECHAZADA";

    /** Rechazar exige decir por que: es lo unico que vuelve util el rechazo. */
    public boolean exigeMotivo() {
        return RECHAZADA.equals(decision) && motivo.map(String::isBlank).orElse(true);
    }

    public static String clave(String esquema, String tabla, String columna) {
        return esquema + "." + tabla + "." + columna;
    }
}

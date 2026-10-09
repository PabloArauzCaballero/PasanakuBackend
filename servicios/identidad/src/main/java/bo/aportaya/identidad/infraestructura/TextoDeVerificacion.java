package bo.aportaya.identidad.infraestructura;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.jooq.DSLContext;

/** Texto acotado y enmascarado para lo que se persiste de una verificación de correo. */
final class TextoDeVerificacion {
    private TextoDeVerificacion() {}

    static String enmascarar(String correo) {
        String normal = correo.strip().toLowerCase(Locale.ROOT);
        int arroba = normal.indexOf('@');
        String nombre = arroba > 0 ? normal.substring(0, arroba) : normal;
        String dominio = arroba > 0 ? normal.substring(arroba) : "";
        String visible = nombre.substring(0, Math.min(2, nombre.length()));
        String mascara = visible + "***" + dominio;
        return limitar(mascara, 40);
    }

    static String limitar(String valor, int maximo) {
        if (valor == null) return "desconocido";
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    static void registrarIntento(
            DSLContext dsl, UUID id, UUID tokenId, String resultado, String ip, String agente, OffsetDateTime ahora) {
        dsl.execute(
                """
                INSERT INTO identidad.intento_validacion_token
                    (id, token_id, fecha_hora, resultado, ip_origen, agente_usuario)
                VALUES (?, ?, ?::timestamptz, ?, ?::inet, ?)
                """,
                id,
                tokenId,
                ahora,
                resultado,
                ip,
                limitar(agente, 255));
    }
}

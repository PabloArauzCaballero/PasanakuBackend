package bo.aportaya.auditoria.dominio;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * CU-130 · la ficha de gobierno de una tabla del catalogo de datos: si esta en uso, si alguien reviso su descripcion,
 * quien responde por ella y que retencion le aplica.
 *
 * <p>La sensibilidad NO vive aca: se deriva de las columnas del modelo. Guardarla tambien aca daria dos fuentes de
 * verdad para la misma pregunta.
 */
public record FichaDeGobierno(
        String estado,
        String estadoRevision,
        Optional<String> responsable,
        Optional<UUID> politicaRetencionId,
        boolean criticaParaAuditoria,
        Optional<String> notaRevision,
        Optional<UUID> revisadoPor,
        Optional<OffsetDateTime> actualizadoEn,
        int version) {

    /** La clave con la que se cruza la ficha con su tabla del catalogo. */
    public static String clave(String esquema, String tabla) {
        return esquema + "." + tabla;
    }

    /**
     * Una tabla sin ficha guardada: esta en el modelo (activa) y nadie la reviso (detectada). Es el mismo valor por
     * omision del catalogo de Atlas, y evita sembrar 338 filas que no dicen nada.
     */
    public static FichaDeGobierno porOmision() {
        return new FichaDeGobierno(
                "ACTIVA",
                "DETECTADA",
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0);
    }
}

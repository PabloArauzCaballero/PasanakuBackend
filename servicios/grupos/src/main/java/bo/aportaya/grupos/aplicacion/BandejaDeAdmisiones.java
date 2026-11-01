package bo.aportaya.grupos.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * La bandeja general del backoffice (H4.B.M1): solicitudes de TODOS los grupos, las mas nuevas
 * primero, con el codigo del grupo. Corre con contexto de sistema, igual que la cola del
 * organizador, porque la RLS de {@code solicitud_ingreso} solo deja pasar a BACKOFFICE,
 * CUMPLIMIENTO y AUDITOR y los roles sembrados del backoffice son otros: la puerta es el permiso
 * {@code SOLICITUD_INGRESO_VER} del controlador. Sin telefono ni correo (no hay catalogo de
 * propositos para la bitacora de lectura, R-SEG-02).
 *
 * <p>{@code estado} usa los nombres de la pantalla ({@code ACEPTADA} es {@code APROBADA} en la
 * base); uno que no existe no coincide con ninguna fila. {@code limite} se acota a 1..200.
 *
 * <p>Salio de {@link CU68AceptarIngreso} (limite de 300 lineas) sin cambiar lo que hace; la transaccion la abre ese
 * caso de uso.
 */
final class BandejaDeAdmisiones {

    private static final int FILAS_DE_LA_BANDEJA = 200;

    private final Datos datos;

    BandejaDeAdmisiones(Datos datos) {
        this.datos = datos;
    }

    List<CU68AceptarIngreso.FilaDeBandeja> leer(String estado, Integer limite, ContextoSesion ctx) {
        String enBase = estado == null ? null : "ACEPTADA".equals(estado) ? "APROBADA" : estado;
        int tope = limite == null ? FILAS_DE_LA_BANDEJA : Math.max(1, Math.min(limite, FILAS_DE_LA_BANDEJA));
        var sistema =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(sistema, dsl -> dsl.fetch(
                        """
                        SELECT s.id, s.usuario_id, s.grupo_id, g.codigo_publico, s.cupos_solicitados, s.mensaje,
                               s.puntaje_compatibilidad, s.estado, s.fecha_solicitud, s.nombre_visible,
                               s.enlace_grupo_id IS NOT NULL AS por_enlace
                          FROM grupos.solicitud_ingreso s
                          JOIN grupos.grupo g ON g.id = s.grupo_id
                         WHERE (?::varchar IS NULL OR s.estado = ?::varchar)
                         ORDER BY s.fecha_solicitud DESC, s.id
                         LIMIT ?
                        """,
                        enBase,
                        enBase,
                        tope)
                .map(r -> new CU68AceptarIngreso.FilaDeBandeja(
                        r.get("id", UUID.class),
                        r.get("usuario_id", UUID.class),
                        r.get("grupo_id", UUID.class),
                        r.get("codigo_publico", String.class),
                        r.get("cupos_solicitados", Integer.class),
                        r.get("mensaje", String.class),
                        r.get("puntaje_compatibilidad", java.math.BigDecimal.class),
                        "APROBADA".equals(r.get("estado", String.class)) ? "ACEPTADA" : r.get("estado", String.class),
                        r.get("fecha_solicitud", OffsetDateTime.class),
                        r.get("nombre_visible", String.class),
                        Boolean.TRUE.equals(r.get("por_enlace", Boolean.class)))));
    }
}

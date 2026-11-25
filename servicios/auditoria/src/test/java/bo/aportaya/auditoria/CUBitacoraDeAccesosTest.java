package bo.aportaya.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.aplicacion.ConsultarBitacora;
import bo.aportaya.auditoria.aplicacion.RegistroDeCambiosDeAcceso;
import bo.aportaya.auditoria.aplicacion.RegistroDeCambiosDeAcceso.Resultado;
import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio;
import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio.CambioDeAcceso;
import bo.aportaya.plataforma.datos.Datos;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GU-07 · contra PostgreSQL real: el cambio de acceso entra a {@code comun.bitacora_evento} una sola vez por evento,
 * con su hash de cadena, y {@code GET /auditoria/bitacora} lo devuelve.
 */
class CUBitacoraDeAccesosTest extends BaseDeAuditoria {

    private final RegistroDeCambiosDeAcceso registro =
            new RegistroDeCambiosDeAcceso(new Datos(dsl), new BitacoraRepositorio());

    private static CambioDeAcceso cambio(UUID usuario, UUID actor) {
        return new CambioDeAcceso(
                "usuario_interno",
                usuario,
                "CAMBIO_PERMISOS",
                actor,
                "INTERNAL_IDENTITY_ADMIN",
                "PANEL_ADMIN",
                UUID.randomUUID(),
                "{\"evento\":\"identidad.usuario_interno_roles_asignados\",\"carga\":{\"roles\":\"SOPORTE\"}}",
                "rotacion de turno",
                OffsetDateTime.of(2026, 11, 24, 15, 0, 0, 0, ZoneOffset.UTC));
    }

    private Resultado registrar(UUID evento, CambioDeAcceso cambio) {
        return transaccion.execute(t -> registro.registrar(evento, cambio));
    }

    private long filas(UUID usuario) {
        return (Long) dslFixtura.fetchValue("SELECT count(*) FROM comun.bitacora_evento WHERE entidad_id = ?", usuario);
    }

    @Test
    @DisplayName("correcto: el cambio queda en la bitacora con actor, motivo y hash de la cadena")
    void queda() {
        UUID usuario = UUID.randomUUID();
        UUID actor = fixtura.usuarioConId(UUID.randomUUID());

        assertThat(registrar(UUID.randomUUID(), cambio(usuario, actor))).isEqualTo(Resultado.REGISTRADO);

        var fila = dslFixtura.fetchOne(
                "SELECT accion, actor_usuario_id, motivo, hash_registro FROM comun.bitacora_evento WHERE entidad_id = ?",
                usuario);
        assertThat(fila.get("accion", String.class)).isEqualTo("CAMBIO_PERMISOS");
        assertThat(fila.get("actor_usuario_id", UUID.class)).isEqualTo(actor);
        assertThat(fila.get("motivo", String.class)).isEqualTo("rotacion de turno");
        assertThat(fila.get("hash_registro", String.class)).hasSize(64);
    }

    @Test
    @DisplayName("duplicado: el mismo evento entregado dos veces deja una sola fila")
    void duplicadoUnaVez() {
        UUID usuario = UUID.randomUUID();
        UUID evento = UUID.randomUUID();
        var cambio = cambio(usuario, null);

        assertThat(registrar(evento, cambio)).isEqualTo(Resultado.REGISTRADO);
        assertThat(registrar(evento, cambio)).isEqualTo(Resultado.DUPLICADO);

        assertThat(filas(usuario)).isEqualTo(1L);
    }

    @Test
    @DisplayName("consultable: la bitacora filtrada por entidad devuelve el cambio de acceso")
    void consultable() {
        UUID usuario = UUID.randomUUID();
        registrar(UUID.randomUUID(), cambio(usuario, null));

        var pagina = transaccion.execute(
                t -> new ConsultarBitacora(new Datos(dsl), new BitacoraRepositorio(), new ObjectMapper())
                        .listar("usuario_interno", "CAMBIO_PERMISOS", null, null, null, 100, null, null, contexto()));

        assertThat(pagina.getItems())
                .anySatisfy(e -> assertThat(e.getEntidadId()).isEqualTo(usuario));
    }
}

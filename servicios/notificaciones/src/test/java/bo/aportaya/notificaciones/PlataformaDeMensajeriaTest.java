package bo.aportaya.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.notificaciones.aplicacion.ConsultarMensajeria;
import bo.aportaya.notificaciones.dominio.PlataformaDeMensajeria;
import bo.aportaya.notificaciones.infraestructura.AuditoriaDeMensajeriaRepositorio;
import bo.aportaya.notificaciones.infraestructura.PlataformaDeMensajeriaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sistemas › Proveedores y Webhooks de notificaciones contra Postgres real. El acuse se inserta como dato: todavia no
 * hay codigo que escriba {@code evento_entrega_mensaje} (ver {@code CU83EnviosTest}). Datos sinteticos.
 */
class PlataformaDeMensajeriaTest extends BaseDeNotificaciones {

    private final ConsultarMensajeria consulta = new ConsultarMensajeria(
            new Datos(dsl), new AuditoriaDeMensajeriaRepositorio(), new PlataformaDeMensajeriaRepositorio());

    @AfterEach
    void limpiar() {
        dslFixtura.execute("DELETE FROM notificaciones.evento_entrega_mensaje");
        fixtura.limpiar();
    }

    private static ContextoSesion operador() {
        return ContextoSesion.de(
                UUID.randomUUID(), "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private static String codigo() {
        return "M" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private UUID envio(UUID proveedor) {
        String corto = UUID.randomUUID().toString().substring(0, 8);
        UUID evento = fixtura.evento("aviso.prueba." + corto, "COBRANZA", true, "ALTA");
        UUID version = fixtura.plantilla("PL-" + corto, evento, "SMS", "hola");
        UUID notificacion = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO notificaciones.notificacion
                    (id, usuario_id, evento_id, prioridad, contexto, clave_deduplicacion, estado,
                     programada_para, creada_en, correlation_id)
                VALUES (?, ?, ?, 'ALTA', '{}'::jsonb, ?, 'EN_COLA', now(), now(), gen_random_uuid())
                """,
                notificacion,
                fixtura.usuario(),
                evento,
                "dedupe-" + notificacion);
        UUID envio = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO notificaciones.envio_notificacion
                    (id, notificacion_id, proveedor_id, version_plantilla_id, canal, destinatario,
                     clave_idempotencia, encolado_en, contenido_enviado, estado, orden, intentos,
                     max_intentos, costo, moneda)
                VALUES (?, ?, ?, ?, 'SMS', 'sms:destino-sintetico', ?, clock_timestamp(), 'hola', 'ENVIADO',
                        1, 1, 3, 0.00, 'BOB')
                """,
                envio,
                notificacion,
                proveedor,
                version,
                "idem-" + envio);
        return envio;
    }

    private void acuse(UUID envio, String tipo, String recibido, String error) {
        dslFixtura.execute(
                """
                INSERT INTO notificaciones.evento_entrega_mensaje
                    (envio_id, tipo_evento, fecha_hora_proveedor, recibido_en, payload_crudo, codigo_error,
                     clave_idempotencia)
                VALUES (?, ?, ?::timestamptz, ?::timestamptz, '{"secreto":"no-sale"}'::jsonb, ?, ?)
                """,
                envio,
                tipo,
                recibido,
                recibido,
                error,
                "acuse-" + UUID.randomUUID());
    }

    @Test
    @DisplayName("correcto · proveedores por prioridad con su salud exacta; sin credenciales ni costo sin moneda")
    void proveedores() {
        String a = codigo(), b = codigo();
        fixtura.proveedor(a, "SMS", 92, new BigDecimal("0.0300"), 97, true);
        fixtura.proveedor(b, "WHATSAPP", 91, new BigDecimal("0.0100"), 80, false);
        List<PlataformaDeMensajeria.Proveedor> propios =
                transaccion.execute(s -> consulta.proveedores(operador())).stream()
                        .filter(p -> p.codigo().equals(a) || p.codigo().equals(b))
                        .toList();
        assertThat(propios).extracting(PlataformaDeMensajeria.Proveedor::codigo).containsExactly(b, a);
        assertThat(propios.get(1).salud()).isEqualByComparingTo("97");
        assertThat(propios.get(0).activo()).isFalse();
        assertThat(Arrays.stream(PlataformaDeMensajeria.Proveedor.class.getRecordComponents())
                        .map(c -> c.getName()))
                .doesNotContain("referenciaCredenciales", "urlBase", "costoPorMensaje");
    }

    @Test
    @DisplayName(
            "correcto y limite · acuses del mas reciente al mas viejo, con proveedor y canal; el limite corta; sin payload")
    void acuses() {
        String c = codigo();
        UUID proveedor = fixtura.proveedor(c, "SMS", 93, new BigDecimal("0.0300"), 99, true);
        UUID e = envio(proveedor);
        acuse(e, "ENTREGADO", "2099-03-01T10:00:00Z", null);
        acuse(e, "FALLIDO", "2099-03-02T10:00:00Z", "131026");

        List<PlataformaDeMensajeria.EventoDeEntrega> dos =
                transaccion.execute(s -> consulta.eventosDeEntrega(2, operador()));
        assertThat(dos)
                .extracting(PlataformaDeMensajeria.EventoDeEntrega::tipoEvento)
                .containsExactly("FALLIDO", "ENTREGADO");
        assertThat(dos.get(0))
                .extracting(x -> x.proveedorCodigo(), x -> x.canal())
                .containsExactly(c, "SMS");
        assertThat(dos.get(0).codigoError()).contains("131026");
        assertThat(dos.get(1).codigoError()).isEmpty();
        List<PlataformaDeMensajeria.EventoDeEntrega> uno =
                transaccion.execute(s -> consulta.eventosDeEntrega(1, operador()));
        assertThat(uno).hasSize(1);
        assertThat(Arrays.stream(PlataformaDeMensajeria.EventoDeEntrega.class.getRecordComponents())
                        .map(x -> x.getName()))
                .doesNotContain("payloadCrudo", "claveIdempotencia", "destinatario");
    }
}

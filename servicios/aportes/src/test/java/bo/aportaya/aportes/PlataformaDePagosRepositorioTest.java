package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.ConsultarPlataformaDePagos;
import bo.aportaya.aportes.dominio.PlataformaDePagos;
import bo.aportaya.aportes.infraestructura.PlataformaDePagosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sistemas › Proveedores y Webhooks contra Postgres real. Codigos propios por prueba: el contenedor se comparte. */
class PlataformaDePagosRepositorioTest extends BaseDeAportes {

    private final ConsultarPlataformaDePagos consulta =
            new ConsultarPlataformaDePagos(new Datos(dsl), new PlataformaDePagosRepositorio());

    private static ContextoSesion operador() {
        return ContextoSesion.de(
                UUID.randomUUID(), "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private static String codigo() {
        return "P" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private void webhook(UUID proveedor, String estado, String recibido, String procesado) {
        dslFixtura.execute(
                """
                insert into aportes.webhook_pasarela
                  (proveedor_id, evento, payload_crudo, firma, firma_valida, recibido_en, procesado_en, estado,
                   clave_idempotencia, intentos_procesamiento)
                values (?, 'pago.confirmado', '{"secreto":"no-sale"}'::jsonb, 'firma-no-sale', true, ?::timestamptz,
                        ?::timestamptz, ?, ?, 1)
                """,
                proveedor,
                recibido,
                procesado,
                estado,
                UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("correcto · proveedores por prioridad, sin credenciales en el registro que se publica")
    void proveedores() {
        String a = codigo(), b = codigo();
        fixtura.proveedor(a, false, 91);
        fixtura.proveedor(b, true, 90);
        var propios = transaccion.execute(s -> consulta.proveedores(operador())).stream()
                .filter(p -> p.codigo().equals(a) || p.codigo().equals(b))
                .toList();
        assertThat(propios).extracting(PlataformaDePagos.Proveedor::codigo).containsExactly(b, a);
        assertThat(propios.get(0).tipo()).isEqualTo("PASARELA");
        assertThat(Arrays.stream(PlataformaDePagos.Proveedor.class.getRecordComponents())
                        .map(c -> c.getName()))
                .doesNotContain("referenciaCredenciales", "urlBase");
    }

    @Test
    @DisplayName(
            "correcto y limite · webhooks del mas reciente al mas viejo, con el codigo del proveedor; el limite corta")
    void webhooks() {
        String c = codigo();
        UUID p = fixtura.proveedor(c, false, 99);
        webhook(p, "PROCESADO", "2099-02-01T10:00:00Z", "2099-02-01T10:00:02Z");
        webhook(p, "FALLIDO", "2099-02-02T10:00:00Z", null);

        var ultimos = transaccion.execute(s -> consulta.webhooks(2, operador()));
        assertThat(ultimos).hasSize(2);
        assertThat(ultimos).extracting(PlataformaDePagos.Webhook::estado).containsExactly("FALLIDO", "PROCESADO");
        assertThat(ultimos.get(0).proveedorCodigo()).isEqualTo(c);
        assertThat(ultimos.get(0).procesadoEn()).isEmpty();
        assertThat(ultimos.get(1).procesadoEn()).isPresent();
        assertThat(Arrays.stream(PlataformaDePagos.Webhook.class.getRecordComponents())
                        .map(x -> x.getName()))
                .doesNotContain("payloadCrudo", "firma", "claveIdempotencia");
        java.util.List<PlataformaDePagos.Webhook> uno = transaccion.execute(s -> consulta.webhooks(1, operador()));
        assertThat(uno).hasSize(1);
    }
}

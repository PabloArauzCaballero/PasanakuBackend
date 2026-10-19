package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.aplicacion.CU45AtenderRequerimiento.EntradaRequerimiento;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-45 por HTTP (carril A · H9): registrar y responder un oficio con el plazo y el actor que corresponden. */
class CU45AccionesTest extends EscenarioDeAcciones {

    private static final String ALCANCE =
            "Movimientos de la cuenta del titular entre el 1 de enero y el 30 de junio de 2026.";

    private String oficio;

    @BeforeEach
    void escenario() {
        oficio = "ACC-" + UUID.randomUUID().toString().substring(0, 10);
    }

    private EntradaRequerimiento entrada(OffsetDateTime plazo, String hash) {
        return new EntradaRequerimiento(
                "FISCALIA", oficio, plazo, fixtura.usuario(), ALCANCE, "https://oficios.bo/" + oficio, hash, false);
    }

    private UUID registrado(OffsetDateTime plazo) {
        var ctx = operador(fixtura.usuario());
        UUID id = transaccion
                .execute(t -> acciones().registrarRequerimiento(entrada(plazo, "a".repeat(64)), ctx))
                .salida();
        return borrarAlTerminar("requerimiento_autoridad", id);
    }

    @Test
    @DisplayName(
            "Dado un oficio registrado con plazo vencido · Cuando el oficial responde desde el backoffice · Entonces se usa el plazo guardado y se abre el hallazgo aunque el cliente no mande plazo")
    void plazoGuardado() {
        UUID id = registrado(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        var oficial = operador(fixtura.usuario());

        var r = transaccion.execute(
                t -> acciones().responderRequerimiento(id, "https://oficios.bo/r/" + oficio, oficial));

        assertThat(r.esNuevo()).isTrue();
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.requerimiento_autoridad WHERE id = ? AND respondido_por = ?",
                        id,
                        oficial.usuarioId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.hallazgo_auditoria WHERE codigo = ? AND estado = 'ABIERTO'",
                        "OFI-" + oficio.substring(0, Math.min(16, oficio.length()))))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: el mismo oficio con el mismo documento devuelve el registrado y no crea otro")
    void reintentoDelRegistro() {
        UUID id = registrado(OffsetDateTime.now(ZoneOffset.UTC).plusDays(5));
        var ctx = operador(fixtura.usuario());

        var otra = transaccion.execute(t -> acciones()
                .registrarRequerimiento(
                        entrada(OffsetDateTime.now(ZoneOffset.UTC).plusDays(5), "a".repeat(64)), ctx));

        assertThat(otra.esNuevo()).isFalse();
        assertThat(otra.salida()).isEqualTo(id);
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.requerimiento_autoridad WHERE numero_oficio = ?",
                        oficio))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: el mismo oficial respondiendo con la misma respuesta recibe la que ya hay")
    void reintentoDeLaRespuesta() {
        UUID id = registrado(OffsetDateTime.now(ZoneOffset.UTC).plusDays(5));
        var oficial = operador(fixtura.usuario());
        String url = "https://oficios.bo/r/" + oficio;
        transaccion.execute(t -> acciones().responderRequerimiento(id, url, oficial));

        var otra = transaccion.execute(t -> acciones().responderRequerimiento(id, url, oficial));

        assertThat(otra.esNuevo()).isFalse();
    }

    @Test
    @DisplayName("rechaza con AP-CU45-01 el mismo numero de oficio con OTRO documento")
    void rechazaOtroDocumento() {
        registrado(OffsetDateTime.now(ZoneOffset.UTC).plusDays(5));
        var ctx = operador(fixtura.usuario());
        rechazaCon(
                "AP-CU45-01",
                () -> transaccion.execute(t -> acciones()
                        .registrarRequerimiento(
                                entrada(OffsetDateTime.now(ZoneOffset.UTC).plusDays(5), "b".repeat(64)), ctx)));
    }

    @Test
    @DisplayName("rechaza con AP-CU45-01 que otra persona responda un oficio ya respondido")
    void rechazaSegundaRespuesta() {
        UUID id = registrado(OffsetDateTime.now(ZoneOffset.UTC).plusDays(5));
        transaccion.execute(
                t -> acciones().responderRequerimiento(id, "https://oficios.bo/r/1", operador(fixtura.usuario())));
        rechazaCon(
                "AP-CU45-01",
                () -> transaccion.execute(t ->
                        acciones().responderRequerimiento(id, "https://oficios.bo/r/2", operador(fixtura.usuario()))));
    }

    @Test
    @DisplayName("rechaza con AP-CU45-01 responder un requerimiento que no existe")
    void rechazaInexistente() {
        rechazaCon(
                "AP-CU45-01",
                () -> transaccion.execute(t -> acciones()
                        .responderRequerimiento(
                                UUID.randomUUID(), "https://oficios.bo/r/x", operador(fixtura.usuario()))));
    }
}

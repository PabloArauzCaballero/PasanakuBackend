package bo.aportaya.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.aplicacion.RegistroDeDisponibilidad;
import bo.aportaya.auditoria.dominio.SaludDeServicio;
import bo.aportaya.auditoria.dominio.SaludDeServicio.Estado;
import bo.aportaya.auditoria.infraestructura.DisponibilidadRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * sistemas/servicios · contra Postgres real: el minuto se suma una sola vez aunque se repita, la ultima interrupcion
 * queda y la ventana de 30 dias deja afuera lo viejo. Cada prueba usa su propio nombre de servicio: el contenedor se
 * comparte.
 */
class DisponibilidadRepositorioTest extends BaseDeAuditoria {

    private final RegistroDeDisponibilidad registro =
            new RegistroDeDisponibilidad(new Datos(dsl), new DisponibilidadRepositorio());

    private static String servicioNuevo() {
        return "svc-" + UUID.randomUUID().toString().substring(0, 8).toLowerCase(Locale.ROOT);
    }

    private int registrar(String servicio, Estado estado, String minuto) {
        return transaccion.execute(s -> registro.registrar(
                List.of(new SaludDeServicio(servicio, estado, Optional.empty())), Instant.parse(minuto)));
    }

    @Test
    @DisplayName("correcto · tres minutos con uno caido: 66.66 % y la interrupcion en el minuto caido")
    void tresMinutos() {
        String s = servicioNuevo();
        registrar(s, Estado.ARRIBA, "2026-10-29T10:00:10Z");
        registrar(s, Estado.CAIDO, "2026-10-29T10:01:10Z");
        registrar(s, Estado.ARRIBA, "2026-10-29T10:02:10Z");

        var medido = transaccion
                .execute(x -> registro.enLaVentana(Instant.parse("2026-10-29T12:00:00Z")))
                .get(s);
        assertThat(medido.minutosMedidos()).isEqualTo(3);
        assertThat(medido.minutosArriba()).isEqualTo(2);
        assertThat(medido.disponibilidad()).contains(new BigDecimal("66.66"));
        assertThat(medido.ultimaInterrupcion()).contains(Instant.parse("2026-10-29T10:01:00Z"));
    }

    @Test
    @DisplayName("idempotencia · el mismo minuto dos veces (o uno anterior) no se cuenta de nuevo")
    void mismoMinuto() {
        String s = servicioNuevo();
        assertThat(registrar(s, Estado.ARRIBA, "2026-10-29T10:00:05Z")).isEqualTo(1);
        assertThat(registrar(s, Estado.CAIDO, "2026-10-29T10:00:40Z")).isZero();
        assertThat(registrar(s, Estado.ARRIBA, "2026-10-29T09:59:00Z")).isZero();

        var medido = transaccion
                .execute(x -> registro.enLaVentana(Instant.parse("2026-10-29T12:00:00Z")))
                .get(s);
        assertThat(medido.minutosMedidos()).isEqualTo(1);
        assertThat(medido.minutosArriba()).isEqualTo(1);
        assertThat(medido.ultimaInterrupcion()).isEmpty();
    }

    @Test
    @DisplayName("limite · el dia 30 entra en la ventana, el 31 no; la ultima interrupcion se ve igual")
    void ventana() {
        String s = servicioNuevo();
        registrar(s, Estado.CAIDO, "2026-09-29T23:59:00Z"); // 31 dias antes del 29-10: afuera
        registrar(s, Estado.ARRIBA, "2026-09-30T00:00:00Z"); // dia 30 contando hoy: adentro

        var medido = transaccion
                .execute(x -> registro.enLaVentana(Instant.parse("2026-10-29T12:00:00Z")))
                .get(s);
        assertThat(medido.minutosMedidos()).isEqualTo(1);
        assertThat(medido.minutosArriba()).isEqualTo(1);
        assertThat(medido.ultimaInterrupcion()).contains(Instant.parse("2026-09-29T23:59:00Z"));
    }

    @Test
    @DisplayName("limite · el cambio de dia UTC abre otro renglon y la suma de la ventana los junta")
    void cambioDeDia() {
        String s = servicioNuevo();
        registrar(s, Estado.ARRIBA, "2026-10-28T23:59:00Z");
        registrar(s, Estado.DEGRADADO, "2026-10-29T00:00:00Z");

        long renglones = dslFixtura.fetchCount(
                dslFixtura.selectFrom("auditoria.disponibilidad_servicio_dia").where("servicio = ?", s));
        var medido = transaccion
                .execute(x -> registro.enLaVentana(Instant.parse("2026-10-29T12:00:00Z")))
                .get(s);
        assertThat(renglones).isEqualTo(2);
        assertThat(medido.minutosMedidos()).isEqualTo(2);
        assertThat(medido.minutosArriba()).isEqualTo(1);
    }
}

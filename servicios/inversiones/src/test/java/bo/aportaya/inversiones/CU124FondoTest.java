package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.aplicacion.CU124SolicitarRescate.EntradaRescate;
import bo.aportaya.inversiones.aplicacion.VistaRescate;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M6 · el rescate de CUOTAS de un fondo: corte informado, pendiente hasta confirmar,
 * doble disponibilidad (en la aplicacion y en la base, con concurrencia real), rechazo y aliado caido.
 */
class CU124FondoTest extends BaseDeRescates {

    // ------------------------------------------------------------------ fondo
    @Test
    @DisplayName(
            "Dado un fondo con plazo de rescate · Cuando solicita · Entonces informa el corte y queda PENDIENTE: el dinero no esta disponible hasta que el aliado confirme los fondos")
    void criterio6RescateDemorado() {
        UUID posicion = fondo1000();
        BigDecimal antes = libro.total(cuenta);

        VistaRescate r = cu124.solicitar(pedir(posicion, "4.000000"), ctx);

        assertThat(r.estado()).isEqualTo("PENDIENTE");
        assertThat(r.tipo()).isEqualTo("PARCIAL");
        assertThat(r.fechaValor())
                .as("el corte informado: lunes 08:00 La Paz, antes de las 15:00")
                .isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(r.mensaje()).contains("NO esta disponible");
        assertThat(libro.total(cuenta)).as("no se acredito nada").isEqualByComparingTo(antes);
        assertThat(cuotasDe(posicion))
                .as("las cuotas siguen en la posicion hasta que el aliado liquide")
                .isEqualByComparingTo("10.000000");
        assertThat(contar(
                        "select count(*)::int from inversiones.comprobante_inversion where posicion_inversion_id = ? and tipo = 'LIQUIDACION'",
                        posicion))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un pedido despues de la hora de corte · Cuando se solicita el rescate · Entonces informa el proximo dia habil; un viernes tarde, el lunes")
    void corteInformado() {
        UUID posicion = fondo1000();
        AHORA.set(Instant.parse("2026-10-12T20:00:00Z")); // lunes 16:00 en La Paz
        assertThat(cu124.solicitar(pedir(posicion, "1.000000"), ctx).fechaValor())
                .isEqualTo(LocalDate.of(2026, 10, 13));
        AHORA.set(Instant.parse("2026-10-16T20:00:00Z")); // viernes 16:00
        assertThat(cu124.solicitar(pedir(posicion, "1.000000"), ctx).fechaValor())
                .isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    @DisplayName(
            "Dadas cuotas ya comprometidas en otro rescate · Cuando se piden otra vez · Entonces AP-CU124-02; el borde (lo que queda libre) si pasa")
    void rechazoDeDobleDisponibilidad() {
        UUID posicion = fondo1000();
        cu124.solicitar(pedir(posicion, "7.000000"), ctx);

        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, "4.000000"), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-02"));
        assertThat(cu124.solicitar(pedir(posicion, "3.000000"), ctx).estado()).isEqualTo("PENDIENTE");
        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, "0.000001"), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-02"));
        assertThat(contar(
                        "select count(*)::int from inversiones.rescate_inversion where posicion_inversion_id = ?",
                        posicion))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("rechaza por R-INV-05: la doble disponibilidad aunque la aplicacion no la vea")
    void baseRechazaDobleDisponibilidad() {
        UUID posicion = fondo1000();
        tx.en(() -> datos.conContexto(
                ctx,
                d -> rescates.crear(
                        d,
                        posicion,
                        usuario,
                        "PARCIAL",
                        new BigDecimal("8.000000"),
                        UUID.randomUUID().toString(),
                        "h",
                        null,
                        java.time.OffsetDateTime.now())));
        assertThatThrownBy(() -> tx.en(() -> datos.conContexto(
                        ctx,
                        d -> rescates.crear(
                                d,
                                posicion,
                                usuario,
                                "PARCIAL",
                                new BigDecimal("2.000001"),
                                UUID.randomUUID().toString(),
                                "h",
                                null,
                                java.time.OffsetDateTime.now()))))
                .isInstanceOf(RescateRepositorio.DobleDisponibilidad.class);
        assertThat(contar(
                        "select count(*)::int from inversiones.rescate_inversion where posicion_inversion_id = ?",
                        posicion))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: dos rescates SIMULTANEOS que juntos superan las cuotas, solo uno entra")
    void concurrenciaRealSobreLasMismasCuotas() throws Exception {
        UUID posicion = fondo1000();
        var pool = Executors.newFixedThreadPool(2);
        var salida = new CountDownLatch(1);
        List<Future<String>> futuros = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futuros.add(pool.submit(() -> {
                salida.await();
                try {
                    return cu124.solicitar(pedir(posicion, "6.000000"), contextoDe(usuario))
                            .estado();
                } catch (ErrorDeNegocio e) {
                    return e.codigo().valor();
                }
            }));
        }
        salida.countDown();
        List<String> resultados = List.of(futuros.get(0).get(), futuros.get(1).get());
        pool.shutdown();

        assertThat(resultados).containsExactlyInAnyOrder("PENDIENTE", "AP-CU124-02");
        assertThat(dslFixtura
                        .fetchOne(
                                "select coalesce(sum(cuotas), 0) from inversiones.rescate_inversion where posicion_inversion_id = ? and estado <> 'RECHAZADO'",
                                posicion)
                        .get(0, BigDecimal.class))
                .isEqualByComparingTo("6.000000");
    }

    @Test
    @DisplayName(
            "Dado un aliado que rechaza el rescate · Cuando se solicita · Entonces queda RECHAZADO con su motivo y las cuotas vuelven a estar libres")
    void rechazoDelAliado() {
        UUID posicion = fondo1000();
        aliado.modo(AliadoDoble.Modo.RECHAZA);

        VistaRescate r = cu124.solicitar(pedir(posicion, "10.000000"), ctx);

        assertThat(r.estado()).isEqualTo("RECHAZADO");
        assertThat(r.motivoRechazo()).isEqualTo("RECHAZO_DE_PRUEBA");
        aliado.modo(AliadoDoble.Modo.NORMAL);
        assertThat(cu124.solicitar(pedir(posicion, "10.000000"), ctx).estado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName(
            "Dado el aliado CAIDO al pedir el rescate · Cuando se solicita · Entonces INCIERTO y las cuotas siguen comprometidas; al volver, se consulta y se reenvia con la misma referencia")
    void aliadoCaido() {
        UUID posicion = fondo1000();
        aliado.modo(AliadoDoble.Modo.CAIDO);

        VistaRescate incierto = cu124.solicitar(pedir(posicion, "10.000000"), ctx);
        assertThat(incierto.estado()).isEqualTo("INCIERTO");
        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, "1.000000"), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-02"));

        aliado.modo(AliadoDoble.Modo.NORMAL);
        VistaRescate resuelto = cu125.sincronizar(incierto.rescateId(), ctx);
        assertThat(resuelto.estado()).isEqualTo("PENDIENTE");
        assertThat(aliado.operacion(incierto.rescateId())).isPresent();
    }

    @Test
    @DisplayName(
            "Dados cuotas en cero o con mas de seis decimales, una posicion ajena o inexistente y una clave reutilizada con otro contenido · Cuando se solicita · Entonces se rechaza con su codigo y no se escribe nada")
    void entradasInvalidas() {
        UUID posicion = fondo1000();
        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, "0.000000"), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-04"));
        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, "1.0000001"), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-04"));
        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, "-1.000000"), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-04"));
        assertThatThrownBy(() -> cu124.solicitar(pedir(posicion, "1.000000"), contextoDe(UUID.randomUUID())))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-06"));
        assertThatThrownBy(() -> cu124.solicitar(pedir(UUID.randomUUID(), "1.000000"), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-06"));

        UUID clave = UUID.randomUUID();
        cu124.solicitar(new EntradaRescate(posicion, Optional.of(new BigDecimal("2.000000")), clave.toString()), ctx);
        assertThatThrownBy(() -> cu124.solicitar(
                        new EntradaRescate(posicion, Optional.of(new BigDecimal("3.000000")), clave.toString()), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU124-05"));
        assertThat(contar(
                        "select count(*)::int from inversiones.rescate_inversion where posicion_inversion_id = ?",
                        posicion))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: la misma clave dos veces devuelve el MISMO rescate y no vuelve a escribirle al aliado")
    void idempotenciaDelRescate() {
        UUID posicion = fondo1000();
        var entrada = pedir(posicion, "4.000000");

        VistaRescate a = cu124.solicitar(entrada, ctx);
        VistaRescate b = cu124.solicitar(entrada, ctx);

        assertThat(b.rescateId()).isEqualTo(a.rescateId());
        assertThat(aliado.envios(a.rescateId())).isEqualTo(1);
        assertThat(contar(
                        "select count(*)::int from inversiones.rescate_inversion where posicion_inversion_id = ?",
                        posicion))
                .isEqualTo(1);
    }
}

package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.trabajos.DevengoDiario;
import bo.aportaya.inversiones.trabajos.SincronizacionDelAliado;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M4 / M5 (cierre del hallazgo del carril D) · el devengo diario y las sincronizaciones con el
 * aliado ya no dependen de que alguien llame a la API: los dispara el reloj, con clave de idempotencia
 * reservada, y cada corrida queda visible. PostgreSQL real; aliado y libro dobles.
 */
class CU123TrabajosTest extends BaseDeAnalisis {

    private static final LocalDate DIA0_DE_LA_POSICION = LocalDate.of(2026, 10, 12);

    private UUID dpf10000() {
        conSaldo("20000.00");
        return posicionConfirmada(PRODUCTO_DPF, "10000.00");
    }

    private int devengosDe(UUID posicion) {
        return contar("select count(*)::int from inversiones.devengo_dpf where posicion_inversion_id = ?", posicion);
    }

    @Test
    @DisplayName(
            "Dado un DPF abierto · Cuando corre el trabajo de devengo diario · Entonces devenga la fecha de hoy una sola vez y deja su corrida PROGRAMADO visible")
    void elTrabajoDevenga() {
        UUID posicion = dpf10000();
        AHORA.set(LUNES.plusSeconds(30L * 86400));
        var trabajo = new DevengoDiario(operaciones, RELOJ);
        String clave = "DEV:" + RELOJ.hoy();

        trabajo.correr();
        trabajo.correr(); // un segundo disparo del mismo dia (otra replica, un reinicio): no devenga de nuevo

        assertThat(devengosDe(posicion)).isEqualTo(1);
        // 10000 x 4 % x 30 / 365 = 32,8767... -> 32,88
        assertThat(dslFixtura
                        .fetchOne(
                                "select interes_acumulado from inversiones.devengo_dpf where posicion_inversion_id = ? and fecha = ?",
                                posicion,
                                DIA0_DE_LA_POSICION.plusDays(30))
                        .get(0, java.math.BigDecimal.class))
                .isEqualByComparingTo("32.88");
        assertThat(contar("select count(*)::int from inversiones.corrida_trabajo where clave_idempotencia = ?", clave))
                .isEqualTo(1);
        assertThat(deLaCorrida(clave, "origen")).isEqualTo("PROGRAMADO");
        assertThat(deLaCorrida(clave, "estado")).isEqualTo("OK");
        assertThat(operaciones.corridas("DEVENGAR", 200, sistema()))
                .extracting(c -> c.clave())
                .contains(clave);
    }

    @Test
    @DisplayName(
            "Dado el aliado caido · Cuando corren las sincronizaciones programadas · Entonces las corridas quedan FALLIDAS con su codigo y los trabajos no se caen, y al volver el aliado se retoman con la misma clave")
    void lasSincronizacionesSobrevivenAlAliadoCaido() {
        // Un instante que ninguna otra prueba uso: las claves del reloj son las del dia y la hora.
        AHORA.set(LUNES.plusSeconds(3600L * ThreadLocalRandom.current().nextInt(24 * 400, 24 * 4000)));
        var trabajo = new SincronizacionDelAliado(operaciones, RELOJ);
        String hora = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH")
                .format(java.time.ZonedDateTime.ofInstant(AHORA.get(), bo.aportaya.plataforma.dominio.Reloj.ZONA));
        String dia = hora.substring(0, 8);
        aliado.modo(AliadoDoble.Modo.CAIDO);

        trabajo.catalogo();
        trabajo.valores();

        assertThat(deLaCorrida("CAT:" + dia, "estado")).isEqualTo("FALLIDA");
        assertThat(deLaCorrida("CAT:" + dia, "error")).isEqualTo("AP-CU120-02");
        assertThat(deLaCorrida("VAL:" + hora, "estado")).isEqualTo("FALLIDA");
        assertThat(deLaCorrida("VAL:" + hora, "error")).isEqualTo("AP-CU123-02");

        aliado.modo(AliadoDoble.Modo.NORMAL);
        aliado.publicar(productoFondo, DIA0_DE_LA_POSICION, "101.000000");
        trabajo.catalogo();
        trabajo.valores();

        assertThat(deLaCorrida("CAT:" + dia, "estado")).isEqualTo("OK");
        assertThat(deLaCorrida("VAL:" + hora, "estado")).isEqualTo("OK");
        assertThat(contar(
                        "select count(*)::int from inversiones.corrida_trabajo where clave_idempotencia in (?, ?)",
                        "CAT:" + dia,
                        "VAL:" + hora))
                .as("las fallidas se retomaron: no hay filas de mas")
                .isEqualTo(2);
        assertThat(contar(
                        "select count(*)::int from inversiones.valor_cuota vc join inversiones.producto_inversion p on p.id = vc.producto_inversion_id where p.codigo = ?",
                        productoFondo))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "reintento: la clave del devengo repetida con la misma fecha devuelve lo guardado y no devenga otra vez")
    void devengoConClave() {
        UUID posicion = dpf10000();
        String clave = clave();

        var primera = operaciones.devengar(DIA0_DE_LA_POSICION.plusDays(10), clave, "API", sistema());
        var segunda = operaciones.devengar(DIA0_DE_LA_POSICION.plusDays(10), clave, "API", sistema());

        assertThat(segunda).isEqualTo(primera);
        assertThat(devengosDe(posicion)).isEqualTo(1);
        assertThat(contar("select count(*)::int from inversiones.corrida_trabajo where clave_idempotencia = ?", clave))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "rechaza: la clave del devengo con OTRA fecha es AP-CU123-03, y la de la sincronizacion de valores tambien cuenta como usada")
    void claveConOtroContenido() {
        dpf10000();
        String clave = clave();
        operaciones.devengar(DIA0_DE_LA_POSICION.plusDays(10), clave, "API", sistema());

        assertThatThrownBy(() -> operaciones.devengar(DIA0_DE_LA_POSICION.plusDays(11), clave, "API", sistema()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU123-03"));

        // La clave se reserva por operacion: la misma clave en OTRA operacion es una corrida distinta.
        String compartida = clave();
        operaciones.sincronizarValores(compartida, "API", sistema());
        assertThat(operaciones.devengar(DIA0_DE_LA_POSICION.plusDays(12), compartida, "API", sistema()))
                .containsKey("devengadas");
    }

    @Test
    @DisplayName(
            "reintento: la sincronizacion de valores con la misma clave devuelve lo guardado aunque el aliado ya publique otro valor")
    void valoresConClave() {
        aliado.publicar(productoFondo, DIA0_DE_LA_POSICION, "101.000000");
        String clave = clave();

        var primera = operaciones.sincronizarValores(clave, "API", sistema());
        aliado.publicar(productoFondo, DIA0_DE_LA_POSICION.plusDays(1), "102.000000");
        var repetida = operaciones.sincronizarValores(clave, "API", sistema());
        var otraClave = operaciones.sincronizarValores(clave(), "API", sistema());

        assertThat(repetida).isEqualTo(primera);
        assertThat(otraClave.get("nuevos"))
                .as("una clave nueva si ve el valor nuevo")
                .isNotEqualTo("0");
    }
}

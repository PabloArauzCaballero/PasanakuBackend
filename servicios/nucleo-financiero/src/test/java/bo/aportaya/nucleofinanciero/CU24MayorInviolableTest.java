package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-24 · F1.S1.M1 (TYA-DINERO-001): el mayor rechaza líneas negativas, de dos lados, tardías y
 * asientos en firme sin líneas. Se ejercita la base saltándose {@code CU24RegistrarAsiento} a
 * propósito: {@code Partida} ya rechaza lo mismo en el dominio, y una prueba que pasa porque la
 * aplicación validó antes no probó la restricción (`pruebas-cu`).
 */
class CU24MayorInviolableTest extends BaseDeCU24 {

    private static final String ASIENTO =
            """
            INSERT INTO nucleo_financiero.asiento_contable
                (id, fecha, glosa, origen_tipo, origen_id, estado)
            VALUES (?, now(), ?, 'AJUSTE', gen_random_uuid(), 'CONFIRMADO')
            """;

    private static final String LINEA =
            """
            INSERT INTO nucleo_financiero.movimiento_contable
                (id, asiento_id, cuenta_id, debe, haber, descripcion)
            VALUES (gen_random_uuid(), ?, ?, ?::numeric, ?::numeric, 'linea de prueba')
            """;

    @Test
    @DisplayName("rechaza R-AUD-13: una línea del mayor con importe negativo no entra")
    void rechazaLineaNegativa() {
        UUID asientoId = UUID.randomUUID();
        UUID cuenta = fixtura.cuentaDeMovimiento(codigoCorto(), "ACTIVO", "DEUDORA");

        assertThatThrownBy(() -> transaccion.execute(estado -> {
                    dsl.execute(ASIENTO, asientoId, "negativa");
                    dsl.execute(LINEA, asientoId, cuenta, "-10.00", "0.00");
                    return null;
                }))
                .satisfies(e -> assertThat(raizDe(e)).contains("ck_movimiento_contable_un_lado"));
    }

    @Test
    @DisplayName("rechaza R-AUD-13: una línea con debe y haber a la vez no entra")
    void rechazaLineaDeDosLados() {
        UUID asientoId = UUID.randomUUID();
        UUID cuenta = fixtura.cuentaDeMovimiento(codigoCorto(), "ACTIVO", "DEUDORA");

        assertThatThrownBy(() -> transaccion.execute(estado -> {
                    dsl.execute(ASIENTO, asientoId, "dos lados");
                    dsl.execute(LINEA, asientoId, cuenta, "20.00", "20.00");
                    return null;
                }))
                .satisfies(e -> assertThat(raizDe(e)).contains("ck_movimiento_contable_un_lado"));
    }

    @Test
    @DisplayName("rechaza R-AUD-14: un asiento ya confirmado no recibe una línea nueva desde otra transacción")
    void rechazaLineaTardia() {
        UUID caja = fixtura.cuentaDeMovimiento(codigoCorto(), "ACTIVO", "DEUDORA");
        UUID ingreso = fixtura.cuentaDeMovimiento(codigoCorto(), "INGRESO", "ACREEDORA");
        UUID asientoId = asientoCuadrado(caja, ingreso, "100.00", "confirmado y cerrado");

        // Una línea suelta lo descuadraría; un par cuadrado cambiaría saldos sin asiento nuevo.
        // Las dos se rechazan: corregir un asiento en firme es un contra-asiento.
        String suelta = rechazaLaBase(LINEA, asientoId, caja, "50.00", "0.00");
        assertThat(suelta).contains("R-AUD-14");

        assertThatThrownBy(() -> transaccion.execute(estado -> {
                    dsl.execute(LINEA, asientoId, caja, "30.00", "0.00");
                    dsl.execute(LINEA, asientoId, ingreso, "0.00", "30.00");
                    return null;
                }))
                .satisfies(e -> assertThat(raizDe(e)).contains("R-AUD-14"));

        assertThat(dsl.fetchOne(
                                "SELECT count(*)::int FROM nucleo_financiero.movimiento_contable WHERE asiento_id = ?",
                                asientoId)
                        .get(0, Integer.class))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("rechaza R-AUD-14: adelantar las restricciones diferidas no habilita una línea que descuadra")
    void rechazaDescuadreTrasAdelantarRestricciones() {
        UUID asientoId = UUID.randomUUID();
        UUID caja = fixtura.cuentaDeMovimiento(codigoCorto(), "ACTIVO", "DEUDORA");
        UUID ingreso = fixtura.cuentaDeMovimiento(codigoCorto(), "INGRESO", "ACREEDORA");

        // Los triggers del encabezado corren al SET CONSTRAINTS y pasan; la línea de después
        // tiene que volver a exigir el cuadre desde su propio trigger.
        assertThatThrownBy(() -> transaccion.execute(estado -> {
                    dsl.execute(ASIENTO, asientoId, "adelanto");
                    dsl.execute(LINEA, asientoId, caja, "100.00", "0.00");
                    dsl.execute(LINEA, asientoId, ingreso, "0.00", "100.00");
                    dsl.execute("SET CONSTRAINTS ALL IMMEDIATE");
                    dsl.execute(LINEA, asientoId, caja, "999.00", "0.00");
                    return null;
                }))
                .satisfies(e -> assertThat(raizDe(e)).contains("R-AUD-05"));
    }

    @Test
    @DisplayName("rechaza R-AUD-05: un asiento en firme sin líneas no se confirma")
    void rechazaAsientoSinLineas() {
        UUID asientoId = UUID.randomUUID();

        assertThatThrownBy(() -> transaccion.execute(estado -> {
                    dsl.execute(ASIENTO, asientoId, "sin lineas");
                    return null;
                }))
                .satisfies(e -> assertThat(raizDe(e)).contains("R-AUD-05"));
    }

    @Test
    @DisplayName("cuadre: las líneas insertadas tras un SAVEPOINT de la misma transacción entran")
    void aceptaLineasTrasSavepoint() {
        UUID asientoId = UUID.randomUUID();
        UUID caja = fixtura.cuentaDeMovimiento(codigoCorto(), "ACTIVO", "DEUDORA");
        UUID ingreso = fixtura.cuentaDeMovimiento(codigoCorto(), "INGRESO", "ACREEDORA");

        // El xid de una subtransacción es mayor que el de la principal: R-AUD-14 no puede
        // confundirlo con una transacción ajena.
        transaccion.execute(estado -> {
            dsl.execute("SAVEPOINT s1");
            dsl.execute(ASIENTO, asientoId, "con savepoint");
            dsl.execute("RELEASE SAVEPOINT s1");
            dsl.execute("SAVEPOINT s2");
            dsl.execute(LINEA, asientoId, caja, "5.00", "0.00");
            dsl.execute(LINEA, asientoId, ingreso, "0.00", "5.00");
            dsl.execute("RELEASE SAVEPOINT s2");
            return null;
        });

        assertThat(dsl.fetchOne(
                                "SELECT count(*)::int FROM nucleo_financiero.movimiento_contable WHERE asiento_id = ?",
                                asientoId)
                        .get(0, Integer.class))
                .isEqualTo(2);
    }
}

package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.inversiones.aplicacion.VistaOrden;
import bo.aportaya.inversiones.trabajos.ReintentoDeInstrucciones;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La saga de la inversion terminada por el RELOJ, no por la persona: el libro se cae en dos pasos distintos
 * y el trabajo programado de reintento completa cada uno cuando el libro vuelve, con la misma clave de
 * idempotencia, sin mover la plata dos veces. PostgreSQL real; aliado y libro dobles.
 */
class ReintentoDelRelojSagaTest extends BaseDeAnalisis {

    private ReintentoDeInstrucciones trabajo() {
        return new ReintentoDeInstrucciones(operaciones, RELOJ);
    }

    /** Un instante que ninguna otra prueba uso: la clave del trabajo es la del minuto y la base se comparte. */
    private void unMinutoPropio() {
        AHORA.set(LUNES.plusSeconds(60L * ThreadLocalRandom.current().nextInt(300000, 600000)));
    }

    private String estadoDeLaInstruccion(UUID origen, String tipo) {
        return dslFixtura
                .fetchOne(
                        "select estado from inversiones.instruccion_libro where origen_id = ? and tipo = ?",
                        origen,
                        tipo)
                .get(0, String.class);
    }

    @Test
    @DisplayName(
            "Suscripcion de punta a punta con el libro caido al retener y al debitar: el reloj completa cada paso, la plata sale UNA vez y nada queda pendiente")
    void suscripcionTerminadaPorElReloj() {
        unMinutoPropio();
        conSaldo("5000.00");
        libro.fallarEn("retener", true);
        VistaOrden creada = ordenar(PRODUCTO_DPF, "1000.00");
        assertThat(creada.estado()).isEqualTo("CREADA");

        libro.fallarEn("retener", false);
        AHORA.set(AHORA.get().plusSeconds(200));
        trabajo().correr(); // aplica RETENER
        assertThat(estadoDeLaInstruccion(creada.ordenId(), "RETENER")).isEqualTo("APLICADA");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("1000.00");

        libro.fallarEn("debitar", true);
        VistaOrden confirmada = cu122.sincronizar(creada.ordenId(), ctx);
        assertThat(confirmada.posicionId())
                .as("la posicion existe aunque el debito no llego")
                .isNotNull();
        assertThat(libro.retenido(cuenta))
                .as("sigue retenido: no hay doble disponibilidad")
                .isEqualByComparingTo("1000.00");
        assertThat(estadoDeLaInstruccion(creada.ordenId(), "DEBITAR")).isEqualTo("PENDIENTE");

        libro.fallarEn("debitar", false);
        AHORA.set(AHORA.get().plusSeconds(3 * 3600));
        trabajo().correr(); // aplica DEBITAR
        trabajo().correr(); // la misma ventana: no repite

        assertThat(estadoDeLaInstruccion(creada.ordenId(), "DEBITAR")).isEqualTo("APLICADA");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("4000.00");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");
        assertThat(libro.asientos()).hasSize(1);
        assertThat(libro.clavesAplicadas())
                .containsOnlyOnce("RETENER:" + creada.ordenId(), "DEBITAR:" + creada.ordenId());
        assertThat(operaciones.contarPendientes(0)).isZero();
    }

    @Test
    @DisplayName(
            "Rescate de punta a punta con el libro caido al acreditar: queda POR_ACREDITAR y el reloj lo acredita UNA vez cuando el libro vuelve")
    void rescateTerminadoPorElReloj() {
        unMinutoPropio();
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");
        aliado.rescateFondoInmediato(true).valorCuotaAplicado("100.000000");
        libro.fallarEn("acreditar", true);

        var rescate = cu124.solicitar(pedir(posicion, null), ctx);
        assertThat(rescate.estado()).isEqualTo("POR_ACREDITAR");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("19000.00");

        libro.fallarEn("acreditar", false);
        AHORA.set(AHORA.get().plusSeconds(3 * 3600));
        trabajo().correr();

        assertThat(estadoDeLaInstruccion(rescate.rescateId(), "ACREDITAR")).isEqualTo("APLICADA");
        assertThat(cu125.ver(rescate.rescateId(), ctx).estado()).isEqualTo("LIQUIDADO");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("20000.00");
        assertThat(libro.clavesAplicadas()).containsOnlyOnce("ACREDITAR:" + rescate.rescateId());
    }
}

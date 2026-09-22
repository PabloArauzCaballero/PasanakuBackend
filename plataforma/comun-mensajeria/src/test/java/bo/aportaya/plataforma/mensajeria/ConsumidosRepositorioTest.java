package bo.aportaya.plataforma.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H2.S2.M5 — el consumidor de prueba aplica {@link Consumidos#registrar}: una entrega doble
 * (el transporte es al menos una vez, {@code docs/auditoria-produccion/contratos/evento-kafka.md})
 * no repite el efecto. Nombrada {@code *RepositorioTest} (no el {@code ConsumidosTest} literal
 * del encargo) para caer en el corredor {@code integrationTest} contra PostgreSQL real — mismo
 * ajuste de nombre que ya hizo falta para {@code RelevoRepositorioTest}.
 */
class ConsumidosRepositorioTest extends BaseDePruebaMensajeria {

    private final Consumidos consumidos = new Consumidos(ESQUEMA);

    @AfterEach
    void limpiarConsumidos() {
        dsl.deleteFrom(DSL.table(DSL.name(ESQUEMA, "evento_consumido"))).execute();
    }

    @Test
    @DisplayName("primera vez: registra y devuelve true")
    void primeraVezRegistra() {
        UUID idEvento = UUID.randomUUID();

        boolean esNueva = consumidos.registrar(dsl, idEvento, "consumidor-de-prueba");

        assertThat(esNueva).isTrue();
        assertThat(filaDeConsumido(idEvento, "consumidor-de-prueba")).isNotNull();
    }

    @Test
    @DisplayName("entrega doble del mismo event_id al mismo consumidor: no repite el efecto")
    void entregaDobleNoRepiteElEfecto() {
        UUID idEvento = UUID.randomUUID();

        boolean primera = consumidos.registrar(dsl, idEvento, "consumidor-de-prueba");
        boolean segunda = consumidos.registrar(dsl, idEvento, "consumidor-de-prueba");

        assertThat(primera).isTrue();
        assertThat(segunda)
                .as("la segunda entrega del mismo event_id no debe repetir el efecto")
                .isFalse();
        assertThat(dsl.selectCount()
                        .from(DSL.table(DSL.name(ESQUEMA, "evento_consumido")))
                        .where(DSL.field("id_evento").eq(idEvento))
                        .fetchOne(0, Integer.class))
                .as("una sola marca, aunque se haya intentado registrar dos veces")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("dos consumidores distintos del mismo evento son independientes")
    void dosConsumidoresSonIndependientes() {
        UUID idEvento = UUID.randomUUID();

        boolean paraA = consumidos.registrar(dsl, idEvento, "consumidor-a");
        boolean paraB = consumidos.registrar(dsl, idEvento, "consumidor-b");

        assertThat(paraA).isTrue();
        assertThat(paraB).isTrue();
    }

    private org.jooq.Record filaDeConsumido(UUID idEvento, String consumidor) {
        return dsl.select(DSL.field("id_evento"), DSL.field("consumidor"), DSL.field("consumido_en"))
                .from(DSL.table(DSL.name(ESQUEMA, "evento_consumido")))
                .where(DSL.field("id_evento").eq(idEvento))
                .and(DSL.field("consumidor").eq(consumidor))
                .fetchOne();
    }
}

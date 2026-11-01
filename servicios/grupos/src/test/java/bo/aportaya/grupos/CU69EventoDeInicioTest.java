package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * R-GRP-20 · Al iniciar el grupo se publica {@code grupos.grupo_iniciado} por outbox, en la misma transaccion, con la
 * carga del pacto {@code contratos/pactos/grupos-aportes-grupo-iniciado.json}: lo que {@code aportes} necesita para abrir
 * la cuota 1 de cada cupo. Tres niveles: correcto (8 de 10), limite (reintento con la misma clave y con otra) e invalido
 * (no alcanza el minimo: no hay inicio y no hay evento).
 */
class CU69EventoDeInicioTest extends BaseDeInicio {

    private static final String TIPO = "grupos.grupo_iniciado";

    private int eventos(UUID grupo) {
        return contar(
                "SELECT count(*)::int FROM grupos.evento_dominio WHERE agregado_id = ? AND tipo = ?", grupo, TIPO);
    }

    private String carga(UUID grupo, String ruta) {
        return (String) dsl.fetchOne(
                        "SELECT payload #>> ? FROM grupos.evento_dominio WHERE agregado_id = ? AND tipo = ? LIMIT 1",
                        (Object) ruta.split("\\."),
                        grupo,
                        TIPO)
                .get(0);
    }

    @Test
    @DisplayName(
            "Dado el caso correcto de un grupo con 8 de 10 cupos ocupados · "
                    + "Cuando el organizador lo inicia · "
                    + "Entonces se emite un solo grupos.grupo_iniciado con el periodo 1 (id y fechas), los 8 cupos OCUPADO numerados del 1 al 8 con titular y Bs 500,00, la moneda y el total achicado")
    void correcto() {
        UUID grupo = grupoAbierto(10, del1Al(8), Set.of());

        iniciar(grupo, UUID.randomUUID(), organizador);

        assertThat(eventos(grupo)).isEqualTo(1);
        assertThat(carga(grupo, "grupoId")).isEqualTo(grupo.toString());
        assertThat(carga(grupo, "moneda")).isEqualTo("BOB");
        assertThat(carga(grupo, "cuposTotales")).isEqualTo("8");
        assertThat(carga(grupo, "iniciadoEn")).isNotBlank();
        UUID periodoUno = (UUID) dsl.fetchOne("SELECT id FROM grupos.periodo WHERE grupo_id = ? AND numero = 1", grupo)
                .get(0);
        assertThat(carga(grupo, "periodo.periodoId")).isEqualTo(periodoUno.toString());
        assertThat(carga(grupo, "periodo.numero")).isEqualTo("1");
        assertThat(carga(grupo, "periodo.fechaLimitePago")).matches("\\d{4}-\\d{2}-\\d{2}");
        assertThat(carga(grupo, "periodo.fechaFinGracia")).matches("\\d{4}-\\d{2}-\\d{2}");
        assertThat(carga(grupo, "periodo.fechaEntregaPrevista")).matches("\\d{4}-\\d{2}-\\d{2}");
        assertThat(contar(
                        "SELECT jsonb_array_length(payload -> 'cupos') FROM grupos.evento_dominio WHERE agregado_id = ? AND tipo = ?",
                        grupo,
                        TIPO))
                .isEqualTo(8);
        // Cada cupo del evento es un cupo OCUPADO real del grupo, con su titular y su aporte como cadena de dos
        // decimales.
        assertThat(contar(
                        """
                        SELECT count(*)::int
                          FROM grupos.evento_dominio e, jsonb_array_elements(e.payload -> 'cupos') c
                          JOIN grupos.cupo k ON k.id = (c ->> 'cupoId')::uuid
                         WHERE e.agregado_id = ? AND e.tipo = ?
                           AND k.estado = 'OCUPADO' AND k.numero = (c ->> 'numero')::int
                           AND k.participante_id = (c ->> 'participanteId')::uuid
                           AND c ->> 'montoAporte' = '500.00'
                        """,
                        grupo,
                        TIPO))
                .isEqualTo(8);
        assertThat(contar(
                        "SELECT (payload -> 'cupos' -> 0 ->> 'numero')::int FROM grupos.evento_dominio WHERE agregado_id = ? AND tipo = ?",
                        grupo,
                        TIPO))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado el caso limite de un grupo ya iniciado con una clave · "
                    + "Cuando el inicio se repite con la misma clave y despues con otra · "
                    + "Entonces la misma clave no re-emite, la otra responde AP-CU69-17 sin emitir y queda un solo grupos.grupo_iniciado")
    void limiteIdempotente() {
        UUID grupo = grupoAbierto(4, del1Al(3), Set.of());
        UUID clave = UUID.randomUUID();

        iniciar(grupo, clave, organizador);
        iniciar(grupo, clave, organizador);
        assertThatThrownBy(() -> iniciar(grupo, UUID.randomUUID(), organizador))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU69-17"));

        assertThat(eventos(grupo)).isEqualTo(1);
    }

    @Test
    @DisplayName("Dado el caso invalido de un grupo con 7 de 10 cupos ocupados · "
            + "Cuando el organizador lo inicia · "
            + "Entonces recibe AP-CU69-16 y no se emite ningun grupos.grupo_iniciado")
    void invalidoSinMinimo() {
        UUID grupo = grupoAbierto(10, del1Al(7), Set.of());

        assertThatThrownBy(() -> iniciar(grupo, UUID.randomUUID(), organizador))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU69-16"));

        assertThat(eventos(grupo)).isZero();
    }
}

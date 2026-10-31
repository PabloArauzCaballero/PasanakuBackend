package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · El inicio al 75 % bajo carrera y con el rol de base real del servicio (2026-10-30). Sobre PostgreSQL real;
 * datos sinteticos.
 */
class CU69IniciarGrupoConcurrenciaTest extends BaseDeInicio {

    @Test
    @DisplayName("Dado un grupo con 8 de 10 cupos ocupados · "
            + "Cuando se piden dos inicios a la vez con claves distintas · "
            + "Entonces uno queda CONFORMADO, el otro recibe AP-CU69-17 y hay un solo achique")
    void dosALaVez() throws Exception {
        UUID grupo = grupoAbierto(10, del1Al(8), Set.of());
        var hilos = Executors.newFixedThreadPool(2);
        try {
            List<Callable<String>> pedidos = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                pedidos.add(() -> {
                    try {
                        return iniciar(grupo, UUID.randomUUID(), organizador).estado();
                    } catch (ErrorDeNegocio e) {
                        return e.codigo().valor();
                    }
                });
            }
            List<String> resultados = new ArrayList<>();
            for (Future<String> f : hilos.invokeAll(pedidos)) resultados.add(f.get());
            assertThat(resultados).containsExactlyInAnyOrder("CONFORMADO", "AP-CU69-17");
        } finally {
            hilos.shutdownNow();
        }
        assertThat(contar("SELECT count(*) FROM grupos.historial_estado_grupo WHERE grupo_id = ?", grupo))
                .isEqualTo(1);
        assertThat(grupo(grupo)).containsEntry("cupos_totales", (short) 8);
    }

    @Test
    @DisplayName("Dado un grupo con 8 de 10 cupos ocupados · "
            + "Cuando el organizador lo inicia con el rol de base del servicio (svc_grupos) · "
            + "Entonces queda con 8 cupos sin necesitar borrar filas y los 10 cupos siguen existiendo")
    void conElRolDelServicio() {
        UUID grupo = grupoAbierto(10, del1Al(8), Set.of());

        var r = transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return inicio.iniciar(grupo, UUID.randomUUID(), de(organizador));
        });

        assertThat(r.cuposTotales()).isEqualTo(8);
        assertThat(contar("SELECT count(*) FROM grupos.cupo WHERE grupo_id = ?", grupo))
                .isEqualTo(10);
    }
}

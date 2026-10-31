package bo.aportaya.grupos.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * La etapa por {@code transparencia}, niveles correcto y limite, contra el doble HTTP de {@link BaseDeEtapasPorHttp}.
 * El nivel invalido esta en {@link EtapasPorHttpInvalidoTest}.
 */
class EtapasPorHttpTest extends BaseDeEtapasPorHttp {

    // --- correcto ---

    @Test
    void laEtapaDeUnaPersonaSePideConElTokenDeServicioYNuncaElPuntaje() {
        catalogo("200", CATALOGO);
        personas("200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]");

        var persona = hechos().etapaDe(USUARIO).orElseThrow();

        assertThat(persona.etapa().codigo()).isEqualTo("MEDIA");
        // Grupos no conoce el puntaje exacto: compara con el piso de la etapa (tramos contiguos).
        assertThat(persona.puntaje()).isEqualByComparingTo("500.00");
        assertThat(persona.etapa().desbloqueo().vigente()).isTrue();
        assertThat(persona.catalogo()).extracting(a -> a.codigo()).containsExactly("BAJA", "MEDIA", "ALTA");
        assertThat(autorizaciones).containsExactly("Bearer " + TOKEN_DE_SERVICIO);
        assertThat(idsPedidos).containsExactly(List.of(USUARIO.toString()));
        assertThat(rutas).noneMatch(r -> r.endsWith("/puntaje"));
    }

    @Test
    void elDetallePideLasEtapasDeTodosEnUnaSolaConsultaYTraeCodigoYNombre() {
        UUID otra = UUID.randomUUID();
        personas(
                "200",
                "[" + persona(USUARIO, true, "MEDIA", "500.00") + ","
                        + "{\"usuarioId\":\"" + otra + "\",\"tieneHistorial\":false,"
                        + "\"etapa\":{\"codigo\":\"ALTA\",\"nombre\":\"Etapa 2 · Alta\",\"orden\":2,\"desde\":\"700.00\"}}]");

        var etapas = hechos().etapasDe(List.of(USUARIO, otra)).orElseThrow();

        assertThat(etapas).hasSize(2);
        assertThat(etapas.get(otra).codigo()).isEqualTo("ALTA");
        assertThat(etapas.get(otra).nombre()).isEqualTo("Etapa 2 · Alta");
        assertThat(idsPedidos).hasSize(1);
        assertThat(idsPedidos.get(0)).containsExactly(USUARIO.toString(), otra.toString());
        // El catalogo no hace falta para el detalle: la ruta de servicio ya trae codigo y nombre.
        assertThat(rutas).containsExactly(DE_PERSONAS);
    }

    @Test
    void elCatalogoSeLeeSinPreguntarPorNingunaPersona() {
        catalogo("200", CATALOGO);
        assertThat(hechos().catalogoDeEtapas().orElseThrow())
                .extracting(a -> a.codigo())
                .containsExactly("BAJA", "MEDIA", "ALTA");
        assertThat(autorizaciones).isEmpty();
    }

    // --- limite ---

    @Test
    void sinHistorialCuentaElPisoDeLaEtapaBaseNoUnCero() {
        catalogo("200", CATALOGO);
        personas("200", "[" + persona(USUARIO, false, "MEDIA", "500.00") + "]");

        var persona = hechos().etapaDe(USUARIO).orElseThrow();

        assertThat(persona.puntaje()).isEqualByComparingTo("500.00");
        assertThat(persona.etapa().codigo()).isEqualTo("MEDIA");
    }

    @Test
    void masDeSesentaPersonasVanEnTandasDeSesentaYLasRepetidasUnaVez() {
        etapaEco = "MEDIA";
        List<UUID> pedidos = new ArrayList<>(
                IntStream.range(0, 61).mapToObj(i -> UUID.randomUUID()).toList());
        pedidos.add(pedidos.get(0));
        Collections.shuffle(pedidos.subList(1, 61));

        var etapas = hechos().etapasDe(pedidos).orElseThrow();

        assertThat(etapas).hasSize(61);
        assertThat(idsPedidos).extracting(List::size).containsExactly(60, 1);
    }

    @Test
    void quienNoTieneEtapaEnLaRespuestaQuedaAfueraSinTirarALosDemas() {
        UUID sinEtapa = UUID.randomUUID();
        personas(
                "200",
                "[" + persona(USUARIO, true, "MEDIA", "500.00") + ",{\"usuarioId\":\"" + sinEtapa
                        + "\",\"tieneHistorial\":false}]");

        var etapas = hechos().etapasDe(List.of(USUARIO, sinEtapa)).orElseThrow();

        assertThat(etapas).containsOnlyKeys(USUARIO);
    }

    @Test
    void sinNadieQuePreguntarNoSaleNingunaPeticion() {
        assertThat(hechos().etapasDe(List.of()).orElseThrow()).isEmpty();
        assertThat(rutas).isEmpty();
    }
}

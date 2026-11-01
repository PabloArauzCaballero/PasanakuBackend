package bo.aportaya.grupos.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * La etapa por {@code transparencia}, nivel invalido, contra el doble HTTP de {@link BaseDeEtapasPorHttp}: ante
 * cualquier duda la respuesta es vacia (quien decide un ingreso no deja pasar). Partida de {@link EtapasPorHttpTest}.
 */
class EtapasPorHttpInvalidoTest extends BaseDeEtapasPorHttp {

    // --- invalido ---

    @Test
    void sinCredencialDeServicioNoSaleNingunaPeticionYLaRespuestaEsVacia() {
        catalogo("200", CATALOGO);
        personas("200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]");

        for (CredencialDeServicio sin : List.of(CredencialDeServicio.ninguna(), CredencialDeServicio.fija("  "))) {
            assertThat(hechos(sin).etapaDe(USUARIO)).isEmpty();
            assertThat(hechos(sin).etapasDe(List.of(USUARIO))).isEmpty();
        }
        assertThat(autorizaciones).as("ni siquiera con el token de la sesion").isEmpty();
    }

    @Test
    void conUnTokenQueTransparenciaNoAceptaComoServicioLaRespuestaEsVacia() {
        catalogo("200", CATALOGO);
        personas("200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]");

        assertThat(hechos(CredencialDeServicio.fija("token-de-otra-cosa")).etapaDe(USUARIO))
                .isEmpty();
    }

    @Test
    void caidoLentoIlegibleOConUnaEtapaQueNoEstaEnElCatalogoEsVacio() {
        catalogo("200", CATALOGO);
        var hechos = hechos();

        personas("503", "{}");
        assertThat(hechos.etapaDe(USUARIO)).as("5xx").isEmpty();
        personas("200", "[" + persona(USUARIO, true, "OTRA", "500.00") + "]");
        assertThat(hechos.etapaDe(USUARIO)).as("etapa desconocida").isEmpty();
        personas("200", "{\"no\":\"es una lista\"}");
        assertThat(hechos.etapasDe(List.of(USUARIO))).as("cuerpo ilegible").isEmpty();
        personas("200", "[" + persona(UUID.randomUUID(), true, "MEDIA", "500.00") + "]");
        assertThat(hechos.etapasDe(List.of(USUARIO)))
                .as("alguien que no se pidio")
                .isEmpty();
        respuestas.put(
                DE_PERSONAS, new String[] {"200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]", "1200"});
        assertThat(hechos.etapaDe(USUARIO)).as("transparencia lenta").isEmpty();

        servidor.stop(0);
        assertThat(hechos.etapaDe(USUARIO)).as("red caida").isEmpty();
        servidor = null;
    }

    @Test
    void sinCatalogoVacioOIlegibleLaEtapaParaDecidirEsVacia() {
        personas("200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]");
        var hechos = hechos();

        catalogo("404", "{}");
        assertThat(hechos.etapaDe(USUARIO)).as("transparencia sin catalogo").isEmpty();
        catalogo("200", "[]");
        assertThat(hechos.etapaDe(USUARIO)).as("catalogo vacio").isEmpty();
        catalogo("200", CATALOGO.replace("\"500.00\",\"moneda\"", "\"quinientos\",\"moneda\""));
        assertThat(hechos.etapaDe(USUARIO)).as("un importe que no es decimal").isEmpty();
        catalogo("500", "{}");
        assertThat(hechos.etapaDe(USUARIO)).as("transparencia con 5xx").isEmpty();
    }

    @Test
    void elDesbloqueoPropuestoLlegaPeroNoEsVigente() {
        catalogo("200", CATALOGO.replace("\"VIGENTE\"", "\"PROPUESTO\""));
        personas("200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]");

        var desbloqueo = hechos().etapaDe(USUARIO).orElseThrow().etapa().desbloqueo();

        assertThat(desbloqueo.vigente()).isFalse();
        assertThat(desbloqueo.cuposMax()).isEqualTo(10);
        assertThat(new BigDecimal("500.00")).isEqualByComparingTo(desbloqueo.aporteMax());
    }
}

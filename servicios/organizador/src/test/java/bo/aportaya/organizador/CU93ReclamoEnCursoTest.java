package bo.aportaya.organizador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.organizador.aplicacion.CU93SancionarOrganizador.EntradaSancion;
import bo.aportaya.organizador.aplicacion.ReclamosEnCurso;
import bo.aportaya.organizador.aplicacion.UsuarioDelOrganizador;
import bo.aportaya.organizador.infraestructura.ConfiguracionDeCircuito;
import bo.aportaya.organizador.infraestructura.EfectoDeReclamosPorHttp;
import bo.aportaya.organizador.infraestructura.OrganizadorRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.EfectoDeReclamosDeEnsayo;
import bo.aportaya.plataforma.pruebas.EfectoDeReclamosDeEnsayo.Caso;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * H13.S1.M6 · Un reclamo en revision NO es una sancion firme (regla 91.4).
 *
 * <p>La cadena es la real salvo el servicio del otro lado: base de datos real, el caso de uso real (CU-93),
 * {@link ReclamosEnCurso} real y el adaptador HTTP real contra el doble del contrato de cumplimiento
 * ({@link EfectoDeReclamosDeEnsayo}). El servicio de cumplimiento de verdad lo ejerce el script de integracion
 * de {@code despliegue/ensayos/integracion}.
 */
class CU93ReclamoEnCursoTest extends BaseDeOrganizador {

    private static final ConfiguracionDeCircuito CIRCUITO =
            new ConfiguracionDeCircuito(10, 50, Duration.ofSeconds(5), 3);

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private record Caso93(UUID usuario, UUID organizadorId, ContextoSesion operaciones) {}

    private Caso93 caso() {
        UUID usuario = fixtura.usuario();
        return new Caso93(usuario, fixtura.organizadorHabilitado(usuario), contextoDe(fixtura.usuario()));
    }

    private static ReclamosEnCurso guardia(EfectoDeReclamosDeEnsayo cumplimiento) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(300);
        fabrica.setReadTimeout(400);
        var efecto = new EfectoDeReclamosPorHttp(
                RestClient.builder().requestFactory(fabrica), cumplimiento.urlBase(), CIRCUITO);
        return new ReclamosEnCurso(usuarioDelOrganizador(), efecto);
    }

    /** Lo que el proxy de Spring hace en un servicio: la lectura corre en SU transaccion de solo lectura. */
    private static UsuarioDelOrganizador usuarioDelOrganizador() {
        return new UsuarioDelOrganizador(new Datos(dsl), new OrganizadorRepositorio()) {
            @Override
            public Optional<UUID> usuarioDe(UUID organizadorId, ContextoSesion ctx) {
                return transaccion.execute(t -> super.usuarioDe(organizadorId, ctx));
            }
        };
    }

    /** Lo que hace el controlador: preguntar FUERA de la transaccion y recien despues escribir. */
    private void sancionar(ReclamosEnCurso guardia, Caso93 c) {
        guardia.exigirQueNoFrene(c.organizadorId(), c.operaciones());
        transaccion.execute(t -> sancionCU.sancionar(
                new EntradaSancion(
                        c.organizadorId(),
                        Optional.empty(),
                        "SUSPENSION",
                        "Morosidad de cartera sostenida por encima del 20% durante tres periodos",
                        Optional.of(Duration.ofDays(30))),
                c.operaciones()));
    }

    private int sancionesDe(UUID organizadorId) {
        return contar(
                "SELECT count(*)::int FROM organizador.sancion_organizador WHERE organizador_id = ?", organizadorId);
    }

    private static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    @Test
    @DisplayName("rechaza: con un reclamo en revision la sancion NO queda firme (AP-CU93-07) y no se escribe nada")
    void reclamoEnRevisionNoDejaFirmeLaSancion() {
        Caso93 c = caso();
        try (var cumplimiento =
                new EfectoDeReclamosDeEnsayo().conCasos(c.usuario(), Caso.queFrenaTodo("EN_REVISION"))) {
            assertThatThrownBy(() -> sancionar(guardia(cumplimiento), c))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU93-07"));
        }
        assertThat(sancionesDe(c.organizadorId())).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM organizador.organizador WHERE id = ? AND estado = 'HABILITADO'",
                        c.organizadorId()))
                .as("el organizador sigue habilitado: un reclamo en revision no lo suspende")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza: un caso que frena solo la deuda NO frena la sancion: cada indicador es suyo")
    void soloFrenaLaDeudaNoFrenaLaSancion() {
        Caso93 c = caso();
        try (var cumplimiento =
                new EfectoDeReclamosDeEnsayo().conCasos(c.usuario(), new Caso("EN_REVISION", false, true))) {
            sancionar(guardia(cumplimiento), c);
        }
        assertThat(sancionesDe(c.organizadorId())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "rechaza: cumplimiento caido, 403 por falta de RECLAMO_VER o respuesta ilegible frenan igual: no saber no es no haber")
    void noSaberFrena() {
        Caso93 c = caso();
        java.util.List<java.util.function.Supplier<EfectoDeReclamosDeEnsayo>> escenarios = java.util.List.of(
                () -> new EfectoDeReclamosDeEnsayo().respondeEstado(403),
                () -> new EfectoDeReclamosDeEnsayo().respondeEstado(503),
                () -> new EfectoDeReclamosDeEnsayo().respondeCuerpo("no es json"));
        for (var escenario : escenarios) {
            try (var cumplimiento = escenario.get()) {
                assertThatThrownBy(() -> sancionar(guardia(cumplimiento), c))
                        .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU93-07"));
            }
        }
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().sinReclamos(c.usuario())) {
            var guardia = guardia(cumplimiento);
            cumplimiento.caer();
            assertThatThrownBy(() -> sancionar(guardia, c))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU93-07"));
        }
        assertThat(sancionesDe(c.organizadorId())).isZero();
    }

    @Test
    @DisplayName("reintento: resuelto el reclamo la sancion se aplica y cuenta como la unica")
    void resueltoElReclamoSeAplica() {
        Caso93 c = caso();
        try (var cumplimiento =
                new EfectoDeReclamosDeEnsayo().conCasos(c.usuario(), Caso.queFrenaTodo("EN_REVISION"))) {
            assertThatThrownBy(() -> sancionar(guardia(cumplimiento), c))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU93-07"));

            cumplimiento.conCasos(c.usuario(), Caso.libre());
            sancionar(guardia(cumplimiento), c);
        }
        assertThat(sancionesDe(c.organizadorId())).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza: un organizador que no existe no se pregunta a cumplimiento: lo rechaza CU-93 con su codigo")
    void organizadorInexistenteNoLlamaAfuera() {
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().respondeEstado(500)) {
            guardia(cumplimiento).exigirQueNoFrene(UUID.randomUUID(), contextoDe(fixtura.usuario()));
            assertThat(cumplimiento.peticionesRecibidas()).isZero();
        }
    }
}

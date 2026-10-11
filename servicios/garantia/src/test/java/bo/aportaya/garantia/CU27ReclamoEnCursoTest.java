package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.garantia.aplicacion.CU23CubrirIncumplimiento.EntradaCobertura;
import bo.aportaya.garantia.aplicacion.CU25DeclararIncumplimiento.EntradaDeclaracion;
import bo.aportaya.garantia.aplicacion.CU27RestringirDeudor.EntradaRestriccion;
import bo.aportaya.garantia.aplicacion.ReclamosEnCurso;
import bo.aportaya.garantia.aplicacion.UsuarioDelExpediente;
import bo.aportaya.garantia.infraestructura.ConfiguracionDeCircuito;
import bo.aportaya.garantia.infraestructura.EfectoDeReclamosPorHttp;
import bo.aportaya.garantia.infraestructura.ExpedienteRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
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
 * H13.S1.M6 · Un reclamo en revision NO es una deuda ejecutable ni una restriccion firme (regla 91.4).
 *
 * <p>La cadena es la real salvo el servicio del otro lado: base de datos real, el caso de uso real (CU-27),
 * {@link ReclamosEnCurso} real y el adaptador HTTP real contra el doble del contrato de cumplimiento
 * ({@link EfectoDeReclamosDeEnsayo}). El servicio de cumplimiento de verdad lo ejerce el script de integracion
 * de {@code despliegue/ensayos/integracion}.
 */
class CU27ReclamoEnCursoTest extends BaseDeGarantia {

    private static final ConfiguracionDeCircuito CIRCUITO =
            new ConfiguracionDeCircuito(10, 50, Duration.ofSeconds(5), 3);

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private record Caso27(UUID usuario, UUID expedienteId, ContextoSesion gestor) {}

    private Caso27 expediente() {
        UUID usuario = fixtura.usuario();
        var escenario = fixtura.escenario(usuario);
        ContextoSesion gestor = contextoDe(fixtura.usuario());
        UUID politica = fixtura.politica(escenario.grupoId(), "100.00", "50000.00", "50000.00", 3, "100000.00", 15);
        UUID fondo = fixtura.fondo(escenario.grupoId(), politica, "50000.00");
        fixtura.aportarAlFondo(fondo, escenario.participanteId(), "50000.00", "50000.00");
        var expediente = transaccion.execute(t -> expedienteCU.declarar(
                new EntradaDeclaracion(
                        "EXP-" + UUID.randomUUID().toString().substring(0, 8),
                        usuario,
                        escenario.participanteId(),
                        escenario.grupoId(),
                        escenario.periodoId(),
                        escenario.cupoId(),
                        escenario.obligacionId(),
                        "APORTE_IMPAGO",
                        "GRAVE",
                        "AUTOMATICO_VENCIMIENTO",
                        bob("800.00"),
                        30,
                        true,
                        "LOG_SISTEMA",
                        "Vencido y sin pago",
                        null,
                        null),
                gestor));
        transaccion.execute(t ->
                coberturaCU.cubrir(new EntradaCobertura(expediente.expedienteId(), bob("800.00"), 30, null), gestor));
        return new Caso27(usuario, expediente.expedienteId(), gestor);
    }

    private static ReclamosEnCurso guardia(EfectoDeReclamosDeEnsayo cumplimiento) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(300);
        fabrica.setReadTimeout(400);
        var efecto = new EfectoDeReclamosPorHttp(
                RestClient.builder().requestFactory(fabrica), cumplimiento.urlBase(), CIRCUITO);
        return new ReclamosEnCurso(usuarioDelExpediente(), efecto);
    }

    /** Lo que el proxy de Spring hace en un servicio: la lectura corre en SU transaccion de solo lectura. */
    private static UsuarioDelExpediente usuarioDelExpediente() {
        return new UsuarioDelExpediente(new Datos(dsl), new ExpedienteRepositorio()) {
            @Override
            public Optional<UUID> usuarioDe(UUID expedienteId, ContextoSesion ctx) {
                return transaccion.execute(t -> super.usuarioDe(expedienteId, ctx));
            }
        };
    }

    /** Lo que hace el controlador: preguntar FUERA de la transaccion y recien despues escribir. */
    private void restringir(ReclamosEnCurso guardia, Caso27 c) {
        guardia.exigirQueNoFrene(c.expedienteId(), 27, c.gestor());
        transaccion.execute(t -> restriccionCU.restringir(
                new EntradaRestriccion(
                        c.expedienteId(), "LIMITADO", "Deuda de Bs 800 sin regularizar", Optional.empty()),
                c.gestor()));
    }

    private int restriccionesDe(UUID usuario) {
        return contar(
                "SELECT count(*)::int FROM garantia.lista_restriccion_interna WHERE usuario_id = ? AND retirado_en IS NULL",
                usuario);
    }

    private static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    @Test
    @DisplayName("rechaza: con un reclamo en revision la restriccion NO queda firme (AP-CU27-07) y no se escribe nada")
    void reclamoEnRevisionNoDejaFirmeLaRestriccion() {
        Caso27 c = expediente();
        try (var cumplimiento =
                new EfectoDeReclamosDeEnsayo().conCasos(c.usuario(), Caso.queFrenaTodo("EN_REVISION"))) {
            assertThatThrownBy(() -> restringir(guardia(cumplimiento), c))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU27-07"));
        }
        assertThat(restriccionesDe(c.usuario())).isZero();
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM garantia.evento_dominio
                         WHERE tipo = 'garantia.usuario_restringido' AND payload->>'usuarioId' = ?
                        """,
                        c.usuario().toString()))
                .isZero();
    }

    @Test
    @DisplayName(
            "rechaza: cumplimiento caido, 403 por falta de RECLAMO_VER o respuesta ilegible frenan igual: no saber no es no haber")
    void noSaberFrena() {
        Caso27 c = expediente();
        java.util.List<java.util.function.Supplier<EfectoDeReclamosDeEnsayo>> escenarios = java.util.List.of(
                () -> new EfectoDeReclamosDeEnsayo().respondeEstado(403),
                () -> new EfectoDeReclamosDeEnsayo().respondeEstado(503),
                () -> new EfectoDeReclamosDeEnsayo().respondeCuerpo("no es json"));
        for (var escenario : escenarios) {
            try (var cumplimiento = escenario.get()) {
                assertThatThrownBy(() -> restringir(guardia(cumplimiento), c))
                        .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU27-07"));
            }
        }
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().sinReclamos(c.usuario())) {
            var guardia = guardia(cumplimiento);
            cumplimiento.caer();
            assertThatThrownBy(() -> restringir(guardia, c))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU27-07"));
        }
        assertThat(restriccionesDe(c.usuario())).isZero();
    }

    @Test
    @DisplayName("reintento: resuelto el reclamo la misma restriccion se aplica, y repetirla no abre otra")
    void resueltoElReclamoSeAplica() {
        Caso27 c = expediente();
        try (var cumplimiento =
                new EfectoDeReclamosDeEnsayo().conCasos(c.usuario(), Caso.queFrenaTodo("EN_REVISION"))) {
            assertThatThrownBy(() -> restringir(guardia(cumplimiento), c))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU27-07"));

            // El reclamo se resuelve: terminado sin bloqueo.
            cumplimiento.conCasos(c.usuario(), Caso.libre());
            restringir(guardia(cumplimiento), c);
            restringir(guardia(cumplimiento), c);
        }
        assertThat(restriccionesDe(c.usuario())).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza: ejecutar el aval con un reclamo en curso responde AP-CU26-07 antes de tocar nada")
    void reclamoEnCursoNoEjecutaElAval() {
        Caso27 c = expediente();
        try (var cumplimiento =
                new EfectoDeReclamosDeEnsayo().conCasos(c.usuario(), Caso.queFrenaTodo("EN_REVISION"))) {
            assertThatThrownBy(() -> guardia(cumplimiento).exigirQueNoFrene(c.expedienteId(), 26, c.gestor()))
                    .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU26-07"));
        }
        assertThat(contar("SELECT count(*)::int FROM garantia.ejecucion_aval WHERE registro_id = ?", c.expedienteId()))
                .isZero();
    }

    @Test
    @DisplayName(
            "rechaza: un expediente que no existe no se pregunta a cumplimiento: lo rechaza el caso de uso con su codigo")
    void expedienteInexistenteNoLlamaAfuera() {
        try (var cumplimiento = new EfectoDeReclamosDeEnsayo().respondeEstado(500)) {
            guardia(cumplimiento).exigirQueNoFrene(UUID.randomUUID(), 27, contextoDe(fixtura.usuario()));
            assertThat(cumplimiento.peticionesRecibidas()).isZero();
        }
    }
}

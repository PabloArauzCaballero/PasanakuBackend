package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a los otros seis servicios, por sus contratos.
 *
 * <p>Cada respuesta que no llega se traduce a **la que no deja pasar**: el organizador no esta habilitado, no hay
 * tarifario, la licencia no cubre, el participante no esta al dia, hay restriccion vigente, el contacto esta
 * suprimido. Es denegar por omision (invariante 9), y es deliberado: seguir adelante porque el servicio que valida
 * estaba caido es exactamente el hueco por donde entra lo que las reglas existen para frenar.
 *
 * <p>Excepcion: la reputacion («no se sabe», {@code tieneHistorial=false}). Cada dependencia lleva tiempo maximo y
 * circuit breaker propio ({@link ClienteConCircuito}, regla 98.2.3): con el circuito abierto responde como caida,
 * salvo identidad al EMITIR o CONSUMIR un enlace, que falla con la excepcion de red y no crea invitaciones sin token.
 */
@Component
public class HechosPorHttp implements HechosDeOtrosServicios {

    private final ClienteConCircuito organizadores;
    private final ClienteConCircuito tarifas;
    private final ClienteConCircuito cumplimiento;
    private final ClienteConCircuito aportes;
    private final ClienteConCircuito transparencia;
    private final ClienteConCircuito garantia;
    private final ClienteConCircuito notificaciones;
    private final ClienteConCircuito identidad;
    private final TokensDeInvitacionPorHttp tokens;
    private final EtapasPorHttp etapas;
    private final PersonasPorHttp personas;

    @Autowired
    @SuppressWarnings("checkstyle:ParameterNumber")
    public HechosPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.organizador}") String urlOrganizador,
            @Value("${aportaya.servicios.tarifas}") String urlTarifas,
            @Value("${aportaya.servicios.cumplimiento}") String urlCumplimiento,
            @Value("${aportaya.servicios.aportes}") String urlAportes,
            @Value("${aportaya.servicios.transparencia}") String urlTransparencia,
            @Value("${aportaya.servicios.garantia}") String urlGarantia,
            @Value("${aportaya.servicios.notificaciones}") String urlNotificaciones,
            @Value("${aportaya.servicios.identidad}") String urlIdentidad,
            ConfiguracionDeCircuito circuito,
            CredencialDeServicio servicio,
            @Value("${aportaya.grupos.transparencia.maximo-por-consulta:60}") int maximoPorConsulta) {
        this.organizadores = new ClienteConCircuito(constructor, urlOrganizador, "organizador", circuito);
        this.tarifas = new ClienteConCircuito(constructor, urlTarifas, "tarifas", circuito);
        this.cumplimiento = new ClienteConCircuito(constructor, urlCumplimiento, "cumplimiento", circuito);
        this.aportes = new ClienteConCircuito(constructor, urlAportes, "aportes", circuito);
        this.transparencia = new ClienteConCircuito(constructor, urlTransparencia, "transparencia", circuito);
        this.garantia = new ClienteConCircuito(constructor, urlGarantia, "garantia", circuito);
        this.notificaciones = new ClienteConCircuito(constructor, urlNotificaciones, "notificaciones", circuito);
        this.identidad = new ClienteConCircuito(constructor, urlIdentidad, "identidad", circuito);
        this.tokens = new TokensDeInvitacionPorHttp(identidad);
        this.etapas = new EtapasPorHttp(this.transparencia, servicio, maximoPorConsulta);
        this.personas = new PersonasPorHttp(this.identidad);
    }

    /** Con los parametros del plan de resiliencia: la usan las pruebas del adaptador. */
    @SuppressWarnings("checkstyle:ParameterNumber")
    HechosPorHttp(
            RestClient.Builder constructor,
            String urlOrganizador,
            String urlTarifas,
            String urlCumplimiento,
            String urlAportes,
            String urlTransparencia,
            String urlGarantia,
            String urlNotificaciones,
            String urlIdentidad) {
        this(
                constructor,
                urlOrganizador,
                urlTarifas,
                urlCumplimiento,
                urlAportes,
                urlTransparencia,
                urlGarantia,
                urlNotificaciones,
                urlIdentidad,
                CredencialDeServicio.ninguna());
    }

    /** Con los parametros del plan de resiliencia y una credencial de servicio a medida: pruebas del adaptador. */
    @SuppressWarnings("checkstyle:ParameterNumber")
    HechosPorHttp(
            RestClient.Builder constructor,
            String urlOrganizador,
            String urlTarifas,
            String urlCumplimiento,
            String urlAportes,
            String urlTransparencia,
            String urlGarantia,
            String urlNotificaciones,
            String urlIdentidad,
            CredencialDeServicio servicio) {
        this(
                constructor,
                urlOrganizador,
                urlTarifas,
                urlCumplimiento,
                urlAportes,
                urlTransparencia,
                urlGarantia,
                urlNotificaciones,
                urlIdentidad,
                ConfiguracionDeCircuito.porOmision(),
                servicio,
                // El de application.yml (aportaya.grupos.transparencia.maximo-por-consulta), como porOmision().
                60);
    }

    @Override
    public boolean organizadorHabilitado(UUID organizadorId) {
        return organizadores
                .consultar("/organizadores/" + organizadorId + "/habilitacion", Habilitacion.class)
                .map(Habilitacion::habilitado)
                .orElse(false);
    }

    @Override
    public java.util.OptionalInt limiteDeGruposDelOrganizador(UUID organizadorId) {
        return organizadores
                .consultar("/organizadores/" + organizadorId + "/habilitacion", Habilitacion.class)
                .filter(h -> h.habilitado() && h.limiteDeGrupos() != null)
                .map(h -> java.util.OptionalInt.of(h.limiteDeGrupos()))
                .orElse(java.util.OptionalInt.empty());
    }

    @Override
    public Optional<UUID> organizadorHabilitadoDelUsuario(UUID usuarioId) {
        return organizadores
                .consultar("/organizadores/usuarios/" + usuarioId + "/habilitacion", HabilitacionPersonal.class)
                .filter(h -> h.habilitado() && usuarioId.equals(h.usuarioId()))
                .map(HabilitacionPersonal::organizadorId);
    }

    private record HabilitacionPersonal(UUID usuarioId, UUID organizadorId, boolean habilitado) {}

    @Override
    public Optional<UUID> tarifarioVigente(String codigo) {
        return tarifas.consultar("/tarifas/vigentes/" + codigo, Tarifario.class)
                .filter(Tarifario::vigente)
                .map(Tarifario::tarifarioId);
    }

    @Override
    public boolean licenciaHabilita(String servicio) {
        return cumplimiento
                .consultar("/licencia/alcance?servicio=" + servicio, Alcance.class)
                .map(Alcance::habilitado)
                .orElse(false);
    }

    @Override
    public EstadoDePagos estadoDePagos(UUID participanteId) {
        return aportes.consultar("/aportes/participantes/" + participanteId + "/estado", Estado.class)
                // Un cuerpo sin los importes (`{}`) no es una respuesta: es «sin dato». Antes era un NPE (500) en vez
                // del
                // fallback declarado (carril I: hallado por HechosCaidaTest cuando el circuito vuelve a semiabrirse).
                .filter(e -> e.totalAportado() != null && e.deudaVigente() != null && e.porAportar() != null)
                .map(e -> new EstadoDePagos(
                        e.alDia(),
                        new BigDecimal(e.totalAportado()),
                        new BigDecimal(e.deudaVigente()),
                        new BigDecimal(e.porAportar()),
                        e.obligacionesAbiertas(),
                        e.moneda()))
                // Sin respuesta no se afirma que esta al dia: es lo unico que no se
                // puede suponer sin arriesgar el turno de otro.
                .orElse(new EstadoDePagos(false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, "BOB"));
    }

    @Override
    public int morososDelGrupo(UUID grupoId) {
        return aportes.consultar("/aportes/grupos/" + grupoId + "/morosos", Morosos.class)
                .map(Morosos::morosos)
                .orElse(SIN_DATO_DE_MOROSOS);
    }

    @Override
    public Reputacion reputacion(UUID usuarioId) {
        return transparencia
                .consultar("/reputacion/" + usuarioId + "/puntaje", Puntaje.class)
                .map(p -> new Reputacion(p.tieneHistorial(), new BigDecimal(p.puntaje())))
                .orElse(new Reputacion(false, BigDecimal.ZERO));
    }

    @Override
    public Optional<bo.aportaya.grupos.dominio.Etapas.DeLaPersona> etapaDe(UUID usuarioId) {
        return etapas.de(usuarioId);
    }

    @Override
    public Optional<Map<UUID, bo.aportaya.grupos.dominio.Etapas.Visible>> etapasDe(
            java.util.Collection<UUID> usuarios) {
        return etapas.visibles(usuarios);
    }

    @Override
    public Optional<java.util.List<bo.aportaya.grupos.dominio.Etapas.Etapa>> catalogoDeEtapas() {
        return etapas.catalogo();
    }

    @Override
    public Restriccion restriccion(UUID usuarioId) {
        return garantia.consultar("/cobranza/restricciones/vigentes/" + usuarioId, Restringido.class)
                .map(r -> new Restriccion(r.vigente(), new BigDecimal(r.montoQueLaLevanta())))
                // Sin respuesta se asume restringido: dejar entrar porque el verificador no contesto no se paga solo.
                .orElse(new Restriccion(true, BigDecimal.ZERO));
    }

    @Override
    public boolean contactoSuprimido(String identificador, String categoria) {
        // El contacto viaja en el CUERPO, no en la URL (90.2.2): una URL queda en historiales, proxies y logs del
        // gateway.
        return notificaciones
                .consultarPorCuerpo(
                        "/notificaciones/supresion/consultas",
                        Map.of("identificador", identificador, "categoria", categoria),
                        Suprimido.class)
                .map(Suprimido::suprimido)
                .orElse(true);
    }

    @Override
    public TokenInvitacion tokenDeInvitacion(UUID clave, UUID grupoId, String canal, String telefono) {
        return tokens.emitir(clave, grupoId, canal, telefono);
    }

    @Override
    public boolean enlaceDeInvitacionValido(UUID tokenId, String token, String telefonoE164, String kycMinimo) {
        return tokens.esValido(tokenId, token, telefonoE164, kycMinimo);
    }

    @Override
    public ConsumoInvitacion consumirInvitacion(UUID tokenId, UUID grupoId, UUID clave, String token) {
        return tokens.consumir(tokenId, grupoId, clave, token);
    }

    @Override
    public boolean revocarTokenDeInvitacion(UUID tokenId) {
        return tokens.revocar(tokenId);
    }

    @Override
    public String nivelDeKyc(UUID usuarioId) {
        return personas.nivelDeKyc(usuarioId);
    }

    @Override
    public Optional<Contacto> contactoDeLaSesion() {
        return personas.contactoDeLaSesion();
    }

    /** El telefono viaja en el CUERPO, no en la URL (regla 90.2.2): {@link PersonasPorHttp#usuarioDelTelefono}. */
    @Override
    public Optional<UUID> usuarioDelTelefono(String telefonoE164) {
        return personas.usuarioDelTelefono(telefonoE164);
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record Habilitacion(boolean habilitado, Integer limiteDeGrupos) {}

    private record Tarifario(boolean vigente, UUID tarifarioId) {}

    private record Alcance(boolean habilitado) {}

    private record Estado(
            boolean alDia,
            String totalAportado,
            String deudaVigente,
            String porAportar,
            int obligacionesAbiertas,
            String moneda) {}

    private record Morosos(int morosos) {}

    private record Puntaje(boolean tieneHistorial, String puntaje) {}

    private record Restringido(boolean vigente, String montoQueLaLevanta) {}

    private record Suprimido(boolean suprimido) {}
}

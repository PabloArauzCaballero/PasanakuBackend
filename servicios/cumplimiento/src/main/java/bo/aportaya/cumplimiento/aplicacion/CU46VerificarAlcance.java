package bo.aportaya.cumplimiento.aplicacion;

import bo.aportaya.cumplimiento.dominio.EstadoDeLicencia;
import bo.aportaya.cumplimiento.dominio.HabilitacionDeServicio;
import bo.aportaya.cumplimiento.dominio.HabilitacionDeServicio.Decision;
import bo.aportaya.cumplimiento.infraestructura.LicenciaRepositorio;
import bo.aportaya.cumplimiento.infraestructura.SandboxRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-46 · Verificar el alcance de la licencia.
 *
 * <p>Es el caso de uso que impide el error regulatorio mas caro y mas facil de
 * cometer: lanzar una funcion que la autorizacion no cubre. Por eso **deniega por
 * omision** (invariante 9): sin licencia legible, sin alcance o sin sandbox, la
 * respuesta es no. Un fallo de lectura no puede traducirse en «segui adelante».
 *
 * <p>No tiene endpoint. Lo consultan los otros servicios antes de habilitar una
 * operacion, y el planificador antes de un despliegue.
 */
@Service
public class CU46VerificarAlcance {

    private final Datos datos;
    private final LicenciaRepositorio licencias;
    private final SandboxRepositorio sandboxes;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Set<String> serviciosDeSalida;

    public CU46VerificarAlcance(
            Datos datos,
            LicenciaRepositorio licencias,
            SandboxRepositorio sandboxes,
            Outbox outbox,
            Reloj reloj,
            // La boveda no trae un catalogo de «servicios de salida». Se declara en
            // configuracion, donde se ve y se audita, en vez de hornearse aca.
            @Value("${cumplimiento.servicios-de-salida:RETIRO}") Set<String> serviciosDeSalida) {
        this.datos = datos;
        this.licencias = licencias;
        this.sandboxes = sandboxes;
        this.outbox = outbox;
        this.reloj = reloj;
        this.serviciosDeSalida = Set.copyOf(serviciosDeSalida);
    }

    /**
     * La consulta: <b>no escribe</b> (F1.L4.M25). Antes cada rechazo emitia un evento al
     * outbox, asi que cualquier participante podia hacer crecer la tabla con un GET y el
     * registro no distinguia una pregunta de una operacion rechazada.
     */
    @Transactional(readOnly = true)
    public SalidaAlcance ejecutar(EntradaAlcance entrada, ContextoSesion ctx) {
        LocalDate hoy = reloj.ahora().atOffset(ZoneOffset.UTC).toLocalDate();
        return datos.conContexto(ctx, dsl -> resolver(dsl, entrada, hoy).salida());
    }

    /**
     * La version que corta, para una operacion real: registra el rechazo (con el actor del
     * token, nunca uno recibido) y lanza el codigo que el contrato promete.
     */
    @Transactional
    public void exigirHabilitado(EntradaAlcance entrada, ContextoSesion ctx) {
        LocalDate hoy = reloj.ahora().atOffset(ZoneOffset.UTC).toLocalDate();
        SalidaAlcance salida = datos.conContexto(ctx, dsl -> {
            Resuelta resuelta = resolver(dsl, entrada, hoy);
            if (!resuelta.salida().habilitado()) {
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "cumplimiento.servicio_rechazado_por_alcance",
                                "licencia_regulatoria",
                                ctx.usuarioId() != null
                                        ? ctx.usuarioId()
                                        : UUID.fromString(ctx.traza().id()),
                                Map.of(
                                        "servicio", entrada.servicio(),
                                        "estadoLicencia", resuelta.estado().name(),
                                        "motivo",
                                                resuelta.salida().motivo() == null
                                                        ? ""
                                                        : resuelta.salida().motivo()),
                                UUID.fromString(ctx.traza().id())));
            }
            return resuelta.salida();
        });
        if (salida.habilitado()) {
            return;
        }
        throw new ErrorDeNegocio(codigoDe(entrada, salida), salida.motivo());
    }

    private Resuelta resolver(org.jooq.DSLContext dsl, EntradaAlcance entrada, LocalDate hoy) {
        var licencia = licencias.vigente(dsl, hoy);

        // AP-CU46-01: sin fila de licencia no hay nada que interpretar. Denegar
        // por omision es la unica lectura segura de una tabla vacia.
        EstadoDeLicencia estado =
                licencia.map(LicenciaRepositorio.Licencia::estado).orElse(EstadoDeLicencia.EN_TRAMITE);
        boolean vigente = licencia.map(LicenciaRepositorio.Licencia::vigente).orElse(false);
        Set<String> alcance =
                licencia.map(LicenciaRepositorio.Licencia::alcance).orElse(Set.of());

        Decision decision = HabilitacionDeServicio.resolver(
                entrada.servicio(),
                estado,
                vigente,
                alcance,
                sandboxes.activoPara(dsl, entrada.servicio(), hoy, entrada.usuariosRegistrados()),
                serviciosDeSalida);

        return new Resuelta(
                estado,
                new SalidaAlcance(
                        decision.habilitado(),
                        decision.via().name(),
                        decision.limites()
                                .map(l -> new LimitesSandbox(
                                        l.usuarios(), l.montoOperacion().toPlainString()))
                                .orElse(null),
                        decision.motivo().orElse(null)));
    }

    private record Resuelta(EstadoDeLicencia estado, SalidaAlcance salida) {}

    private CodigoError codigoDe(EntradaAlcance entrada, SalidaAlcance salida) {
        String motivo = salida.motivo() == null ? "" : salida.motivo();
        if (motivo.contains("tope de")) {
            return CodigoError.de(46, 3); // SANDBOX_AGOTADO
        }
        if (motivo.contains("SUSPENDIDA") || motivo.contains("REVOCADA")) {
            return CodigoError.de(46, 4); // LICENCIA_SUSPENDIDA
        }
        if (motivo.startsWith("Todavia no hay licencia")) {
            return CodigoError.de(46, 1); // LICENCIA_NO_OTORGADA
        }
        return CodigoError.de(46, 2); // FUERA_DE_ALCANCE (R-LIC-01)
    }

    /** Sin usuario: quien pregunta sale del token (F1.L4.M25). */
    public record EntradaAlcance(String servicio, int usuariosRegistrados) {

        /** El caso comun: preguntar por un servicio, sin sandbox de por medio. */
        public static EntradaAlcance de(String servicio) {
            return new EntradaAlcance(servicio, 0);
        }
    }

    public record SalidaAlcance(boolean habilitado, String via, LimitesSandbox limitesSandbox, String motivo) {}

    public record LimitesSandbox(int usuarios, String montoOperacion) {}
}

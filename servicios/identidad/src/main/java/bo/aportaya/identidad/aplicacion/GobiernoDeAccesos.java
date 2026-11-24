package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.aplicacion.CU08AsignarRol.EntradaAsignacion;
import bo.aportaya.identidad.aplicacion.CU08AsignarRol.SalidaAsignacion;
import bo.aportaya.identidad.dominio.AmbitoDeRol;
import bo.aportaya.identidad.dominio.HechoDeAcceso;
import bo.aportaya.identidad.dominio.PermisosEfectivos;
import bo.aportaya.identidad.infraestructura.AccesoRepositorio;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio.Asignacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-08 por asignacion, como lo pide el backoffice (GU-05, decision P-2a): las reglas de gobierno alrededor de
 * {@link CU08AsignarRol} y {@link CU08RevocarRol}, en la misma transaccion.
 *
 * <p>Lo que agrega a los organismos: step-up de quien actua, que el destinatario sea un operador ACTIVO, que el
 * ambito de la asignacion sea el del rol, y la idempotencia por contenido (una asignacion viva igual se devuelve,
 * no se duplica ni se vuelve a anunciar).
 */
@Service
public class GobiernoDeAccesos {

    private static final String VENCIMIENTO = "VENCIMIENTO";

    private final Datos datos;
    private final AccesosRepositorio accesos;
    private final AccesoRepositorio factores;
    private final GobiernoDeAccesosRepositorio gobierno;
    private final CU08AsignarRol asignar;
    private final CU08RevocarRol revocar;
    private final ReglasDeGobierno reglas;
    private final BitacoraDeAccesos bitacora;
    private final Reloj reloj;

    public GobiernoDeAccesos(
            Datos datos,
            AccesosRepositorio accesos,
            AccesoRepositorio factores,
            GobiernoDeAccesosRepositorio gobierno,
            CU08AsignarRol asignar,
            CU08RevocarRol revocar,
            ReglasDeGobierno reglas,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.accesos = accesos;
        this.factores = factores;
        this.gobierno = gobierno;
        this.asignar = asignar;
        this.revocar = revocar;
        this.reglas = reglas;
        this.bitacora = new BitacoraDeAccesos(outbox, gobierno);
        this.reloj = reloj;
    }

    /** Lo que pide quien asigna. {@code operador} es el usuarioId o el id de su perfil interno. */
    public record PedidoDeAsignacion(
            UUID operador,
            UUID rolId,
            String ambito,
            Optional<UUID> ambitoId,
            Optional<OffsetDateTime> vigenteHasta,
            String justificacion,
            String evidenciaMfa) {}

    public record ResultadoDeAsignacion(
            UUID asignacionId,
            UUID usuarioId,
            String rolCodigo,
            String ambito,
            Optional<UUID> ambitoId,
            Optional<OffsetDateTime> vigenteHasta,
            Set<String> permisosEfectivos,
            boolean requiereMfa,
            boolean yaExistia) {}

    public record ResultadoDeRevocacion(
            UUID asignacionId, UUID usuarioId, OffsetDateTime revocadaEn, int sesionesCerradas, boolean yaEstaba) {}

    @Transactional
    public ResultadoDeAsignacion asignar(PedidoDeAsignacion pedido, ContextoSesion ctx) {
        reglas.exigirStepUp(ctx, pedido.evidenciaMfa());
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            UUID usuario = operadorActivo(dsl, pedido.operador());
            ReglasDeGobierno.noSobreSiMismo(ctx, usuario);
            String ambitoDelRol = accesos.ambitoDelRol(dsl, pedido.rolId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(8, 1), "Ese rol no existe."));
            if (!ambitoDelRol.equals(pedido.ambito())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(8, 2),
                        "El ambito de la asignacion tiene que ser el del rol.",
                        Map.of("ambitoDelRol", ambitoDelRol));
            }

            Optional<Asignacion> igual =
                    gobierno.sinRevocarIgual(dsl, usuario, pedido.rolId(), pedido.ambito(), pedido.ambitoId());
            if (igual.isPresent() && igual.get().vigenteEn(ahora)) {
                return existente(dsl, igual.get(), ahora);
            }
            // Vencida pero sin revocar todavia (el trabajo diario no paso): se la cierra como
            // vencida para que uq_asignacion_vigente deje escribir la nueva.
            igual.ifPresent(vencida -> vencerAntesDeReasignar(dsl, vencida, ctx, ahora));

            SalidaAsignacion salida = asignar.ejecutar(
                    new EntradaAsignacion(
                            usuario,
                            pedido.rolId(),
                            pedido.ambito(),
                            pedido.ambitoId(),
                            pedido.vigenteHasta(),
                            pedido.justificacion()),
                    ctx);
            String codigo = gobierno.asignacion(dsl, salida.asignacionId())
                    .map(Asignacion::rolCodigo)
                    .orElse("");
            return new ResultadoDeAsignacion(
                    salida.asignacionId(),
                    usuario,
                    codigo,
                    pedido.ambito(),
                    pedido.ambitoId(),
                    pedido.vigenteHasta(),
                    salida.permisosEfectivos(),
                    salida.requiereMfa(),
                    false);
        });
    }

    @Transactional
    public ResultadoDeRevocacion revocar(
            UUID asignacionId, String motivo, boolean cerrarSesiones, String evidenciaMfa, ContextoSesion ctx) {
        reglas.exigirStepUp(ctx, evidenciaMfa);

        return datos.conContexto(ctx, dsl -> {
            Asignacion asignacion = gobierno.asignacion(dsl, asignacionId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(8, 1), "Esa asignacion no existe."));
            ReglasDeGobierno.noSobreSiMismo(ctx, asignacion.usuarioId());
            if (asignacion.revocadaEn().isPresent()) {
                return new ResultadoDeRevocacion(
                        asignacionId,
                        asignacion.usuarioId(),
                        asignacion.revocadaEn().get(),
                        0,
                        true);
            }
            int sesiones = revocar.ejecutar(asignacionId, motivo, cerrarSesiones, ctx);
            OffsetDateTime revocadaEn = gobierno.asignacion(dsl, asignacionId)
                    .flatMap(Asignacion::revocadaEn)
                    .orElseThrow();
            return new ResultadoDeRevocacion(asignacionId, asignacion.usuarioId(), revocadaEn, sesiones, false);
        });
    }

    /** AP-CU08-10: solo se asigna a un operador (perfil interno) con la cuenta ACTIVA (precondicion 3). */
    private UUID operadorActivo(DSLContext dsl, UUID id) {
        UUID usuario = gobierno.operador(dsl, id).orElseThrow(GobiernoDeAccesos::noHabilitado);
        if (!"ACTIVO".equals(gobierno.estadoDelOperador(dsl, usuario).orElse(""))) {
            throw noHabilitado();
        }
        return usuario;
    }

    static ErrorDeNegocio noHabilitado() {
        return new ErrorDeNegocio(
                CodigoError.de(8, 10), "Esa persona no es un operador con la cuenta activa: no se le asignan roles.");
    }

    private ResultadoDeAsignacion existente(DSLContext dsl, Asignacion viva, OffsetDateTime ahora) {
        Set<String> permisos = PermisosEfectivos.de(accesos.asignacionesDe(dsl, viva.usuarioId()), ahora);
        boolean faltaMfa = accesos.ambitoDelRol(dsl, viva.rolId())
                        .map(AmbitoDeRol.GLOBAL.name()::equals)
                        .orElse(false)
                && factores.factorActivo(dsl, viva.usuarioId()).isEmpty();
        return new ResultadoDeAsignacion(
                viva.id(),
                viva.usuarioId(),
                viva.rolCodigo(),
                viva.ambito(),
                viva.ambitoId(),
                viva.vigenteHasta(),
                permisos,
                faltaMfa,
                true);
    }

    private void vencerAntesDeReasignar(DSLContext dsl, Asignacion vencida, ContextoSesion ctx, OffsetDateTime ahora) {
        OffsetDateTime vencio = vencida.vigenteHasta().orElse(ahora);
        String motivo = VENCIMIENTO + ": vigente_hasta cumplido";
        if (accesos.revocar(dsl, vencida.id(), motivo, vencio) == 0) {
            return;
        }
        bitacora.registrar(
                dsl,
                new HechoDeAcceso(
                        HechoDeAcceso.Accion.ROL_VENCIDO,
                        Optional.empty(),
                        vencida.usuarioId(),
                        motivo,
                        ahora,
                        Map.of(
                                "asignacionId", vencida.id().toString(),
                                "rolCodigo", vencida.rolCodigo(),
                                "vencioEn", vencio.toString())),
                vencida.id(),
                ctx);
    }
}

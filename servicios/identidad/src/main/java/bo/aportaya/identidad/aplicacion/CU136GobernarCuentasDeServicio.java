package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import bo.aportaya.identidad.dominio.puertos.HasheadorDeCredencial;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.CredencialRepositorio;
import bo.aportaya.identidad.infraestructura.CuentasDeServicioRepositorio;
import bo.aportaya.identidad.infraestructura.CuentasDeServicioRepositorio.Fila;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-136 · Gobernar las cuentas de servicio (GU-18, decision P-10).
 *
 * <p>Una cuenta de servicio es el usuario que porta un rol {@code SERVICIO_*}: con ese rol un servicio le habla a
 * otro. Hasta ahora esos usuarios se aprovisionaban fuera del repositorio y nadie podia decir quien los habia creado,
 * cuando vencian ni cuando cambiaron su clave. Este caso de uso no crea el usuario (ambiguedad A5 del plan: como se
 * identifica y como enrola su segundo factor lo decide Seguridad): <b>registra</b> una cuenta ya aprovisionada, y desde
 * ahi la gobierna — rotar su clave, revocarla, ver cuando vence.
 *
 * <p>La credencial que se entrega al rotar es de un solo uso: viaja en la respuesta de ESA llamada y no se persiste
 * (solo su hash, via {@link HasheadorDeCredencial}); ni el log, ni el evento, ni ninguna lectura posterior la llevan.
 * Los permisos son los del rol {@code SERVICIO_*}: no se agregan permisos sueltos a una cuenta.
 */
@Service
public class CU136GobernarCuentasDeServicio {

    public static final int CU = 136;
    public static final CodigoError NOMBRE_INVALIDO = CodigoError.de(CU, 1);
    public static final CodigoError MOTIVO_REQUERIDO = CodigoError.de(CU, 2);
    public static final CodigoError VENCIMIENTO_INVALIDO = CodigoError.de(CU, 3);
    public static final CodigoError STEP_UP_REQUERIDO = CodigoError.de(CU, 4);
    public static final CodigoError SIN_ROL_DE_SERVICIO = CodigoError.de(CU, 5);
    public static final CodigoError YA_REGISTRADA = CodigoError.de(CU, 6);
    public static final CodigoError CUENTA_INEXISTENTE = CodigoError.de(CU, 7);
    public static final CodigoError CUENTA_REVOCADA = CodigoError.de(CU, 8);
    public static final CodigoError NADIE_SE_GOBIERNA_A_SI_MISMO = CodigoError.de(CU, 9);
    public static final CodigoError SIN_CREDENCIAL = CodigoError.de(CU, 10);

    private static final String PREFIJO_DE_SERVICIO = "SERVICIO_";
    private static final int FILAS_DEL_LISTADO = 200;

    private final Datos datos;
    private final CuentasDeServicioRepositorio cuentas;
    private final AccesosRepositorio accesos;
    private final GobiernoDeAccesosRepositorio gobierno;
    private final CredencialRepositorio credenciales;
    private final HasheadorDeCredencial hasheador;
    private final EvidenciaDeStepUp evidencias;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Duration vigenciaMaxima;

    public CU136GobernarCuentasDeServicio(
            Datos datos,
            CuentasDeServicioRepositorio cuentas,
            AccesosRepositorio accesos,
            GobiernoDeAccesosRepositorio gobierno,
            CredencialRepositorio credenciales,
            HasheadorDeCredencial hasheador,
            EvidenciaDeStepUp evidencias,
            Outbox outbox,
            Reloj reloj,
            @Value("${aportaya.cuentas-servicio.vigencia-maxima:P90D}") Duration vigenciaMaxima) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.accesos = accesos;
        this.gobierno = gobierno;
        this.credenciales = credenciales;
        this.hasheador = hasheador;
        this.evidencias = evidencias;
        this.outbox = outbox;
        this.reloj = reloj;
        this.vigenciaMaxima = vigenciaMaxima;
    }

    public record PedidoDeAlta(
            UUID usuarioId, String nombre, OffsetDateTime venceEn, String motivo, String evidencia) {}

    public record PedidoDeRotacion(Optional<OffsetDateTime> venceEn, String motivo, String evidencia) {}

    public record Alta(UUID cuentaId, UUID usuarioId, String rolCodigo, OffsetDateTime venceEn) {}

    /** {@code credencial} es el unico lugar donde el valor existe: se entrega una vez y no se puede volver a pedir. */
    public record Rotacion(UUID cuentaId, String credencial, OffsetDateTime venceEn, int sesionesCerradas) {
        @Override
        public String toString() {
            return "Rotacion[cuentaId=" + cuentaId + ", credencial=<oculta>]";
        }
    }

    public record Revocacion(UUID cuentaId, int asignacionesRevocadas, int sesionesCerradas) {}

    @Transactional
    public Alta registrar(PedidoDeAlta p, ContextoSesion ctx) {
        ValidacionesDeCuentaDeServicio.validarNombre(p.nombre());
        ValidacionesDeCuentaDeServicio.validarMotivo(p.motivo());
        exigirStepUp(ctx, p.evidencia());
        OffsetDateTime ahora = ahora();
        ValidacionesDeCuentaDeServicio.validarVencimiento(p.venceEn(), vigenciaMaxima, ahora);
        if (p.usuarioId().equals(ctx.usuarioId())) {
            throw new ErrorDeNegocio(NADIE_SE_GOBIERNA_A_SI_MISMO, "No podes gobernar tu propia cuenta.");
        }
        return datos.conContexto(ctx, dsl -> {
            if (cuentas.registradaPara(dsl, p.usuarioId())) {
                throw new ErrorDeNegocio(YA_REGISTRADA, "Esa cuenta ya esta registrada.");
            }
            String rolCodigo = accesos.codigosDeRolesVigentes(dsl, p.usuarioId(), ahora).stream()
                    .filter(c -> c.startsWith(PREFIJO_DE_SERVICIO))
                    .sorted()
                    .findFirst()
                    .orElseThrow(() -> new ErrorDeNegocio(
                            SIN_ROL_DE_SERVICIO,
                            "Ese usuario no tiene una identidad de servicio vigente (SERVICIO_*)."));
            UUID rolId = cuentas.rolIdDe(dsl, rolCodigo)
                    .orElseThrow(() -> new ErrorDeNegocio(SIN_ROL_DE_SERVICIO, "Ese rol de servicio ya no existe."));
            UUID cuenta = cuentas.insertar(dsl, p.usuarioId(), rolId, p.nombre(), p.venceEn(), ctx.usuarioId(), ahora);
            emitir(
                    dsl,
                    "identidad.cuenta_servicio_alta",
                    cuenta,
                    ctx,
                    p.motivo(),
                    Map.of(
                            "rolCodigo",
                            rolCodigo,
                            "usuarioId",
                            p.usuarioId().toString(),
                            "venceEn",
                            p.venceEn().toString()));
            return new Alta(cuenta, p.usuarioId(), rolCodigo, p.venceEn());
        });
    }

    @Transactional
    public Rotacion rotar(UUID cuentaId, PedidoDeRotacion p, ContextoSesion ctx) {
        ValidacionesDeCuentaDeServicio.validarMotivo(p.motivo());
        exigirStepUp(ctx, p.evidencia());
        OffsetDateTime ahora = ahora();
        p.venceEn().ifPresent(v -> ValidacionesDeCuentaDeServicio.validarVencimiento(v, vigenciaMaxima, ahora));
        return datos.conContexto(ctx, dsl -> {
            Fila cuenta = activa(dsl, cuentaId);
            if (cuenta.usuarioId().equals(ctx.usuarioId())) {
                throw new ErrorDeNegocio(NADIE_SE_GOBIERNA_A_SI_MISMO, "No podes gobernar tu propia cuenta.");
            }
            String hashAnterior = cuentas.hashVigente(dsl, cuenta.usuarioId())
                    .orElseThrow(() -> new ErrorDeNegocio(SIN_CREDENCIAL, "Esa cuenta no tiene credencial que rotar."));
            OffsetDateTime vence = p.venceEn().orElse(cuenta.venceEn());
            if (!vence.isAfter(ahora)) {
                throw new ErrorDeNegocio(VENCIMIENTO_INVALIDO, "La cuenta ya vencio: indica un vencimiento nuevo.");
            }

            char[] valor = CredencialDeServicio.nueva();
            try {
                credenciales.archivar(dsl, cuenta.usuarioId(), hashAnterior, ahora);
                credenciales.reemplazar(dsl, cuenta.usuarioId(), hasheador.hashear(valor), ahora);
                cuentas.rotar(dsl, cuentaId, vence, ahora);
                int sesiones =
                        accesos.cerrarSesionesDe(dsl, cuenta.usuarioId(), "Credencial de servicio rotada", ahora);
                emitir(
                        dsl,
                        "identidad.cuenta_servicio_credencial_rotada",
                        cuentaId,
                        ctx,
                        p.motivo(),
                        Map.of(
                                "rolCodigo",
                                cuenta.rolCodigo(),
                                "sesionesCerradas",
                                sesiones,
                                "venceEn",
                                vence.toString()));
                return new Rotacion(cuentaId, new String(valor), vence, sesiones);
            } finally {
                Arrays.fill(valor, '\0');
            }
        });
    }

    @Transactional
    public Revocacion revocar(UUID cuentaId, String motivo, String evidencia, ContextoSesion ctx) {
        ValidacionesDeCuentaDeServicio.validarMotivo(motivo);
        exigirStepUp(ctx, evidencia);
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            Fila cuenta = activa(dsl, cuentaId);
            if (cuenta.usuarioId().equals(ctx.usuarioId())) {
                throw new ErrorDeNegocio(NADIE_SE_GOBIERNA_A_SI_MISMO, "No podes gobernar tu propia cuenta.");
            }
            if (!cuentas.revocar(dsl, cuentaId, ctx.usuarioId(), motivo, ahora)) {
                throw new ErrorDeNegocio(CUENTA_REVOCADA, "Esa cuenta ya esta revocada.");
            }
            int asignaciones = (int) gobierno.sinRevocarDe(dsl, cuenta.usuarioId()).stream()
                    .filter(a -> accesos.revocar(dsl, a.id(), "Cuenta de servicio revocada", ahora) == 1)
                    .count();
            cuentas.vencerCredencial(dsl, cuenta.usuarioId(), ahora);
            cuentas.suspenderUsuario(dsl, cuenta.usuarioId());
            int sesiones = accesos.cerrarSesionesDe(dsl, cuenta.usuarioId(), "Cuenta de servicio revocada", ahora);
            emitir(
                    dsl,
                    "identidad.cuenta_servicio_revocada",
                    cuentaId,
                    ctx,
                    motivo,
                    Map.of(
                            "rolCodigo",
                            cuenta.rolCodigo(),
                            "asignacionesRevocadas",
                            asignaciones,
                            "sesionesCerradas",
                            sesiones));
            return new Revocacion(cuentaId, asignaciones, sesiones);
        });
    }

    @Transactional(readOnly = true)
    public List<ResumenDeCuentaDeServicio> listar(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> cuentas.listar(dsl, FILAS_DEL_LISTADO).stream()
                .map(ResumenDeCuentaDeServicio::de)
                .toList());
    }

    // ------------------------------------------------------------------ reglas

    private Fila activa(DSLContext dsl, UUID cuentaId) {
        Fila cuenta = cuentas.porId(dsl, cuentaId)
                .orElseThrow(() -> new ErrorDeNegocio(CUENTA_INEXISTENTE, "Esa cuenta de servicio no existe."));
        if (!"ACTIVA".equals(cuenta.estado())) {
            throw new ErrorDeNegocio(CUENTA_REVOCADA, "Esa cuenta ya esta revocada.");
        }
        return cuenta;
    }

    private void exigirStepUp(ContextoSesion ctx, String evidencia) {
        if (evidencia == null || !evidencias.consumir(ctx.usuarioId(), evidencia)) {
            throw new ErrorDeNegocio(
                    STEP_UP_REQUERIDO, "Confirma esta accion con tu segundo factor y volve a intentarlo.");
        }
    }

    // ------------------------------------------------------------------ eventos

    private void emitir(
            DSLContext dsl,
            String tipo,
            UUID agregadoId,
            ContextoSesion ctx,
            String motivo,
            Map<String, Object> detalle) {
        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("version", "1");
        carga.put("actorId", ctx.usuarioId().toString());
        carga.put("actorRol", ctx.rol());
        carga.put("origen", "PANEL_ADMIN");
        carga.put("motivo", motivo);
        carga.put("ocurridoEn", ahora().toString());
        carga.putAll(detalle);
        outbox.emitir(
                dsl,
                new EventoDominio(
                        tipo,
                        "cuenta_servicio",
                        agregadoId,
                        carga,
                        UUID.fromString(ctx.traza().id())));
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }
}

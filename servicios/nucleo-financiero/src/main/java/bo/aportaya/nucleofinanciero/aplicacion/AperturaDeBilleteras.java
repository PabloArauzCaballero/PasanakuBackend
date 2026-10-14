package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio.Apertura;
import bo.aportaya.nucleofinanciero.infraestructura.DestinoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carril Q · quien abre las billeteras {@code USUARIO} y {@code GRUPO} (S4 y S5 de la tabla de sagas del plan 20).
 *
 * <p>Antes NADIE las abria: sin la billetera del grupo, {@code aportes.aporte_cobrado} se rechazaba (AP-CU12-02) y sin la de
 * la persona {@code GET /billetera/cuentas/propia} daba 422 AP-CU13-01 a todo el mundo. Es coreografia por eventos
 * (CU-01 paso 9-10 y CU-20 paso 3): {@code identidad.usuario_registrado} abre la cuenta de la persona y
 * {@code grupos.grupo_creado} la del grupo (R-GRP-04: el titular es el GRUPO, nunca el organizador). Cada consumo es:
 *
 * <ul>
 *   <li><b>Idempotente por evento</b>: la marca {@code evento_consumido} va en la MISMA transaccion que la cuenta.
 *   <li><b>Idempotente por titular</b>: si el titular ya tiene una cuenta en esa moneda (en cualquier estado, tambien
 *       {@code CERRADA}: un reproceso no reabre lo que la persona cerro) no se crea otra; el indice unico R-BIL-04 es la
 *       ultima barrera ante dos eventos distintos del mismo titular al mismo tiempo.
 *   <li><b>Fuera de orden</b>: si el usuario o el grupo todavia no existe en su servicio, la clave foranea rechaza el
 *       alta, la transaccion se revierte (tampoco queda marca) y el reproceso la abre cuando llegue.
 * </ul>
 *
 * <p><b>Supuestos declarados (decision delegada por el dueno, 2026-10-11; confirma Cumplimiento/Producto):</b> (1) solo en
 * bolivianos: el plan de cuentas minimo solo tiene cuentas en BOB (una cuenta en USD se abrira cuando Contabilidad entregue
 * el mapeo); (2) la persona nace {@code SIMPLIFICADA} (CU-01) y la cuenta en el estado de
 * {@code aportaya.billetera.apertura.estado-inicial-usuario}: {@code ACTIVA} por omision, porque quien impide operar a una
 * persona sin verificar es {@code identidad} (no inicia sesion hasta que su KYC se aprueba); {@code EN_APERTURA} deja la
 * cuenta cerrada a toda operacion hasta que exista una senal de activacion, que hoy NO existe; (3) la cuenta del grupo nace
 * {@code ACTIVA} y {@code ESTANDAR}, sin politica P2P (el grupo no transfiere: R-GRP-04). La cuenta contable espejo no se
 * crea: el mayor se mueve por cuentas logicas del plan (SALDO_USUARIOS / SALDO_GRUPOS).
 */
@Service
public class AperturaDeBilleteras {

    public static final String CONSUMIDOR_USUARIOS = "nucleo-financiero.usuarios-registrados";
    public static final String CONSUMIDOR_GRUPOS = "nucleo-financiero.grupos-creados";

    private static final Moneda MONEDA = Moneda.BOB;
    private static final Set<String> ESTADOS_DE_INICIO = Set.of("ACTIVA", "EN_APERTURA");

    /** El identificador de proceso del actor Sistema; el mismo que usa el consumo de aportes. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-00000000a5e2");

    public enum Resultado {
        ABIERTA,
        YA_EXISTIA,
        DUPLICADO
    }

    public record UsuarioRegistrado(UUID eventoId, UUID correlationId, UUID usuarioId) {}

    public record GrupoCreado(UUID eventoId, UUID correlationId, UUID grupoId) {}

    public record Salida(Resultado resultado, Optional<UUID> cuentaId) {}

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final DestinoRepositorio destinos;
    private final Outbox outbox;
    private final Reloj reloj;
    private final String estadoInicialDelUsuario;
    private final Consumidos consumidos = new Consumidos("nucleo_financiero");

    public AperturaDeBilleteras(
            Datos datos,
            CuentaBilleteraRepositorio cuentas,
            DestinoRepositorio destinos,
            Outbox outbox,
            Reloj reloj,
            @Value("${aportaya.billetera.apertura.estado-inicial-usuario:ACTIVA}") String estadoInicialDelUsuario) {
        if (!ESTADOS_DE_INICIO.contains(estadoInicialDelUsuario)) {
            throw new IllegalStateException(
                    "aportaya.billetera.apertura.estado-inicial-usuario debe ser ACTIVA o EN_APERTURA, no «"
                            + estadoInicialDelUsuario + "»");
        }
        this.datos = datos;
        this.cuentas = cuentas;
        this.destinos = destinos;
        this.outbox = outbox;
        this.reloj = reloj;
        this.estadoInicialDelUsuario = estadoInicialDelUsuario;
    }

    @Transactional
    public Salida abrirLaDeUsuario(UsuarioRegistrado e) {
        return datos.conContexto(sistema(e.correlationId()), dsl -> {
            if (!consumidos.registrar(dsl, e.eventoId(), CONSUMIDOR_USUARIOS)) {
                return new Salida(Resultado.DUPLICADO, Optional.empty());
            }
            var existente = destinos.cuentaDeUsuarioEn(dsl, e.usuarioId(), MONEDA.name());
            if (existente.isPresent()) {
                return new Salida(Resultado.YA_EXISTIA, existente);
            }
            // Sin politica no hay plazo de retencion ni regla P2P: abrir sin ella dejaria una cuenta que niega todo.
            // Es un faltante de configuracion (semilla `politica_billetera`), no un rechazo de negocio: se reintenta.
            UUID politica = cuentas.politicaVigente(dsl, MONEDA)
                    .orElseThrow(() -> new IllegalStateException(
                            "No hay una politica de billetera vigente en " + MONEDA + ": no se abre la cuenta."));
            return abrir(
                    dsl,
                    e.correlationId(),
                    "USUARIO",
                    "BOB-",
                    e.usuarioId(),
                    estadoInicialDelUsuario,
                    "SIMPLIFICADA",
                    Optional.of(politica));
        });
    }

    @Transactional
    public Salida abrirLaDeGrupo(GrupoCreado e) {
        return datos.conContexto(sistema(e.correlationId()), dsl -> {
            if (!consumidos.registrar(dsl, e.eventoId(), CONSUMIDOR_GRUPOS)) {
                return new Salida(Resultado.DUPLICADO, Optional.empty());
            }
            var existente = destinos.cuentaDeGrupoEn(dsl, e.grupoId(), MONEDA.name());
            if (existente.isPresent()) {
                return new Salida(Resultado.YA_EXISTIA, existente);
            }
            return abrir(dsl, e.correlationId(), "GRUPO", "GRP-", e.grupoId(), "ACTIVA", "ESTANDAR", Optional.empty());
        });
    }

    private Salida abrir(
            DSLContext dsl,
            UUID correlationId,
            String tipo,
            String prefijo,
            UUID titular,
            String estado,
            String nivel,
            Optional<UUID> politica) {
        UUID id = UUID.randomUUID();
        // Determinista por titular: un reintento del mismo alta propone el mismo numero.
        String numero = prefijo + titular.toString().replace("-", "").substring(0, 15);
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        boolean abierta =
                cuentas.abrir(dsl, new Apertura(id, numero, tipo, titular, MONEDA, estado, nivel, politica, ahora));
        if (!abierta) {
            // Otro evento del mismo titular gano la carrera (R-BIL-04): su cuenta es la buena.
            var ganadora = "USUARIO".equals(tipo)
                    ? destinos.cuentaDeUsuarioEn(dsl, titular, MONEDA.name())
                    : destinos.cuentaDeGrupoEn(dsl, titular, MONEDA.name());
            return new Salida(
                    Resultado.YA_EXISTIA,
                    Optional.of(ganadora.orElseThrow(() -> new IllegalStateException(
                            "No se pudo abrir la cuenta " + numero + " y el titular no tiene otra."))));
        }
        outbox.emitir(
                dsl,
                new EventoDominio(
                        "nucleo_financiero.billetera_abierta",
                        "cuenta_billetera",
                        id,
                        Map.of(
                                "cuentaId",
                                id.toString(),
                                "tipo",
                                tipo,
                                "titularId",
                                titular.toString(),
                                "moneda",
                                MONEDA.name(),
                                "estado",
                                estado,
                                "nivelDebidaDiligencia",
                                nivel),
                        correlationId));
        return new Salida(Resultado.ABIERTA, Optional.of(id));
    }

    private static ContextoSesion sistema(UUID correlationId) {
        return ContextoSesion.deSistema(PROCESO, new Traza(correlationId.toString()));
    }
}

package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.GrupoNuevo;
import bo.aportaya.grupos.infraestructura.CreacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-20 · Crear el grupo — <b>la parte que es de {@code grupos}</b>.
 *
 * <p>El caso de uso pide que en la misma transaccion se congele el tarifario
 * ({@code tarifas.tarifa_congelada_grupo}), se abra la cuenta del grupo
 * ({@code nucleo_financiero.cuenta_billetera}) y su espejo contable. Los tres viven en
 * esquemas ajenos, y {@code svc_grupos} no escribe ahi.
 *
 * <p>Misma respuesta que en CU-01, por la misma razon: <b>coreografia</b>. El grupo
 * nace {@code BORRADOR} y emite {@code grupos.grupo_creado}; {@code tarifas} congela
 * el precio y {@code nucleo-financiero} abre la cuenta al consumirlo. El grupo pasa a
 * {@code ABIERTO_A_INSCRIPCION} cuando {@code tarifas} responde ({@link AbrirInscripcionDelGrupo}; la apertura no
 * espera a la cuenta del grupo: supuesto A1, a confirmar con producto y con ese servicio).
 *
 * <p>Que nazca en borrador no es un rodeo: <b>un grupo sin precio congelado no puede
 * recibir a nadie</b>, porque el precio es justamente lo que cada participante acepta
 * al firmar el reglamento. Dejarlo abierto antes seria pedirle a alguien que se
 * comprometa a un costo que todavia no existe.
 */
@Service
public class CU20CrearGrupo {

    private final Datos datos;
    private final CreacionRepositorio creacion;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Ids ids;

    /**
     * El quorum por omision, como FRACCION —{@code quorum_decisiones} es
     * {@code numeric(4,3)}—. Viene de configuracion y no de una constante: cuanto
     * hace falta para que un grupo decida es una decision de producto, y la regla
     * `sin-umbral-literal` tiene razon en no dejarla escrita en el codigo.
     */
    private final BigDecimal quorumPorOmision;

    public CU20CrearGrupo(
            Datos datos,
            CreacionRepositorio creacion,
            Outbox outbox,
            Reloj reloj,
            Ids ids,
            @org.springframework.beans.factory.annotation.Value("${aportaya.grupos.quorum-por-omision}")
                    BigDecimal quorumPorOmision) {
        this.datos = datos;
        this.creacion = creacion;
        this.outbox = outbox;
        this.reloj = reloj;
        this.ids = ids;
        this.quorumPorOmision = quorumPorOmision;
    }

    @Transactional
    public SalidaCreacion ejecutar(EntradaCreacion entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        if (entrada.claveIdempotencia() == null) {
            throw new ErrorDeNegocio(CodigoError.de(20, 2), "Falta la clave de idempotencia del alta.");
        }

        // El precio no puede quedar indefinido: sin tarifario vigente no hay grupo.
        if (entrada.tarifarioVigenteId().isEmpty()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(20, 1), "No hay un tarifario vigente, asi que todavia no se puede crear el grupo.");
        }
        if (entrada.organizador().isEmpty() || !entrada.organizadorHabilitado()) {
            throw new ErrorDeNegocio(CodigoError.de(20, 2), "Ese organizador no esta habilitado.");
        }
        if (!entrada.licenciaHabilitaElServicio()) {
            throw new ErrorDeNegocio(CodigoError.de(20, 4), "El servicio no esta habilitado en este momento.");
        }

        return datos.conContexto(ctx, dsl -> {
            // En la huella entra la etapa PEDIDA, no la minima derivada: omitida, la minima sale de la etapa actual del
            // organizador, que puede cambiar entre el alta y su reintento; y las altas de antes conservan su huella.
            String huella = TextoDelReglamento.hash(entrada.datos().toString() + "|" + entrada.organizador() + "|"
                    + entrada.permitePermutaDeTurnos()
                    + (entrada.etapaMinimaPedida() != null ? "|" + entrada.etapaMinimaPedida() : ""));
            var anterior = creacion.altaAnterior(dsl, ctx.usuarioId(), entrada.claveIdempotencia());
            if (anterior.isPresent()) {
                var alta = anterior.get();
                if (!alta.creadorId().equals(ctx.usuarioId()) || !alta.huella().equals(huella)) {
                    throw new ErrorDeNegocio(
                            CodigoError.de(20, 2), "La clave de idempotencia pertenece a otra creación.");
                }
                return new SalidaCreacion(alta.grupoId(), alta.fondo());
            }
            exigirCupoDeOrganizador(dsl, entrada.organizador().orElseThrow(), entrada.limiteDeGrupos());
            UUID grupo = creacion.crear(
                    dsl,
                    entrada.datos(),
                    codigoPublico(),
                    entrada.organizador(),
                    quorumPorOmision,
                    entrada.reputacionMinima(),
                    ahora);
            creacion.configurar(dsl, grupo, entrada.permitePermutaDeTurnos());

            String reglamento = TextoDelReglamento.de(entrada.datos());
            creacion.redactarReglamento(
                    dsl, grupo, reglamento, TextoDelReglamento.hash(reglamento), ctx.usuarioId(), ahora);
            creacion.abrirCupos(dsl, grupo, entrada.datos().cupos(), ahora);
            creacion.guardarAlta(
                    dsl,
                    grupo,
                    ctx.usuarioId(),
                    entrada.claveIdempotencia(),
                    huella,
                    entrada.datos().fondoPorPeriodo().toString(),
                    entrada.nombreDelCreador());

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.grupo_creado",
                            "grupo",
                            grupo,
                            Map.of(
                                    "grupoId", grupo.toString(),
                                    "tarifarioId",
                                            entrada.tarifarioVigenteId()
                                                    .orElseThrow()
                                                    .toString()),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaCreacion(grupo, entrada.datos().fondoPorPeriodo().toString());
        });
    }

    /**
     * Si esta clave ya creo un grupo de quien llama. Se pregunta ANTES de pedirle la etapa a {@code transparencia}:
     * el reintento de un alta hecha devuelve el grupo original aunque transparencia no conteste o la etapa haya
     * cambiado (regla 96.3.3). La huella la sigue comparando {@link #ejecutar}.
     */
    @Transactional(readOnly = true)
    public boolean yaCreado(UUID claveIdempotencia, ContextoSesion ctx) {
        if (claveIdempotencia == null) return false;
        return datos.conContexto(ctx, dsl -> creacion.altaAnterior(dsl, ctx.usuarioId(), claveIdempotencia)
                .isPresent());
    }

    /**
     * Limite de grupos simultaneos de la habilitacion (CU-20, {@code AP-CU20-06}). Cuentan los
     * grupos que siguen vivos; los finalizados, cancelados o disueltos no. Un candado por organizador dentro de la
     * transaccion: sin el, dos altas al mismo tiempo cuentan lo mismo y pasan las dos.
     */
    private static void exigirCupoDeOrganizador(org.jooq.DSLContext dsl, UUID organizador, int limite) {
        int vivos = Datos.comoSistema(dsl, () -> {
            dsl.fetch("SELECT pg_advisory_xact_lock(hashtext(?))", "cu20-organizador-" + organizador);
            return dsl.fetchOne(
                            """
                            SELECT count(*)::int FROM grupos.grupo
                             WHERE organizador_id = ?
                               AND estado NOT IN ('FINALIZADO', 'CANCELADO', 'DISUELTO_ANTICIPADAMENTE')
                            """,
                            organizador)
                    .get(0, Integer.class);
        });
        if (vivos >= limite) {
            throw new ErrorDeNegocio(
                    CodigoError.de(20, 6),
                    "Ya organizas %d grupos, el maximo que te permite tu habilitacion. Termina o cancela uno antes de crear otro."
                            .formatted(vivos));
        }
    }

    private String codigoPublico() {
        return "GR-" + ids.nuevo().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }

    public record EntradaCreacion(
            GrupoNuevo datos,
            Optional<UUID> organizador,
            boolean organizadorHabilitado,
            Optional<UUID> tarifarioVigenteId,
            boolean licenciaHabilitaElServicio,
            boolean permitePermutaDeTurnos,
            UUID claveIdempotencia,
            int limiteDeGrupos,
            String nombreDelCreador,
            BigDecimal reputacionMinima,
            String etapaMinimaPedida) {

        /** Con la minima ya resuelta y sin etapa pedida (la del organizador por omision). */
        @SuppressWarnings("checkstyle:ParameterNumber")
        public EntradaCreacion(
                GrupoNuevo datos,
                Optional<UUID> organizador,
                boolean organizadorHabilitado,
                Optional<UUID> tarifarioVigenteId,
                boolean licenciaHabilitaElServicio,
                boolean permitePermutaDeTurnos,
                UUID claveIdempotencia,
                int limiteDeGrupos,
                String nombreDelCreador,
                BigDecimal reputacionMinima) {
            this(
                    datos,
                    organizador,
                    organizadorHabilitado,
                    tarifarioVigenteId,
                    licenciaHabilitaElServicio,
                    permitePermutaDeTurnos,
                    claveIdempotencia,
                    limiteDeGrupos,
                    nombreDelCreador,
                    reputacionMinima,
                    null);
        }

        /**
         * Sin etapa minima (reputacion_minima 0): la forma de antes de las etapas, para los llamadores que no la
         * resuelven. La API siempre la resuelve ({@code MapeoDeAltaDeGrupo}).
         */
        @SuppressWarnings("checkstyle:ParameterNumber")
        public EntradaCreacion(
                GrupoNuevo datos,
                Optional<UUID> organizador,
                boolean organizadorHabilitado,
                Optional<UUID> tarifarioVigenteId,
                boolean licenciaHabilitaElServicio,
                boolean permitePermutaDeTurnos,
                UUID claveIdempotencia,
                int limiteDeGrupos,
                String nombreDelCreador) {
            this(
                    datos,
                    organizador,
                    organizadorHabilitado,
                    tarifarioVigenteId,
                    licenciaHabilitaElServicio,
                    permitePermutaDeTurnos,
                    claveIdempotencia,
                    limiteDeGrupos,
                    nombreDelCreador,
                    BigDecimal.ZERO,
                    null);
        }
    }

    public record SalidaCreacion(UUID grupoId, String fondoPorPeriodo) {}
}

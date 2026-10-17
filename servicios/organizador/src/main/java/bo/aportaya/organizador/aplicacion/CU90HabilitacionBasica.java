package bo.aportaya.organizador.aplicacion;

import bo.aportaya.organizador.dominio.NivelDeOrganizador;
import bo.aportaya.organizador.infraestructura.ContratoRepositorio;
import bo.aportaya.organizador.infraestructura.OrganizadorRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-90 (vía básica): un participante con KYC aprobado se habilita como organizador
 * APRENDIZ aceptando el contrato de organizador desde la app, sin pasar por la bandeja.
 *
 * <p>Decisión del producto (2026-10-17): con cientos de miles de altas, aprobar a mano a
 * cada organizador no escala. El riesgo lo acotan el nivel de entrada (los límites de
 * {@code aportaya.organizador.limite-inicial-*}), el contrato firmado que exige R-ORG-02 y
 * la evaluación de desempeño (CU-92), que sube o baja el nivel.
 *
 * <p>El texto del contrato es <b>provisional y solo para TEST</b>: el repositorio no tiene
 * el texto legal definitivo. Se rotula como tal y se reemplaza subiendo {@link #VERSION}.
 *
 * <p>La ficha y el contrato los escribe el sistema ({@link Datos#comoSistema}): la política
 * de filas no deja al titular escribir su propio organizador, porque sería subirse los
 * límites. Antes de entrar al bloque se comprobó el permiso (PARTICIPANTE, es decir KYC
 * aprobado) y la aceptación expresa del contrato.
 */
@Service
public class CU90HabilitacionBasica {

    /** Cabe en {@code contrato_organizador.version} (VARCHAR(20)). */
    public static final String VERSION = "APR-PROV-TEST-2610";

    public static final String TITULO =
            "Contrato de organizador (nivel Aprendiz) — PROVISIONAL DE PRUEBA, no válido para producción";

    private static final String ESTADO_HABILITADO = "HABILITADO";

    private final Datos datos;
    private final OrganizadorRepositorio organizadores;
    private final ContratoRepositorio contratos;
    private final Outbox outbox;
    private final Reloj reloj;
    private final int limiteInicialDeGrupos;
    private final BigDecimal limiteInicialDeMonto;

    public CU90HabilitacionBasica(
            Datos datos,
            OrganizadorRepositorio organizadores,
            ContratoRepositorio contratos,
            Outbox outbox,
            Reloj reloj,
            @Value("${aportaya.organizador.limite-inicial-de-grupos}") int limiteInicialDeGrupos,
            @Value("${aportaya.organizador.limite-inicial-de-monto}") BigDecimal limiteInicialDeMonto) {
        this.datos = datos;
        this.organizadores = organizadores;
        this.contratos = contratos;
        this.outbox = outbox;
        this.reloj = reloj;
        this.limiteInicialDeGrupos = limiteInicialDeGrupos;
        this.limiteInicialDeMonto = limiteInicialDeMonto;
    }

    /** Lo que la persona lee antes de aceptar: siempre el mismo texto para la misma versión. */
    public Contrato contrato() {
        String obligaciones = obligaciones();
        String causales = causales();
        return new Contrato(
                VERSION,
                TITULO,
                obligaciones,
                causales,
                limiteInicialDeGrupos,
                limiteInicialDeMonto,
                hash(TITULO + "\n" + obligaciones + "\n" + causales));
    }

    /** Si ya es organizador habilitado con contrato vigente firmado. Solo lee lo propio. */
    @Transactional(readOnly = true)
    public Estado estado(ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> estadoDe(dsl, ctx.usuarioId()));
    }

    /**
     * Acepta el contrato: crea la ficha Aprendiz habilitada y el contrato, todavía sin firmar.
     * La firma la pone {@link #firmar} con el token que emite identidad. Idempotente por
     * usuario: repetirlo devuelve la misma ficha y el mismo contrato pendiente.
     */
    @Transactional
    public Resultado habilitar(boolean aceptaContrato, String version, ContextoSesion ctx) {
        if (!aceptaContrato) {
            throw new ErrorDeNegocio(CodigoError.de(90, 9), "Para organizar grupos hay que aceptar el contrato.");
        }
        if (!VERSION.equals(version)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(90, 10), "El contrato de organizador cambió. Volvé a leerlo antes de aceptarlo.");
        }
        Contrato texto = contrato();
        OffsetDateTime ahora = reloj.ahora().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
        UUID usuarioId = ctx.usuarioId();

        return datos.conContexto(ctx, dsl -> {
            // Un pedido por persona a la vez: dos toques seguidos no crean dos fichas.
            dsl.execute("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", "organizador-basico:" + usuarioId);
            return Datos.comoSistema(dsl, () -> {
                boolean esNueva = false;
                Optional<OrganizadorRepositorio.Organizador> existente = organizadores.porUsuario(dsl, usuarioId);
                UUID organizadorId;
                if (existente.isPresent()) {
                    if (!ESTADO_HABILITADO.equals(existente.get().estado())) {
                        throw new ErrorDeNegocio(
                                CodigoError.de(90, 11),
                                "Tu habilitación de organizador no está activa. Escribinos a soporte para revisarla.");
                    }
                    organizadorId = existente.get().id();
                } else {
                    organizadorId = organizadores.crear(
                            dsl,
                            usuarioId,
                            ESTADO_HABILITADO,
                            NivelDeOrganizador.APRENDIZ.name(),
                            limiteInicialDeGrupos,
                            limiteInicialDeMonto,
                            ahora);
                    esNueva = true;
                }

                var vigente = contratos.vigente(dsl, organizadorId, ahora.toLocalDate());
                UUID contratoId;
                // `vigente` solo devuelve contratos ya firmados: si hay uno, la persona ya aceptó.
                // Si hay uno emitido sin firmar (aceptó y la firma no llegó), se reusa.
                if (vigente.isPresent()) {
                    contratoId = vigente.get().id();
                } else {
                    contratoId = pendienteDeFirma(dsl, organizadorId).orElse(null);
                    if (contratoId == null) {
                        contratoId = contratos.emitir(
                                dsl,
                                organizadorId,
                                texto.version(),
                                texto.contenidoHash(),
                                texto.obligaciones(),
                                texto.causalesRescision(),
                                ahora.toLocalDate());
                        esNueva = true;
                    }
                }

                if (esNueva) {
                    outbox.emitir(
                            dsl,
                            new EventoDominio(
                                    "organizador.contrato_aceptado",
                                    "organizador",
                                    organizadorId,
                                    Map.of(
                                            "usuarioId", usuarioId.toString(),
                                            "nivel", NivelDeOrganizador.APRENDIZ.name(),
                                            "contratoId", contratoId.toString(),
                                            "version", texto.version(),
                                            "contenidoHash", texto.contenidoHash()),
                                    UUID.fromString(ctx.traza().id())));
                }
                return new Resultado(
                        organizadorId, contratoId, texto.contenidoHash(), NivelDeOrganizador.APRENDIZ.name(), esNueva);
            });
        });
    }

    /**
     * Firma el contrato pendiente con el token {@code FIRMA_CONTRATO} que emitió identidad
     * ({@code fk_contrato_organizador_token_firma_id}). Solo el titular; firmar dos veces no
     * pisa la primera firma (R-ORG-03).
     */
    @Transactional
    public boolean firmar(UUID contratoId, UUID tokenFirmaId, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            var contrato = contratos
                    .ver(dsl, contratoId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(91, 1), "Ese contrato no existe."));
            var organizador = organizadores.ver(dsl, contrato.organizadorId()).orElseThrow();
            if (!organizador.usuarioId().equals(ctx.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(91, 4), "El contrato lo firma el organizador, no un tercero.");
            }
            return Datos.comoSistema(dsl, () -> {
                boolean firmado = contratos.firmar(dsl, contratoId, tokenFirmaId, ahora);
                if (firmado) {
                    outbox.emitir(
                            dsl,
                            new EventoDominio(
                                    "organizador.contrato_firmado",
                                    "contrato_organizador",
                                    contratoId,
                                    Map.of(
                                            "organizadorId",
                                                    contrato.organizadorId().toString(),
                                            "firmadoEn", ahora.toString()),
                                    UUID.fromString(ctx.traza().id())));
                }
                return firmado;
            });
        });
    }

    private static Optional<UUID> pendienteDeFirma(org.jooq.DSLContext dsl, UUID organizadorId) {
        return dsl.fetchOptional(
                        """
                        SELECT id FROM organizador.contrato_organizador WHERE organizador_id = ? AND firmado_en IS NULL AND rescindido_en IS NULL ORDER BY vigente_desde DESC LIMIT 1
                        """,
                        organizadorId)
                .map(f -> f.get(0, UUID.class));
    }

    private Estado estadoDe(org.jooq.DSLContext dsl, UUID usuarioId) {
        var organizador = organizadores.porUsuario(dsl, usuarioId);
        if (organizador.isEmpty()) {
            return new Estado(null, false, null);
        }
        var o = organizador.get();
        var vigente = contratos.vigente(
                dsl, o.id(), reloj.ahora().atOffset(ZoneOffset.UTC).toLocalDate());
        return new Estado(o.id(), ESTADO_HABILITADO.equals(o.estado()) && vigente.isPresent(), o.nivel());
    }

    private String obligaciones() {
        return """
                1. Administrás el grupo según su reglamento y lo que aceptó cada participante.
                2. No recibís ni guardás dinero del grupo: los aportes y las entregas los mueve AportaYa.
                3. No cobrás comisión ni ningún cargo a los participantes.
                4. Tratás con reserva los datos de los participantes y no los usás para otra cosa.
                5. Como organizador Aprendiz podés tener hasta %d grupos activos y administrar hasta Bs %s.
                6. Tu desempeño se evalúa periódicamente y puede subir o bajar tu nivel."""
                .formatted(limiteInicialDeGrupos, limiteInicialDeMonto.toPlainString());
    }

    private static String causales() {
        return """
                AportaYa puede rescindir este contrato si incumplís el reglamento del grupo, pedís dinero \
                por fuera de AportaYa, cobrás comisiones o usás indebidamente datos de los participantes. \
                Vos podés rescindirlo cuando quieras; los grupos que ya administrás no quedan sin administrar."""
                .strip();
    }

    private static String hash(String contenido) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(contenido.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 no disponible", imposible);
        }
    }

    public record Contrato(
            String version,
            String titulo,
            String obligaciones,
            String causalesRescision,
            int limiteDeGrupos,
            BigDecimal limiteDeMonto,
            String contenidoHash) {}

    public record Estado(UUID organizadorId, boolean habilitado, String nivel) {}

    public record Resultado(UUID organizadorId, UUID contratoId, String contenidoHash, String nivel, boolean esNueva) {}
}

package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.GrupoNuevo;
import bo.aportaya.grupos.infraestructura.CreacionRepositorio;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * La version nueva del reglamento que publica {@link CU69IniciarGrupo}. Salio de ese caso de uso (limite de 300 lineas)
 * sin cambiar lo que hace; la transaccion la abre el inicio.
 */
final class ReglamentoAlIniciar {

    private ReglamentoAlIniciar() {}

    /**
     * Reglamento al achicar (decision del dueño 2026-10-30): en la MISMA transaccion del inicio se publica la version
     * N+1 con los numeros reales del grupo achicado (participantes y fondo por periodo), armada igual que la del alta
     * ({@link TextoDelReglamento#de}). La anterior deja de regir ahora. Nadie la tiene aceptada al nacer:
     * cada integrante la acepta con la firma del reglamento (el detalle le dice si le falta). La fila del grupo esta
     * tomada, asi que dos inicios no calculan la misma version; y el reintento con la misma clave sale antes, sin otra.
     */
    static void publicar(
            DSLContext dsl, UUID grupoId, Record g, int ocupados, UUID redactadoPor, OffsetDateTime ahora) {
        var achicado = new GrupoNuevo(
                g.get("nombre", String.class),
                Dinero.de(
                        g.get("monto_aporte", BigDecimal.class),
                        Moneda.valueOf(g.get("moneda", String.class).trim())),
                g.get("periodicidad", String.class),
                g.get("dia_cobro", Integer.class),
                ocupados,
                g.get("fecha_inicio", LocalDate.class));
        String texto = TextoDelReglamento.de(achicado);
        Record anterior = dsl.fetchOne(
                """
                SELECT version, clausulas_mora, clausulas_abandono FROM grupos.reglamento_grupo
                 WHERE grupo_id = ? ORDER BY version DESC LIMIT 1
                """,
                grupoId);
        int version = anterior == null ? 1 : anterior.get("version", Integer.class) + 1;
        dsl.execute(
                """
                UPDATE grupos.reglamento_grupo SET vigente_hasta = CAST(? AS timestamptz)
                 WHERE grupo_id = ? AND vigente_hasta IS NULL
                """,
                ahora,
                grupoId);
        dsl.execute(
                """
                INSERT INTO grupos.reglamento_grupo
                  (grupo_id, version, contenido, hash_contenido, clausulas_mora, clausulas_abandono,
                   vigente_desde, redactado_por)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS timestamptz), ?)
                """,
                grupoId,
                (short) version,
                texto,
                TextoDelReglamento.hash(texto),
                anterior == null ? CreacionRepositorio.CLAUSULAS_MORA : anterior.get("clausulas_mora", String.class),
                anterior == null
                        ? CreacionRepositorio.CLAUSULAS_ABANDONO
                        : anterior.get("clausulas_abandono", String.class),
                ahora,
                redactadoPor);
    }
}

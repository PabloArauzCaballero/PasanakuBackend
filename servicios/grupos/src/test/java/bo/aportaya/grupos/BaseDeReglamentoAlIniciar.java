package bo.aportaya.grupos;

import bo.aportaya.grupos.aplicacion.CU69FirmarReglamento;
import bo.aportaya.grupos.aplicacion.ConsultarDetalleDelGrupo;
import bo.aportaya.grupos.infraestructura.CalendarioDelGrupoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

/**
 * El armado comun de las pruebas del reglamento al achicar (CU-69, 2026-10-30): un grupo de 10 con 8 que firmaron la
 * version 1, y el inicio, la aceptacion, el detalle y el sorteo con el rol de base del servicio
 * ({@code SET LOCAL ROLE svc_grupos}). Datos sinteticos.
 */
abstract class BaseDeReglamentoAlIniciar extends BaseDeInicio {

    protected static final String TEXTO_V1 = "Reglamento de Grupo de prueba, version del alta (sintetica).";

    protected CU69FirmarReglamento firma;
    protected ConsultarDetalleDelGrupo detalle;

    @BeforeEach
    void prepararFirmaYDetalle() {
        firma = new CU69FirmarReglamento(new Datos(dsl), new CalendarioDelGrupoRepositorio(), Reloj.delSistema());
        detalle = new ConsultarDetalleDelGrupo(new Datos(dsl));
    }

    /** Un grupo de 10 con 8 ocupados que firmaron la version 1 (su aceptacion existe), listo para iniciar. */
    protected UUID grupoConVersion1() {
        UUID grupo = grupoAbierto(10, del1Al(8), Set.of());
        UUID v1 = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO grupos.reglamento_grupo
                  (id, grupo_id, version, contenido, hash_contenido, clausulas_mora, clausulas_abandono,
                   vigente_desde, redactado_por)
                VALUES (?, ?, 1, ?, ?, 'Mora sintetica.', 'Abandono sintetico.', now(), ?)
                """,
                v1,
                grupo,
                TEXTO_V1,
                sha256(TEXTO_V1),
                organizador);
        for (UUID participante : participantesConCupo(grupo)) {
            dsl.execute(
                    """
                    INSERT INTO grupos.aceptacion_reglamento
                      (reglamento_id, participante_id, aceptado_en, hash_firmado, ip_origen)
                    VALUES (?, ?, now(), ?, '127.0.0.1'::inet)
                    """,
                    v1,
                    participante,
                    sha256(TEXTO_V1));
        }
        return grupo;
    }

    protected List<UUID> participantesConCupo(UUID grupo) {
        return dsl.fetch(
                        "SELECT participante_id FROM grupos.cupo WHERE grupo_id = ? AND estado = 'OCUPADO' ORDER BY numero",
                        grupo)
                .getValues(0, UUID.class);
    }

    /** El usuario que ocupa ese cupo, por numero. */
    protected UUID usuarioDelCupo(UUID grupo, int numero) {
        return dsl.fetchOne(
                        """
                        SELECT p.usuario_id FROM grupos.cupo c JOIN grupos.participante p ON p.id = c.participante_id
                         WHERE c.grupo_id = ? AND c.numero = ?
                        """,
                        grupo,
                        (short) numero)
                .get(0, UUID.class);
    }

    protected void iniciarConElRolDelServicio(UUID grupo, UUID clave) {
        transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return inicio.iniciar(grupo, clave, de(organizador));
        });
    }

    protected CU69FirmarReglamento.Firmado aceptarConElRolDelServicio(UUID grupo, String hash, UUID quien) {
        return transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return firma.firmar(grupo, hash, "127.0.0.1", de(quien));
        });
    }

    protected ConsultarDetalleDelGrupo.Detalle detalleDe(UUID grupo, UUID quien) {
        return transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return detalle.consultar(grupo, de(quien));
        });
    }

    protected String hashVigente(UUID grupo) {
        return dsl.fetchOne(
                        "SELECT hash_contenido FROM grupos.reglamento_grupo WHERE grupo_id = ? ORDER BY version DESC LIMIT 1",
                        grupo)
                .get(0, String.class);
    }

    protected int aceptacionesDeLaVersion(UUID grupo, int version) {
        return contar(
                """
                SELECT count(*) FROM grupos.aceptacion_reglamento a
                  JOIN grupos.reglamento_grupo r ON r.id = a.reglamento_id
                 WHERE r.grupo_id = ? AND r.version = ?
                """,
                grupo,
                (short) version);
    }

    protected void comprometerConElRolDelServicio(UUID grupo) {
        transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return sortear.comprometer(grupo, List.of(), java.util.Optional.empty(), de(organizador));
        });
    }

    protected static String sha256(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

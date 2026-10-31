package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU69FirmarReglamento;
import bo.aportaya.grupos.aplicacion.ConsultarDetalleDelGrupo;
import bo.aportaya.grupos.infraestructura.CalendarioDelGrupoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · Reglamento al achicar (decision del dueño 2026-10-30): al iniciar, en la MISMA transaccion, el grupo publica la
 * version N+1 del reglamento con los numeros reales; nadie la tiene aceptada; cada integrante ACTIVO la acepta por la
 * firma del reglamento, una sola vez; y el sorteo espera a que la acepten todos. Sobre PostgreSQL real con el rol de
 * base del servicio ({@code SET LOCAL ROLE svc_grupos}); datos sinteticos.
 */
class CU69ReglamentoAlIniciarTest extends BaseDeInicio {

    private static final String TEXTO_V1 = "Reglamento de Grupo de prueba, version del alta (sintetica).";

    private CU69FirmarReglamento firma;
    private ConsultarDetalleDelGrupo detalle;

    @BeforeEach
    void prepararFirmaYDetalle() {
        firma = new CU69FirmarReglamento(new Datos(dsl), new CalendarioDelGrupoRepositorio(), Reloj.delSistema());
        detalle = new ConsultarDetalleDelGrupo(new Datos(dsl));
    }

    /** Un grupo de 10 con 8 ocupados que firmaron la version 1 (su aceptacion existe), listo para iniciar. */
    private UUID grupoConVersion1() {
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

    private List<UUID> participantesConCupo(UUID grupo) {
        return dsl.fetch(
                        "SELECT participante_id FROM grupos.cupo WHERE grupo_id = ? AND estado = 'OCUPADO' ORDER BY numero",
                        grupo)
                .getValues(0, UUID.class);
    }

    /** El usuario que ocupa ese cupo, por numero. */
    private UUID usuarioDelCupo(UUID grupo, int numero) {
        return dsl.fetchOne(
                        """
                        SELECT p.usuario_id FROM grupos.cupo c JOIN grupos.participante p ON p.id = c.participante_id
                         WHERE c.grupo_id = ? AND c.numero = ?
                        """,
                        grupo,
                        (short) numero)
                .get(0, UUID.class);
    }

    private void iniciarConElRolDelServicio(UUID grupo, UUID clave) {
        transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return inicio.iniciar(grupo, clave, de(organizador));
        });
    }

    private CU69FirmarReglamento.Firmado aceptarConElRolDelServicio(UUID grupo, String hash, UUID quien) {
        return transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return firma.firmar(grupo, hash, "127.0.0.1", de(quien));
        });
    }

    private ConsultarDetalleDelGrupo.Detalle detalleDe(UUID grupo, UUID quien) {
        return transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return detalle.consultar(grupo, de(quien));
        });
    }

    private String hashVigente(UUID grupo) {
        return dsl.fetchOne(
                        "SELECT hash_contenido FROM grupos.reglamento_grupo WHERE grupo_id = ? ORDER BY version DESC LIMIT 1",
                        grupo)
                .get(0, String.class);
    }

    private int aceptacionesDeLaVersion(UUID grupo, int version) {
        return contar(
                """
                SELECT count(*) FROM grupos.aceptacion_reglamento a
                  JOIN grupos.reglamento_grupo r ON r.id = a.reglamento_id
                 WHERE r.grupo_id = ? AND r.version = ?
                """,
                grupo,
                (short) version);
    }

    @Test
    @DisplayName(
            "CU-69 · iniciar 8/10 publica la version 2 con 8 participantes y el fondo real; nadie la tiene aceptada")
    void iniciarPublicaLaVersionNueva() {
        UUID grupo = grupoConVersion1();

        iniciarConElRolDelServicio(grupo, UUID.randomUUID());

        var v2 = dsl.fetchOne(
                """
                SELECT contenido, hash_contenido, vigente_hasta, redactado_por, clausulas_mora
                  FROM grupos.reglamento_grupo WHERE grupo_id = ? AND version = 2
                """,
                grupo);
        assertThat(v2).as("la version N+1 existe").isNotNull();
        String texto = v2.get("contenido", String.class);
        assertThat(texto)
                .contains("Participantes: 8.")
                .contains("Fondo de cada periodo: BOB 4000.00.")
                .contains("Aporte: BOB 500.00 por periodo, mensual, dia 5.");
        assertThat(v2.get("hash_contenido", String.class)).isEqualTo(sha256(texto));
        assertThat(v2.get("vigente_hasta", Object.class)).as("la nueva rige").isNull();
        assertThat(v2.get("redactado_por", UUID.class)).isEqualTo(organizador);
        assertThat(v2.get("clausulas_mora", String.class))
                .as("conserva las clausulas")
                .isEqualTo("Mora sintetica.");
        assertThat(contar(
                        "SELECT count(*) FROM grupos.reglamento_grupo WHERE grupo_id = ? AND version = 1 AND vigente_hasta IS NOT NULL",
                        grupo))
                .as("la anterior dejo de regir")
                .isEqualTo(1);
        assertThat(aceptacionesDeLaVersion(grupo, 2))
                .as("nadie la tiene aceptada")
                .isZero();
        assertThat(aceptacionesDeLaVersion(grupo, 1))
                .as("las firmas de la version 1 quedan")
                .isEqualTo(8);
    }

    @Test
    @DisplayName("CU-69 · el reintento del inicio con la misma clave no publica otra version")
    void reintentoNoPublicaOtraVersion() {
        UUID grupo = grupoConVersion1();
        UUID clave = UUID.randomUUID();

        iniciarConElRolDelServicio(grupo, clave);
        iniciarConElRolDelServicio(grupo, clave);

        assertThat(contar("SELECT count(*) FROM grupos.reglamento_grupo WHERE grupo_id = ?", grupo))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("CU-69 · sin reglamento previo (grupo de antes), iniciar publica la version 1")
    void sinReglamentoPrevioPublicaLaPrimera() {
        UUID grupo = grupoAbierto(10, del1Al(8), Set.of());

        iniciarConElRolDelServicio(grupo, UUID.randomUUID());

        assertThat(contar("SELECT count(*) FROM grupos.reglamento_grupo WHERE grupo_id = ? AND version = 1", grupo))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "CU-69 · el detalle dice a cada integrante que le falta aceptar la version vigente, y el sorteo espera")
    void elDetalleMuestraLaAceptacionPendiente() {
        UUID grupo = grupoConVersion1();
        iniciarConElRolDelServicio(grupo, UUID.randomUUID());

        var integrante = detalleDe(grupo, usuarioDelCupo(grupo, 2));
        assertThat(integrante.reglamentoPendienteDeAceptar()).isTrue();
        assertThat(integrante.debeFirmar()).as("no es la primera firma").isFalse();
        assertThat(integrante.reglamentoVersion()).isEqualTo(2);
        assertThat(integrante.reglamentoHash()).isEqualTo(hashVigente(grupo));

        var delOrganizador = detalleDe(grupo, organizador);
        assertThat(delOrganizador.estado()).isEqualTo("CONFORMADO");
        assertThat(delOrganizador.puedeSortear())
                .as("nadie acepto la version 2")
                .isFalse();
    }

    @Test
    @DisplayName("CU-69 · aceptar la version nueva dos veces deja UNA aceptacion; el cupo no cambia")
    void aceptarDosVecesEsIdempotente() {
        UUID grupo = grupoConVersion1();
        iniciarConElRolDelServicio(grupo, UUID.randomUUID());
        UUID juan = usuarioDelCupo(grupo, 2);
        String hash = hashVigente(grupo);
        var cupoAntes = dsl.fetchOne(
                        "SELECT id, estado, numero FROM grupos.cupo WHERE grupo_id = ? AND numero = 2", grupo)
                .intoMap();

        var primera = aceptarConElRolDelServicio(grupo, hash, juan);
        var segunda = aceptarConElRolDelServicio(grupo, hash, juan);

        assertThat(segunda.participanteId()).isEqualTo(primera.participanteId());
        assertThat(primera.grupoConformado()).isFalse();
        assertThat(primera.estadoDelGrupo()).isEqualTo("CONFORMADO");
        assertThat(contar(
                        """
                        SELECT count(*) FROM grupos.aceptacion_reglamento a
                          JOIN grupos.reglamento_grupo r ON r.id = a.reglamento_id
                         WHERE r.grupo_id = ? AND r.version = 2 AND a.participante_id = ?
                        """,
                        grupo,
                        primera.participanteId()))
                .isEqualTo(1);
        assertThat(dsl.fetchOne("SELECT id, estado, numero FROM grupos.cupo WHERE grupo_id = ? AND numero = 2", grupo)
                        .intoMap())
                .isEqualTo(cupoAntes);
        assertThat(detalleDe(grupo, juan).reglamentoPendienteDeAceptar()).isFalse();
    }

    @Test
    @DisplayName("CU-69 · aceptar con un hash que no es el de la version vigente: AP-CU69-05, sin aceptacion")
    void hashQueNoEsElVigente() {
        UUID grupo = grupoConVersion1();
        iniciarConElRolDelServicio(grupo, UUID.randomUUID());
        UUID juan = usuarioDelCupo(grupo, 2);

        assertThatThrownBy(() -> aceptarConElRolDelServicio(grupo, sha256(TEXTO_V1), juan))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU69-05"));
        assertThat(aceptacionesDeLaVersion(grupo, 2)).isZero();
    }

    @Test
    @DisplayName("CU-69 · cuando los 8 aceptan la version vigente, el organizador puede sortear")
    void conTodosAceptadosSePuedeSortear() {
        UUID grupo = grupoConVersion1();
        iniciarConElRolDelServicio(grupo, UUID.randomUUID());
        String hash = hashVigente(grupo);

        for (int numero = 1; numero <= 7; numero++) {
            aceptarConElRolDelServicio(grupo, hash, usuarioDelCupo(grupo, numero));
        }
        assertThat(detalleDe(grupo, organizador).puedeSortear()).as("falta uno").isFalse();
        aceptarConElRolDelServicio(grupo, hash, usuarioDelCupo(grupo, 8));

        assertThat(aceptacionesDeLaVersion(grupo, 2)).isEqualTo(8);
        assertThat(detalleDe(grupo, organizador).puedeSortear()).isTrue();
    }

    @Test
    @DisplayName(
            "CU-60 · el servidor no compromete el sorteo con la version nueva sin aceptar (AP-CU60-05), aunque todos"
                    + " tengan la anterior; con los 8 aceptados, si")
    void elServidorExigeLaVersionVigenteParaSortear() {
        UUID grupo = grupoConVersion1();
        iniciarConElRolDelServicio(grupo, UUID.randomUUID());
        String hash = hashVigente(grupo);
        for (int numero = 1; numero <= 7; numero++) {
            aceptarConElRolDelServicio(grupo, hash, usuarioDelCupo(grupo, numero));
        }

        assertThatThrownBy(() -> comprometerConElRolDelServicio(grupo))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU60-05"));
        assertThat(contar("SELECT count(*) FROM grupos.sorteo_turnos WHERE grupo_id = ?", grupo))
                .isZero();

        aceptarConElRolDelServicio(grupo, hash, usuarioDelCupo(grupo, 8));
        comprometerConElRolDelServicio(grupo);
        assertThat(contar("SELECT count(*) FROM grupos.sorteo_turnos WHERE grupo_id = ?", grupo))
                .isEqualTo(1);
    }

    private void comprometerConElRolDelServicio(UUID grupo) {
        transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return sortear.comprometer(grupo, List.of(), java.util.Optional.empty(), de(organizador));
        });
    }

    @Test
    @DisplayName("CU-69 · quien no integra el grupo no ve el detalle (AP-CU69-14), ni las etapas de nadie")
    void ajenoNoVeElDetalle() {
        UUID grupo = grupoConVersion1();
        UUID ajeno = fixtura.usuario();

        assertThatThrownBy(() -> detalleDe(grupo, ajeno))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU69-14"));
        assertThatThrownBy(() -> aceptarConElRolDelServicio(grupo, hashVigente(grupo), ajeno))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU69-13"));
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

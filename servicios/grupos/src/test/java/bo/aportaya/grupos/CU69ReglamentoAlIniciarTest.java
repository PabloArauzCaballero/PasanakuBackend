package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · Reglamento al achicar (decision del dueño 2026-10-30): al iniciar, en la MISMA transaccion, el grupo publica la
 * version N+1 del reglamento con los numeros reales; nadie la tiene aceptada; cada integrante ACTIVO la acepta por la
 * firma del reglamento, una sola vez; y el sorteo espera a que la acepten todos. Sobre PostgreSQL real con el rol de
 * base del servicio ({@code SET LOCAL ROLE svc_grupos}); datos sinteticos.
 */
class CU69ReglamentoAlIniciarTest extends BaseDeReglamentoAlIniciar {

    @Test
    @DisplayName(
            "Dado un grupo de 10 cupos con 8 integrantes que firmaron la version 1 del reglamento · "
                    + "Cuando el organizador lo inicia · "
                    + "Entonces se publica la version 2 con 8 participantes y el fondo real, la version 1 deja de regir y nadie tiene aceptada la version 2")
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
    @DisplayName("Dado un grupo con la version 1 del reglamento · "
            + "Cuando el inicio se repite con la misma clave · "
            + "Entonces no se publica otra version y el grupo tiene dos")
    void reintentoNoPublicaOtraVersion() {
        UUID grupo = grupoConVersion1();
        UUID clave = UUID.randomUUID();

        iniciarConElRolDelServicio(grupo, clave);
        iniciarConElRolDelServicio(grupo, clave);

        assertThat(contar("SELECT count(*) FROM grupos.reglamento_grupo WHERE grupo_id = ?", grupo))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Dado un grupo de antes sin reglamento previo y 8 de 10 cupos ocupados · "
            + "Cuando el organizador lo inicia · "
            + "Entonces se publica la version 1 del reglamento")
    void sinReglamentoPrevioPublicaLaPrimera() {
        UUID grupo = grupoAbierto(10, del1Al(8), Set.of());

        iniciarConElRolDelServicio(grupo, UUID.randomUUID());

        assertThat(contar("SELECT count(*) FROM grupos.reglamento_grupo WHERE grupo_id = ? AND version = 1", grupo))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dado un grupo recien iniciado con la version 2 del reglamento sin aceptar · "
            + "Cuando un integrante y el organizador abren el detalle · "
            + "Entonces al integrante le dice que le falta aceptar la version vigente sin pedirle la primera firma · "
            + "Y al organizador le dice que el grupo esta CONFORMADO pero todavia no puede sortear")
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
    @DisplayName("Dada una persona que no integra el grupo · "
            + "Cuando pide el detalle o intenta aceptar el reglamento vigente · "
            + "Entonces el detalle responde AP-CU69-14 y la aceptacion responde AP-CU69-13")
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
}

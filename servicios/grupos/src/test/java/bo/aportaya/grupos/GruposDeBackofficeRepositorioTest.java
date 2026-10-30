package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.ConsultarGruposDeBackoffice;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Backoffice · consulta de grupos (2026-10-29), contra Postgres real. Datos sintéticos. */
class GruposDeBackofficeRepositorioTest extends BaseDeCU68 {

    private ConsultarGruposDeBackoffice consultas() {
        return new ConsultarGruposDeBackoffice(new Datos(dsl));
    }

    private ContextoSesion operador() {
        return ContextoSesion.de(
                fixtura.usuario(),
                "ADMIN_PLATAFORMA",
                new Traza(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName(
            "listado: trae el grupo con su aporte exacto y su ocupación; el filtro por estado lo deja afuera si no coincide")
    void listado() {
        UUID grupo = fixtura.grupoConformado(3);
        // Base compartida y listado con tope (200, fecha_inicio DESC): con la fecha de hoy de la fixture, el
        // grupo entra o no según su código aleatorio. Una fecha futura lo deja siempre primero.
        dslFixtura.execute(
                """
                UPDATE grupos.grupo
                   SET fecha_inicio = CURRENT_DATE + 3650, fecha_fin_estimada = CURRENT_DATE + 3830
                 WHERE id = ?
                """,
                grupo);

        var todos = transaccion.execute(e -> consultas().listar(null, null, operador()));
        var fila = todos.stream().filter(g -> g.id().equals(grupo)).findFirst().orElseThrow();
        assertThat(fila.estado()).isEqualTo("CONFORMADO");
        assertThat(fila.montoAporte()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(fila.moneda()).isEqualTo("BOB");
        assertThat(fila.cuposTotales()).isEqualTo(3);

        var conformados = transaccion.execute(e -> consultas().listar("CONFORMADO", null, operador()));
        assertThat(conformados)
                .extracting(ConsultarGruposDeBackoffice.Grupo::id)
                .contains(grupo);
        var cancelados = transaccion.execute(e -> consultas().listar("CANCELADO", null, operador()));
        assertThat(cancelados).extracting(ConsultarGruposDeBackoffice.Grupo::id).doesNotContain(grupo);
    }

    @Test
    @DisplayName("límite: se acota a 1..200 (0 trae uno, 1000 no pasa de 200)")
    void limite() {
        fixtura.grupoConformado(3);
        fixtura.grupoConformado(3);
        java.util.List<ConsultarGruposDeBackoffice.Grupo> uno =
                transaccion.execute(e -> consultas().listar(null, 0, operador()));
        java.util.List<ConsultarGruposDeBackoffice.Grupo> muchos =
                transaccion.execute(e -> consultas().listar(null, 1000, operador()));
        assertThat(uno).hasSize(1);
        assertThat(muchos).hasSizeLessThanOrEqualTo(200);
    }

    @Test
    @DisplayName(
            "detalle: cada cupo con su número y estado; el ocupado trae alias y si organiza, el libre no trae persona")
    void detalle() {
        UUID grupo = fixtura.grupoConformado(3);
        UUID participante = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO grupos.participante
                    (id, grupo_id, usuario_id, alias, estado, es_organizador, fecha_ingreso,
                     reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                VALUES (?, ?, ?, 'Ana', 'ACTIVO', true, now(), 0, 0, 0)
                """,
                participante,
                grupo,
                fixtura.usuario());
        dslFixtura.execute(
                """
                INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en)
                VALUES (gen_random_uuid(), ?, 1, ?, 'OCUPADO', 1.00, now())
                """,
                grupo,
                participante);
        dslFixtura.execute(
                "INSERT INTO grupos.cupo (id, grupo_id, numero, estado, fraccion) VALUES (gen_random_uuid(), ?, 2, 'LIBRE', 1.00)",
                grupo);

        var d = transaccion.execute(e -> consultas().detalle(grupo, operador()));

        assertThat(d.grupo().id()).isEqualTo(grupo);
        assertThat(d.cupos()).hasSize(2);
        assertThat(d.cupos().get(0).alias()).isEqualTo("Ana");
        assertThat(d.cupos().get(0).esOrganizador()).isTrue();
        assertThat(d.cupos().get(0).estadoParticipante()).isEqualTo("ACTIVO");
        assertThat(d.cupos().get(1).estado()).isEqualTo("LIBRE");
        assertThat(d.cupos().get(1).alias()).isNull();
        assertThat(d.cupos().get(1).esOrganizador()).isFalse();
    }

    @Test
    @DisplayName("inválido: un grupo que no existe es un error de negocio, no un vacío")
    void inexistente() {
        assertThatThrownBy(() -> transaccion.execute(e -> consultas().detalle(UUID.randomUUID(), operador())))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no existe");
    }
}

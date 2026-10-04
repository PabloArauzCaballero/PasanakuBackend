package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-68 · el organizador decide sobre una solicitud de ingreso (flujo principal 4). */
class CU68AceptarIngresoTest extends BaseDeCU68 {

    private CU68AceptarIngreso decidirCU() {
        return new CU68AceptarIngreso(new Datos(dsl), new Outbox("grupos"));
    }

    private ContextoSesion como(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    /** Un grupo de 2 cupos: el organizador ocupa el 1, el 2 queda LIBRE. Devuelve {grupo, organizador, solicitante, solicitud}. */
    private UUID[] armar(boolean cupoLibre) {
        UUID grupo = fixtura.grupoConformado(3);
        UUID organizador = fixtura.usuario();
        UUID participanteOrg = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO grupos.participante
                    (id, grupo_id, usuario_id, estado, es_organizador, fecha_ingreso,
                     reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                VALUES (?, ?, ?, 'ACTIVO', true, now(), 0, 0, 0)
                """,
                participanteOrg,
                grupo,
                organizador);
        dslFixtura.execute(
                """
                INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en)
                VALUES (gen_random_uuid(), ?, 1, ?, 'OCUPADO', 1.00, now())
                """,
                grupo,
                participanteOrg);
        dslFixtura.execute(
                """
                INSERT INTO grupos.cupo (id, grupo_id, numero, estado, fraccion)
                VALUES (gen_random_uuid(), ?, 2, ?, 1.00)
                """,
                grupo,
                cupoLibre ? "LIBRE" : "OCUPADO");
        dslFixtura.execute(
                """
                INSERT INTO grupos.cupo (id, grupo_id, numero, estado, fraccion, asignado_en)
                VALUES (gen_random_uuid(), ?, 3, 'OCUPADO', 1.00, now())
                """,
                grupo);
        dslFixtura.execute("UPDATE grupos.grupo SET cupos_ocupados = ? WHERE id = ?", cupoLibre ? 2 : 3, grupo);
        UUID solicitante = fixtura.usuario();
        UUID solicitud = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO grupos.solicitud_ingreso
                    (id, grupo_id, usuario_id, cupos_solicitados, mensaje, estado, fecha_solicitud)
                VALUES (?, ?, ?, 1, 'quiero entrar', 'PENDIENTE', now())
                """,
                solicitud,
                grupo,
                solicitante);
        return new UUID[] {grupo, organizador, solicitante, solicitud};
    }

    @Test
    @DisplayName("CU-68 · aceptar: reserva el cupo, crea al participante pendiente de firma y deja el evento")
    void aceptar() {
        UUID[] a = armar(true);
        var r = transaccion.execute(
                e -> decidirCU().decidir(a[3], true, null, new java.math.BigDecimal("700"), como(a[1])));

        assertThat(r.estado()).isEqualTo("APROBADA");
        assertThat(r.participanteId()).isNotNull();
        assertThat(dslFixtura
                        .fetchOne(
                                "SELECT estado, revisada_por, fecha_resolucion FROM grupos.solicitud_ingreso WHERE id = ?",
                                a[3])
                        .intoMap())
                .containsEntry("estado", "APROBADA")
                .containsEntry("revisada_por", a[1]);
        assertThat(dslFixtura
                        .fetchOne("SELECT estado FROM grupos.participante WHERE id = ?", r.participanteId())
                        .get(0))
                .isEqualTo("ACEPTADO_PENDIENTE_FIRMA");
        assertThat(dslFixtura
                        .fetchOne("SELECT estado FROM grupos.cupo WHERE id = ?", r.cupoId())
                        .get(0))
                .isEqualTo("RESERVADO");
        assertThat(dslFixtura
                        .fetchOne("SELECT cupos_ocupados FROM grupos.grupo WHERE id = ?", a[0])
                        .get(0))
                .isEqualTo((short) 3);
        assertThat(dslFixtura
                        .fetchOne(
                                "SELECT count(*) FROM grupos.evento_dominio WHERE tipo = 'grupos.ingreso_aceptado' AND agregado_id = ?",
                                a[3])
                        .get(0))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("CU-68 · repetir la MISMA decisión devuelve lo ya resuelto y no mueve nada")
    void repetirEsInocuo() {
        UUID[] a = armar(true);
        var primera = transaccion.execute(
                e -> decidirCU().decidir(a[3], true, null, new java.math.BigDecimal("700"), como(a[1])));
        var segunda = transaccion.execute(
                e -> decidirCU().decidir(a[3], true, null, new java.math.BigDecimal("700"), como(a[1])));

        assertThat(segunda.participanteId()).isEqualTo(primera.participanteId());
        assertThat(dslFixtura
                        .fetchOne(
                                "SELECT count(*) FROM grupos.participante WHERE grupo_id = ? AND usuario_id = ?",
                                a[0],
                                a[2])
                        .get(0))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("CU-68 · rechazar exige motivo, y rechazada no toma cupo")
    void rechazar() {
        UUID[] a = armar(true);
        assertThatThrownBy(() ->
                        transaccion.execute(e -> decidirCU().decidir(a[3], false, " ", BigDecimal.ZERO, como(a[1]))))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("por que");

        var r = transaccion.execute(
                e -> decidirCU().decidir(a[3], false, "no cumple el perfil", BigDecimal.ZERO, como(a[1])));
        assertThat(r.estado()).isEqualTo("RECHAZADA");
        assertThat(r.participanteId()).isNull();
        assertThat(dslFixtura
                        .fetchOne("SELECT cupos_ocupados FROM grupos.grupo WHERE id = ?", a[0])
                        .get(0))
                .isEqualTo((short) 2);
    }

    @Test
    @DisplayName("CU-68 · sin cupos libres no se acepta, y la solicitud sigue PENDIENTE")
    void sinCupos() {
        UUID[] a = armar(false);
        assertThatThrownBy(() -> transaccion.execute(
                        e -> decidirCU().decidir(a[3], true, null, new java.math.BigDecimal("700"), como(a[1]))))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("cupos libres");
        assertThat(dslFixtura
                        .fetchOne("SELECT estado FROM grupos.solicitud_ingreso WHERE id = ?", a[3])
                        .get(0))
                .isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("CU-68 · el organizador de OTRO grupo (o un participante) no decide: «esa solicitud no existe»")
    void ajenoNoDecide() {
        UUID[] a = armar(true);
        UUID[] otro = armar(true);
        assertThatThrownBy(() -> transaccion.execute(
                        e -> decidirCU().decidir(a[3], true, null, new java.math.BigDecimal("700"), como(otro[1]))))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no existe");
        assertThatThrownBy(() -> transaccion.execute(
                        e -> decidirCU().decidir(a[3], true, null, new java.math.BigDecimal("700"), como(a[2]))))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(dslFixtura
                        .fetchOne("SELECT estado FROM grupos.solicitud_ingreso WHERE id = ?", a[3])
                        .get(0))
                .isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("CU-68 · decidir lo contrario de lo ya resuelto es un error, no un reintento")
    void contrarioNoEsReintento() {
        UUID[] a = armar(true);
        transaccion.execute(e -> decidirCU().decidir(a[3], true, null, new java.math.BigDecimal("700"), como(a[1])));
        assertThatThrownBy(() -> transaccion.execute(
                        e -> decidirCU().decidir(a[3], false, "ahora no", BigDecimal.ZERO, como(a[1]))))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya esta resuelta");
    }

    @Test
    @DisplayName("CU-68 · la cola del organizador lista solo las PENDIENTES de SU grupo")
    void cola() {
        UUID[] a = armar(true);
        var lista = transaccion.execute(e -> decidirCU().pendientes(a[0], como(a[1])));
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).id()).isEqualTo(a[3]);
        UUID[] otro = armar(true);
        assertThatThrownBy(() -> transaccion.execute(e -> decidirCU().pendientes(a[0], como(otro[1]))))
                .isInstanceOf(ErrorDeNegocio.class);
    }
}

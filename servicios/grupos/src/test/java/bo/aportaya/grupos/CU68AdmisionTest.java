package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso.Entrada;
import bo.aportaya.grupos.infraestructura.AdmisionRepositorio;
import bo.aportaya.grupos.infraestructura.CreacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Decisiones persistidas en PostgreSQL real, sin dobles del repositorio. */
class CU68AdmisionTest extends BaseDeCU68 {
    private CU68AceptarIngreso admision;
    private UUID solicitud;
    private UUID grupo;
    private ContextoSesion administrador;
    private ContextoSesion backoffice;

    @BeforeEach
    void prepararExpediente() {
        admision = new CU68AceptarIngreso(
                new Datos(dsl), new AdmisionRepositorio(), new Outbox("grupos"), Reloj.delSistema());
        grupo = grupoConCupoLibre();
        dslFixtura.execute("UPDATE grupos.grupo SET estado='ABIERTO_A_INSCRIPCION' WHERE id=?", grupo);
        new CreacionRepositorio().configurar(dslFixtura, grupo, false);
        var participante = dslFixtura.fetchOne(
                "SELECT id, usuario_id FROM grupos.participante WHERE grupo_id=? ORDER BY id LIMIT 1", grupo);
        dslFixtura.execute(
                "UPDATE grupos.participante SET es_organizador=true WHERE id=?", participante.get("id", UUID.class));
        administrador = sesion(participante.get("usuario_id", UUID.class), "ORGANIZADOR");
        backoffice = sesion(fixtura.usuario(), "BACKOFFICE");
        criterioVigente();
        solicitud = postular(grupo, false, true, 0, 4).solicitudId();
    }

    private ContextoSesion sesion(UUID usuario, String rol) {
        return ContextoSesion.de(usuario, rol, new Traza(UUID.randomUUID().toString()));
    }

    private java.util.List<bo.aportaya.grupos.dominio.DecisionDeIngreso> historial() {
        return transaccion.execute(tx -> admision.historial(solicitud, backoffice));
    }

    private Entrada propuesta(String decision, int revision) {
        return new Entrada(solicitud, UUID.randomUUID(), decision, "Revisión humana documentada", revision, null);
    }

    @Test
    void backofficePuedeAceptarContraLaPropuestaYLaAlertaDelAlgoritmo() {
        var propuesta = transaccion.execute(tx -> admision.proponer(propuesta("RECHAZAR", 0), administrador));
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.participante WHERE grupo_id=? AND usuario_id=?",
                        grupo,
                        contexto().usuarioId()))
                .isZero();
        var entrada = new Entrada(
                solicitud,
                UUID.randomUUID(),
                "ACEPTAR",
                "Se revisó evidencia adicional y capacidad del grupo",
                1,
                propuesta.id());
        var decision = transaccion.execute(tx -> admision.resolver(entrada, backoffice));
        assertThat(decision.participanteId()).isNotNull();
        assertThat(decision.evidenciaAlgoritmo()).contains("REVISAR_CONCENTRACION");
        assertThat(dsl.fetchOne("SELECT estado FROM grupos.participante WHERE id=?", decision.participanteId())
                        .get(0))
                .isEqualTo("ACEPTADO_PENDIENTE_FIRMA");
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.cupo WHERE participante_id=? AND estado='RESERVADO'",
                        decision.participanteId()))
                .isEqualTo(1);
        var repetida = transaccion.execute(tx -> admision.resolver(entrada, backoffice));
        assertThat(repetida).isEqualTo(decision);
        assertThat(historial()).hasSize(2);
    }

    @Test
    void rechazoHumanoSeConservaSinCrearMiembro() {
        var propuesta = transaccion.execute(tx -> admision.proponer(propuesta("ACEPTAR", 0), administrador));
        var entrada = new Entrada(
                solicitud, UUID.randomUUID(), "RECHAZAR", "Capacidad insuficiente documentada", 1, propuesta.id());
        transaccion.execute(tx -> admision.resolver(entrada, backoffice));
        assertThat(dsl.fetchOne("SELECT estado FROM grupos.solicitud_ingreso WHERE id=?", solicitud)
                        .get(0))
                .isEqualTo("RECHAZADA");
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.participante WHERE grupo_id=? AND usuario_id=?",
                        grupo,
                        contexto().usuarioId()))
                .isZero();
        assertThat(historial()).hasSize(2);
    }

    @Test
    void rolRealDelServicioPermitePropuestaYResolucionSinSuperusuario() {
        var propuesta = transaccion.execute(tx -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return admision.proponer(propuesta("ACEPTAR", 0), administrador);
        });
        var entrada = new Entrada(
                solicitud, UUID.randomUUID(), "ACEPTAR", "Verificación con permisos del servicio", 1, propuesta.id());
        var decision = transaccion.execute(tx -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return admision.resolver(entrada, backoffice);
        });
        assertThat(decision.participanteId()).isNotNull();
    }

    @Test
    void administradorNoPuedeResolverYNadiePuedeResolverSuPropiaAdmision() {
        var propuesta = transaccion.execute(tx -> admision.proponer(propuesta("ACEPTAR", 0), administrador));
        var entrada = new Entrada(solicitud, UUID.randomUUID(), "ACEPTAR", "Revisión", 1, propuesta.id());
        assertThatThrownBy(() -> transaccion.execute(tx -> admision.resolver(entrada, administrador)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> transaccion.execute(
                        tx -> admision.resolver(entrada, sesion(contexto().usuarioId(), "BACKOFFICE"))))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> transaccion.execute(
                        tx -> admision.resolver(entrada, sesion(administrador.usuarioId(), "BACKOFFICE"))))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    void administradorAjenoNoProponeNiConsultaElExpediente() {
        var ajeno = sesion(fixtura.usuario(), "ORGANIZADOR");
        assertThatThrownBy(() -> transaccion.execute(tx -> admision.proponer(propuesta("ACEPTAR", 0), ajeno)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> transaccion.execute(tx -> admision.historial(solicitud, ajeno)))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    void propuestaObsoletaNoPuedeSerResuelta() {
        var vieja = transaccion.execute(tx -> admision.proponer(propuesta("ACEPTAR", 0), administrador));
        transaccion.execute(tx -> admision.proponer(propuesta("RECHAZAR", 1), administrador));
        var entrada = new Entrada(solicitud, UUID.randomUUID(), "ACEPTAR", "Revisión", 1, vieja.id());
        assertThatThrownBy(() -> transaccion.execute(tx -> admision.resolver(entrada, backoffice)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(historial()).hasSize(2);
    }

    @Test
    void cambiarContenidoDeLaMismaClaveNoCambiaElExpediente() {
        var entrada = propuesta("ACEPTAR", 0);
        transaccion.execute(tx -> admision.proponer(entrada, administrador));
        var cambiada = new Entrada(solicitud, entrada.clave(), "RECHAZAR", entrada.motivo(), 0, null);
        assertThatThrownBy(() -> transaccion.execute(tx -> admision.proponer(cambiada, administrador)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(historial()).hasSize(1);
    }

    @Test
    @DisplayName("rechaza por R-GRP-17: las decisiones de admisión no se modifican ni se borran")
    void historialNoSePuedeModificarOBorrar() {
        var propuesta = transaccion.execute(tx -> admision.proponer(propuesta("ACEPTAR", 0), administrador));
        assertThat(rechazaLaBase("DELETE FROM grupos.decision_ingreso WHERE id=?", propuesta.id()))
                .contains("R-AUD-01");
        assertThat(rechazaLaBase("UPDATE grupos.decision_ingreso SET motivo='alterado' WHERE id=?", propuesta.id()))
                .contains("R-AUD-01");
    }

    @Test
    void sinCupoNoDejaResolucionNiMiembroParcial() {
        var propuesta = transaccion.execute(tx -> admision.proponer(propuesta("ACEPTAR", 0), administrador));
        dslFixtura.execute("UPDATE grupos.cupo SET estado='RESERVADO' WHERE grupo_id=? AND estado='LIBRE'", grupo);
        var entrada = new Entrada(solicitud, UUID.randomUUID(), "ACEPTAR", "Revisión", 1, propuesta.id());
        assertThatThrownBy(() -> transaccion.execute(tx -> admision.resolver(entrada, backoffice)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(historial()).hasSize(1);
        assertThat(dsl.fetchOne("SELECT estado FROM grupos.solicitud_ingreso WHERE id=?", solicitud)
                        .get(0))
                .isEqualTo("PENDIENTE");
    }
}

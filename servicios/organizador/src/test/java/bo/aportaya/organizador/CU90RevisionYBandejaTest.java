package bo.aportaya.organizador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.organizador.aplicacion.CU90PostularOrganizador.EntradaAprobacion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-90 · revisión posterior, historial inmutable, bandeja paginada y expediente propio.
 *
 * <p>Amenazas: decisión que se reescribe o desaparece, bandeja inestable o filtrable por cualquiera,
 * resolución que quede fuera del historial y permisos reales insuficientes.
 */
class CU90RevisionYBandejaTest extends BaseDeResolucion {

    @Test
    @DisplayName(
            "Dada una solicitud aprobada con su organizador · Cuando se la revisa y se revoca la habilitación · Entonces se agrega una decisión nueva de revisión y el organizador queda SUSPENDIDO · Y la primera decisión se conserva sin cambios y se emite el evento de revocación")
    void revisionRevocaSinReescribir() {
        UUID solicitud = postular(fixtura.usuario());
        var aprobada = resolver(entrada(solicitud, UUID.randomUUID(), "APROBAR", "Cumple", 0), backoffice);
        UUID organizador = aprobada.decision().organizadorId();

        var revision = transaccion.execute(t -> resolucionCU.revisar(
                entrada(solicitud, UUID.randomUUID(), "REVOCAR", "Hallazgo posterior de cumplimiento", 1),
                comoBackoffice(backoffice)));

        assertThat(revision.decision().fase()).isEqualTo("REVISION");
        assertThat(revision.decision().revision()).isEqualTo(2);
        assertThat(dsl.fetchOne("SELECT estado FROM organizador.organizador WHERE id=?", organizador)
                        .get(0))
                .isEqualTo("SUSPENDIDO");
        assertThat(estadoSolicitud(solicitud)).isEqualTo("APROBADA");
        var historial = dsl.fetch(
                "SELECT fase, decision, motivo FROM organizador.decision_habilitacion WHERE solicitud_id=? ORDER BY revision",
                solicitud);
        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).get("decision")).isEqualTo("APROBAR");
        assertThat(historial.get(0).get("motivo")).isEqualTo("Cumple");
        assertThat(eventos("organizador.habilitacion_revocada", solicitud)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una solicitud aprobada y otra que sigue pendiente · Cuando se confirma la habilitación de la aprobada y se intenta revisar la pendiente · Entonces el organizador de la aprobada queda como estaba · Y revisar la solicitud que no está aprobada se rechaza")
    void revisionConfirmaYSoloAprobadas() {
        UUID pendiente = postular(fixtura.usuario());
        assertThatThrownBy(() -> transaccion.execute(t -> resolucionCU.revisar(
                        entrada(pendiente, UUID.randomUUID(), "CONFIRMAR", "x", 0), comoBackoffice(backoffice))))
                .isInstanceOf(ErrorDeNegocio.class);

        UUID solicitud = postular(fixtura.usuario());
        var aprobada = resolver(entrada(solicitud, UUID.randomUUID(), "APROBAR", "Cumple", 0), backoffice);
        transaccion.execute(t -> resolucionCU.revisar(
                entrada(solicitud, UUID.randomUUID(), "CONFIRMAR", "Revision anual sin novedades", 1),
                comoBackoffice(backoffice)));
        assertThat(dsl.fetchOne(
                                "SELECT estado FROM organizador.organizador WHERE id=?",
                                aprobada.decision().organizadorId())
                        .get(0))
                .isEqualTo("CAPACITACION_PENDIENTE");
    }

    @Test
    @DisplayName("rechaza por R-ORG-08: el historial no se reescribe, no se borra ni admite una segunda resolución")
    void historialInmutable() {
        UUID solicitud = postular(fixtura.usuario());
        var decision = resolver(entrada(solicitud, UUID.randomUUID(), "RECHAZAR", "No cumple el perfil", 0), backoffice)
                .decision();

        assertThat(rechazaLaBase(
                        "UPDATE organizador.decision_habilitacion SET motivo='alterado' WHERE id=?", decision.id()))
                .isNotBlank();
        assertThat(rechazaLaBase("DELETE FROM organizador.decision_habilitacion WHERE id=?", decision.id()))
                .isNotBlank();
        assertThat(rechazaLaBase(
                        "INSERT INTO organizador.decision_habilitacion (id,solicitud_id,clave_idempotencia,fase,decision,actor_id,motivo,revision,evidencia_requisitos,correlacion_id) VALUES (gen_random_uuid(),?,gen_random_uuid(),'RESOLUCION','APROBAR',?,'segunda resolucion',9,'x',gen_random_uuid())",
                        solicitud,
                        backoffice))
                .isNotBlank();
    }

    @Test
    @DisplayName(
            "Dadas varias solicitudes pendientes y una ya resuelta · Cuando backoffice recorre la bandeja por cursor filtrando por PENDIENTE · Entonces la bandeja lista de la más antigua a la más nueva, sin repetir ni saltear y sin la resuelta · Y un estado inventado o un cursor incompleto se rechazan")
    void bandejaPaginadaYEstable() {
        List<UUID> creadas = new ArrayList<>();
        for (int i = 0; i < 5; i++) creadas.add(postular(fixtura.usuario()));
        resolver(entrada(creadas.get(1), UUID.randomUUID(), "RECHAZAR", "No cumple el perfil", 0), backoffice);

        var vistas = new ArrayList<UUID>();
        java.time.OffsetDateTime fecha = null;
        UUID id = null;
        int paginas = 0;
        List<bo.aportaya.organizador.dominio.ExpedienteDeHabilitacion> pagina;
        do {
            final java.time.OffsetDateTime f = fecha;
            final UUID i = id;
            pagina = transaccion.execute(
                    t -> bandejaCU.bandeja(List.of("PENDIENTE"), f, i, 2, comoBackoffice(backoffice)));
            pagina.forEach(e -> vistas.add(e.solicitudId()));
            if (!pagina.isEmpty()) {
                fecha = pagina.get(pagina.size() - 1).fechaSolicitud();
                id = pagina.get(pagina.size() - 1).solicitudId();
            }
            paginas++;
        } while (pagina.size() == 2 && paginas < 10);

        var esperadas = new ArrayList<>(creadas);
        esperadas.remove(creadas.get(1));
        assertThat(vistas).containsExactlyElementsOf(esperadas);
        assertThat(vistas).doesNotHaveDuplicates();
        assertThatThrownBy(() -> transaccion.execute(
                        t -> bandejaCU.bandeja(List.of("INVENTADO"), null, null, 2, comoBackoffice(backoffice))))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> transaccion.execute(t -> bandejaCU.bandeja(
                        List.of("PENDIENTE"), null, UUID.randomUUID(), 2, comoBackoffice(backoffice))))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName(
            "Dada una solicitud rechazada con su motivo · Cuando el postulante abre su propio expediente · Entonces ve la decisión con su motivo · Y otra persona sin postulación no ve el expediente del primero")
    void expedientePropio() {
        UUID solicitante = fixtura.usuario();
        UUID solicitud = postular(solicitante);
        UUID otro = fixtura.usuario();
        resolver(entrada(solicitud, UUID.randomUUID(), "RECHAZAR", "Falta antiguedad", 0), backoffice);

        var propio = transaccion.execute(t -> bandejaCU.expedienteDelUsuario(contextoDe(solicitante)));
        assertThat(propio.solicitud().solicitudId()).isEqualTo(solicitud);
        assertThat(propio.decisiones()).extracting("motivo").containsExactly("Falta antiguedad");
        assertThatThrownBy(() -> transaccion.execute(t -> bandejaCU.expedienteDelUsuario(contextoDe(otro))))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName(
            "Dada una solicitud pendiente · Cuando se la aprueba por la ruta heredada de aprobación · Entonces la decisión queda en el historial con su evidencia marcada como heredada · Y ninguna resolución queda afuera: resolverla de nuevo por la ruta nueva se rechaza")
    void rutaHeredadaTambienDejaHistorial() {
        UUID solicitud = postular(fixtura.usuario());
        transaccion.execute(
                t -> postulacionCU.aprobar(new EntradaAprobacion(solicitud, medidos), comoBackoffice(backoffice)));

        assertThat(decisiones(solicitud)).isEqualTo(1);
        assertThat(dsl.fetchOne(
                                "SELECT evidencia_requisitos FROM organizador.decision_habilitacion WHERE solicitud_id=?",
                                solicitud)
                        .get(0))
                .asString()
                .contains("heredada");
        assertThatThrownBy(() -> resolver(entrada(solicitud, UUID.randomUUID(), "RECHAZAR", "Tarde", 1), backoffice))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName(
            "Dada una solicitud pendiente y el servicio con su rol real, sin superusuario · Cuando backoffice la resuelve · Entonces la resolución se registra y se lee en el historial")
    void rolRealDelServicio() {
        UUID solicitud = postular(fixtura.usuario());
        var resultado = transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_organizador");
            return resolucionCU.resolver(
                    entrada(solicitud, UUID.randomUUID(), "RECHAZAR", "No cumple el perfil", 0),
                    comoBackoffice(backoffice));
        });
        assertThat(resultado.decision().decision()).isEqualTo("RECHAZAR");
        assertThat(decisiones(solicitud)).isEqualTo(1);
    }
}

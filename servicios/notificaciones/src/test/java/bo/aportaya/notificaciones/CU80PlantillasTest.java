package bo.aportaya.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.notificaciones.aplicacion.ConsultarMensajeria;
import bo.aportaya.notificaciones.infraestructura.AuditoriaDeMensajeriaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.lang.reflect.RecordComponent;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-80 · las plantillas de mensaje que mira Auditoria (backoffice carril A · A6), contra la base real.
 * Correcto (aprobada y borrador con su version), limite (recorrido con cursor) e invalido.
 */
class CU80PlantillasTest extends BaseDeNotificaciones {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static ConsultarMensajeria consultas() {
        return new ConsultarMensajeria(
                new Datos(dsl),
                new AuditoriaDeMensajeriaRepositorio(),
                new bo.aportaya.notificaciones.infraestructura.PlataformaDeMensajeriaRepositorio());
    }

    private static ContextoSesion auditoria(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    /** Una plantilla aprobada con su version; devuelve el id de la version. */
    private UUID plantilla() {
        String corto = UUID.randomUUID().toString().substring(0, 8);
        UUID evento = fixtura.evento("aporte.recordatorio." + corto, "COBRANZA", true, "ALTA");
        return fixtura.plantilla("PL-" + corto, evento, "WHATSAPP", "Texto sintetico de prueba");
    }

    @Test
    @DisplayName(
            "Dada una plantilla aprobada y otra en borrador, cada una con su versión · Cuando Auditoría abre las plantillas · Entonces ve las dos con su estado de aprobación y su versión, sin cuerpo ni asunto")
    void plantillasConEstado() {
        UUID aprobada = plantilla();
        UUID borrador = plantilla();
        dslFixtura.execute(
                """
                UPDATE notificaciones.plantilla_mensaje
                   SET estado_aprobacion = 'BORRADOR', activa = false
                 WHERE id = (SELECT plantilla_id FROM notificaciones.version_plantilla WHERE id = ?)
                """,
                borrador);
        ContextoSesion ctx = auditoria(fixtura.usuario());

        var pagina = transaccion.execute(
                t -> consultas().plantillas(List.of(), Optional.empty(), Optional.empty(), 25, ctx));

        assertThat(pagina.items())
                .extracting(ConsultarMensajeria.Plantilla::versionId)
                .contains(aprobada, borrador);
        assertThat(pagina.items())
                .filteredOn(p -> p.versionId().equals(borrador))
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.estadoAprobacion()).isEqualTo("BORRADOR");
                    assertThat(p.activa()).isFalse();
                    assertThat(p.canal()).isEqualTo("WHATSAPP");
                    assertThat(p.version()).isEqualTo(1);
                    assertThat(p.evento()).startsWith("aporte.recordatorio.");
                });

        var soloAprobadas = transaccion.execute(
                t -> consultas().plantillas(List.of("APROBADA"), Optional.empty(), Optional.empty(), 25, ctx));
        assertThat(soloAprobadas.items())
                .extracting(ConsultarMensajeria.Plantilla::versionId)
                .contains(aprobada)
                .doesNotContain(borrador);

        assertThat(Arrays.stream(ConsultarMensajeria.Plantilla.class.getRecordComponents())
                        .map(RecordComponent::getName))
                .doesNotContain("cuerpo", "asunto", "botones", "urlEncabezadoMedia");
    }

    @Test
    @DisplayName(
            "Dadas dos versiones de plantilla · Cuando Auditoría recorre las plantillas de a una · Entonces ve cada versión una sola vez, de la más reciente a la más antigua, y la última página cierra sin cursor")
    void recorridoConCursor() {
        UUID primera = plantilla();
        UUID segunda = plantilla();
        ContextoSesion ctx = auditoria(fixtura.usuario());

        List<UUID> vistos = new ArrayList<>();
        Optional<OffsetDateTime> antesDe = Optional.empty();
        Optional<UUID> antesDeId = Optional.empty();
        ConsultarMensajeria.Pagina<ConsultarMensajeria.Plantilla> pagina;
        int vueltas = 0;
        do {
            Optional<OffsetDateTime> d = antesDe;
            Optional<UUID> i = antesDeId;
            pagina = transaccion.execute(t -> consultas().plantillas(List.of(), d, i, 1, ctx));
            pagina.items().forEach(x -> vistos.add(x.versionId()));
            antesDe = pagina.proximoAntesDe();
            antesDeId = pagina.proximoAntesDeId();
            vueltas++;
        } while (antesDeId.isPresent() && vueltas < 10);

        assertThat(vistos).containsExactly(segunda, primera);
        assertThat(pagina.items()).isEmpty();
        assertThat(pagina.proximoAntesDe()).isEmpty();
    }

    @Test
    @DisplayName("rechaza un estado de aprobación desconocido o un cursor a medias con AP-CU80-05")
    void rechazaFiltroInvalido() {
        ContextoSesion ctx = auditoria(fixtura.usuario());

        assertThatThrownBy(
                        () -> consultas().plantillas(List.of("VIGENTE"), Optional.empty(), Optional.empty(), 25, ctx))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU80-05"));
        assertThatThrownBy(() ->
                        consultas().plantillas(List.of(), Optional.of(OffsetDateTime.now()), Optional.empty(), 25, ctx))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU80-05"));
    }
}

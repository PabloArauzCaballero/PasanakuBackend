package bo.aportaya.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.auditoria.aplicacion.CU58EjecutarReporte;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H13.S1.M2 · IDOR de CU-58: la exportacion de un reporte es de quien lo pidio.
 *
 * <p>{@code AUDITORIA_LEER} lo tiene todo el equipo de auditoria; que dos auditores tengan
 * el permiso no hace que uno pueda bajar lo que el otro pidio con SU justificacion. Y la
 * negativa no revela: para quien no es el solicitante, la exportacion ajena es
 * indistinguible de una que no existe.
 */
class AutorizacionNegativaTest extends BaseDeAuditoria {

    private static final String PERMISO = "REPORTE_HISTORICO_PAGOS";

    private UUID exportacionPedidaPor(ContextoSesion solicitante) {
        UUID definicion = reportes.definicionDeReporte(
                "ambito " + UUID.randomUUID(), PERMISO, false, "SELECT 1 AS columna", "[]");
        var salida = transaccion.execute(estado -> reportesCU.ejecutar(
                new CU58EjecutarReporte.EntradaReporte(
                        definicion, Map.of(), Optional.of("CSV"), Optional.empty(), Set.of(PERMISO)),
                solicitante));
        return reportes.exportacionDe(salida.ejecucionId()).get("id", UUID.class);
    }

    private static ContextoSesion otroAuditor() {
        return ContextoSesion.de(
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                "AUDITOR",
                new Traza(UUID.randomUUID().toString()));
    }

    private String descargar(UUID exportacion, ContextoSesion quien) {
        return ((ErrorDeNegocio) org.assertj.core.api.Assertions.catchThrowable(() -> transaccion.execute(estado -> {
                    descargasCU.ejecutar(exportacion, quien);
                    return null;
                })))
                .getMessage();
    }

    @Test
    @DisplayName("rechaza · otro auditor con el mismo permiso no baja la exportacion ajena")
    void rechazaDescargaDeOtroAuditor() {
        UUID exportacion = exportacionPedidaPor(contexto());
        int antes = reportes.exportacionDe(reportesEjecucionDe(exportacion)).get("descargas", Integer.class);

        assertThatThrownBy(() -> transaccion.execute(estado -> {
                    descargasCU.ejecutar(exportacion, otroAuditor());
                    return null;
                }))
                .isInstanceOf(ErrorDeNegocio.class)
                .extracting(e -> ((ErrorDeNegocio) e).codigo().toString())
                .isEqualTo("AP-CU58-05");

        // Ni siquiera se conto: un intento ajeno no consume el tope del dueno.
        assertThat(reportes.exportacionDe(reportesEjecucionDe(exportacion)).get("descargas", Integer.class))
                .isEqualTo(antes);
    }

    @Test
    @DisplayName("rechaza · la negativa por ajena es identica a la de una exportacion que no existe")
    void rechazaSinRevelarQueExiste() {
        UUID ajena = exportacionPedidaPor(contexto());

        String porAjena = descargar(ajena, otroAuditor());
        String porInexistente = descargar(UUID.randomUUID(), otroAuditor());

        assertThat(porAjena).isEqualTo(porInexistente);
    }

    @Test
    @DisplayName("el solicitante sigue bajando lo suyo y se cuenta una descarga")
    void elSolicitanteBajaLoSuyo() {
        UUID exportacion = exportacionPedidaPor(contexto());
        transaccion.execute(estado -> {
            descargasCU.ejecutar(exportacion, contexto());
            return null;
        });

        assertThat(reportes.exportacionDe(reportesEjecucionDe(exportacion)).get("descargas", Integer.class))
                .isEqualTo(1);
    }

    private UUID reportesEjecucionDe(UUID exportacion) {
        return dslFixtura
                .fetchOne("SELECT ejecucion_id FROM auditoria.exportacion_reporte WHERE id = ?", exportacion)
                .get(0, UUID.class);
    }
}

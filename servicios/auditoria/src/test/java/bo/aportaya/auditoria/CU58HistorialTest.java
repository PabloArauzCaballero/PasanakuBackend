package bo.aportaya.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.auditoria.aplicacion.CU58EjecutarReporte;
import bo.aportaya.auditoria.aplicacion.ConsultarHistorialDeReportes;
import bo.aportaya.auditoria.infraestructura.HistorialDeReportesRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-58 · catalogo de reportes e historial de ejecuciones que lee el backoffice (carril A · A6), contra la
 * base real y con el rol BACKOFFICE de la sesion de un operador. Las ejecuciones salen del caso de uso real.
 */
class CU58HistorialTest extends BaseDeAuditoria {

    private static final String PERMISO = "REPORTE_HISTORICO_PAGOS";

    private static ConsultarHistorialDeReportes historial() {
        return new ConsultarHistorialDeReportes(new Datos(dsl), new HistorialDeReportesRepositorio());
    }

    private static ContextoSesion operador() {
        return ContextoSesion.de(
                OPERADOR, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private static UUID ejecucionCompletada(UUID definicion) {
        return transaccion
                .execute(t -> reportesCU.ejecutar(
                        new CU58EjecutarReporte.EntradaReporte(
                                definicion, Map.of(), Optional.empty(), Optional.empty(), Set.of(PERMISO)),
                        contexto()))
                .ejecucionId();
    }

    /** Todas las ejecuciones con los estados pedidos, recorriendo el cursor de a {@code limite}. */
    private static List<ConsultarHistorialDeReportes.Ejecucion> todas(List<String> estados, int limite) {
        var filas = new ArrayList<ConsultarHistorialDeReportes.Ejecucion>();
        Optional<OffsetDateTime> antesDe = Optional.empty();
        Optional<UUID> antesDeId = Optional.empty();
        do {
            var a = antesDe;
            var b = antesDeId;
            var p = transaccion.execute(t -> historial().ejecuciones(estados, a, b, limite, operador()));
            filas.addAll(p.items());
            antesDe = p.proximo();
            antesDeId = p.proximoId();
        } while (antesDeId.isPresent());
        return filas;
    }

    @Test
    @DisplayName(
            "Dada una definición activa y otra inactiva · Cuando el backoffice lista el catálogo de reportes · Entonces ve la activa con su permiso requerido y sin la consulta base")
    void catalogo() {
        String nombre = "catalogo " + UUID.randomUUID();
        UUID activa = reportes.definicionDeReporte(nombre, PERMISO, true, "SELECT 1", "[]");
        UUID inactiva = reportes.definicionDeReporte("inactiva " + UUID.randomUUID(), PERMISO, false, "SELECT 1", "[]");
        dslFixtura.execute("UPDATE auditoria.definicion_reporte SET activa = false WHERE id = ?", inactiva);

        var filas = new ArrayList<ConsultarHistorialDeReportes.Definicion>();
        Optional<String> despues = Optional.empty();
        Optional<UUID> despuesId = Optional.empty();
        do {
            var a = despues;
            var b = despuesId;
            var p = transaccion.execute(t -> historial().definiciones(a, b, 2, operador()));
            filas.addAll(p.items());
            despues = p.proximo();
            despuesId = p.proximoId();
        } while (despuesId.isPresent());

        assertThat(filas)
                .extracting(ConsultarHistorialDeReportes.Definicion::definicionId)
                .contains(activa)
                .doesNotContain(inactiva)
                .doesNotHaveDuplicates();
        assertThat(filas)
                .filteredOn(d -> d.definicionId().equals(activa))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.permisoRequerido()).isEqualTo(PERMISO);
                    assertThat(d.sensible()).isTrue();
                });
        // Respuesta minima: el record no tiene la consulta base.
        assertThat(ConsultarHistorialDeReportes.Definicion.class.getRecordComponents())
                .extracting(c -> c.getName())
                .doesNotContain("consultaBase", "consulta");
    }

    @Test
    @DisplayName(
            "Dadas dos ejecuciones completadas y una fallida por permiso · Cuando el backoffice lista el historial filtrando por COMPLETADA · Entonces ve las dos completadas, de la más reciente a la más antigua, con su hash")
    void historialFiltrado() {
        UUID definicion =
                reportes.definicionDeReporte("historial " + UUID.randomUUID(), PERMISO, false, "SELECT 1", "[]");
        UUID primera = ejecucionCompletada(definicion);
        UUID segunda = ejecucionCompletada(definicion);
        assertThatThrownBy(() -> transaccion.execute(t -> reportesCU.ejecutar(
                new CU58EjecutarReporte.EntradaReporte(
                        definicion, Map.of(), Optional.empty(), Optional.empty(), Set.of("OTRA_COSA")),
                contexto())));

        var completadas = todas(List.of("COMPLETADA"), 2).stream()
                .filter(e -> e.definicionId().equals(definicion))
                .toList();

        assertThat(completadas)
                .extracting(ConsultarHistorialDeReportes.Ejecucion::ejecucionId)
                .containsExactlyInAnyOrder(primera, segunda);
        assertThat(completadas).allSatisfy(e -> assertThat(e.hash()).isPresent());
        assertThat(completadas.get(0).iniciadaEn())
                .isAfterOrEqualTo(completadas.get(1).iniciadaEn());
        assertThat(todas(List.of("FALLIDA"), 50))
                .filteredOn(e -> e.definicionId().equals(definicion))
                .hasSize(1);
    }

    @Test
    @DisplayName("rechaza un estado desconocido o un cursor a medias con AP-CU58-07")
    void rechazaFiltroInvalido() {
        assertThatThrownBy(() -> transaccion.execute(t -> historial()
                        .ejecuciones(List.of("PERDIDA"), Optional.empty(), Optional.empty(), 10, operador())))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU58-07"));
        assertThatThrownBy(() -> transaccion.execute(
                        t -> historial().definiciones(Optional.of("x"), Optional.empty(), 10, operador())))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU58-07"));
    }
}

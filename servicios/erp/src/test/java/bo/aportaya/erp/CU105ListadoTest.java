package bo.aportaya.erp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp;
import bo.aportaya.erp.aplicacion.ConsultarListadosDeErp.Filtro;
import bo.aportaya.erp.aplicacion.FilasDeErp;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · A6 · el inventario de activos fijos, contra PostgreSQL real.
 *
 * <p>Ejercicios del rango 2570 con reloj fijo a mediados de junio de ese anio, y activos adquiridos ese mismo
 * anio: el cursor inicial en el anio siguiente deja la primera pagina para la prueba. Los activos y su cuota se
 * insertan como datos sinteticos de la fixture: lo que se prueba es la lectura, no el calculo de CU-105.
 */
class CU105ListadoTest extends BaseDeErp {

    private static final AtomicInteger ANIO = new AtomicInteger(2570);
    private static final UUID MAXIMO = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private int anio;
    private UUID ejercicioId;
    private UUID categoria;
    private ContextoSesion contabilidad;
    private ConsultarListadosDeErp enJunio;

    @BeforeEach
    void escenario() {
        anio = ANIO.incrementAndGet();
        contabilidad = contextoDe(fixtura.usuario());
        ejercicioId = transaccion
                .execute(t -> periodoCU.abrirEjercicio(anio, contabilidad))
                .ejercicioId();
        categoria = fixtura.categoriaDeActivo(
                "CAT-L-" + anio,
                36,
                fixtura.cuenta("1201-L" + anio, "ACTIVO", "DEUDORA"),
                fixtura.cuenta("1202-L" + anio, "ACTIVO", "ACREEDORA"),
                fixtura.cuenta("5201-L" + anio, "EGRESO", "DEUDORA"));
        enJunio = listados(Reloj.fijo(Instant.parse(anio + "-06-15T15:00:00Z")));
    }

    private UUID activo(String estado, LocalDate adquirido) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO erp.activo_fijo
                    (id, categoria_activo_fijo_id, codigo_inventario, descripcion, fecha_adquisicion,
                     costo_adquisicion, moneda, valor_residual, depreciacion_acumulada, estado)
                VALUES (?, ?, ?, 'Equipo de computo', ?, 3600.00, 'BOB', 0.00, 100.00, ?)
                """,
                id,
                categoria,
                "INV-L-" + id.toString().substring(0, 12),
                adquirido,
                estado);
        return id;
    }

    private void cuotaEnJunio(UUID activoId) {
        dslFixtura.execute(
                """
                INSERT INTO erp.depreciacion_activo (id, activo_fijo_id, periodo_contable_id, monto, moneda, calculada_en)
                SELECT gen_random_uuid(), ?, p.id, 100.00, 'BOB', now()
                  FROM erp.periodo_contable p
                 WHERE p.ejercicio_fiscal_id = ? AND p.mes = 6
                """,
                activoId,
                ejercicioId);
    }

    private Filtro<LocalDate> desdeMiAnio(List<String> estados, int limite) {
        return new Filtro<>(estados, Optional.of(LocalDate.of(anio + 1, 1, 1)), Optional.of(MAXIMO), limite);
    }

    @Test
    @DisplayName("Dados dos activos adquiridos el mismo dia y uno ya depreciado en el periodo vigente · Cuando"
            + " Contabilidad recorre el inventario de a uno · Entonces no se repite ninguno y solo el depreciado dice"
            + " que ya tiene cuota en el periodo vigente, con su valor en libros")
    void inventarioPaginado() {
        LocalDate dia = LocalDate.of(anio, 2, 10);
        UUID depreciado = activo("ACTIVO", dia);
        UUID sinCuota = activo("ACTIVO", dia);
        cuotaEnJunio(depreciado);

        var primera = transaccion.execute(t -> enJunio.activos(desdeMiAnio(List.of(), 1), contabilidad));
        var segunda = transaccion.execute(t -> enJunio.activos(
                new Filtro<>(List.of(), primera.proximoAntesDe(), primera.proximoAntesDeId(), 1), contabilidad));

        var vistos = List.of(primera.items().get(0), segunda.items().get(0));
        assertThat(vistos).extracting(FilasDeErp.Activo::activoFijoId).containsExactlyInAnyOrder(depreciado, sinCuota);
        assertThat(vistos)
                .filteredOn(FilasDeErp.Activo::depreciadoEnElPeriodoVigente)
                .extracting(FilasDeErp.Activo::activoFijoId)
                .containsExactly(depreciado);
        var fila = vistos.get(0);
        assertThat(fila.valorEnLibros().toString()).isEqualTo("3500.00");
        assertThat(fila.costo().toString()).isEqualTo("3600.00");
        assertThat(fila.vidaUtilMeses()).isEqualTo(36);
        assertThat(fila.categoriaNombre()).isEqualTo("Categoria CAT-L-" + anio);
    }

    @Test
    @DisplayName("Dado un activo depreciado en junio y otro dado de baja · Cuando Contabilidad consulta el inventario"
            + " en julio · Entonces el depreciado ya no figura con cuota en el periodo vigente y el filtro por"
            + " DADO_DE_BAJA trae solo el dado de baja")
    void otroMesYFiltro() {
        UUID depreciado = activo("ACTIVO", LocalDate.of(anio, 3, 1));
        UUID baja = activo("DADO_DE_BAJA", LocalDate.of(anio, 3, 2));
        cuotaEnJunio(depreciado);
        var enJulio = listados(Reloj.fijo(Instant.parse(anio + "-07-15T15:00:00Z")));

        var activos = transaccion.execute(t -> enJulio.activos(desdeMiAnio(List.of("ACTIVO"), 1), contabilidad));
        var bajas = transaccion.execute(t -> enJulio.activos(desdeMiAnio(List.of("DADO_DE_BAJA"), 1), contabilidad));

        assertThat(activos.items().get(0).activoFijoId()).isEqualTo(depreciado);
        assertThat(activos.items().get(0).depreciadoEnElPeriodoVigente()).isFalse();
        assertThat(bajas.items().get(0).activoFijoId()).isEqualTo(baja);
    }

    @Test
    @DisplayName("rechaza el listado de activos con cursor a medias o estado desconocido: AP-CU105-04")
    void rechazaFiltroInvalido() {
        assertThatThrownBy(() -> transaccion.execute(t -> listadosCU.activos(
                        new Filtro<>(List.of(), Optional.of(LocalDate.of(anio, 1, 1)), Optional.empty(), 5),
                        contabilidad)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU105-04"));
        assertThatThrownBy(() -> transaccion.execute(t -> listadosCU.activos(
                        new Filtro<>(List.of("AGOTADO"), Optional.empty(), Optional.empty(), 5), contabilidad)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU105-04"));
    }
}

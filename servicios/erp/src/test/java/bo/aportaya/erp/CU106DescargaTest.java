package bo.aportaya.erp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.erp.aplicacion.CU100AbrirCerrarPeriodo.EntradaCierre;
import bo.aportaya.erp.aplicacion.CU106DescargarEstadoFinanciero;
import bo.aportaya.erp.dominio.DocumentoDeEstadoFinanciero;
import bo.aportaya.erp.infraestructura.EstadosFinancierosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · H7.S3 · la descarga del estado financiero sellado, contra PostgreSQL real.
 *
 * <p>Lo que importa: lo que se entrega hashea EXACTAMENTE al hash sellado al generarlo (aunque
 * {@code datos} sea JSONB y Postgres lo normalice), y un documento cuyo contenido ya no da ese hash
 * no se entrega. Anios del rango 2850, que ninguna otra prueba de erp reparte.
 */
class CU106DescargaTest extends BaseDeErp {

    private static final AtomicInteger ANIO = new AtomicInteger(2850);

    private ContextoSesion ctx;
    private UUID ejercicioId;
    private CU106DescargarEstadoFinanciero descargaCU;

    @BeforeEach
    void escenario() {
        int anio = ANIO.incrementAndGet();
        ctx = contextoDe(fixtura.usuario());
        ejercicioId =
                transaccion.execute(t -> periodoCU.abrirEjercicio(anio, ctx)).ejercicioId();
        descargaCU = new CU106DescargarEstadoFinanciero(new Datos(dsl), new EstadosFinancierosRepositorio());
    }

    private UUID periodo(int mes) {
        return dsl.fetchOne(
                        "SELECT id FROM erp.periodo_contable WHERE ejercicio_fiscal_id = ? AND mes = ?",
                        ejercicioId,
                        (short) mes)
                .get(0, UUID.class);
    }

    @Test
    @DisplayName("Dado un balance general generado sobre un mes cerrado · Cuando Contabilidad descarga su documento ·"
            + " Entonces el contenido entregado hashea al hash sellado al generarlo")
    void descargaSellada() {
        UUID enero = periodo(1);
        UUID activo = fixtura.cuenta("1301-D" + enero.toString().substring(0, 4), "ACTIVO", "DEUDORA");
        UUID pasivo = fixtura.cuenta("2301-D" + enero.toString().substring(0, 4), "PASIVO", "ACREEDORA");
        fixtura.asiento(enero, activo, pasivo, "6000.00", ctx.usuarioId());
        transaccion.execute(t -> periodoCU.cerrarPeriodo(new EntradaCierre(enero, "Cierre"), ctx));
        var generado = transaccion.execute(t -> estadoCU.generar(enero, "BALANCE_GENERAL", ctx));

        var doc = transaccion.execute(t -> descargaCU.descargar(generado.estadoId(), ctx));

        assertThat(doc.hashContenido()).isEqualTo(generado.hashContenido());
        String texto = new String(doc.contenido(), StandardCharsets.UTF_8);
        assertThat(DocumentoDeEstadoFinanciero.sha256(texto)).isEqualTo(generado.hashContenido());
        assertThat(texto).startsWith("{\"tipo\":\"BALANCE_GENERAL\",\"renglones\":[");
    }

    @Test
    @DisplayName("rechaza entregar un documento cuyo contenido ya no da su hash sellado: AP-CU106-04")
    void integridadRota() {
        UUID febrero = periodo(2);
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO erp.estado_financiero_generado
                    (id, periodo_contable_id, tipo, generado_en, generado_por, hash_contenido, datos)
                VALUES (?, ?, 'BALANCE_GENERAL', now(), ?, ?, ?::jsonb)
                """,
                id,
                febrero,
                ctx.usuarioId(),
                DocumentoDeEstadoFinanciero.sha256("otro contenido"),
                "{\"tipo\":\"BALANCE_GENERAL\",\"renglones\":[{\"concepto\":\"Caja\",\"monto\":\"10.00\"}]}");

        assertThatThrownBy(() -> transaccion.execute(t -> descargaCU.descargar(id, ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU106-04"));
    }

    @Test
    @DisplayName("rechaza descargar un estado financiero inexistente: AP-CU106-03")
    void inexistente() {
        assertThatThrownBy(() -> transaccion.execute(t -> descargaCU.descargar(UUID.randomUUID(), ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU106-03"));
    }
}

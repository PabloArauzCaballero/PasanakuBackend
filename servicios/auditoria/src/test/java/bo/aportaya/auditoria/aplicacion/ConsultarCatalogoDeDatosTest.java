package bo.aportaya.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bo.aportaya.auditoria.dominio.FichaDeGobierno;
import bo.aportaya.auditoria.dominio.RevisionDeColumna;
import bo.aportaya.auditoria.web.generado.modelo.TablaDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.TipoDeTabla;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Catalogo de datos · H2 — la lista, el resumen y la ficha contra el recurso REAL generado por
 * {@code scripts/catalogo_de_datos.py}. Las fichas (CU-130) y revisiones (CU-131) son dobles: lo que se prueba aca es
 * el filtro, la pagina y lo que se superpone, no la base.
 */
class ConsultarCatalogoDeDatosTest {

    private final CatalogoDeDatosVigente vigente = new CatalogoDeDatosVigente(new ObjectMapper());
    private final CU130GobernarFichaDeTabla cu130 = mock(CU130GobernarFichaDeTabla.class);
    private final CU131RevisarColumna cu131 = mock(CU131RevisarColumna.class);
    private ConsultarCatalogoDeDatos consulta;
    private int totalDelRecurso;

    @BeforeEach
    void preparar() {
        sinFichasNiRevisiones();
        consulta = new ConsultarCatalogoDeDatos(vigente, cu130, cu131);
        totalDelRecurso = vigente.catalogo().path("tablas").size();
    }

    private void sinFichasNiRevisiones() {
        when(cu130.consultar(any())).thenReturn(new CU130GobernarFichaDeTabla.Vista(Map.of(), List.of()));
        when(cu131.consultar(any())).thenReturn(Map.of());
    }

    private static ConsultarCatalogoDeDatos.Filtro filtro(
            String q, String esquema, String tipo, String estado, String sensibilidad) {
        return new ConsultarCatalogoDeDatos.Filtro(
                Optional.ofNullable(q),
                Optional.ofNullable(esquema),
                Optional.empty(),
                Optional.ofNullable(tipo),
                Optional.ofNullable(estado),
                Optional.empty(),
                Optional.ofNullable(sensibilidad));
    }

    @Test
    @DisplayName("catálogo · lista: sin filtros, 20 filas de todo el catálogo en orden esquema y tabla")
    void primeraPagina() {
        var pagina = consulta.tablas(ConsultarCatalogoDeDatos.Filtro.ninguno(), 1, 20, null);

        assertThat(pagina.items()).hasSize(20);
        assertThat(pagina.total()).isEqualTo(totalDelRecurso);
        assertThat(pagina.totalPaginas()).isEqualTo((totalDelRecurso + 19) / 20);
        assertThat(pagina.items())
                .extracting(f -> f.esquema() + "." + f.tabla())
                .isSorted();
        assertThat(pagina.items())
                .allSatisfy(f -> assertThat(f.gobierno().estado()).isEqualTo("ACTIVA"));
    }

    @Test
    @DisplayName("catálogo · lista: la búsqueda ignora acentos, mayúsculas y guiones bajos")
    void busquedaNormalizada() {
        var pagina = consulta.tablas(filtro("Obligación APORTE", null, null, null, null), 1, 50, null);

        assertThat(pagina.items())
                .extracting(ConsultarCatalogoDeDatos.Fila::tabla)
                .contains("obligacion_aporte");
    }

    @Test
    @DisplayName("catálogo · lista: esquema y tipo se combinan; la infraestructura se distingue del negocio")
    void infraestructuraPorEsquema() {
        var pagina = consulta.tablas(filtro(null, "aportes", "INFRAESTRUCTURA", null, null), 1, 50, null);

        assertThat(pagina.items())
                .extracting(ConsultarCatalogoDeDatos.Fila::tabla)
                .containsExactly("estado_saga", "evento_consumido", "evento_dominio", "shedlock");
    }

    @Test
    @DisplayName("catálogo · lista: el filtro por estado usa la ficha guardada, no la de omisión")
    void estadoDeLaFichaGuardada() {
        var retiro = new FichaDeGobierno(
                "CANDIDATA_A_RETIRO",
                "APROBADA",
                Optional.of("Sistemas"),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                1);
        when(cu130.consultar(any()))
                .thenReturn(new CU130GobernarFichaDeTabla.Vista(
                        Map.of(FichaDeGobierno.clave("aportes", "pago"), retiro), List.of()));

        var pagina = consulta.tablas(filtro(null, null, null, "CANDIDATA_A_RETIRO", null), 1, 20, null);

        assertThat(pagina.total()).isEqualTo(1);
        assertThat(pagina.items().getFirst().gobierno().responsable()).contains("Sistemas");
    }

    @Test
    @DisplayName("catálogo · lista: sensibilidad PERSONAL trae solo tablas con al menos una columna personal")
    void sensibilidad() {
        var pagina = consulta.tablas(filtro(null, null, null, null, "PERSONAL"), 1, 100, null);

        assertThat(pagina.total()).isPositive().isLessThan(totalDelRecurso);
        assertThat(pagina.items())
                .allSatisfy(f -> assertThat(f.clasificacion().personales()).isPositive());
    }

    @Test
    @DisplayName("catálogo · lista: una página que pasa del final viene vacía, con el total real")
    void paginaFueraDeRango() {
        var pagina = consulta.tablas(ConsultarCatalogoDeDatos.Filtro.ninguno(), 999, 20, null);

        assertThat(pagina.items()).isEmpty();
        assertThat(pagina.total()).isEqualTo(totalDelRecurso);
    }

    @Test
    @DisplayName("catálogo · lista: el límite se acota a 100 y la página mínima es 1")
    void limitesAcotados() {
        var pagina = consulta.tablas(ConsultarCatalogoDeDatos.Filtro.ninguno(), 0, 5_000, null);

        assertThat(pagina.limite()).isEqualTo(ConsultarCatalogoDeDatos.FILAS_POR_PAGINA_MAXIMO);
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.items()).hasSize(100);
    }

    @Test
    @DisplayName("catálogo · resumen: las cifras cierran con la lista y cada esquema trae su servicio")
    void resumen() {
        var resumen = consulta.resumen(null);

        assertThat(resumen.tablas()).isEqualTo(totalDelRecurso);
        assertThat(resumen.tablasDeNegocio() + resumen.tablasDeInfraestructura())
                .isEqualTo(totalDelRecurso);
        assertThat(resumen.porEstado().get("ACTIVA")).isEqualTo(totalDelRecurso);
        assertThat(resumen.porRevision().get("DETECTADA")).isEqualTo(totalDelRecurso);
        assertThat(resumen.esquemas().stream()
                        .mapToInt(ConsultarCatalogoDeDatos.Esquema::tablas)
                        .sum())
                .isEqualTo(totalDelRecurso);
        assertThat(resumen.esquemas())
                .filteredOn(e -> e.nombre().equals("aportes"))
                .singleElement()
                .satisfies(e -> assertThat(e.servicio()).contains("aportes"));
        assertThat(resumen.sinResponsable()).isEqualTo(totalDelRecurso);
    }

    @Test
    @DisplayName("catálogo · ficha: trae solo las revisiones de sus columnas; una tabla que no está, vacío")
    void ficha() {
        var aprobada = new RevisionDeColumna(
                "APROBADA", Optional.of("ALTA"), Optional.empty(), Optional.empty(), Optional.empty(), 1);
        when(cu131.consultar(any()))
                .thenReturn(Map.of(
                        RevisionDeColumna.clave("aportes", "pago", "moneda"), aprobada,
                        RevisionDeColumna.clave("aportes", "pago_x", "moneda"), aprobada));

        var ficha = consulta.tabla("aportes", "pago", null).orElseThrow();

        assertThat(ficha.tabla().path("tabla").asText()).isEqualTo("pago");
        assertThat(ficha.revisiones()).containsOnlyKeys("aportes.pago.moneda");
        assertThat(consulta.tabla("aportes", "no_existe", null)).isEmpty();
    }

    @Test
    @DisplayName("catálogo · ficha: trae los casos de uso que nombran la tabla con sus endpoints, y cumple el contrato")
    void operacionesDeLaFicha() throws Exception {
        sinFichasNiRevisiones();
        when(cu131.consultar(any())).thenReturn(Map.of());

        var pago = new ObjectMapper()
                .treeToValue(
                        consulta.tabla("aportes", "pago", null).orElseThrow().tabla(), TablaDelCatalogo.class);
        var cu21 = pago.getOperaciones().stream()
                .filter(o -> o.getCasoDeUso().equals("CU-21"))
                .findFirst()
                .orElseThrow();
        assertThat(cu21.getEndpoints()).extracting(e -> e.getOperationId()).contains("cobrarAporte");

        // La infraestructura no la nombra ningún caso de uso: sin operaciones, y el contrato lo admite vacío.
        var borrador = new ObjectMapper()
                .treeToValue(
                        consulta.tabla("identidad", "borrador_onboarding", null)
                                .orElseThrow()
                                .tabla(),
                        TablaDelCatalogo.class);
        assertThat(borrador.getOperaciones()).isEmpty();
    }

    @Test
    @DisplayName(
            "catálogo · ficha: una tabla fuera del modelo (borrador de onboarding) cumple el contrato y se clasifica")
    void fichaDeInfraestructuraCumpleElContrato() throws Exception {
        sinFichasNiRevisiones();
        when(cu131.consultar(any())).thenReturn(Map.of());

        var borrador = new ObjectMapper()
                .treeToValue(
                        consulta.tabla("identidad", "borrador_onboarding", null)
                                .orElseThrow()
                                .tabla(),
                        TablaDelCatalogo.class);

        assertThat(borrador.getTipo()).isEqualTo(TipoDeTabla.INFRAESTRUCTURA);
        assertThat(borrador.getClasificacion().getPersonales()).isPositive();
        assertThat(borrador.getColumnas())
                .filteredOn(c -> Boolean.TRUE.equals(c.getPk()))
                .extracting(c -> c.getNombre())
                .containsExactly("correo");
    }
}

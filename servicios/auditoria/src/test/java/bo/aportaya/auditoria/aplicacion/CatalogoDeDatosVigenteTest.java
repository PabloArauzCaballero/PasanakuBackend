package bo.aportaya.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.web.generado.modelo.CatalogoDeDatos;
import bo.aportaya.auditoria.web.generado.modelo.ColumnaDelCatalogo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tableros · H7 — el recurso REAL que sale de {@code scripts/catalogo_de_datos.py} cumple el contrato. Si el
 * generador cambia de forma y nadie toca el contrato (o al reves), esto falla en el build y no en el navegador.
 */
class CatalogoDeDatosVigenteTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("tableros · catálogo: el recurso generado cumple CatalogoDeDatos y toda tabla trae su «qué es»")
    void cumpleElContrato() throws Exception {
        var catalogo = mapper.treeToValue(new CatalogoDeDatosVigente(mapper).catalogo(), CatalogoDeDatos.class);

        assertThat(catalogo.getTablas()).hasSizeGreaterThanOrEqualTo(337);
        assertThat(catalogo.getTablas())
                .allSatisfy(t -> assertThat(t.getNegocio().getQueEs()).isNotBlank());
        assertThat(catalogo.getTablas())
                .filteredOn(t -> t.getTabla().equals("credencial_acceso"))
                .flatMap(t -> t.getColumnas())
                .filteredOn(c -> c.getNombre().equals("hash_contrasena"))
                .extracting(ColumnaDelCatalogo::getSensibilidad)
                .containsExactly(ColumnaDelCatalogo.SensibilidadEnum.DERIVADO);
    }
}

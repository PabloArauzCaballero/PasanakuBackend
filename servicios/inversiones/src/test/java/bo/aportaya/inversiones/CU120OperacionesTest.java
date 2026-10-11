package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M1 (cierre de los hallazgos del carril D) · AP-CU120-01 y AP-CU120-03 ahora los emite un camino
 * real, y la sincronizacion del catalogo reserva su clave de idempotencia. PostgreSQL real; aliado
 * doble. Los parametros de los productos son SINTETICOS.
 */
class CU120OperacionesTest extends BaseDeAnalisis {

    @Test
    @DisplayName(
            "Dado un producto vigente · Cuando se consulta por su codigo · Entonces trae sus condiciones con fecha de cotizacion, fuente y origen de los datos")
    void productoPorCodigo() {
        var p = cu120.producto(PRODUCTO_DPF, ctx);

        assertThat(p.codigo()).isEqualTo(PRODUCTO_DPF);
        assertThat(p.condiciones().fuente()).contains("SINTETICO");
        assertThat(p.condiciones().fechaCotizacion()).isNotNull();
        assertThat(p.condiciones().origen().name()).isEqualTo("SINTETICO");
        assertThat(p.condiciones().aptoProduccion()).isFalse();
    }

    @Test
    @DisplayName("Dado un producto que no existe · Cuando se consulta por su codigo · Entonces AP-CU120-01")
    void productoInexistente() {
        assertThatThrownBy(() -> cu120.producto("NO-EXISTE", ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU120-01"));
    }

    @Test
    @DisplayName(
            "Dada una respuesta del aliado que no se puede verificar · Cuando se sincroniza el catalogo · Entonces AP-CU120-03 y no se guarda nada")
    void respuestaNoVerificable() {
        int versiones = contar("select count(*)::int from inversiones.version_condiciones");
        aliado.modo(AliadoDoble.Modo.NO_VERIFICABLE);

        assertThatThrownBy(() -> cu120.sincronizar(sistema()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU120-03"));

        assertThat(contar("select count(*)::int from inversiones.version_condiciones"))
                .isEqualTo(versiones);
    }

    @Test
    @DisplayName(
            "Dado el aliado caido · Cuando se sincroniza el catalogo · Entonces sigue siendo AP-CU120-02, no AP-CU120-03")
    void aliadoCaidoNoEsNoVerificable() {
        aliado.modo(AliadoDoble.Modo.CAIDO);

        assertThatThrownBy(() -> cu120.sincronizar(sistema()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU120-02"));
    }

    @Test
    @DisplayName(
            "reintento: una corrida fallida se retoma con la MISMA clave y la repetida con el mismo contenido devuelve lo guardado")
    void sincronizacionConClave() {
        String clave = clave();
        aliado.modo(AliadoDoble.Modo.CAIDO);
        assertThatThrownBy(() -> operaciones.sincronizarCatalogo(clave, "API", sistema()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU120-02"));
        assertThat(deLaCorrida(clave, "estado")).isEqualTo("FALLIDA");
        assertThat(deLaCorrida(clave, "error")).isEqualTo("AP-CU120-02");

        aliado.modo(AliadoDoble.Modo.NORMAL);
        var primera = operaciones.sincronizarCatalogo(clave, "API", sistema());
        var segunda = operaciones.sincronizarCatalogo(clave, "API", sistema());

        assertThat(deLaCorrida(clave, "estado")).isEqualTo("OK");
        assertThat(segunda).isEqualTo(primera);
        assertThat(contar("select count(*)::int from inversiones.corrida_trabajo where clave_idempotencia = ?", clave))
                .as("una sola fila: la fallida se retomo, no se duplico")
                .isEqualTo(1);
    }

    private String corrida(String estado, String error, String terminadaEn) {
        return "insert into inversiones.corrida_trabajo (id, operacion, clave_idempotencia, hash_solicitud, origen, estado, error,"
                + " iniciada_en, terminada_en) values (gen_random_uuid(), 'DEVENGAR', ?, repeat('a', 64), 'API', '"
                + estado + "', " + error + ", now(), " + terminadaEn + ")";
    }

    @Test
    @DisplayName(
            "rechaza por R-INV-10: una corrida terminada sin fecha de fin, una en curso con fin, y una fallida sin su error")
    void baseProtegeLasCorridas() {
        assertThat(rechazaLaBase(corrida("OK", "null", "null"), clave())).contains("ck_corrida_trabajo_fin");
        assertThat(rechazaLaBase(corrida("EN_CURSO", "null", "now()"), clave())).contains("ck_corrida_trabajo_fin");
        assertThat(rechazaLaBase(corrida("FALLIDA", "null", "now()"), clave())).contains("ck_corrida_trabajo_error");
        assertThat(rechazaLaBase(corrida("OK", "null", "now()"), clave()))
                .as("control negativo: la misma fila, coherente, entra")
                .isEmpty();
        String repetida = clave();
        dslFixtura.execute(corrida("EN_CURSO", "null", "null"), repetida);
        assertThat(rechazaLaBase(corrida("EN_CURSO", "null", "null"), repetida))
                .as("la clave de una operacion es unica")
                .contains("uq_corrida_trabajo_operacion_clave_idempotencia");
    }
}

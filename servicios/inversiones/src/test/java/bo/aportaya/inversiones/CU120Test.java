package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.aplicacion.CU120Catalogo;
import bo.aportaya.inversiones.dominio.OrigenDatos;
import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.ProductoDelAliado;
import bo.aportaya.inversiones.infraestructura.GuardiaDeProduccion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M1 · producto, emisor y condiciones, con fecha de cotizacion y fuente.
 *
 * <p>Contra PostgreSQL real. El aliado es el doble declarado; los datos son sinteticos.
 */
class CU120Test extends BaseDeInversiones {

    private static final String COLUMNAS =
            "id, producto_inversion_id, numero, plazo_dias, base_dias, tasa_nominal_anual, permite_rescate_anticipado,"
                    + " penalizacion_anticipo, dias_rescate, hora_corte, monto_minimo, tasa_retencion, tasa_comision_exito,"
                    + " costos_texto, texto_condiciones, texto_hash, fuente, fecha_cotizacion, origen_datos, apto_produccion,"
                    + " vigente_desde";

    private static String copiaDeUnaVersion(String origen, String apto, String plazo, String tasa) {
        return "insert into inversiones.version_condiciones (" + COLUMNAS
                + ") select gen_random_uuid(), producto_inversion_id,"
                + " 900 + (random() * 30000)::int, " + plazo + ", base_dias, " + tasa + ", permite_rescate_anticipado,"
                + " penalizacion_anticipo, dias_rescate, hora_corte, monto_minimo, tasa_retencion, tasa_comision_exito,"
                + " costos_texto, texto_condiciones, texto_hash, fuente, fecha_cotizacion, " + origen + ", " + apto
                + ", now() from inversiones.version_condiciones where id = ?";
    }

    @Test
    @DisplayName(
            "Dado un DPF y un fondo del aliado · Cuando se consulta el catalogo · Entonces identifica riesgo, plazo, costos, titularidad, fuente y fecha de cotizacion")
    void criterio1() {
        var dpf = producto(PRODUCTO_DPF);
        assertThat(dpf.tipo()).isEqualTo(TipoProducto.DPF);
        assertThat(dpf.nivelRiesgo()).isNotBlank();
        assertThat(dpf.titularidad()).contains("titular");
        var c = dpf.condiciones();
        assertThat(c.plazoDias()).contains(180);
        assertThat(c.baseDias()).contains(365);
        assertThat(c.tasaNominalAnual().orElseThrow()).isEqualByComparingTo("0.040000");
        assertThat(c.costos()).isNotBlank();
        assertThat(c.fuente()).contains("SINTETICO");
        assertThat(c.fechaCotizacion()).isNotNull();
        assertThat(c.texto()).startsWith("DATOS SINTETICOS DE DEMOSTRACION").contains("no esta garantizado");

        var fondo = producto(PRODUCTO_FONDO).condiciones();
        assertThat(fondo.horaCorte()).contains(LocalTime.of(15, 0));
        assertThat(fondo.diasRescate()).contains(1);
        assertThat(fondo.plazoDias()).isEmpty();
        assertThat(fondo.tasaNominalAnual())
                .as("un fondo no declara tasa fija: no hay rentabilidad prometida")
                .isEmpty();
    }

    @Test
    @DisplayName(
            "Dado un producto cuyos parametros no tienen fuente aprobada · Cuando se sincroniza el catalogo · Entonces sale marcado SINTETICO y no apto para produccion")
    void sinteticoYNoAptoParaProduccion() {
        for (var v : cu120.listar(ctx)) {
            assertThat(v.condiciones().origen()).isEqualTo(OrigenDatos.SINTETICO);
            assertThat(v.condiciones().aptoProduccion()).isFalse();
        }
        assertThat(
                        contar(
                                "select count(*)::int from inversiones.version_condiciones where origen_datos = 'SINTETICO' and apto_produccion"))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un entorno productivo y un producto sintetico · Cuando se consulta el catalogo · Entonces no se ofrece ningun producto sintetico")
    void enProduccionNoSeOfreceNadaSintetico() {
        var enProduccion = new CU120Catalogo(
                datos,
                tx,
                catalogo,
                aliado,
                new GuardiaDeProduccion(true, "deshabilitado"),
                new Outbox("inversiones"),
                RELOJ);
        assertThat(enProduccion.listar(ctx)).isEmpty();
    }

    @Test
    @DisplayName("reintento: sincronizar dos veces no duplica versiones, la segunda corrida no inserta nada")
    void sincronizacionIdempotente() {
        int antes = contar("select count(*)::int from inversiones.version_condiciones");
        assertThat(cu120.sincronizar(sistema())).isZero();
        assertThat(cu120.sincronizar(sistema())).isZero();
        assertThat(contar("select count(*)::int from inversiones.version_condiciones"))
                .isEqualTo(antes);
    }

    @Test
    @DisplayName(
            "Dado un producto con condiciones vigentes · Cuando el aliado publica condiciones distintas · Entonces nace una version NUEVA y la anterior queda intacta")
    void versionNuevaSiCambia() {
        String codigo = "DPF-T-CAMBIA-" + System.nanoTime();
        var original = producto(aliado.producto(PRODUCTO_DPF), codigo, "0.040000");
        aliado.agregarProducto(original);
        assertThat(cu120.sincronizar(sistema())).isEqualTo(1);
        var v1 = producto(codigo).condiciones();

        aliado.agregarProducto(producto(original, codigo, "0.050000"));
        assertThat(cu120.sincronizar(sistema())).isEqualTo(1);
        var v2 = producto(codigo).condiciones();

        assertThat(v2.numero()).isEqualTo(v1.numero() + 1);
        assertThat(v2.versionId()).isNotEqualTo(v1.versionId());
        assertThat(v2.textoHash()).isNotEqualTo(v1.textoHash());
        // La que acepto alguien con la version 1 sigue siendo exactamente la misma.
        var releida = tx.en(() -> datos.conContexto(ctx, d -> catalogo.version(d, v1.versionId())))
                .orElseThrow();
        assertThat(releida.tasaNominalAnual().orElseThrow()).isEqualByComparingTo("0.040000");
        assertThat(releida.textoHash()).isEqualTo(v1.textoHash());
        assertThat(rechazaLaBase(
                        "update inversiones.version_condiciones set costos_texto = 'x' where id = ?", v1.versionId()))
                .contains("R-AUD-01");
        assertThat(rechazaLaBase("delete from inversiones.version_condiciones where id = ?", v1.versionId()))
                .contains("R-AUD-01");
    }

    @Test
    @DisplayName("rechaza por R-INV-01: un dato sintetico marcado apto para produccion")
    void baseRechazaSinteticoAptoParaProduccion() {
        UUID version = producto(PRODUCTO_DPF).condiciones().versionId();
        assertThat(rechazaLaBase(copiaDeUnaVersion("'SINTETICO'", "true", "plazo_dias", "tasa_nominal_anual"), version))
                .contains("ck_version_condiciones_sintetico");
        // Control negativo: el mismo insert, valido, SI entra (y se deshace).
        assertThat(rechazaLaBase(
                        copiaDeUnaVersion("'VERIFICADO'", "true", "plazo_dias", "tasa_nominal_anual"), version))
                .isEmpty();
    }

    @Test
    @DisplayName("rechaza por R-INV-01: un fondo con tasa fija y un DPF sin plazo")
    void baseRechazaCondicionesIncoherentes() {
        UUID fondo = producto(PRODUCTO_FONDO).condiciones().versionId();
        assertThat(rechazaLaBase(copiaDeUnaVersion("origen_datos", "apto_produccion", "plazo_dias", "0.040000"), fondo))
                .contains("R-INV-01");
        UUID dpf = producto(PRODUCTO_DPF).condiciones().versionId();
        assertThat(rechazaLaBase(
                        copiaDeUnaVersion("origen_datos", "apto_produccion", "null", "tasa_nominal_anual"), dpf))
                .contains("R-INV-01");
    }

    @Test
    @DisplayName(
            "Dado el aliado caido · Cuando se sincroniza el catalogo · Entonces falla con AP-CU120-02 y no cambia nada")
    void aliadoCaido() {
        int antes = contar("select count(*)::int from inversiones.version_condiciones");
        aliado.modo(AliadoDoble.Modo.CAIDO);
        assertThatThrownBy(() -> cu120.sincronizar(sistema()))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU120-02"));
        assertThat(contar("select count(*)::int from inversiones.version_condiciones"))
                .isEqualTo(antes);
        // Y el catalogo ya sincronizado sigue ofreciendose.
        assertThat(cu120.listar(ctx)).isNotEmpty();
    }

    private static ProductoDelAliado producto(ProductoDelAliado base, String codigo, String tasa) {
        return new ProductoDelAliado(
                codigo,
                base.tipo(),
                base.nombre(),
                base.plazoDias(),
                base.baseDias(),
                Optional.of(new BigDecimal(tasa)),
                base.permiteRescateAnticipado(),
                base.penalizacionAnticipo(),
                base.diasRescate(),
                base.horaCorte(),
                base.montoMinimo(),
                base.tasaRetencion(),
                base.tasaComisionExito(),
                base.fuente(),
                base.fechaCotizacion(),
                base.sintetico());
    }
}

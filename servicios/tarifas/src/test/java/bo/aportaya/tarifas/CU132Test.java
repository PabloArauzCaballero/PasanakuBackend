package bo.aportaya.tarifas;

import static bo.aportaya.tarifas.generado.Tables.PARAMETRO_FINANCIERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.tarifas.aplicacion.CU132RegistroDeParametros;
import bo.aportaya.tarifas.infraestructura.ParametroRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-132 contra la base real: el repositorio jOOQ, el {@code ON CONFLICT} de la clave, la precondición del
 * {@code UPDATE} y el CHECK de cuatro ojos. Cada prueba usa un código de parámetro propio: las versiones no se
 * borran (R-TAR-16). Datos sintéticos; ningún valor es real.
 */
class CU132Test extends BaseDeTarifas {

    private static final OffsetDateTime VIGENTE = OffsetDateTime.parse("2026-01-01T00:00:00Z");

    private CU132RegistroDeParametros cu132;
    private String codigo;
    private final UUID propone = UUID.randomUUID();
    private final UUID aprueba = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        cu132 = new CU132RegistroDeParametros(new Datos(dsl), new ParametroRepositorio(), Reloj.delSistema());
        codigo = "PRUEBA_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        dslFixtura
                .insertInto(PARAMETRO_FINANCIERO)
                .set(PARAMETRO_FINANCIERO.CODIGO, codigo)
                .set(PARAMETRO_FINANCIERO.DESCRIPCION, "Parámetro sintético de prueba")
                .set(PARAMETRO_FINANCIERO.UNIDAD, "DIAS")
                .set(PARAMETRO_FINANCIERO.POR_NIVEL, false)
                .execute();
    }

    private CU132RegistroDeParametros.Propuesta propuesta(String valor) {
        return new CU132RegistroDeParametros.Propuesta(
                Optional.empty(),
                new BigDecimal(valor),
                Optional.empty(),
                VIGENTE,
                "Acta sintética",
                "REF-PRUEBA",
                LocalDate.parse("2025-12-30"));
    }

    @Test
    @DisplayName("Correcto · propone, otra persona aprueba y el catálogo la muestra vigente")
    void proponerYAprobar() {
        UUID clave = UUID.randomUUID();
        var v = cu132.proponer(codigo, propuesta("5"), clave, contextoDe(propone));
        assertThat(v.estado()).isEqualTo("PROPUESTA");
        assertThat(cu132.propuestaPor(v.id(), contextoDe(aprueba))).isEqualTo(propone);

        var aprobada = cu132.aprobar(v.id(), contextoDe(aprueba));
        assertThat(aprobada.estado()).isEqualTo("APROBADA");
        assertThat(aprobada.aprobadoPor()).contains(aprueba);

        var enCatalogo = cu132.catalogo(contextoDe(aprueba)).stream()
                .filter(e -> e.parametro().catalogo().codigo().equals(codigo))
                .findFirst()
                .orElseThrow();
        assertThat(enCatalogo.vigentes())
                .extracting(x -> x.valor().compareTo(new BigDecimal("5")))
                .containsExactly(0);
    }

    @Test
    @DisplayName("Límite · repetir la propuesta con la misma clave no crea otra fila")
    void reintentoSinDuplicar() {
        UUID clave = UUID.randomUUID();
        var primera = cu132.proponer(codigo, propuesta("5"), clave, contextoDe(propone));
        var segunda = cu132.proponer(codigo, propuesta("5.00"), clave, contextoDe(propone));

        assertThat(segunda.id()).isEqualTo(primera.id());
        assertThat(contar("select count(*) from tarifas.version_parametro where clave_idempotencia = ?", clave))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Inválido · quien propuso no aprueba lo suyo y la fila sigue PROPUESTA")
    void mismaPersona() {
        var v = cu132.proponer(codigo, propuesta("5"), UUID.randomUUID(), contextoDe(propone));

        assertThatThrownBy(() -> cu132.aprobar(v.id(), contextoDe(propone))).isInstanceOf(ErrorDeNegocio.class);
        assertThat(contar(
                        "select count(*) from tarifas.version_parametro where id = ? and estado = 'PROPUESTA'", v.id()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Inválido · una segunda aprobación de otra persona no reescribe la primera")
    void segundaAprobacion() {
        var v = cu132.proponer(codigo, propuesta("5"), UUID.randomUUID(), contextoDe(propone));
        cu132.aprobar(v.id(), contextoDe(aprueba));

        assertThatThrownBy(() -> cu132.aprobar(v.id(), contextoDe(UUID.randomUUID())))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(contar(
                        "select count(*) from tarifas.version_parametro where id = ? and aprobado_por = ?",
                        v.id(),
                        aprueba))
                .isEqualTo(1);
    }
}

package bo.aportaya.tarifas;

import static bo.aportaya.tarifas.generado.Tables.PARAMETRO_FINANCIERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.tarifas.aplicacion.CU132RegistroDeParametros;
import bo.aportaya.tarifas.dominio.VersionDelParametro;
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

    // El caso de uso se construye sin Spring en esta base de pruebas. Cada llamada necesita su propia transacción.
    private VersionDelParametro proponer(String valor, UUID clave) {
        return transaccion.execute(t -> cu132.proponer(codigo, propuesta(valor), clave, contextoDe(propone)));
    }

    private VersionDelParametro aprobar(UUID version, UUID persona) {
        return transaccion.execute(t -> cu132.aprobar(version, contextoDe(persona)));
    }

    @Test
    @DisplayName(
            "Dada una versión de parámetro propuesta por una persona · Cuando otra persona la aprueba con su propio"
                    + " token · Entonces la versión queda APROBADA y rige desde su vigencia")
    void proponerYAprobar() {
        UUID clave = UUID.randomUUID();
        var v = proponer("5", clave);
        assertThat(v.estado()).isEqualTo("PROPUESTA");
        UUID proponente = transaccion.execute(t -> cu132.propuestaPor(v.id(), contextoDe(aprueba)));
        assertThat(proponente).isEqualTo(propone);

        var aprobada = aprobar(v.id(), aprueba);
        assertThat(aprobada.estado()).isEqualTo("APROBADA");
        assertThat(aprobada.aprobadoPor()).contains(aprueba);

        var catalogo = transaccion.execute(t -> cu132.catalogo(contextoDe(aprueba)));
        var enCatalogo = catalogo.stream()
                .filter(e -> e.parametro().catalogo().codigo().equals(codigo))
                .findFirst()
                .orElseThrow();
        assertThat(enCatalogo.vigentes())
                .extracting(x -> x.valor().compareTo(new BigDecimal("5")))
                .containsExactly(0);
    }

    @Test
    @DisplayName("Reintento · repetir la propuesta con la misma clave no crea otra fila")
    void reintentoSinDuplicar() {
        UUID clave = UUID.randomUUID();
        var primera = proponer("5", clave);
        var segunda = proponer("5.00", clave);

        assertThat(segunda.id()).isEqualTo(primera.id());
        assertThat(contar("select count(*) from tarifas.version_parametro where clave_idempotencia = ?", clave))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dada una versión de parámetro propuesta por una persona · Cuando la misma persona intenta aprobarla"
            + " · Entonces la aprobación se rechaza y la versión sigue PROPUESTA")
    void mismaPersona() {
        var v = proponer("5", UUID.randomUUID());

        assertThatThrownBy(() -> aprobar(v.id(), propone)).isInstanceOf(ErrorDeNegocio.class);
        assertThat(contar(
                        "select count(*) from tarifas.version_parametro where id = ? and estado = 'PROPUESTA'", v.id()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Rechaza una segunda aprobación de otra persona: no reescribe la primera")
    void segundaAprobacion() {
        var v = proponer("5", UUID.randomUUID());
        aprobar(v.id(), aprueba);

        assertThatThrownBy(() -> aprobar(v.id(), UUID.randomUUID()))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(contar(
                        "select count(*) from tarifas.version_parametro where id = ? and aprobado_por = ?",
                        v.id(),
                        aprueba))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una versión de parámetro APROBADA · Cuando alguien intenta cambiar su valor o borrarla · Entonces"
                    + " se rechaza y la corrección exige una versión nueva")
    void aprobadaInmutable() {
        var v = proponer("5", UUID.randomUUID());
        aprobar(v.id(), aprueba);

        // R-TAR-16: ni el valor ni la fila; se corrige con una versión nueva.
        assertThat(rechazaLaBase("update tarifas.version_parametro set valor = 6 where id = '" + v.id() + "'"))
                .contains("R-TAR-16");
        assertThat(rechazaLaBase("delete from tarifas.version_parametro where id = '" + v.id() + "'"))
                .contains("R-TAR-16");
        var nueva = proponer("6", UUID.randomUUID());
        assertThat(aprobar(nueva.id(), aprueba).estado()).isEqualTo("APROBADA");
    }

    @Test
    @DisplayName("Dado un grupo que ya congeló un parámetro en un nivel · Cuando se intenta congelar otra versión del"
            + " mismo parámetro y nivel para ese grupo · Entonces se rechaza y el grupo conserva la versión que congeló"
            + " al iniciar")
    void congelamientoUnico() {
        var primera = proponer("5", UUID.randomUUID());
        aprobar(primera.id(), aprueba);
        var segunda = proponer("6", UUID.randomUUID());
        aprobar(segunda.id(), aprueba);
        UUID grupo = UUID.randomUUID();
        String congelar = "insert into tarifas.parametro_congelado_grupo"
                + " (grupo_id, parametro_id, version_parametro_id, congelado_en)"
                + " select '%s', parametro_id, id, now() from tarifas.version_parametro where id = '%s'";
        dslFixtura.execute(congelar.formatted(grupo, primera.id()));

        // R-TAR-17: una sola copia por grupo, parámetro y nivel.
        assertThat(rechazaLaBase(congelar.formatted(grupo, segunda.id()))).contains("uq_parametro_congelado_grupo");
        assertThat(contar(
                        "select count(*) from tarifas.parametro_congelado_grupo where grupo_id = ?"
                                + " and version_parametro_id = ?",
                        grupo,
                        primera.id()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Rechaza en la base a quien aprueba lo que propuso y un valor negativo (R-TAR-14, R-TAR-15)")
    void rechazosDeLaBase() {
        var v = proponer("5", UUID.randomUUID());

        assertThat(rechazaLaBase(
                        "update tarifas.version_parametro set estado = 'APROBADA', aprobado_por = propuesto_por,"
                                + " aprobado_en = now() where id = '" + v.id() + "'"))
                .contains("ck_version_parametro_cuatro_ojos");
        assertThat(rechazaLaBase("insert into tarifas.version_parametro (parametro_id, valor, vigente_desde,"
                        + " procedencia_fuente, procedencia_referencia, procedencia_fecha, propuesto_por, propuesto_en,"
                        + " estado, clave_idempotencia) select parametro_id, -1, now(), 'f', 'r', current_date,"
                        + " gen_random_uuid(), now(), 'PROPUESTA', gen_random_uuid() from tarifas.version_parametro"
                        + " where id = '" + v.id() + "'"))
                .contains("ck_version_parametro_valor");
    }
}

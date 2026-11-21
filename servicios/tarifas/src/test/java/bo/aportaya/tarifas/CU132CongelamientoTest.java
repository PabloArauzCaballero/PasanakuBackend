package bo.aportaya.tarifas;

import static bo.aportaya.tarifas.generado.Tables.PARAMETRO_FINANCIERO;
import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.tarifas.aplicacion.CU132RegistroDeParametros;
import bo.aportaya.tarifas.aplicacion.CongelarParametrosDelGrupo;
import bo.aportaya.tarifas.aplicacion.CongelarParametrosDelGrupo.Desenlace;
import bo.aportaya.tarifas.aplicacion.CongelarParametrosDelGrupo.GrupoIniciado;
import bo.aportaya.tarifas.infraestructura.CongelamientoRepositorio;
import bo.aportaya.tarifas.infraestructura.ParametroRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F3.S8.M6 contra la base real: el consumidor congela una vez por evento y una vez por grupo y parámetro, con la
 * versión que regía al iniciar. Las filas no se borran (solo agregado): cada prueba usa su propio parámetro y grupo.
 * Datos sintéticos; ningún valor es real.
 */
class CU132CongelamientoTest extends BaseDeTarifas {

    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-06-01T12:00:00Z");

    private CongelarParametrosDelGrupo congelar;
    private CU132RegistroDeParametros registro;
    private UUID parametroId;
    private String codigo;
    private final UUID grupo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        var parametros = new ParametroRepositorio();
        congelar = new CongelarParametrosDelGrupo(
                new Datos(dsl), parametros, new CongelamientoRepositorio(), Reloj.delSistema());
        registro = new CU132RegistroDeParametros(new Datos(dsl), parametros, Reloj.delSistema());
        codigo = "PRUEBA_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        parametroId = dslFixtura
                .insertInto(PARAMETRO_FINANCIERO)
                .set(PARAMETRO_FINANCIERO.CODIGO, codigo)
                .set(PARAMETRO_FINANCIERO.DESCRIPCION, "Parámetro sintético de prueba")
                .set(PARAMETRO_FINANCIERO.UNIDAD, "DIAS")
                .set(PARAMETRO_FINANCIERO.POR_NIVEL, false)
                .returning(PARAMETRO_FINANCIERO.ID)
                .fetchOne(PARAMETRO_FINANCIERO.ID);
    }

    private UUID aprobada(String valor, OffsetDateTime desde) {
        var v = registro.proponer(
                codigo,
                new CU132RegistroDeParametros.Propuesta(
                        Optional.empty(),
                        new BigDecimal(valor),
                        Optional.empty(),
                        desde,
                        "Acta sintética",
                        "REF-PRUEBA",
                        LocalDate.parse("2026-01-01")),
                UUID.randomUUID(),
                contextoDe(UUID.randomUUID()));
        return registro.aprobar(v.id(), contextoDe(UUID.randomUUID())).id();
    }

    private int filas() {
        return contar(
                "select count(*) from tarifas.parametro_congelado_grupo where grupo_id = ? and parametro_id = ?",
                grupo,
                parametroId);
    }

    private UUID versionCongelada() {
        return (UUID) dsl.fetchOne(
                        "select version_parametro_id from tarifas.parametro_congelado_grupo"
                                + " where grupo_id = ? and parametro_id = ?",
                        grupo,
                        parametroId)
                .get(0);
    }

    @Test
    @DisplayName("Dado un parámetro con una versión aprobada vigente y otra que entra en vigor después · Cuando un"
            + " grupo inicia · Entonces el grupo congela la versión que regía al iniciar")
    void laQueRegiaAlIniciar() {
        UUID vieja = aprobada("3", INICIO.minusDays(30));
        aprobada("9", INICIO.plusDays(1));

        var r = congelar.procesar(new GrupoIniciado(UUID.randomUUID(), UUID.randomUUID(), grupo, INICIO));

        assertThat(r.desenlace()).isEqualTo(Desenlace.CONGELADO);
        assertThat(versionCongelada()).isEqualTo(vieja);
    }

    @Test
    @DisplayName("Evento duplicado · el mismo grupo_iniciado dos veces no congela dos veces; re-emitido con otro id no"
            + " agrega filas")
    void unaSolaVez() {
        aprobada("3", INICIO.minusDays(30));
        UUID evento = UUID.randomUUID();

        congelar.procesar(new GrupoIniciado(evento, UUID.randomUUID(), grupo, INICIO));
        var repetido = congelar.procesar(new GrupoIniciado(evento, UUID.randomUUID(), grupo, INICIO));
        congelar.procesar(new GrupoIniciado(UUID.randomUUID(), UUID.randomUUID(), grupo, INICIO));

        assertThat(repetido.desenlace()).isEqualTo(Desenlace.DUPLICADO);
        assertThat(filas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rechaza completar un código sin versión aprobada al iniciar: no se congela nada y se informa")
    void sinVersionAprobada() {
        var r = congelar.procesar(new GrupoIniciado(UUID.randomUUID(), UUID.randomUUID(), grupo, INICIO));

        assertThat(filas()).isZero();
        assertThat(r.faltantes()).contains(codigo);
    }
}

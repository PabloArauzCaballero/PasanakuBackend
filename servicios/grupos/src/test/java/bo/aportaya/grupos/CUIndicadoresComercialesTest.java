package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.aplicacion.IndicadoresComerciales;
import bo.aportaya.grupos.infraestructura.IndicadoresComercialesRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Indicador.Motivo;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tableros · H5.S1.M2 — los numeros comerciales de {@code grupos}. Fechas en un año lejano propio de cada prueba: el
 * contenedor se comparte y asi el conteo es solo el de la prueba.
 */
class CUIndicadoresComercialesTest extends BaseDeCU60 {

    private final IndicadoresComercialesRepositorio repositorio = new IndicadoresComercialesRepositorio();

    private static ContextoSesion sistema() {
        return ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
    }

    private <T> T leer(java.util.function.Function<org.jooq.DSLContext, T> consulta) {
        return transaccion.execute(t -> new Datos(dsl).conContexto(sistema(), consulta));
    }

    private static LocalDate dia() {
        return LocalDate.of(2170, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(0, 30_000));
    }

    private void transicion(UUID grupo, String de, String a, LocalDate dia) {
        dsl.execute(
                """
                INSERT INTO grupos.historial_estado_grupo
                    (id, grupo_id, estado_anterior, estado_nuevo, motivo, ejecutado_por, fecha_hora)
                VALUES (gen_random_uuid(), ?, ?, ?, 'prueba de tablero', ?, CAST(? AS timestamptz))
                """,
                grupo,
                de,
                a,
                organizador,
                dia + " 10:00:00-04");
    }

    @Test
    @DisplayName(
            "cuadre: un grupo creado en D y conformado en D+4 cuenta como nuevo en D, completo en D+4 y tardó 4 días")
    void creadoYConformado() {
        LocalDate d = dia();
        UUID grupo = fixtura.grupoConformado(3);
        transicion(grupo, "BORRADOR", "ABIERTO_A_INSCRIPCION", d);
        transicion(grupo, "ABIERTO_A_INSCRIPCION", "CONFORMADO", d.plusDays(4));

        long creados = leer(x -> repositorio.creados(x, d, d));
        long conformados = leer(x -> repositorio.conformados(x, d.plusDays(4), d.plusDays(4)));
        assertThat(creados).isEqualTo(1);
        assertThat(conformados).isEqualTo(1);
        var llenado = leer(x -> repositorio.diasDeLlenado(x, d.plusDays(4), d.plusDays(4)));
        assertThat(llenado.total()).isEqualByComparingTo("4.00");
        assertThat(llenado.casos()).isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: de tres participantes de un grupo finalizado, uno vuelve a otro grupo a los 10 días: 1 de 3")
    void retencion() {
        LocalDate d = dia();
        UUID terminado = fixtura.grupoConformado(3);
        var participantes = fixtura.participantesConCupo(terminado, 3);
        transicion(terminado, "EN_CURSO", "FINALIZADO", d);
        UUID vuelve =
                (UUID) dsl.fetchOne("SELECT usuario_id FROM grupos.participante WHERE id = ?", participantes.get(0))
                        .get(0);
        UUID otro = fixtura.grupoConformado(3);
        UUID nuevo = fixtura.participantesConCupo(otro, 1).get(0);
        dsl.execute(
                "UPDATE grupos.participante SET usuario_id = ?, fecha_ingreso = CAST(? AS timestamptz) WHERE id = ?",
                vuelve,
                d.plusDays(10) + " 11:00:00-04",
                nuevo);

        var r = leer(x -> repositorio.retencion(x, d, d));

        assertThat(r.numerador()).isEqualTo(1);
        assertThat(r.denominador()).isEqualTo(3);
    }

    @Test
    @DisplayName("límite: un período viejo no publica activos ni volumen (SIN_HISTORIA); sin hechos, nada tiene valor")
    void sinHistoria() {
        var c = leer(x -> new IndicadoresComerciales(repositorio, Reloj.delSistema())
                .calcular(x, PeriodoDeTablero.de("1970-01-01")));

        assertThat(c)
                .extracting(k -> k.indicador().codigo())
                .containsExactly(
                        "USUARIOS_ACTIVOS",
                        "EMBUDO_EN_GRUPO",
                        "GRUPOS_NUEVOS",
                        "GRUPOS_COMPLETOS",
                        "DIAS_LLENADO_GRUPO",
                        "RETENCION_60D",
                        "VOLUMEN_COMPROMETIDO");
        assertThat(c).allSatisfy(k -> assertThat(k.indicador().valor()).isNull());
        assertThat(c.get(0).indicador().motivo()).isEqualTo(Motivo.SIN_HISTORIA);
        assertThat(c.get(6).indicador().motivo()).isEqualTo(Motivo.SIN_HISTORIA);
    }

    @Test
    @DisplayName("rechaza contar como retenido a quien vuelve después de los 60 días")
    void fueraDeVentana() {
        LocalDate d = dia();
        UUID terminado = fixtura.grupoConformado(3);
        var participantes = fixtura.participantesConCupo(terminado, 1);
        transicion(terminado, "EN_CURSO", "FINALIZADO", d);
        UUID vuelve =
                (UUID) dsl.fetchOne("SELECT usuario_id FROM grupos.participante WHERE id = ?", participantes.get(0))
                        .get(0);
        UUID nuevo = fixtura.participantesConCupo(fixtura.grupoConformado(3), 1).get(0);
        dsl.execute(
                "UPDATE grupos.participante SET usuario_id = ?, fecha_ingreso = CAST(? AS timestamptz) WHERE id = ?",
                vuelve,
                d.plusDays(61) + " 11:00:00-04",
                nuevo);

        var r = leer(x -> repositorio.retencion(x, d, d));

        assertThat(r.numerador()).isZero();
        assertThat(r.denominador()).isEqualTo(1);
    }
}

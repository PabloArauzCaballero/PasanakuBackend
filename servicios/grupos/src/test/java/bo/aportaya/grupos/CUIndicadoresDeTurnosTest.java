package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.aplicacion.IndicadoresDeTurnos;
import bo.aportaya.grupos.infraestructura.IndicadoresDeTurnosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tableros · H4.S1.M4 — turnos normales y de cierre de ciclo. Cada prueba usa fechas de entrega propias en un año
 * lejano: el contenedor se comparte y asi el conteo es solo el de la prueba.
 */
class CUIndicadoresDeTurnosTest extends BaseDeCU60 {

    private final IndicadoresDeTurnosRepositorio repositorio = new IndicadoresDeTurnosRepositorio();

    private static ContextoSesion sistema() {
        return ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
    }

    /** Un grupo de tres turnos con entrega prevista en tres dias seguidos desde `desde`. */
    private List<UUID> grupoDeTres(LocalDate desde) {
        UUID grupo = fixtura.grupoConformado(3);
        var cupos = fixtura.cuposOcupados(grupo, 3);
        var periodos = fixtura.periodos(grupo, 3, new BigDecimal("1500.00"));
        for (int i = 0; i < 3; i++) {
            dsl.execute(
                    "UPDATE grupos.periodo SET fecha_entrega_prevista = ? WHERE id = ?",
                    desde.plusDays(i),
                    periodos.get(i));
        }
        return fixtura.turnos(grupo, periodos, cupos);
    }

    private void estado(UUID turno, String estado) {
        dsl.execute("UPDATE grupos.turno SET estado = ? WHERE id = ?", estado, turno);
    }

    private IndicadoresDeTurnosRepositorio.Turnos cobrados(LocalDate desde, LocalDate hasta) {
        return transaccion.execute(
                t -> new Datos(dsl).conContexto(sistema(), d -> repositorio.cobrados(d, desde, hasta)));
    }

    private static LocalDate dia() {
        return LocalDate.of(2150, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(0, 30_000));
    }

    @Test
    @DisplayName("cuadre: tres turnos cobrados de un grupo de tres son dos normales y uno de cierre de ciclo")
    void normalesYCierre() {
        LocalDate d = dia();
        grupoDeTres(d).forEach(t -> estado(t, "COBRADO"));

        var r = cobrados(d, d.plusDays(2));

        assertThat(r.normales()).isEqualTo(2);
        assertThat(r.deCierre()).isEqualTo(1);
    }

    @Test
    @DisplayName("límite: con un turno cobrado y ninguno de cierre, el cero de cierre se publica (cero real)")
    void ceroRealDeCierre() {
        LocalDate d = dia();
        estado(grupoDeTres(d).get(0), "COBRADO");

        var calculados =
                transaccion.execute(t -> new Datos(dsl).conContexto(sistema(), x -> new IndicadoresDeTurnos(repositorio)
                        .calcular(x, PeriodoDeTablero.dia(d))));

        assertThat(calculados.get(0).indicador().valor()).isEqualByComparingTo("1");
        assertThat(calculados.get(1).indicador().codigo()).isEqualTo("TURNOS_CIERRE_CICLO");
        assertThat(calculados.get(1).indicador().valor()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("límite: un período sin turnos cobrados no tiene valor (SIN_DATOS)")
    void sinDatos() {
        var calculados =
                transaccion.execute(t -> new Datos(dsl).conContexto(sistema(), x -> new IndicadoresDeTurnos(repositorio)
                        .calcular(x, PeriodoDeTablero.de("1999-01-01"))));

        assertThat(calculados).allSatisfy(c -> assertThat(c.indicador().valor()).isNull());
    }

    @Test
    @DisplayName("rechaza tomar un turno ANULADO como el último: el cierre pasa al último vigente")
    void anuladoNoEsElUltimo() {
        LocalDate d = dia();
        var turnos = grupoDeTres(d);
        estado(turnos.get(0), "COBRADO");
        estado(turnos.get(1), "COBRADO");
        estado(turnos.get(2), "ANULADO");

        var r = cobrados(d, d.plusDays(2));

        assertThat(r.normales()).isEqualTo(1);
        assertThat(r.deCierre()).isEqualTo(1);
    }
}

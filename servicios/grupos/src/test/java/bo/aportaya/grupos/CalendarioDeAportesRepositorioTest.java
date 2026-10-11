package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.CalendarioDeAportes;
import bo.aportaya.grupos.infraestructura.CalendarioDeAportesRepositorio;
import bo.aportaya.grupos.infraestructura.ConsultasRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H8.S1.M1 · el productor real del calendario de aportes, contra PostgreSQL: lo que `grupos` afirma de un
 * periodo (turno, fechas, titular e importe de cada cupo OCUPADO) sale de sus propias tablas, con el rol de
 * sistema y detras del permiso del endpoint.
 */
class CalendarioDeAportesRepositorioTest extends BaseDeCU59 {

    private Consultas consultas() {
        return new Consultas(new Datos(dsl), new ConsultasRepositorio(), new CalendarioDeAportesRepositorio());
    }

    private Optional<CalendarioDeAportes> calendario(UUID periodoId) {
        // Un participante cualquiera, sin ningun permiso especial: la lectura es de sistema, el permiso lo pone el
        // endpoint.
        return transaccion.execute(e -> consultas().calendarioDeAportes(periodoId, contexto()));
    }

    @Test
    @DisplayName("devuelve el turno, las fechas y, por cupo OCUPADO, su titular y el aporte del grupo")
    void devuelveLosTitularesYLosImportes() {
        UUID grupo = fixtura.grupoConformado(3);
        List<UUID> titulares = fixtura.participantesConCupo(grupo, 3);
        UUID periodo = fixtura.periodo(grupo, 2, new BigDecimal("1500.00"));

        var resultado = calendario(periodo).orElseThrow();

        assertThat(resultado.periodoId()).isEqualTo(periodo);
        assertThat(resultado.grupoId()).isEqualTo(grupo);
        assertThat(resultado.numero()).isEqualTo(2);
        assertThat(resultado.estado()).isEqualTo("ABIERTO");
        // Las fechas las pone la fixtura con CURRENT_DATE de la base: se comparan contra la base, no contra el reloj de
        // la JVM.
        LocalDate hoyEnLaBase = dsl.fetchOne("SELECT CURRENT_DATE AS hoy").get("hoy", LocalDate.class);
        assertThat(resultado.fechaLimitePago()).isEqualTo(hoyEnLaBase.plusDays(65));
        assertThat(resultado.fechaFinGracia()).isEqualTo(hoyEnLaBase.plusDays(68));
        assertThat(resultado.fechaEntregaPrevista()).isEqualTo(hoyEnLaBase.plusDays(70));
        assertThat(resultado.cupos())
                .extracting(CalendarioDeAportes.Cupo::participanteId)
                .containsExactlyElementsOf(titulares);
        assertThat(resultado.cupos())
                .extracting(CalendarioDeAportes.Cupo::montoEsperado)
                .containsOnly(Dinero.de("500.00", Moneda.BOB));
    }

    @Test
    @DisplayName("un cupo LIBRE o RESERVADO no aporta: solo los OCUPADOS con titular entran al calendario (limite)")
    void soloLosCuposOcupadosAportan() {
        UUID grupo = fixtura.grupoConformado(4);
        List<UUID> titulares = fixtura.participantesConCupo(grupo, 2);
        transaccion.execute(e -> dsl.execute(
                "INSERT INTO grupos.cupo (id, grupo_id, numero, estado, fraccion) VALUES (gen_random_uuid(), ?, 3, 'LIBRE', 1.00)",
                grupo));
        transaccion.execute(e -> dsl.execute(
                "INSERT INTO grupos.cupo (id, grupo_id, numero, estado, fraccion) VALUES (gen_random_uuid(), ?, 4, 'RESERVADO', 1.00)",
                grupo));
        UUID periodo = fixtura.periodo(grupo, 1, new BigDecimal("1000.00"));

        var resultado = calendario(periodo).orElseThrow();

        assertThat(resultado.cupos())
                .extracting(CalendarioDeAportes.Cupo::participanteId)
                .containsExactlyElementsOf(titulares);
    }

    @Test
    @DisplayName("la fraccion del cupo escala el importe, redondeado half-even al centavo (limite)")
    void laFraccionEscalaElImporte() {
        UUID grupo = fixtura.grupoConformado(3);
        List<UUID> titulares = fixtura.participantesConCupo(grupo, 2);
        transaccion.execute(e -> dsl.execute(
                "UPDATE grupos.cupo SET fraccion = 0.50 WHERE grupo_id = ? AND participante_id = ?",
                grupo,
                titulares.get(0)));
        transaccion.execute(e -> dsl.execute(
                "UPDATE grupos.cupo SET fraccion = 0.33 WHERE grupo_id = ? AND participante_id = ?",
                grupo,
                titulares.get(1)));
        // 33.33 x 0.50 = 16.665 (half-even: 16.66, el 6 es par) y 33.33 x 0.33 = 10.9989 (11.00).
        transaccion.execute(e -> dsl.execute("UPDATE grupos.grupo SET monto_aporte = 33.33 WHERE id = ?", grupo));
        UUID periodo = fixtura.periodo(grupo, 1, new BigDecimal("1000.00"));

        var resultado = calendario(periodo).orElseThrow();

        assertThat(resultado.cupos())
                .extracting(c -> c.montoEsperado().monto().toPlainString())
                .containsExactly("16.66", "11.00");
    }

    @Test
    @DisplayName("un periodo que no existe se informa vacio: no se inventa un calendario")
    void periodoDesconocido() {
        assertThat(calendario(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("un periodo de otro grupo no mezcla los cupos de este (aislamiento por grupo)")
    void noMezclaGrupos() {
        UUID grupoA = fixtura.grupoConformado(3);
        UUID grupoB = fixtura.grupoConformado(3);
        List<UUID> titularesA = fixtura.participantesConCupo(grupoA, 2);
        fixtura.participantesConCupo(grupoB, 3);
        UUID periodoA = fixtura.periodo(grupoA, 1, new BigDecimal("1000.00"));

        var resultado = calendario(periodoA).orElseThrow();

        assertThat(resultado.grupoId()).isEqualTo(grupoA);
        assertThat(resultado.cupos())
                .extracting(CalendarioDeAportes.Cupo::participanteId)
                .containsExactlyElementsOf(titularesA);
    }
}

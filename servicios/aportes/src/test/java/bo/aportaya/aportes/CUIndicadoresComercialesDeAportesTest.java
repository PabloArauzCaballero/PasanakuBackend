package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.aplicacion.IndicadoresComercialesDeAportes;
import bo.aportaya.aportes.infraestructura.IndicadoresDeAportesRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tableros · H5.S1.M3 — paso 4 del embudo: el PRIMER aporte de cada participacion, no cada aporte. */
class CUIndicadoresComercialesDeAportesTest extends BaseDeAportes {

    private final IndicadoresDeAportesRepositorio repositorio = new IndicadoresDeAportesRepositorio();

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/La_Paz"));
    }

    private long primerosDeHoy() {
        return transaccion.execute(t -> new Datos(dsl)
                .conContexto(
                        ContextoSesion.deSistema(
                                UUID.randomUUID(), new Traza(UUID.randomUUID().toString())),
                        d -> repositorio.primerAporte(d, hoy().minusDays(1), hoy().plusDays(1))));
    }

    private void pagar(UUID usuario, UUID obligacion, String clave) {
        transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        clave,
                        obligacion,
                        Dinero.de("500.00", Moneda.BOB),
                        Dinero.de("0.00", Moneda.BOB),
                        "BILLETERA_MOVIL",
                        "ref-" + clave,
                        Optional.empty(),
                        false,
                        true),
                contextoDe(usuario)));
    }

    @Test
    @DisplayName("cuadre: el primer aporte pagado de una participación suma uno al paso 4 del embudo")
    void primero() {
        long antes = primerosDeHoy();
        UUID usuario = fixtura.usuario();
        var o = fixtura.obligacion(usuario, "500.00", 10);

        pagar(usuario, o.id(), "embudo-" + o.id());

        assertThat(primerosDeHoy() - antes).isEqualTo(1);
    }

    @Test
    @DisplayName("límite: un período sin aportes no tiene valor (SIN_DATOS)")
    void sinDatos() {
        var c = transaccion.execute(t -> new Datos(dsl)
                .conContexto(
                        ContextoSesion.deSistema(
                                UUID.randomUUID(), new Traza(UUID.randomUUID().toString())),
                        d -> new IndicadoresComercialesDeAportes(repositorio)
                                .calcular(d, PeriodoDeTablero.de("1975-05-05"))));

        assertThat(c.get(0).indicador().codigo()).isEqualTo("EMBUDO_PRIMER_APORTE");
        assertThat(c.get(0).indicador().valor()).isNull();
    }

    @Test
    @DisplayName("rechaza contar un pago REVERSADO o no acreditado como primer aporte")
    void noAcreditadoNoCuenta() {
        long antes = primerosDeHoy();
        UUID usuario = fixtura.usuario();
        var o = fixtura.obligacion(usuario, "500.00", 10);
        pagar(usuario, o.id(), "embudo-rev-" + o.id());

        dsl.execute("UPDATE aportes.pago SET estado = 'REVERSADO' WHERE obligacion_id = ?", o.id());

        assertThat(primerosDeHoy()).isEqualTo(antes);
    }
}

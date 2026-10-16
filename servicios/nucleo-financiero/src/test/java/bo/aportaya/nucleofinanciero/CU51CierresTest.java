package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.ConsultarTesoreria;
import bo.aportaya.nucleofinanciero.infraestructura.TesoreriaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-51 · el listado de cierres diarios que lee Contabilidad en el backoffice (carril A · H8).
 *
 * <p>Los cierres se producen con el caso de uso real ({@code cierreDiarioCU}), no con INSERT: lo que
 * se lista es lo que el sistema escribe. Correcto (orden y totales), limite (la ultima pagina no
 * trae cursor) e invalido (cursor a medias).
 */
class CU51CierresTest extends EscenarioDeCierreDiario {

    private static ConsultarTesoreria consultas() {
        return new ConsultarTesoreria(new Datos(dsl), new TesoreriaRepositorio());
    }

    private static ContextoSesion operador(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private void cerrar(LocalDate fecha) {
        UUID usuario = fixtura.usuario();
        fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        ContextoSesion ctx = contextoDe(usuario);
        transaccion.execute(t -> cierreDiarioCU.ejecutar(entrada(fecha, 0, true), ctx));
    }

    @Test
    @DisplayName(
            "Dados dos días cerrados · Cuando Contabilidad lista los cierres diarios · Entonces los recibe del más reciente al más antiguo con sus totales")
    void listaDelMasReciente() {
        LocalDate antes = diaDe(202);
        LocalDate despues = diaDe(201);
        cerrar(antes);
        cerrar(despues);
        ContextoSesion ctx = operador(fixtura.usuario());

        var primera = transaccion.execute(t -> consultas().cierres(Optional.empty(), Optional.empty(), 1, ctx));
        assertThat(primera.items()).singleElement().satisfies(c -> {
            assertThat(c.fecha()).isEqualTo(despues);
            assertThat(c.totalRecaudado()).isEqualByComparingTo("1000.00");
            assertThat(c.totalConciliado()).isEqualByComparingTo("1000.00");
            assertThat(c.cuadrado()).isTrue();
            assertThat(c.reabiertoEn()).isEmpty();
        });
        assertThat(primera.proximoAntesDe()).contains(despues);

        var segunda = transaccion.execute(
                t -> consultas().cierres(primera.proximoAntesDe(), primera.proximoAntesDeId(), 1, ctx));
        assertThat(segunda.items()).singleElement().satisfies(c -> assertThat(c.fecha())
                .isEqualTo(antes));

        // Limite: despues del ultimo no hay pagina ni cursor inventado.
        var tercera = transaccion.execute(
                t -> consultas().cierres(segunda.proximoAntesDe(), segunda.proximoAntesDeId(), 1, ctx));
        assertThat(tercera.items()).isEmpty();
        assertThat(tercera.proximoAntesDe()).isEmpty();
    }

    @Test
    @DisplayName("rechaza un cursor a medias en el listado de cierres con AP-CU51-05")
    void rechazaCursorAMedias() {
        ContextoSesion ctx = operador(fixtura.usuario());
        assertThatThrownBy(() -> transaccion.execute(
                        t -> consultas().cierres(Optional.empty(), Optional.of(UUID.randomUUID()), 10, ctx)))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU51-05"));
    }
}

package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;

/** Lo comun de las pruebas de la compraventa del derecho: limpieza, rechazo por codigo y fondeo del pozo. */
abstract class BaseDeCompraDeTurno extends BaseDeMercado {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    protected void rechaza(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo(codigo));
    }

    protected void fondearElPozo(Caso c) {
        aportesDoble.responde(new RecaudoDelPozo.Recaudo(
                c.e().periodoId(), c.e().grupoId(), OffsetDateTime.now(), bob("6000.00"), bob("6000.00"), bob("0.00")));
        pozoCU.fondear(
                new CU22EntregarPozoCompleto.Entrada(
                        c.e().grupoId(),
                        c.e().periodoId(),
                        c.e().turnoId(),
                        c.e().cupoId(),
                        c.e().participanteId(),
                        "BILLETERA_MOVIL",
                        LocalDate.now(),
                        "f-" + c.e().turnoId()),
                c.ctxVendedor());
    }
}

package bo.aportaya.entregas;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto.Entrada;
import bo.aportaya.entregas.aplicacion.RegistroDelFondeo;
import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;

/**
 * Lo comun de las pruebas del pozo completo (CU-133): limpieza, un caso de un turno, el recaudo que
 * confirma aportes y el pedido de fondeo. aportes y garantia van como DOBLES de tres niveles
 * (regla 65), definidos en {@link BaseDeEntregas}.
 */
abstract class BaseDePozoCompleto extends BaseDeEntregas {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    protected static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    protected record Caso(UUID usuario, FixturaDeEntregas.Escenario e, ContextoSesion ctx) {}

    protected Caso caso() {
        UUID usuario = fixtura.usuario();
        return new Caso(usuario, fixtura.escenario(usuario), contextoDe(usuario));
    }

    /** aportes confirma pozo 6.000 con `confirmado` en caja: el ejemplo del plan es 5.000. */
    protected void aportesConfirma(Caso c, String confirmado) {
        aportesDoble.responde(new RecaudoDelPozo.Recaudo(
                c.e().periodoId(),
                c.e().grupoId(),
                OffsetDateTime.now(),
                bob("6000.00"),
                bob(confirmado),
                bob("0.00")));
    }

    protected Entrada pedido(Caso c, String clave) {
        return new Entrada(
                c.e().grupoId(),
                c.e().periodoId(),
                c.e().turnoId(),
                c.e().cupoId(),
                c.e().participanteId(),
                "BILLETERA_MOVIL",
                LocalDate.now(),
                clave);
    }

    protected RegistroDelFondeo.Salida fondear(Caso c, String clave) {
        // Sin transaccion alrededor: el caso de uso pregunta afuera y solo escribe adentro.
        return pozoCU.fondear(pedido(c, clave), c.ctx());
    }

    protected int entregasDelTurno(Caso c) {
        return contar("SELECT count(*)::int FROM entregas.entrega_fondo WHERE turno_id = ?", c.e().turnoId());
    }

    protected BigDecimal numero(String consulta, Object... parametros) {
        return dsl.fetchOne(consulta, parametros).get(0, BigDecimal.class);
    }
}

package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la transaccion propia que {@link EvidenciaMfaConsumidaRepositorio} necesita.
 *
 * <p><b>Por que un bean aparte, y no {@code datos.conContexto} directo dentro de
 * {@code SegundoFactorStepUp}.</b> {@code SegundoFactor.verificado} se llama hoy desde
 * {@code BilleteraController}, ANTES de que {@code CU11RetirarSaldo.solicitar} abra su
 * {@code @Transactional} (invariante 2: la transaccion la abre el caso de uso). En ese
 * punto no hay transaccion activa, y {@code Datos.conContexto} exige una. Este bean
 * SEPARADO, con su propio {@code @Transactional}, se la da: al llamarlo desde otro
 * bean (nunca auto-invocado) pasa por el proxy de Spring y abre su propia transaccion
 * corta, aislada de la del retiro.
 *
 * <p><b>Consecuencia declarada, no escondida (H2.S4, A MEDIAS en el daily):</b> el
 * consumo del {@code jti} queda en SU PROPIA transaccion, no en la de CU-11 como pide
 * el plan original (`revierte con la orden`). Si la evidencia pasa pero el retiro
 * falla despues por otra razon (saldo insuficiente, instrumento bloqueado…), el
 * {@code jti} queda consumido igual: la persona necesita un desafio MFA nuevo para
 * reintentar. Es una peor UX, nunca un hueco de seguridad — el {@code jti} jamas se
 * reutiliza, que es la garantia que importa. Cerrar esto del todo pide cambiar la
 * firma de {@code SegundoFactor} para que reciba el {@code DSLContext} de la
 * transaccion del CU, lo que en cascada toca {@code BilleteraController},
 * {@code EntradaRetiro} y cada prueba que construye una — fuera del presupuesto de
 * este turno, declarado en el daily en vez de improvisado a medias.
 */
@Service
public class ConsumoDeEvidenciaMfa {

    private final Datos datos;
    private final EvidenciaMfaConsumidaRepositorio consumos;
    private final Reloj reloj;

    public ConsumoDeEvidenciaMfa(Datos datos, EvidenciaMfaConsumidaRepositorio consumos, Reloj reloj) {
        this.datos = datos;
        this.consumos = consumos;
        this.reloj = reloj;
    }

    @Transactional
    public boolean consumir(UUID jti, UUID usuarioId, String proposito) {
        ContextoSesion sistema = ContextoSesion.deSistema(usuarioId, new Traza(jti.toString()));
        return datos.conContexto(
                sistema,
                dsl -> consumos.consumir(
                        dsl, jti, usuarioId, proposito, reloj.ahora().atOffset(ZoneOffset.UTC)));
    }
}

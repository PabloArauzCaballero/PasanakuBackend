package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaRetiro;
import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro;
import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro.Situacion;
import bo.aportaya.nucleofinanciero.dominio.CostoDeOperacion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-11 · Retirar saldo. */
class CU11Test extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    /** Una billetera con saldo, su instrumento habilitado y la custodia en regla. */
    private Escenario escenario(String saldo, Integer horasDeBloqueo) {
        fixtura.tipoDeCambioDeHoy();
        custodia.cumpleEncaje();
        fixtura.limite("RETIRO", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        return otroEscenario(saldo, horasDeBloqueo);
    }

    /**
     * Un segundo escenario SIN repetir el tipo de cambio, el encaje ni el limite del
     * catalogo: llamarlos dos veces en el mismo metodo de prueba choca con
     * {@code ex_limite_vigencia}. {@code escenario()} configura eso una sola vez; esto
     * es lo que queda para una segunda cuenta dentro de la MISMA prueba.
     */
    private Escenario otroEscenario(String saldo, Integer horasDeBloqueo) {
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal(saldo));
        UUID instrumento = custodia.instrumentoDestino(usuario, true, true, horasDeBloqueo);
        return new Escenario(usuario, cuenta, instrumento, contextoDe(usuario));
    }

    private record Escenario(UUID usuario, UUID cuenta, UUID instrumento, ContextoSesion ctx) {}

    private SalidaRetiro pedir(Escenario e, String monto, String clave) {
        return transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(clave, e.cuenta(), bob(monto), bob("5.00"), e.instrumento(), true, true, false),
                e.ctx()));
    }

    @Test
    @DisplayName(
            "Dado un usuario con saldo suficiente y MFA verificado · Cuando solicita un retiro · Entonces se crea una retencion_saldo VIGENTE por el importe total · Y el saldo_disponible disminuye y el saldo_retenido aumenta en el mismo monto")
    void criterio1() {
        Escenario e = escenario("1000.00", null);

        SalidaRetiro salida = pedir(e, "400.00", "ret-1");

        // H3.S1/Q-02: por debajo del umbral de doble aprobación, la orden nace
        // AUTORIZADA en la misma transacción — nunca PENDIENTE (ese estado ya no lo usa
        // ninguna orden nueva; solo se agrega EN_REVISION cuando el monto lo exige).
        assertThat(salida.estado()).isEqualTo("AUTORIZADA");
        assertThat(salida.montoNeto()).isEqualByComparingTo(bob("395.00"));
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id = ? AND estado = 'VIGENTE'",
                        salida.retencionId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?",
                        e.cuenta()))
                .isEqualTo(600);
        assertThat(contar(
                        "SELECT saldo_retenido::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", e.cuenta()))
                .isEqualTo(400);
    }

    @Test
    @DisplayName(
            "Dado un retiro cuyo proveedor responde error definitivo · Cuando se procesa la respuesta · Entonces la retención queda LIBERADA · Y el saldo_disponible vuelve a su valor original")
    void criterio2() {
        Escenario e = escenario("1000.00", null);
        SalidaRetiro salida = pedir(e, "400.00", "ret-2");
        transaccion.execute(t -> retiroCU.instruirPago(salida.ordenRetiroId(), e.ctx()));

        transaccion.execute(t -> retiroCU.rechazar(salida.ordenRetiroId(), "PROVEEDOR_RECHAZO", e.ctx()));

        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id = ? AND estado = 'LIBERADA'",
                        salida.retencionId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?",
                        e.cuenta()))
                .isEqualTo(1000);
        // Y NO se escribio ningun movimiento: la plata nunca salio. Registrar un
        // debito y su reverso ensuciaria el extracto con dos lineas que no explican
        // nada.
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE origen_id = ?",
                        salida.ordenRetiroId()))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un instrumento de fondeo agregado hace una hora · Cuando el usuario intenta retirar hacia él · Entonces la operación se rechaza por período de enfriamiento")
    void criterio3() {
        Escenario e = escenario("1000.00", 23);

        assertThatThrownBy(() -> pedir(e, "100.00", "ret-3"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("se agrego hace poco");
        assertThat(contar("SELECT count(*)::int FROM nucleo_financiero.orden_retiro"))
                .isZero();
    }

    @Test
    @DisplayName("reintento: la misma clave de idempotencia dos veces devuelve la misma respuesta y un solo efecto")
    void reintento() {
        Escenario e = escenario("1000.00", null);

        SalidaRetiro primera = pedir(e, "200.00", "ret-idem");
        SalidaRetiro segunda = pedir(e, "200.00", "ret-idem");

        assertThat(segunda.ordenRetiroId()).isEqualTo(primera.ordenRetiroId());
        // Una sola retencion: si hubiera dos, la persona tendria 400 apartados por un
        // retiro de 200.
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE cuenta_billetera_id = ?",
                        e.cuenta()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT saldo_retenido::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", e.cuenta()))
                .isEqualTo(200);
    }

    @Test
    @DisplayName(
            "kill-test H1: misma clave de idempotencia, dos cuentas distintas · Cuando cada una retira · Entonces cada una recibe su PROPIA orden, nunca la de la otra")
    void mismaClaveDistintaCuenta() {
        Escenario a = escenario("1000.00", null);
        Escenario b = otroEscenario("1000.00", null);

        SalidaRetiro salidaA = pedir(a, "300.00", "ret-compartida");
        SalidaRetiro salidaB = pedir(b, "300.00", "ret-compartida");

        assertThat(salidaB.ordenRetiroId()).isNotEqualTo(salidaA.ordenRetiroId());
        assertThat(contar(
                        "SELECT saldo_retenido::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", b.cuenta()))
                .isEqualTo(300);
    }

    @Test
    @DisplayName("replay: la clave repetida devuelve el costo ALMACENADO en la orden, nunca el de la entrada repetida")
    void replayDevuelveCostoAlmacenado() {
        Escenario e = escenario("1000.00", null);

        SalidaRetiro primera = transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(
                        "ret-costo", e.cuenta(), bob("200.00"), bob("5.00"), e.instrumento(), true, true, false),
                e.ctx()));
        // Mismo clave, misma cuenta, pero con un costo DISTINTO en la entrada: si el
        // caso de uso recotizara en el replay, este segundo costo se filtraria.
        SalidaRetiro segunda = transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(
                        "ret-costo", e.cuenta(), bob("200.00"), bob("99.00"), e.instrumento(), true, true, false),
                e.ctx()));

        assertThat(segunda.ordenRetiroId()).isEqualTo(primera.ordenRetiroId());
        assertThat(segunda.costoRetiro()).isEqualByComparingTo(bob("5.00"));
    }

    @Test
    @DisplayName("concurrencia: dos transacciones sobre el mismo agregado, una gana y nunca hay doble efecto")
    void concurrencia() {
        // Pagar y rechazar la misma orden: la decide el WHERE estado = 'PENDIENTE'.
        Escenario e = escenario("1000.00", null);
        SalidaRetiro salida = pedir(e, "300.00", "ret-carrera");
        transaccion.execute(t -> retiroCU.instruirPago(salida.ordenRetiroId(), e.ctx()));

        transaccion.execute(t -> retiroCU.confirmarPago(salida.ordenRetiroId(), e.ctx()));

        assertThatThrownBy(() -> transaccion.execute(t -> retiroCU.rechazar(salida.ordenRetiroId(), "tarde", e.ctx())))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND estado = 'PAGADA'",
                        salida.ordenRetiroId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: la suma de debitos iguala la de creditos, al centavo")
    void cuadre() {
        // El pago escribe dos patas que suman cero, y el disponible baja por el monto
        // COMPLETO: el costo lo paga quien retira, no se cobra aparte.
        Escenario e = escenario("1000.00", null);
        SalidaRetiro salida = pedir(e, "400.00", "ret-cuadre");
        transaccion.execute(t -> retiroCU.instruirPago(salida.ordenRetiroId(), e.ctx()));

        var pago = transaccion.execute(t -> retiroCU.confirmarPago(salida.ordenRetiroId(), e.ctx()));

        assertThat(contar(
                        """
                        SELECT COALESCE(SUM(CASE WHEN sentido = 'CREDITO' THEN monto ELSE -monto END), 0)::int
                          FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id = ?
                        """,
                        pago.transaccionId()))
                .isZero();
        assertThat(pago.saldoDespues()).isEqualByComparingTo(bob("600.00"));
        // Y el neto es el monto menos el costo, al centavo.
        assertThat(CostoDeOperacion.netoDeRetiro(bob("400.00"), bob("5.00"))).isEqualByComparingTo(bob("395.00"));
    }

    @Test
    @DisplayName("evento duplicado y fuera de orden: un solo efecto")
    void eventoDuplicado() {
        UUID idEvento = UUID.randomUUID();

        Boolean primera = transaccion.execute(e -> consumidos.registrar(dsl, idEvento, "pagos"));
        Boolean segunda = transaccion.execute(e -> consumidos.registrar(dsl, idEvento, "pagos"));

        assertThat(primera).isTrue();
        assertThat(segunda).isFalse();
    }

    @Test
    @DisplayName("compensacion: se fuerza el fallo de cada paso y el sistema queda cuadrado")
    void compensa() {
        // Pedir mas de lo disponible aborta ANTES de retener: no puede quedar una
        // retencion sin su orden, ni una orden sin su retencion.
        Escenario e = escenario("100.00", null);

        assertThatThrownBy(() -> pedir(e, "100.01", "ret-compensa"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no cubre el retiro");

        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE cuenta_billetera_id = ?",
                        e.cuenta()))
                .isZero();
        assertThat(contar("SELECT count(*)::int FROM nucleo_financiero.orden_retiro"))
                .isZero();
        assertThat(contar(
                        "SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?",
                        e.cuenta()))
                .isEqualTo(100);
    }

    @Test
    @DisplayName("rechaza sin segundo factor: el retiro no sale sin MFA")
    void rechazaSinMfa() {
        Escenario e = escenario("1000.00", null);

        assertThatThrownBy(() -> transaccion.execute(t -> retiroCU.solicitar(
                        new EntradaRetiro(
                                "ret-sinmfa",
                                e.cuenta(),
                                bob("100.00"),
                                bob("5.00"),
                                e.instrumento(),
                                false,
                                false,
                                false),
                        e.ctx())))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("segundo factor");
    }

    @Test
    @DisplayName(
            "rechaza con MFA_INVALIDO (no MFA_REQUERIDO, H2.S2.M5) cuando SI se mando evidencia pero no paso la verificacion")
    void rechazaMfaInvalido() {
        // La diferencia con rechazaSinMfa() de arriba es exactamente el tercer
        // booleano: aca SI se proveyo evidencia (evidenciaMfaProvista=true), solo
        // que no fue valida — es la distincion que separa MFA_REQUERIDO de
        // MFA_INVALIDO, y sin ella el mensaje le diria a la persona "falta" cuando
        // en realidad "lo que mandaste no sirve".
        Escenario e = escenario("1000.00", null);

        assertThatThrownBy(() -> transaccion.execute(t -> retiroCU.solicitar(
                        new EntradaRetiro(
                                "ret-mfa-invalido",
                                e.cuenta(),
                                bob("100.00"),
                                bob("5.00"),
                                e.instrumento(),
                                false,
                                true,
                                false),
                        e.ctx())))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no es valida");
        assertThat(contar("SELECT count(*)::int FROM nucleo_financiero.orden_retiro"))
                .isZero();
    }

    @Test
    @DisplayName("rechaza el orden inverso: primero se retiene, despues se paga")
    void rechazaOrdenInverso() {
        // Si el pago fuera antes que la retencion, entre las dos cosas la persona
        // podria gastar el mismo saldo en otra operacion. Se verifica que al pedir el
        // retiro el saldo YA esta apartado, antes de que exista ningun movimiento.
        Escenario e = escenario("1000.00", null);

        SalidaRetiro salida = pedir(e, "700.00", "ret-orden");

        assertThat(contar(
                        "SELECT saldo_retenido::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", e.cuenta()))
                .isEqualTo(700);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE origen_id = ?",
                        salida.ordenRetiroId()))
                .isZero();
        // Y con 700 apartados, ya no se puede pedir otro por 400.
        assertThatThrownBy(() -> pedir(e, "400.00", "ret-orden-2"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no cubre el retiro");
    }

    @Test
    @DisplayName("rechaza con el encaje roto: no salen retiros nuevos")
    void rechazaEncajeRoto() {
        // Registrar que el encaje no se cumple y seguir pagando es el escenario
        // clasico de la corrida: cobran los primeros y no queda para los demas.
        var conEncaje =
                new Situacion(true, true, bob("1000.00"), bob("100.00"), true, true, Optional.empty(), false, true);
        var sinEncaje =
                new Situacion(true, true, bob("1000.00"), bob("100.00"), true, true, Optional.empty(), false, false);
        OffsetDateTime ahora = OffsetDateTime.of(2026, 8, 27, 12, 0, 0, 0, ZoneOffset.UTC);

        assertThat(CondicionesDeRetiro.evaluar(conEncaje, ahora).permitido()).isTrue();
        assertThat(CondicionesDeRetiro.evaluar(sinEncaje, ahora).codigo()).isEqualTo("ENCAJE_INCUMPLIDO");
        // Y el bloqueo de autoridad pesa mas que el saldo: se mira antes del encaje.
        var conOficio =
                new Situacion(true, true, bob("1000.00"), bob("100.00"), true, true, Optional.empty(), true, true);
        assertThat(CondicionesDeRetiro.evaluar(conOficio, ahora).codigo()).isEqualTo("BLOQUEO_DE_AUTORIDAD");
    }
}

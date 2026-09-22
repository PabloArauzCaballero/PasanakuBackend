package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.SalidaCobro;
import bo.aportaya.aportes.dominio.RecargoDeMora;
import bo.aportaya.aportes.dominio.SaldoDeLaObligacion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-21 · Cobrar el aporte del periodo. */
class CU21Test extends BaseDeAportes {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private SalidaCobro cobrar(UUID obligacionId, String monto, String clave, ContextoSesion ctx) {
        return transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        clave,
                        obligacionId,
                        bob(monto),
                        bob("0.00"),
                        "BILLETERA_MOVIL",
                        "ref-" + clave,
                        Optional.empty(),
                        false,
                        true),
                ctx));
    }

    @Test
    @DisplayName(
            "Dada una obligación de Bs 500 pendiente · Cuando el participante paga con saldo · Entonces monto_pagado es 500 y estado es PAGADO · Y la cuenta del grupo aumentó Bs 500")
    void criterio1() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        ContextoSesion ctx = contextoDe(usuario);

        SalidaCobro salida = cobrar(obligacion.id(), "500.00", "cob-1", ctx);

        assertThat(salida.estadoObligacion()).isEqualTo("PAGADO");
        assertThat(salida.pendiente()).isEqualByComparingTo(bob("0.00"));
        assertThat(contar("SELECT monto_pagado::int FROM aportes.obligacion_aporte WHERE id = ?", obligacion.id()))
                .isEqualTo(500);
        // El saldo del grupo lo mueve nucleo-financiero (invariante 12): este
        // servicio lo PIDE por evento. Lo que se verifica aca es que lo pidio.
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.evento_dominio WHERE tipo = ? AND agregado_id = ?",
                        "aportes.aporte_cobrado",
                        obligacion.id()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un pago no conciliado con el extracto · Cuando se intenta cerrar el día · Entonces el cierre_diario no puede marcarse cuadrado")
    void criterio2() {
        // fn_bil_validar_cierre_diario es la autoridad: con excepciones abiertas el
        // dia no se marca cuadrado. La regla vive en la base, no en la aplicacion.
        assertThat(contar("SELECT count(*)::int FROM pg_proc WHERE proname = ?", "fn_bil_validar_cierre_diario"))
                .isEqualTo(1);
        assertThat(
                        rechazaLaBase(
                                """
                        INSERT INTO nucleo_financiero.cierre_diario
                            (id, fecha, total_recaudado, total_conciliado, total_excepciones,
                             cantidad_pagos, cuadrado, cerrado_por, cerrado_en)
                        VALUES (gen_random_uuid(), current_date, 100.00, 50.00, 50.00, 1, true,
                                gen_random_uuid(), now())
                        """))
                .isNotEmpty();
    }

    @Test
    @DisplayName(
            "Dado un aporte vencido con política de mora · Cuando corre el proceso diario · Entonces se crea una obligación de tipo RECARGO_MORA con obligacion_origen_id")
    void criterio3() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", -10);
        fixtura.politicaDeMora(obligacion.grupoId(), 0, "PORCENTUAL", "0.05", "100.00");
        ContextoSesion ctx = contextoDe(usuario);

        var salida = transaccion.execute(t -> cobroCU.generarRecargos(ctx));

        assertThat(salida.generados()).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.obligacion_aporte WHERE tipo = 'RECARGO_MORA' AND obligacion_origen_id = ?",
                        obligacion.id()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT monto_esperado::int FROM aportes.obligacion_aporte WHERE obligacion_origen_id = ?",
                        obligacion.id()))
                .isEqualTo(25);
    }

    @Test
    @DisplayName("reintento: la misma clave de idempotencia dos veces devuelve la misma respuesta y un solo efecto")
    void reintento() {
        // El webhook del proveedor llega dos veces mas seguido de lo que uno cree.
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        ContextoSesion ctx = contextoDe(usuario);

        SalidaCobro a = cobrar(obligacion.id(), "200.00", "cob-idem", ctx);
        SalidaCobro b = cobrar(obligacion.id(), "200.00", "cob-idem", ctx);

        assertThat(b.pagoId()).isEqualTo(a.pagoId());
        assertThat(b.esNuevo()).isFalse();
        assertThat(contar("SELECT monto_pagado::int FROM aportes.obligacion_aporte WHERE id = ?", obligacion.id()))
                .isEqualTo(200);
    }

    @Test
    @DisplayName(
            "idempotencia con el scope del indice: la misma clave en OTRA obligacion es un pago distinto (H1.S1, hallazgo C)")
    void mismaClaveOtraObligacionEsOtroPago() {
        // uq_pago_idem ampara (obligacion_id, clave_idempotencia): dos obligaciones que
        // por coincidencia comparten clave (una plantilla de la app movil, un mismo
        // instante para dos participantes) no son la misma operacion. Antes del fix,
        // `porClaveIdempotencia` filtraba SOLO por la clave y el segundo pago devolvia
        // el pagoId de la primera obligacion — la persona equivocada.
        // La referencia del proveedor es del proveedor, no de nuestra clave: dos
        // obligaciones distintas SIEMPRE traen una referencia distinta en la vida
        // real (`uq_pago_proveedor_id_referencia_proveedor`, hallazgo real
        // encontrado corriendo este test contra PostgreSQL real). Lo unico que se
        // comparte a proposito, para probar el scope, es la CLAVE DE IDEMPOTENCIA.
        UUID usuario1 = fixtura.usuario();
        UUID usuario2 = fixtura.usuario();
        var obligacion1 = fixtura.obligacion(usuario1, "500.00", 10);
        var obligacion2 = fixtura.obligacion(usuario2, "500.00", 10);
        String claveCompartida = "cob-colision-scope";

        SalidaCobro a = transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        claveCompartida,
                        obligacion1.id(),
                        bob("500.00"),
                        bob("0.00"),
                        "BILLETERA_MOVIL",
                        "ref-colision-scope-1",
                        Optional.empty(),
                        false,
                        true),
                contextoDe(usuario1)));
        SalidaCobro b = transaccion.execute(t -> cobroCU.acreditar(
                new EntradaCobro(
                        claveCompartida,
                        obligacion2.id(),
                        bob("500.00"),
                        bob("0.00"),
                        "BILLETERA_MOVIL",
                        "ref-colision-scope-2",
                        Optional.empty(),
                        false,
                        true),
                contextoDe(usuario2)));

        assertThat(a.pagoId()).isNotEqualTo(b.pagoId());
        assertThat(a.esNuevo()).isTrue();
        assertThat(b.esNuevo()).isTrue();
        assertThat(b.obligacionId()).isEqualTo(obligacion2.id());
        assertThat(contar("SELECT count(*)::int FROM aportes.pago WHERE clave_idempotencia = ?", claveCompartida))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("concurrencia: dos transacciones sobre el mismo agregado, una gana y nunca hay doble efecto")
    void concurrencia() {
        // La version optimista es la barrera: dos pagos que leyeron la misma version
        // no pueden escribir los dos.
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        ContextoSesion ctx = contextoDe(usuario);

        cobrar(obligacion.id(), "300.00", "cob-c1", ctx);
        cobrar(obligacion.id(), "200.00", "cob-c2", ctx);

        assertThat(contar("SELECT monto_pagado::int FROM aportes.obligacion_aporte WHERE id = ?", obligacion.id()))
                .isEqualTo(500);
        assertThat(contar("SELECT version FROM aportes.obligacion_aporte WHERE id = ?", obligacion.id()))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("cuadre: la suma de debitos iguala la de creditos, al centavo")
    void cuadre() {
        // Lo pagado, lo condonado y lo cubierto por la garantia cuentan IGUAL contra
        // el esperado: para el grupo el periodo esta cubierto, venga de donde venga.
        var estado = new SaldoDeLaObligacion.Estado(bob("500.00"), bob("200.00"), bob("100.00"), bob("200.00"));

        assertThat(estado.cubierto()).isEqualByComparingTo(bob("500.00"));
        assertThat(estado.pendiente()).isEqualByComparingTo(bob("0.00"));
        assertThat(estado.estaSaldada()).isTrue();
        // Pagar de mas no deja pendiente negativo: el excedente es una devolucion,
        // no un numero que confunda todas las sumas de arriba.
        var conExcedente = new SaldoDeLaObligacion.Estado(bob("500.00"), bob("600.00"), bob("0.00"), bob("0.00"));
        assertThat(conExcedente.pendiente()).isEqualByComparingTo(bob("0.00"));
    }

    @Test
    @DisplayName("evento duplicado y fuera de orden: un solo efecto")
    void eventoDuplicado() {
        UUID idEvento = UUID.randomUUID();

        Boolean primera = transaccion.execute(e -> consumidos.registrar(dsl, idEvento, "nucleo-financiero"));
        Boolean segunda = transaccion.execute(e -> consumidos.registrar(dsl, idEvento, "nucleo-financiero"));

        assertThat(primera).isTrue();
        assertThat(segunda).isFalse();
    }

    @Test
    @DisplayName("compensacion: se fuerza el fallo de cada paso y el sistema queda cuadrado")
    void compensa() {
        // Pagar de mas aborta ANTES de escribir: el excedente se devuelve por su
        // propio camino, no se mete en la cuota.
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", 10);
        ContextoSesion ctx = contextoDe(usuario);

        assertThatThrownBy(() -> cobrar(obligacion.id(), "500.01", "cob-demas", ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no coincide");

        assertThat(contar("SELECT count(*)::int FROM aportes.pago")).isZero();
        assertThat(contar("SELECT monto_pagado::int FROM aportes.obligacion_aporte WHERE id = ?", obligacion.id()))
                .isZero();
    }

    @Test
    @DisplayName("rechaza generar recargo sin politica escrita: cobrar sin regla es lo que se reclama")
    void rechazaRecargoSinPolitica() {
        UUID usuario = fixtura.usuario();
        var obligacion = fixtura.obligacion(usuario, "500.00", -10);
        ContextoSesion ctx = contextoDe(usuario);

        var salida = transaccion.execute(t -> cobroCU.generarRecargos(ctx));

        assertThat(salida.generados()).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.obligacion_aporte WHERE obligacion_origen_id = ?",
                        obligacion.id()))
                .isZero();
    }

    @Test
    @DisplayName("rechaza un recargo sin tope: una deuda que crece sola no la cobra nadie")
    void rechazaRecargoSinTope() {
        var politica = new RecargoDeMora.Politica(
                3, RecargoDeMora.Tipo.PORCENTUAL_DIARIO, new BigDecimal("0.01"), bob("50.00"), 15, 30);

        // Dentro de la gracia, cero: castigar a quien paga un dia tarde igual que a
        // quien no paga no distingue nada.
        assertThat(RecargoDeMora.calcular(bob("500.00"), 2, politica).recargo()).isEqualByComparingTo(bob("0.00"));
        // A los 100 dias el porcentaje daria 500, pero el tope manda.
        assertThat(RecargoDeMora.calcular(bob("500.00"), 103, politica).recargo())
                .isEqualByComparingTo(bob("50.00"));
        assertThat(RecargoDeMora.calcular(bob("500.00"), 103, politica).severidad())
                .isEqualTo("INCUMPLIMIENTO");
    }
}

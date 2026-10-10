package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.Resultado;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril I (quinta vuelta) · lo que {@code aportes.aporte_cobrado} NO puede hacer: moneda distinta, cuentas que no existen o
 * no operan, saldo que no alcanza, canal que no sale de una billetera, caida a mitad y carreras. En todos los casos
 * rechazados NO se escribe nada (ni libro, ni mayor, ni marca de consumo): el reproceso aplica el evento cuando la causa se
 * resuelva. El camino correcto, el cuadre y la reversa estan en {@link ConsumoDeAportesTest}.
 */
class ConsumoDeAportesRechazosTest extends BaseDeAportes {

    /** Nada quedo escrito para este aporte. */
    private void sinEfectos(Aporte a, bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.AporteCobrado e) {
        assertThat(transacciones(a.obligacion())).isZero();
        assertThat(asientosDeAportes(a.grupo())).isZero();
        assertThat(marcas(e.eventoId())).isZero();
        assertThat(contar(
                        "SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE tipo ="
                                + " 'nucleo_financiero.aporte_aplicado' AND agregado_id = ?",
                        a.obligacion()))
                .isZero();
    }

    @Test
    @DisplayName("rechaza moneda distinta: un aporte en USD sin billetera en USD (solo hay BOB) es AP-CU12-02 y no"
            + " convierte ni mueve nada")
    void rechazaMonedaSinBilletera() {
        var a = aporte("1000.00");
        var enDolares = cobrado(a, "100.00", "USD", "BILLETERA_MOVIL");

        assertThatThrownBy(() -> procesar(enDolares))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU12-02"));

        sinEfectos(a, enDolares);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("1000.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza moneda distinta: billetera del participante en USD y del grupo solo en BOB no se mezcla"
            + " (AP-CU12-02)")
    void rechazaMonedasMezcladas() {
        var a = aporte("1000.00");
        billeteraUsdDe(a.usuario());
        var enDolares = cobrado(a, "100.00", "USD", "BILLETERA_MOVIL");

        assertThatThrownBy(() -> procesar(enDolares))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU12-02"));

        sinEfectos(a, enDolares);
    }

    @Test
    @DisplayName(
            "rechaza moneda distinta: con billeteras en USD pero SIN cuenta del plan en USD no se asienta ni se mueve"
                    + " (AP-CU24-02) y el libro tampoco")
    void rechazaMonedaSinMapeoContable() {
        var a = aporte("1000.00");
        billeteraUsdDe(a.usuario());
        billeteraDelGrupo(a.grupo(), "USD", true);
        var enDolares = cobrado(a, "100.00", "USD", "BILLETERA_MOVIL");

        assertThatThrownBy(() -> procesar(enDolares))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU24-02"));

        sinEfectos(a, enDolares);
    }

    @Test
    @DisplayName("moneda distinta: con billeteras y plan en USD el aporte se mueve en USD y la billetera en BOB queda"
            + " intacta")
    void aplicaEnDolaresSinTocarBolivianos() {
        var a = aporte("1000.00");
        UUID usd = billeteraUsdDe(a.usuario());
        UUID usdGrupo = billeteraDelGrupo(a.grupo(), "USD", true);
        var mapeo = new HashMap<>(MAPEO);
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        contable.cuentaDeMovimiento("SU.USD." + sufijo, "PASIVO", "ACREEDORA", "USD");
        contable.cuentaDeMovimiento("SG.USD." + sufijo, "PASIVO", "ACREEDORA", "USD");
        mapeo.put("SALDO_USUARIOS.USD", "SU.USD." + sufijo);
        mapeo.put("SALDO_GRUPOS.USD", "SG.USD." + sufijo);
        var enDolares = cobrado(a, "100.00", "USD", "BILLETERA_MOVIL");

        var salida = transaccion.execute(t -> armar(libroReal, mapeo).procesar(enDolares));

        assertThat(salida.resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(saldoDelLibro(usd)).isEqualByComparingTo("-100.00");
        assertThat(saldoDelLibro(usdGrupo)).isEqualByComparingTo("100.00");
        assertThat(saldoDelMayor("SG.USD." + sufijo)).isEqualByComparingTo("100.00");
        assertThat(saldoDelMayor("SU.USD." + sufijo)).isEqualByComparingTo("-100.00");
        // Los bolivianos no se tocaron.
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("1000.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("0.00");
        assertThat((String) dsl.fetchOne(
                                "SELECT moneda FROM nucleo_financiero.transaccion_billetera WHERE id = ?",
                                salida.transaccionId().orElseThrow())
                        .get(0))
                .isEqualTo("USD");
    }

    @Test
    @DisplayName(
            "rechaza cuenta inexistente: el participante sin billetera, y el grupo sin billetera, son AP-CU12-02 sin"
                    + " efectos")
    void rechazaCuentasInexistentes() {
        // Participante sin billetera.
        UUID sinBilletera = fixtura.usuario();
        UUID obligacion = obligaciones.obligacionDeAporte(sinBilletera);
        UUID grupo = (UUID) dsl.fetchOne("SELECT grupo_id FROM aportes.obligacion_aporte WHERE id = ?", obligacion)
                .get(0);
        billeteraDelGrupo(grupo, "BOB", false);
        var a1 = new Aporte(sinBilletera, obligacion, grupo, null, null);
        var e1 = cobrado(a1, "100.00");
        assertThatThrownBy(() -> procesar(e1))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU12-02"));
        sinEfectos(a1, e1);

        // Grupo sin billetera.
        UUID usuario = fixtura.usuario();
        fixtura.billetera(usuario, "ESTANDAR", new BigDecimal("500.00"));
        UUID obligacion2 = obligaciones.obligacionDeAporte(usuario);
        UUID grupo2 = (UUID) dsl.fetchOne("SELECT grupo_id FROM aportes.obligacion_aporte WHERE id = ?", obligacion2)
                .get(0);
        var a2 = new Aporte(usuario, obligacion2, grupo2, null, null);
        var e2 = cobrado(a2, "100.00");
        assertThatThrownBy(() -> procesar(e2))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU12-02"));
        sinEfectos(a2, e2);
    }

    @Test
    @DisplayName("rechaza cuenta no operativa: una billetera BLOQUEADA o CONGELADA (del participante o del grupo) es"
            + " AP-CU12-03 y no se mueve")
    void rechazaCuentaNoOperativa() {
        var a = aporte("1000.00");
        var e = cobrado(a, "100.00");

        dslFixtura.execute(
                "UPDATE nucleo_financiero.cuenta_billetera SET estado = 'BLOQUEADA_AUTORIDAD' WHERE id = ?",
                a.billetera());
        assertThatThrownBy(() -> procesar(e)).isInstanceOf(ErrorDeNegocio.class).satisfies(x -> assertThat(codigo(x))
                .isEqualTo("AP-CU12-03"));
        dslFixtura.execute(
                "UPDATE nucleo_financiero.cuenta_billetera SET estado = 'ACTIVA' WHERE id = ?", a.billetera());

        dslFixtura.execute(
                "UPDATE nucleo_financiero.cuenta_billetera SET estado = 'CONGELADA' WHERE id = ?",
                a.billeteraDelGrupo());
        assertThatThrownBy(() -> procesar(e)).isInstanceOf(ErrorDeNegocio.class).satisfies(x -> assertThat(codigo(x))
                .isEqualTo("AP-CU12-03"));

        sinEfectos(a, e);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("rechaza saldo insuficiente: el libro decide, no la cache; AP-CU12-01 y la billetera no baja de lo que"
            + " tiene")
    void rechazaSaldoInsuficiente() {
        var a = aporte("300.00");
        var e = cobrado(a, "500.00");

        assertThatThrownBy(() -> procesar(e)).isInstanceOf(ErrorDeNegocio.class).satisfies(x -> assertThat(codigo(x))
                .isEqualTo("AP-CU12-01"));

        sinEfectos(a, e);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName(
            "rechaza un importe que no es positivo (AP-CU24-07) y un canal que no sale de una billetera no mueve nada"
                    + " (se marca consumido)")
    void rechazaImporteCeroYCanalExterno() {
        var a = aporte("1000.00");
        var cero = cobrado(a, "0.00");
        assertThatThrownBy(() -> procesar(cero))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(x -> assertThat(codigo(x)).isEqualTo("AP-CU24-07"));
        sinEfectos(a, cero);

        // Un cobro por QR/transferencia entra por la conciliacion: este consumidor no inventa su contrapartida.
        var porQr = cobrado(a, "500.00", "BOB", "QR_INTEROPERABLE");
        var salida = procesar(porQr);
        assertThat(salida.resultado()).isEqualTo(Resultado.CANAL_SIN_BILLETERA);
        assertThat(transacciones(a.obligacion())).isZero();
        assertThat(asientosDeAportes(a.grupo())).isZero();
        assertThat(marcas(porQr.eventoId())).isEqualTo(1);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("rechaza una caida a mitad: si el libro falla DESPUES de asentar en el mayor, todo se revierte (sin"
            + " asiento huerfano, sin marca) y el reproceso aplica el aporte una sola vez")
    void caidaAMitad() {
        var a = aporte("1000.00");
        var e = cobrado(a, "500.00");
        var libroCaido = new LibroDeBilletera() {
            @Override
            public UUID registrarVinculada(
                    org.jooq.DSLContext d,
                    UUID id,
                    String tipo,
                    String origenTipo,
                    UUID origenId,
                    String canal,
                    bo.aportaya.plataforma.dominio.Dinero monto,
                    String clave,
                    Optional<UUID> iniciadaPor,
                    List<Pata> patas,
                    Vinculos vinculos,
                    java.time.OffsetDateTime ahora) {
                throw new IllegalStateException("se cayo el libro a mitad de la transaccion");
            }
        };

        assertThatThrownBy(
                        () -> transaccion.execute(t -> armar(libroCaido, MAPEO).procesar(e)))
                .isInstanceOf(IllegalStateException.class);

        sinEfectos(a, e);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("1000.00");

        // El reproceso (la entrega es al menos una vez) lo aplica, y solo una vez.
        assertThat(procesar(e).resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(procesar(e).resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(transacciones(a.obligacion())).isEqualTo(1);
        assertThat(asientosDeAportes(a.grupo())).isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: dos replicas que entregan el MISMO evento a la vez mueven la plata una sola vez")
    void concurrenciaDelMismoEvento() throws Exception {
        var a = aporte("1000.00");
        var e = cobrado(a, "500.00");
        var resultados =
                enParalelo(() -> procesar(e).resultado(), () -> procesar(e).resultado());

        assertThat(resultados).containsExactlyInAnyOrder(Resultado.APLICADO, Resultado.DUPLICADO);
        assertThat(transacciones(a.obligacion())).isEqualTo(1);
        assertThat(asientosDeAportes(a.grupo())).isEqualTo(1);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("500.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName(
            "concurrencia: dos aportes DISTINTOS de Bs 500 con Bs 600 en la billetera no la dejan en negativo: uno se"
                    + " aplica y el otro es AP-CU12-01")
    void concurrenciaNoGastaDosVeces() throws Exception {
        var a = aporte("600.00");
        var uno = cobrado(a, "500.00");
        var otro = cobrado(a, "500.00");

        var resultados = enParalelo(() -> intentar(uno), () -> intentar(otro));

        assertThat(resultados).containsExactlyInAnyOrder("APLICADO", "AP-CU12-01");
        assertThat(transacciones(a.obligacion())).isEqualTo(1);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("100.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("500.00");
    }

    private String intentar(bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.AporteCobrado e) {
        try {
            return procesar(e).resultado().name();
        } catch (ErrorDeNegocio rechazo) {
            return rechazo.codigo().valor();
        }
    }

    @SafeVarargs
    private <T> List<T> enParalelo(java.util.concurrent.Callable<T>... tareas) throws Exception {
        var hilos = Executors.newFixedThreadPool(tareas.length);
        try {
            var largada = new CountDownLatch(1);
            var futuros = new java.util.ArrayList<java.util.concurrent.Future<T>>();
            for (var tarea : tareas) {
                futuros.add(hilos.submit(() -> {
                    largada.await();
                    return tarea.call();
                }));
            }
            largada.countDown();
            var resultados = new java.util.ArrayList<T>();
            for (var f : futuros) {
                resultados.add(f.get(60, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            hilos.shutdownNow();
        }
    }
}

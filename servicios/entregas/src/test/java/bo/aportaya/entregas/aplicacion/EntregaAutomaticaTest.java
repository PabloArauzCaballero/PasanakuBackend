package bo.aportaya.entregas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bo.aportaya.entregas.aplicacion.EntregaAutomatica.PeriodoRecaudado;
import bo.aportaya.entregas.aplicacion.EntregaAutomatica.Resultado;
import bo.aportaya.entregas.aplicacion.PasosDeEntregaAutomatica.Pendiente;
import bo.aportaya.entregas.dominio.FondeoDelPozo;
import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.entregas.infraestructura.SagaDeEntregaAutomaticaRepositorio.Saga;
import bo.aportaya.entregas.infraestructura.TurnoDelGrupoRepositorio.Turno;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * F3.L2.M01 · la saga de la entrega automatica contra DOBLES (regla 65), en sus tres niveles.
 *
 * <p>{@code aportes} es un doble del puerto {@link RecaudoDelPozo} (el contrato real se prueba en
 * {@code AdaptadoresDelPozoTest}); la escritura local ({@link RegistroDelFondeo}) y las transacciones cortas
 * ({@link PasosDeEntregaAutomatica}) son mocks: lo que se prueba es la DECISION de la saga. La persistencia real, el
 * duplicado y el fuera de orden contra la base estan en {@code CU133EntregaAutomaticaTest} (integracion, CI).
 */
class EntregaAutomaticaTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID TURNO = UUID.randomUUID();
    private static final String CORRELACION = UUID.randomUUID().toString();
    private static final Reloj RELOJ = Reloj.fijo(Instant.parse("2026-11-21T15:00:00Z"));

    /** DOBLE de aportes: responde lo cargado; vacio = caido. */
    private static final class Aportes implements RecaudoDelPozo {
        Optional<Recaudo> respuesta = Optional.empty();
        final AtomicInteger consultas = new AtomicInteger();

        @Override
        public Optional<Recaudo> consultar(UUID periodoId) {
            consultas.incrementAndGet();
            return respuesta;
        }
    }

    private final PasosDeEntregaAutomatica pasos = mock(PasosDeEntregaAutomatica.class);
    private final RegistroDelFondeo registro = mock(RegistroDelFondeo.class);
    private final Aportes aportes = new Aportes();
    private final EntregaAutomatica automatica =
            new EntregaAutomatica(pasos, new CU22EntregarPozoCompleto(aportes, registro), RELOJ, "BILLETERA_MOVIL");

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private void hayAviso(Optional<Turno> turno) {
        when(pasos.pendiente(PERIODO))
                .thenReturn(Optional.of(new Pendiente(new Saga(PERIODO, GRUPO, "INICIADA", CORRELACION), turno)));
    }

    private static Optional<Turno> turnoDe(UUID grupo) {
        return Optional.of(new Turno(TURNO, grupo, PERIODO, UUID.randomUUID(), UUID.randomUUID(), 1));
    }

    private void aportesConfirma(UUID grupo, String confirmado) {
        aportes.respuesta = Optional.of(new RecaudoDelPozo.Recaudo(
                PERIODO, grupo, OffsetDateTime.now(), bob("6000.00"), bob(confirmado), bob("0.00")));
    }

    private void elRegistroDeja(String estado) {
        var cifras = new FondeoDelPozo.Resultado(
                bob("6000.00"), bob("6000.00"), bob("0.00"), bob("0.00"), bob("0.00"), bob("0.00"));
        when(registro.registrar(any(), any(), any(), any()))
                .thenReturn(new RegistroDelFondeo.Salida(UUID.randomUUID(), UUID.randomUUID(), estado, cifras, true));
    }

    @Test
    @DisplayName("correcto: con el turno conocido y el pozo en caja, fondea con la clave del periodo y cierra la saga")
    void fondeaYCierra() {
        hayAviso(turnoDe(GRUPO));
        aportesConfirma(GRUPO, "6000.00");
        elRegistroDeja("FONDEADO");

        assertThat(automatica.avanzar(PERIODO)).isEqualTo(Resultado.FONDEADO);

        var pedido = ArgumentCaptor.forClass(RegistroDelFondeo.Entrada.class);
        verify(registro).registrar(pedido.capture(), any(), any(), any());
        assertThat(pedido.getValue().clave()).isEqualTo("fondeo-auto-" + PERIODO);
        assertThat(pedido.getValue().turnoId()).isEqualTo(TURNO);
        assertThat(pedido.getValue().metodoDesembolso()).isEqualTo("BILLETERA_MOVIL");
        assertThat(pedido.getValue().fechaProgramada()).isEqualTo(RELOJ.hoy());
        verify(pasos).cerrar(PERIODO, "COMPLETADA", 2, CORRELACION);
    }

    @Test
    @DisplayName("correcto: el aviso nuevo se registra y fondea en el mismo paso")
    void avisoNuevoFondea() {
        var aviso = new PeriodoRecaudado(UUID.randomUUID(), UUID.randomUUID(), PERIODO, GRUPO, "COMPLETO");
        when(pasos.avisar(aviso.eventoId(), aviso.correlationId(), PERIODO, GRUPO, "COMPLETO"))
                .thenReturn(true);
        hayAviso(turnoDe(GRUPO));
        aportesConfirma(GRUPO, "6000.00");
        elRegistroDeja("FONDEADO");

        assertThat(automatica.recibir(aviso)).isEqualTo(Resultado.FONDEADO);
    }

    @Test
    @DisplayName("limite: el mismo aviso dos veces no vuelve a preguntar ni a fondear")
    void avisoDuplicado() {
        var aviso = new PeriodoRecaudado(UUID.randomUUID(), UUID.randomUUID(), PERIODO, GRUPO, "COMPLETO");
        when(pasos.avisar(any(), any(), any(), any(), any())).thenReturn(false);

        assertThat(automatica.recibir(aviso)).isEqualTo(Resultado.DUPLICADO);
        assertThat(aportes.consultas).hasValue(0);
        verify(registro, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("limite: el aviso antes que los turnos deja la saga esperando, sin preguntar a aportes")
    void turnoTodaviaDesconocido() {
        hayAviso(Optional.empty());

        assertThat(automatica.avanzar(PERIODO)).isEqualTo(Resultado.ESPERA_TURNO);
        assertThat(aportes.consultas).hasValue(0);
        verify(pasos).postergar(PERIODO, CORRELACION);
        verify(pasos, never()).cerrar(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("limite: con faltante la saga queda EN_CURSO, esperando otro aviso del mismo periodo")
    void conFaltante() {
        hayAviso(turnoDe(GRUPO));
        aportesConfirma(GRUPO, "5000.00");
        elRegistroDeja("CON_PENDIENTE");

        assertThat(automatica.avanzar(PERIODO)).isEqualTo(Resultado.CON_PENDIENTE);
        verify(pasos).cerrar(PERIODO, "EN_CURSO", 1, CORRELACION);
    }

    @Test
    @DisplayName("limite: una saga que ya no esta INICIADA no hace nada")
    void sinPendiente() {
        when(pasos.pendiente(PERIODO)).thenReturn(Optional.empty());

        assertThat(automatica.avanzar(PERIODO)).isEqualTo(Resultado.SIN_PENDIENTE);
        assertThat(aportes.consultas).hasValue(0);
    }

    @Test
    @DisplayName("invalido: aportes caido no escribe nada y la saga se reintenta")
    void aportesCaido() {
        hayAviso(turnoDe(GRUPO));

        assertThat(automatica.avanzar(PERIODO)).isEqualTo(Resultado.ESPERA_APORTES);
        verify(registro, never()).registrar(any(), any(), any(), any());
        verify(pasos).postergar(PERIODO, CORRELACION);
        verify(pasos, never()).cerrar(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("invalido: un recaudo de otro grupo rechaza la saga y no fondea")
    void recaudoDeOtroGrupo() {
        hayAviso(turnoDe(GRUPO));
        aportesConfirma(UUID.randomUUID(), "6000.00");

        assertThat(automatica.avanzar(PERIODO)).isEqualTo(Resultado.RECHAZADO);
        verify(registro, never()).registrar(any(), any(), any(), any());
        verify(pasos).cerrar(PERIODO, "FALLIDA", 9, CORRELACION);
    }

    @Test
    @DisplayName("invalido: un turno de otro grupo rechaza la saga sin preguntar a aportes")
    void turnoDeOtroGrupo() {
        hayAviso(turnoDe(UUID.randomUUID()));

        assertThat(automatica.avanzar(PERIODO)).isEqualTo(Resultado.RECHAZADO);
        assertThat(aportes.consultas).hasValue(0);
        verify(pasos).cerrar(PERIODO, "FALLIDA", 9, CORRELACION);
    }

    @Test
    @DisplayName("invalido: encendida sin metodo de desembolso valido, no levanta")
    void sinMetodoNoLevanta() {
        var cu = new CU22EntregarPozoCompleto(aportes, registro);

        assertThatThrownBy(() -> new EntregaAutomatica(pasos, cu, RELOJ, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DECISION_REQUIRED");
        assertThatThrownBy(() -> new EntregaAutomatica(pasos, cu, RELOJ, "COMPENSACION_INTERNA"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("limite: el barrido sigue con las demas sagas aunque una falle")
    void barridoSigueTrasUnFallo() {
        UUID roto = UUID.randomUUID();
        when(pasos.iniciadas(eq(RELOJ.ahora().minus(Duration.ofMinutes(5)).atOffset(java.time.ZoneOffset.UTC)), eq(10)))
                .thenReturn(List.of(roto, PERIODO));
        when(pasos.pendiente(roto)).thenThrow(new IllegalStateException("base caida"));
        hayAviso(turnoDe(GRUPO));
        aportesConfirma(GRUPO, "6000.00");
        elRegistroDeja("FONDEADO");

        assertThat(automatica.barrer(Duration.ofMinutes(5), 10)).isEqualTo(1);
    }
}

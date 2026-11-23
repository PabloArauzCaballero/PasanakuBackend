package bo.aportaya.notificaciones.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bo.aportaya.notificaciones.aplicacion.CU82ProcesarRespuesta.EntradaRespuesta;
import bo.aportaya.notificaciones.dominio.FirmaDeWebhook;
import bo.aportaya.notificaciones.infraestructura.RespuestaRepositorio;
import bo.aportaya.notificaciones.infraestructura.SupresionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-82 · F1.L5.M23: todo sale de la carga FIRMADA, con ventana de tiempo y deduplicacion por el id firmado.
 *
 * <p>Doble del proveedor de mensajeria (no hay proveedor contratado, P-15): firma de verdad, con HMAC-SHA256 y el
 * secreto de prueba, igual que lo haria el real. Tres niveles del contrato: correcto (firma valida), limite (el mismo
 * mensaje dos veces; {@code emitidoEn} justo en el borde de la ventana) e invalido (firma invalida; par capturado y
 * reenviado con otro remitente; fuera de la ventana). La base es un doble: lo que se fija es que el caso de uso no
 * llega a escribir nada que no este firmado.
 */
class WebhookDeMensajeriaTest {

    private static final String SECRETO = "secreto-solo-para-pruebas";
    private static final Instant AHORA = Instant.parse("2026-11-21T12:00:00Z");
    private static final Duration VENTANA = Duration.ofMinutes(5);
    private static final String VICTIMA = "+59170000001"; // sintetico
    private static final String ATACANTE = "+59170000002"; // sintetico

    private CU82ProcesarRespuesta cu82;
    private RespuestaRepositorio respuestas;
    private SupresionRepositorio supresiones;
    private final Set<UUID> yaConsumidos = new HashSet<>();
    private final ContextoSesion ctx = ContextoSesion.deSistema(
            UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void armar() {
        var datos = mock(Datos.class);
        when(datos.conContexto(any(), any()))
                .thenAnswer(i -> ((Function<Object, Object>) i.getArgument(1)).apply(null));
        respuestas = mock(RespuestaRepositorio.class);
        supresiones = mock(SupresionRepositorio.class);
        var consumidos = mock(Consumidos.class);
        // Como la tabla con UNIQUE: el segundo registro del mismo id devuelve falso.
        when(consumidos.registrar(any(), any(), anyString())).thenAnswer(i -> yaConsumidos.add(i.getArgument(1)));
        when(respuestas.porIdentificador(any(), anyString(), anyString()))
                .thenReturn(Optional.of(new RespuestaRepositorio.Remitente(UUID.randomUUID(), UUID.randomUUID())));
        when(respuestas.registrar(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());
        cu82 = new CU82ProcesarRespuesta(
                datos, respuestas, supresiones, consumidos, mock(Outbox.class), Reloj.fijo(AHORA), SECRETO, VENTANA);
    }

    /** Lo que manda el doble del proveedor: la carga y su firma. */
    private static String carga(String id, String remitente, String contenido, Instant emitido) {
        return "{\"idMensaje\":\"" + id + "\",\"canal\":\"SMS\",\"remitente\":\"" + remitente + "\",\"contenido\":\""
                + contenido + "\",\"emitidoEn\":\"" + emitido + "\"}";
    }

    private CU82ProcesarRespuesta.SalidaRespuesta recibir(String carga, String firma, String remitenteDeAfuera) {
        return cu82.ejecutar(new EntradaRespuesta(null, remitenteDeAfuera, firma, carga, null, null, null), ctx);
    }

    @Test
    @DisplayName("Correcto · firma válida: el remitente que se busca es el FIRMADO")
    void firmaValidaUsaLoFirmado() {
        String c = carga("m-1", VICTIMA, "BAJA", AHORA);

        var salida = recibir(c, FirmaDeWebhook.firmar(c, SECRETO), null);

        assertThat(salida.respuestaId()).isNotNull();
        verify(respuestas).porIdentificador(any(), eq(VICTIMA), eq("SMS"));
    }

    @Test
    @DisplayName("Límite · el mismo mensaje firmado dos veces se procesa una sola vez")
    void elMismoMensajeUnaVez() {
        String c = carga("m-2", VICTIMA, "ya pague", AHORA);
        String f = FirmaDeWebhook.firmar(c, SECRETO);

        var primera = recibir(c, f, null);
        var segunda = recibir(c, f, null);

        assertThat(primera.respuestaId()).isNotNull();
        assertThat(segunda.respuestaId()).isNull();
        verify(respuestas, org.mockito.Mockito.times(1))
                .registrar(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Límite · emitido justo en el borde de la ventana se acepta; un segundo después, no")
    void bordeDeLaVentana() {
        String enElBorde = carga("m-3", VICTIMA, "consulta", AHORA.minus(VENTANA));
        assertThat(recibir(enElBorde, FirmaDeWebhook.firmar(enElBorde, SECRETO), null)
                        .respuestaId())
                .isNotNull();

        String pasado = carga("m-4", VICTIMA, "consulta", AHORA.minus(VENTANA).minusSeconds(1));
        assertThatThrownBy(() -> recibir(pasado, FirmaDeWebhook.firmar(pasado, SECRETO), null))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("fuera de la ventana");
    }

    @Test
    @DisplayName("Inválido · firma inválida: se rechaza sin tocar nada")
    void firmaInvalida() {
        String c = carga("m-5", VICTIMA, "BAJA", AHORA);

        assertThatThrownBy(() -> recibir(c, FirmaDeWebhook.firmar(c, "otro-secreto"), null))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("firma del webhook no valida");
        verifyNoInteractions(respuestas, supresiones);
    }

    @Test
    @DisplayName("Inválido · un par (carga, firma) capturado y reenviado con OTRO remitente se rechaza")
    void parCapturadoConOtroRemitente() {
        String c = carga("m-6", VICTIMA, "BAJA", AHORA);

        assertThatThrownBy(() -> recibir(c, FirmaDeWebhook.firmar(c, SECRETO), ATACANTE))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("firma del webhook no valida");
        verify(respuestas, never()).porIdentificador(any(), any(), any());
        verifyNoInteractions(supresiones);
    }

    @Test
    @DisplayName("Inválido · una carga firmada sin los campos del formato se rechaza")
    void cargaSinFormato() {
        String c = "{\"de\":\"" + VICTIMA + "\",\"texto\":\"BAJA\"}";

        assertThatThrownBy(() -> recibir(c, FirmaDeWebhook.firmar(c, SECRETO), null))
                .isInstanceOf(ErrorDeNegocio.class);
        verifyNoInteractions(respuestas, supresiones);
    }
}

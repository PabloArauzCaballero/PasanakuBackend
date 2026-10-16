package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo.EntradaReclamo;
import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo.EntradaRespuesta;
import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo.SalidaReclamo;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · H3 · responder un reclamo es seguro de reintentar.
 *
 * <p>El esquema de cumplimiento no tiene {@code respuesta_idempotente}: el estado guardado hace de
 * clave. La misma respuesta del mismo responsable sobre un reclamo ya cerrado devuelve el recibo de
 * antes, sin otro evento; cualquier otra cosa sobre un reclamo cerrado es AP-CU52-06.
 */
class CU52RespuestaTest extends BaseDeCumplimiento {

    private static final String TEXTO = "Se reviso el cobro: corresponde al tarifario vigente del mes.";

    private String punto;

    @BeforeEach
    void escenario() {
        punto = "APP-" + UUID.randomUUID().toString().substring(0, 8);
        gobiernoFixtura.puntoDeReclamo(punto, "APP", true);
    }

    private static ContextoSesion operador(UUID id) {
        return ContextoSesion.de(id, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private UUID ingresar() {
        UUID usuario = fixtura.usuario();
        var ctx = ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
        return transaccion
                .execute(t -> reclamoCU.ingresar(
                        new EntradaReclamo(
                                usuario,
                                punto,
                                "COMISION",
                                "BILLETERA",
                                null,
                                "Me cobraron una comision que no figura en el tarifario publicado del mes.",
                                "APP"),
                        ctx))
                .reclamoId();
    }

    private SalidaReclamo responder(UUID reclamo, String resultado, String texto, ContextoSesion ctx) {
        return transaccion.execute(
                t -> reclamoCU.responder(new EntradaRespuesta(reclamo, resultado, texto, null), ctx));
    }

    private int eventos(UUID reclamo) {
        return contar(
                """
                        SELECT count(*)::int FROM cumplimiento.evento_dominio
                         WHERE tipo = 'cumplimiento.reclamo_respondido' AND agregado_id = ?
                        """,
                reclamo);
    }

    private static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    @Test
    @DisplayName("Dado un reclamo respondido · Cuando el mismo responsable repite la misma respuesta · Entonces"
            + " recibe el mismo recibo y el evento se emitio una sola vez")
    void reintentoIdentico() {
        UUID reclamo = ingresar();
        ContextoSesion responsable = operador(fixtura.usuario());

        SalidaReclamo primera = responder(reclamo, "DESFAVORABLE", TEXTO, responsable);
        SalidaReclamo repetida = responder(reclamo, "DESFAVORABLE", TEXTO, responsable);

        assertThat(repetida).isEqualTo(primera);
        assertThat(primera.estado()).isEqualTo("CERRADO");
        assertThat(eventos(reclamo)).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza otra respuesta sobre un reclamo cerrado: AP-CU52-06 y la primera queda intacta")
    void otraRespuesta() {
        UUID reclamo = ingresar();
        ContextoSesion responsable = operador(fixtura.usuario());
        responder(reclamo, "DESFAVORABLE", TEXTO, responsable);

        assertThatThrownBy(() -> responder(reclamo, "PARCIAL", TEXTO, responsable))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU52-06"));
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.reclamo_cliente WHERE id = ? AND resultado = 'DESFAVORABLE'",
                        reclamo))
                .isEqualTo(1);
        assertThat(eventos(reclamo)).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza la misma respuesta de OTRO responsable: no es un reintento, es AP-CU52-06")
    void otroResponsable() {
        UUID reclamo = ingresar();
        responder(reclamo, "DESFAVORABLE", TEXTO, operador(fixtura.usuario()));

        assertThatThrownBy(() -> responder(reclamo, "DESFAVORABLE", TEXTO, operador(fixtura.usuario())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU52-06"));
    }

    @Test
    @DisplayName("rechaza responder un reclamo inexistente: AP-CU52-05, ya no el codigo del punto de reclamo")
    void inexistente() {
        assertThatThrownBy(() -> responder(UUID.randomUUID(), "DESFAVORABLE", TEXTO, operador(fixtura.usuario())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU52-05"));
    }

    @Test
    @DisplayName("concurrencia: dos respuestas identicas simultaneas cierran el reclamo una vez y emiten un evento")
    void dosRespuestasSimultaneas() throws Exception {
        UUID reclamo = ingresar();
        ContextoSesion responsable = operador(fixtura.usuario());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<SalidaReclamo>> envios = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                envios.add(pool.submit(() -> {
                    largada.await();
                    return responder(reclamo, "DESFAVORABLE", TEXTO, responsable);
                }));
            }
            largada.countDown();
            assertThat(envios.get(0).get(30, TimeUnit.SECONDS))
                    .isEqualTo(envios.get(1).get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        assertThat(eventos(reclamo)).isEqualTo(1);
    }
}

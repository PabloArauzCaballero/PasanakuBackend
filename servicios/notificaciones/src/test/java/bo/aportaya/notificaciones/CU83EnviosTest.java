package bo.aportaya.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.notificaciones.aplicacion.CU83DespacharLote.EntradaLote;
import bo.aportaya.notificaciones.aplicacion.ConsultarMensajeria;
import bo.aportaya.notificaciones.infraestructura.AuditoriaDeMensajeriaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-83 · los intentos de envio que mira Auditoria (backoffice carril A · A6), contra la base real.
 *
 * <p>El estado de cada envio lo escribe el despacho real (CU-83 con el adaptador espia), no un INSERT:
 * lo que la lista muestra es lo que el sistema escribe. El acuse del proveedor se inserta como dato,
 * porque en este servicio todavia no hay codigo que escriba {@code evento_entrega_mensaje}.
 * Correcto, limite (recorrido con cursor) e invalido (estado desconocido, cursor a medias).
 */
class CU83EnviosTest extends BaseDeNotificaciones {

    @BeforeEach
    void reiniciarAdaptador() {
        adaptador.reiniciar();
    }

    @AfterEach
    void limpiar() {
        // El acuse lo inserta esta prueba y la fixtura compartida no lo conoce: sin borrarlo antes, la FK
        // fk_evento_entrega_mensaje_envio_id impide limpiar los envios.
        dslFixtura
                .deleteFrom(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("notificaciones", "evento_entrega_mensaje")))
                .execute();
        fixtura.limpiar();
    }

    private static ConsultarMensajeria consultas() {
        return new ConsultarMensajeria(
                new Datos(dsl),
                new AuditoriaDeMensajeriaRepositorio(),
                new bo.aportaya.notificaciones.infraestructura.PlataformaDeMensajeriaRepositorio());
    }

    private static ContextoSesion auditoria(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    /** Un envio en cola con su notificacion; el destinatario es sintetico y no debe salir en la lista. */
    private UUID envioEnCola(UUID proveedorId, int maxIntentos) {
        String corto = UUID.randomUUID().toString().substring(0, 8);
        UUID evento = fixtura.evento("aporte.recordatorio." + corto, "COBRANZA", true, "ALTA");
        UUID version = fixtura.plantilla("PL-" + corto, evento, "IN_APP", "hola");
        UUID notificacion = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO notificaciones.notificacion
                    (id, usuario_id, evento_id, prioridad, contexto, clave_deduplicacion, estado,
                     programada_para, creada_en, correlation_id)
                VALUES (?, ?, ?, 'ALTA', '{}'::jsonb, ?, 'EN_COLA', now(), now(), gen_random_uuid())
                """,
                notificacion,
                fixtura.usuario(),
                evento,
                "dedupe-" + notificacion);
        UUID envio = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO notificaciones.envio_notificacion
                    (id, notificacion_id, proveedor_id, version_plantilla_id, canal, destinatario,
                     clave_idempotencia, encolado_en, contenido_enviado, estado, orden, intentos,
                     max_intentos, costo, moneda)
                VALUES (?, ?, ?, ?, 'IN_APP', 'app:destino-sintetico', ?, clock_timestamp(), 'hola', 'PENDIENTE',
                        1, 0, ?, 0.00, 'BOB')
                """,
                envio,
                notificacion,
                proveedorId,
                version,
                "idem-" + envio,
                (short) maxIntentos);
        dslFixtura.execute(
                """
                INSERT INTO notificaciones.cola_envio (id, envio_id, particion, disponible_en, intentos)
                VALUES (gen_random_uuid(), ?, 'OBLIGATORIO', now() - interval '1 minute', 0)
                """,
                envio);
        return envio;
    }

    private void despachar() {
        transaccion.execute(t -> loteCU.ejecutar(new EntradaLote("OBLIGATORIO", 10), contexto()));
    }

    private String estadoEnBase(UUID envio) {
        return dslFixtura
                .fetchSingle("SELECT estado FROM notificaciones.envio_notificacion WHERE id = ?", envio)
                .get(0, String.class);
    }

    @Test
    @DisplayName(
            "Dado un envío despachado con acuse de entrega y otro que agotó su único proveedor · Cuando Auditoría abre los intentos de envío · Entonces ve los dos con su estado, intentos y último acuse, sin destinatario ni contenido")
    void enviosConEstadoYAcuse() {
        UUID unico = fixtura.proveedor("UNICO", "IN_APP", 1, new BigDecimal("0.00"), 99, true);
        UUID entregado = envioEnCola(unico, 3);
        despachar();
        dslFixtura.execute(
                """
                INSERT INTO notificaciones.evento_entrega_mensaje
                    (id, envio_id, tipo_evento, fecha_hora_proveedor, payload_crudo, clave_idempotencia)
                VALUES (gen_random_uuid(), ?, 'ENTREGADO', now(), '{"sintetico":true}'::jsonb, ?)
                """,
                entregado,
                "acuse-" + entregado);
        adaptador.proveedoresQueFallan = Set.of("UNICO");
        UUID agotado = envioEnCola(unico, 1);
        despachar();

        var pagina = transaccion.execute(t ->
                consultas().envios(List.of(), Optional.empty(), Optional.empty(), 25, auditoria(fixtura.usuario())));

        assertThat(pagina.items())
                .extracting(ConsultarMensajeria.Envio::envioId)
                .containsSubsequence(agotado, entregado);
        var ok = pagina.items().stream()
                .filter(e -> e.envioId().equals(entregado))
                .findFirst()
                .orElseThrow();
        assertThat(ok.estado()).isEqualTo(estadoEnBase(entregado));
        assertThat(ok.proveedor()).isEqualTo("UNICO");
        assertThat(ok.intentos()).isGreaterThanOrEqualTo(1);
        assertThat(ok.costo().scale()).isEqualTo(4);
        assertThat(ok.moneda()).isEqualTo("BOB");
        assertThat(ok.ultimoAcuse())
                .hasValueSatisfying(a -> assertThat(a.tipoEvento()).isEqualTo("ENTREGADO"));
        var caido = pagina.items().stream()
                .filter(e -> e.envioId().equals(agotado))
                .findFirst()
                .orElseThrow();
        assertThat(caido.estado()).isEqualTo(estadoEnBase(agotado)).isNotEqualTo(ok.estado());
        assertThat(caido.ultimoAcuse()).isEmpty();

        // Con el filtro explicito solo sale el estado pedido.
        var soloOk = transaccion.execute(t -> consultas()
                .envios(List.of(ok.estado()), Optional.empty(), Optional.empty(), 25, auditoria(fixtura.usuario())));
        assertThat(soloOk.items())
                .extracting(ConsultarMensajeria.Envio::envioId)
                .contains(entregado)
                .doesNotContain(agotado);

        // Lo que no se lee no se filtra: el registro no tiene donde llevar el destinatario ni el texto.
        assertThat(Arrays.stream(ConsultarMensajeria.Envio.class.getRecordComponents())
                        .map(RecordComponent::getName))
                .doesNotContain("destinatario", "contenidoEnviado", "idMensajeProveedor", "claveIdempotencia");
    }

    @Test
    @DisplayName(
            "Dados dos envíos despachados · Cuando Auditoría recorre los intentos de a uno · Entonces ve cada uno una sola vez, del más reciente al más antiguo, y la última página cierra sin cursor")
    void recorridoConCursor() {
        UUID unico = fixtura.proveedor("UNICO", "IN_APP", 1, new BigDecimal("0.00"), 99, true);
        UUID primero = envioEnCola(unico, 3);
        UUID segundo = envioEnCola(unico, 3);
        despachar();
        ContextoSesion ctx = auditoria(fixtura.usuario());

        List<UUID> vistos = new ArrayList<>();
        Optional<OffsetDateTime> antesDe = Optional.empty();
        Optional<UUID> antesDeId = Optional.empty();
        ConsultarMensajeria.Pagina<ConsultarMensajeria.Envio> pagina;
        int vueltas = 0;
        do {
            Optional<OffsetDateTime> d = antesDe;
            Optional<UUID> i = antesDeId;
            pagina = transaccion.execute(t -> consultas().envios(List.of(), d, i, 1, ctx));
            pagina.items().forEach(x -> vistos.add(x.envioId()));
            antesDe = pagina.proximoAntesDe();
            antesDeId = pagina.proximoAntesDeId();
            vueltas++;
        } while (antesDeId.isPresent() && vueltas < 10);

        assertThat(vistos).containsExactly(segundo, primero);
        assertThat(pagina.items()).isEmpty();
        assertThat(pagina.proximoAntesDe()).isEmpty();
    }

    @Test
    @DisplayName("rechaza un estado de envío desconocido o un cursor a medias con AP-CU83-06")
    void rechazaFiltroInvalido() {
        ContextoSesion ctx = auditoria(fixtura.usuario());

        assertThatThrownBy(() -> consultas().envios(List.of("PERDIDO"), Optional.empty(), Optional.empty(), 25, ctx))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU83-06"));
        assertThatThrownBy(
                        () -> consultas().envios(List.of(), Optional.empty(), Optional.of(UUID.randomUUID()), 25, ctx))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU83-06"));
    }
}

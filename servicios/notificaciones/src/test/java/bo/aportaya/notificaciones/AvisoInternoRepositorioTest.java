package bo.aportaya.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.notificaciones.aplicacion.GestionarAvisosInternos;
import bo.aportaya.notificaciones.generado.tables.records.AvisoInternoRecord;
import bo.aportaya.notificaciones.infraestructura.AvisoInternoRepositorio;
import bo.aportaya.notificaciones.web.generado.modelo.AvisoInterno;
import bo.aportaya.notificaciones.web.generado.modelo.EntradaAvisoInterno;
import bo.aportaya.notificaciones.web.generado.modelo.PaginaDeAvisosInternos;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La campana del backoffice ({@code notificaciones.aviso_interno}) contra PostgreSQL real: quien recibe
 * cada aviso (la persona o su rol), que se cuenta como no leido, y que marcar leido no pisa la fecha.
 */
class AvisoInternoRepositorioTest extends BaseDeNotificaciones {

    private final AvisoInternoRepositorio repo = new AvisoInternoRepositorio();
    private GestionarAvisosInternos avisos;
    private UUID operador;
    private String rol;
    private ContextoSesion ctx;

    @BeforeEach
    void preparar() {
        avisos = new GestionarAvisosInternos(new Datos(dsl), repo, Reloj.delSistema());
        operador = fixtura.usuario();
        // Un rol propio de cada prueba: los avisos de otras pruebas no se cuelan en los conteos.
        rol = "ROL_" + UUID.randomUUID().toString().substring(0, 8);
        ctx = contextoDe(operador);
    }

    @AfterEach
    void limpiar() {
        dslFixtura.execute(
                "DELETE FROM notificaciones.aviso_interno WHERE usuario_id = ? OR rol_destinatario = ?", operador, rol);
        fixtura.limpiar();
    }

    private AvisoInterno emitir(UUID usuarioId, String rolDestinatario, String titulo) {
        var entrada = new EntradaAvisoInterno(titulo, "Mensaje de prueba", EntradaAvisoInterno.TipoEnum.ALERTA)
                .usuarioId(usuarioId)
                .rolDestinatario(rolDestinatario)
                .enlace("/casos/1");
        return transaccion.execute(s -> avisos.emitir(entrada, ctx));
    }

    private PaginaDeAvisosInternos consultar(UUID usuarioId, Set<String> roles, boolean soloNoLeidos) {
        return transaccion.execute(s -> avisos.consultar(usuarioId, roles, soloNoLeidos, 50, ctx));
    }

    @Test
    @DisplayName("consulta · recibe los avisos dirigidos a la persona y a su rol, con los datos que se emitieron")
    void recibePorPersonaYPorRol() {
        AvisoInterno personal = emitir(operador, null, "Para vos");
        emitir(null, rol, "Para tu rol");

        PaginaDeAvisosInternos pagina = consultar(operador, Set.of(rol), false);

        assertThat(pagina.getTotalNoLeidos()).isEqualTo(2);
        assertThat(pagina.getElementos())
                .extracting(AvisoInterno::getTitulo)
                .containsExactlyInAnyOrder("Para vos", "Para tu rol");
        assertThat(personal.getTipo()).isEqualTo(AvisoInterno.TipoEnum.ALERTA);
        assertThat(personal.getEnlace()).isEqualTo("/casos/1");
        assertThat(personal.getLeidoEn()).isNull();
    }

    @Test
    @DisplayName("rechaza · sin persona ni roles no se ve nada; el aviso de otra persona no cruza")
    void sinDestinatarioNoVeNada() {
        emitir(operador, null, "Solo del operador");
        UUID otro = fixtura.usuario();

        assertThat(consultar(null, Set.of(), false).getElementos()).isEmpty();
        assertThat(consultar(null, null, false).getTotalNoLeidos()).isZero();
        assertThat(consultar(otro, Set.of(), false).getElementos()).isEmpty();
    }

    @Test
    @DisplayName("marcar leido · fija la fecha una sola vez; la segunda lectura no la pisa; solo no leidos lo saca")
    void marcarLeidoEsIdempotente() {
        AvisoInterno aviso = emitir(operador, null, "Leeme");
        emitir(operador, null, "Sigo sin leer");

        AvisoInterno primera =
                transaccion.execute(s -> avisos.marcarLeido(aviso.getId(), ctx)).orElseThrow();
        AvisoInterno segunda =
                transaccion.execute(s -> avisos.marcarLeido(aviso.getId(), ctx)).orElseThrow();

        assertThat(primera.getLeidoEn()).isNotNull();
        assertThat(segunda.getLeidoEn()).isEqualTo(primera.getLeidoEn());
        PaginaDeAvisosInternos noLeidos = consultar(operador, Set.of(), true);
        assertThat(noLeidos.getTotalNoLeidos()).isEqualTo(1);
        assertThat(noLeidos.getElementos()).extracting(AvisoInterno::getTitulo).containsExactly("Sigo sin leer");
    }

    @Test
    @DisplayName("rechaza · marcar leido un aviso que no existe devuelve vacio")
    void marcarLeidoInexistente() {
        Optional<AvisoInterno> ninguno = transaccion.execute(s -> avisos.marcarLeido(UUID.randomUUID(), ctx));
        assertThat(ninguno).isEmpty();
    }

    @Test
    @DisplayName("marcar todos · marca los no leidos de la persona y su rol; repetirlo no modifica nada")
    void marcarTodosLeidos() {
        emitir(operador, null, "Uno");
        emitir(null, rol, "Dos");

        int primera = transaccion
                .execute(s -> avisos.marcarTodosLeidos(operador, List.of(rol), ctx))
                .getModificados();
        int segunda = transaccion
                .execute(s -> avisos.marcarTodosLeidos(operador, List.of(rol), ctx))
                .getModificados();

        assertThat(primera).isEqualTo(2);
        assertThat(segunda).isZero();
        assertThat(consultar(operador, Set.of(rol), false).getTotalNoLeidos()).isZero();
    }

    @Test
    @DisplayName("crear · sin fecha la toma del momento, y buscar por id lo devuelve")
    void crearSinFechaYBuscar() {
        OffsetDateTime antes = OffsetDateTime.now().minusMinutes(1);
        UUID id = transaccion.execute(s -> repo.crear(dsl, operador, null, "Sin fecha", "Mensaje", "INFO", null, null)
                .getId());

        var guardado = transaccion.execute(s -> repo.buscarPorId(dsl, id)).orElseThrow();

        assertThat(guardado.getCreadoEn()).isAfter(antes);
        Optional<AvisoInternoRecord> otro = transaccion.execute(s -> repo.buscarPorId(dsl, UUID.randomUUID()));
        assertThat(otro).isEmpty();
    }
}

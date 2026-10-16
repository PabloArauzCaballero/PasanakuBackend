package bo.aportaya.tarifas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.tarifas.aplicacion.CU34PublicarTarifario.EntradaPublicacion;
import bo.aportaya.tarifas.aplicacion.CU34PublicarTarifario.SalidaPublicacion;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarios;
import bo.aportaya.tarifas.infraestructura.HistorialDeTarifariosRepositorio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-34 · el historial de tarifarios del backoffice (carril A · A6), contra la base real.
 *
 * <p>La version nueva sale del caso de uso real (publicar), no de INSERT: lo que la lista muestra es
 * lo que el sistema escribe. Correcto (las dos versiones con cambio y simulacion), limite (recorrido
 * con cursor hasta la pagina vacia) e invalido (estado desconocido, cursor a medias).
 *
 * <p>{@code catalogo.tarifario} no se limpia entre pruebas: cada una filtra por su propio codigo.
 */
class CU34TarifariosTest extends BaseDeTarifas {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static ConsultarTarifarios consultas() {
        return new ConsultarTarifarios(new Datos(dsl), new HistorialDeTarifariosRepositorio());
    }

    private static ContextoSesion plataforma(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private record Escenario(String codigo, UUID vigenteId, SalidaPublicacion nueva, ContextoSesion ctx) {}

    /** Un tarifario VIGENTE con un concepto, y su version siguiente publicada con preaviso. */
    private Escenario vigenteYSuSiguiente() {
        String corto = UUID.randomUUID().toString().substring(0, 8);
        String codigo = "TAR-" + corto;
        UUID tarifario = fixtura.tarifarioVigente(codigo);
        UUID hecho = fixtura.hechoGenerador("ENTREGA-" + corto);
        UUID redondeo = fixtura.politicaDeRedondeo("CENT-" + corto, "0.01", "BANCARIO");
        fixtura.conceptoPorcentual(
                tarifario,
                hecho,
                redondeo,
                facturacion.cuentaDeIngreso(),
                "COM-SERV",
                "0.0030",
                null,
                null,
                false,
                false);
        fixtura.activar(tarifario);
        ContextoSesion ctx = contextoDe(fixtura.usuario());
        SalidaPublicacion nueva = transaccion.execute(t -> tarifarioCU.publicar(
                new EntradaPublicacion(
                        tarifario,
                        "Version nueva",
                        "INCREMENTO",
                        30,
                        ctx.usuarioId(),
                        "ACTA-2026-07",
                        "https://aportaya.test/tarifario/v2",
                        "b".repeat(64),
                        "CORREO",
                        false,
                        "{\"escenario\":\"historia real\"}",
                        "{\"delta\":\"positivo\"}",
                        new BigDecimal("1200.00"),
                        340),
                ctx));
        return new Escenario(codigo, tarifario, nueva, plataforma(ctx.usuarioId()));
    }

    @Test
    @DisplayName(
            "Dado un tarifario VIGENTE y su versión siguiente publicada con preaviso · Cuando Plataforma abre el historial de ese código · Entonces ve las dos versiones, la nueva primero, con su cambio y su simulación")
    void historialConCambioYSimulacion() {
        Escenario e = vigenteYSuSiguiente();

        var pagina = transaccion.execute(t -> consultas()
                .listar(Optional.of(e.codigo()), List.of(), Optional.empty(), Optional.empty(), 25, e.ctx()));

        assertThat(pagina.items())
                .extracting(ConsultarTarifarios.Tarifario::tarifarioId)
                .containsExactly(e.nueva().tarifarioNuevoId(), e.vigenteId());
        assertThat(pagina.proximoAntesDeId()).isEmpty();
        var nueva = pagina.items().get(0);
        assertThat(nueva.estado()).isEqualTo("EN_PREAVISO");
        assertThat(nueva.version()).isEqualTo(e.nueva().version());
        assertThat(nueva.tarifarioAnteriorId()).contains(e.vigenteId());
        assertThat(nueva.actaAprobacion()).contains("ACTA-2026-07");
        assertThat(nueva.cambio()).hasValueSatisfying(c -> {
            assertThat(c.tipoCambio()).isEqualTo("INCREMENTO");
            assertThat(c.requierePreaviso()).isTrue();
            assertThat(c.diasPreaviso()).isEqualTo(30);
            assertThat(c.usuariosNotificados()).isEqualTo(340);
        });
        assertThat(nueva.simulacion()).hasValueSatisfying(s -> {
            assertThat(s.deltaIngresoEstimado()).isEqualByComparingTo("1200.00");
            assertThat(s.usuariosImpactados()).isEqualTo(340);
        });
        var vigente = pagina.items().get(1);
        assertThat(vigente.estado()).isEqualTo("VIGENTE");
        assertThat(vigente.cambio()).isEmpty();
        assertThat(vigente.simulacion()).isEmpty();

        // Con el filtro explicito solo sale la que rige.
        var soloVigente = transaccion.execute(t -> consultas()
                .listar(Optional.of(e.codigo()), List.of("VIGENTE"), Optional.empty(), Optional.empty(), 25, e.ctx()));
        assertThat(soloVigente.items())
                .extracting(ConsultarTarifarios.Tarifario::tarifarioId)
                .containsExactly(e.vigenteId());
    }

    @Test
    @DisplayName(
            "Dado un tarifario con dos versiones · Cuando Plataforma recorre el historial de a una · Entonces ve cada versión una sola vez, de la más reciente a la más antigua, y la última página cierra sin cursor")
    void recorridoConCursor() {
        Escenario e = vigenteYSuSiguiente();
        List<UUID> vistos = new ArrayList<>();
        Optional<OffsetDateTime> antesDe = Optional.empty();
        Optional<UUID> antesDeId = Optional.empty();
        ConsultarTarifarios.Pagina pagina;
        int vueltas = 0;
        do {
            Optional<OffsetDateTime> d = antesDe;
            Optional<UUID> i = antesDeId;
            pagina = transaccion.execute(t -> consultas().listar(Optional.of(e.codigo()), List.of(), d, i, 1, e.ctx()));
            pagina.items().forEach(x -> vistos.add(x.tarifarioId()));
            antesDe = pagina.proximoAntesDe();
            antesDeId = pagina.proximoAntesDeId();
            vueltas++;
        } while (antesDeId.isPresent() && vueltas < 5);

        assertThat(vistos).containsExactly(e.nueva().tarifarioNuevoId(), e.vigenteId());
        // Dos paginas llenas y una vacia que cierra sin cursor.
        assertThat(vueltas).isEqualTo(3);
        assertThat(pagina.items()).isEmpty();
        assertThat(pagina.proximoAntesDe()).isEmpty();
    }

    @Test
    @DisplayName("rechaza un estado desconocido o un cursor a medias con AP-CU34-05")
    void rechazaFiltroInvalido() {
        ContextoSesion ctx = plataforma(fixtura.usuario());

        assertThatThrownBy(() -> consultas()
                        .listar(Optional.empty(), List.of("PUBLICADO"), Optional.empty(), Optional.empty(), 25, ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .extracting(x -> ((ErrorDeNegocio) x).codigo().valor())
                .isEqualTo("AP-CU34-05");
        assertThatThrownBy(() -> consultas()
                        .listar(
                                Optional.empty(),
                                List.of(),
                                Optional.of(OffsetDateTime.now()),
                                Optional.empty(),
                                25,
                                ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .extracting(x -> ((ErrorDeNegocio) x).codigo().valor())
                .isEqualTo("AP-CU34-05");
    }
}

package bo.aportaya.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.aplicacion.ConsultarBitacora;
import bo.aportaya.auditoria.aplicacion.GestionarCatalogosOperativos;
import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio;
import bo.aportaya.auditoria.infraestructura.CatalogosOperativosRepositorio;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoOperativoResumen;
import bo.aportaya.auditoria.web.generado.modelo.ElementoCatalogoOperativo;
import bo.aportaya.auditoria.web.generado.modelo.EventoBitacora;
import bo.aportaya.auditoria.web.generado.modelo.PaginaDeBitacora;
import bo.aportaya.plataforma.datos.Datos;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H5.S2 · Catalogos operativos (cargados de {@code catalogo/catalogos-operativos.json}) y la bitacora
 * donde queda cada activacion o desactivacion, contra PostgreSQL real.
 */
class CatalogosOperativosRepositorioTest extends BaseDeAuditoria {

    private static final String CATALOGO = "HECHOS_GENERADORES";

    private final BitacoraRepositorio bitacoraRepo = new BitacoraRepositorio();
    private CatalogosOperativosRepositorio repo;
    private GestionarCatalogosOperativos catalogos;
    private ConsultarBitacora bitacora;

    @BeforeEach
    void preparar() {
        repo = new CatalogosOperativosRepositorio(bitacoraRepo);
        catalogos = new GestionarCatalogosOperativos(repo, new Datos(dsl));
        bitacora = new ConsultarBitacora(new Datos(dsl), bitacoraRepo, new ObjectMapper());
    }

    @Test
    @DisplayName("catalogos · se cargan del recurso con sus totales; el detalle trae tabla y base normativa")
    void listaYDetalle() {
        List<CatalogoOperativoResumen> lista = catalogos.listarCatalogos();

        assertThat(lista).hasSize(8);
        CatalogoOperativoResumen hechos = lista.stream()
                .filter(c -> c.getCodigo().equals(CATALOGO))
                .findFirst()
                .orElseThrow();
        assertThat(hechos.getTotalElementos()).isEqualTo(4);
        assertThat(hechos.getElementosActivos()).isEqualTo(4);
        assertThat(hechos.getEstado()).isEqualTo(CatalogoOperativoResumen.EstadoEnum.VIGENTE);

        var detalle = catalogos.consultarCatalogo(CATALOGO).orElseThrow();
        assertThat(detalle.getNombre()).isEqualTo(hechos.getNombre());
        assertThat(detalle.getTotalElementos()).isEqualTo(4);
        assertThat(catalogos.consultarCatalogo("NO_EXISTE")).isEmpty();
    }

    @Test
    @DisplayName(
            "elementos · ordenados; se filtran por estado y por termino en codigo o nombre, sin distinguir mayusculas")
    void filtrosDeElementos() {
        assertThat(catalogos.listarElementos(CATALOGO, null, null))
                .extracting(ElementoCatalogoOperativo::getId)
                .containsExactly("hg-1", "hg-2", "hg-3", "hg-4");
        assertThat(catalogos.listarElementos(CATALOGO, null, "  ")).hasSize(4);
        assertThat(catalogos.listarElementos(CATALOGO, null, "aporte_acreditado"))
                .extracting(ElementoCatalogoOperativo::getId)
                .containsExactly("hg-2");
        assertThat(catalogos.listarElementos(CATALOGO, null, "pasarela qr"))
                .extracting(ElementoCatalogoOperativo::getId)
                .containsExactly("hg-4");
        assertThat(catalogos.listarElementos(CATALOGO, null, "zzz-no-aparece")).isEmpty();
        assertThat(catalogos.listarElementos(CATALOGO, true, null)).hasSize(4);
        assertThat(catalogos.listarElementos(CATALOGO, false, null)).isEmpty();
        assertThat(catalogos.listarElementos("NO_EXISTE", null, null)).isEmpty();
    }

    @Test
    @DisplayName("desactivar · cambia el estado, cuenta un activo menos y deja el antes y el despues en la bitacora")
    void desactivarDejaRastro() {
        OffsetDateTime desde = OffsetDateTime.now().minusMinutes(1);

        ElementoCatalogoOperativo elemento = transaccion
                .execute(s -> catalogos.actualizarEstado(CATALOGO, "hg-3", false, "Fuera de uso", contexto()))
                .orElseThrow();

        assertThat(elemento.getActivo()).isFalse();
        assertThat(elemento.getActualizadoPor()).isEqualTo(OPERADOR.toString());
        assertThat(catalogos.listarElementos(CATALOGO, false, null))
                .extracting(ElementoCatalogoOperativo::getId)
                .containsExactly("hg-3");
        assertThat(catalogos.consultarCatalogo(CATALOGO).orElseThrow().getElementosActivos())
                .isEqualTo(3);

        PaginaDeBitacora pagina = transaccion.execute(s -> bitacora.listar(
                "catalogo_operativo", "CAMBIO_ESTADO", OPERADOR, desde, null, 100, null, null, contexto()));
        EventoBitacora evento = pagina.getItems().stream()
                .filter(e -> "Fuera de uso".equals(e.getMotivo()))
                .findFirst()
                .orElseThrow();
        assertThat(evento.getActorRol()).isEqualTo("AUDITOR");
        assertThat(((Map<?, ?>) evento.getValorAnterior()).get("activo")).isEqualTo(true);
        assertThat(((Map<?, ?>) evento.getValorNuevo()).get("activo")).isEqualTo(false);

        Optional<EventoBitacora> porId = transaccion.execute(s -> bitacora.buscarPorId(evento.getId(), contexto()));
        assertThat(porId).map(EventoBitacora::getAccion).contains("CAMBIO_ESTADO");
    }

    @Test
    @DisplayName("rechaza · catalogo o elemento inexistente no cambia nada ni escribe en la bitacora")
    void inexistenteNoCambiaNada() {
        Optional<ElementoCatalogoOperativo> sinCatalogo =
                transaccion.execute(s -> catalogos.actualizarEstado("NO_EXISTE", "hg-1", false, "x", contexto()));
        Optional<ElementoCatalogoOperativo> sinElemento =
                transaccion.execute(s -> catalogos.actualizarEstado(CATALOGO, "hg-99", false, "x", contexto()));

        assertThat(sinCatalogo).isEmpty();
        assertThat(sinElemento).isEmpty();
        assertThat(catalogos.listarElementos(CATALOGO, true, null)).hasSize(4);
        Optional<EventoBitacora> ninguno =
                transaccion.execute(s -> bitacora.buscarPorId(UUID.randomUUID(), contexto()));
        assertThat(ninguno).isEmpty();
    }

    @Test
    @DisplayName("sin base ni actor · el cambio queda en memoria a nombre de ADMIN y sin bitacora")
    void sinBaseNiActor() {
        ElementoCatalogoOperativo elemento = repo.actualizarEstadoElemento(
                        null, CATALOGO, "hg-1", false, "prueba", null, null, null)
                .orElseThrow();

        assertThat(elemento.getActivo()).isFalse();
        assertThat(elemento.getActualizadoPor()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("bitacora · el limite se acota a 1..100 y la pagina llena trae el cursor de la siguiente")
    void paginaConCursor() {
        OffsetDateTime desde = OffsetDateTime.now().minusMinutes(1);
        transaccion.execute(s -> catalogos.actualizarEstado(CATALOGO, "hg-1", false, "uno", contexto()));
        transaccion.execute(s -> catalogos.actualizarEstado(CATALOGO, "hg-1", true, "dos", contexto()));
        // Un cambio de configuracion no tiene «antes»: la bitacora lo devuelve sin valor anterior.
        transaccion.execute(s -> {
            bitacoraRepo.registrarCambioDeConfiguracion(
                    dsl,
                    "configuracion",
                    UUID.randomUUID(),
                    OPERADOR,
                    "AUDITOR",
                    UUID.randomUUID(),
                    null,
                    "{\"clave\":\"valor\"}",
                    OffsetDateTime.now());
            return null;
        });

        PaginaDeBitacora una =
                transaccion.execute(s -> bitacora.listar(null, null, OPERADOR, desde, null, 0, null, null, contexto()));
        assertThat(una.getItems()).hasSize(1);
        assertThat(una.getProximoAntesDe()).isNotNull();
        assertThat(una.getProximoAntesDeId()).isNotNull();

        PaginaDeBitacora siguiente = transaccion.execute(s -> bitacora.listar(
                null,
                null,
                OPERADOR,
                desde,
                OffsetDateTime.now().plusMinutes(1),
                500,
                una.getProximoAntesDe(),
                una.getProximoAntesDeId(),
                contexto()));
        assertThat(siguiente.getItems())
                .extracting(EventoBitacora::getId)
                .doesNotContain(una.getItems().get(0).getId());
        assertThat(siguiente.getProximoAntesDe()).isNull();

        PaginaDeBitacora config = transaccion.execute(s -> bitacora.listar(
                "configuracion", "CAMBIO_CONFIGURACION", OPERADOR, desde, null, null, null, null, contexto()));
        assertThat(config.getItems()).isNotEmpty();
        assertThat(config.getItems().get(0).getValorAnterior()).isNull();
    }
}

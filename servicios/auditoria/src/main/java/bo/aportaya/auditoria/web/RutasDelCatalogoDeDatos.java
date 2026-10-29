package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla;
import bo.aportaya.auditoria.aplicacion.CU131RevisarColumna;
import bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente;
import bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos;
import bo.aportaya.auditoria.web.generado.IndicadoresApi;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoDeDatos;
import bo.aportaya.auditoria.web.generado.modelo.EntradaFichaDeGobierno;
import bo.aportaya.auditoria.web.generado.modelo.EntradaRevisionDeColumna;
import bo.aportaya.auditoria.web.generado.modelo.EstadoDeRevision;
import bo.aportaya.auditoria.web.generado.modelo.EstadoDeTabla;
import bo.aportaya.auditoria.web.generado.modelo.FichaDeGobierno;
import bo.aportaya.auditoria.web.generado.modelo.PaginaDeTablasDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.ResumenDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.RevisionDeColumna;
import bo.aportaya.auditoria.web.generado.modelo.SensibilidadDeTabla;
import bo.aportaya.auditoria.web.generado.modelo.TablaDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.TipoDeTabla;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.http.ResponseEntity;

/**
 * Catalogo de datos del tablero de sistemas (H7, H2, CU-130 y CU-131): la mitad de {@code IndicadoresApi} que lee y
 * gobierna el catalogo. Traduce y delega, sin logica; vive aparte para que {@link IndicadoresController} no pase de
 * las 300 lineas que el barrido de tamano permite, con el mismo corte en cadena que usa {@code grupos}.
 */
abstract class RutasDelCatalogoDeDatos implements IndicadoresApi {

    private final CatalogoDeDatosVigente catalogoDeDatos;
    private final ObjectMapper mapper;
    private final SesionDeLaPeticion sesion;
    private final CU130GobernarFichaDeTabla cu130;
    private final CU131RevisarColumna cu131;
    private final ConsultarCatalogoDeDatos catalogo;

    protected RutasDelCatalogoDeDatos(
            CatalogoDeDatosVigente catalogoDeDatos,
            ObjectMapper mapper,
            SesionDeLaPeticion sesion,
            CU130GobernarFichaDeTabla cu130,
            CU131RevisarColumna cu131,
            ConsultarCatalogoDeDatos catalogo) {
        this.catalogoDeDatos = catalogoDeDatos;
        this.mapper = mapper;
        this.sesion = sesion;
        this.cu130 = cu130;
        this.cu131 = cu131;
        this.catalogo = catalogo;
    }

    /**
     * Tableros · H7 — el catalogo de datos. El recurso se lee al arrancar y se pasa por el modelo generado del
     * contrato: un catalogo que no cumple {@code CatalogoDeDatos} falla aca y no en el navegador.
     *
     * @deprecated desde 2026-10-21 (contrato): descarga el catalogo entero. Lo reemplazan el resumen, la lista
     *     paginada y la ficha por tabla; sigue respondiendo igual hasta que ningun cliente lo pida.
     */
    @Deprecated(since = "2026-10-21")
    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<CatalogoDeDatos> obtenerCatalogoDeDatos() {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "catalogo-de-datos");
        try {
            CatalogoDeDatos catalogo = mapper.treeToValue(catalogoDeDatos.catalogo(), CatalogoDeDatos.class);
            // CU-130: a cada tabla se le superpone su ficha de gobierno (o la de omision).
            // CU-131: y a cada columna, su revision (si la tiene).
            return ResponseEntity.ok(MapeoDeCatalogo.conRevisiones(
                    MapeoDeCatalogo.conGobierno(catalogo, cu130.consultar(sesion.actual())),
                    cu131.consultar(sesion.actual())));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("El catalogo de datos no cumple el contrato CatalogoDeDatos", e);
        }
    }

    /** Catalogo de datos · H2 — las cifras, los esquemas y los modulos, sin una columna. Traduce y delega. */
    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<ResumenDelCatalogo> obtenerResumenDelCatalogo() {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "catalogo-de-datos/resumen");
        return ResponseEntity.ok(MapeoDeCatalogo.aContrato(catalogo.resumen(sesion.actual())));
    }

    /** Catalogo de datos · H2 — una pagina de la lista, filtrada en el servidor. Traduce y delega. */
    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<PaginaDeTablasDelCatalogo> listarTablasDelCatalogo(
            String q,
            String esquema,
            String modulo,
            TipoDeTabla tipo,
            EstadoDeTabla estado,
            EstadoDeRevision revision,
            SensibilidadDeTabla sensibilidad,
            Integer pagina,
            Integer limite) {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "catalogo-de-datos/tablas");
        var filtro = new ConsultarCatalogoDeDatos.Filtro(
                Optional.ofNullable(q),
                Optional.ofNullable(esquema),
                Optional.ofNullable(modulo),
                Optional.ofNullable(tipo).map(TipoDeTabla::getValue),
                Optional.ofNullable(estado).map(EstadoDeTabla::getValue),
                Optional.ofNullable(revision).map(EstadoDeRevision::getValue),
                Optional.ofNullable(sensibilidad).map(SensibilidadDeTabla::getValue));
        var resultado =
                catalogo.tablas(filtro, pagina == null ? 1 : pagina, limite == null ? 20 : limite, sesion.actual());
        return ResponseEntity.ok(MapeoDeCatalogo.aContrato(resultado));
    }

    /** Catalogo de datos · H2 — la ficha de una tabla con su gobierno y sus revisiones; 404 si no esta. */
    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<TablaDelCatalogo> obtenerTablaDelCatalogo(String esquema, String tabla) {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "catalogo-de-datos/" + esquema + "." + tabla);
        return catalogo.tabla(esquema, tabla, sesion.actual())
                .map(ficha -> ResponseEntity.ok(MapeoDeCatalogo.aContrato(mapper, ficha)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** CU-130 · guarda la ficha de gobierno de una tabla. Traduce y delega. */
    @Override
    @Permiso("CATALOGO_DATOS_EDITAR")
    public ResponseEntity<FichaDeGobierno> guardarFichaDeGobierno(
            String esquema, String tabla, EntradaFichaDeGobierno cuerpo) {
        Traza.marcarCasoDeUso("CU-130", esquema + "." + tabla);
        var guardada = cu130.guardar(esquema, tabla, MapeoDeCatalogo.aDominio(cuerpo), sesion.actual());
        return ResponseEntity.ok(MapeoDeCatalogo.aContrato(guardada));
    }

    /** CU-131 · guarda la revision de una columna. Traduce y delega. */
    @Override
    @Permiso("CATALOGO_DATOS_EDITAR")
    public ResponseEntity<RevisionDeColumna> revisarColumna(
            String esquema, String tabla, String columna, EntradaRevisionDeColumna cuerpo) {
        Traza.marcarCasoDeUso("CU-131", esquema + "." + tabla + "." + columna);
        var guardada = cu131.guardar(esquema, tabla, columna, MapeoDeCatalogo.aDominio(cuerpo), sesion.actual());
        return ResponseEntity.ok(MapeoDeCatalogo.aContrato(guardada));
    }
}

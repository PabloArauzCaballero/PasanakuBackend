package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla;
import bo.aportaya.auditoria.aplicacion.CU131RevisarColumna;
import bo.aportaya.auditoria.aplicacion.CU98PublicarTablero;
import bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente;
import bo.aportaya.auditoria.aplicacion.ConsultarCatalogoDeDatos;
import bo.aportaya.auditoria.aplicacion.ConsultarIncidentesOperativos;
import bo.aportaya.auditoria.aplicacion.EndpointsDelDia;
import bo.aportaya.auditoria.aplicacion.OutboxPorServicio;
import bo.aportaya.auditoria.aplicacion.SaludDeServicios;
import bo.aportaya.auditoria.dominio.FamiliasVisibles;
import bo.aportaya.auditoria.web.generado.IndicadoresApi;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoDeDatos;
import bo.aportaya.auditoria.web.generado.modelo.EntradaFichaDeGobierno;
import bo.aportaya.auditoria.web.generado.modelo.EntradaRevisionDeColumna;
import bo.aportaya.auditoria.web.generado.modelo.EstadoDeIncidenteOperativo;
import bo.aportaya.auditoria.web.generado.modelo.EstadoDeRevision;
import bo.aportaya.auditoria.web.generado.modelo.EstadoDeTabla;
import bo.aportaya.auditoria.web.generado.modelo.FichaDeGobierno;
import bo.aportaya.auditoria.web.generado.modelo.PaginaDeIncidentesOperativos;
import bo.aportaya.auditoria.web.generado.modelo.PaginaDeTablasDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.ResumenDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.RevisionDeColumna;
import bo.aportaya.auditoria.web.generado.modelo.SalidaEndpointsDelDia;
import bo.aportaya.auditoria.web.generado.modelo.SalidaOutboxPorServicio;
import bo.aportaya.auditoria.web.generado.modelo.SalidaSaludDeServicios;
import bo.aportaya.auditoria.web.generado.modelo.SalidaTablero;
import bo.aportaya.auditoria.web.generado.modelo.SensibilidadDeTabla;
import bo.aportaya.auditoria.web.generado.modelo.SeveridadDeIncidenteOperativo;
import bo.aportaya.auditoria.web.generado.modelo.TablaDelCatalogo;
import bo.aportaya.auditoria.web.generado.modelo.TipoDeTabla;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pagina de CU-98: traduce y delega, sin logica.
 *
 * <p>Lo unico que decide es como se serializa un numero, y ahi hay una sola regla que
 * importa: **el valor sale como cadena, con la escala que trae de la base**. Pasarlo a
 * `number` lo convertiria en un doble en el navegador, y el numero del tablero dejaria
 * de ser el numero de la tabla.
 */
@RestController
public class IndicadoresController implements IndicadoresApi {

    private final CU98PublicarTablero cu98;
    private final EndpointsDelDia endpointsDelDia;
    private final CatalogoDeDatosVigente catalogoDeDatos;
    private final SaludDeServicios saludDeServicios;
    private final ObjectMapper mapper;
    private final SesionDeLaPeticion sesion;
    private final CU130GobernarFichaDeTabla cu130;
    private final CU131RevisarColumna cu131;
    private final ConsultarCatalogoDeDatos catalogo;
    private final ConsultarIncidentesOperativos incidentes;
    private final OutboxPorServicio outbox;

    public IndicadoresController(
            CU98PublicarTablero cu98,
            EndpointsDelDia endpointsDelDia,
            CatalogoDeDatosVigente catalogoDeDatos,
            SaludDeServicios saludDeServicios,
            ObjectMapper mapper,
            SesionDeLaPeticion sesion,
            CU130GobernarFichaDeTabla cu130,
            CU131RevisarColumna cu131,
            ConsultarCatalogoDeDatos catalogo,
            ConsultarIncidentesOperativos incidentes,
            OutboxPorServicio outbox) {
        this.cu98 = cu98;
        this.endpointsDelDia = endpointsDelDia;
        this.catalogoDeDatos = catalogoDeDatos;
        this.saludDeServicios = saludDeServicios;
        this.mapper = mapper;
        this.sesion = sesion;
        this.cu130 = cu130;
        this.cu131 = cu131;
        this.catalogo = catalogo;
        this.incidentes = incidentes;
        this.outbox = outbox;
    }

    @Override
    @Permiso("AUDITORIA_LEER")
    @PermisoAlternativo({"TABLERO_FINANCIERO_VER", "TABLERO_COMERCIAL_VER", "TABLERO_SISTEMAS_VER"})
    public ResponseEntity<SalidaTablero> publicarTablero(
            String periodo, String dimension, UUID dimensionId, Integer periodosDeSerie) {

        Traza.marcarCasoDeUso("CU-98", periodo);

        var contexto = sesion.actual();
        var salida = cu98.ejecutar(
                new CU98PublicarTablero.EntradaTablero(
                        periodo,
                        dimension,
                        Optional.ofNullable(dimensionId),
                        periodosDeSerie == null ? 6 : periodosDeSerie),
                contexto,
                // El permiso dejo pasar; QUE familias ve lo decide el servidor, no la pestaña del backoffice.
                FamiliasVisibles.para(contexto.rol(), sesion.permisos()));

        return ResponseEntity.ok(MapeoDeTablero.aContrato(salida));
    }

    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<SalidaEndpointsDelDia> listarEndpointsDelDia(Integer limite) {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "endpoints-del-dia");
        var salida = endpointsDelDia.consultar(limite == null ? 25 : limite);
        return ResponseEntity.ok(MapeoDeSistemas.aContrato(salida));
    }

    /** operacion/estado · la salud de cada servicio, preguntada ahora (o hace menos de 10 s). */
    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<SalidaSaludDeServicios> consultarSaludDeServicios() {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "salud-de-servicios");
        var salida = saludDeServicios.consultar();
        return ResponseEntity.ok(MapeoDeSistemas.aContrato(salida));
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

    /** Sistemas › Outbox: lo que cada relevo publica en Prometheus. Traduce y delega. */
    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<SalidaOutboxPorServicio> consultarOutboxPorServicio() {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "operacion/outbox");
        var salida = outbox.consultar();
        return ResponseEntity.ok(MapeoDeSistemas.aContrato(salida));
    }

    /** Sistemas › Incidentes: una pagina de auditoria.incidente_operativo. Traduce y delega. */
    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<PaginaDeIncidentesOperativos> listarIncidentesOperativos(
            EstadoDeIncidenteOperativo estado,
            SeveridadDeIncidenteOperativo severidad,
            Integer pagina,
            Integer limite) {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "operacion/incidentes");
        var resultado = incidentes.listar(
                Optional.ofNullable(estado).map(EstadoDeIncidenteOperativo::getValue),
                Optional.ofNullable(severidad).map(SeveridadDeIncidenteOperativo::getValue),
                pagina == null ? 1 : pagina,
                limite == null ? 50 : limite,
                sesion.actual());
        return ResponseEntity.ok(MapeoDeSistemas.aContrato(resultado));
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

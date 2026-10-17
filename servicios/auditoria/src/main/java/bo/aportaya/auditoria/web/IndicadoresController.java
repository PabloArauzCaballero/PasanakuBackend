package bo.aportaya.auditoria.web;

import bo.aportaya.auditoria.aplicacion.CU98PublicarTablero;
import bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente;
import bo.aportaya.auditoria.aplicacion.EndpointsDelDia;
import bo.aportaya.auditoria.aplicacion.SaludDeServicios;
import bo.aportaya.auditoria.dominio.FamiliasVisibles;
import bo.aportaya.auditoria.web.generado.IndicadoresApi;
import bo.aportaya.auditoria.web.generado.modelo.CatalogoDeDatos;
import bo.aportaya.auditoria.web.generado.modelo.EndpointDelDia;
import bo.aportaya.auditoria.web.generado.modelo.Indicador;
import bo.aportaya.auditoria.web.generado.modelo.IndicadorSerieInner;
import bo.aportaya.auditoria.web.generado.modelo.SalidaEndpointsDelDia;
import bo.aportaya.auditoria.web.generado.modelo.SalidaSaludDeServicios;
import bo.aportaya.auditoria.web.generado.modelo.SalidaTablero;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
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

    public IndicadoresController(
            CU98PublicarTablero cu98,
            EndpointsDelDia endpointsDelDia,
            CatalogoDeDatosVigente catalogoDeDatos,
            SaludDeServicios saludDeServicios,
            ObjectMapper mapper,
            SesionDeLaPeticion sesion) {
        this.cu98 = cu98;
        this.endpointsDelDia = endpointsDelDia;
        this.catalogoDeDatos = catalogoDeDatos;
        this.saludDeServicios = saludDeServicios;
        this.mapper = mapper;
        this.sesion = sesion;
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

        return ResponseEntity.ok(mapear(salida));
    }

    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<SalidaEndpointsDelDia> listarEndpointsDelDia(Integer limite) {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "endpoints-del-dia");
        var salida = endpointsDelDia.consultar(limite == null ? 25 : limite);
        var cuerpo = new SalidaEndpointsDelDia(
                salida.desde().atOffset(java.time.ZoneOffset.UTC),
                salida.hasta().atOffset(java.time.ZoneOffset.UTC),
                salida.fuenteDisponible(),
                salida.endpoints().stream()
                        .map(e -> {
                            var fila = new EndpointDelDia(
                                    e.servicio(),
                                    e.metodo(),
                                    e.ruta(),
                                    e.solicitudes(),
                                    e.errores4xx(),
                                    e.errores5xx());
                            e.p95Ms().map(BigDecimal::toPlainString).ifPresent(fila::setP95Ms);
                            return fila;
                        })
                        .toList());
        return ResponseEntity.ok(cuerpo);
    }

    /** operacion/estado · la salud de cada servicio, preguntada ahora (o hace menos de 10 s). */
    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<SalidaSaludDeServicios> consultarSaludDeServicios() {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "salud-de-servicios");
        var salida = saludDeServicios.consultar();
        var cuerpo = new SalidaSaludDeServicios(
                salida.consultadoEn().atOffset(java.time.ZoneOffset.UTC),
                salida.servicios().stream()
                        .map(r -> {
                            var fila = new bo.aportaya.auditoria.web.generado.modelo.SaludDeServicio(
                                    r.servicio(),
                                    bo.aportaya.auditoria.web.generado.modelo.SaludDeServicio.EstadoEnum.fromValue(
                                            r.estado().name()));
                            r.latenciaMs().ifPresent(fila::setLatenciaMs);
                            return fila;
                        })
                        .toList());
        return ResponseEntity.ok(cuerpo);
    }

    /**
     * Tableros · H7 — el catalogo de datos. El recurso se lee al arrancar y se pasa por el modelo generado del
     * contrato: un catalogo que no cumple {@code CatalogoDeDatos} falla aca y no en el navegador.
     */
    @Override
    @Permiso("TABLERO_SISTEMAS_VER")
    @PermisoAlternativo({"AUDITORIA_LEER"})
    public ResponseEntity<CatalogoDeDatos> obtenerCatalogoDeDatos() {
        Traza.marcarCasoDeUso("TABLERO-SISTEMAS", "catalogo-de-datos");
        try {
            return ResponseEntity.ok(mapper.treeToValue(catalogoDeDatos.catalogo(), CatalogoDeDatos.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("El catalogo de datos no cumple el contrato CatalogoDeDatos", e);
        }
    }

    private static SalidaTablero mapear(CU98PublicarTablero.SalidaTablero salida) {
        SalidaTablero cuerpo = new SalidaTablero(
                salida.periodo(),
                salida.provisorio(),
                SalidaTablero.DimensionEnum.fromValue(salida.dimension()),
                salida.indicadores().stream().map(IndicadoresController::mapear).toList());
        salida.dimensionId().ifPresent(cuerpo::setDimensionId);
        return cuerpo;
    }

    private static Indicador mapear(CU98PublicarTablero.Indicador indicador) {
        Indicador cuerpo = new Indicador(
                indicador.codigo(),
                indicador.nombre(),
                indicador.unidad(),
                Indicador.FamiliaEnum.fromValue(indicador.familia()),
                indicador.duenoFamilia(),
                indicador.definicionVersion(),
                indicador.suprimidoPorPrivacidad(),
                indicador.calculadoEn());

        indicador.valor().map(IndicadoresController::comoCadena).ifPresent(cuerpo::setValor);
        indicador.casos().ifPresent(cuerpo::setCasos);
        cuerpo.setMinimoCasos(indicador.minimoCasos());
        indicador.meta().map(IndicadoresController::comoCadena).ifPresent(cuerpo::setMeta);
        indicador.cumpleMeta().ifPresent(cuerpo::setCumpleMeta);
        indicador
                .variacionPeriodoAnterior()
                .map(IndicadoresController::comoCadena)
                .ifPresent(cuerpo::setVariacionPeriodoAnterior);
        cuerpo.setSerie(mapearSerie(indicador.serie()));
        return cuerpo;
    }

    private static List<IndicadorSerieInner> mapearSerie(List<CU98PublicarTablero.PuntoDeSerie> serie) {
        return serie.stream()
                .map(punto -> new IndicadorSerieInner(punto.periodo(), comoCadena(punto.valor())))
                .toList();
    }

    /**
     * `toPlainString` y no `toString`: para un valor chico o muy grande, `toString`
     * usa notacion cientifica —`1E+3`— y en un tablero eso no se lee, se adivina.
     */
    private static String comoCadena(BigDecimal valor) {
        return valor.toPlainString();
    }
}

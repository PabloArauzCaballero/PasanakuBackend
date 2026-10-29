package bo.aportaya.organizador.web;

import bo.aportaya.organizador.aplicacion.CU90ConsultarBandeja;
import bo.aportaya.organizador.aplicacion.CU90ResolverHabilitacion;
import bo.aportaya.organizador.aplicacion.ConsultarHabilitacion;
import bo.aportaya.organizador.web.generado.OrganizadoresApi;
import bo.aportaya.organizador.web.generado.modelo.DecisionDeHabilitacion;
import bo.aportaya.organizador.web.generado.modelo.EntradaResolucionDeHabilitacion;
import bo.aportaya.organizador.web.generado.modelo.EntradaRevisionDeHabilitacion;
import bo.aportaya.organizador.web.generado.modelo.ExpedienteDeHabilitacion;
import bo.aportaya.organizador.web.generado.modelo.Habilitacion;
import bo.aportaya.organizador.web.generado.modelo.PaginaDeBandeja;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/**
 * Bandeja del backoffice y resolución humana de habilitaciones (CU-90): traduce y delega.
 *
 * <p>Cada ruta exige su permiso y además el caso de uso vuelve a exigir backoffice con sesión propia:
 * ocultar un botón o tener un permiso suelto no es una barrera.
 */
abstract class RutasDeHabilitacion implements OrganizadoresApi {
    private final CU90ResolverHabilitacion resolutor;
    private final CU90ConsultarBandeja bandeja;
    private final SesionDeLaPeticion sesion;
    private ConsultarHabilitacion habilitaciones;

    protected RutasDeHabilitacion(
            CU90ResolverHabilitacion resolutor, CU90ConsultarBandeja bandeja, SesionDeLaPeticion sesion) {
        this.resolutor = resolutor;
        this.bandeja = bandeja;
        this.sesion = sesion;
    }

    /**
     * Por setter y no por constructor: sumar un parametro arrastra la cadena de constructores hasta el controlador, que
     * esta al limite de largo del archivo (testBarrido, 300 lineas). Spring lo inyecta antes de atender peticiones.
     */
    @org.springframework.beans.factory.annotation.Autowired
    void conConsultaDeHabilitacion(ConsultarHabilitacion habilitaciones) {
        this.habilitaciones = habilitaciones;
    }

    /**
     * Si el organizador esta habilitado, y hasta donde.
     *
     * <p>La pregunta {@code grupos} antes de dejar crear un grupo. Existe para que no
     * tenga que leer este esquema (invariante 11) ni creerle al que pide. Backoffice ({@code ADMIN_PLATAFORMA}) la
     * lee para decidir si habilita (es quien llama a {@code habilitarOrganizador}): sin esto, su pantalla no podia
     * ofrecer la accion.
     */
    @Override
    @Permiso("GRUPO_CREAR")
    @PermisoAlternativo({"ADMIN_PLATAFORMA"})
    public ResponseEntity<Habilitacion> consultarHabilitacion(UUID organizadorId) {
        Traza.marcarCasoDeUso("CU-90", organizadorId.toString());

        var estado = habilitaciones.ejecutar(organizadorId, sesion.actual());

        var respuesta = new Habilitacion();
        respuesta.setHabilitado(estado.habilitado());
        respuesta.setNivel(estado.nivel());
        respuesta.setLimiteDeGrupos(estado.limiteDeGrupos());
        respuesta.setLimiteDeMonto(estado.limiteDeMonto()
                .setScale(2, java.math.RoundingMode.HALF_EVEN)
                .toPlainString());
        respuesta.setGruposActivos(estado.gruposActivos());
        return ResponseEntity.ok(respuesta);
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<PaginaDeBandeja> listarBandejaDeHabilitaciones(
            List<String> estados, Integer limite, OffsetDateTime despuesDeFecha, UUID despuesDeId) {
        int tamano = limite == null ? 25 : limite;
        var filas = bandeja.bandeja(
                estados == null || estados.isEmpty() ? List.of("PENDIENTE", "EN_REVISION") : estados,
                despuesDeFecha,
                despuesDeId,
                tamano,
                sesion.actual());
        var pagina = new PaginaDeBandeja();
        pagina.setItems(
                filas.stream().map(f -> MapeoDeHabilitacion.resumen(f, true)).toList());
        if (!filas.isEmpty() && filas.size() >= Math.min(tamano, 100)) {
            pagina.setProximoFecha(filas.get(filas.size() - 1).fechaSolicitud());
            pagina.setProximoId(filas.get(filas.size() - 1).solicitudId());
        }
        return ResponseEntity.ok(pagina);
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<ExpedienteDeHabilitacion> consultarMiExpedienteDeHabilitacion() {
        return ResponseEntity.ok(expediente(bandeja.expedienteDelUsuario(sesion.actual()), false));
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<ExpedienteDeHabilitacion> consultarExpedienteDeHabilitacion(UUID solicitudId) {
        return ResponseEntity.ok(expediente(bandeja.expediente(solicitudId, sesion.actual()), true));
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<DecisionDeHabilitacion> resolverHabilitacion(
            UUID solicitudId, UUID idempotencyKey, EntradaResolucionDeHabilitacion cuerpo) {
        var resultado = resolutor.resolver(
                new CU90ResolverHabilitacion.Entrada(
                        solicitudId,
                        idempotencyKey,
                        cuerpo.getDecision().getValue(),
                        cuerpo.getMotivo(),
                        cuerpo.getRevisionEsperada(),
                        MapeoDeOrganizador.medidos(cuerpo.getMedidos() == null ? Map.of() : cuerpo.getMedidos())),
                sesion.actual());
        return ResponseEntity.ok(MapeoDeHabilitacion.decision(resultado.decision(), resultado.nueva(), true));
    }

    @Override
    @Permiso("ADMIN_PLATAFORMA")
    public ResponseEntity<DecisionDeHabilitacion> revisarHabilitacion(
            UUID solicitudId, UUID idempotencyKey, EntradaRevisionDeHabilitacion cuerpo) {
        var resultado = resolutor.revisar(
                new CU90ResolverHabilitacion.Entrada(
                        solicitudId,
                        idempotencyKey,
                        cuerpo.getDecision().getValue(),
                        cuerpo.getMotivo(),
                        cuerpo.getRevisionEsperada(),
                        Map.of()),
                sesion.actual());
        return ResponseEntity.ok(MapeoDeHabilitacion.decision(resultado.decision(), resultado.nueva(), true));
    }

    private ExpedienteDeHabilitacion expediente(CU90ConsultarBandeja.Expediente e, boolean backoffice) {
        var salida = new ExpedienteDeHabilitacion();
        salida.setSolicitud(MapeoDeHabilitacion.resumen(e.solicitud(), backoffice));
        salida.setDecisiones(e.decisiones().stream()
                .map(d -> MapeoDeHabilitacion.decision(d, false, backoffice))
                .toList());
        return salida;
    }
}

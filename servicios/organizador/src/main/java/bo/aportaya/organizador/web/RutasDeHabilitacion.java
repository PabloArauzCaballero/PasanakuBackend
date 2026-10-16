package bo.aportaya.organizador.web;

import bo.aportaya.organizador.aplicacion.CU90ConsultarBandeja;
import bo.aportaya.organizador.aplicacion.CU90ResolverHabilitacion;
import bo.aportaya.organizador.web.generado.OrganizadoresApi;
import bo.aportaya.organizador.web.generado.modelo.DecisionDeHabilitacion;
import bo.aportaya.organizador.web.generado.modelo.EntradaResolucionDeHabilitacion;
import bo.aportaya.organizador.web.generado.modelo.EntradaRevisionDeHabilitacion;
import bo.aportaya.organizador.web.generado.modelo.ExpedienteDeHabilitacion;
import bo.aportaya.organizador.web.generado.modelo.PaginaDeBandeja;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
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

    protected RutasDeHabilitacion(
            CU90ResolverHabilitacion resolutor, CU90ConsultarBandeja bandeja, SesionDeLaPeticion sesion) {
        this.resolutor = resolutor;
        this.bandeja = bandeja;
        this.sesion = sesion;
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

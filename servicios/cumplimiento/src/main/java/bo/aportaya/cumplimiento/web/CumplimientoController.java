package bo.aportaya.cumplimiento.web;

import bo.aportaya.cumplimiento.aplicacion.CU44DecidirComoRevisor;
import bo.aportaya.cumplimiento.aplicacion.CU44InvestigarYReportar;
import bo.aportaya.cumplimiento.aplicacion.CU54RegistrarRiesgoOperativo;
import bo.aportaya.cumplimiento.aplicacion.CU55GestionarIncidente;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejasLft;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaCasoLft;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaDecisionCasoLft;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaIncidente;
import bo.aportaya.cumplimiento.web.generado.modelo.EntradaRiesgoOperativo;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeAlertas;
import bo.aportaya.cumplimiento.web.generado.modelo.PaginaDeCasos;
import bo.aportaya.cumplimiento.web.generado.modelo.SalidaCasoLft;
import bo.aportaya.cumplimiento.web.generado.modelo.SalidaDecisionCasoLft;
import bo.aportaya.cumplimiento.web.generado.modelo.SalidaIncidente;
import bo.aportaya.cumplimiento.web.generado.modelo.SalidaRiesgoOperativo;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.Publico;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de las rutas {@code /cumplimiento}: traducen y delegan, sin logica.
 *
 * <p><b>Es una sola clase y no una por caso de uso a proposito.</b> El generador agrupa
 * las operaciones por el primer tramo de la ruta, asi que CU-54 y CU-55 caen en la misma
 * interfaz; dos {@code @RestController} implementandola registrarian dos veces cada
 * mapeo —incluidos los que ninguno de los dos escribe— y Spring no levanta. Partirla por
 * caso de uso exigiria partir las rutas, y las rutas las manda el contrato.
 *
 * <p>La logica esta entera en {@code aplicacion/}: esta clase no decide nada. Si aparece
 * un {@code if} sobre una regla del pasanaku aca, esta mal ubicado.
 */
@RestController
public class CumplimientoController extends ColasDeCumplimientoWeb {

    private final CU54RegistrarRiesgoOperativo cu54;
    private final CU55GestionarIncidente cu55;
    private final SesionDeLaPeticion sesion;
    private final AdhesionController adhesion;
    private final ConsultarBandejasLft bandejasLft;
    private final CU44InvestigarYReportar cu44;
    private final CU44DecidirComoRevisor decisionLft;

    public CumplimientoController(
            CU54RegistrarRiesgoOperativo cu54,
            CU55GestionarIncidente cu55,
            bo.aportaya.cumplimiento.aplicacion.CU02ElevarDiligencia cu02,
            bo.aportaya.cumplimiento.aplicacion.CU03DeclararPep cu03,
            bo.aportaya.cumplimiento.aplicacion.CU05AceptarContrato cu05,
            bo.aportaya.cumplimiento.aplicacion.CU05ConsultarContratosVigentes vigentes,
            ConsultarBandejasLft bandejasLft,
            CU44InvestigarYReportar cu44,
            CU44DecidirComoRevisor decisionLft,
            SesionDeLaPeticion sesion,
            jakarta.servlet.http.HttpServletRequest peticion,
            TableroDeCumplimientoWeb tablero) {
        super(tablero);
        this.cu54 = cu54;
        this.cu55 = cu55;
        this.sesion = sesion;
        this.adhesion = new AdhesionController(cu02, cu03, cu05, vigentes, sesion, peticion);
        this.bandejasLft = bandejasLft;
        this.cu44 = cu44;
        this.decisionLft = decisionLft;
    }

    // CU-44 · paso 4: el oficial agrupa alertas de un titular y asigna analista. Reintento = mismo caso.
    @Override
    @Permiso("CUMPLIMIENTO_CASOS")
    public ResponseEntity<SalidaCasoLft> abrirCasoLft(UUID idempotencyKey, EntradaCasoLft cuerpo) {
        Traza.marcarCasoDeUso("CU-44", "abrir-caso");
        var salida = cu44.abrirCaso(
                new CU44InvestigarYReportar.EntradaCaso(
                        cuerpo.getAlertaIds(), cuerpo.getAnalistaId(), cuerpo.getResumen()),
                sesion.actual());
        var respuesta = new SalidaCasoLft();
        respuesta.setCasoId(salida.casoId());
        respuesta.setEstado(SalidaCasoLft.EstadoEnum.fromValue(salida.estado()));
        respuesta.setPlazoLimite(salida.plazoLimite());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // CU-44 · pasos 5-8: el revisor es la SESION; el analista y las alertas salen del caso guardado.
    @Override
    @Permiso("CUMPLIMIENTO_CASOS")
    public ResponseEntity<SalidaDecisionCasoLft> decidirCasoLft(
            UUID casoId, UUID idempotencyKey, EntradaDecisionCasoLft cuerpo) {
        Traza.marcarCasoDeUso("CU-44", casoId.toString());
        var decision = decisionLft.decidir(
                new CU44DecidirComoRevisor.Entrada(
                        casoId,
                        cuerpo.getDecision().getValue(),
                        cuerpo.getConclusion(),
                        cuerpo.getReporteSospechosoId()),
                sesion.actual());
        var respuesta = new SalidaDecisionCasoLft();
        respuesta.setCasoId(decision.casoId());
        respuesta.setDecision(SalidaDecisionCasoLft.DecisionEnum.fromValue(decision.decision()));
        respuesta.setRevisadoPor(decision.revisadoPor());
        respuesta.setAlertasCerradas(decision.alertasCerradas());
        respuesta.setFueraDePlazo(decision.fueraDePlazo());
        respuesta.setReporteSospechosoId(decision.reporteSospechosoId());
        return ResponseEntity.ok(respuesta);
    }

    // CU-44 · la cola del analista: sin persona, monto ni detalle (deber de reserva, regla 90.2.7).
    @Override
    @Permiso("CUMPLIMIENTO_ALERTAS")
    public ResponseEntity<PaginaDeAlertas> listarBandejaDeAlertas(
            List<String> estados,
            List<String> severidades,
            Integer limite,
            OffsetDateTime despuesDeFecha,
            UUID despuesDeId) {
        Traza.marcarCasoDeUso("CU-44", "alertas");
        return ResponseEntity.ok(MapeoDeLft.alertas(bandejasLft.alertas(
                estados,
                severidades,
                Optional.ofNullable(despuesDeFecha),
                Optional.ofNullable(despuesDeId),
                limite,
                sesion.actual())));
    }

    // CU-44 · la cola de casos, el que vence antes primero: sin persona, resumen ni hallazgos.
    @Override
    @Permiso("CUMPLIMIENTO_CASOS")
    public ResponseEntity<PaginaDeCasos> listarBandejaDeCasos(
            List<String> estados, Integer limite, OffsetDateTime despuesDePlazo, UUID despuesDeId) {
        Traza.marcarCasoDeUso("CU-44", "casos");
        return ResponseEntity.ok(MapeoDeLft.casos(bandejasLft.casos(
                estados,
                Optional.ofNullable(despuesDePlazo),
                Optional.ofNullable(despuesDeId),
                limite,
                sesion.actual())));
    }

    @Override
    @Publico("CU-05: se consulta durante el alta, cuando todavia no hay sesion; devuelve"
            + " lo mismo que ya esta publicado en el sitio web y ningun dato de nadie")
    public ResponseEntity<List<bo.aportaya.cumplimiento.web.generado.modelo.ContratoVigente>>
            listarContratosVigentes() {
        return adhesion.listarContratosVigentes();
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<bo.aportaya.cumplimiento.web.generado.modelo.SalidaAceptacion> aceptarContrato(
            UUID idempotencyKey,
            UUID contratoId,
            bo.aportaya.cumplimiento.web.generado.modelo.EntradaAceptacion cuerpo) {
        return adhesion.aceptarContrato(contratoId, cuerpo);
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<bo.aportaya.cumplimiento.web.generado.modelo.SalidaPep> declararPep(
            UUID usuarioId, bo.aportaya.cumplimiento.web.generado.modelo.EntradaPep cuerpo) {
        return adhesion.declararPep(usuarioId, cuerpo);
    }

    @Override
    @Permiso("ANALISTA_CUMPLIMIENTO")
    public ResponseEntity<bo.aportaya.cumplimiento.web.generado.modelo.SalidaDiligencia> elevarDiligencia(
            UUID idempotencyKey,
            UUID usuarioId,
            bo.aportaya.cumplimiento.web.generado.modelo.EntradaDiligencia cuerpo) {
        return adhesion.elevarDiligencia(idempotencyKey, usuarioId, cuerpo);
    }

    /**
     * Devuelve {@code 201} incluso sin plan de accion: el recurso que se creo es el
     * evento, y perder el registro porque todavia no hay responsable asignado seria
     * perder justo lo que la norma pide conservar.
     */
    @Override
    @Permiso("RESPONSABLE_RIESGOS")
    public ResponseEntity<SalidaRiesgoOperativo> registrarRiesgoOperativo(
            UUID idempotencyKey, EntradaRiesgoOperativo cuerpo) {

        Traza.marcarCasoDeUso("CU-54", cuerpo.getLineaNegocio());

        var salida = cu54.ejecutar(
                new CU54RegistrarRiesgoOperativo.EntradaEvento(
                        cuerpo.getCategoriaEvento().getValue(),
                        cuerpo.getFactorRiesgo().getValue(),
                        cuerpo.getLineaNegocio(),
                        cuerpo.getDescripcion(),
                        cuerpo.getFechaOcurrencia(),
                        cuerpo.getFechaDeteccion(),
                        cuerpo.getPerdidaBruta(),
                        Optional.ofNullable(cuerpo.getRecuperacion()),
                        cuerpo.getMoneda().getValue(),
                        Optional.ofNullable(cuerpo.getResponsableId()),
                        Optional.ofNullable(cuerpo.getAccionComprometida()),
                        Optional.ofNullable(cuerpo.getFechaCompromiso())),
                sesion.actual());

        SalidaRiesgoOperativo respuesta = new SalidaRiesgoOperativo(
                salida.eventoId(),
                salida.codigo(),
                salida.perdidaNeta(),
                SalidaRiesgoOperativo.MonedaEnum.fromValue(salida.moneda()));
        salida.planAccionId().ifPresent(respuesta::setPlanAccionId);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Override
    @Permiso("RESPONSABLE_SEGURIDAD")
    public ResponseEntity<SalidaIncidente> registrarIncidente(UUID idempotencyKey, EntradaIncidente cuerpo) {
        Traza.marcarCasoDeUso("CU-55", cuerpo.getTipo().getValue());

        var salida = cu55.registrar(
                new CU55GestionarIncidente.EntradaIncidente(
                        cuerpo.getTipo().getValue(),
                        cuerpo.getSeveridad().getValue(),
                        Optional.ofNullable(cuerpo.getActivoInformacionId()),
                        Boolean.TRUE.equals(cuerpo.getDatosPersonalesAfectados()),
                        cuerpo.getUsuariosAfectados(),
                        cuerpo.getDetectadoEn(),
                        Optional.ofNullable(cuerpo.getEventoRiesgoId())),
                sesion.actual());

        SalidaIncidente respuesta = new SalidaIncidente(
                salida.incidenteId(),
                salida.codigo(),
                salida.plazoReporte(),
                salida.requiereNotificarTitulares(),
                salida.registradoEn());
        salida.plazoNotificacion().ifPresent(respuesta::setPlazoNotificacion);
        salida.contratoTerceroId().ifPresent(respuesta::setContratoTerceroId);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Reportar y notificar son <b>dos llamadas y no dos banderas de una</b>: son dos
     * momentos distintos del expediente, cada uno con su propia hora. Es exactamente lo
     * que una inspeccion pide ver, y lo que una sola operacion con parametros perderia.
     */
    @Override
    @Permiso("RESPONSABLE_SEGURIDAD")
    public ResponseEntity<Void> reportarIncidente(UUID incidenteId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-55", String.valueOf(incidenteId));
        cu55.reportarAlOrganismo(incidenteId, sesion.actual());
        return ResponseEntity.noContent().build();
    }

    @Override
    @Permiso("RESPONSABLE_SEGURIDAD")
    public ResponseEntity<Void> notificarTitulares(UUID incidenteId, UUID idempotencyKey) {
        Traza.marcarCasoDeUso("CU-55", String.valueOf(incidenteId));
        cu55.notificarTitulares(incidenteId, sesion.actual());
        return ResponseEntity.noContent().build();
    }
}

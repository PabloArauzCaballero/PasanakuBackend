package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU134ConsultarParticipante;
import bo.aportaya.identidad.aplicacion.CU134GestionarMedida;
import bo.aportaya.identidad.dominio.RegistroDeMedida;
import bo.aportaya.identidad.web.generado.IdentidadApi;
import bo.aportaya.identidad.web.generado.modelo.DatoRevelado;
import bo.aportaya.identidad.web.generado.modelo.EntradaBusquedaParticipantes;
import bo.aportaya.identidad.web.generado.modelo.EntradaDecisionDeMedida;
import bo.aportaya.identidad.web.generado.modelo.EntradaDescargoDeMedida;
import bo.aportaya.identidad.web.generado.modelo.EntradaJustificacionDeLectura;
import bo.aportaya.identidad.web.generado.modelo.EntradaMedidaDeCuenta;
import bo.aportaya.identidad.web.generado.modelo.EntradaNotificacionDeMedida;
import bo.aportaya.identidad.web.generado.modelo.EntradaResolucionDeApelacion;
import bo.aportaya.identidad.web.generado.modelo.EntradaRevelacion;
import bo.aportaya.identidad.web.generado.modelo.EvidenciaDeMedida;
import bo.aportaya.identidad.web.generado.modelo.ExpedienteDeMedida;
import bo.aportaya.identidad.web.generado.modelo.FichaDeParticipante;
import bo.aportaya.identidad.web.generado.modelo.HitoDeMedida;
import bo.aportaya.identidad.web.generado.modelo.MedidaVigente;
import bo.aportaya.identidad.web.generado.modelo.PaginaDeParticipantes;
import bo.aportaya.identidad.web.generado.modelo.ParticipanteResumen;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.PermitidoEnRestriccion;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * CU-134 · traduce y delega, sin logica. El permiso de cada operacion lo fija el contrato aprobado
 * (D-5); descargo y apelacion los presenta el titular, que no necesita permiso de operador, y lo
 * verifica el organismo. La IP viaja solo al rastro de lectura (R-SEG-02), nunca a un log.
 *
 * <p>Vive bajo {@code /identidad}, asi que es parte de {@link IdentidadApi}: la atiende
 * {@link VerificacionesController}, el unico controlador concreto de esa interfaz (dos beans que la
 * implementaran registrarian las mismas rutas dos veces).
 *
 * <p>D-6: consultar el expediente, el descargo y la apelacion son de la lista cerrada que una cuenta suspendida
 * alcanza con su sesion restringida ({@link PermitidoEnRestriccion}).
 */
abstract class RutasDeParticipantes implements IdentidadApi {

    private final CU134ConsultarParticipante consulta;
    private final CU134GestionarMedida medidas;
    private final SesionDeLaPeticion sesion;
    private final HttpServletRequest peticion;

    protected RutasDeParticipantes(
            CU134ConsultarParticipante consulta,
            CU134GestionarMedida medidas,
            SesionDeLaPeticion sesion,
            HttpServletRequest peticion) {
        this.consulta = consulta;
        this.medidas = medidas;
        this.sesion = sesion;
        this.peticion = peticion;
    }

    @Override
    @Permiso("PARTICIPANTES_LEER")
    public ResponseEntity<PaginaDeParticipantes> buscarParticipantes(EntradaBusquedaParticipantes e) {
        ContextoSesion ctx = sesion.actual();
        var filas = consulta.buscar(
                new CU134ConsultarParticipante.Busqueda(
                        Optional.ofNullable(e.getCodigoPublico()),
                        Optional.ofNullable(e.getTelefonoE164()),
                        Optional.ofNullable(e.getEstado()).map(Object::toString),
                        e.getJustificacion(),
                        e.getTicketSoporteId(),
                        Optional.ofNullable(e.getAntesDe()),
                        Optional.ofNullable(e.getAntesDeId()),
                        e.getLimite() == null ? 20 : e.getLimite()),
                ip(),
                ctx);
        return ResponseEntity.ok(new PaginaDeParticipantes()
                .filas(filas.stream()
                        .map(r -> new ParticipanteResumen()
                                .usuarioId(r.fila().usuarioId())
                                .codigoPublico(r.fila().codigoPublico())
                                .nombreAbreviado(r.nombreAbreviado())
                                .estado(r.fila().estado())
                                .nivelKyc(r.fila().nivelKyc())
                                .fechaRegistro(r.fila().fechaRegistro())
                                .tieneRestriccion(r.fila().tieneRestriccion())
                                .tieneBloqueo(r.fila().tieneBloqueo())
                                .tieneMedidaEnCurso(r.fila().tieneMedidaEnCurso()))
                        .toList()));
    }

    @Override
    @Permiso("PARTICIPANTES_LEER")
    public ResponseEntity<FichaDeParticipante> abrirFichaDeParticipante(
            UUID usuarioId, EntradaJustificacionDeLectura e) {
        ContextoSesion ctx = sesion.actual();
        return consulta.ficha(usuarioId, e.getJustificacion(), e.getTicketSoporteId(), ip(), ctx)
                .map(f -> ResponseEntity.ok(new FichaDeParticipante()
                        .usuarioId(f.resumen().usuarioId())
                        .codigoPublico(f.resumen().codigoPublico())
                        .nombreCompleto(f.nombreCompleto())
                        .estado(f.resumen().estado())
                        .nivelKyc(f.resumen().nivelKyc())
                        .fechaRegistro(f.resumen().fechaRegistro())
                        .ultimoAcceso(f.ultimoAcceso())
                        .telefonoEnmascarado(f.telefonoEnmascarado())
                        .correoEnmascarado(f.correoEnmascarado())
                        .medidas(f.medidas().stream()
                                .map(m -> resumen(m, f.resumen().estado()))
                                .toList())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Permiso("DATOS_SENSIBLES_LEER")
    public ResponseEntity<DatoRevelado> revelarDatoDeParticipante(UUID usuarioId, EntradaRevelacion e) {
        ContextoSesion ctx = sesion.actual();
        String campo = e.getCampo().getValue();
        return consulta.revelar(usuarioId, campo, e.getJustificacion(), e.getEvidenciaMfa(), ip(), ctx)
                .map(valor -> ResponseEntity.ok(new DatoRevelado()
                        .campo(DatoRevelado.CampoEnum.fromValue(campo))
                        .valor(valor)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Permiso("PARTICIPANTES_MEDIDA_PROPONER")
    public ResponseEntity<ExpedienteDeMedida> proponerMedidaDeCuenta(
            UUID idempotencyKey, UUID usuarioId, EntradaMedidaDeCuenta e) {
        ContextoSesion ctx = sesion.actual();
        var vista = medidas.proponer(
                usuarioId,
                new CU134GestionarMedida.Propuesta(
                        e.getCausal().getValue(),
                        e.getMedida().getValue(),
                        Boolean.TRUE.equals(e.getCautelar()),
                        e.getMotivo(),
                        evidencias(e.getEvidencias()),
                        Optional.ofNullable(e.getVigenteHasta())),
                idempotencyKey.toString(),
                ctx);
        return ResponseEntity.status(HttpStatus.CREATED).body(expediente(vista));
    }

    @Override
    @Permiso("PARTICIPANTE")
    @PermisoAlternativo({"PARTICIPANTES_LEER"})
    @PermitidoEnRestriccion
    public ResponseEntity<ExpedienteDeMedida> consultarExpedienteDeMedida(UUID expedienteId) {
        ContextoSesion ctx = sesion.actual();
        return medidas.consultar(expedienteId, ctx)
                .map(v -> ResponseEntity.ok(expediente(v)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Permiso("PARTICIPANTES_MEDIDA_PROPONER")
    public ResponseEntity<ExpedienteDeMedida> registrarNotificacionDeMedida(
            UUID idempotencyKey, UUID expedienteId, EntradaNotificacionDeMedida e) {
        return ResponseEntity.ok(expediente(medidas.notificar(expedienteId, e.getEntregadaEn(), sesion.actual())));
    }

    @Override
    @Permiso("PARTICIPANTE")
    @PermitidoEnRestriccion
    public ResponseEntity<ExpedienteDeMedida> presentarDescargoDeMedida(
            UUID idempotencyKey, UUID expedienteId, EntradaDescargoDeMedida e) {
        return ResponseEntity.ok(expediente(
                medidas.descargo(expedienteId, e.getArgumento(), evidencias(e.getEvidencias()), sesion.actual())));
    }

    @Override
    @Permiso("PARTICIPANTES_MEDIDA_DECIDIR")
    public ResponseEntity<ExpedienteDeMedida> decidirMedidaDeCuenta(
            UUID idempotencyKey, UUID expedienteId, EntradaDecisionDeMedida e) {
        return ResponseEntity.ok(expediente(medidas.decidir(
                expedienteId, e.getResultado().getValue(), e.getFundamento(), e.getEvidenciaMfa(), sesion.actual())));
    }

    @Override
    @Permiso("PARTICIPANTE")
    @PermitidoEnRestriccion
    public ResponseEntity<ExpedienteDeMedida> apelarMedidaDeCuenta(
            UUID idempotencyKey, UUID expedienteId, EntradaDescargoDeMedida e) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expediente(medidas.apelar(expedienteId, e.getArgumento(), sesion.actual())));
    }

    @Override
    @Permiso("PARTICIPANTES_APELACION_RESOLVER")
    public ResponseEntity<ExpedienteDeMedida> resolverApelacionDeMedida(
            UUID idempotencyKey, UUID expedienteId, EntradaResolucionDeApelacion e) {
        return ResponseEntity.ok(expediente(medidas.resolver(
                expedienteId, e.getResultado().getValue(), e.getResolucion(), e.getEvidenciaMfa(), sesion.actual())));
    }

    @Override
    @Permiso("PARTICIPANTES_MEDIDA_VIGENTE_LEER")
    public ResponseEntity<MedidaVigente> consultarMedidaVigente(UUID usuarioId) {
        Optional<UUID> expediente = consulta.medidaQueRetiene(usuarioId, sesion.actual());
        return ResponseEntity.ok(new MedidaVigente()
                .usuarioId(usuarioId)
                .retenerEntrega(expediente.isPresent())
                .expedienteId(expediente.orElse(null)));
    }

    private String ip() {
        return peticion.getRemoteAddr();
    }

    private static List<CU134GestionarMedida.Evidencia> evidencias(List<EvidenciaDeMedida> lista) {
        return lista == null
                ? List.of()
                : lista.stream()
                        .map(ev -> new CU134GestionarMedida.Evidencia(ev.getDescripcion(), ev.getReferenciaId()))
                        .toList();
    }

    private static ExpedienteDeMedida expediente(CU134GestionarMedida.Vista v) {
        ExpedienteDeMedida salida = resumen(v.expediente(), v.estadoCuenta());
        salida.lineaDeTiempo(v.lineaDeTiempo().stream()
                .map(h -> new HitoDeMedida()
                        .estadoAnterior(h.estadoAnterior())
                        .estado(h.estado())
                        .motivo(h.motivo())
                        .ocurridoEn(h.ocurridoEn()))
                .toList());
        v.apelacion().ifPresent(a -> {
            salida.apelacionEstado(ExpedienteDeMedida.ApelacionEstadoEnum.fromValue(a.estado()));
            salida.fechaLimiteResolucionApelacion(a.fechaLimiteResolucion());
        });
        return salida;
    }

    private static ExpedienteDeMedida resumen(RegistroDeMedida.Expediente e, String estadoCuenta) {
        ExpedienteDeMedida s = new ExpedienteDeMedida()
                .expedienteId(e.id())
                .usuarioId(e.usuarioId())
                .causal(e.causal())
                .medida(e.medida())
                .cautelar(e.cautelar())
                .motivo(e.motivo())
                .estado(ExpedienteDeMedida.EstadoEnum.fromValue(e.estado()))
                .estadoCuenta(estadoCuenta)
                .propuestaEn(e.propuestaEn())
                .notificadaEn(e.notificadaEn())
                .fechaLimiteDescargo(e.fechaLimiteDescargo())
                .fundamentoDecision(e.fundamentoDecision())
                .fechaLimiteApelacion(e.fechaLimiteApelacion())
                .vigenteHasta(e.vigenteHasta())
                .prescribeEn(e.prescribeEn())
                .lineaDeTiempo(List.of());
        if (e.resultadoDecision() != null) {
            s.resultadoDecision(ExpedienteDeMedida.ResultadoDecisionEnum.fromValue(e.resultadoDecision()));
        }
        return s;
    }
}

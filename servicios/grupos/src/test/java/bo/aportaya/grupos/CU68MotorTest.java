package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso.Entrada;
import bo.aportaya.grupos.aplicacion.CU68Postular.EntradaPostulacion;
import bo.aportaya.grupos.aplicacion.CU68Postular.SalidaPostulacion;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.infraestructura.AdmisionRepositorio;
import bo.aportaya.grupos.infraestructura.CreacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * H6: el motor recomienda, explica y se puede reproducir; no decide. Sin dato no es un valor malo, y el apartamiento
 * de una persona respecto de la lectura del motor queda registrado junto con la versión que la dio.
 */
class CU68MotorTest extends BaseDeCU68 {
    private static final AtomicInteger ORDEN = new AtomicInteger();

    private CU68AceptarIngreso admision;
    private UUID grupo;
    private ContextoSesion administrador;
    private ContextoSesion backoffice;

    @BeforeEach
    void preparar() {
        admision = new CU68AceptarIngreso(
                new Datos(dsl), new AdmisionRepositorio(), new Outbox("grupos"), Reloj.delSistema());
        grupo = grupoAbierto();
        administrador = administradorDe(grupo);
        backoffice = sesion(fixtura.usuario(), "BACKOFFICE");
    }

    private ContextoSesion sesion(UUID usuario, String rol) {
        return ContextoSesion.de(usuario, rol, new Traza(UUID.randomUUID().toString()));
    }

    private UUID grupoAbierto() {
        UUID g = grupoConCupoLibre();
        dslFixtura.execute("UPDATE grupos.grupo SET estado='ABIERTO_A_INSCRIPCION' WHERE id=?", g);
        new CreacionRepositorio().configurar(dslFixtura, g, false);
        return g;
    }

    private ContextoSesion administradorDe(UUID g) {
        var participante = dslFixtura.fetchOne(
                "SELECT id, usuario_id FROM grupos.participante WHERE grupo_id=? ORDER BY id LIMIT 1", g);
        dslFixtura.execute(
                "UPDATE grupos.participante SET es_organizador=true WHERE id=?", participante.get("id", UUID.class));
        return sesion(participante.get("usuario_id", UUID.class), "ORGANIZADOR");
    }

    /** Un criterio nuevo, más reciente que cualquiera de las demás clases de prueba. Devuelve su id (la versión). */
    private UUID criterio(int reputacionMinima) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO grupos.criterio_emparejamiento
                    (id, peso_reputacion, peso_monto, peso_geografia, peso_historial_comun,
                     reputacion_minima, max_morosos_por_grupo, vigente_desde)
                VALUES (?, 0.40, 0.30, 0.20, 0.10, ?, 2, now() - interval '10 minutes' + make_interval(secs => ?))
                """,
                id,
                reputacionMinima,
                ORDEN.incrementAndGet());
        return id;
    }

    private SalidaPostulacion postularCon(UUID g, int reputacion, int morosos, boolean sinRep, boolean sinConc) {
        return transaccion.execute(e -> postularCU.postular(
                new EntradaPostulacion(
                        g,
                        (short) 1,
                        "quiero entrar",
                        false,
                        BigDecimal.ZERO,
                        true,
                        reputacion,
                        morosos,
                        new BigDecimal("0.80"),
                        new BigDecimal("0.90"),
                        new BigDecimal("0.70"),
                        new BigDecimal("0.10"),
                        sinRep,
                        sinConc),
                contexto()));
    }

    private String evidencia(UUID solicitud, String campo) {
        return (String) dsl.fetchOne(
                        "SELECT payload->>? FROM grupos.evento_dominio WHERE agregado_id=? AND tipo='grupos.ingreso_solicitado'",
                        campo,
                        solicitud)
                .get(0);
    }

    private String estadoDe(UUID solicitud) {
        return (String) dsl.fetchOne("SELECT estado FROM grupos.solicitud_ingreso WHERE id=?", solicitud)
                .get(0);
    }

    private int miembros(UUID g) {
        return contar(
                "SELECT count(*)::int FROM grupos.participante WHERE grupo_id=? AND usuario_id=?",
                g,
                contexto().usuarioId());
    }

    @Test
    void sinHistorialDeReputacionNoEsRechazoEsRevisionHumana() {
        criterio(50);
        var salida = postularCon(grupo, 0, 0, true, false);

        assertThat(salida.motivos()).anyMatch(m -> m.startsWith("SIN_DATOS_REPUTACION"));
        assertThat(salida.motivos()).noneMatch(m -> m.startsWith("REVISAR_REPUTACION"));
        assertThat(estadoDe(salida.solicitudId())).isEqualTo("PENDIENTE");
        assertThat(miembros(grupo)).isZero();
        assertThat(evidencia(salida.solicitudId(), "recomendacion")).isEqualTo("REVISION_HUMANA");
        assertThat(evidencia(salida.solicitudId(), "decisionAutomatica")).isEqualTo("false");
    }

    @Test
    void sinMedicionDeMoraDelGrupoNoEsRechazoEsRevisionHumana() {
        criterio(0);
        var salida = postularCon(grupo, 80, HechosDeOtrosServicios.SIN_DATO_DE_MOROSOS, false, true);

        assertThat(salida.motivos()).anyMatch(m -> m.startsWith("SIN_DATOS_CONCENTRACION"));
        assertThat(estadoDe(salida.solicitudId())).isEqualTo("PENDIENTE");
        assertThat(evidencia(salida.solicitudId(), "recomendacion")).isEqualTo("REVISION_HUMANA");
        assertThat(dsl.fetchOne(
                                "SELECT payload->'entradas'->>'morososDelGrupo' FROM grupos.evento_dominio WHERE agregado_id=? AND tipo='grupos.ingreso_solicitado'",
                                salida.solicitudId())
                        .get(0))
                .isEqualTo("SIN_DATO");
    }

    @Test
    void reputacionBajaSeMarcaParaRevisarYNuncaRechazaSola() {
        criterio(50);
        var salida = postularCon(grupo, 10, 0, false, false);

        assertThat(salida.motivos()).anyMatch(m -> m.startsWith("REVISAR_REPUTACION"));
        assertThat(estadoDe(salida.solicitudId())).isEqualTo("PENDIENTE");
        assertThat(evidencia(salida.solicitudId(), "recomendacion")).isEqualTo("REVISION_HUMANA");
    }

    @Test
    void conDatosCompletosYSuficientesElMotorRecomiendaAceptarSinDecidir() {
        criterio(50);
        var salida = postularCon(grupo, 90, 0, false, false);

        assertThat(salida.motivos()).noneMatch(m -> m.startsWith("REVISAR_") || m.startsWith("SIN_DATOS_"));
        assertThat(evidencia(salida.solicitudId(), "recomendacion")).isEqualTo("ACEPTAR");
        assertThat(estadoDe(salida.solicitudId())).isEqualTo("PENDIENTE");
        assertThat(miembros(grupo)).isZero();
    }

    @Test
    void laVersionDelMotorQuedaConLaDecisionYMismosInsumosDanMismoResultado() {
        UUID v1 = criterio(50);
        var primera = postularCon(grupo, 90, 0, false, false);
        UUID otroGrupo = grupoAbierto();
        var igual = postularCon(otroGrupo, 90, 0, false, false);
        assertThat(evidencia(primera.solicitudId(), "versionMotor")).isEqualTo(v1.toString());
        assertThat(igual.puntaje()).isEqualByComparingTo(primera.puntaje());
        assertThat(igual.motivos()).isEqualTo(primera.motivos());

        UUID v2 = criterio(95);
        UUID grupoNuevo = grupoAbierto();
        var nueva = postularCon(grupoNuevo, 90, 0, false, false);
        assertThat(evidencia(nueva.solicitudId(), "versionMotor")).isEqualTo(v2.toString());
        assertThat(evidencia(nueva.solicitudId(), "recomendacion")).isEqualTo("REVISION_HUMANA");
        // La postulación anterior conserva la versión con la que se evaluó: no se re-lee con reglas nuevas.
        assertThat(evidencia(primera.solicitudId(), "versionMotor")).isEqualTo(v1.toString());
        assertThat(evidencia(primera.solicitudId(), "recomendacion")).isEqualTo("ACEPTAR");
    }

    private Entrada entrada(UUID solicitud, String decision, int revision, UUID propuesta) {
        return new Entrada(
                solicitud, UUID.randomUUID(), decision, "Valoración humana documentada", revision, propuesta);
    }

    @Test
    void aceptarPeseASinDatosRegistraApartamientoConLaVersionYLaRecomendacion() {
        UUID version = criterio(50);
        var salida = postularCon(grupo, 0, 0, true, false);
        var propuesta = transaccion.execute(
                tx -> admision.proponer(entrada(salida.solicitudId(), "ACEPTAR", 0, null), administrador));
        var decision = transaccion.execute(
                tx -> admision.resolver(entrada(salida.solicitudId(), "ACEPTAR", 1, propuesta.id()), backoffice));

        assertThat(decision.apartamiento()).isTrue();
        assertThat(decision.recomendacionAlgoritmo()).isEqualTo("REVISION_HUMANA");
        assertThat(decision.versionMotor()).isEqualTo(version.toString());
        assertThat(decision.actorId()).isEqualTo(backoffice.usuarioId());
        assertThat(decision.evidenciaAlgoritmo()).contains("SIN_DATOS_REPUTACION");
        // Las dos lecturas conviven en el historial: la propuesta del administrador y la resolución del backoffice.
        java.util.List<bo.aportaya.grupos.dominio.DecisionDeIngreso> historial =
                transaccion.execute(tx -> admision.historial(salida.solicitudId(), backoffice));
        assertThat(historial).hasSize(2);
    }

    @Test
    void rechazarContraUnaRecomendacionDeAceptarTambienEsApartamiento() {
        criterio(50);
        var salida = postularCon(grupo, 90, 0, false, false);
        var propuesta = transaccion.execute(
                tx -> admision.proponer(entrada(salida.solicitudId(), "RECHAZAR", 0, null), administrador));
        var decision = transaccion.execute(
                tx -> admision.resolver(entrada(salida.solicitudId(), "RECHAZAR", 1, propuesta.id()), backoffice));

        assertThat(decision.apartamiento()).isTrue();
        assertThat(decision.recomendacionAlgoritmo()).isEqualTo("ACEPTAR");
        assertThat(estadoDe(salida.solicitudId())).isEqualTo("RECHAZADA");
        assertThat(miembros(grupo)).isZero();
    }

    @Test
    void decidirEnLineaConElMotorNoEsApartamiento() {
        criterio(50);
        var salida = postularCon(grupo, 90, 0, false, false);
        var propuesta = transaccion.execute(
                tx -> admision.proponer(entrada(salida.solicitudId(), "ACEPTAR", 0, null), administrador));
        var decision = transaccion.execute(
                tx -> admision.resolver(entrada(salida.solicitudId(), "ACEPTAR", 1, propuesta.id()), backoffice));

        assertThat(decision.apartamiento()).isFalse();
        assertThat(decision.participanteId()).isNotNull();
    }
}

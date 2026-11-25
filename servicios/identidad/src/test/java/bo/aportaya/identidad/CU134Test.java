package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.CU134ConsultarParticipante;
import bo.aportaya.identidad.aplicacion.CU134GestionarMedida;
import bo.aportaya.identidad.aplicacion.CU134GestionarMedida.Evidencia;
import bo.aportaya.identidad.aplicacion.CU134GestionarMedida.Propuesta;
import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.MedidasDeCuentaRepositorio;
import bo.aportaya.identidad.infraestructura.ParticipantesRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * CU-134 contra PostgreSQL real. Un test por criterio de aceptacion de la boveda, con el mismo nombre, y
 * las pruebas de rechazo de las restricciones que el caso cita. El segundo factor es un doble declarado
 * del puerto {@link EvidenciaDeStepUp}: acepta una sola evidencia conocida (la real se prueba en CU-08).
 */
class CU134Test {

    private static final String EVIDENCIA = "evidencia-step-up-de-prueba";

    private static DSLContext dsl;
    private static TransactionTemplate transaccion;
    private static FixturaDeIdentidad fixtura;
    private static CU134ConsultarParticipante consulta;
    private static CU134GestionarMedida medidas;
    private static final AtomicInteger TELEFONO = new AtomicInteger(70000000);

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        fixtura = new FixturaDeIdentidad(dsl);
        Datos datos = new Datos(dsl);
        EvidenciaDeStepUp stepUp = (actor, evidencia) -> EVIDENCIA.equals(evidencia);
        var repo = new MedidasDeCuentaRepositorio();
        consulta =
                new CU134ConsultarParticipante(datos, new ParticipantesRepositorio(), repo, stepUp, Reloj.delSistema());
        medidas = new CU134GestionarMedida(
                datos, repo, new AccesosRepositorio(), stepUp, new Outbox("identidad"), Reloj.delSistema());
        dsl.execute(
                """
                INSERT INTO identidad.politica_medida_cuenta
                    (version, plazo_descargo_dias_habiles, plazo_apelacion_dias_habiles,
                     plazo_resolucion_apelacion_dias_habiles, prescribe_en_meses,
                     duracion_maxima_suspension_dias, fuente, vigente_desde)
                VALUES ('prueba-cu134', 5, 5, 10, 12, 90, 'prueba sintetica', now() - interval '1 day')
                ON CONFLICT (version) DO NOTHING
                """);
    }

    @Test
    @DisplayName(
            "Dado un operador con permiso de lectura de participantes · Cuando abre la ficha de un participante con"
                    + " una justificación válida · Entonces ve el teléfono y el correo enmascarados · Y existe una fila en"
                    + " registro_acceso_datos con operacion LECTURA y su justificación")
    void criterio1() {
        UUID operador = persona();
        UUID participante = persona();
        dsl.execute("UPDATE identidad.usuario SET correo = 'juana@ejemplo.bo' WHERE id = ?", participante);

        var ficha = transaccion.execute(e -> consulta.ficha(
                participante, "Ticket de soporte por cobro duplicado", null, "10.0.0.1", como(operador)));

        assertThat(ficha).isPresent();
        assertThat(ficha.get().telefonoEnmascarado())
                .contains("•")
                .doesNotContain(telefonoDe(participante).substring(5));
        assertThat(ficha.get().correoEnmascarado()).isEqualTo("j•••@e•••.bo");
        // R-SEG-02: la lectura deja rastro con justificacion, en la misma transaccion.
        assertThat(dsl.fetchCount(
                        DSL.table("comun.registro_acceso_datos"),
                        DSL.field("usuario_afectado_id")
                                .eq(participante)
                                .and(DSL.field("operacion").eq("LECTURA"))
                                .and(DSL.field("justificacion").eq("Ticket de soporte por cobro duplicado"))))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dado un operador que busca participantes sin justificación · Cuando envía la búsqueda · Entonces se"
            + " rechaza con JUSTIFICACION_INSUFICIENTE y no se devuelve ninguna fila")
    void criterio2() {
        UUID operador = persona();
        assertThatThrownBy(() -> consulta.buscar(
                        new CU134ConsultarParticipante.Busqueda(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                "corta",
                                null,
                                Optional.empty(),
                                Optional.empty(),
                                20),
                        "10.0.0.1",
                        como(operador)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-01"));
    }

    @Test
    @DisplayName("Dado un operador sin segundo factor reciente · Cuando pide revelar el teléfono de un participante ·"
            + " Entonces se rechaza con SEGUNDO_FACTOR_REQUERIDO")
    void criterio3() {
        UUID operador = persona();
        UUID participante = persona();
        // R-SEG-12: sin evidencia de step-up no se lee el dato completo de un tercero.
        assertThatThrownBy(() -> consulta.revelar(
                        participante,
                        "TELEFONO",
                        "Verificar titularidad por llamada",
                        "sin-evidencia",
                        "10.0.0.1",
                        como(operador)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-03"));
    }

    @Test
    @DisplayName(
            "Dado un operador que también es participante · Cuando intenta abrir su propia ficha o proponer una medida"
                    + " sobre su cuenta · Entonces se rechaza con ACTUA_SOBRE_SI_MISMO")
    void criterio4() {
        UUID operador = persona();
        // R-SEG-04: nadie actua sobre si mismo.
        assertThatThrownBy(() ->
                        consulta.ficha(operador, "Revision de mi propia cuenta", null, "10.0.0.1", como(operador)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-02"));
        assertThatThrownBy(() -> proponer(operador, operador, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-02"));
    }

    @Test
    @DisplayName("Dado un participante con deuda vencida · Cuando un analista intenta suspender su cuenta por mora ·"
            + " Entonces se rechaza con CAUSAL_FUERA_DE_CATALOGO y se indica el caso de restricción por mora")
    void criterio5() {
        UUID analista = persona();
        UUID participante = persona();
        assertThatThrownBy(() -> proponer(analista, participante, "MORA", "SUSPENSION_TEMPORAL", false))
                .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> {
                    assertThat(e.codigo().valor()).isEqualTo("AP-CU134-04");
                    assertThat(e.getMessage()).contains("mora");
                });
    }

    @Test
    @DisplayName("Dada una propuesta de suspensión por abuso contra participantes · Cuando se la marca como cautelar ·"
            + " Entonces se rechaza con CAUTELAR_NO_ADMITIDA")
    void criterio6() {
        UUID analista = persona();
        UUID participante = persona();
        assertThatThrownBy(() ->
                        proponer(analista, participante, "ABUSO_CONTRA_PARTICIPANTES", "SUSPENSION_TEMPORAL", true))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-05"));
    }

    @Test
    @DisplayName(
            "Dada una cuenta comprometida con evidencia de sesiones anómalas · Cuando el analista propone un bloqueo"
                    + " cautelar · Entonces la cuenta queda BLOQUEADO_SEGURIDAD, sus sesiones quedan revocadas y el"
                    + " expediente queda CAUTELAR · Y al acreditarse la notificación se guarda la fecha límite de descargo"
                    + " y el expediente pasa a EN_DESCARGO")
    void criterio7() {
        UUID analista = persona();
        UUID participante = persona();
        fixtura.sesionAbierta(participante);

        var vista = proponer(analista, participante, "CUENTA_COMPROMETIDA", "BLOQUEO_CAUTELAR", true);

        assertThat(vista.expediente().estado()).isEqualTo("CAUTELAR");
        assertThat(estadoDe(participante)).isEqualTo("BLOQUEADO_SEGURIDAD");
        // R-SEG-09: ninguna sesion ni refresco sigue vivo tras el bloqueo.
        assertThat(dsl.fetchCount(
                        DSL.table("identidad.sesion"),
                        DSL.field("usuario_id")
                                .eq(participante)
                                .and(DSL.field("revocada_en").isNull())))
                .isZero();

        var notificada = transaccion.execute(
                e -> medidas.notificar(vista.expediente().id(), OffsetDateTime.now(), como(analista)));
        assertThat(notificada.expediente().estado()).isEqualTo("EN_DESCARGO");
        assertThat(notificada.expediente().fechaLimiteDescargo())
                .isAfter(notificada.expediente().notificadaEn());
    }

    @Test
    @DisplayName(
            "Dado un expediente con el plazo de descargo abierto y sin descargo · Cuando se intenta decidir · Entonces"
                    + " se rechaza con PLAZO_DESCARGO_ABIERTO")
    void criterio8() {
        UUID analista = persona();
        UUID responsable = persona();
        UUID participante = persona();
        var vista = proponer(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false);
        transaccion.execute(e -> medidas.notificar(vista.expediente().id(), OffsetDateTime.now(), como(analista)));

        assertThatThrownBy(() -> decidir(responsable, vista.expediente().id(), "CONFIRMADA"))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-07"));
    }

    @Test
    @DisplayName(
            "Dado un expediente propuesto por un analista · Cuando el mismo analista intenta decidirlo · Entonces se"
                    + " rechaza con MISMA_PERSONA")
    void criterio9() {
        UUID analista = persona();
        UUID participante = persona();
        var vista = proponer(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false);
        // R-SEG-13: quien propone no decide; la base lo repite con ck_expediente_medida_decide_otro.
        assertThatThrownBy(() -> decidir(analista, vista.expediente().id(), "CONFIRMADA"))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-08"));
    }

    @Test
    @DisplayName(
            "Dada una propuesta de medida reenviada con la misma clave de idempotencia · Cuando llega por segunda vez"
                    + " · Entonces devuelve el mismo expedienteId y no se crea otro expediente")
    void criterio10() {
        UUID analista = persona();
        UUID participante = persona();
        String clave = UUID.randomUUID().toString();
        var primera =
                proponerConClave(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false, clave);
        var segunda =
                proponerConClave(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false, clave);

        assertThat(segunda.expediente().id()).isEqualTo(primera.expediente().id());
        assertThat(dsl.fetchCount(
                        DSL.table("identidad.expediente_medida_cuenta"),
                        DSL.field("usuario_id").eq(participante)))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dada una apelación que una tercera persona acepta · Cuando la resuelve · Entonces la medida queda"
            + " REVOCADA, la cuenta vuelve a su estado previo · Y se emite identidad.medida_cuenta_revocada sin"
            + " borrar el historial del expediente")
    void criterio11() {
        UUID analista = persona();
        UUID responsable = persona();
        UUID comite = persona();
        UUID participante = persona();
        var id = expedienteDecidido(analista, responsable, participante);
        transaccion.execute(e -> medidas.apelar(
                id, "No fui yo: adjunto la denuncia policial del robo del telefono.", como(participante)));

        var revocada = transaccion.execute(e -> medidas.resolver(
                id,
                "ACEPTADA",
                "La evidencia del afectado acredita el robo del telefono en la fecha de los hechos.",
                EVIDENCIA,
                como(comite)));

        assertThat(revocada.expediente().estado()).isEqualTo("REVOCADA");
        assertThat(estadoDe(participante)).isEqualTo("ACTIVO");
        // R-AUD-04: el evento sale en la misma transaccion; R-AUD-01: el historial no se borra.
        assertThat(dsl.fetchCount(
                        DSL.table("identidad.evento_dominio"),
                        DSL.field("tipo")
                                .eq("identidad.medida_cuenta_revocada")
                                .and(DSL.field("agregado_id").eq(id))))
                .isEqualTo(1);
        assertThat(revocada.lineaDeTiempo())
                .extracting(MedidasDeCuentaRepositorio.Hito::estado)
                .contains("CAUTELAR", "EN_DESCARGO", "DECIDIDA", "EN_APELACION", "REVOCADA");
    }

    @Test
    @DisplayName(
            "Dada una apelación recibida y pendiente de resolución · Cuando vencen los diez días hábiles sin decisión"
                    + " · Entonces la apelación queda ACEPTADA_POR_VENCIMIENTO y la medida se revierte · Y se emite una"
                    + " sola vez identidad.medida_cuenta_revocada")
    void resuelvePorVencimiento() {
        UUID participante = persona();
        UUID id = expedienteDecidido(persona(), persona(), participante);
        var vista =
                transaccion.execute(e -> medidas.apelar(id, "Apelo con los respaldos de acceso.", como(participante)));
        assertThat(vista.apelacion().orElseThrow().fechaLimiteResolucion()).isAfter(OffsetDateTime.now());
        dsl.execute(
                """
                UPDATE identidad.apelacion_medida_cuenta
                SET fecha_limite_resolucion = now() - interval '1 second'
                WHERE expediente_medida_cuenta_id = ?
                """,
                id);

        var sistema = ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
        transaccion.execute(e -> medidas.vencer(sistema));
        transaccion.execute(e -> medidas.vencer(sistema));

        var cerrada = transaccion
                .execute(e -> medidas.consultar(id, como(participante)))
                .orElseThrow();
        assertThat(cerrada.apelacion().orElseThrow().estado()).isEqualTo("ACEPTADA_POR_VENCIMIENTO");
        assertThat(cerrada.expediente().estado()).isEqualTo("REVOCADA");
        assertThat(estadoDe(participante)).isEqualTo("ACTIVO");
        assertThat(dsl.fetchCount(
                        DSL.table("identidad.evento_dominio"),
                        DSL.field("tipo")
                                .eq("identidad.medida_cuenta_revocada")
                                .and(DSL.field("agregado_id").eq(id))))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una cuenta suspendida con aportes pendientes en un grupo · Cuando se aplica la suspensión · Entonces"
                    + " ninguna obligación, entrega ni cupo del grupo cambia por la medida")
    void criterio12() {
        UUID analista = persona();
        UUID participante = persona();
        int antes = filasDeDinero();

        proponer(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", true);

        assertThat(estadoDe(participante)).isEqualTo("SUSPENDIDO");
        assertThat(filasDeDinero()).isEqualTo(antes);
    }

    @Test
    @DisplayName("rechaza un segundo expediente abierto por la misma causal (R-SEG-14)")
    void rechazaSegundoExpedienteAbierto() {
        UUID analista = persona();
        UUID participante = persona();
        proponer(analista, participante, "CUENTA_DUPLICADA_O_DE_TERCERO", "SUSPENSION_TEMPORAL", false);
        assertThatThrownBy(() ->
                        proponer(analista, participante, "CUENTA_DUPLICADA_O_DE_TERCERO", "SUSPENSION_TEMPORAL", false))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-06"));
    }

    @Test
    @DisplayName(
            "rechaza que decida o resuelva la apelacion quien ya actuo, y una segunda apelacion (R-SEG-13, R-SEG-15)")
    void rechazaSegregacionYApelacionDuplicada() {
        UUID analista = persona();
        UUID responsable = persona();
        UUID participante = persona();
        var id = expedienteDecidido(analista, responsable, participante);
        transaccion.execute(e ->
                medidas.apelar(id, "Pido revision: la evidencia es de otra cuenta, no de la mia.", como(participante)));

        assertThatThrownBy(() -> transaccion.execute(
                        e -> medidas.resolver(id, "RECHAZADA", "x".repeat(60), EVIDENCIA, como(responsable))))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-08"));
        assertThatThrownBy(() -> transaccion.execute(
                        e -> medidas.apelar(id, "Segunda apelacion que no corresponde.", como(participante))))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-09"));
        // R-SEG-15: la base no deja una decision sin fundamento.
        assertThat(rechazaLaBase(
                        "UPDATE identidad.expediente_medida_cuenta SET fundamento_decision = 'corto' WHERE id = ?", id))
                .contains("ck_expediente_medida_decision_fundada");
    }

    @Test
    @DisplayName("rechaza editar la evidencia o el historial del expediente (R-AUD-01)")
    void rechazaEditarElExpediente() {
        UUID analista = persona();
        UUID participante = persona();
        var id = proponer(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false)
                .expediente()
                .id();
        // Mismo criterio que R-GAR-02 (evidencia inmutable) y R-ORG-05 (una apelacion, por otro) en sus casos.
        assertThat(rechazaLaBase(
                        """
                        UPDATE identidad.evidencia_medida_cuenta SET contenido = 'otra'
                         WHERE expediente_medida_cuenta_id = ?
                        """,
                        id))
                .contains("R-AUD-01");
        assertThat(rechazaLaBase(
                        "DELETE FROM identidad.historial_estado_medida_cuenta WHERE expediente_medida_cuenta_id = ?",
                        id))
                .contains("R-AUD-01");
    }

    // R-SEG-03 (RLS) y R-SEG-10 (operador con TOTP) los aplica la plataforma antes de llegar al organismo:
    // la sesion de backoffice exige TOTP confirmado y cada lectura corre con datos.conContexto. R-REP-01 lo
    // garantiza CU-70 al consumir identidad.medida_cuenta_firme / _revocada, un evento por hecho.

    private static UUID persona() {
        return fixtura.usuario("+591" + TELEFONO.incrementAndGet());
    }

    private static ContextoSesion como(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private static CU134GestionarMedida.Vista proponer(
            UUID por, UUID sobre, String causal, String medida, boolean cautelar) {
        return proponerConClave(
                por, sobre, causal, medida, cautelar, UUID.randomUUID().toString());
    }

    private static CU134GestionarMedida.Vista proponerConClave(
            UUID por, UUID sobre, String causal, String medida, boolean cautelar, String clave) {
        return transaccion.execute(e -> medidas.proponer(
                sobre,
                new Propuesta(
                        causal,
                        medida,
                        cautelar,
                        "Hallazgo documentado por el equipo de prevencion de fraude en la revision semanal.",
                        List.of(new Evidencia("Captura de los intentos de acceso", null)),
                        Optional.empty()),
                clave,
                como(por)));
    }

    private static CU134GestionarMedida.Vista decidir(UUID por, UUID expediente, String resultado) {
        return transaccion.execute(e -> medidas.decidir(
                expediente,
                resultado,
                "La evidencia del sistema y el descargo se ponderaron y sostienen la medida propuesta.",
                EVIDENCIA,
                como(por)));
    }

    private static UUID expedienteDecidido(UUID analista, UUID responsable, UUID participante) {
        UUID id = proponer(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", true)
                .expediente()
                .id();
        transaccion.execute(e -> medidas.notificar(id, OffsetDateTime.now(), como(analista)));
        transaccion.execute(e -> medidas.descargo(
                id, "Presento mi descargo con los comprobantes de mis pagos.", List.of(), como(participante)));
        decidir(responsable, id, "CONFIRMADA");
        return id;
    }

    private static String estadoDe(UUID usuario) {
        return dsl.fetchValue("SELECT estado FROM identidad.usuario WHERE id = ?", usuario)
                .toString();
    }

    private static String telefonoDe(UUID usuario) {
        return dsl.fetchValue("SELECT telefono_e164 FROM identidad.usuario WHERE id = ?", usuario)
                .toString();
    }

    private static int filasDeDinero() {
        return dsl.fetchValue(
                        """
                        SELECT (SELECT count(*) FROM aportes.obligacion_aporte)
                             + (SELECT count(*) FROM entregas.entrega_fondo)
                             + (SELECT count(*) FROM grupos.cupo)
                        """,
                        Long.class)
                .intValue();
    }

    private static String rechazaLaBase(String sql, Object... valores) {
        try {
            transaccion.execute(estado -> {
                dsl.execute(sql, valores);
                estado.setRollbackOnly();
                return null;
            });
            return "";
        } catch (RuntimeException e) {
            Throwable raiz = e;
            while (raiz.getCause() != null && raiz.getCause() != raiz) {
                raiz = raiz.getCause();
            }
            return String.valueOf(raiz.getMessage());
        }
    }
}

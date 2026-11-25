package bo.aportaya.identidad;

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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * CU-134 contra PostgreSQL real: el armado y las ayudas que comparten {@link CU134Test} y
 * {@link CU134ApelacionTest}. El segundo factor es un doble declarado del puerto {@link EvidenciaDeStepUp}:
 * acepta una sola evidencia conocida (la real se prueba en CU-08).
 */
abstract class EscenarioDeCU134 {

    static final String EVIDENCIA = "evidencia-step-up-de-prueba";

    static DSLContext dsl;
    static TransactionTemplate transaccion;
    static FixturaDeIdentidad fixtura;
    static CU134ConsultarParticipante consulta;
    static CU134GestionarMedida medidas;
    static final AtomicInteger TELEFONO = new AtomicInteger(70000000);

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

    static UUID persona() {
        return fixtura.usuario("+591" + TELEFONO.incrementAndGet());
    }

    static ContextoSesion como(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    static CU134GestionarMedida.Vista proponer(
            UUID por, UUID sobre, String causal, String medida, boolean cautelar) {
        return proponerConClave(
                por, sobre, causal, medida, cautelar, UUID.randomUUID().toString());
    }

    static CU134GestionarMedida.Vista proponerConClave(
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

    static CU134GestionarMedida.Vista decidir(UUID por, UUID expediente, String resultado) {
        return transaccion.execute(e -> medidas.decidir(
                expediente,
                resultado,
                "La evidencia del sistema y el descargo se ponderaron y sostienen la medida propuesta.",
                EVIDENCIA,
                como(por)));
    }

    static UUID expedienteDecidido(UUID analista, UUID responsable, UUID participante) {
        UUID id = proponer(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", true)
                .expediente()
                .id();
        transaccion.execute(e -> medidas.notificar(id, OffsetDateTime.now(), como(analista)));
        transaccion.execute(e -> medidas.descargo(
                id, "Presento mi descargo con los comprobantes de mis pagos.", List.of(), como(participante)));
        decidir(responsable, id, "CONFIRMADA");
        return id;
    }

    static String estadoDe(UUID usuario) {
        return dsl.fetchValue("SELECT estado FROM identidad.usuario WHERE id = ?", usuario)
                .toString();
    }

    static String telefonoDe(UUID usuario) {
        return dsl.fetchValue("SELECT telefono_e164 FROM identidad.usuario WHERE id = ?", usuario)
                .toString();
    }

    static int filasDeDinero() {
        return dsl.fetchSingle(
                        """
                        SELECT (SELECT count(*) FROM aportes.obligacion_aporte)
                             + (SELECT count(*) FROM entregas.entrega_fondo)
                             + (SELECT count(*) FROM grupos.cupo)
                        """)
                .get(0, Integer.class);
    }

    static String rechazaLaBase(String sql, Object... valores) {
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

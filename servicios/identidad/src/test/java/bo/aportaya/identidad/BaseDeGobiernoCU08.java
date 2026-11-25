package bo.aportaya.identidad;

import bo.aportaya.identidad.aplicacion.CU08AsignarRol;
import bo.aportaya.identidad.aplicacion.CU08DarDeBaja;
import bo.aportaya.identidad.aplicacion.CU08InformeDeAccesos;
import bo.aportaya.identidad.aplicacion.CU08RevocarRol;
import bo.aportaya.identidad.aplicacion.CU08VencerAccesos;
import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.PedidoDeAsignacion;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.ResultadoDeAsignacion;
import bo.aportaya.identidad.aplicacion.ReglasDeGobierno;
import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import bo.aportaya.identidad.infraestructura.AccesoRepositorio;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.Argon2Hasheador;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.identidad.infraestructura.InformeDeAccesosRepositorio;
import bo.aportaya.identidad.infraestructura.RolesYPermisosRepositorio;
import bo.aportaya.identidad.infraestructura.UsuariosInternosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
 * El armado del gobierno de accesos de CU-08 (GU-B) contra PostgreSQL real, con las piezas a mano.
 *
 * <p><b>Cada escenario corre en una transaccion que se deshace</b> ({@link #enTransaccionQueSeDeshace}): la
 * base de pruebas es compartida y la regla «nunca sin administrador» cuenta a TODOS los titulares de
 * {@code ACCESOS_ADMINISTRAR}. Dentro de la transaccion se revocan las asignaciones vivas de las demas pruebas
 * ({@link #aislar}) y nada de eso sobrevive al escenario.
 *
 * <p><b>Doble declarado</b> (regla 65): la evidencia step-up es un doble que acepta solo {@link #EVIDENCIA}; la
 * validacion real del JWT se prueba aparte en {@code EvidenciaDeStepUpJwtTest}.
 */
abstract class BaseDeGobiernoCU08 {

    protected static final String EVIDENCIA = "evidencia-step-up-valida-de-prueba";
    protected static final EvidenciaDeStepUp DOBLE_DE_STEP_UP = (actor, evidencia) -> EVIDENCIA.equals(evidencia);

    protected static DSLContext dsl;
    protected static TransactionTemplate transaccion;
    protected static FixturaDeIdentidad fixtura;
    protected static AccesosRepositorio accesos;
    protected static GobiernoDeAccesos gobierno;
    protected static CU08DarDeBaja baja;
    protected static CU08VencerAccesos vencer;
    protected static CU08InformeDeAccesos informe;
    protected static GestionarUsuariosInternos gestion;

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        fixtura = new FixturaDeIdentidad(dsl);

        Datos datos = new Datos(dsl);
        accesos = new AccesosRepositorio();
        AccesoRepositorio factores = new AccesoRepositorio();
        GobiernoDeAccesosRepositorio repositorio = new GobiernoDeAccesosRepositorio();
        Outbox outbox = new Outbox("identidad");
        Reloj reloj = Reloj.delSistema();
        ReglasDeGobierno reglas = new ReglasDeGobierno(DOBLE_DE_STEP_UP);

        gobierno = new GobiernoDeAccesos(
                datos,
                accesos,
                factores,
                repositorio,
                new CU08AsignarRol(datos, accesos, factores, outbox, reloj),
                new CU08RevocarRol(datos, accesos, outbox, reloj),
                reglas,
                outbox,
                reloj);
        baja = new CU08DarDeBaja(datos, accesos, repositorio, reglas, outbox, reloj);
        vencer = new CU08VencerAccesos(datos, accesos, repositorio, outbox, reloj, 500);
        informe = new CU08InformeDeAccesos(datos, new InformeDeAccesosRepositorio(), reloj, Duration.ofDays(7));
        RolesYPermisosRepositorio roles = new RolesYPermisosRepositorio(accesos);
        gestion = new GestionarUsuariosInternos(
                datos,
                new UsuariosInternosRepositorio(accesos, new Argon2Hasheador("pimienta-de-prueba"), roles),
                roles,
                accesos,
                repositorio,
                reglas,
                reloj,
                outbox);
    }

    protected static void enTransaccionQueSeDeshace(Runnable escenario) {
        transaccion.execute(estado -> {
            estado.setRollbackOnly();
            aislar();
            escenario.run();
            return null;
        });
    }

    /** Dentro de la transaccion del escenario: nadie mas tiene accesos vivos. Se deshace con todo lo demas. */
    private static void aislar() {
        dsl.execute(
                """
                UPDATE identidad.asignacion_rol
                   SET revocada_en = now(), motivo_revocacion = 'aislamiento de la prueba'
                 WHERE revocada_en IS NULL
                """);
    }

    protected static ContextoSesion como(UUID actor) {
        return ContextoSesion.de(
                actor, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    /** Un operador sintetico: usuario ACTIVO con perfil interno. */
    protected static UUID operador() {
        UUID usuario = fixtura.usuario("+5917" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999));
        dsl.execute(
                """
                INSERT INTO identidad.usuario_interno (id, usuario_id, departamento, cargo)
                VALUES (gen_random_uuid(), ?, 'SISTEMAS', 'Analista de prueba')
                """,
                usuario);
        return usuario;
    }

    /** Un rol GLOBAL con codigo unico y, si se pide, uno o mas permisos (existentes o nuevos). */
    protected static UUID rol(String... permisos) {
        UUID rol = fixtura.rolGlobal("GU_B_" + UUID.randomUUID().toString().substring(0, 8));
        for (String permiso : permisos) {
            fixtura.darPermisoAlRol(rol, permiso(permiso));
        }
        return rol;
    }

    /** El rol con ese codigo (se crea dentro de la transaccion si la base de pruebas no lo tiene). */
    protected static UUID rolLlamado(String codigo) {
        return Optional.ofNullable(dsl.fetchValue("SELECT id FROM identidad.rol WHERE codigo = ?", codigo))
                .map(UUID.class::cast)
                .orElseGet(() -> fixtura.rolGlobal(codigo));
    }

    private static UUID permiso(String codigo) {
        dsl.execute(
                """
                INSERT INTO identidad.permiso (id, codigo, descripcion, recurso, accion, nivel_riesgo, requiere_mfa)
                VALUES (gen_random_uuid(), ?, 'permiso de prueba', 'accesos', 'ADMINISTRAR', 'ALTO', true)
                ON CONFLICT (codigo) DO NOTHING
                """,
                codigo);
        return (UUID) dsl.fetchValue("SELECT id FROM identidad.permiso WHERE codigo = ?", codigo);
    }

    protected static ResultadoDeAsignacion asignar(UUID actor, UUID destinatario, UUID rol) {
        return gobierno.asignar(
                new PedidoDeAsignacion(
                        destinatario,
                        rol,
                        "GLOBAL",
                        Optional.empty(),
                        Optional.empty(),
                        "Cobertura de funciones aprobada",
                        EVIDENCIA),
                como(actor));
    }

    protected static int eventos(String tipo, UUID agregado) {
        return dsl.fetchCount(
                DSL.table("identidad.evento_dominio"),
                DSL.field("tipo").eq(tipo).and(DSL.field("agregado_id").eq(agregado)));
    }

    protected static Object columna(String sql, Object... parametros) {
        return dsl.fetchValue(sql, parametros);
    }

    protected static int sesionesVivas(UUID usuario) {
        return dsl.fetchCount(
                DSL.table("identidad.sesion"),
                DSL.field("usuario_id").eq(usuario).and(DSL.field("revocada_en").isNull()));
    }
}

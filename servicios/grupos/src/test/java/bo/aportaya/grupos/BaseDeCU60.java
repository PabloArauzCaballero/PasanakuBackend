package bo.aportaya.grupos;

import bo.aportaya.grupos.aplicacion.CU60Sortear;
import bo.aportaya.grupos.dominio.puertos.PrimerAporteDelCupo;
import bo.aportaya.grupos.infraestructura.AceptacionVigenteRepositorio;
import bo.aportaya.grupos.infraestructura.SorteoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/** El armado de CU-60, con las piezas construidas a mano. */
abstract class BaseDeCU60 {

    protected static DSLContext dsl;
    protected static TransactionTemplate transaccion;
    protected static CU60Sortear sortear;
    protected static FixturaDeGrupos fixtura;
    protected static SorteoRepositorio sorteos;
    protected static Consumidos consumidos;
    protected static UUID organizador;
    protected static AportesDoble aportes;

    /**
     * El doble de `aportes` (R-GRP-20): por omision todos los cupos ocupados pagaron la cuota 1; cada prueba marca los
     * que no, o lo deja caido para que no conteste. Lee los cupos por su propia conexion, fuera de la transaccion.
     */
    protected static final class AportesDoble implements PrimerAporteDelCupo {
        private final DSLContext dsl;
        final Set<UUID> sinPagar = new HashSet<>();
        boolean caido;

        AportesDoble(DSLContext dsl) {
            this.dsl = dsl;
        }

        @Override
        public Optional<Set<UUID>> cuposConPrimeraCuotaPagada(UUID grupoId) {
            if (caido) return Optional.empty();
            return Optional.of(dsl
                    .fetch("SELECT id FROM grupos.cupo WHERE grupo_id = ? AND estado = 'OCUPADO'", grupoId)
                    .map(f -> f.get("id", UUID.class))
                    .stream()
                    .filter(id -> !sinPagar.contains(id))
                    .collect(Collectors.toSet()));
        }

        void reiniciar() {
            sinPagar.clear();
            caido = false;
        }
    }

    @AfterEach
    void aportesSano() {
        aportes.reiniciar();
    }

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        // La fixtura escribe por SU PROPIA conexion, fuera de la transaccion que se
        // esta probando: sembrada dentro, una prueba que revierte se lleva puesta la
        // fila que las siguientes daban por hecha.
        DSLContext dslFixtura = DSL.using(fuente, SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        sorteos = new SorteoRepositorio();
        aportes = new AportesDoble(dslFixtura);
        sortear = new CU60Sortear(
                new Datos(dsl),
                sorteos,
                new Outbox("grupos"),
                Reloj.delSistema(),
                Ids.seguros(),
                aportes,
                new AceptacionVigenteRepositorio());
        fixtura = new FixturaDeGrupos(dslFixtura);
        consumidos = new Consumidos("grupos");
        // Fuera de toda transaccion: creado dentro de una que despues revierte, la
        // clave foranea de `ejecutado_por` apunta a un usuario que ya no existe y
        // fallan las pruebas siguientes, no la que reviritio.
        organizador = fixtura.usuario();
    }

    protected ContextoSesion contexto() {
        return ContextoSesion.de(
                organizador, "ORGANIZADOR", new Traza(UUID.randomUUID().toString()));
    }

    protected int turnosDe(UUID grupoId) {
        return dsl.fetchCount(DSL.table("grupos.turno"), DSL.field("grupo_id").eq(grupoId));
    }

    protected String estadoDelSorteo(UUID sorteoId) {
        return String.valueOf(dsl.fetchOne("SELECT estado FROM grupos.sorteo_turnos WHERE id = ?", sorteoId)
                .get(0));
    }

    protected void dejarUnaFilaEnLaBitacora() {
        transaccion.execute(estado -> {
            dsl.execute(
                    """
                    INSERT INTO comun.bitacora_evento
                        (id, secuencia, entidad, entidad_id, accion, origen, correlation_id,
                         hash_registro, hash_anterior, fecha_hora)
                    VALUES (gen_random_uuid(),
                            nextval(pg_get_serial_sequence('comun.bitacora_evento','secuencia')),
                            'prueba_sorteo', gen_random_uuid(), 'CREACION', 'TAREA_PROGRAMADA',
                            gen_random_uuid(), repeat('a', 64), repeat('0', 64), now())
                    """);
            return null;
        });
    }

    protected String rechazaLaBase(String sql, Object... parametros) {
        try {
            transaccion.execute(estado -> {
                dsl.execute(sql, parametros);
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

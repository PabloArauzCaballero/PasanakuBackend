package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.infraestructura.CalendarioDeAportesRepositorio;
import bo.aportaya.grupos.infraestructura.ConsultasRepositorio;
import bo.aportaya.grupos.infraestructura.clientes.HechosDeEnsayo;
import bo.aportaya.grupos.web.EtapaDelIngreso;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La etapa minima de un ingreso contra las condiciones REALES del grupo (PostgreSQL, rol {@code svc_grupos}) y contra un
 * transparencia por HTTP real (2026-10-30). El transparencia es un DOBLE declarado del contrato
 * {@code consultarEtapasDePersonas} + {@code listarEtapas} ({@link HechosDeEnsayo}), en los tres niveles: alcanza (pasa),
 * no alcanza (AP-CU68-03 con la etapa minima por su codigo) y transparencia caida (no entra: falla cerrado, regla
 * 98.2.4). Datos sinteticos.
 */
class CU69EtapaContraTransparenciaTest {

    private static DSLContext dsl;
    private static TransactionTemplate transaccion;
    private static Consultas consultas;

    private HechosDeEnsayo transparencia;
    private UUID grupo;

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        consultas = new Consultas(new Datos(dsl), new ConsultasRepositorio(), new CalendarioDeAportesRepositorio());
    }

    @BeforeEach
    void grupoQuePideMedia() throws Exception {
        transparencia = HechosDeEnsayo.levantar();
        grupo = UUID.randomUUID();
        // Pide la etapa MEDIA del catalogo sintetico: su `desde` (500.00) es lo que guarda reputacion_minima.
        dsl.execute(
                """
                INSERT INTO grupos.grupo
                    (id, codigo_publico, nombre, monto_aporte, moneda, periodicidad, dia_cobro,
                     num_periodos, cupos_totales, cupos_ocupados, fecha_inicio, fecha_fin_estimada,
                     estado, tipo_conformacion, modalidad_turnos, visibilidad, es_autogestionado,
                     requiere_kyc_minimo, reputacion_minima, dias_gracia, aplica_recargo_mora,
                     usa_fondo_garantia, porcentaje_fondo_garantia, quorum_decisiones)
                VALUES (?, ?, 'Grupo de prueba', 300.00, 'BOB', 'MENSUAL', 5, 8, 8, 1,
                        CURRENT_DATE, CURRENT_DATE + 240, 'ABIERTO_A_INSCRIPCION', 'MANUAL_POR_INVITACION',
                        'SORTEO_ALEATORIO', 'PRIVADO', true, 'BASICO', 500.00, 3, true, false, 0.00, 0.600)
                """,
                grupo,
                "GR-" + grupo.toString().substring(0, 8));
    }

    @AfterEach
    void bajar() {
        transparencia.bajar();
    }

    /** Lee las condiciones con el rol de base del servicio y exige la etapa de quien entra, como la API. */
    private void exigir(UUID entrante) {
        var ctx = ContextoSesion.de(
                entrante, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
        var condiciones = transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_grupos");
            return consultas.condicionesDeIngreso(grupo, ctx);
        });
        assertThat(condiciones).as("el grupo existe y recibe integrantes").isPresent();
        EtapaDelIngreso.exigir(transparencia.hechos(), condiciones, entrante);
    }

    @Test
    @DisplayName(
            "Dado el caso correcto de un grupo que pide la etapa MEDIA · "
                    + "Cuando quiere entrar alguien que esta en MEDIA · "
                    + "Entonces entra con la etapa que dio transparencia por la ruta de servicio, con el token del servicio y sin pedir el puntaje exacto")
    void alcanzaLaMinima() {
        UUID entrante = UUID.randomUUID();
        transparencia.etapaDe(entrante, "MEDIA", "500.00");

        exigir(entrante);

        assertThat(transparencia.autorizacionesDePersonas()).containsOnly("Bearer " + HechosDeEnsayo.TOKEN);
        assertThat(transparencia.pidioUnPuntaje()).as("nunca el puntaje exacto").isFalse();
    }

    @Test
    @DisplayName("Dado el caso limite de un grupo que pide la etapa MEDIA · "
            + "Cuando quiere entrar alguien que esta en BAJA, una etapa por debajo · "
            + "Entonces no entra y recibe AP-CU68-03 con detalle.etapaMinima MEDIA")
    void noAlcanzaLaMinima() {
        UUID entrante = UUID.randomUUID();
        transparencia.etapaDe(entrante, "BAJA", "0.00");

        assertThatThrownBy(() -> exigir(entrante))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(e -> {
                    var error = (ErrorDeNegocio) e;
                    assertThat(error.codigo().valor()).isEqualTo("AP-CU68-03");
                    assertThat(error.detalle()).containsEntry("etapaMinima", "MEDIA");
                });
    }

    @Test
    @DisplayName("Dado el caso invalido de un grupo que pide la etapa MEDIA con transparencia caido · "
            + "Cuando quiere entrar alguien que esta en MEDIA · "
            + "Entonces el ingreso se rechaza (falla cerrado) y no se supone ninguna etapa")
    void transparenciaCaidaRechazaElIngreso() {
        UUID entrante = UUID.randomUUID();
        transparencia.etapaDe(entrante, "MEDIA", "500.00");
        transparencia.bajar();

        assertThatThrownBy(() -> exigir(entrante)).isInstanceOf(ErrorDeDominio.class);
    }

    @Test
    @DisplayName(
            "Dado el caso invalido de un grupo que pide la etapa MEDIA con transparencia respondiendo 403 a la ruta de servicio · "
                    + "Cuando quiere entrar alguien que esta en MEDIA · "
                    + "Entonces el ingreso se rechaza")
    void sinIdentidadDeServicioRechazaElIngreso() {
        UUID entrante = UUID.randomUUID();
        transparencia.etapaDe(entrante, "MEDIA", "500.00");
        transparencia.rechazarTodoToken();

        assertThatThrownBy(() -> exigir(entrante)).isInstanceOf(ErrorDeDominio.class);
    }
}

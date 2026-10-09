package bo.aportaya.entregas;

import bo.aportaya.entregas.aplicacion.CU18RegistrarCuentaDestino;
import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.aplicacion.CU22LiquidarEntrega;
import bo.aportaya.entregas.aplicacion.CU28EmitirDesembolso;
import bo.aportaya.entregas.aplicacion.ComprarOfertaDeTurno;
import bo.aportaya.entregas.aplicacion.GestionDeOfertas;
import bo.aportaya.entregas.aplicacion.RegistroDeOfertas;
import bo.aportaya.entregas.aplicacion.RegistroDelFondeo;
import bo.aportaya.entregas.aplicacion.TransicionesDeCesion;
import bo.aportaya.entregas.infraestructura.CesionRepositorio;
import bo.aportaya.entregas.infraestructura.CuentaDestinoRepositorio;
import bo.aportaya.entregas.infraestructura.DesembolsoRepositorio;
import bo.aportaya.entregas.infraestructura.EntregaRepositorio;
import bo.aportaya.entregas.infraestructura.FondeoRepositorio;
import bo.aportaya.entregas.infraestructura.OfertaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.Duration;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/** El armado del carril de entregas, con las piezas construidas a mano. */
abstract class BaseDeEntregas {

    /**
     * La pimienta del hash de busqueda.
     *
     * <p>En produccion sale del almacen de secretos; aca es un valor de prueba fijo
     * para que el hash sea reproducible entre corridas. Lo que importa del invariante
     * —que exista y que no se guarde junto al hash— se verifica igual.
     */
    protected static final String PIMIENTA = "pimienta-de-prueba-no-es-la-de-produccion";

    protected static final int VERSION_LLAVE = 1;
    protected static final int MAXIMO_DE_CUENTAS = 3;
    protected static final Duration ENFRIAMIENTO = Duration.ofHours(24);
    protected static final int INTENTOS_MAXIMOS = 3;

    protected static DSLContext dsl;
    protected static DSLContext dslFixtura;
    protected static TransactionTemplate transaccion;
    protected static FixturaDeEntregas fixtura;
    protected static Consumidos consumidos;

    protected static CU18RegistrarCuentaDestino cuentaCU;
    protected static CU22LiquidarEntrega entregaCU;
    protected static RegistroDelFondeo registroDelFondeo;
    protected static DoblesDelPozo.Aportes aportesDoble;
    protected static DoblesDelPozo.Garantia garantiaDoble;
    protected static CU22EntregarPozoCompleto pozoCU;
    protected static DoblesDelMercado.Grupos gruposDoble;
    protected static DoblesDelMercado.Fondos fondosDoble;
    protected static RegistroDeOfertas registroDeOfertasReal;
    protected static GestionDeOfertas ofertasCU;
    protected static TransicionesDeCesion pasosDeCesion;
    protected static ComprarOfertaDeTurno compraCU;
    protected static CU28EmitirDesembolso desembolsoCU;

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        dslFixtura = DSL.using(fuente, SQLDialect.POSTGRES);
        var gestor = new DataSourceTransactionManager(fuente);
        transaccion = new TransactionTemplate(gestor);
        fixtura = new FixturaDeEntregas(dslFixtura);
        consumidos = new Consumidos("entregas");

        Datos datos = new Datos(dsl);
        Outbox outbox = new Outbox("entregas");
        var cuentas = new CuentaDestinoRepositorio();
        var entregas = new EntregaRepositorio();

        cuentaCU = new CU18RegistrarCuentaDestino(
                datos, cuentas, outbox, Reloj.delSistema(), PIMIENTA, VERSION_LLAVE, MAXIMO_DE_CUENTAS, ENFRIAMIENTO);
        entregaCU = new CU22LiquidarEntrega(datos, entregas, outbox, Reloj.delSistema());
        // En produccion Spring abre la transaccion en @Transactional; aca se arma a mano, asi que el
        // doble la abre alrededor de la escritura LOCAL y nada mas: las preguntas a otros servicios
        // siguen quedando FUERA de ella, como en el servicio real.
        registroDelFondeo =
                new RegistroDelFondeo(
                        datos,
                        new FondeoRepositorio(),
                        entregas,
                        new CesionRepositorio(),
                        outbox,
                        Reloj.delSistema(),
                        Duration.ofHours(24)) {
                    @Override
                    public Salida registrar(
                            Entrada e,
                            bo.aportaya.entregas.dominio.FondeoDelPozo.Resultado cifras,
                            java.util.UUID coberturaId,
                            java.time.OffsetDateTime corte,
                            ContextoSesion ctx) {
                        return transaccion.execute(t -> super.registrar(e, cifras, coberturaId, corte, ctx));
                    }
                };
        aportesDoble = new DoblesDelPozo.Aportes();
        garantiaDoble = new DoblesDelPozo.Garantia();
        pozoCU = new CU22EntregarPozoCompleto(aportesDoble, garantiaDoble, registroDelFondeo);
        // Los casos de uso del mercado abren su transaccion con @Transactional; aca se arma a mano,
        // asi que se les pone el mismo interceptor que les pondria Spring. Las llamadas a grupos y a
        // nucleo viven en clases SIN transaccion, como en el servicio real.
        gruposDoble = new DoblesDelMercado.Grupos();
        fondosDoble = new DoblesDelMercado.Fondos();
        var ofertasRepo = new OfertaRepositorio();
        var cesionesRepo = new CesionRepositorio();
        var candados = new FondeoRepositorio();
        registroDeOfertasReal = conTransaccion(
                new RegistroDeOfertas(datos, ofertasRepo, cesionesRepo, candados, outbox, Reloj.delSistema()), gestor);
        ofertasCU = new GestionDeOfertas(gruposDoble, registroDeOfertasReal, Reloj.delSistema(), Duration.ofDays(7));
        pasosDeCesion = conTransaccion(
                new TransicionesDeCesion(datos, ofertasRepo, cesionesRepo, candados, outbox, Reloj.delSistema()),
                gestor);
        compraCU = new ComprarOfertaDeTurno(
                gruposDoble, fondosDoble, pasosDeCesion, Reloj.delSistema(), Duration.ofMinutes(10));
        desembolsoCU = new CU28EmitirDesembolso(
                datos,
                new DesembolsoRepositorio(),
                entregas,
                cuentas,
                outbox,
                Reloj.delSistema(),
                INTENTOS_MAXIMOS,
                Duration.ofMinutes(15));
    }

    @SuppressWarnings("unchecked")
    private static <T> T conTransaccion(T objetivo, org.springframework.transaction.PlatformTransactionManager gestor) {
        var interceptor = new org.springframework.transaction.interceptor.TransactionInterceptor();
        interceptor.setTransactionManager(gestor);
        interceptor.setTransactionAttributeSource(
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource());
        var fabrica = new org.springframework.aop.framework.ProxyFactory(objetivo);
        fabrica.setProxyTargetClass(true);
        fabrica.addAdvice(interceptor);
        return (T) fabrica.getProxy();
    }

    protected ContextoSesion contextoDe(UUID usuarioId) {
        return ContextoSesion.de(
                usuarioId, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    protected int contar(String consulta, Object... parametros) {
        return ((Number) dsl.fetchOne(consulta, parametros).get(0)).intValue();
    }

    protected String raizDe(Throwable e) {
        Throwable raiz = e;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        return String.valueOf(raiz.getMessage());
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
            return raizDe(e);
        }
    }
}

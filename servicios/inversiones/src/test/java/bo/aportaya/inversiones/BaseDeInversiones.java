package bo.aportaya.inversiones;

import bo.aportaya.inversiones.aplicacion.AplicadorDeInstrucciones;
import bo.aportaya.inversiones.aplicacion.CU120Catalogo;
import bo.aportaya.inversiones.aplicacion.CU121OrdenarInversion;
import bo.aportaya.inversiones.aplicacion.CU122ConfirmarPosicion;
import bo.aportaya.inversiones.aplicacion.CU123Devengo;
import bo.aportaya.inversiones.aplicacion.CU123Posiciones;
import bo.aportaya.inversiones.aplicacion.CU124SolicitarRescate;
import bo.aportaya.inversiones.aplicacion.CU125LiquidarRescate;
import bo.aportaya.inversiones.aplicacion.CalculoDeLiquidacion;
import bo.aportaya.inversiones.aplicacion.Transaccionar;
import bo.aportaya.inversiones.aplicacion.VistaOrden;
import bo.aportaya.inversiones.infraestructura.CatalogoRepositorio;
import bo.aportaya.inversiones.infraestructura.ComprobanteRepositorio;
import bo.aportaya.inversiones.infraestructura.GuardiaDeProduccion;
import bo.aportaya.inversiones.infraestructura.InstruccionRepositorio;
import bo.aportaya.inversiones.infraestructura.OrdenRepositorio;
import bo.aportaya.inversiones.infraestructura.PosicionRepositorio;
import bo.aportaya.inversiones.infraestructura.RescateRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CalendarioHabil;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

/**
 * El armado del servicio con las piezas construidas a mano, contra PostgreSQL REAL
 * (Testcontainers, con {@code sql/aplicar.sql}). El libro y el aliado son dobles declarados
 * ({@link LibroDoble}, {@link AliadoDoble}); todo lo demas es el codigo de produccion.
 *
 * <p>El reloj lo mueve la prueba. El aliado tiene su propio «hoy» y se alinea con el nuestro.
 */
public abstract class BaseDeInversiones {

    protected static final Instant LUNES = Instant.parse("2026-10-12T12:00:00Z"); // 08:00 en La Paz
    protected static final String PRODUCTO_DPF = "DPF-T";
    protected static final String PRODUCTO_DPF_ANTICIPABLE = "DPF-T-ANT";
    protected static final String PRODUCTO_FONDO = "FONDO-T";

    protected static DSLContext dsl;
    protected static DSLContext dslFixtura;
    protected static Datos datos;
    protected static Transaccionar tx;
    protected static CatalogoRepositorio catalogo;
    protected static OrdenRepositorio ordenes;
    protected static PosicionRepositorio posiciones;
    protected static RescateRepositorio rescates;
    protected static ComprobanteRepositorio comprobantes;
    protected static InstruccionRepositorio instrucciones;

    protected static final AtomicReference<Instant> AHORA = new AtomicReference<>(LUNES);
    protected static final Reloj RELOJ = () -> AHORA.get();
    protected static final CalendarioHabil CALENDARIO =
            f -> f.getDayOfWeek() == DayOfWeek.SATURDAY || f.getDayOfWeek() == DayOfWeek.SUNDAY;

    protected LibroDoble libro;
    protected AliadoDoble aliado;
    protected AplicadorDeInstrucciones aplicador;
    protected CU120Catalogo cu120;
    protected CU121OrdenarInversion cu121;
    protected CU122ConfirmarPosicion cu122;
    protected CU123Posiciones cu123;
    protected CU123Devengo cu123Devengo;
    protected CU124SolicitarRescate cu124;
    protected CU125LiquidarRescate cu125;

    /** Un fondo propio de cada prueba: el valor de cuota es estado compartido del producto y las pruebas no deben pisarse. */
    protected String productoFondo;

    protected UUID usuario;
    protected UUID cuenta;
    protected ContextoSesion ctx;

    @BeforeEach
    void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        dslFixtura = DSL.using(fuente, SQLDialect.POSTGRES);
        var gestor = new DataSourceTransactionManager(fuente);
        datos = new Datos(dsl);
        tx = new Transaccionar(gestor);
        catalogo = new CatalogoRepositorio();
        ordenes = new OrdenRepositorio();
        posiciones = new PosicionRepositorio();
        rescates = new RescateRepositorio();
        comprobantes = new ComprobanteRepositorio();
        instrucciones = new InstruccionRepositorio();

        AHORA.set(LUNES);
        libro = new LibroDoble();
        aliado = new AliadoDoble();
        var outbox = new Outbox("inversiones");
        var guardia = new GuardiaDeProduccion(false, "simulado");

        aplicador = new AplicadorDeInstrucciones(datos, tx, instrucciones, ordenes, rescates, libro, outbox, RELOJ);
        cu120 = new CU120Catalogo(datos, tx, catalogo, aliado, guardia, outbox, RELOJ);
        cu122 = new CU122ConfirmarPosicion(
                datos,
                tx,
                ordenes,
                catalogo,
                posiciones,
                comprobantes,
                instrucciones,
                aplicador,
                aliado,
                outbox,
                RELOJ);
        cu121 = new CU121OrdenarInversion(
                datos, tx, catalogo, ordenes, instrucciones, libro, cu122, guardia, outbox, RELOJ);
        cu123 = new CU123Posiciones(datos, tx, posiciones, catalogo, comprobantes, aliado, RELOJ, 3);
        cu123Devengo = new CU123Devengo(datos, tx, posiciones, catalogo, RELOJ);
        cu125 = new CU125LiquidarRescate(
                datos,
                tx,
                rescates,
                posiciones,
                catalogo,
                comprobantes,
                instrucciones,
                aplicador,
                aliado,
                new CalculoDeLiquidacion(catalogo, comprobantes),
                outbox,
                RELOJ);
        cu124 = new CU124SolicitarRescate(
                datos, tx, posiciones, catalogo, rescates, cu125, aliado, CALENDARIO, outbox, RELOJ);

        usuario = UUID.randomUUID();
        cuenta = UUID.randomUUID();
        ctx = contextoDe(usuario);
        productoFondo = PRODUCTO_FONDO + "-" + UUID.randomUUID().toString().substring(0, 8);
        var modelo = aliado.producto(PRODUCTO_FONDO);
        aliado.agregarProducto(new bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.ProductoDelAliado(
                productoFondo,
                modelo.tipo(),
                modelo.nombre(),
                modelo.plazoDias(),
                modelo.baseDias(),
                modelo.tasaNominalAnual(),
                modelo.permiteRescateAnticipado(),
                modelo.penalizacionAnticipo(),
                modelo.diasRescate(),
                modelo.horaCorte(),
                modelo.montoMinimo(),
                modelo.tasaRetencion(),
                modelo.tasaComisionExito(),
                modelo.fuente(),
                modelo.fechaCotizacion(),
                modelo.sintetico()));
        cu120.sincronizar(ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString())));
    }

    protected ContextoSesion contextoDe(UUID quien) {
        return ContextoSesion.de(
                quien, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    protected ContextoSesion sistema() {
        return ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
    }

    /** Una cuenta con saldo del titular de la sesion. */
    protected void conSaldo(String saldo) {
        libro.abrirCuenta(cuenta, usuario, saldo);
    }

    protected Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    /** Las condiciones vigentes que veria la persona antes de aceptar. */
    protected CU120Catalogo.VistaProducto producto(String codigo) {
        return cu120.listar(ctx).stream()
                .filter(v -> v.codigo().equals(codigo))
                .findFirst()
                .orElseThrow();
    }

    protected CU121OrdenarInversion.EntradaOrden entrada(String codigo, String monto, UUID clave) {
        var c = producto(codigo).condiciones();
        return new CU121OrdenarInversion.EntradaOrden(
                codigo, c.versionId(), c.textoHash(), cuenta, bob(monto), clave.toString());
    }

    protected VistaOrden ordenar(String codigo, String monto) {
        return cu121.ordenar(entrada(codigo, monto, UUID.randomUUID()), ctx);
    }

    /** Una inversion ya confirmada, para las pruebas que parten de una posicion. */
    protected UUID posicionConfirmada(String codigo, String monto) {
        VistaOrden o = ordenar(codigo, monto);
        if (!"CONFIRMADA".equals(o.estado())) {
            throw new AssertionError("Se esperaba una orden confirmada y esta " + o.estado());
        }
        return o.posicionId();
    }

    protected int contar(String sql, Object... parametros) {
        return ((Number) dslFixtura.fetchOne(sql, parametros).get(0)).intValue();
    }

    private static final class Deshacer extends RuntimeException {
        Deshacer() {
            super("deshacer", null, false, false);
        }
    }

    /** Devuelve el mensaje con que la BASE rechaza la sentencia, o vacio si la acepto (y la deshace). */
    protected String rechazaLaBase(String sql, Object... parametros) {
        try {
            tx.en(() -> {
                dsl.execute(sql, parametros);
                throw new Deshacer();
            });
            return "";
        } catch (Deshacer aceptada) {
            return "";
        } catch (RuntimeException e) {
            return raizDe(e);
        }
    }

    protected String raizDe(Throwable e) {
        Throwable raiz = e;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        return String.valueOf(raiz.getMessage());
    }
}

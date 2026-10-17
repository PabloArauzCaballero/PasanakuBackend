package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.CU24RegistrarAsiento;
import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes;
import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.AporteCobrado;
import bo.aportaya.nucleofinanciero.infraestructura.AsientoRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaContableRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentasDeSistemaRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.DestinoRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.LibroDeBilletera;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;

/**
 * El armado comun de las pruebas del consumo de {@code aportes.aporte_cobrado} (carril I): el mapeo de cuentas logicas al plan,
 * la cadena de un aporte (usuario, grupo, obligacion), las billeteras del participante y del grupo, y las lecturas con las que
 * se CUADRA: el saldo de la billetera sale del libro y el de las cuentas del plan sale del mayor, nunca de la cache.
 */
abstract class BaseDeAportes extends BaseDeBilletera {

    protected static final Map<String, String> MAPEO = new HashMap<>();
    protected static ConsumoDeAportes consumo;
    protected static LibroDeBilletera libroReal;

    @BeforeAll
    static void armarConsumoDeAportes() {
        // Pasivo exigible, de naturaleza acreedora: el saldo de los usuarios y el de los grupos es plata que se les
        // debe.
        // Solo en bolivianos, como el plan MINIMO sembrado: en dolares NO hay mapeo y un aporte en USD se rechaza.
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        contable.cuentaDeMovimiento("SU.BOB." + sufijo, "PASIVO", "ACREEDORA", "BOB");
        contable.cuentaDeMovimiento("SG.BOB." + sufijo, "PASIVO", "ACREEDORA", "BOB");
        MAPEO.put("SALDO_USUARIOS.BOB", "SU.BOB." + sufijo);
        MAPEO.put("SALDO_GRUPOS.BOB", "SG.BOB." + sufijo);
        libroReal = new LibroDeBilletera();
        consumo = armar(libroReal, MAPEO);
    }

    /** El consumidor armado a mano, con el libro y el mapeo que se le pasen (un libro que falla a mitad, un plan en USD). */
    protected static ConsumoDeAportes armar(LibroDeBilletera libro, Map<String, String> mapeo) {
        return new ConsumoDeAportes(
                new Datos(dsl),
                new DestinoRepositorio(),
                new CuentaBilleteraRepositorio(),
                libro,
                new CU24RegistrarAsiento(
                        new CuentaContableRepositorio(),
                        new AsientoRepositorio(),
                        new Outbox("nucleo_financiero"),
                        Reloj.delSistema()),
                (logica, moneda) -> Optional.ofNullable(mapeo.get(logica + "." + moneda.name())),
                new CuentasDeSistemaRepositorio(),
                new Outbox("nucleo_financiero"),
                Reloj.delSistema());
    }

    /** Todo lo que un aporte necesita: el participante con su billetera, la obligacion real y la billetera de SU grupo. */
    protected record Aporte(UUID usuario, UUID obligacion, UUID grupo, UUID billetera, UUID billeteraDelGrupo) {}

    protected Aporte aporte(String saldoInicial) {
        UUID usuario = fixtura.usuario();
        UUID billetera = fixtura.billetera(usuario, "ESTANDAR", new BigDecimal(saldoInicial));
        UUID obligacion = obligaciones.obligacionDeAporte(usuario);
        UUID grupo = (UUID) dsl.fetchOne("SELECT grupo_id FROM aportes.obligacion_aporte WHERE id = ?", obligacion)
                .get(0);
        return new Aporte(usuario, obligacion, grupo, billetera, billeteraDelGrupo(grupo, "BOB", false));
    }

    /** La billetera {@code GRUPO} del grupo en esa moneda. {@code sobregiro} solo para probar monedas sin libro de fondeo. */
    protected UUID billeteraDelGrupo(UUID grupo, String moneda, boolean sobregiro) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO nucleo_financiero.cuenta_billetera
                    (id, numero_cuenta, tipo, grupo_id, moneda, estado, nivel_debida_diligencia,
                     saldo_disponible, saldo_retenido, permite_saldo_negativo, fecha_apertura, version)
                VALUES (?, ?, 'GRUPO', ?, ?, 'ACTIVA', 'ESTANDAR', 0, 0, ?, now(), 0)
                """,
                id,
                "GRP-" + id.toString().substring(0, 12),
                grupo,
                moneda,
                sobregiro);
        return id;
    }

    /** Una billetera de usuario en dolares (el libro de fondeo es en bolivianos: se prueba con sobregiro declarado). */
    protected UUID billeteraUsdDe(UUID usuario) {
        UUID id = UUID.randomUUID();
        dslFixtura.execute(
                """
                INSERT INTO nucleo_financiero.cuenta_billetera
                    (id, numero_cuenta, tipo, usuario_id, moneda, estado, nivel_debida_diligencia,
                     saldo_disponible, saldo_retenido, permite_saldo_negativo, fecha_apertura, version)
                VALUES (?, ?, 'USUARIO', ?, 'USD', 'ACTIVA', 'ESTANDAR', 0, 0, true, now(), 0)
                """,
                id,
                "USD-" + id.toString().substring(0, 12),
                usuario);
        return id;
    }

    protected AporteCobrado cobrado(Aporte a, String monto) {
        return cobrado(a, monto, "BOB", "BILLETERA_MOVIL");
    }

    protected AporteCobrado cobrado(Aporte a, String monto, String moneda, String canal) {
        return new AporteCobrado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                a.obligacion(),
                a.grupo(),
                a.usuario(),
                Dinero.de(monto, Moneda.valueOf(moneda)),
                canal);
    }

    /** El mismo hecho (mismo pago) con OTRO id de evento: lo que pasa si el productor republica. */
    protected AporteCobrado reenviado(AporteCobrado original) {
        return new AporteCobrado(
                UUID.randomUUID(),
                original.correlationId(),
                original.pagoId(),
                original.obligacionId(),
                original.grupoId(),
                original.usuarioId(),
                original.monto(),
                original.canal());
    }

    protected ConsumoDeAportes.Salida procesar(AporteCobrado a) {
        return transaccion.execute(t -> consumo.procesar(a));
    }

    protected static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    // ------------------------------------------------------------------ lecturas para CUADRAR

    /** El saldo de una billetera segun el LIBRO (suma de movimientos), no segun la columna cache. */
    protected BigDecimal saldoDelLibro(UUID cuenta) {
        return (BigDecimal) dsl.fetchOne(
                        """
                        SELECT COALESCE(SUM(CASE WHEN sentido = 'CREDITO' THEN monto ELSE -monto END), 0)
                          FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id = ?
                        """,
                        cuenta)
                .get(0);
    }

    /** El saldo de una cuenta del plan segun el MAYOR (acreedora: haber menos debe), no segun {@code cuenta_contable.saldo}. */
    protected BigDecimal saldoDelMayor(String codigoDelPlan) {
        return (BigDecimal) dsl.fetchOne(
                        """
                        SELECT COALESCE(SUM(m.haber - m.debe), 0) FROM nucleo_financiero.movimiento_contable m
                          JOIN nucleo_financiero.cuenta_contable c ON c.id = m.cuenta_id WHERE c.codigo = ?
                        """,
                        codigoDelPlan)
                .get(0);
    }

    protected int transacciones(UUID obligacion) {
        return contar(
                """
                SELECT count(*) FROM nucleo_financiero.transaccion_billetera WHERE tipo = 'APORTE_A_GRUPO' AND origen_id = ?
                """,
                obligacion);
    }

    protected int asientosDeAportes(UUID grupo) {
        return contar(
                "SELECT count(*) FROM nucleo_financiero.asiento_contable WHERE origen_tipo = 'PAGO' AND grupo_id = ?",
                grupo);
    }

    protected int marcas(UUID evento) {
        return contar("SELECT count(*) FROM nucleo_financiero.evento_consumido WHERE id_evento = ?", evento);
    }
}

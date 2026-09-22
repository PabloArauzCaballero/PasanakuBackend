package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo.EntradaSolicitud;
import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo.SalidaSolicitud;
import bo.aportaya.nucleofinanciero.aplicacion.CU15EmitirExtracto.EntradaExtracto;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H4.S2.M5 (IDOR) · un permiso de ROL ("puedo operar billeteras") no es lo mismo que un
 * permiso sobre el RECURSO ("puedo operar ESTA billetera").
 *
 * <p><b>Hallazgo que cambió como está escrito este archivo</b> (documentado tambien en
 * el daily, no escondido): la primera version probaba esto llamando a los casos de uso
 * a traves de {@code BaseDeBilletera.dsl}/{@code transaccion} — la misma conexion que
 * usa TODO el resto de la suite de este modulo. Esa conexion es la del ROL DUEÑO de la
 * base (`BaseDePrueba.conexion()`, Javadoc: "el rol dueño"), que en PostgreSQL
 * **bypasea RLS incondicionalmente**, `FORCE ROW LEVEL SECURITY` o no. Con esa
 * conexion, las cuatro pruebas de aca pasaban SIEMPRE sin importar si la política de
 * fila de verdad protege algo — un falso negativo silencioso, no una prueba real.
 * Confirmado con una consulta directa: {@code svc_nucleo_financiero} (el rol con el
 * que arranca el servicio de verdad, `application.yml: spring.datasource.username`)
 * {@code rolbypassrls = false, rolsuper = false} — en produccion RLS SI aplica. El
 * hueco era del arnes de prueba, no del sistema. Mismo patron que ya uso
 * {@code AislamientoEsquemaTest} (carril PR4-seguridad, Marcelo): {@code SET ROLE
 * svc_nucleo_financiero} + fijar {@code app.usuario_id}/{@code app.rol} a mano, EXACTO
 * lo que {@code Datos.conContexto} hace, sobre una conexion cruda — es la unica forma
 * de que esta suite vea lo que un cliente HTTP real veria.
 *
 * <p>Las tres primeras pruebas van contra la POLITICA (SQL crudo, `SET ROLE`); la
 * cuarta ({@code emitirExtractoAjeno}) va contra el CASO DE USO (por el camino normal
 * de la suite): CU-15 duplica la comprobacion de propiedad EN JAVA (AP-CU15-03,
 * defensa en profundidad), asi que esa SI se puede probar sin `SET ROLE` — no depende
 * de RLS para funcionar.
 *
 * <p><b>Fuera de este archivo, y a proposito</b>: la aprobacion de retiros (H3.S2,
 * {@code CU11AprobacionTest}) NO es un caso de IDOR — {@code RETIRO_APROBAR} es, por
 * diseño, una autoridad TRANSVERSAL a toda cuenta (un oficial de tesoreria aprueba
 * retiros ajenos, esa es la funcion), asi que no hay "propietario" contra quien
 * comparar. Lo unico que ahi se exige es solicitante ≠ aprobador, y eso ya esta
 * probado.
 */
class AutorizacionNegativaTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";
    private static final String ROL_APLICACION = "svc_nucleo_financiero";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private record Escenario(UUID usuario, UUID cuenta, ContextoSesion ctx) {}

    private Escenario deA() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("RECARGA", ESTANDAR, "MES", new BigDecimal("10000.00"), null);
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        return new Escenario(usuario, cuenta, contextoDe(usuario));
    }

    private Escenario deB() {
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        return new Escenario(usuario, cuenta, contextoDe(usuario));
    }

    /**
     * Una conexion cruda, como {@code svc_nucleo_financiero} veria una peticion de
     * {@code comoUsuario}: {@code SET ROLE} + los mismos {@code set_config(..., true)}
     * que {@code Datos.conContexto} hace, dentro de la MISMA transaccion (por eso
     * {@code autoCommit=false} y todo corre antes del unico {@code commit()}/cierre —
     * {@code SET LOCAL} fuera de una transaccion no fija nada, WARNING silencioso).
     */
    private interface ConsultaComoRol {
        void ejecutar(Connection conexion) throws SQLException;
    }

    private void comoUsuario(UUID usuarioId, ConsultaComoRol consulta) throws SQLException {
        try (Connection conexion = BaseDePrueba.conexion()) {
            conexion.setAutoCommit(false);
            try (var st = conexion.createStatement()) {
                st.execute("SET ROLE " + ROL_APLICACION);
                st.execute("SELECT set_config('app.usuario_id', '" + usuarioId + "', true)");
                st.execute("SELECT set_config('app.rol', 'PARTICIPANTE', true)");
                st.execute("SELECT set_config('app.traza', '" + UUID.randomUUID() + "', true)");
            }
            consulta.ejecutar(conexion);
            conexion.rollback(); // nunca se confirma nada escrito por esta prueba
        }
    }

    private int contarComo(UUID usuarioId, String sql, Object... parametros) throws SQLException {
        int[] resultado = {-1};
        comoUsuario(usuarioId, conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                for (int i = 0; i < parametros.length; i++) {
                    ps.setObject(i + 1, parametros[i]);
                }
                var filas = ps.executeQuery();
                filas.next();
                resultado[0] = filas.getInt(1);
            }
        });
        return resultado[0];
    }

    @Test
    @DisplayName("RLS: B no ve la fila de cuenta_billetera de A, aunque svc_nucleo_financiero tenga el GRANT")
    void consultarSaldoAjeno() throws SQLException {
        Escenario a = deA();
        Escenario b = deB();
        fixtura.acreditar(a.cuenta(), new BigDecimal("777.00"));

        assertThat(contarComo(
                        b.usuario(),
                        "SELECT count(*)::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?",
                        a.cuenta()))
                .as("B, con su propio app.usuario_id fijado, no deberia ver la cuenta de A")
                .isZero();
        // Control: A SI ve su propia cuenta bajo la MISMA politica — el cero de
        // arriba es por propiedad, no porque svc_nucleo_financiero este mal armado.
        assertThat(contarComo(
                        a.usuario(),
                        "SELECT count(*)::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?",
                        a.cuenta()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("RLS: B no puede liberar (cerrarRetencion) una retencion_saldo de A — el UPDATE afecta cero filas")
    void liberarRetencionAjena() throws SQLException {
        Escenario a = deA();
        Escenario b = deB();
        fixtura.acreditar(a.cuenta(), new BigDecimal("500.00"));
        var retencion = transaccion.execute(t -> retencionCU.retener(
                bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo.EntradaRetencion.simple(
                        a.cuenta(), bob("100.00"), "COMISION_PENDIENTE"),
                a.ctx()));

        int[] filasAfectadas = {-1};
        comoUsuario(b.usuario(), conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(
                    "UPDATE nucleo_financiero.retencion_saldo SET estado = 'LIBERADA', liberada_en = now() "
                            + "WHERE id = ? AND estado = 'VIGENTE'")) {
                ps.setObject(1, retencion.retencionId());
                filasAfectadas[0] = ps.executeUpdate();
            }
        });

        assertThat(filasAfectadas[0])
                .as("B no puede tocar una retencion que no es suya — RLS la vuelve invisible para el UPDATE")
                .isZero();
        // Control, con la conexion admin (bypasea RLS a proposito: es lo que
        // confirma que el cero de arriba es autorizacion y no "no existe de verdad"):
        // la retencion de A SIGUE VIGENTE, intacta.
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id = ? AND estado ="
                                + " 'VIGENTE'",
                        retencion.retencionId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("RLS: B no ve la orden_recarga de A — acreditarRecarga no tiene a que orden aplicarse")
    void acreditarRecargaAjena() throws SQLException {
        Escenario a = deA();
        Escenario b = deB();
        SalidaSolicitud orden = transaccion.execute(t -> recargaCU.solicitar(
                new EntradaSolicitud("idor-recarga", a.cuenta(), bob("300.00"), bob("0.00"), "QR", Optional.empty()),
                a.ctx()));

        assertThat(contarComo(
                        b.usuario(),
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE id = ?",
                        orden.ordenRecargaId()))
                .as("B no deberia ver la orden de recarga de A — RLS la filtra por la cuenta_billetera del titular")
                .isZero();
        // Control: A si la ve.
        assertThat(contarComo(
                        a.usuario(),
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE id = ?",
                        orden.ordenRecargaId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "CU-15: emitirExtracto con el cuentaId de OTRO usuario y sin permiso delegado, rechazado en Java (AP-CU15-03)")
    void emitirExtractoAjeno() {
        Escenario a = deA();
        Escenario b = deB();

        assertThatThrownBy(() -> transaccion.execute(t -> extractoCU.emitir(
                        new EntradaExtracto(a.cuenta(), LocalDate.now().minusDays(1), LocalDate.now(), false),
                        b.ctx())))
                .as("B no puede emitir el extracto de A sin BILLETERA_VER_TERCEROS")
                .isInstanceOf(ErrorDeNegocio.class);
    }
}

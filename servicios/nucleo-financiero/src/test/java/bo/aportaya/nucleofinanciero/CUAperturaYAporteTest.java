package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.AperturaDeBilleteras;
import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.Resultado;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.DestinoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril Q · la brecha de punta a punta: con las billeteras abiertas POR LOS CONSUMIDORES (no sembradas a mano por SQL) el
 * aporte cobrado se aplica y cuadra desde el libro y desde el mayor. Antes de este carril, sin billetera de grupo el
 * consumidor rechazaba todo aporte (AP-CU12-02).
 */
class CUAperturaYAporteTest extends BaseDeAportes {

    private static AperturaDeBilleteras apertura;

    @BeforeAll
    static void armarApertura() {
        fixtura.politica();
        apertura = new AperturaDeBilleteras(
                new Datos(dsl),
                new CuentaBilleteraRepositorio(),
                new DestinoRepositorio(),
                new Outbox("nucleo_financiero"),
                Reloj.delSistema(),
                "ACTIVA");
    }

    private UUID cuentaDeUsuario(UUID usuario) {
        return (UUID) dsl.fetchOne(
                        "SELECT id FROM nucleo_financiero.cuenta_billetera WHERE tipo = 'USUARIO' AND usuario_id = ?",
                        usuario)
                .get(0);
    }

    private UUID cuentaDeGrupo(UUID grupo) {
        return (UUID) dsl.fetchOne(
                        "SELECT id FROM nucleo_financiero.cuenta_billetera WHERE tipo = 'GRUPO' AND grupo_id = ?",
                        grupo)
                .get(0);
    }

    @Test
    @DisplayName(
            "cuadre: persona y grupo con billetera abierta por los consumidores · el aporte cobrado de Bs 500 se aplica;"
                    + " el libro sube el grupo y baja a la persona, y el mayor cuadra (debe = haber) por el mismo importe")
    void elAporteSeAplicaSobreBilleterasAbiertasPorElSistema() {
        UUID usuario = fixtura.usuario();
        UUID obligacion = obligaciones.obligacionDeAporte(usuario);
        UUID grupo = (UUID) dsl.fetchOne("SELECT grupo_id FROM aportes.obligacion_aporte WHERE id = ?", obligacion)
                .get(0);
        var deLaPersona = transaccion.execute(t -> apertura.abrirLaDeUsuario(
                new AperturaDeBilleteras.UsuarioRegistrado(UUID.randomUUID(), UUID.randomUUID(), usuario)));
        var deGrupo = transaccion.execute(t -> apertura.abrirLaDeGrupo(
                new AperturaDeBilleteras.GrupoCreado(UUID.randomUUID(), UUID.randomUUID(), grupo)));
        UUID cuentaPersona = deLaPersona.cuentaId().orElseThrow();
        UUID cuentaGrupo = deGrupo.cuentaId().orElseThrow();
        assertThat(cuentaDeUsuario(usuario)).isEqualTo(cuentaPersona);
        assertThat(cuentaDeGrupo(grupo)).isEqualTo(cuentaGrupo);
        fixtura.acreditar(cuentaPersona, new BigDecimal("1000.00"));

        var e = cobrado(new Aporte(usuario, obligacion, grupo, cuentaPersona, cuentaGrupo), "500.00");
        var salida = procesar(e);

        assertThat(salida.resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(saldoDelLibro(cuentaPersona)).isEqualByComparingTo("500.00");
        assertThat(saldoDelLibro(cuentaGrupo)).isEqualByComparingTo("500.00");
        // Desde el MAYOR: pasivo de usuarios baja 500 y el de grupos sube 500; la suma de las dos cuentas no cambia.
        assertThat(saldoDelMayor(MAPEO.get("SALDO_USUARIOS.BOB"))).isEqualByComparingTo("-500.00");
        assertThat(saldoDelMayor(MAPEO.get("SALDO_GRUPOS.BOB"))).isEqualByComparingTo("500.00");
        assertThat((BigDecimal) dsl.fetchOne(
                                """
                                SELECT SUM(m.debe) - SUM(m.haber) FROM nucleo_financiero.movimiento_contable m
                                  JOIN nucleo_financiero.asiento_contable a ON a.id = m.asiento_id WHERE a.id = ?
                                """,
                                salida.asientoId().orElseThrow())
                        .get(0))
                .isEqualByComparingTo("0");
        // Duplicado del mismo evento: nada nuevo.
        assertThat(procesar(e).resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(transacciones(obligacion)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "reintento: el aporte rechazado por falta de billetera del grupo (AP-CU12-02, sin marca) se aplica en el"
                    + " reproceso cuando el consumidor de grupo_creado la abre")
    void elReprocesoAplicaCuandoSeAbreLaDelGrupo() {
        UUID usuario = fixtura.usuario();
        UUID obligacion = obligaciones.obligacionDeAporte(usuario);
        UUID grupo = (UUID) dsl.fetchOne("SELECT grupo_id FROM aportes.obligacion_aporte WHERE id = ?", obligacion)
                .get(0);
        UUID cuentaPersona = fixtura.billetera(usuario, "ESTANDAR", new BigDecimal("800.00"));
        var e = cobrado(new Aporte(usuario, obligacion, grupo, cuentaPersona, null), "300.00");

        assertThatThrownBy(() -> procesar(e)).isInstanceOf(ErrorDeNegocio.class).satisfies(x -> assertThat(codigo(x))
                .isEqualTo("AP-CU12-02"));
        assertThat(marcas(e.eventoId())).isZero();

        var abierta = transaccion.execute(t -> apertura.abrirLaDeGrupo(
                new AperturaDeBilleteras.GrupoCreado(UUID.randomUUID(), UUID.randomUUID(), grupo)));

        assertThat(procesar(e).resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(saldoDelLibro(abierta.cuentaId().orElseThrow())).isEqualByComparingTo("300.00");
        assertThat(saldoDelLibro(cuentaPersona)).isEqualByComparingTo("500.00");
    }
}

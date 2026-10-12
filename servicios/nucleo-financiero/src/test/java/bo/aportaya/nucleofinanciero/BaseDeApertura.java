package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.AperturaDeBilleteras;
import bo.aportaya.nucleofinanciero.aplicacion.AperturaDeBilleteras.GrupoCreado;
import bo.aportaya.nucleofinanciero.aplicacion.AperturaDeBilleteras.UsuarioRegistrado;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.DestinoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;

/**
 * El armado comun de las pruebas de apertura de billeteras (carril Q): el consumidor construido a mano, el productor como
 * doble de CONTRATO (los eventos se arman con los campos exactos de {@code CU01RegistrarUsuario} y {@code CU20CrearGrupo}) y
 * las lecturas con las que se comprueba lo abierto.
 */
abstract class BaseDeApertura extends BaseDeBilletera {

    protected static AperturaDeBilleteras apertura;

    @BeforeAll
    static void armarApertura() {
        // La cuenta de una persona exige una politica vigente en BOB: la fixtura la deja.
        fixtura.politica();
        apertura = aperturaCon("ACTIVA", new CuentaBilleteraRepositorio());
    }

    protected static AperturaDeBilleteras aperturaCon(String estadoInicial, CuentaBilleteraRepositorio cuentas) {
        return new AperturaDeBilleteras(
                new Datos(dsl),
                cuentas,
                new DestinoRepositorio(),
                new Outbox("nucleo_financiero"),
                Reloj.delSistema(),
                estadoInicial);
    }

    protected UsuarioRegistrado registrado(UUID usuario) {
        return new UsuarioRegistrado(UUID.randomUUID(), UUID.randomUUID(), usuario);
    }

    protected GrupoCreado creado(UUID grupo) {
        return new GrupoCreado(UUID.randomUUID(), UUID.randomUUID(), grupo);
    }

    protected AperturaDeBilleteras.Salida abrir(AperturaDeBilleteras servicio, UsuarioRegistrado e) {
        return transaccion.execute(t -> servicio.abrirLaDeUsuario(e));
    }

    protected AperturaDeBilleteras.Salida abrir(AperturaDeBilleteras servicio, GrupoCreado e) {
        return transaccion.execute(t -> servicio.abrirLaDeGrupo(e));
    }

    protected int cuentasDeUsuario(UUID usuario) {
        return contar(
                "SELECT count(*) FROM nucleo_financiero.cuenta_billetera WHERE tipo = 'USUARIO' AND usuario_id = ?",
                usuario);
    }

    protected int cuentasDeGrupo(UUID grupo) {
        return contar(
                "SELECT count(*) FROM nucleo_financiero.cuenta_billetera WHERE tipo = 'GRUPO' AND grupo_id = ?", grupo);
    }

    protected int marcas(UUID evento) {
        return contar("SELECT count(*) FROM nucleo_financiero.evento_consumido WHERE id_evento = ?", evento);
    }

    protected int avisosDeApertura(UUID cuenta) {
        return contar(
                """
                SELECT count(*) FROM nucleo_financiero.evento_dominio
                 WHERE tipo = 'nucleo_financiero.billetera_abierta' AND agregado_id = ?
                """,
                cuenta);
    }

    /** Un grupo real de la cadena de fixturas (la cuenta del grupo lo referencia por clave foranea). */
    protected UUID grupoReal() {
        UUID obligacion = obligaciones.obligacionDeAporte(fixtura.usuario());
        return (UUID) dsl.fetchOne("SELECT grupo_id FROM aportes.obligacion_aporte WHERE id = ?", obligacion)
                .get(0);
    }
}

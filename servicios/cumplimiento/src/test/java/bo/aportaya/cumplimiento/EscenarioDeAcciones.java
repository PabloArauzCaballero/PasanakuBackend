package bo.aportaya.cumplimiento;

import bo.aportaya.cumplimiento.aplicacion.AccionesDeCumplimiento;
import bo.aportaya.cumplimiento.infraestructura.AccionesDeCumplimientoRepositorio;
import bo.aportaya.cumplimiento.infraestructura.ReporteRegulatorioRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;

/**
 * Las acciones HTTP del backoffice (carril A · H9) contra la base real: los casos de uso son los mismos que
 * arma {@link BaseDeCumplimiento}; lo que se prueba es la capa que pone el actor de la sesion y vuelve
 * seguro el reintento.
 */
abstract class EscenarioDeAcciones extends EscenarioDeColas {

    protected static AccionesDeCumplimiento acciones() {
        return new AccionesDeCumplimiento(
                new Datos(dsl),
                reporteCU,
                oficioCU,
                continuidadCU,
                new ReporteRegulatorioRepositorio(),
                new AccionesDeCumplimientoRepositorio());
    }

    protected static ContextoSesion operador(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }
}

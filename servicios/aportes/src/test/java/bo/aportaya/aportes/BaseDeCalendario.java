package bo.aportaya.aportes;

import bo.aportaya.aportes.aplicacion.CalendarioDelPeriodo;
import bo.aportaya.aportes.aplicacion.GenerarObligacionesDelPeriodo;
import bo.aportaya.aportes.aplicacion.RegistroDelCalendario;
import bo.aportaya.aportes.infraestructura.CalendarioDeObligacionesRepositorio;
import bo.aportaya.aportes.infraestructura.FeriadosRepositorio;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;

/** El armado comun de las pruebas del calendario de obligaciones: el doble de `grupos` y los ayudantes. */
abstract class BaseDeCalendario extends BaseDeAportes {

    /** El doble de `grupos`: responde lo que se le diga, y cuenta cuantas veces le preguntaron. */
    protected static final class GruposDoble implements CalendarioDelPeriodo {
        Optional<Periodo> respuesta = Optional.empty();
        int preguntas;

        @Override
        public Optional<Periodo> deLosCupos(UUID periodoId) {
            preguntas++;
            return respuesta;
        }
    }

    protected final GruposDoble grupos = new GruposDoble();

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    protected GenerarObligacionesDelPeriodo generador() {
        Datos datos = new Datos(dsl);
        return new GenerarObligacionesDelPeriodo(
                grupos,
                new RegistroDelCalendario(
                        datos,
                        new FeriadosRepositorio(),
                        new CalendarioDeObligacionesRepositorio(),
                        new ObligacionRepositorio(),
                        new Outbox("aportes")));
    }

    protected static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    /** Lo que `grupos` afirmaria de ese periodo: cupos ocupados con Bs 500 cada uno. */
    protected CalendarioDelPeriodo.Periodo afirma(
            FixturaDeAportes.PeriodoDePrueba p, LocalDate limite, LocalDate gracia) {
        var cupos = p.cupos().stream()
                .map(c -> new CalendarioDelPeriodo.Cupo(c.cupoId(), c.participanteId(), bob("500.00")))
                .toList();
        var periodo = new CalendarioDelPeriodo.Periodo(p.periodoId(), p.grupoId(), 1, "ABIERTO", limite, gracia, cupos);
        grupos.respuesta = Optional.of(periodo);
        return periodo;
    }

    protected GenerarObligacionesDelPeriodo.Salida generar(UUID periodoId) {
        return transaccion.execute(t -> generador().generar(periodoId, contextoDe(UUID.randomUUID())));
    }

    protected int eventosDeGeneracion(UUID periodoId) {
        return contar(
                "SELECT count(*)::int FROM aportes.evento_dominio WHERE tipo = 'aportes.obligaciones_generadas' AND agregado_id = ?",
                periodoId);
    }
}

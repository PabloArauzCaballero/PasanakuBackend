package bo.aportaya.publicidad;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.publicidad.aplicacion.CU110AltaDeAnunciante.EntradaAnunciante;
import bo.aportaya.publicidad.aplicacion.CU111CrearCampana.Entrada;
import bo.aportaya.publicidad.aplicacion.ConsultarListadosDePublicidad;
import bo.aportaya.publicidad.infraestructura.ListadosDePublicidadRepositorio;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

/**
 * Backoffice carril A · H7.S1 · escenario comun de los listados de publicidad (CU-110/111/112).
 *
 * <p>Las tablas son globales y otras clases dejan filas: las campanas y piezas de estas pruebas viven
 * en ventanas de tiempo que nadie mas usa (2091 y 2001) y el cursor arranca justo antes. Los
 * anunciantes se listan del mas nuevo al mas viejo, asi que los recien creados van primero.
 */
abstract class EscenarioDeListados extends EscenarioDeCampana {

    protected static final UUID ANTES_DE_TODO = new UUID(0L, 0L);
    protected ConsultarListadosDePublicidad listados;

    @BeforeEach
    void armarListados() {
        listados = new ConsultarListadosDePublicidad(new Datos(dsl), new ListadosDePublicidadRepositorio());
    }

    protected UUID otroAnunciante(String razonSocial) {
        return transaccion
                .execute(t -> anuncianteCU.darDeAlta(
                        new EntradaAnunciante(
                                "ORGANIZADOR", fixtura.organizador("HABILITADO"), null, razonSocial, null, "BOB"),
                        operaciones))
                .anuncianteId();
    }

    protected UUID campanaQueEmpieza(OffsetDateTime inicio) {
        Entrada base = campana("900.00", "30.00", "0.5000", "CPM");
        Entrada fechada = new Entrada(
                base.cuentaPublicitariaId(),
                base.nombre(),
                base.objetivo(),
                base.presupuestoTotal(),
                base.moneda(),
                inicio,
                inicio.plusDays(30),
                base.conjuntos());
        return transaccion.execute(t -> campanaCU.crear(fechada, operaciones)).campanaPublicitariaId();
    }
}

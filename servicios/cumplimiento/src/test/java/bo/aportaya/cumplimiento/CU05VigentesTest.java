package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.dominio.ContratoPublicado;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-05 · que contratos rigen hoy.
 *
 * <p><b>Por que existe.</b> {@code aceptaContratos} de CU-01 son UUID, y los ids de
 * {@code contrato_adhesion} son {@code gen_random_uuid()}: uno distinto por entorno. Sin
 * forma de preguntarlos, la app no podia armar el alta y mandaba codigos inventados que
 * el servidor rechazaba al deserializar — el alta entera no llegaba al backend.
 */
class CU05VigentesTest extends BaseDeCumplimiento {

    @AfterEach
    void limpiar() {
        dslFixtura.execute("DELETE FROM cumplimiento.aceptacion_contrato");
        dslFixtura.execute("DELETE FROM cumplimiento.contrato_adhesion");
    }

    private List<ContratoPublicado> vigentes() {
        return transaccion.execute(e -> contratosVigentesCU.ejecutar(contexto()));
    }

    @Test
    @DisplayName("Dado un contrato VIGENTE de cada tipo · Cuando se consultan · Entonces salen todos con su id")
    void devuelveUnoPorTipo() {
        UUID billetera = fixtura.contrato("BILLETERA", 1, "VIGENTE");
        UUID tarifas = fixtura.contrato("TARIFAS", 1, "VIGENTE");
        UUID datos = fixtura.contrato("TRATAMIENTO_DATOS", 1, "VIGENTE");

        assertThat(vigentes()).extracting(ContratoPublicado::id).containsExactlyInAnyOrder(billetera, tarifas, datos);
    }

    @Test
    @DisplayName("Dado un contrato en BORRADOR · Cuando se consultan · Entonces no aparece")
    void elBorradorNoSePublica() {
        fixtura.contrato("BILLETERA", 1, "BORRADOR");

        // Un contrato que todavia no rige no se puede aceptar. Ofrecerlo seria pedir
        // que se acepte algo que no obliga a nadie.
        assertThat(vigentes()).isEmpty();
    }

    @Test
    @DisplayName("Dadas dos versiones VIGENTES del mismo tipo · Cuando se consultan · Entonces gana la mas alta")
    void conDosVigentesGanaLaVersionMasAlta() {
        fixtura.contrato("BILLETERA", 2, "VIGENTE");
        UUID nueva = fixtura.contrato("BILLETERA", 5, "VIGENTE");

        // Es un estado que la base no deberia permitir, pero si lo deja pasar nadie
        // queda atado a la version vieja de dos.
        assertThat(vigentes()).singleElement().satisfies(c -> {
            assertThat(c.id()).isEqualTo(nueva);
            assertThat(c.version()).isEqualTo((short) 5);
        });
    }

    @Test
    @DisplayName("Dado un contrato sin registro ante ASFI · Cuando se consulta · Entonces el numero viene vacio")
    void sinRegistroNoSeInventaUnNumero() {
        fixtura.contrato("BILLETERA", 1, "VIGENTE");

        assertThat(vigentes()).singleElement().satisfies(c -> {
            assertThat(c.numeroRegistro()).isEmpty();
            assertThat(c.fechaRegistro()).isEmpty();
            // Lo que si tiene que venir: con que comprobar que el texto leido
            // es el mismo que quedara en la evidencia de la aceptacion.
            assertThat(c.hashDocumento()).hasSize(64);
            assertThat(c.urlDocumento()).isNotBlank();
        });
    }
}

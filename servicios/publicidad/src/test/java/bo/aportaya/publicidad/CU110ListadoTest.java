package bo.aportaya.publicidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.publicidad.aplicacion.ConsultarListadosDePublicidad.FilaAnunciante;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Backoffice carril A · H7.S1 · el listado de anunciantes (CU-110) contra PostgreSQL real; escenario en {@link EscenarioDeListados}. */
class CU110ListadoTest extends EscenarioDeListados {

    @Test
    @DisplayName(
            "Dados tres anunciantes recien dados de alta · Cuando Operaciones recorre el listado de a dos · Entonces"
                    + " los ve del mas nuevo al mas viejo, con su cuenta y su limite, sin repetir ninguno")
    void anunciantesDelMasNuevo() {
        List<UUID> creados = new ArrayList<>(List.of(anuncianteId));
        creados.add(otroAnunciante("Panaderia del grupo"));
        creados.add(otroAnunciante("Ferreteria del grupo"));

        var primera = transaccion.execute(
                t -> listados.anunciantes(null, Optional.empty(), Optional.empty(), 2, operaciones));
        var segunda = transaccion.execute(
                t -> listados.anunciantes(null, primera.proximoFecha(), primera.proximoId(), 2, operaciones));

        assertThat(primera.items())
                .extracting(FilaAnunciante::anuncianteId)
                .containsExactly(creados.get(2), creados.get(1));
        assertThat(segunda.items().get(0).anuncianteId()).isEqualTo(creados.get(0));
        var conLimite = segunda.items().get(0);
        assertThat(conLimite.cuentaPublicitariaId()).contains(cuentaId);
        assertThat(conLimite.limiteGastoMensual())
                .hasValueSatisfying(l -> assertThat(l).isEqualByComparingTo("9000.00"));
        assertThat(conLimite.moneda()).contains("BOB");
        assertThat(primera.items().get(0).limiteGastoMensual()).isEmpty();
    }

    @Test
    @DisplayName("rechaza un filtro invalido o un cursor a medias con AP-CU110-04")
    void filtroInvalido() {
        assertThatThrownBy(() -> transaccion.execute(t ->
                        listados.anunciantes(List.of("BORRADO"), Optional.empty(), Optional.empty(), 25, operaciones)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU110-04"));
    }
}

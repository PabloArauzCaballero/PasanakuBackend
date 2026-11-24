package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.ListarUsuariosInternos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** GU-17 · el cursor del listado de personal interno va completo o no va. Se rechaza antes de tocar la base. */
class ListarUsuariosInternosTest {

    private final ListarUsuariosInternos listar = new ListarUsuariosInternos(null, null, Reloj.delSistema());
    private final ContextoSesion ctx = ContextoSesion.de(
            UUID.randomUUID(), "BACKOFFICE", new Traza(UUID.randomUUID().toString()));

    @Test
    @DisplayName("cursor a medias: antesDe sin antesDeId es AP-CU08-12")
    void soloLaFecha() {
        assertThatThrownBy(() -> listar.listar(null, null, null, 10, OffsetDateTime.now(), null, ctx))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU08-12"));
    }

    @Test
    @DisplayName("cursor a medias: antesDeId sin antesDe es AP-CU08-12")
    void soloElId() {
        assertThatThrownBy(() -> listar.listar(null, null, null, 10, null, UUID.randomUUID(), ctx))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU08-12"));
    }
}

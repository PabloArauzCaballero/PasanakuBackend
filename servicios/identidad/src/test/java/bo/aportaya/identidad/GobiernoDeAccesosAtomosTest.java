package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.dominio.ContinuidadDeAdministracion;
import bo.aportaya.identidad.dominio.HechoDeAcceso;
import bo.aportaya.identidad.dominio.MotivoDeRevocacion;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Los atomos puros del gobierno de accesos (GU-07, GU-11, GU-14). */
class GobiernoDeAccesosAtomosTest {

    private static final UUID ACTOR = UUID.fromString("77777777-8888-4999-8aaa-bbbbbbbbbbbb");
    private static final UUID AFECTADO = UUID.fromString("88888888-9999-4aaa-8bbb-cccccccccccc");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-24T12:00:00Z");

    @Test
    @DisplayName("bitacora: la carga v1 lleva quien, que, a quien, motivo y cuando, sin nulos")
    void cargaDelHecho() {
        var hecho = new HechoDeAcceso(
                HechoDeAcceso.Accion.ROL_REVOCADO,
                Optional.of(ACTOR),
                AFECTADO,
                "Cambio de funcion aprobado",
                AHORA,
                Map.of("sesionesCerradas", 2));

        assertThat(hecho.carga())
                .containsEntry("version", "1")
                .containsEntry("accion", "ROL_REVOCADO")
                .containsEntry("actorId", ACTOR.toString())
                .containsEntry("afectadoId", AFECTADO.toString())
                .containsEntry("motivo", "Cambio de funcion aprobado")
                .containsEntry("ocurridoEn", "2026-11-24T12:00Z")
                .containsEntry("sesionesCerradas", 2);
        assertThat(hecho.accion().tipoDeEvento()).isEqualTo("identidad.rol_revocado");
        assertThat(hecho.accion().esDeAsignacion()).isTrue();
    }

    @Test
    @DisplayName("bitacora: el vencimiento no tiene actor y la carga no inventa uno")
    void vencimientoSinActor() {
        var hecho = new HechoDeAcceso(
                HechoDeAcceso.Accion.ROL_VENCIDO, Optional.empty(), AFECTADO, "VENCIMIENTO", AHORA, Map.of());

        assertThat(hecho.carga()).doesNotContainKey("actorId");
        assertThat(HechoDeAcceso.Accion.OPERADOR_DADO_DE_BAJA.esDeAsignacion()).isFalse();
    }

    @Test
    @DisplayName("nunca sin administrador: solo se rechaza el acto que baja el numero a cero")
    void continuidad() {
        assertThat(ContinuidadDeAdministracion.laDejaSinAdministrador(1, 0)).isTrue();
        assertThat(ContinuidadDeAdministracion.laDejaSinAdministrador(2, 1)).isFalse();
        assertThat(ContinuidadDeAdministracion.laDejaSinAdministrador(0, 0)).isFalse();
        assertThat(ContinuidadDeAdministracion.laDejaSinAdministrador(1, 1)).isFalse();
    }

    @Test
    @DisplayName("motivo: se recorta a la columna y conserva el prefijo del acto")
    void motivoRecortado() {
        String largo = "x".repeat(200);

        assertThat(MotivoDeRevocacion.de("SUSPENSION", largo, MotivoDeRevocacion.LARGO_EN_ASIGNACION))
                .hasSize(120)
                .startsWith("SUSPENSION: ");
        assertThat(MotivoDeRevocacion.de("", "corto", MotivoDeRevocacion.LARGO_EN_SESION))
                .isEqualTo("corto");
    }
}

package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.aplicacion.ConsultarDetalleDelGrupo;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Perfil del integrante (2026-10-21): {@code GET /grupos/{grupoId}/integrantes/{usuarioId}/comparte} sobre PostgreSQL
 * real. Es la respuesta en la que identidad y transparencia se apoyan para mostrar el telefono y la reputacion de
 * alguien: tiene que ser «si» solo cuando los dos estan en el grupo. Datos sinteticos.
 */
class PertenenciaCompartidaRepositorioTest extends BaseDeCU60 {

    private ConsultarDetalleDelGrupo detalle;

    @BeforeEach
    void preparar() {
        detalle = new ConsultarDetalleDelGrupo(new Datos(dsl));
    }

    private static ContextoSesion de(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    private UUID integrante(UUID grupo, String estado) {
        UUID usuario = fixtura.usuario();
        dsl.execute(
                """
                INSERT INTO grupos.participante
                  (id, grupo_id, usuario_id, alias, estado, es_organizador, fecha_ingreso,
                   reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                VALUES (gen_random_uuid(), ?, ?, 'Integrante de prueba', ?, false, now(), 0, 0, 0)
                """,
                grupo,
                usuario,
                estado);
        return usuario;
    }

    private boolean comparten(UUID grupo, UUID quienPregunta, UUID otro) {
        return transaccion.execute(s -> detalle.comparten(grupo, otro, de(quienPregunta)));
    }

    @Test
    @DisplayName("comparte · dos integrantes del grupo (activo, en mora, pendiente de firma) comparten grupo")
    void integrantesComparten() {
        UUID grupo = fixtura.grupoConformado(4);
        UUID ana = integrante(grupo, "ACTIVO");
        UUID beto = integrante(grupo, "EN_MORA");
        UUID carla = integrante(grupo, "ACEPTADO_PENDIENTE_FIRMA");

        assertThat(comparten(grupo, ana, beto)).isTrue();
        assertThat(comparten(grupo, beto, carla)).isTrue();
        assertThat(comparten(grupo, ana, ana)).as("uno mismo").isTrue();
    }

    @Test
    @DisplayName("rechaza · quien es de OTRO grupo no comparte con el integrante, aunque pregunte por este grupo")
    void deOtroGrupoNoComparte() {
        UUID grupo = fixtura.grupoConformado(3);
        UUID otroGrupo = fixtura.grupoConformado(3);
        UUID beto = integrante(grupo, "ACTIVO");
        UUID carla = integrante(otroGrupo, "ACTIVO");

        assertThat(comparten(grupo, carla, beto)).isFalse();
        assertThat(comparten(otroGrupo, carla, beto)).isFalse();
    }

    @Test
    @DisplayName("rechaza · quien se fue o todavia no entro (retirado, expulsado, postulante, invitado) no comparte")
    void quienNoEstaNoComparte() {
        UUID grupo = fixtura.grupoConformado(6);
        UUID ana = integrante(grupo, "ACTIVO");
        for (String estado : new String[] {"RETIRADO", "EXPULSADO", "REEMPLAZADO", "POSTULANTE", "INVITADO"}) {
            UUID fuera = integrante(grupo, estado);
            assertThat(comparten(grupo, fuera, ana)).as("pregunta " + estado).isFalse();
            assertThat(comparten(grupo, ana, fuera))
                    .as("preguntan por " + estado)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("rechaza · un grupo o una persona que no existen no comparten con nadie")
    void inexistenteNoComparte() {
        UUID grupo = fixtura.grupoConformado(3);
        UUID ana = integrante(grupo, "ACTIVO");

        assertThat(comparten(UUID.randomUUID(), ana, ana)).isFalse();
        assertThat(comparten(grupo, ana, UUID.randomUUID())).isFalse();
    }
}

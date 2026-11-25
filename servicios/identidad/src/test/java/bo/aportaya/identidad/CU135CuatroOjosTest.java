package bo.aportaya.identidad;

import static bo.aportaya.identidad.CU135GobernarRolesTest.codigoNuevo;
import static bo.aportaya.identidad.CU135GobernarRolesTest.pedido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.CU135GobernarRoles;
import bo.aportaya.identidad.aplicacion.CU135GobernarRoles.Resultado;
import bo.aportaya.identidad.infraestructura.RolesPersonalizadosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-135, la parte de cuatro ojos (GU-16): quien propone no resuelve, y el rol nace recien al aprobar. */
class CU135CuatroOjosTest extends BaseDeGobiernoCU08 {

    private static CU135GobernarRoles casoDeUso;

    @BeforeAll
    static void armarCU135() {
        casoDeUso = new CU135GobernarRoles(
                new Datos(dsl),
                new RolesPersonalizadosRepositorio(),
                accesos,
                DOBLE_DE_STEP_UP,
                new Outbox("identidad"),
                Reloj.delSistema());
    }

    @Test
    @DisplayName(
            "Dado un operador que crea un rol con un permiso de riesgo alto · Cuando otra persona aprueba la solicitud"
                    + " · Entonces el rol existe recien al aprobar y el evento nombra a quien propuso")
    void cuatroOjosCreaElRolAlAprobar() {
        enTransaccionQueSeDeshace(() -> {
            fixtura.permisoDelCatalogo("RETIRO_APROBAR", "orden_retiro", "APROBAR");
            UUID proponente = operador();
            UUID aprobador = operador();
            String codigo = codigoNuevo();

            Resultado pendiente = casoDeUso.crear(pedido(codigo, Set.of("RETIRO_APROBAR")), como(proponente));

            assertThat(pendiente.estado()).isEqualTo("PENDIENTE_DE_APROBACION");
            assertThat(pendiente.rolId()).isNull();
            assertThat(columna("SELECT count(*)::int FROM identidad.rol WHERE codigo = ?", codigo))
                    .isEqualTo(0);
            assertThat(eventos("identidad.rol_solicitado", pendiente.solicitudId()))
                    .isEqualTo(1);

            Resultado aprobado =
                    casoDeUso.aprobar(pendiente.solicitudId(), "Revisado y aprobado", EVIDENCIA, como(aprobador));

            assertThat(aprobado.estado()).isEqualTo("APLICADO");
            assertThat(columna("SELECT codigo FROM identidad.rol WHERE id = ?", aprobado.rolId()))
                    .isEqualTo(codigo);
            assertThat(columna(
                            "SELECT payload->>'propuestaPor' FROM identidad.evento_dominio WHERE agregado_id = ?",
                            aprobado.rolId()))
                    .isEqualTo(proponente.toString());
            assertThat(columna(
                            "SELECT estado FROM identidad.solicitud_cambio_rol WHERE id = ?", pendiente.solicitudId()))
                    .isEqualTo("APROBADA");
        });
    }

    @Test
    @DisplayName(
            "Dada una solicitud pendiente · Cuando la persona que la propuso intenta aprobarla · Entonces se rechaza"
                    + " con APROBADOR_ES_EL_PROPONENTE y sigue pendiente")
    void rechazaQueElProponenteResuelva() {
        enTransaccionQueSeDeshace(() -> {
            fixtura.permisoDelCatalogo("RETIRO_APROBAR", "orden_retiro", "APROBAR");
            UUID proponente = operador();
            Resultado pendiente = casoDeUso.crear(pedido(codigoNuevo(), Set.of("RETIRO_APROBAR")), como(proponente));

            assertThatThrownBy(() ->
                            casoDeUso.aprobar(pendiente.solicitudId(), "Me lo apruebo", EVIDENCIA, como(proponente)))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU135-12"));

            assertThat(columna(
                            "SELECT estado FROM identidad.solicitud_cambio_rol WHERE id = ?", pendiente.solicitudId()))
                    .isEqualTo("PENDIENTE");
        });
    }
}

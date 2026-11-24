package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.PedidoDeAsignacion;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.ResultadoDeAsignacion;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.ResultadoDeRevocacion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-08 por asignacion contra PostgreSQL: asignar, revocar y las reglas de gobierno (GU-05, GU-06, GU-07, GU-11). */
class CU08GobiernoTest extends BaseDeGobiernoCU08 {

    private static final String ADMINISTRAR = "ACCESOS_ADMINISTRAR";

    @Test
    @DisplayName(
            "Dado un operador activo sin el rol · Cuando el administrador se lo asigna por asignación con justificación y step-up · Entonces la asignación queda con quién y cuándo la otorgó · Y la bitácora registra rol_asignado con la justificación · Y se pide el aviso al afectado y al oficial de cumplimiento")
    void asignaYDejaRastro() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID oficial = operador();
            fixtura.asignarRol(oficial, rolLlamado("OFICIAL_CUMPLIMIENTO"), operador());
            UUID destinatario = operador();

            ResultadoDeAsignacion r = asignar(administrador, destinatario, rol("GU_B_SOPORTE_LEER"));

            assertThat(r.yaExistia()).isFalse();
            assertThat(columna("SELECT otorgada_por FROM identidad.asignacion_rol WHERE id = ?", r.asignacionId()))
                    .isEqualTo(administrador);
            assertThat(eventos("identidad.rol_asignado", r.asignacionId())).isEqualTo(1);
            assertThat(columna(
                            "SELECT payload->>'motivo' FROM identidad.evento_dominio WHERE agregado_id = ?",
                            r.asignacionId()))
                    .isEqualTo("Cobertura de funciones aprobada");
            assertThat(columna(
                            """
                            SELECT payload->>'actorId' || '|' || (payload->>'actorRol') || '|' || (payload->>'origen')
                              FROM identidad.evento_dominio WHERE agregado_id = ?
                            """,
                            r.asignacionId()))
                    .isEqualTo(administrador + "|BACKOFFICE|PANEL_ADMIN");
            String destinatarios = (String) columna(
                    """
                    SELECT payload->>'destinatarios' FROM identidad.evento_dominio
                     WHERE tipo = 'identidad.aviso_de_acceso_solicitado' AND agregado_id = ?
                    """,
                    destinatario);
            assertThat(destinatarios).contains(destinatario.toString()).contains(oficial.toString());
        });
    }

    @Test
    @DisplayName(
            "Dada una asignación viva igual · Cuando se repite la misma asignación · Entonces se devuelve la misma asignación sin escribir otra ni emitir otro evento")
    void asignarEsIdempotente() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID destinatario = operador();
            UUID rol = rol("GU_B_SOPORTE_LEER");

            ResultadoDeAsignacion primera = asignar(administrador, destinatario, rol);
            ResultadoDeAsignacion segunda = asignar(administrador, destinatario, rol);

            assertThat(segunda.asignacionId()).isEqualTo(primera.asignacionId());
            assertThat(segunda.yaExistia()).isTrue();
            assertThat(columna("SELECT count(*)::int FROM identidad.asignacion_rol WHERE usuario_id = ?", destinatario))
                    .isEqualTo(1);
            assertThat(eventos("identidad.rol_asignado", primera.asignacionId()))
                    .isEqualTo(1);
        });
    }

    @Test
    @DisplayName(
            "Dado un operador con dos roles · Cuando se le agrega un tercero · Entonces las asignaciones existentes conservan quién y cuándo las otorgó · Y sus sesiones siguen abiertas")
    void agregarNoReescribeNiEcha() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID otro = operador();
            UUID destinatario = operador();
            fixtura.factor(destinatario, "TOTP", true, true);
            ResultadoDeAsignacion primera = asignar(administrador, destinatario, rol("GU_B_LEER_A"));
            ResultadoDeAsignacion segunda = asignar(otro, destinatario, rol("GU_B_LEER_B"));
            OffsetDateTime otorgadaAntes = (OffsetDateTime)
                    columna("SELECT otorgada_en FROM identidad.asignacion_rol WHERE id = ?", primera.asignacionId());
            fixtura.sesionAbierta(destinatario);

            asignar(otro, destinatario, rol("GU_B_LEER_C"));

            assertThat(columna("SELECT otorgada_en FROM identidad.asignacion_rol WHERE id = ?", primera.asignacionId()))
                    .isEqualTo(otorgadaAntes);
            assertThat(columna(
                            "SELECT otorgada_por FROM identidad.asignacion_rol WHERE id = ?", segunda.asignacionId()))
                    .isEqualTo(otro);
            assertThat(sesionesVivas(destinatario)).isEqualTo(1);
        });
    }

    @Test
    @DisplayName(
            "Dado un acto de gobierno sin evidencia step-up válida · Cuando se intenta · Entonces se rechaza con STEP_UP_REQUERIDO y no se escribe nada")
    void sinStepUpNoHayActo() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID destinatario = operador();
            UUID rol = rol("GU_B_SOPORTE_LEER");

            assertThatThrownBy(() -> gobierno.asignar(
                            new PedidoDeAsignacion(
                                    destinatario,
                                    rol,
                                    "GLOBAL",
                                    Optional.empty(),
                                    Optional.empty(),
                                    "Cobertura de funciones aprobada",
                                    "evidencia-de-otro-o-vencida"),
                            como(administrador)))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .hasMessageContaining("segundo factor");
            assertThat(columna("SELECT count(*)::int FROM identidad.asignacion_rol WHERE usuario_id = ?", destinatario))
                    .isEqualTo(0);
        });
    }

    @Test
    @DisplayName(
            "Dado un administrador · Cuando intenta revocar su propia asignación · Entonces se rechaza con AUTOASIGNACION")
    void nadieSobreSiMismo() {
        enTransaccionQueSeDeshace(() -> {
            UUID otorgante = operador();
            UUID administrador = operador();
            ResultadoDeAsignacion propia = asignar(otorgante, administrador, rol(ADMINISTRAR));

            assertThatThrownBy(() -> gobierno.revocar(
                            propia.asignacionId(), "Me quito el acceso", true, EVIDENCIA, como(administrador)))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .hasMessageContaining("propio acceso");
        });
    }

    @Test
    @DisplayName(
            "Dado un único administrador de accesos activo · Cuando se revoca su asignación · Entonces se rechaza con ULTIMO_ADMINISTRADOR")
    void nuncaSinAdministrador() {
        enTransaccionQueSeDeshace(() -> {
            UUID otorgante = operador();
            UUID unico = operador();
            ResultadoDeAsignacion asignacion = asignar(otorgante, unico, rol(ADMINISTRAR));

            assertThatThrownBy(() -> gobierno.revocar(
                            asignacion.asignacionId(), "Cambio de funcion aprobado", true, EVIDENCIA, como(otorgante)))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .hasMessageContaining("administre accesos");
        });
    }

    @Test
    @DisplayName(
            "Dado un segundo administrador con la cuenta suspendida · Cuando se revoca la asignación del único administrador activo · Entonces se rechaza con ULTIMO_ADMINISTRADOR")
    void cuentaPersonasActivasNoFilas() {
        enTransaccionQueSeDeshace(() -> {
            UUID otorgante = operador();
            UUID activo = operador();
            UUID suspendido = operador();
            UUID rol = rol(ADMINISTRAR);
            ResultadoDeAsignacion delActivo = asignar(otorgante, activo, rol);
            asignar(otorgante, suspendido, rol);
            dsl.execute("UPDATE identidad.usuario SET estado = 'SUSPENDIDO' WHERE id = ?", suspendido);

            assertThatThrownBy(() -> gobierno.revocar(
                            delActivo.asignacionId(), "Cambio de funcion aprobado", true, EVIDENCIA, como(otorgante)))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .hasMessageContaining("administre accesos");
        });
    }

    @Test
    @DisplayName(
            "Dada una asignación revocada · Cuando se la revoca otra vez · Entonces responde que ya estaba revocada sin emitir otro evento")
    void revocarEsIdempotente() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID destinatario = operador();
            ResultadoDeAsignacion a = asignar(administrador, destinatario, rol("GU_B_SOPORTE_LEER"));

            ResultadoDeRevocacion primera = gobierno.revocar(
                    a.asignacionId(), "Cambio de funcion aprobado", true, EVIDENCIA, como(administrador));
            ResultadoDeRevocacion segunda =
                    gobierno.revocar(a.asignacionId(), "Otro motivo distinto", true, EVIDENCIA, como(administrador));

            assertThat(primera.yaEstaba()).isFalse();
            assertThat(segunda.yaEstaba()).isTrue();
            assertThat(segunda.revocadaEn()).isEqualTo(primera.revocadaEn());
            assertThat(columna("SELECT motivo_revocacion FROM identidad.asignacion_rol WHERE id = ?", a.asignacionId()))
                    .isEqualTo("Cambio de funcion aprobado");
            assertThat(eventos("identidad.rol_revocado", a.asignacionId())).isEqualTo(1);
        });
    }

    @Test
    @DisplayName(
            "Dado un participante sin perfil interno · Cuando se le intenta asignar un rol · Entonces se rechaza con DESTINATARIO_NO_HABILITADO")
    void soloOperadores() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID participante =
                    fixtura.usuario("+5916" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999));

            assertThatThrownBy(() -> asignar(administrador, participante, rol("GU_B_SOPORTE_LEER")))
                    .isInstanceOf(ErrorDeNegocio.class)
                    .hasMessageContaining("no es un operador");
        });
    }
}

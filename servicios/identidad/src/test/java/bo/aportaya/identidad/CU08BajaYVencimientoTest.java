package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.CU08DarDeBaja.ResultadoDeBaja;
import bo.aportaya.identidad.aplicacion.CU08InformeDeAccesos.Pagina;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.ResultadoDeAsignacion;
import bo.aportaya.identidad.dominio.InformeDeAccesos.Fila;
import bo.aportaya.identidad.dominio.InformeDeAccesos.Seccion;
import bo.aportaya.identidad.web.generado.modelo.EntradaActualizarUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAsignarRolesUsuarioInterno;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-08 contra PostgreSQL: baja y suspension (GU-14), vencimiento e informe (GU-08), reemplazo por diferencia (GU-06). */
class CU08BajaYVencimientoTest extends BaseDeGobiernoCU08 {

    @Test
    @DisplayName(
            "Dado un operador con asignaciones y una sesión abierta · Cuando se lo da de baja · Entonces todas sus asignaciones quedan revocadas con motivo sin borrarse · Y su sesión se cierra · Y la cuenta queda en BAJA_LABORAL")
    void bajaDelEmpleado() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID empleado = operador();
            fixtura.factor(empleado, "TOTP", true, true);
            asignar(administrador, empleado, rol("GU_B_LEER_A"));
            asignar(administrador, empleado, rol("GU_B_LEER_B"));
            fixtura.sesionAbierta(empleado);

            ResultadoDeBaja r = baja.ejecutar(empleado, "Fin de la relacion laboral", EVIDENCIA, como(administrador));

            assertThat(r.asignacionesRevocadas()).isEqualTo(2);
            assertThat(columna(
                            "SELECT count(*)::int FROM identidad.asignacion_rol WHERE usuario_id = ? AND revocada_en IS NULL",
                            empleado))
                    .isEqualTo(0);
            assertThat(columna("SELECT count(*)::int FROM identidad.asignacion_rol WHERE usuario_id = ?", empleado))
                    .isEqualTo(2);
            assertThat(sesionesVivas(empleado)).isZero();
            assertThat(columna("SELECT estado FROM identidad.usuario WHERE id = ?", empleado))
                    .isEqualTo("BAJA_LABORAL");
        });
    }

    @Test
    @DisplayName(
            "Dado un operador dado de baja · Cuando se repite la baja · Entonces responde que ya estaba de baja sin emitir otro evento")
    void bajaIdempotente() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID empleado = operador();
            asignar(administrador, empleado, rol("GU_B_LEER_A"));

            baja.ejecutar(empleado, "Fin de la relacion laboral", EVIDENCIA, como(administrador));
            ResultadoDeBaja segunda =
                    baja.ejecutar(empleado, "Fin de la relacion laboral", EVIDENCIA, como(administrador));

            assertThat(segunda.yaEstaba()).isTrue();
            assertThat(eventos("identidad.usuario_interno_dado_de_baja", empleado))
                    .isEqualTo(1);
        });
    }

    @Test
    @DisplayName(
            "Dado un operador con roles · Cuando se lo suspende · Entonces sus asignaciones se revocan · Y al reactivarlo vuelve sin roles")
    void suspenderNoDejaRolesVivos() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID operador = operador();
            asignar(administrador, operador, rol("GU_B_LEER_A"));

            gestion.actualizar(
                    operador,
                    cambioDeEstado(EntradaActualizarUsuarioInterno.EstadoEnum.SUSPENDIDO),
                    como(administrador));
            int vivasSuspendido = vivas(operador);
            var reactivado = gestion.actualizar(
                    operador, cambioDeEstado(EntradaActualizarUsuarioInterno.EstadoEnum.ACTIVO), como(administrador));

            assertThat(vivasSuspendido).isZero();
            assertThat(reactivado.getRoles()).isEmpty();
            assertThat(eventos("identidad.usuario_interno_suspendido", operador))
                    .isEqualTo(1);
            assertThat(eventos("identidad.usuario_interno_reactivado", operador))
                    .isEqualTo(1);
        });
    }

    @Test
    @DisplayName(
            "Dada una asignación vencida sin revocar · Cuando corre el trabajo diario de vencimiento · Entonces queda revocada en su vigente_hasta con motivo VENCIMIENTO · Y una segunda corrida no la vuelve a tocar")
    void vencimientoDiario() {
        enTransaccionQueSeDeshace(() -> {
            UUID operador = operador();
            UUID vencida = asignacionConVigencia(
                    operador, rol("GU_B_LEER_A"), OffsetDateTime.now().minusDays(1));
            ContextoSesion sistema = ContextoSesion.deSistema(
                    UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));

            int primera = vencer.ejecutar(sistema);
            int segunda = vencer.ejecutar(sistema);

            assertThat(primera).isGreaterThanOrEqualTo(1);
            assertThat(segunda).isZero();
            assertThat(columna(
                            "SELECT revocada_en = vigente_hasta FROM identidad.asignacion_rol WHERE id = ?", vencida))
                    .isEqualTo(true);
            assertThat((String) columna("SELECT motivo_revocacion FROM identidad.asignacion_rol WHERE id = ?", vencida))
                    .startsWith("VENCIMIENTO");
            assertThat(eventos("identidad.rol_vencido", vencida)).isEqualTo(1);
            assertThat(columna(
                            """
                            SELECT payload->>'origen' FROM identidad.evento_dominio
                             WHERE tipo = 'identidad.rol_vencido' AND agregado_id = ?
                            """,
                            vencida))
                    .isEqualTo("TAREA_PROGRAMADA");
        });
    }

    @Test
    @DisplayName(
            "Dado un acceso que vence en tres días y un operador sin TOTP · Cuando se pide el informe de accesos · Entonces el primero aparece por vencer · Y el segundo aparece sin TOTP")
    void informeDeAccesos() {
        enTransaccionQueSeDeshace(() -> {
            UUID conTotp = operador();
            fixtura.factor(conTotp, "TOTP", true, true);
            UUID porVencer = asignacionConVigencia(
                    conTotp, rol("GU_B_LEER_A"), OffsetDateTime.now().plusDays(3));
            UUID sinTotp = operador();
            UUID deSinTotp = asignacionConVigencia(
                    sinTotp, rol("GU_B_LEER_B"), OffsetDateTime.now().plusDays(30));
            ContextoSesion lector = como(UUID.randomUUID());

            Pagina vencen = informe.ejecutar(Seccion.POR_VENCER, 1, 50, lector);
            Pagina sinFactor = informe.ejecutar(Seccion.SIN_TOTP, 1, 50, lector);
            Pagina vivos = informe.ejecutar(Seccion.VIVOS, 1, 1, lector);

            assertThat(ids(vencen.filas())).containsExactly(porVencer);
            assertThat(ids(sinFactor.filas())).containsExactly(deSinTotp);
            assertThat(vivos.total()).isEqualTo(2);
            assertThat(vivos.totalPaginas()).isEqualTo(2);
            assertThat(vivos.filas()).hasSize(1);
        });
    }

    @Test
    @DisplayName(
            "Dado un operador con dos roles por el reemplazo deprecado · Cuando se reemplazan conservando uno · Entonces el que sigue conserva quién y cuándo lo otorgó · Y solo se revoca el que sale")
    void reemplazoPorDiferencia() {
        enTransaccionQueSeDeshace(() -> {
            UUID administrador = operador();
            UUID operador = operador();
            UUID sigue = rol("GU_B_LEER_A");
            UUID sale = rol("GU_B_LEER_B");
            ResultadoDeAsignacion queSigue = asignar(administrador, operador, sigue);
            ResultadoDeAsignacion queSale = asignar(administrador, operador, sale);
            UUID otro = operador();

            var entrada = new EntradaAsignarRolesUsuarioInterno();
            entrada.setRoles(List.of(codigoDe(sigue)));
            entrada.setMotivo("Ajuste de funciones aprobado");
            gestion.asignarRoles(operador, entrada, como(otro));

            assertThat(columna(
                            "SELECT otorgada_por FROM identidad.asignacion_rol WHERE id = ?", queSigue.asignacionId()))
                    .isEqualTo(administrador);
            assertThat(columna(
                            "SELECT revocada_en FROM identidad.asignacion_rol WHERE id = ?", queSigue.asignacionId()))
                    .isNull();
            assertThat(columna("SELECT revocada_en FROM identidad.asignacion_rol WHERE id = ?", queSale.asignacionId()))
                    .isNotNull();
            assertThat(eventos("identidad.rol_revocado", queSale.asignacionId()))
                    .isEqualTo(1);
        });
    }

    private static EntradaActualizarUsuarioInterno cambioDeEstado(EntradaActualizarUsuarioInterno.EstadoEnum estado) {
        var entrada = new EntradaActualizarUsuarioInterno();
        entrada.setEstado(estado);
        entrada.setMotivo("Licencia aprobada por recursos humanos");
        entrada.setEvidenciaMfa(EVIDENCIA);
        return entrada;
    }

    private static UUID asignacionConVigencia(UUID usuario, UUID rol, OffsetDateTime hasta) {
        UUID id = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO identidad.asignacion_rol (id, usuario_id, rol_id, ambito, otorgada_por, otorgada_en, vigente_hasta)
                VALUES (?, ?, ?, 'GLOBAL', ?, ?::timestamptz, ?::timestamptz)
                """,
                id,
                usuario,
                rol,
                // Un otorgante de ESTA transaccion: el de la fixtura queda en cache y, creado dentro de un
                // escenario que se deshizo, ya no existe en el siguiente (fk_asignacion_rol_otorgada_por).
                operador(),
                // Otorgada siempre en el pasado: con vigencia a 30 dias, «hasta - 10» caia en el futuro y la
                // asignacion todavia no estaba vigente.
                hasta.isBefore(OffsetDateTime.now())
                        ? hasta.minusDays(10).toString()
                        : OffsetDateTime.now().minusDays(1).toString(),
                hasta.toString());
        return id;
    }

    private static int vivas(UUID usuario) {
        return (Integer) columna(
                "SELECT count(*)::int FROM identidad.asignacion_rol WHERE usuario_id = ? AND revocada_en IS NULL",
                usuario);
    }

    private static String codigoDe(UUID rol) {
        return (String) columna("SELECT codigo FROM identidad.rol WHERE id = ?", rol);
    }

    private static List<UUID> ids(List<Fila> filas) {
        return filas.stream().map(Fila::asignacionId).toList();
    }
}

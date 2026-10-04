package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.CU08AsignarRol.EntradaAsignacion;
import bo.aportaya.identidad.aplicacion.CU08AsignarRol.SalidaAsignacion;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El token lleva el codigo de cada rol vigente: hay endpoints que exigen el rol por nombre
 * ({@code ADMIN_PLATAFORMA}, {@code SOPORTE}, {@code ORGANIZADOR}) y sin esto eran inalcanzables.
 */
class CU08CodigosDeRolVigentesTest extends BaseDeCU08 {

    @Test
    @DisplayName(
            "Dado un usuario con un rol vigente · Cuando se piden los codigos de rol · Entonces vienen · Y los de una asignacion revocada no")
    void soloLosVigentes() {
        UUID administrador = fixtura.usuario("+59172100001");
        UUID operador = fixtura.usuario("+59172100002");
        fixtura.factor(operador, "TOTP", true, true);
        UUID rolVigente = fixtura.rolGlobal("CODVIG1");
        UUID rolRevocado = fixtura.rolGlobal("CODREV1");
        fixtura.darPermisoAlRol(rolVigente, fixtura.permiso("CODVIG1_VER", "codvig", "LEER", false));
        fixtura.darPermisoAlRol(rolRevocado, fixtura.permiso("CODREV1_VER", "codrev", "LEER", false));

        asignarComo(administrador, operador, rolVigente);
        SalidaAsignacion revocada = asignarComo(administrador, operador, rolRevocado);
        fixtura.sesionAbierta(operador);
        transaccion.execute(
                e -> revocar.ejecutar(revocada.asignacionId(), "prueba", true, comoAdministrador(administrador)));

        var codigos = transaccion.execute(e -> accesos.codigosDeRolesVigentes(dsl, operador, OffsetDateTime.now()));

        assertThat(codigos).contains("CODVIG1").doesNotContain("CODREV1");
    }

    @Test
    @DisplayName("Dado un usuario sin roles · Cuando se piden los codigos de rol · Entonces no hay ninguno")
    void sinRolesNoHayCodigos() {
        UUID nadie = fixtura.usuario("+59172100003");

        var codigos = transaccion.execute(e -> accesos.codigosDeRolesVigentes(dsl, nadie, OffsetDateTime.now()));

        assertThat(codigos).isEmpty();
    }

    private SalidaAsignacion asignarComo(UUID administrador, UUID destinatario, UUID rol) {
        return transaccion.execute(e -> asignar.ejecutar(
                new EntradaAsignacion(
                        destinatario, rol, "GLOBAL", Optional.empty(), Optional.empty(), "justificacion de prueba"),
                comoAdministrador(administrador)));
    }
}

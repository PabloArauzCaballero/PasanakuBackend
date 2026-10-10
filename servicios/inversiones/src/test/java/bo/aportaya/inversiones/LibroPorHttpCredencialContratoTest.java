package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular.LibroNoDisponible;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Contrato del consumidor, en los tres niveles, sobre QUIEN firma cada llamada al libro: debitar la
 * inversion y acreditar el rescate mueven plata hacia y desde la custodia y el libro las acepta solo con
 * identidad de servicio; el resto de las operaciones lleva el token de la persona. La credencial usada
 * aca es de PRUEBA. El productor real de estas dos rutas es del carril del nucleo.
 */
class LibroPorHttpCredencialContratoTest extends BaseDelLibroHttp {

    private static void conSesion(String tokenDelUsuario) {
        var jwt = Jwt.withTokenValue(tokenDelUsuario)
                .header("alg", "none")
                .claim("sub", "usuario-de-prueba")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void sinSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName(
            "correcto · debitar la inversion y acreditar el rescate van con la credencial de SERVICIO, aunque haya una sesion de persona, y liberar con la sesion")
    void debitarYAcreditarConIdentidadDeServicio() {
        UUID retencion = UUID.randomUUID();
        responder(200, "{\"transaccionId\":\"" + UUID.randomUUID() + "\"}");
        conSesion("token-de-la-persona");
        var libroDeServicio = conCredencial(CREDENCIAL);

        libroDeServicio.debitarInversion(retencion, bob("1000.00"), "DEBITAR:y");
        libroDeServicio.acreditarRescate(UUID.randomUUID(), bob("1.00"), "ACREDITAR:y", UUID.randomUUID());
        libroDeServicio.liberar(retencion, "LIBERAR:y");

        assertThat(recibidas.get(0).autorizacion()).isEqualTo("Bearer " + CREDENCIAL);
        assertThat(recibidas.get(1).autorizacion()).isEqualTo("Bearer " + CREDENCIAL);
        assertThat(recibidas.get(2).autorizacion())
                .as("liberar es del titular: viaja su token, no la credencial de servicio")
                .isEqualTo("Bearer token-de-la-persona");
    }

    @Test
    @DisplayName(
            "limite · sin sesion (un reintento del reloj) retener y liberar usan la credencial de servicio; una credencial en blanco equivale a no tener ninguna")
    void reintentosDelRelojSinSesion() {
        responder(201, "{\"retencionId\":\"" + UUID.randomUUID() + "\"}");

        conCredencial("  " + CREDENCIAL + "  ").retener(UUID.randomUUID(), bob("1.00"), "RETENER:z", UUID.randomUUID());
        conCredencial("   ").retener(UUID.randomUUID(), bob("1.00"), "RETENER:w", UUID.randomUUID());

        assertThat(recibidas.get(0).autorizacion()).isEqualTo("Bearer " + CREDENCIAL);
        assertThat(recibidas.get(1).autorizacion()).isNull();
    }

    @Test
    @DisplayName(
            "invalido · sin credencial de servicio debitar y acreditar fallan CERRADO: no se manda nada al libro ni se prueba con el token de una persona")
    void sinCredencialDeServicio() {
        conSesion("token-de-la-persona");

        assertThatThrownBy(() -> libro.debitarInversion(UUID.randomUUID(), bob("1.00"), "DEBITAR:s"))
                .isInstanceOf(LibroNoDisponible.class)
                .hasMessageContaining("credencial de servicio");
        assertThatThrownBy(
                        () -> libro.acreditarRescate(UUID.randomUUID(), bob("1.00"), "ACREDITAR:s", UUID.randomUUID()))
                .isInstanceOf(LibroNoDisponible.class);
        assertThat(recibidas).as("ni una sola peticion salio").isEmpty();
    }
}

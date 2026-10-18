package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.EntradaAutenticacion;
import bo.aportaya.identidad.dominio.PoliticaDeIntentos;
import bo.aportaya.identidad.dominio.ResultadoDeAutenticacion;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-04 · Entrar con el correo, y el segundo factor que queda para la operacion que lo
 * exige (2026-10-17: al participante ya no se le pide para entrar).
 *
 * <p>Contra PostgreSQL real, igual que {@link CU04Test}.
 */
class CU04IngresoConCorreoTest extends BaseDeCU04 {

    @Test
    @DisplayName(
            "Dado un participante con MFA activo · Cuando la operación exige MFA (requiere_mfa) · Entonces se le sigue pidiendo el factor y no se crea sesión")
    void operacionSensibleSigueExigiendoFactor() {
        String telefono = "+59175" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        UUID usuario = participanteConCredencial(telefono);
        fixtura.factor(usuario, "TOTP", true, true);

        ResultadoDeAutenticacion resultado = autenticar(sensible(entrada(telefono, "clave-correcta", "huella-nueva")));

        assertThat(resultado.requiereFactorAdicional()).isTrue();
        assertThat(resultado.codigo()).map(c -> c.valor()).contains("AP-CU04-03");
        assertThat(sesionesDe(usuario)).isZero();
    }

    @Test
    @DisplayName(
            "Dado un usuario con correo en su cuenta · Cuando inicia sesión con ese correo en otras mayúsculas y su contraseña · Entonces entra igual que con su teléfono")
    void ingresoConCorreo() {
        String telefono = "+59176" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        String correo = "ingreso-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        UUID usuario = participanteConCredencial(telefono);
        dsl.execute("UPDATE identidad.usuario SET correo = ? WHERE id = ?", correo, usuario);

        ResultadoDeAutenticacion resultado =
                autenticar(entrada(correo.toUpperCase(java.util.Locale.ROOT), "clave-correcta", "huella-correo"));

        assertThat(resultado.exitoso())
                .as("rechazo: %s (%s)", resultado.codigo(), resultado.mensaje())
                .isTrue();
        assertThat(sesionesDe(usuario)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un correo que no es de ninguna cuenta, o que dos cuentas comparten salvo mayúsculas · Cuando se intenta entrar con él · Entonces se responde AP-CU04-01, igual que una contraseña mala")
    void correoInexistenteOAmbiguoEsCredencialInvalida() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        UUID una =
                participanteConCredencial("+59177" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        UUID otra =
                participanteConCredencial("+59178" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        dsl.execute("UPDATE identidad.usuario SET correo = ? WHERE id = ?", "Doble-" + sufijo + "@example.com", una);
        dsl.execute("UPDATE identidad.usuario SET correo = ? WHERE id = ?", "doble-" + sufijo + "@example.com", otra);

        ResultadoDeAutenticacion inexistente =
                autenticar(entrada("nadie-" + sufijo + "@example.com", "clave-correcta", "huella-x"));
        ResultadoDeAutenticacion ambiguo =
                autenticar(entrada("doble-" + sufijo + "@example.com", "clave-correcta", "huella-x"));

        assertThat(inexistente.codigo()).map(c -> c.valor()).contains("AP-CU04-01");
        assertThat(ambiguo.codigo()).map(c -> c.valor()).contains("AP-CU04-01");
        assertThat(inexistente.mensaje()).isEqualTo(ambiguo.mensaje());
        assertThat(sesionesDe(una) + sesionesDe(otra)).isZero();
    }

    private EntradaAutenticacion entrada(String identificador, String credencial, String huella) {
        return new EntradaAutenticacion(
                identificador,
                credencial.toCharArray(),
                huella,
                "ANDROID",
                "127.0.0.1",
                "prueba",
                UUID.randomUUID().toString(),
                Optional.empty(),
                false,
                Duration.ofHours(12));
    }

    /** La misma entrada, para una operacion con {@code permiso.requiere_mfa}. */
    private static EntradaAutenticacion sensible(EntradaAutenticacion e) {
        return new EntradaAutenticacion(
                e.identificador(),
                e.credencial(),
                e.huella(),
                e.plataforma(),
                e.ip(),
                e.agente(),
                e.trazaId(),
                e.factorPresentado(),
                true,
                e.vigenciaDeSesion());
    }

    private ResultadoDeAutenticacion autenticar(EntradaAutenticacion entrada) {
        return transaccion.execute(estado -> cu04.ejecutar(entrada, new PoliticaDeIntentos(5, Duration.ofMinutes(30))));
    }
}

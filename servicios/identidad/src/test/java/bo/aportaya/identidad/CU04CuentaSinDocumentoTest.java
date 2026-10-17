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
 * CU-04 · la cuenta abierta sin documento (INCOMPLETO_KYC, 2026-10-17) entra igual que la que espera
 * revision: conoce la app, sin permisos de dinero, y el ingreso no cambia su estado.
 */
class CU04CuentaSinDocumentoTest extends BaseDeCU04 {

    @Test
    @DisplayName(
            "Dada una cuenta sin documento · Cuando inicia sesion con credenciales correctas · Entonces inicia sesion y sigue INCOMPLETO_KYC")
    void cuentaSinDocumentoPuedeIniciarSesion() {
        String telefono = "+59175" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        UUID usuario = participanteConCredencial(telefono);
        dsl.execute("UPDATE identidad.usuario SET estado = 'INCOMPLETO_KYC', nivel_kyc = 'NINGUNO' WHERE id = ?", usuario);

        ResultadoDeAutenticacion resultado = autenticar(entrada(telefono, "clave-correcta", "huella-sin-documento"));

        assertThat(resultado.exitoso())
                .as("rechazo: %s (%s)", resultado.codigo(), resultado.mensaje())
                .isTrue();
        assertThat(sesionesDe(usuario)).isEqualTo(1);
        assertThat(dsl.fetchOne("SELECT estado FROM identidad.usuario WHERE id = ?", usuario)
                        .get("estado", String.class))
                .isEqualTo("INCOMPLETO_KYC");
    }

    private EntradaAutenticacion entrada(String telefono, String credencial, String huella) {
        return new EntradaAutenticacion(
                telefono,
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

    private ResultadoDeAutenticacion autenticar(EntradaAutenticacion entrada) {
        return transaccion.execute(estado -> cu04.ejecutar(entrada, new PoliticaDeIntentos(5, Duration.ofMinutes(30))));
    }
}

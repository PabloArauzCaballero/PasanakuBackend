package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.EntradaAutenticacion;
import bo.aportaya.identidad.dominio.PoliticaDeIntentos;
import bo.aportaya.identidad.dominio.ResultadoDeAutenticacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * GU-18 · CU-04 contra PostgreSQL real: una cuenta de servicio vencida o revocada (CU-136) no abre sesion aunque su
 * clave sea correcta; una vigente, y cualquier persona, siguen entrando como antes. Datos sinteticos (regla 97.6).
 */
class CU04CuentaDeServicioTest extends BaseDeCU04 {

    @ParameterizedTest
    @ValueSource(strings = {"VENCIDA", "REVOCADA"})
    @DisplayName("Rechaza el ingreso de una cuenta de servicio vencida o revocada aunque su clave sea correcta (GU-18)")
    void cuentaDeServicioNoVigenteNoEntra(String situacion) {
        String telefono = telefonoUnico();
        UUID cuenta = participanteConCredencial(telefono);
        registrarCuentaDeServicio(cuenta, situacion);
        int sesionesAntes = sesionesDe(cuenta);

        ResultadoDeAutenticacion resultado = autenticar(telefono, "clave-correcta");

        assertThat(resultado.exitoso()).isFalse();
        assertThat(resultado.codigo()).contains(CodigoError.de(4, 2));
        assertThat(sesionesDe(cuenta)).isEqualTo(sesionesAntes);
        assertThat(dsl.fetchValue(
                        "SELECT motivo_fallo FROM identidad.intento_autenticacion WHERE usuario_id = ? ORDER BY fecha_hora DESC LIMIT 1",
                        cuenta))
                .isEqualTo("CUENTA_DE_SERVICIO_NO_VIGENTE");
    }

    @Test
    @DisplayName(
            "Rechaza solo lo no vigente: una cuenta de servicio con vencimiento futuro no se rechaza por esto (GU-18)")
    void cuentaDeServicioVigenteNoSeRechazaPorVencimiento() {
        String telefono = telefonoUnico();
        UUID cuenta = participanteConCredencial(telefono);
        registrarCuentaDeServicio(cuenta, "VIGENTE");

        ResultadoDeAutenticacion resultado = autenticar(telefono, "clave-correcta");

        assertThat(resultado.codigo().filter(CodigoError.de(4, 2)::equals)).isEmpty();
    }

    @Test
    @DisplayName("Rechaza nada a una persona sin cuenta de servicio: su ingreso no cambia (GU-18)")
    void unaPersonaNoSeVeAfectada() {
        String telefono = telefonoUnico();
        participanteConCredencial(telefono);

        ResultadoDeAutenticacion resultado = autenticar(telefono, "clave-correcta");

        assertThat(resultado.codigo().filter(CodigoError.de(4, 2)::equals)).isEmpty();
    }

    private void registrarCuentaDeServicio(UUID usuario, String situacion) {
        UUID rol = fixtura.rolGlobal(
                "SERVICIO_T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        boolean revocada = "REVOCADA".equals(situacion);
        dsl.execute(
                """
                INSERT INTO identidad.cuenta_servicio
                    (usuario_id, rol_id, nombre, estado, vence_en, creada_por, creada_en, credencial_rotada_en,
                     revocada_por, revocada_en, motivo_revocacion)
                VALUES (?, ?, 'Cuenta sintetica', ?, ?, ?, now(), now(), ?, ?, ?)
                """,
                usuario,
                rol,
                revocada ? "REVOCADA" : "ACTIVA",
                OffsetDateTime.now().plusDays("VENCIDA".equals(situacion) ? -1 : 30),
                fixtura.otorgante(),
                revocada ? fixtura.otorgante() : null,
                revocada ? OffsetDateTime.now() : null,
                revocada ? "Prueba" : null);
    }

    private ResultadoDeAutenticacion autenticar(String telefono, String clave) {
        var entrada = new EntradaAutenticacion(
                telefono,
                clave.toCharArray(),
                "huella-" + telefono,
                "ANDROID",
                "127.0.0.1",
                "prueba",
                UUID.randomUUID().toString(),
                Optional.empty(),
                false,
                Duration.ofHours(12));
        return transaccion.execute(e -> cu04.ejecutar(entrada, new PoliticaDeIntentos(5, Duration.ofMinutes(30))));
    }

    private static String telefonoUnico() {
        return "+5917%07d".formatted(Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000L));
    }
}

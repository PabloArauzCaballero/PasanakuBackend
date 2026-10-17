package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.EntradaAutenticacion;
import bo.aportaya.identidad.dominio.PoliticaDeIntentos;
import bo.aportaya.identidad.dominio.ResultadoDeAutenticacion;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-04 · El segundo factor por correo para el participante que no enrolo ninguno,
 * en la operacion que lo exige (al entrar ya no se le pide).
 *
 * <p>Contra PostgreSQL real; el envio va a un DOBLE ({@link CorreoEnMemoria}). Que Gmail
 * lo entregue no se prueba aca.
 */
class CU04CodigoPorCorreoTest extends BaseDeCU04 {

    @Test
    @DisplayName(
            "Dado un participante sin factor y con el correo confirmado en el alta · Cuando hace una operacion que exige MFA · Entonces se le pide el codigo y le llega por correo · Y con ese codigo abre la sesion")
    void codigoLlegaPorCorreoYEntra() {
        String telefono = telefono();
        String destino = destino();
        UUID usuario = participanteConCredencial(telefono);
        fixtura.correoConfirmadoEnAlta(usuario, destino, proteccion.huellaDestino(destino));

        ResultadoDeAutenticacion pedido = autenticar(entrada(telefono, "huella-nueva", Optional.empty()));

        assertThat(pedido.requiereFactorAdicional()).isTrue();
        // 278dc8ca: cuando el codigo YA salio al correo verificado se contesta AP-CU04-10
        // (FACTOR_ENVIADO_POR_CORREO, ver identidad.yaml), no el -03 generico: la app sabe que no debe pedirlo otra
        // vez.
        assertThat(pedido.codigo()).map(c -> c.valor()).contains("AP-CU04-10");
        List<CorreoEnMemoria.Enviado> enviados = enviadosA(destino);
        assertThat(enviados).hasSize(1);
        assertThat(enviados.get(0).deAcceso()).isTrue();

        ResultadoDeAutenticacion conCodigo = autenticar(
                entrada(telefono, "huella-nueva", Optional.of(enviados.get(0).codigo())));

        assertThat(conCodigo.exitoso()).isTrue();
        assertThat(sesionesDe(usuario)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un participante sin factor que recibio el codigo por correo · Cuando escribe un codigo que no es el enviado · Entonces se rechaza con AP-CU04-04 y no abre sesion")
    void codigoEquivocadoNoEntra() {
        String telefono = telefono();
        String destino = destino();
        UUID usuario = participanteConCredencial(telefono);
        fixtura.correoConfirmadoEnAlta(usuario, destino, proteccion.huellaDestino(destino));
        autenticar(entrada(telefono, "huella-nueva", Optional.empty()));
        String enviado = enviadosA(destino).get(0).codigo();
        String otro = enviado.equals("123456") ? "654321" : "123456";

        ResultadoDeAutenticacion resultado = autenticar(entrada(telefono, "huella-nueva", Optional.of(otro)));

        assertThat(resultado.exitoso()).isFalse();
        assertThat(resultado.codigo()).map(c -> c.valor()).contains("AP-CU04-04");
        assertThat(sesionesDe(usuario)).isZero();
    }

    @Test
    @DisplayName(
            "Dado un participante cuyo correo actual NO es el que confirmo en el alta · Cuando hace una operacion que exige MFA · Entonces no se manda ningun correo")
    void correoCambiadoNoRecibe() {
        String telefono = telefono();
        String confirmado = destino();
        String actual = destino();
        UUID usuario = participanteConCredencial(telefono);
        fixtura.correoConfirmadoEnAlta(usuario, actual, proteccion.huellaDestino(confirmado));

        autenticar(entrada(telefono, "huella-nueva", Optional.empty()));

        assertThat(enviadosA(actual)).isEmpty();
        assertThat(enviadosA(confirmado)).isEmpty();
    }

    @Test
    @DisplayName(
            "Dado un participante con TOTP enrolado y correo confirmado · Cuando hace una operacion que exige MFA · Entonces se le pide su factor enrolado y no se envia correo")
    void factorEnroladoGana() {
        String telefono = telefono();
        String destino = destino();
        UUID usuario = participanteConCredencial(telefono);
        fixtura.factor(usuario, "TOTP", true, true);
        fixtura.correoConfirmadoEnAlta(usuario, destino, proteccion.huellaDestino(destino));

        autenticar(entrada(telefono, "huella-nueva", Optional.empty()));

        assertThat(enviadosA(destino)).isEmpty();
    }

    @Test
    @DisplayName(
            "Dado un operador sin factor y con correo confirmado · Cuando entra · Entonces sigue rechazado con AP-CU04-06 y no se envia correo (R-SEG-10)")
    void operadorNoUsaCorreo() {
        String telefono = telefono();
        String destino = destino();
        UUID operador = participanteConCredencial(telefono);
        fixtura.asignarRol(operador, fixtura.rolGlobal("OP" + telefono.substring(8)));
        fixtura.correoConfirmadoEnAlta(operador, destino, proteccion.huellaDestino(destino));

        ResultadoDeAutenticacion resultado = autenticar(entrada(telefono, "huella-nueva", Optional.empty()));

        assertThat(resultado.codigo()).map(c -> c.valor()).contains("AP-CU04-06");
        assertThat(enviadosA(destino)).isEmpty();
    }

    private static List<CorreoEnMemoria.Enviado> enviadosA(String destino) {
        return correo.enviados.stream().filter(e -> e.destino().equals(destino)).toList();
    }

    private static String telefono() {
        return "+5917" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
    }

    private static String destino() {
        return "acceso-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private EntradaAutenticacion entrada(String telefono, String huella, Optional<String> codigo) {
        return new EntradaAutenticacion(
                telefono,
                "clave-correcta".toCharArray(),
                huella,
                "ANDROID",
                "127.0.0.1",
                "prueba",
                UUID.randomUUID().toString(),
                codigo,
                // Desde el 2026-10-17 al participante no se le pide codigo para entrar
                // (CU-04 paso 3): el codigo por correo solo sale en una operacion sensible.
                true,
                Duration.ofHours(12));
    }

    private ResultadoDeAutenticacion autenticar(EntradaAutenticacion entrada) {
        return transaccion.execute(estado -> cu04.ejecutar(entrada, new PoliticaDeIntentos(5, Duration.ofMinutes(30))));
    }
}

package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.dominio.AlcanceDeCuenta;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.clientes.CodigoTotp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-09 §10 y cambio de clave: la invitacion de un solo uso y la clave propia del operador, contra la base real. */
class CU09CuentaDeOperadorTest extends BaseDeCuentaDeOperador {

    @Test
    @DisplayName(
            "Dado un operador recién dado de alta con una invitación vigente · Cuando abre el enlace, elige una clave"
                    + " válida y confirma su TOTP · Entonces su credencial queda con la clave elegida y requiere_cambio en"
                    + " false · Y el token queda CONSUMIDO y no sirve una segunda vez")
    void criterio8() {
        UUID operador = operadorConClaveInutilizable("+59176100001");
        emitir(operador, AlcanceDeCuenta.ALTA);
        String token = correo.ultimoToken();

        var inicio = transaccion.execute(e -> invitaciones.iniciarFactor(token, origen()));
        String codigo = CodigoTotp.de(inicio.semillaBase32(), Instant.now());
        var resultado = completar(token, "una-clave-larga-del-operador-1", null, inicio.factorId(), codigo);

        assertThat(resultado.exito()).isTrue();
        assertThat(hasheador.coincide("una-clave-larga-del-operador-1".toCharArray(), hashDe(operador)))
                .isTrue();
        assertThat(dsl.fetchOne(
                                "SELECT requiere_cambio FROM identidad.credencial_acceso WHERE usuario_id = ?",
                                operador)
                        .get(0, Boolean.class))
                .isFalse();
        assertThat(estadoDelUltimoToken(operador)).isEqualTo("CONSUMIDO");
        assertThat(factorConfirmadoDe(operador)).isEqualTo(1);

        var segunda = completar(token, "otra-clave-larga-del-operador-2", null, inicio.factorId(), codigo);
        assertThat(segunda.exito()).isFalse();
        assertThat(segunda.rechazo().codigo().valor()).isEqualTo("AP-CU09-04");
    }

    @Test
    @DisplayName("Dado una invitación vencida, usada o de otro propósito · Cuando se intenta consultar o completar ·"
            + " Entonces se rechaza con TOKEN_INVALIDO, con el mismo mensaje en los tres casos")
    void criterio9() {
        UUID operador = operadorConClaveInutilizable("+59176100002");
        emitir(operador, AlcanceDeCuenta.ALTA);
        String vencido = correo.ultimoToken();
        dsl.execute(
                "UPDATE identidad.token_verificacion SET expira_en = now() - interval '1 minute' WHERE usuario_id = ?",
                operador);

        UUID otro = operadorConClaveInutilizable("+59176100003");
        emitir(otro, AlcanceDeCuenta.CLAVE);
        String deOtroProposito = correo.ultimoToken();

        UUID tercero = operadorConClaveInutilizable("+59176100004");
        emitir(tercero, AlcanceDeCuenta.ALTA);
        String usado = correo.ultimoToken();
        dsl.execute(
                """
                UPDATE identidad.token_verificacion SET estado = 'CONSUMIDO', consumido_en = now()
                WHERE usuario_id = ?
                """,
                tercero);

        String falso = "token-que-nadie-emitio-0123456789";
        String mensajeVencido = rechazoDeConsultar(vencido);
        assertThat(rechazoDeConsultar(usado)).isEqualTo(mensajeVencido);
        assertThat(rechazoDeConsultar(falso)).isEqualTo(mensajeVencido);
        // El token de recuperacion de la clave no sirve para enrolar un factor: otro proposito, mismo mensaje.
        assertThat(rechazoDeEnrolar(deOtroProposito)).isEqualTo(mensajeVencido);
        assertThat(mensajeVencido).contains("ya no sirve");
    }

    @Test
    @DisplayName(
            "Dado un operador con una invitación de reinscripción del factor · Cuando la completa con su clave actual"
                    + " y un código del TOTP nuevo · Entonces el factor anterior queda inactivo y el nuevo confirmado · Y"
                    + " todas sus sesiones quedan revocadas")
    void criterio12() {
        UUID operador = operadorConClaveInutilizable("+59176100008");
        String claveActual = "la-clave-actual-del-operador-8";
        dsl.execute(
                "UPDATE identidad.credencial_acceso SET hash_contrasena = ? WHERE usuario_id = ?",
                hasheador.hashear(claveActual.toCharArray()),
                operador);
        factoresViejo(operador);
        UUID sesion = fixtura.sesionAbierta(operador);
        emitir(operador, AlcanceDeCuenta.FACTOR);
        String token = correo.ultimoToken();

        var inicio = transaccion.execute(e -> invitaciones.iniciarFactor(token, origen()));
        String codigo = CodigoTotp.de(inicio.semillaBase32(), Instant.now());
        var resultado = completar(token, null, claveActual, inicio.factorId(), codigo);

        assertThat(resultado.exito()).isTrue();
        assertThat(factorConfirmadoDe(operador)).isEqualTo(1);
        assertThat(sesionViva(sesion)).isFalse();
        assertThat(sesionesVivasDe(operador)).isZero();
        assertThat(hasheador.coincide(claveActual.toCharArray(), hashDe(operador)))
                .isTrue();
    }

    @Test
    @DisplayName("Dado un operador con la marca de cambio de clave pendiente · Cuando cambia su clave con la actual"
            + " correcta · Entonces la marca queda en falso y la clave nueva rige · Y todas sus sesiones, incluida"
            + " la actual, quedan cerradas")
    void criterio13() {
        UUID operador = operadorConClaveInutilizable("+59176100013");
        String actual = "la-clave-actual-del-operador-13";
        dsl.execute(
                "UPDATE identidad.credencial_acceso SET hash_contrasena = ? WHERE usuario_id = ?",
                hasheador.hashear(actual.toCharArray()),
                operador);
        fixtura.factor(operador, "TOTP", true, true);
        UUID sesion = fixtura.sesionAbierta(operador);
        fixtura.sesionAbierta(operador);

        var salida = transaccion.execute(
                e -> cambioDeClave.cambiar(actual, "la-clave-nueva-del-operador-13", sesion, comoOperador(operador)));

        assertThat(salida.cierraLaSesionActual()).isTrue();
        assertThat(sesionesVivasDe(operador)).isZero();
        assertThat(hasheador.coincide("la-clave-nueva-del-operador-13".toCharArray(), hashDe(operador)))
                .isTrue();
        assertThat(dsl.fetchOne(
                                "SELECT debe_cambiar_contrasena FROM identidad.usuario_interno WHERE usuario_id = ?",
                                operador)
                        .get(0, Boolean.class))
                .isFalse();
    }

    @Test
    @DisplayName("rechaza cambiar la clave con la actual equivocada y no toca nada")
    void rechazaClaveActualEquivocada() {
        UUID operador = operadorConClaveInutilizable("+59176100014");
        fixtura.factor(operador, "TOTP", true, true);
        UUID sesion = fixtura.sesionAbierta(operador);

        try {
            transaccion.execute(e -> cambioDeClave.cambiar(
                    "no-es-la-clave-actual", "la-clave-nueva-del-operador-14", sesion, comoOperador(operador)));
            throw new AssertionError("la clave actual equivocada no debio pasar");
        } catch (ErrorDeNegocio rechazo) {
            assertThat(rechazo.codigo().valor()).isEqualTo("AP-CU09-01");
        }
        assertThat(sesionViva(sesion)).isTrue();
    }

    @Test
    @DisplayName("rechaza un codigo TOTP incorrecto sin consumir el token y cuenta el intento")
    void rechazaCodigoIncorrecto() {
        UUID operador = operadorConClaveInutilizable("+59176100010");
        emitir(operador, AlcanceDeCuenta.ALTA);
        String token = correo.ultimoToken();
        var inicio = transaccion.execute(e -> invitaciones.iniciarFactor(token, origen()));
        String bueno = CodigoTotp.de(inicio.semillaBase32(), Instant.now());
        String malo = "000000".equals(bueno) ? "111111" : "000000";

        var resultado = completar(token, "una-clave-larga-del-operador-10", null, inicio.factorId(), malo);

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.rechazo().codigo().valor()).isEqualTo("AP-CU04-04");
        assertThat(estadoDelUltimoToken(operador)).isEqualTo("EMITIDO");
        assertThat(dsl.fetchOne(
                                "SELECT intentos_fallidos FROM identidad.token_verificacion WHERE usuario_id = ?",
                                operador)
                        .get(0, Integer.class))
                .isEqualTo(1);
        // Con el codigo bueno, dentro del mismo token, todavia entra.
        assertThat(completar(token, "una-clave-larga-del-operador-10", null, inicio.factorId(), bueno)
                        .exito())
                .isTrue();
    }

    @Test
    @DisplayName("rechaza una clave debil sin consumir el token")
    void rechazaClaveDebil() {
        UUID operador = operadorConClaveInutilizable("+59176100011");
        emitir(operador, AlcanceDeCuenta.CLAVE);
        String token = correo.ultimoToken();

        var resultado = completar(token, "corta", null, null, null);

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.rechazo().codigo().valor()).isEqualTo("AP-CU09-02");
        assertThat(estadoDelUltimoToken(operador)).isEqualTo("EMITIDO");
    }
}

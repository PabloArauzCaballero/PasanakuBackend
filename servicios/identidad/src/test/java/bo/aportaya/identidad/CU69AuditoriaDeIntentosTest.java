package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · auditoría de intentos de canje, revocación idempotente y bitácora sin secretos.
 *
 * <p>Amenazas: intentos sin rastro, enlaces revocados que "resucitan" por reintento y el secreto
 * filtrado a la base o a los logs.
 */
class CU69AuditoriaDeIntentosTest extends BaseDeEmisionDeInvitacion {

    @Test
    void cadaIntentoSobreUnTokenExistenteDejaRastroSinElSecreto() {
        var token = emitir(UUID.randomUUID(), grupo, telefono);
        var ajeno = fixtura.usuario(
                "+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)));
        canjearConSecreto(token, grupo, ajeno, token.token());
        canjearConSecreto(token, grupo, destinatario, "0".repeat(64));
        assertThat(canjear(token, grupo, destinatario)).isPresent();
        assertThat(resultados(token)).containsExactly("CANAL_NO_COINCIDE", "CODIGO_INCORRECTO", "VALIDO");
        canjearConSecreto(token, grupo, destinatario, token.token());
        assertThat(resultados(token)).last().isEqualTo("YA_CONSUMIDO");
        var texto = dsl.fetch("SELECT * FROM identidad.intento_validacion_token WHERE token_id=?", token.tokenId())
                .toString();
        assertThat(texto).doesNotContain(token.token());
        assertThat(bitacora.list).isNotEmpty();
        assertThat(bitacora.list.stream().map(ILoggingEvent::getFormattedMessage))
                .noneMatch(m -> m.contains(token.token()) || m.contains(telefono));
    }

    @Test
    void vencidaBloqueadaYRevocadaQuedanRegistradasYNoSePuedenCanjear() {
        var vence = emitir(UUID.randomUUID(), grupo, telefono);
        AHORA.set(vence.expiraEn().toInstant());
        assertThat(canjear(vence, grupo, destinatario)).isEmpty();
        assertThat(resultados(vence)).containsExactly("EXPIRADO");

        AHORA.set(BASE.plusSeconds(120));
        var bloquea = emitir(UUID.randomUUID(), UUID.randomUUID(), telefono);
        UUID g = grupoDe(bloquea);
        for (int i = 0; i < 3; i++) canjearConSecreto(bloquea, g, destinatario, "1".repeat(64));
        assertThat(canjear(bloquea, g, destinatario)).isEmpty();
        assertThat(resultados(bloquea))
                .containsExactly(
                        "CODIGO_INCORRECTO", "CODIGO_INCORRECTO", "CODIGO_INCORRECTO", "BLOQUEADO_POR_INTENTOS");

        var revocada = emitir(UUID.randomUUID(), grupo, telefono);
        assertThat(tx.<Boolean>execute(s -> consumir.revocar(revocada.tokenId(), ctx(emisor))))
                .isTrue();
        assertThat(canjear(revocada, grupo, destinatario)).isEmpty();
        assertThat(resultados(revocada)).containsExactly("NO_ENCONTRADO");
    }

    @Test
    void revocarEsIdempotenteParaElEmisorYNuncaParaOtrosNiParaLoConsumido() {
        var token = emitir(UUID.randomUUID(), grupo, telefono);
        assertThat(tx.<Boolean>execute(s -> consumir.revocar(token.tokenId(), ctx(emisor))))
                .isTrue();
        assertThat(tx.<Boolean>execute(s -> consumir.revocar(token.tokenId(), ctx(emisor))))
                .isTrue();
        assertThat(tx.<Boolean>execute(s -> consumir.revocar(token.tokenId(), ctx(destinatario))))
                .isFalse();
        var consumido = emitir(UUID.randomUUID(), grupo, telefono);
        assertThat(canjear(consumido, grupo, destinatario)).isPresent();
        assertThat(tx.<Boolean>execute(s -> consumir.revocar(consumido.tokenId(), ctx(emisor))))
                .isFalse();
    }

    @Test
    void unEnlaceRevocadoNoSeRecuperaPorReintentoDeLaMismaClave() {
        UUID clave = UUID.randomUUID();
        var token = emitir(clave, grupo, telefono);
        assertThat(tx.<Boolean>execute(s -> consumir.revocar(token.tokenId(), ctx(emisor))))
                .isTrue();
        assertThatThrownBy(() -> emitir(clave, grupo, telefono)).isInstanceOf(ErrorDeNegocio.class);
    }
}

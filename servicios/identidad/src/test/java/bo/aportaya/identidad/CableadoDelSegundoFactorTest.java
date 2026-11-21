package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import bo.aportaya.identidad.dominio.puertos.CorreoDeVerificacion;
import bo.aportaya.identidad.dominio.puertos.DesafioDeFactor;
import bo.aportaya.identidad.infraestructura.CorreoDeVerificacionLocal;
import bo.aportaya.identidad.infraestructura.DesafioLocal;
import bo.aportaya.identidad.infraestructura.DesafioPorTipo;
import bo.aportaya.identidad.infraestructura.GmailApiCorreoDeVerificacion;
import bo.aportaya.identidad.infraestructura.VerificadorTotp;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

/**
 * El cableado del segundo factor, con y sin el perfil {@code local} (el que corre TEST).
 *
 * <p>Reemplaza a la version que exigia {@code DesafioDeDesarrollo} con perfil {@code local}:
 * ese bean aceptaba {@code 000000} para cualquiera y se retiro (F1.S3.M2, IDCU-HUECO-077,
 * IDCU-SEG-004). Ahora el desafio primario es el mismo en todos los perfiles, y ningun perfil
 * acepta un codigo fijo.
 */
class CableadoDelSegundoFactorTest {

    private final VerificadorTotp totp = mock(VerificadorTotp.class);

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(VerificadorTotp.class, () -> totp)
            .withUserConfiguration(
                    DesafioLocal.class,
                    DesafioPorTipo.class,
                    GmailApiCorreoDeVerificacion.class,
                    CorreoDeVerificacionLocal.class)
            .withPropertyValues(
                    "aportaya.correo.gmail.client-id=cliente",
                    "aportaya.correo.gmail.client-secret=secreto",
                    "aportaya.correo.gmail.refresh-token=refresco",
                    "aportaya.correo.gmail.from-email=cuentas@aportaya.bo");

    @Test
    @DisplayName(
            "TEST (perfil local + gmail_api): el desafio es el real por tipo, el correo sale por Gmail y 000000 no entra")
    void comoEnTest() {
        contexto.withPropertyValues("aportaya.correo.proveedor=gmail_api", "spring.profiles.active=local")
                .withInitializer(c -> c.getEnvironment().setActiveProfiles("local"))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    DesafioDeFactor desafio = ctx.getBean(DesafioDeFactor.class);
                    assertThat(desafio).isInstanceOf(DesafioPorTipo.class);
                    assertThat(ctx.getBean(CorreoDeVerificacion.class))
                            .isInstanceOf(GmailApiCorreoDeVerificacion.class);
                    UUID usuario = UUID.randomUUID();
                    assertThat(desafio.validar(usuario, "TOTP", "000000")).isFalse();
                    assertThat(desafio.validar(usuario, "CORREO", "000000")).isFalse();
                });
    }

    @Test
    @DisplayName("sin perfil local: el mismo desafio por tipo y, sin proveedor, el correo local")
    void fueraDeLocal() {
        contexto.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(DesafioDeFactor.class)).isInstanceOf(DesafioPorTipo.class);
            assertThat(ctx.getBean(CorreoDeVerificacion.class)).isInstanceOf(CorreoDeVerificacionLocal.class);
        });
    }
}

package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.dominio.puertos.CorreoDeVerificacion;
import bo.aportaya.identidad.dominio.puertos.DesafioDeFactor;
import bo.aportaya.identidad.infraestructura.CorreoDeVerificacionLocal;
import bo.aportaya.identidad.infraestructura.DesafioDeDesarrollo;
import bo.aportaya.identidad.infraestructura.DesafioLocal;
import bo.aportaya.identidad.infraestructura.GmailApiCorreoDeVerificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

/**
 * El cableado que corre en TEST: perfil {@code local} + {@code gmail_api}. Ninguna otra
 * prueba levanta Spring con el perfil {@code local}, y un bean mal cableado ahi deja a
 * identidad sin arrancar en TEST.
 */
class CableadoDelSegundoFactorTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withUserConfiguration(
                    DesafioLocal.class,
                    DesafioDeDesarrollo.class,
                    GmailApiCorreoDeVerificacion.class,
                    CorreoDeVerificacionLocal.class)
            .withPropertyValues(
                    "aportaya.correo.gmail.client-id=cliente",
                    "aportaya.correo.gmail.client-secret=secreto",
                    "aportaya.correo.gmail.refresh-token=refresco",
                    "aportaya.correo.gmail.from-email=cuentas@aportaya.bo");

    @Test
    @DisplayName("TEST (perfil local + gmail_api): el desafio primario es el de desarrollo y el correo sale por Gmail")
    void comoEnTest() {
        contexto.withPropertyValues("aportaya.correo.proveedor=gmail_api", "spring.profiles.active=local")
                .withInitializer(c -> c.getEnvironment().setActiveProfiles("local"))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(DesafioDeFactor.class)).isInstanceOf(DesafioDeDesarrollo.class);
                    assertThat(ctx.getBean(CorreoDeVerificacion.class))
                            .isInstanceOf(GmailApiCorreoDeVerificacion.class);
                });
    }

    @Test
    @DisplayName("sin perfil local: el desafio es el real y, sin proveedor, el correo es el local")
    void fueraDeLocal() {
        contexto.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(DesafioDeFactor.class)).isInstanceOf(DesafioLocal.class);
            assertThat(ctx.getBean(CorreoDeVerificacion.class)).isInstanceOf(CorreoDeVerificacionLocal.class);
        });
    }
}

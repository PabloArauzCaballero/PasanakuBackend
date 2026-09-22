package bo.aportaya.identidad.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.EmitirAcceso;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ADR-024 · el JWKS, la ruta de la que dependen los otros trece servicios.
 *
 * <p>Es la única ruta del producto que <b>tiene</b> que estar abierta sin sesión, y por
 * un motivo circular: los trece la consultan para poder verificar la firma de una
 * sesión. Pedirle sesión sería pedir sesión para poder comprobar la sesión, y ninguno
 * arrancaría.
 *
 * <p>Y la única prueba que importa además de esa: <b>solo sale la clave pública</b>. Si
 * la privada saliera por acá, cualquiera que alcance la red podría emitir un token de
 * administrador — que es exactamente el escenario que RS256 existe para impedir.
 */
@PruebaWeb(JwksController.class)
class JwksControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmitirAcceso acceso;

    /** Un JWKS con la forma real: la parte pública de una RSA, y nada más. */
    private static Map<String, Object> jwksPublico() {
        return Map.of(
                "keys",
                List.of(Map.of(
                        "kty", "RSA",
                        "kid", "clave-de-prueba",
                        "alg", "RS256",
                        "use", "sig",
                        "n", "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx",
                        "e", "AQAB")));
    }

    @Test
    @DisplayName("ADR-024 · 200 sin sesión: los otros trece la consultan antes de tener una")
    void seSirveSinSesion() throws Exception {
        when(acceso.clavesPublicas()).thenReturn(jwksPublico());

        // Sin `.with(Sesiones...)` a proposito. Si esto diera 401, ningun servicio
        // podria validar un token y el sistema entero quedaria caido.
        mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].kid").value("clave-de-prueba"));
    }

    @Test
    @DisplayName("ADR-024 · solo la parte PÚBLICA: ningún componente de la clave privada")
    void nuncaSaleLaClavePrivada() throws Exception {
        when(acceso.clavesPublicas()).thenReturn(jwksPublico());

        String cuerpo = mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Los seis campos privados de una RSA en formato JWK. Con cualquiera de ellos
        // afuera, quien lo lea puede firmar tokens de administrador.
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .as("un componente de la clave privada salio en el JWKS")
                .doesNotContain("\"d\"")
                .doesNotContain("\"p\"")
                .doesNotContain("\"q\"")
                .doesNotContain("\"dp\"")
                .doesNotContain("\"dq\"")
                .doesNotContain("\"qi\"");
    }
}

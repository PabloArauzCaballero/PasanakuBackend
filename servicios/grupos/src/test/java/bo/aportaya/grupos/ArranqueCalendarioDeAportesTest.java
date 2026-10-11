package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.web.generado.modelo.CalendarioDeAportes;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import bo.aportaya.plataforma.pruebas.DependidoDeEnsayo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * H8.S1.M1 · el PRODUCTOR REAL del calendario de aportes, de punta a punta dentro de `grupos`: el proceso levanta con su
 * guardia y su decodificador de token REALES (el JWKS lo sirve un doble que publica una clave de ensayo), responde por HTTP de
 * verdad con PostgreSQL real, y la forma del cuerpo es la del archivo dorado que `aportes` prueba en su adaptador.
 *
 * <p>Es el nivel posible sin romper la frontera de servicios (ADR-023: `grupos` y `aportes` no se importan, ni siquiera en
 * pruebas): el cliente real de `aportes` contra este servidor real no cabe en una sola JVM de prueba. Lo cubren en conjunto
 * esta prueba (productor real == dorado) y {@code CalendarioDelPeriodoPorHttpTest} de `aportes` (cliente real sobre el dorado).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArranqueCalendarioDeAportesTest {

    private static final RSAKey CLAVE = clave();
    private static final DependidoDeEnsayo IDENTIDAD =
            new DependidoDeEnsayo().responder(200, new JWKSet(CLAVE.toPublicJWK()).toString());

    @Value("${local.server.port}")
    private int puerto;

    @DynamicPropertySource
    static void configuracion(DynamicPropertyRegistry registro) {
        var contenedor = BaseDePrueba.contenedor();
        registro.add("spring.datasource.url", contenedor::getJdbcUrl);
        registro.add("spring.datasource.username", contenedor::getUsername);
        registro.add("spring.datasource.password", contenedor::getPassword);
        registro.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registro.add("aportaya.jwt.jwks-uri", () -> IDENTIDAD.urlBase() + "/.well-known/jwks.json");
        registro.add("SEGURIDAD_PIMIENTA", () -> "pimienta-de-prueba");
    }

    @AfterAll
    static void apagar() {
        IDENTIDAD.close();
    }

    private static RSAKey clave() {
        try {
            return new RSAKeyGenerator(2048).keyID("clave-de-ensayo").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String token(String rol) throws Exception {
        var reclamos = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .claim("rol", rol)
                .claim("permisos", List.of())
                .claim("dispositivo", "dispositivo-de-prueba")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 300_000))
                .build();
        var jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(CLAVE.getKeyID())
                        .build(),
                reclamos);
        jwt.sign(new RSASSASigner(CLAVE));
        return jwt.serialize();
    }

    private HttpResponse<String> pedir(UUID periodo, String token) throws Exception {
        var peticion = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + puerto + "/grupos/periodos/" + periodo + "/calendario-de-aportes"));
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(peticion.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static UUID periodoConDosTitulares(List<UUID> titularesAlSalir) {
        var datos = new DriverManagerDataSource(
                BaseDePrueba.contenedor().getJdbcUrl(),
                BaseDePrueba.contenedor().getUsername(),
                BaseDePrueba.contenedor().getPassword());
        var fixtura = new FixturaDeGrupos(DSL.using(datos, SQLDialect.POSTGRES));
        UUID grupo = fixtura.grupoConformado(3);
        titularesAlSalir.addAll(fixtura.participantesConCupo(grupo, 2));
        return fixtura.periodo(grupo, 3, new BigDecimal("1000.00"));
    }

    /** Las claves de cada objeto y el tipo de cada valor, sin los valores: la FORMA del cuerpo. */
    private static void forma(JsonNode nodo, String ruta, java.util.TreeSet<String> salida) {
        if (nodo.isObject()) {
            nodo.properties().forEach(e -> forma(e.getValue(), ruta + "." + e.getKey(), salida));
        } else if (nodo.isArray()) {
            salida.add(ruta + "[]");
            if (nodo.size() > 0) {
                forma(nodo.get(0), ruta + "[]", salida);
            }
        } else {
            salida.add(ruta + ":" + nodo.getNodeType());
        }
    }

    @Test
    @DisplayName(
            "el productor real responde 200 con la forma exacta del archivo dorado, deserializable de forma estricta")
    void elCuerpoRealTieneLaFormaDelDorado() throws Exception {
        var titulares = new java.util.ArrayList<UUID>();
        UUID periodo = periodoConDosTitulares(titulares);

        var respuesta = pedir(periodo, token("ADMIN_PLATAFORMA"));

        assertThat(respuesta.statusCode()).isEqualTo(200);
        ObjectMapper estricto = JsonMapper.builder()
                .addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        CalendarioDeAportes modelo = estricto.readValue(respuesta.body(), CalendarioDeAportes.class);
        assertThat(modelo.getPeriodoId()).isEqualTo(periodo);
        assertThat(modelo.getNumero()).isEqualTo(3);
        assertThat(modelo.getCupos()).hasSize(2);
        assertThat(modelo.getCupos().get(0).getMontoEsperado().getMonto()).isEqualTo("500.00");

        JsonNode real = new ObjectMapper().readTree(respuesta.body());
        JsonNode dorado = new ObjectMapper()
                .readTree(Files.readString(Path.of("src/test/resources/fixtures/calendario-de-aportes.json")));
        var formaReal = new java.util.TreeSet<String>();
        var formaDorada = new java.util.TreeSet<String>();
        forma(real, "$", formaReal);
        forma(dorado, "$", formaDorada);
        assertThat(formaReal)
                .as("el cuerpo real y el dorado tienen la misma forma")
                .isEqualTo(formaDorada);
    }

    @Test
    @DisplayName("el guardia real: un participante recibe 403, sin token 401 y un periodo inexistente 422 AP-CU21-08")
    void laGuardiaRealYElPeriodoInexistente() throws Exception {
        UUID periodo = periodoConDosTitulares(new java.util.ArrayList<>());

        assertThat(pedir(periodo, token("PARTICIPANTE")).statusCode()).isEqualTo(403);
        assertThat(pedir(periodo, null).statusCode()).isEqualTo(401);
        var inexistente = pedir(UUID.randomUUID(), token("ADMIN_PLATAFORMA"));
        assertThat(inexistente.statusCode()).isEqualTo(422);
        assertThat(inexistente.body()).contains("AP-CU21-08");
    }
}

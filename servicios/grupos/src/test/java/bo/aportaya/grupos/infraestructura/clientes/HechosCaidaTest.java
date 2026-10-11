package bo.aportaya.grupos.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.DependidoDeEnsayo;
import bo.aportaya.plataforma.pruebas.Espera;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Regla 98.2.3 y 98.8.4 · la caida de los dependidos de `grupos` ejercitada de verdad contra el cliente real
 * {@link HechosPorHttp}: cada uno de los ocho servicios tiene su propio circuito, y con el circuito abierto cada
 * metodo responde LO MISMO que con el dependido caido —lo que no deja pasar— sin salir a la red. El doble
 * {@link DependidoDeEnsayo} se cae, vuelve y cuenta las peticiones que llegan.
 */
class HechosCaidaTest {

    private static final UUID USUARIO = UUID.fromString("c8200000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("c8200000-0000-4000-8000-000000000002");
    private static final UUID TOKEN = UUID.fromString("c8200000-0000-4000-8000-000000000003");
    private static final String TELEFONO = "+59171234567";
    private static final int VENTANA = 10;
    private static final long ESPERA_MS = 400;

    private static HechosPorHttp contra(String url, int ventana) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(300);
        fabrica.setReadTimeout(300);
        return new HechosPorHttp(
                RestClient.builder().requestFactory(fabrica),
                url,
                url,
                url,
                url,
                url,
                url,
                url,
                url,
                new ConfiguracionDeCircuito(ventana, 50, Duration.ofMillis(ESPERA_MS), 3));
    }

    /** Una vuelta por las nueve preguntas que se resuelven con «lo que no deja pasar» cuando el dependido no contesta. */
    private static void preguntarATodos(HechosDeOtrosServicios hechos) {
        hechos.organizadorHabilitado(USUARIO);
        hechos.organizadorHabilitadoDelUsuario(USUARIO);
        hechos.tarifarioVigente("GENERAL");
        hechos.licenciaHabilita("GRUPO_PASANAKU");
        hechos.estadoDePagos(USUARIO);
        hechos.morososDelGrupo(GRUPO);
        hechos.reputacion(USUARIO);
        hechos.restriccion(USUARIO);
        hechos.contactoSuprimido("+59171234567", "INVITACION");
        hechos.nivelDeKyc(USUARIO);
    }

    @Test
    @DisplayName("la caida abre los ocho circuitos; abiertos, NADA sale a la red aunque los dependidos ya volvieron")
    void caidaAbreLosCircuitosYNoLlama() {
        try (var todos = new DependidoDeEnsayo().responder(200, "{}")) {
            var hechos = contra(todos.urlBase(), VENTANA);
            todos.caer();

            for (int i = 0; i < VENTANA; i++) {
                preguntarATodos(hechos);
            }
            todos.levantar();
            preguntarATodos(hechos);

            assertThat(todos.peticionesRecibidas())
                    .as("con todos los circuitos abiertos no sale ninguna peticion")
                    .isZero();
        }
    }

    @Test
    @DisplayName("FALLBACK declarado: con el circuito abierto cada metodo deniega, igual que con el dependido caido")
    void conElCircuitoAbiertoSeDeniega() {
        try (var todos = new DependidoDeEnsayo().responder(200, "{}")) {
            var hechos = contra(todos.urlBase(), VENTANA);
            todos.caer();
            for (int i = 0; i < VENTANA; i++) {
                preguntarATodos(hechos);
            }
            todos.levantar();

            assertThat(hechos.organizadorHabilitado(USUARIO)).isFalse();
            assertThat(hechos.organizadorHabilitadoDelUsuario(USUARIO)).isEmpty();
            assertThat(hechos.tarifarioVigente("GENERAL")).isEmpty();
            assertThat(hechos.licenciaHabilita("GRUPO_PASANAKU")).isFalse();
            assertThat(hechos.estadoDePagos(USUARIO).alDia()).isFalse();
            assertThat(hechos.morososDelGrupo(GRUPO)).isEqualTo(HechosDeOtrosServicios.SIN_DATO_DE_MOROSOS);
            assertThat(hechos.reputacion(USUARIO).tieneHistorial()).isFalse();
            assertThat(hechos.restriccion(USUARIO).vigente()).isTrue();
            assertThat(hechos.contactoSuprimido("+59171234567", "INVITACION")).isTrue();
            assertThat(hechos.nivelDeKyc(USUARIO)).isEqualTo("NINGUNO");
            assertThat(todos.peticionesRecibidas()).isZero();
        }
    }

    @Test
    @DisplayName(
            "las operaciones de identidad que emiten o consumen un enlace FALLAN con la excepcion de red, sin crear nada")
    void identidadCaidaNoEmiteNiConsume() {
        try (var identidad = new DependidoDeEnsayo().responder(200, "{}")) {
            var hechos = contra(identidad.urlBase(), VENTANA);
            identidad.caer();
            for (int i = 0; i < VENTANA; i++) {
                assertThatThrownBy(() -> hechos.tokenDeInvitacion(UUID.randomUUID(), GRUPO, "WHATSAPP", TELEFONO))
                        .isInstanceOf(ResourceAccessException.class);
            }
            identidad.levantar();

            // Circuito abierto: la misma falla de red, y la peticion no sale.
            assertThatThrownBy(() -> hechos.tokenDeInvitacion(UUID.randomUUID(), GRUPO, "WHATSAPP", TELEFONO))
                    .isInstanceOf(ResourceAccessException.class)
                    .hasMessageContaining("circuito abierto");
            assertThatThrownBy(() -> hechos.consumirInvitacion(TOKEN, GRUPO, UUID.randomUUID(), "secreto"))
                    .isInstanceOf(ResourceAccessException.class);
            assertThat(hechos.enlaceDeInvitacionValido(TOKEN, "secreto", TELEFONO, "BASICO"))
                    .isFalse();
            assertThat(hechos.revocarTokenDeInvitacion(TOKEN)).isFalse();
            assertThatThrownBy(() -> hechos.usuarioDelTelefono(TELEFONO)).isInstanceOf(IllegalStateException.class);
            assertThat(identidad.peticionesRecibidas()).isZero();
        }
    }

    @Test
    @DisplayName("el circuito de un dependido no arrastra a los otros: organizador abierto, tarifas sigue respondiendo")
    void cadaDependidoTieneSuCircuito() {
        try (var organizador = new DependidoDeEnsayo().responder(200, "{}");
                var tarifas = new DependidoDeEnsayo()
                        .responder(200, "{\"vigente\":true,\"tarifarioId\":\"" + TOKEN + "\"}")) {
            var fabrica = new SimpleClientHttpRequestFactory();
            fabrica.setConnectTimeout(300);
            fabrica.setReadTimeout(300);
            var hechos = new HechosPorHttp(
                    RestClient.builder().requestFactory(fabrica),
                    organizador.urlBase(),
                    tarifas.urlBase(),
                    tarifas.urlBase(),
                    tarifas.urlBase(),
                    tarifas.urlBase(),
                    tarifas.urlBase(),
                    tarifas.urlBase(),
                    tarifas.urlBase(),
                    new ConfiguracionDeCircuito(VENTANA, 50, Duration.ofMillis(ESPERA_MS), 3));
            organizador.caer();
            for (int i = 0; i < VENTANA; i++) {
                hechos.organizadorHabilitado(USUARIO);
            }
            organizador.levantar();

            assertThat(hechos.organizadorHabilitado(USUARIO)).isFalse();
            assertThat(organizador.peticionesRecibidas()).isZero();
            assertThat(hechos.tarifarioVigente("GENERAL")).contains(TOKEN);
        }
    }

    @Test
    @DisplayName("recuperacion: tras la espera pasan llamadas de prueba y el circuito cierra solo")
    void seRecuperaSolo() {
        try (var kyc = new DependidoDeEnsayo().responder(200, "{\"nivel\":\"COMPLETO\"}")) {
            var hechos = contra(kyc.urlBase(), VENTANA);
            kyc.caer();
            for (int i = 0; i < VENTANA; i++) {
                assertThat(hechos.nivelDeKyc(USUARIO)).isEqualTo("NINGUNO");
            }
            kyc.levantar();
            assertThat(hechos.nivelDeKyc(USUARIO)).isEqualTo("NINGUNO");
            assertThat(kyc.peticionesRecibidas()).isZero();

            Espera.hasta(
                    Duration.ofSeconds(3),
                    Duration.ofMillis(50),
                    "primera llamada de prueba tras la espera",
                    () -> "COMPLETO".equals(hechos.nivelDeKyc(USUARIO)));
            assertThat(hechos.nivelDeKyc(USUARIO)).isEqualTo("COMPLETO");
            assertThat(hechos.nivelDeKyc(USUARIO)).isEqualTo("COMPLETO");
            assertThat(kyc.peticionesRecibidas()).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    @DisplayName("un 404 o un 422 no es una caida: tres ventanas de respuestas de negocio dejan el circuito cerrado")
    void cuatroXXNoAbren() {
        try (var kyc = new DependidoDeEnsayo().responder(404, "{}")) {
            var hechos = contra(kyc.urlBase(), VENTANA);
            for (int i = 0; i < 3 * VENTANA; i++) {
                assertThat(hechos.nivelDeKyc(USUARIO)).isEqualTo("NINGUNO");
            }

            assertThat(kyc.peticionesRecibidas()).isEqualTo(3 * VENTANA);
        }
    }

    @Test
    @DisplayName(
            "el telefono viaja en el CUERPO de POST /usuarios/busquedas/por-telefono y no en la URL (regla 90.2.2)")
    void elTelefonoNoVaEnLaUrl() throws IOException {
        List<String> uris = new CopyOnWriteArrayList<>();
        List<String> metodos = new CopyOnWriteArrayList<>();
        List<String> cuerpos = new CopyOnWriteArrayList<>();
        HttpServer identidad = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        identidad.createContext("/", intercambio -> {
            uris.add(intercambio.getRequestURI().toString());
            metodos.add(intercambio.getRequestMethod());
            cuerpos.add(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = ("{\"existe\":true,\"usuarioId\":\"" + USUARIO + "\"}").getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        identidad.start();
        try {
            var hechos = contra("http://127.0.0.1:" + identidad.getAddress().getPort(), VENTANA);

            assertThat(hechos.usuarioDelTelefono(TELEFONO)).contains(USUARIO);

            assertThat(metodos).containsExactly("POST");
            assertThat(uris).containsExactly("/usuarios/busquedas/por-telefono");
            assertThat(uris.get(0)).doesNotContain("591").doesNotContain("?");
            assertThat(cuerpos.get(0)).contains("\"telefono\"").contains(TELEFONO);
        } finally {
            identidad.stop(0);
        }
    }
}

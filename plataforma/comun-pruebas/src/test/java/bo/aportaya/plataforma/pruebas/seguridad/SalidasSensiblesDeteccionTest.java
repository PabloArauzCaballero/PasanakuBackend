package bo.aportaya.plataforma.pruebas.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.pruebas.seguridad.SalidasSensibles.Hallazgo;
import bo.aportaya.plataforma.pruebas.seguridad.SalidasSensibles.Salida;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El barrido tiene que ver lo que busca. Aca se le dan muestras SINTETICAS —codigo
 * escrito para la prueba, con nombres de variable y ningun dato de nadie— que filtran un
 * telefono, un token, un correo y un documento por cada una de las cuatro salidas, y
 * muestras limpias que no deben gritar. Una regla que nunca se vio disparar no protege.
 */
class SalidasSensiblesDeteccionTest {

    private static List<Hallazgo> barrer(String cuerpoDelMetodo) {
        String fuente = "class Muestra {\n  void caso() {\n" + cuerpoDelMetodo + "\n  }\n}\n";
        return SalidasSensibles.revisar(FuenteJava.deTexto(fuente));
    }

    @Test
    @DisplayName("rechaza: un log con el telefono o el token como argumento")
    void veElLogConPii() {
        var hallazgos = barrer(
                """
                BITACORA.info("llamo a {} con {}", telefono, token.getTokenValue());
                """);

        assertThat(hallazgos).extracting(Hallazgo::salida).containsOnly(Salida.LOG);
        assertThat(hallazgos).extracting(Hallazgo::dato).contains("telefono", "token", "getTokenValue");
    }

    @Test
    @DisplayName("rechaza: un log cuyo texto promete mostrar el valor del codigo")
    void veElTextoQueMuestraElValor() {
        var hallazgos = barrer(
                """
                BITACORA.info("desafio {} para {}: el codigo es {}", id, usuario, CODIGO);
                """);

        assertThat(hallazgos).extracting(Hallazgo::dato).containsExactly("texto:codigo");
    }

    @Test
    @DisplayName("rechaza: una URL con el telefono o el token en la query")
    void veLaUrlConPii() {
        var hallazgos = barrer(
                """
                cliente.get().uri("/usuarios/por-telefono?telefono={t}", telefonoE164).retrieve();
                cliente.consultar("/verificar?token=" + tokenDelCliente, Respuesta.class);
                """);

        assertThat(hallazgos).extracting(Hallazgo::salida).containsOnly(Salida.URL);
        assertThat(hallazgos).extracting(Hallazgo::dato).contains("?telefono", "telefonoE164", "?token");
    }

    @Test
    @DisplayName("rechaza: un evento del outbox con el correo o el documento en el payload")
    void veElEventoConPii() {
        var hallazgos = barrer(
                """
                outbox.emitir(dsl, new EventoDominio("x.alta", "tercero", id,
                        Map.of("correo", entrada.correo(), "numeroDocumento", entrada.numeroDocumento()), traza));
                """);

        assertThat(hallazgos).extracting(Hallazgo::salida).containsOnly(Salida.EVENTO);
        assertThat(hallazgos).extracting(Hallazgo::dato).contains("clave:correo", "clave:numeroDocumento");
    }

    @Test
    @DisplayName("rechaza: un mensaje de error que concatena una credencial, y la salida por consola")
    void veElErrorYLaConsola() {
        // La llamada a volcar la traza se arma por partes: escrita entera, el gate de seguridad del repo
        // (scripts/verificar_seguridad.py, prohibicion 10) la denunciaria en esta misma prueba.
        var hallazgos = barrer(
                """
                throw new ErrorDeNegocio(codigo, "clave invalida: " + claveIngresada);
                System.out.println(valor);
                """
                        + "e.print" + "StackTrace();");

        assertThat(hallazgos).extracting(Hallazgo::salida).contains(Salida.ERROR, Salida.CONSOLA);
        assertThat(hallazgos).extracting(Hallazgo::dato).contains("claveIngresada", "print");
    }

    @Test
    @DisplayName("no grita por identificadores, hashes, longitudes ni nombres que solo SE PARECEN a un dato")
    void noGritaDeMas() {
        var hallazgos = barrer(
                """
                BITACORA.info("invitacion tokenId={} grupo={} emisor={}", tokenId, grupoId, emisorId);
                BITACORA.info("codigo de error {} en {}", codigoDeError, servicio);
                BITACORA.debug("clave de {} caracteres, hash {}", LARGO_CLAVE, claveHash);
                cliente.consultar("/aportes/participantes/" + participanteId + "/estado", Estado.class);
                outbox.emitir(dsl, new EventoDominio("x.alta", "tercero", id, Map.of("terceroId", id, "tipo", tipo), traza));
                throw new ErrorDeNegocio(codigo, "Ese participante no existe.");
                """);

        assertThat(hallazgos).isEmpty();
    }

    @Test
    @DisplayName("lo escrito en un comentario o en un texto no cuenta como llamada")
    void ignoraComentariosYTextos() {
        var hallazgos = barrer(
                """
                // BITACORA.info("dato {}", telefono);
                String ejemplo = "BITACORA.info(telefono)";
                /* new EventoDominio("x", "y", id, Map.of("correo", correo), traza) */
                """);

        assertThat(hallazgos).isEmpty();
    }
}

package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios.ConsumoInvitacion;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios.TokenInvitacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Los tokens de invitacion de {@code identidad}: emitir, validar, consumir y revocar el enlace.
 *
 * <p>Salio de {@link HechosPorHttp} (limite de 300 lineas) sin cambiar lo que hace, salvo dos cosas del carril P:
 *
 * <ul>
 *   <li>Un rechazo de NEGOCIO de identidad (422 con su {@code AP-CU..}) llega al cliente como ese mismo 422 y no
 *       como «fallo no previsto» (500). Un 422 ajeno traducido a 500 le decia a la app «volve a intentar» ante algo
 *       que reintentar no arregla (L3, H2).
 *   <li>Validar un enlace no traga la falla en silencio: deja la CLASE de la falla y el estado HTTP (regla 90.2.1:
 *       nunca el cuerpo, el token ni el telefono) y sigue denegando por omision.
 * </ul>
 */
final class TokensDeInvitacionPorHttp {

    private static final Logger BITACORA = LoggerFactory.getLogger(TokensDeInvitacionPorHttp.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int LARGO_MAXIMO_DEL_MENSAJE = 200;

    private final ClienteConCircuito identidad;

    TokensDeInvitacionPorHttp(ClienteConCircuito identidad) {
        this.identidad = identidad;
    }

    TokenInvitacion emitir(UUID clave, UUID grupoId, String canal, String telefono) {
        TokenInvitacion emitido;
        try {
            emitido = identidad.ejecutar(rest -> rest.post()
                    .uri("/usuarios/tokens/invitacion")
                    .header("Idempotency-Key", clave.toString())
                    .headers(ClienteDeServicio::propagarElToken)
                    .body(Map.of("canal", canal, "telefonoDestino", telefono, "grupoId", grupoId))
                    .retrieve()
                    .body(TokenInvitacion.class));
        } catch (RestClientResponseException rechazo) {
            // Solo el 422 es una decision de negocio de identidad; un 400/401/403/5xx es una falla de integracion.
            var negocio = rechazoDeNegocio(rechazo);
            if (negocio.isPresent()) throw negocio.get();
            throw rechazo;
        }
        if (emitido == null) {
            // Sin token no hay enlace, y sin enlace no hay invitacion que enviar.
            throw new IllegalStateException("identidad no emitio el token de invitacion");
        }
        return emitido;
    }

    boolean esValido(UUID tokenId, String token, String telefonoE164, String kycMinimo) {
        try {
            var resultado = identidad.ejecutar(rest -> rest.post()
                    .uri("/usuarios/tokens/invitacion/validar")
                    .headers(ClienteDeServicio::propagarElToken)
                    .body(Map.of(
                            "tokenId", tokenId, "token", token, "telefonoE164", telefonoE164, "kycMinimo", kycMinimo))
                    .retrieve()
                    .body(Validacion.class));
            return resultado != null && resultado.valido();
        } catch (RuntimeException noSePudoValidar) {
            // Denegar por omision: sin respuesta de identidad el enlace no vale. Pero que se VEA que no se pudo
            // validar: de otro modo una caida entre servicios es indistinguible de «enlace vencido».
            BITACORA.warn("identidad no pudo validar el enlace de invitacion: {}", queFallo(noSePudoValidar));
            return false;
        }
    }

    ConsumoInvitacion consumir(UUID tokenId, UUID grupoId, UUID clave, String token) {
        try {
            var consumo = identidad.ejecutar(rest -> rest.post()
                    .uri("/usuarios/tokens/invitacion/" + tokenId + "/consumos")
                    .header("Idempotency-Key", clave.toString())
                    .headers(ClienteDeServicio::propagarElToken)
                    .body(Map.of("grupoId", grupoId, "token", token))
                    .retrieve()
                    .body(ConsumoInvitacion.class));
            if (consumo == null) throw new IllegalStateException("No se confirmó la invitacion");
            return consumo;
        } catch (org.springframework.web.client.HttpClientErrorException.UnprocessableEntity yaNoVale) {
            // Pierde la carrera por un enlace de un solo uso (antes: 500).
            throw new ErrorDeNegocio(CodigoError.de(69, 5), "Esa invitacion ya no es valida.");
        }
    }

    boolean revocar(UUID tokenId) {
        try {
            identidad.ejecutar(rest -> rest.post()
                    .uri("/usuarios/tokens/invitacion/{id}/revocacion", tokenId)
                    .headers(ClienteDeServicio::propagarElToken)
                    .retrieve()
                    .toBodilessEntity());
            return true;
        } catch (RestClientResponseException | ResourceAccessException noSePudo) {
            // 422 (no es suyo / ya consumido), caida, timeout o circuito abierto: no se da por revocada.
            BITACORA.warn("identidad no confirmo la revocacion del enlace {}: {}", tokenId, queFallo(noSePudo));
            return false;
        }
    }

    /**
     * El rechazo de negocio de identidad, con SU codigo y SU mensaje (texto pensado para la persona, nunca datos).
     * Vacio si no es un 422, o si el cuerpo no trae un codigo valido: en ese caso se conserva la falla original.
     */
    static java.util.Optional<ErrorDeNegocio> rechazoDeNegocio(RestClientResponseException rechazo) {
        if (rechazo.getStatusCode().value() != 422) {
            return java.util.Optional.empty();
        }
        try {
            JsonNode cuerpo = JSON.readTree(rechazo.getResponseBodyAsString());
            var codigo = new CodigoError(cuerpo.path("codigo").asText());
            String mensaje = cuerpo.path("mensaje").asText("");
            if (mensaje.isBlank() || mensaje.length() > LARGO_MAXIMO_DEL_MENSAJE) {
                mensaje = "No se pudo completar la invitacion.";
            }
            return java.util.Optional.of(new ErrorDeNegocio(codigo, mensaje));
        } catch (java.io.IOException | RuntimeException ilegible) {
            return java.util.Optional.empty();
        }
    }

    /** La clase de la falla y, si la hubo, el estado HTTP. Ni el cuerpo, ni el token, ni el telefono (90.2.1). */
    static String queFallo(RuntimeException falla) {
        return falla instanceof RestClientResponseException respuesta
                ? "HTTP " + respuesta.getStatusCode().value()
                : falla.getClass().getSimpleName();
    }

    private record Validacion(boolean valido) {}
}

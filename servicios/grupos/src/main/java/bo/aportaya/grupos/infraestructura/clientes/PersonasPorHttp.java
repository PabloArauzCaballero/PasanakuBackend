package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios.Contacto;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que {@code identidad} dice de una persona: su nivel de KYC, el contacto de la sesion y quien tiene un telefono.
 *
 * <p>Salio de {@link HechosPorHttp} (limite de 300 lineas) sin cambiar lo que hace, igual que
 * {@link TokensDeInvitacionPorHttp}: mismo {@link ClienteConCircuito} hacia identidad y la misma degradacion.
 */
final class PersonasPorHttp {

    private final ClienteConCircuito identidad;

    PersonasPorHttp(ClienteConCircuito identidad) {
        this.identidad = identidad;
    }

    String nivelDeKyc(UUID usuarioId) {
        return identidad
                .consultar("/usuarios/" + usuarioId + "/kyc", Kyc.class)
                .map(Kyc::nivel)
                .orElse("NINGUNO");
    }

    Optional<Contacto> contactoDeLaSesion() {
        return identidad
                .consultar("/usuarios/yo/perfil", Perfil.class)
                .map(p -> new Contacto(
                        p.nombreCompleto() != null ? p.nombreCompleto() : p.nombreVisible(), p.telefono(), p.correo()));
    }

    /**
     * El telefono viaja en el CUERPO de {@code POST /usuarios/busquedas/por-telefono}, no en la URL (regla 90.2.2).
     * Reemplaza al {@code GET /usuarios/por-telefono}, deprecado por {@code identidad}.
     */
    Optional<UUID> usuarioDelTelefono(String telefonoE164) {
        try {
            var encontrado = identidad.ejecutar(rest -> rest.post()
                    .uri("/usuarios/busquedas/por-telefono")
                    .headers(ClienteDeServicio::propagarElToken)
                    .body(Map.of("telefono", telefonoE164))
                    .retrieve()
                    .body(Encontrado.class));
            return encontrado != null && encontrado.existe()
                    ? Optional.ofNullable(encontrado.usuarioId())
                    : Optional.empty();
        } catch (RuntimeException noSePudo) {
            throw new IllegalStateException("No se pudo verificar al destinatario de la invitacion", noSePudo);
        }
    }

    private record Kyc(String nivel) {}

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record Perfil(String nombreVisible, String nombreCompleto, String telefono, String correo) {}

    private record Encontrado(boolean existe, UUID usuarioId) {}
}

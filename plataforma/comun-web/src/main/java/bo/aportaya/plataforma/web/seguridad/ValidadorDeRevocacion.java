package bo.aportaya.plataforma.web.seguridad;

import bo.aportaya.plataforma.dominio.Reloj;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * El paso que faltaba entre «la firma es valida» y «la sesion sigue viva» (GU-15, ADR-030).
 *
 * <p>Corre dentro del decodificador, despues de la firma y del vencimiento: un token revocado se rechaza igual que uno
 * vencido, con 401 y {@code invalid_token}, sin que ningun controlador tenga que acordarse de preguntar.
 *
 * <ul>
 *   <li><b>Sin {@code sesion_id}</b> no hay nada que revocar por este camino: se acepta (solo lo usan emisores de
 *       prueba; ningun flujo de produccion firma sin sesion).
 *   <li><b>Lista sin rehidratar</b> ({@link ListaDeRevocaciones#estaAlDia()} falso): se rechaza. Es la ventana de
 *       arranque en frio y ADR-030 §3 pide fallar del lado seguro; la readiness ({@link SaludDeRevocaciones}) avisa
 *       que la replica todavia no deberia recibir trafico.
 *   <li><b>Sesion revocada</b>: se rechaza.
 * </ul>
 */
public class ValidadorDeRevocacion implements OAuth2TokenValidator<Jwt> {

    static final String RECLAMO_SESION = "sesion_id";

    private static final OAuth2Error REVOCADA = new OAuth2Error("invalid_token", "La sesion fue cerrada", null);

    private static final OAuth2Error SIN_LISTA =
            new OAuth2Error("invalid_token", "La lista de sesiones cerradas todavia no esta disponible", null);

    private final ListaDeRevocaciones lista;
    private final Reloj reloj;

    public ValidadorDeRevocacion(ListaDeRevocaciones lista, Reloj reloj) {
        this.lista = lista;
        this.reloj = reloj;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String reclamo = token.getClaimAsString(RECLAMO_SESION);
        if (reclamo == null || reclamo.isBlank()) {
            return OAuth2TokenValidatorResult.success();
        }
        UUID sesion;
        try {
            sesion = UUID.fromString(reclamo);
        } catch (IllegalArgumentException malformado) {
            // Firmado por identidad y con un sesion_id que no es UUID: no es un caso, es un defecto. No se acepta.
            return OAuth2TokenValidatorResult.failure(REVOCADA);
        }
        if (!lista.estaAlDia()) {
            return OAuth2TokenValidatorResult.failure(SIN_LISTA);
        }
        return lista.estaRevocada(sesion, reloj.ahora())
                ? OAuth2TokenValidatorResult.failure(REVOCADA)
                : OAuth2TokenValidatorResult.success();
    }
}

package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.dominio.puertos.DesafioDeFactor;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * El segundo factor **en la maquina de desarrollo**, y solo ahi.
 *
 * <p>{@code DesafioLocal} genera un codigo de seis digitos con azar criptografico y
 * —bien— no lo registra en ningun lado: un codigo en el log es un codigo filtrado.
 * El efecto es que en desarrollo <b>nadie puede completar el segundo factor</b>: no
 * hay SMS ni correo encendido, asi que el codigo no llega a ninguna parte. Y como
 * {@code R-SEG-10} exige segundo factor a todo operador sin excepcion, el backoffice
 * entero quedaba inaccesible: el portal de riesgo existia y no habia forma de entrar.
 *
 * <p>Aca el codigo es fijo y esta escrito en el propio codigo fuente, que es
 * exactamente lo que NO se puede hacer en produccion. Por eso:
 *
 * <ul>
 *   <li>{@code @Profile("local")}: fuera de esa maquina este bean no existe, y el que
 *       corre es el de verdad. No hay bandera que lo encienda por accidente.</li>
 *   <li>Avisa al arrancar, fuerte, para que nadie confunda un entorno con otro.</li>
 * </ul>
 */
@Component
@Primary
@Profile("local")
public class DesafioDeDesarrollo implements DesafioDeFactor {

    /** El codigo de desarrollo. No es un secreto: es lo contrario de un secreto. */
    public static final String CODIGO = "000000";

    private static final Logger BITACORA = LoggerFactory.getLogger(DesafioDeDesarrollo.class);

    public DesafioDeDesarrollo() {
        BITACORA.warn(
                "SEGUNDO FACTOR DE DESARROLLO ACTIVO: cualquier codigo {} entra. "
                        + "Esto SOLO existe con el perfil `local`.",
                CODIGO);
    }

    @Override
    public UUID emitir(UUID usuarioId, String tipoDeFactor) {
        UUID id = UUID.randomUUID();
        BITACORA.info("desafio {} de desarrollo para el usuario {}: el codigo es {}", id, usuarioId, CODIGO);
        return id;
    }

    @Override
    public boolean validar(UUID usuarioId, String tipoDeFactor, String valorPresentado) {
        return CODIGO.equals(valorPresentado);
    }
}

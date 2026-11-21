package bo.aportaya.plataforma.web.seguridad;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * El actor autenticado y los cuatro ojos reales, en un solo lugar para los catorce
 * servicios (F1.S2.M1 del plan de correcciones 2026-11-21; DECISIONES P-13).
 *
 * <p><b>El problema que cierra.</b> Varios casos de uso pedian el aprobador en el cuerpo
 * ({@code autorizadaPor}, {@code aprobadaPor}, {@code segundaRevisionPor}...) y, en el
 * mejor caso, comparaban ese UUID contra el de la sesion. Cualquiera que tuviera el
 * permiso escribia el UUID de un colega y «aprobaba» solo. Un UUID tipeado no es una
 * persona: la segunda firma tiene que ser <b>otra peticion, con otro token</b>.
 *
 * <p><b>Como se usa</b> (ver {@code plataforma/comun-web/docs/cuatro-ojos.md}):
 *
 * <ol>
 *   <li>Primer acto (proponer, solicitar, aprobar): el servicio guarda
 *       {@link #actor()} junto al registro, en estado pendiente de segunda firma.
 *   <li>Segundo acto, en otra peticion: el servicio rehidrata el primer actor de su
 *       tabla y llama {@link #segundaFirma(ActorAutenticado, CodigoError)}. Si es la misma
 *       persona (con la misma u otra sesion), se rechaza con el codigo del caso de uso.
 *   <li>El contrato del endpoint <b>no declara</b> ningun campo de aprobador: un cuerpo
 *       que lo traiga cae en {@code 400 AP-VAL-02} por {@code CuerposEstrictos}.
 * </ol>
 *
 * <p>No decide <b>quien puede</b> firmar: eso es el {@code @Permiso} del endpoint (y, si
 * el par de permisos es incompatible, la segregacion de funciones de identidad). Solo
 * garantiza que las dos firmas son de dos personas autenticadas.
 */
@Component
public class CuatroOjos {

    private final SesionDeLaPeticion sesion;

    public CuatroOjos(SesionDeLaPeticion sesion) {
        this.sesion = sesion;
    }

    /**
     * El actor de la peticion en curso, del token verificado: sujeto y {@code sesion_id}.
     *
     * @throws bo.aportaya.plataforma.dominio.SinContextoDeSesion sin token, sin sujeto,
     *     sin rol o sin {@code sesion_id} ({@code 401}). Un token sin sesion (el de un
     *     paso de step-up, el de un servicio) no puede firmar un acto.
     */
    public ActorAutenticado actor() {
        ContextoSesion ctx = sesion.actual();
        return new ActorAutenticado(ctx.usuarioId(), sesion.idDeLaSesion());
    }

    /**
     * La segunda firma de un acto cuyo primer actor ya esta registrado.
     *
     * @param primero el actor que el servicio guardo al registrar el primer acto.
     * @param codigo el {@code AP-CU<NN>-<nn>} del caso de uso para el rechazo ({@code 422}).
     */
    public SegundaFirma segundaFirma(ActorAutenticado primero, CodigoError codigo) {
        ContextoSesion ctx = sesion.actual();
        if (ctx.esSistema()) {
            // Un trabajo programado no es una persona: no puede ser los segundos ojos.
            throw new ErrorDeNegocio(codigo, "La segunda firma tiene que ser de una persona con su propia sesion.");
        }
        return exigir(primero, new ActorAutenticado(ctx.usuarioId(), sesion.idDeLaSesion()), codigo);
    }

    /**
     * La regla pura, para el caso de uso que ya tiene los dos actores tomados de token.
     *
     * <p>Distinta persona es la condicion. Distinta sesion se comprueba tambien, aunque
     * una sesion pertenece a un solo usuario: si alguna vez dos actores comparten sesion,
     * algo se rompio antes y lo correcto es no firmar.
     */
    public static SegundaFirma exigir(ActorAutenticado primero, ActorAutenticado segundo, CodigoError codigo) {
        Objects.requireNonNull(primero, "primero");
        Objects.requireNonNull(segundo, "segundo");
        Objects.requireNonNull(codigo, "codigo");
        if (primero.usuarioId().equals(segundo.usuarioId())) {
            throw new ErrorDeNegocio(codigo, "La segunda firma tiene que ser de otra persona.");
        }
        if (primero.sesion().isPresent() && primero.sesion().equals(segundo.sesion())) {
            throw new ErrorDeNegocio(codigo, "La segunda firma tiene que llegar en otra sesion.");
        }
        return new SegundaFirma(primero, segundo);
    }
}

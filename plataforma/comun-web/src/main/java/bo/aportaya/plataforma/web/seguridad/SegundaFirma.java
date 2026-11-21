package bo.aportaya.plataforma.web.seguridad;

import java.util.Objects;

/**
 * Las dos firmas de un acto con cuatro ojos: dos personas distintas, cada una con su
 * propio token. Solo la fabrica {@link CuatroOjos}; quien la recibe puede persistir los
 * dos actores sabiendo que ninguno salio del cuerpo de la peticion.
 */
public record SegundaFirma(ActorAutenticado primero, ActorAutenticado segundo) {

    public SegundaFirma {
        Objects.requireNonNull(primero, "primero");
        Objects.requireNonNull(segundo, "segundo");
    }
}

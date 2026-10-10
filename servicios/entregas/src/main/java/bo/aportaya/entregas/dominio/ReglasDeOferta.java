package bo.aportaya.entregas.dominio;

import bo.aportaya.entregas.dominio.puertos.HechosDeGrupos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Cuando un derecho de cobro se puede ofrecer.
 *
 * <p>Lo que se vende es el DERECHO A COBRAR el pozo de un turno. No es la membresia, y no
 * cambia quien debe los aportes: el vendedor sigue aportando (PLAN §1.2, ambiguedad A10). Los
 * codigos son {@code AP-CU130-nn}: el mercado no tiene caso de uso en la boveda y la numeracion
 * es provisional (confirmar con quien gobierna docs/CasosDeUso).
 */
public final class ReglasDeOferta {

    /** Estados del turno en que todavia no se cobro ni esta en curso. */
    private static final Set<String> VENDIBLES = Set.of("PROGRAMADO", "CONFIRMADO");

    private ReglasDeOferta() {}

    public static void validarPublicacion(
            HechosDeGrupos.Turno turno,
            UUID quienVende,
            Dinero precio,
            Dinero cargos,
            Instant ahora,
            Instant vigenteHasta,
            Duration vigenciaMaxima) {
        if (!turno.usuarioId().equals(quienVende)) {
            // No se dice de quien es: confirmar que existe es regalarle datos a un tercero.
            throw new ErrorDeNegocio(CodigoError.de(130, 1), "Ese turno no es tuyo: no podes ofrecerlo.");
        }
        if (!VENDIBLES.contains(turno.estado())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(130, 2), "Ese turno ya no se puede vender: esta " + turno.estado() + ".");
        }
        if (precio.moneda() != turno.montoDelDerecho().moneda() || cargos.moneda() != precio.moneda()) {
            throw new ErrorDeNegocio(CodigoError.de(130, 3), "El precio y los cargos van en la moneda del turno.");
        }
        if (precio.esNegativo() || precio.esCero()) {
            throw new ErrorDeNegocio(CodigoError.de(130, 3), "El precio tiene que ser positivo.");
        }
        if (cargos.esNegativo() || cargos.esMayorQue(precio)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(130, 3), "Los cargos no pueden ser negativos ni superar el precio.");
        }
        if (!vigenteHasta.isAfter(ahora) || vigenteHasta.isAfter(ahora.plus(vigenciaMaxima))) {
            throw new ErrorDeNegocio(
                    CodigoError.de(130, 3),
                    "La vigencia tiene que estar en el futuro y no pasar de " + vigenciaMaxima + ".");
        }
        validarContraElCierre(turno, vigenteHasta);
    }

    /**
     * La oferta expira ANTES del punto de cierre del beneficiario del turno (PLAN H9, hallazgo de H9.S1.M1): no
     * puede seguir viva cuando el derecho ya se cierra. Si grupos no informo el cierre no se publica nada (fallo
     * cerrado: no se supone un plazo); vigente hasta el mismo instante del cierre tampoco vale, porque «antes» es
     * estrictamente antes.
     */
    private static void validarContraElCierre(HechosDeGrupos.Turno turno, Instant vigenteHasta) {
        if (turno.cierreDelTurno() == null) {
            throw new ErrorDeNegocio(
                    CodigoError.de(130, 8), "No se pudo confirmar el cierre del turno con grupos: no se publica nada.");
        }
        if (!vigenteHasta.isBefore(turno.cierreDelTurno())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(130, 3),
                    "La oferta tiene que vencer antes del cierre del turno (" + turno.cierreDelTurno() + ").");
        }
    }

    /** Lo que cobra el vendedor: el precio menos los cargos, que absorbe el (ambiguedad A9). */
    public static Dinero paraElVendedor(Dinero precio, Dinero cargos) {
        return precio.menos(cargos);
    }
}

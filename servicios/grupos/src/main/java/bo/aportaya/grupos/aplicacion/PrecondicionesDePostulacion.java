package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.CriterioDeEmparejamiento;
import bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Lo que tiene que ser cierto para que una postulacion se pueda abrir (CU-68), en UN solo lugar.
 *
 * <p>Son solo LECTURAS: no escriben nada. Eso permite comprobarlas ANTES de un paso irreversible en otro servicio —el
 * canje de una invitacion consume el enlace en {@code identidad}— sin que la comprobacion y la escritura puedan
 * decir cosas distintas (regla 98.3.5: ningun paso irreversible va primero). {@link CU68Postular#postular} las
 * vuelve a ejecutar dentro de su transaccion, donde la escritura las protege de la carrera.
 */
final class PrecondicionesDePostulacion {

    private PrecondicionesDePostulacion() {}

    /** Lanza el rechazo de negocio que corresponda; si todo cumple devuelve el criterio de emparejamiento vigente. */
    static CriterioDeEmparejamiento exigir(
            EmparejamientoRepositorio emparejamientos,
            DSLContext dsl,
            CU68Postular.EntradaPostulacion entrada,
            UUID usuarioId,
            OffsetDateTime ahora) {
        // La restriccion se responde diciendo QUE la levanta: rechazar sin decir
        // como salir deja a alguien fuera sin camino de vuelta.
        if (entrada.tieneRestriccionVigente()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(68, 1),
                    "Tenes una restriccion vigente.",
                    Map.of(
                            "montoQueLaLevanta",
                            entrada.montoQueLevantaLaRestriccion().toString()));
        }
        if (!entrada.kycSuficiente()) {
            throw new ErrorDeNegocio(CodigoError.de(68, 2), "Necesitas elevar tu nivel de verificacion.");
        }
        if (!emparejamientos.hayCuposLibres(dsl, entrada.grupoId())) {
            throw new ErrorDeNegocio(CodigoError.de(68, 4), "Este grupo ya no tiene cupos libres.");
        }
        if (emparejamientos.yaPostulo(dsl, entrada.grupoId(), usuarioId)) {
            throw new ErrorDeNegocio(CodigoError.de(68, 6), "Ya tenes una postulacion pendiente en este grupo.");
        }

        CriterioDeEmparejamiento criterio = emparejamientos
                .criterioVigente(dsl, ahora)
                .orElseThrow(
                        () -> new ErrorDeNegocio(CodigoError.de(68, 5), "No hay criterio de emparejamiento vigente."));

        // Entrada excluyente (CU-68 2a, B24, ya en TEST): la reputacion minima se exige a quien TIENE historial.
        if (entrada.reputacionExcluyente()
                && !entrada.reputacionSinDatos()
                && !criterio.alcanzaLaReputacion(entrada.reputacion())) {
            throw new ErrorDeNegocio(CodigoError.de(68, 3), "Tu reputacion todavia no alcanza para este grupo.");
        }
        return criterio;
    }
}

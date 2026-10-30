package bo.aportaya.grupos.web;

import bo.aportaya.grupos.dominio.Arenas;
import bo.aportaya.grupos.dominio.CondicionesDeIngreso;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Arena minima y desbloqueos en TODOS los caminos de ingreso (2026-10-30). La arena se le pregunta a
 * {@code transparencia} ANTES de abrir la transaccion del ingreso (invariante 6); que el grupo siga abierto lo
 * vuelve a mirar el caso de uso con la fila del grupo tomada (AP-CU69-17).
 *
 * <p>Falla cerrado (regla 98.2.4): si transparencia no contesta no se sabe si la persona llega a la arena ni que le
 * desbloquea, asi que NO entra. Con un grupo cerrado no se pregunta nada: el caso de uso responde lo suyo (un
 * reintento idempotente o AP-CU69-17), que es mas util que un rechazo por arena.
 */
final class ArenaDelIngreso {

    /** AP-CU68-03 REPUTACION_INSUFICIENTE: el codigo de la arena minima en los ingresos de CU-68 y CU-69. */
    static final CodigoError ARENA_DEL_INGRESO = CodigoError.de(68, 3);

    /** AP-CU64-04 ENTRANTE_SIN_REPUTACION: el mismo motivo en el traspaso, con el codigo que ya tenia CU-64. */
    static final CodigoError ARENA_DEL_TRASPASO = CodigoError.de(64, 4);

    private ArenaDelIngreso() {}

    /**
     * Exige la arena minima y el desbloqueo de quien entra a un grupo abierto. Sin condiciones (el grupo no existe) no
     * hay nada que comparar: el caso de uso responde que no existe.
     */
    static void exigir(HechosDeOtrosServicios afuera, Optional<CondicionesDeIngreso> condiciones, UUID entrante) {
        condiciones
                .filter(CondicionesDeIngreso::recibeIntegrantes)
                .ifPresent(c -> comparar(afuera, c, entrante, ARENA_DEL_INGRESO));
    }

    /**
     * El traspaso no es un ingreso nuevo (el cupo ya existe y sigue ocupado) y se hace con el grupo en marcha: la arena
     * se exige en cualquier estado. Devuelve la reputacion con el puntaje con el que se comparo, para que CU-64 mire lo
     * mismo: sin historial es el piso de la arena base, no un cero.
     */
    static HechosDeOtrosServicios.Reputacion alTraspasar(
            HechosDeOtrosServicios afuera, Optional<CondicionesDeIngreso> condiciones, UUID entrante) {
        if (condiciones.isEmpty()) return afuera.reputacion(entrante);
        Arenas.DeLaPersona persona = comparar(afuera, condiciones.get(), entrante, ARENA_DEL_TRASPASO);
        return new HechosDeOtrosServicios.Reputacion(true, persona.puntaje());
    }

    private static Arenas.DeLaPersona comparar(
            HechosDeOtrosServicios afuera, CondicionesDeIngreso c, UUID entrante, CodigoError codigoDeArena) {
        Arenas.DeLaPersona persona = afuera.arenaDe(entrante).orElseThrow(ArenaDelIngreso::sinArena);
        if (!Arenas.alcanza(persona, c.reputacionMinima())) {
            String minima = Arenas.codigoDeLaMinima(persona.catalogo(), c.reputacionMinima())
                    .orElse("");
            throw new ErrorDeNegocio(
                    codigoDeArena,
                    "Tu arena todavia no alcanza la que pide este grupo.",
                    Map.of("arenaMinima", minima));
        }
        Arenas.excedido(persona.arena(), c.cuposTotales(), c.montoAporte(), c.moneda())
                .ifPresent(d -> {
                    throw excedido(CodigoError.de(69, 18), d, "Tu arena todavia no desbloquea un grupo asi.");
                });
        return persona;
    }

    /** AP-CU69-18 al entrar, AP-CU20-07 al crear: con lo que la arena SI desbloquea, para que se pueda actuar. */
    static ErrorDeNegocio excedido(CodigoError codigo, Arenas.Desbloqueo d, String mensaje) {
        return new ErrorDeNegocio(
                codigo,
                mensaje,
                Map.of(
                        "cuposMax", d.cuposMax(),
                        "aporteMax",
                                d.aporteMax()
                                        .setScale(2, RoundingMode.UNNECESSARY)
                                        .toPlainString(),
                        "moneda", d.moneda()));
    }

    /** Sin respuesta de transparencia no se supone ninguna arena: ni se entra ni se crea. */
    static ErrorDeDominio sinArena() {
        return new ErrorDeDominio("No pudimos confirmar tu arena de reputacion. Proba de nuevo en un momento.");
    }

    /** El puntaje sin decimales de mas, para comparar con la minima que guarda el grupo. */
    static BigDecimal comoMinima(BigDecimal desde) {
        return desde.setScale(2, RoundingMode.UNNECESSARY);
    }
}

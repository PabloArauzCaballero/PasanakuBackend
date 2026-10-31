package bo.aportaya.grupos.web;

import bo.aportaya.grupos.dominio.CondicionesDeIngreso;
import bo.aportaya.grupos.dominio.Etapas;
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
 * Etapa minima y desbloqueos en TODOS los caminos de ingreso (2026-10-30). La etapa se le pregunta a
 * {@code transparencia} ANTES de abrir la transaccion del ingreso (invariante 6); que el grupo siga abierto lo
 * vuelve a mirar el caso de uso con la fila del grupo tomada (AP-CU69-17).
 *
 * <p>Falla cerrado (regla 98.2.4): si transparencia no contesta no se sabe si la persona llega a la etapa ni que le
 * desbloquea, asi que NO entra. Con un grupo cerrado no se pregunta nada: el caso de uso responde lo suyo (un
 * reintento idempotente o AP-CU69-17), que es mas util que un rechazo por etapa.
 *
 * <p>Publica (solo {@link #exigir}) para que la prueba contra PostgreSQL y un transparencia por HTTP real la ejercite
 * desde fuera del paquete web sin que la web dependa de infraestructura ({@code ArquitecturaTest}).
 */
public final class EtapaDelIngreso {

    /** AP-CU68-03 REPUTACION_INSUFICIENTE: el codigo de la etapa minima en los ingresos de CU-68 y CU-69. */
    static final CodigoError ETAPA_DEL_INGRESO = CodigoError.de(68, 3);

    /** AP-CU64-04 ENTRANTE_SIN_REPUTACION: el mismo motivo en el traspaso, con el codigo que ya tenia CU-64. */
    static final CodigoError ETAPA_DEL_TRASPASO = CodigoError.de(64, 4);

    private EtapaDelIngreso() {}

    /**
     * Exige la etapa minima y el desbloqueo de quien entra a un grupo abierto. Sin condiciones (el grupo no existe) no
     * hay nada que comparar: el caso de uso responde que no existe. Devuelve la etapa con la que se comparo (vacia si
     * no se comparo nada), para que quien decide guarde ese piso y no pregunte otra vez.
     */
    public static Optional<Etapas.DeLaPersona> exigir(
            HechosDeOtrosServicios afuera, Optional<CondicionesDeIngreso> condiciones, UUID entrante) {
        return condiciones
                .filter(CondicionesDeIngreso::recibeIntegrantes)
                .map(c -> comparar(afuera, c, entrante, ETAPA_DEL_INGRESO));
    }

    /**
     * El traspaso no es un ingreso nuevo (el cupo ya existe y sigue ocupado) y se hace con el grupo en marcha: la etapa
     * se exige en cualquier estado. Devuelve la reputacion con el puntaje con el que se comparo, para que CU-64 mire lo
     * mismo: el piso de la etapa del entrante, nunca su puntaje exacto (es de otra persona, regla 90.2.8). Sin grupo no
     * se pregunta nada: CU-64 responde que no existe.
     */
    static HechosDeOtrosServicios.Reputacion alTraspasar(
            HechosDeOtrosServicios afuera, Optional<CondicionesDeIngreso> condiciones, UUID entrante) {
        if (condiciones.isEmpty()) return SIN_CONSULTAR;
        Etapas.DeLaPersona persona = comparar(afuera, condiciones.get(), entrante, ETAPA_DEL_TRASPASO);
        return new HechosDeOtrosServicios.Reputacion(true, persona.puntaje());
    }

    /**
     * Lo que se le pasa a CU-64 cuando no hace falta preguntar (el reintento del mismo traspaso o un grupo que no
     * existe): CU-64 responde antes de mirar la reputacion, asi que no se lee la de nadie.
     */
    static final HechosDeOtrosServicios.Reputacion SIN_CONSULTAR =
            new HechosDeOtrosServicios.Reputacion(false, BigDecimal.ZERO);

    private static Etapas.DeLaPersona comparar(
            HechosDeOtrosServicios afuera, CondicionesDeIngreso c, UUID entrante, CodigoError codigoDeEtapa) {
        Etapas.DeLaPersona persona = afuera.etapaDe(entrante).orElseThrow(EtapaDelIngreso::sinEtapa);
        if (!Etapas.alcanza(persona, c.reputacionMinima())) {
            String minima = Etapas.codigoDeLaMinima(persona.catalogo(), c.reputacionMinima())
                    .orElse("");
            throw new ErrorDeNegocio(
                    codigoDeEtapa,
                    "Tu etapa todavia no alcanza la que pide este grupo.",
                    Map.of("etapaMinima", minima));
        }
        Etapas.excedido(persona.etapa(), c.cuposTotales(), c.montoAporte(), c.moneda())
                .ifPresent(d -> {
                    throw excedido(CodigoError.de(69, 18), d, "Tu etapa todavia no desbloquea un grupo asi.");
                });
        return persona;
    }

    /** AP-CU69-18 al entrar, AP-CU20-07 al crear: con lo que la etapa SI desbloquea, para que se pueda actuar. */
    static ErrorDeNegocio excedido(CodigoError codigo, Etapas.Desbloqueo d, String mensaje) {
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

    /** Sin respuesta de transparencia no se supone ninguna etapa: ni se entra ni se crea. */
    static ErrorDeDominio sinEtapa() {
        return new ErrorDeDominio("No pudimos confirmar tu etapa de reputacion. Proba de nuevo en un momento.");
    }

    /** El puntaje sin decimales de mas, para comparar con la minima que guarda el grupo. */
    static BigDecimal comoMinima(BigDecimal desde) {
        return desde.setScale(2, RoundingMode.UNNECESSARY);
    }
}

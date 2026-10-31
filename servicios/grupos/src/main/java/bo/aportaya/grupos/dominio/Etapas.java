package bo.aportaya.grupos.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Etapas de reputacion (2026-10-30): tramos CONTINUOS del puntaje que publica {@code transparencia}
 * ({@code GET /reputacion/etapas}). Grupos no las guarda ni las calcula: las recibe por contrato y aca solo compara.
 *
 * <p>Dos reglas viven sobre ellas. La etapa minima del grupo: entra quien tiene un puntaje mayor o igual al
 * {@code desde} de esa etapa (el grupo guarda ese {@code desde} en {@code reputacion_minima}). Y el desbloqueo de cada
 * etapa: cuantos cupos y cuanto aporte por periodo puede tener un grupo que crea o integra quien esta en ella. Solo se
 * aplica el desbloqueo {@code VIGENTE}; uno {@code PROPUESTO} todavia no limita nada (sus valores esperan la
 * confirmacion de negocio, regla 97.5.4).
 *
 * <p>Atomo puro: sin red ni base. Quien llama ya resolvio los hechos afuera y decide que codigo de error corresponde.
 */
public final class Etapas {

    private static final String VIGENTE = "VIGENTE";

    private Etapas() {}

    /** Lo que se muestra de la etapa de otra persona: codigo y nombre, nunca el puntaje ni el avance (regla 90.2.8). */
    public record Visible(String codigo, String nombre) {}

    /** Una etapa del catalogo. {@code desbloqueo} es nulo si la etapa no limita nada. */
    public record Etapa(String codigo, int orden, BigDecimal desde, BigDecimal hasta, Desbloqueo desbloqueo) {}

    /** Lo que desbloquea una etapa: cupos totales y aporte por periodo maximos, con su moneda. */
    public record Desbloqueo(int cuposMax, BigDecimal aporteMax, String moneda, String estado) {
        public boolean vigente() {
            return VIGENTE.equals(estado);
        }
    }

    /**
     * Lo que se sabe de una persona: el puntaje con el que se la compara, su etapa y el catalogo entero (para
     * nombrar la etapa minima de un grupo). El puntaje es el {@code desde} de la etapa que le asigna
     * {@code transparencia} (sin historial, la del puntaje base del modelo): grupos no conoce el puntaje exacto de nadie
     * (regla 90.2.8) y no lo necesita, porque con tramos contiguos {@code desde >= minima} equivale a
     * {@code puntaje >= minima}. «No se sabe» no es «malo», pero tampoco se le supone mas que el piso de su etapa.
     */
    public record DeLaPersona(BigDecimal puntaje, Etapa etapa, List<Etapa> catalogo) {}

    /** La etapa que contiene ese puntaje: {@code desde} incluido y {@code hasta} excluido; la ultima incluye su hasta. */
    public static Optional<Etapa> contiene(List<Etapa> catalogo, BigDecimal puntaje) {
        if (puntaje == null || catalogo.isEmpty()) return Optional.empty();
        Etapa ultima = catalogo.get(catalogo.size() - 1);
        for (Etapa etapa : catalogo) {
            boolean dentro = puntaje.compareTo(etapa.desde()) >= 0
                    && (puntaje.compareTo(etapa.hasta()) < 0
                            || (etapa == ultima && puntaje.compareTo(etapa.hasta()) == 0));
            if (dentro) return Optional.of(etapa);
        }
        return Optional.empty();
    }

    public static Optional<Etapa> porCodigo(List<Etapa> catalogo, String codigo) {
        return catalogo.stream().filter(a -> a.codigo().equals(codigo)).findFirst();
    }

    /** Si la persona llega a la etapa minima del grupo. Un grupo sin minimo (nulo o cero) no pide nada. */
    public static boolean alcanza(DeLaPersona persona, BigDecimal reputacionMinima) {
        return reputacionMinima == null
                || reputacionMinima.signum() <= 0
                || persona.puntaje().compareTo(reputacionMinima) >= 0;
    }

    /**
     * El desbloqueo VIGENTE de la etapa, si no cubre un grupo de esos cupos y ese aporte. Un aporte en otra moneda que
     * la del desbloqueo tampoco queda cubierto: comparar importes de monedas distintas sin tipo de cambio esta
     * prohibido (regla 91.1.2), asi que se deniega por omision. Sin desbloqueo, o con uno PROPUESTO, no limita.
     */
    public static Optional<Desbloqueo> excedido(Etapa etapa, int cupos, BigDecimal aporte, String moneda) {
        Desbloqueo d = etapa.desbloqueo();
        if (d == null || !d.vigente()) return Optional.empty();
        boolean cubre = cupos <= d.cuposMax() && d.moneda().equals(moneda) && aporte.compareTo(d.aporteMax()) <= 0;
        return cubre ? Optional.empty() : Optional.of(d);
    }

    /** El codigo de la etapa minima que pide un grupo, para mostrarlo; vacio si no pide ninguna. */
    public static Optional<String> codigoDeLaMinima(List<Etapa> catalogo, BigDecimal reputacionMinima) {
        if (reputacionMinima == null || reputacionMinima.signum() <= 0) return Optional.empty();
        return contiene(catalogo, reputacionMinima).map(Etapa::codigo);
    }
}

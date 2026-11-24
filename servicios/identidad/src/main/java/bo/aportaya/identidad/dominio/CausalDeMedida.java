package bo.aportaya.identidad.dominio;

import java.util.Optional;

/**
 * Catalogo cerrado de causales de una medida sobre la cuenta del participante (CU-134, D-1).
 *
 * <p>Mora, orden de autoridad y sospecha de lavado NO estan aca: van por CU-27, CU-17 y CU-44. Que
 * el catalogo sea un enum cerrado es lo que impide suspender a alguien por un motivo inventado en el
 * momento; la base lo repite con {@code ck_expediente_medida_cuenta_causal}.
 */
public enum CausalDeMedida {
    CUENTA_COMPROMETIDA(true),
    FRAUDE_CON_EVIDENCIA(true),
    ABUSO_CONTRA_PARTICIPANTES(false),
    CUENTA_DUPLICADA_O_DE_TERCERO(false);

    private final boolean admiteCautelar;

    CausalDeMedida(boolean admiteCautelar) {
        this.admiteCautelar = admiteCautelar;
    }

    /** Solo la cuenta comprometida y el fraude con evidencia justifican actuar antes del descargo. */
    public boolean admiteCautelar() {
        return admiteCautelar;
    }

    /** Una medida firme por esta causal produce evento de reputacion (D-8): la victima no puntua. */
    public boolean afectaReputacion() {
        return this == FRAUDE_CON_EVIDENCIA || this == ABUSO_CONTRA_PARTICIPANTES;
    }

    /** La causal si esta en el catalogo; vacio si no, para que el organismo responda AP-CU134-04. */
    public static Optional<CausalDeMedida> del(String codigo) {
        for (CausalDeMedida c : values()) {
            if (c.name().equals(codigo)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}

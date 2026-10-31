package bo.aportaya.grupos.web;

import bo.aportaya.grupos.dominio.CondicionesDeIngreso;
import bo.aportaya.grupos.dominio.Etapas;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Un catalogo de etapas SINTETICO para las pruebas de la capa web (2026-10-30). No es el catalogo de
 * {@code transparencia}: tres tramos con numeros redondos para que cada prueba se lea sola. MEDIA trae un desbloqueo
 * VIGENTE de 10 cupos y 500.00 BOB; ALTA, uno PROPUESTO (no limita).
 */
final class EtapasDePrueba {

    static final Etapas.Etapa BAJA =
            new Etapas.Etapa("BAJA", 0, new BigDecimal("0.00"), new BigDecimal("500.00"), null);
    static final Etapas.Etapa MEDIA = new Etapas.Etapa(
            "MEDIA",
            1,
            new BigDecimal("500.00"),
            new BigDecimal("700.00"),
            new Etapas.Desbloqueo(10, new BigDecimal("500.00"), "BOB", "VIGENTE"));
    static final Etapas.Etapa ALTA = new Etapas.Etapa(
            "ALTA",
            2,
            new BigDecimal("700.00"),
            new BigDecimal("1000.00"),
            new Etapas.Desbloqueo(3, new BigDecimal("1.00"), "BOB", "PROPUESTO"));
    static final List<Etapas.Etapa> CATALOGO = List.of(BAJA, MEDIA, ALTA);

    private EtapasDePrueba() {}

    /** Alguien con ese puntaje, en la etapa que lo contiene. */
    static Optional<Etapas.DeLaPersona> con(String puntaje) {
        BigDecimal valor = new BigDecimal(puntaje);
        return Optional.of(
                new Etapas.DeLaPersona(valor, Etapas.contiene(CATALOGO, valor).orElseThrow(), CATALOGO));
    }

    /** Un grupo abierto con esa etapa minima, {@code cupos} cupos y {@code aporte} BOB por periodo. */
    static Optional<CondicionesDeIngreso> grupo(UUID grupoId, Etapas.Etapa minima, int cupos, String aporte) {
        return Optional.of(new CondicionesDeIngreso(
                grupoId, "ABIERTO_A_INSCRIPCION", minima.desde(), cupos, new BigDecimal(aporte), "BOB"));
    }
}

package bo.aportaya.grupos.web;

import bo.aportaya.grupos.dominio.Arenas;
import bo.aportaya.grupos.dominio.CondicionesDeIngreso;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Un catalogo de arenas SINTETICO para las pruebas de la capa web (2026-10-30). No es el catalogo de
 * {@code transparencia}: tres tramos con numeros redondos para que cada prueba se lea sola. MEDIA trae un desbloqueo
 * VIGENTE de 10 cupos y 500.00 BOB; ALTA, uno PROPUESTO (no limita).
 */
final class ArenasDePrueba {

    static final Arenas.Arena BAJA =
            new Arenas.Arena("BAJA", 0, new BigDecimal("0.00"), new BigDecimal("500.00"), null);
    static final Arenas.Arena MEDIA = new Arenas.Arena(
            "MEDIA",
            1,
            new BigDecimal("500.00"),
            new BigDecimal("700.00"),
            new Arenas.Desbloqueo(10, new BigDecimal("500.00"), "BOB", "VIGENTE"));
    static final Arenas.Arena ALTA = new Arenas.Arena(
            "ALTA",
            2,
            new BigDecimal("700.00"),
            new BigDecimal("1000.00"),
            new Arenas.Desbloqueo(3, new BigDecimal("1.00"), "BOB", "PROPUESTO"));
    static final List<Arenas.Arena> CATALOGO = List.of(BAJA, MEDIA, ALTA);

    private ArenasDePrueba() {}

    /** Alguien con ese puntaje, en la arena que lo contiene. */
    static Optional<Arenas.DeLaPersona> con(String puntaje) {
        BigDecimal valor = new BigDecimal(puntaje);
        return Optional.of(
                new Arenas.DeLaPersona(valor, Arenas.contiene(CATALOGO, valor).orElseThrow(), CATALOGO));
    }

    /** Un grupo abierto con esa arena minima, {@code cupos} cupos y {@code aporte} BOB por periodo. */
    static Optional<CondicionesDeIngreso> grupo(UUID grupoId, Arenas.Arena minima, int cupos, String aporte) {
        return Optional.of(new CondicionesDeIngreso(
                grupoId, "ABIERTO_A_INSCRIPCION", minima.desde(), cupos, new BigDecimal(aporte), "BOB"));
    }
}

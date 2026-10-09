package bo.aportaya.grupos.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Lo que queda congelado cuando se compromete un sorteo: quiénes entran (cupos y participantes, en orden de
 * número), contra qué calendario se reparten los turnos y con qué reglas del grupo.
 *
 * <p>El texto canónico es lo que se hashea y lo que cualquiera recomputa: una línea por cupo o periodo, sin
 * adornos, para que verificarlo no dependa de una librería. El hash entra en las entropías del compromiso, de
 * modo que cambiar el plantel, el calendario o las reglas después de publicar el compromiso hace que el
 * resultado ya no verifique.
 */
public record SnapshotDeSorteo(List<CupoCongelado> roster, List<PeriodoCongelado> periodos, String reglas) {

    /** Un cupo ocupado del plantel congelado; el participante es "-" si el cupo no lo registra. */
    public record CupoCongelado(int numero, UUID cupoId, UUID participanteId) {
        String linea() {
            return numero + "|" + cupoId + "|" + (participanteId == null ? "-" : participanteId);
        }
    }

    public record PeriodoCongelado(int numero, UUID periodoId, LocalDate fechaLimitePago) {
        String linea() {
            return numero + "|" + periodoId + "|" + fechaLimitePago;
        }
    }

    public String rosterCanonico() {
        return String.join("\n", roster.stream().map(CupoCongelado::linea).toList());
    }

    public String periodosCanonico() {
        return String.join("\n", periodos.stream().map(PeriodoCongelado::linea).toList());
    }

    /** SHA-256 del texto canónico completo, en hexadecimal minúscula. */
    public String hash() {
        return sha256(rosterCanonico() + "\n--\n" + periodosCanonico() + "\n--\n" + reglas);
    }

    /** La entropía que ata el compromiso a este plantel, calendario y reglas. */
    public String entropia() {
        return "snapshot:" + hash();
    }

    public List<UUID> cuposEnOrden() {
        return roster.stream().map(CupoCongelado::cupoId).toList();
    }

    public List<UUID> periodosEnOrden() {
        return periodos.stream().map(PeriodoCongelado::periodoId).toList();
    }

    /** Reconstruye el roster congelado desde su texto canónico. */
    public static List<CupoCongelado> leerRoster(String canonico) {
        List<CupoCongelado> salida = new ArrayList<>();
        for (String linea : canonico.split("\n")) {
            if (linea.isBlank()) continue;
            String[] partes = linea.split("\\|");
            salida.add(new CupoCongelado(
                    Integer.parseInt(partes[0]),
                    UUID.fromString(partes[1]),
                    "-".equals(partes[2]) ? null : UUID.fromString(partes[2])));
        }
        return List.copyOf(salida);
    }

    public static List<PeriodoCongelado> leerPeriodos(String canonico) {
        List<PeriodoCongelado> salida = new ArrayList<>();
        for (String linea : canonico.split("\n")) {
            if (linea.isBlank()) continue;
            String[] partes = linea.split("\\|");
            salida.add(new PeriodoCongelado(
                    Integer.parseInt(partes[0]), UUID.fromString(partes[1]), LocalDate.parse(partes[2])));
        }
        return List.copyOf(salida);
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("esta JVM no trae SHA-256", imposible);
        }
    }
}

package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.GrupoNuevo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * El texto que cada participante firma y su hash. Salio de {@link CU20CrearGrupo} (limite de 300 lineas) sin cambiar
 * lo que hace: lo usan el alta y {@link CU69IniciarGrupo}, que publica la version nueva con los numeros del grupo
 * achicado (2026-10-30).
 */
final class TextoDelReglamento {

    private TextoDelReglamento() {}

    /**
     * Se guarda con su hash porque lo que se acepta es un texto EXACTO: sin el hash, «acepte el reglamento» no dice
     * cual.
     */
    static String de(GrupoNuevo datos) {
        return """
               Reglamento de %s.
               Aporte: %s %s por periodo, %s, dia %d.
               Participantes: %d. Cada uno cobra una vez.
               Fondo de cada periodo: %s %s.
               """
                .formatted(
                        datos.nombre(),
                        datos.montoDelAporte().moneda(),
                        datos.montoDelAporte(),
                        datos.periodicidad().toLowerCase(java.util.Locale.ROOT),
                        datos.diaDeCobro(),
                        datos.cupos(),
                        datos.montoDelAporte().moneda(),
                        datos.fondoPorPeriodo());
    }

    static String hash(String texto) {
        try {
            byte[] digestion = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder(digestion.length * 2);
            for (byte b : digestion) {
                hexadecimal.append("%02x".formatted(b));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tiene que existir en cualquier JVM", e);
        }
    }
}

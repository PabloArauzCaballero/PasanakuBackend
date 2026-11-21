package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.dominio.puertos.DesafioDeFactor;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * El segundo factor que corre en TODOS los perfiles (F1.S3.M2).
 *
 * <p>Enruta por tipo: {@code TOTP} se verifica contra el secreto enrolado
 * ({@link VerificadorTotp}); el resto (el codigo por correo) sigue en {@link DesafioLocal}.
 * Reemplaza a {@code DesafioDeDesarrollo}, que con el perfil {@code local} —el que corre
 * TEST— aceptaba {@code 000000} para cualquiera (IDCU-HUECO-077 / IDCU-SEG-004). No hay
 * codigo fijo en ningun entorno.
 */
@Component
@Primary
public class DesafioPorTipo implements DesafioDeFactor {

    static final String TOTP = "TOTP";

    private final DesafioLocal porCanal;
    private final VerificadorTotp totp;

    public DesafioPorTipo(DesafioLocal porCanal, VerificadorTotp totp) {
        this.porCanal = porCanal;
        this.totp = totp;
    }

    @Override
    public UUID emitir(UUID usuarioId, String tipoDeFactor) {
        // El TOTP no se emite: lo calcula la app del titular. Solo se identifica el desafio.
        return TOTP.equals(tipoDeFactor) ? UUID.randomUUID() : porCanal.emitir(usuarioId, tipoDeFactor);
    }

    @Override
    public UUID emitir(UUID usuarioId, String tipoDeFactor, String destino) {
        return TOTP.equals(tipoDeFactor) ? UUID.randomUUID() : porCanal.emitir(usuarioId, tipoDeFactor, destino);
    }

    @Override
    public boolean entregaPorCorreoConfirmada(UUID usuarioId) {
        return porCanal.entregaPorCorreoConfirmada(usuarioId);
    }

    @Override
    public boolean validar(UUID usuarioId, String tipoDeFactor, String valorPresentado) {
        if (TOTP.equals(tipoDeFactor)) {
            return totp.validar(usuarioId, valorPresentado);
        }
        return porCanal.validar(usuarioId, tipoDeFactor, valorPresentado);
    }
}

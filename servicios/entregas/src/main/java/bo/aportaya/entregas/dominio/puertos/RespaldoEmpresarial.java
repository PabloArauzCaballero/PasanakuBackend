package bo.aportaya.entregas.dominio.puertos;

import bo.aportaya.plataforma.dominio.Dinero;
import java.util.Optional;
import java.util.UUID;

/**
 * El respaldo empresarial reservado por ciclo, que vive en {@code garantia}.
 *
 * <p>Se pide cubrir el faltante de UN turno y la respuesta es un resultado de negocio:
 * {@code APLICADA}, {@code INSUFICIENTE} o {@code SIN_RESERVA}. La llamada es idempotente por
 * turno: repetirla devuelve la misma cobertura, no otra. Vacio significa que {@code garantia} no
 * respondio, y ahi NO se sabe si cubrio o no: quien pregunta no avanza y reintenta.
 */
public interface RespaldoEmpresarial {

    enum Resultado {
        APLICADA,
        INSUFICIENTE,
        SIN_RESERVA
    }

    record Cobertura(Resultado resultado, UUID coberturaId, Dinero cubierto, Dinero sinCubrir) {}

    Optional<Cobertura> cubrir(UUID grupoId, UUID periodoId, UUID turnoId, Dinero faltanteEsperado);
}

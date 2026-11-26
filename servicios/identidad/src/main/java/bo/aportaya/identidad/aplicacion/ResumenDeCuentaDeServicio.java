package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.CuentasDeServicioRepositorio.Fila;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Lo que el listado de cuentas de servicio muestra (CU-136): sin credencial ni hash, nunca. */
public record ResumenDeCuentaDeServicio(
        UUID id,
        UUID usuarioId,
        String rolCodigo,
        String nombre,
        String estado,
        OffsetDateTime venceEn,
        OffsetDateTime creadaEn,
        OffsetDateTime credencialRotadaEn,
        Optional<OffsetDateTime> revocadaEn) {

    static ResumenDeCuentaDeServicio de(Fila f) {
        return new ResumenDeCuentaDeServicio(
                f.id(),
                f.usuarioId(),
                f.rolCodigo(),
                f.nombre(),
                f.estado(),
                f.venceEn(),
                f.creadaEn(),
                f.credencialRotadaEn(),
                f.revocadaEn());
    }
}

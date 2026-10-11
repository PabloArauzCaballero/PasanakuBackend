package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La parte propia de la consulta de saldo: lo que dice el libro, con la hora del corte.
 *
 * <p>Es de solo lectura y aun asi abre transaccion: el contexto de sesion viaja con
 * {@code SET LOCAL} dentro de ella (invariante 3), y sin transaccion la politica de
 * fila no se aplica — la consulta no falla, devuelve las cuentas de todos.
 *
 * <p>Vive aparte de {@link ConsultarSaldo} porque lo que {@code ConsultarSaldo} le pregunta a
 * {@code aportes} es una llamada de red, y esa va FUERA de la transaccion (invariante 6).
 */
@Service
public class LecturaDeSaldo {

    private final Datos datos;
    private final CuentaBilleteraRepositorio cuentas;
    private final Reloj reloj;

    public LecturaDeSaldo(Datos datos, CuentaBilleteraRepositorio cuentas, Reloj reloj) {
        this.datos = datos;
        this.cuentas = cuentas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public ConsultarSaldo.Salida ejecutar(UUID cuentaId, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            var cuenta = cuentas.ver(dsl, cuentaId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(13, 1), "Esa cuenta no existe."));
            return new ConsultarSaldo.Salida(
                    cuenta.id(), cuenta.disponible(), cuenta.retenido(), ahora, cuenta.usuarioId(), null);
        });
    }
}

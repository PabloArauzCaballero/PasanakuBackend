package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * El saldo de una billetera, con la hora del corte, de quien es y cuanto esta afectado a un pozo.
 *
 * <p>Devuelve tambien {@code alCorteDe} porque un saldo sin momento no se puede
 * comparar con nada: dos capturas distintas del mismo numero no dicen si algo se movio.
 *
 * <p>Los numeros salen del LIBRO (H2.S1.M5), no de la columna cache de la cuenta: ver
 * {@code CuentaBilleteraRepositorio}. {@code titularId} lo pide {@code inversiones} para comparar
 * la cuenta con la sesion, y {@code comprometidoEnPozos} es la parte del disponible que
 * {@code aportes} dice afectada a obligaciones (deuda vigente mas lo por aportar): no es un hecho
 * de este servicio, se pregunta, y se pregunta aca, antes y fuera de la transaccion de lectura.
 * Sin respuesta de {@code aportes} el campo se OMITE: el consumidor falla cerrado antes que
 * invertir plata que quiza es de un pozo.
 */
@Service
public class ConsultarSaldo {

    private final LecturaDeSaldo lectura;
    private final HechosDeOtrosServicios afuera;

    public ConsultarSaldo(LecturaDeSaldo lectura, HechosDeOtrosServicios afuera) {
        this.lectura = lectura;
        this.afuera = afuera;
    }

    public Salida ejecutar(UUID cuentaId, ContextoSesion ctx) {
        Salida propia = lectura.ejecutar(cuentaId, ctx);
        if (propia.titularId() == null) {
            // Una cuenta de grupo o de sistema no tiene un titular persona al que preguntarle.
            return propia;
        }
        Dinero comprometido = afuera.comprometidoEnPozos(
                        propia.titularId(), propia.disponible().moneda())
                .orElse(null);
        return new Salida(
                propia.cuentaId(),
                propia.disponible(),
                propia.retenido(),
                propia.alCorteDe(),
                propia.titularId(),
                comprometido);
    }

    /**
     * @param titularId la persona dueña de la cuenta; {@code null} si es de un grupo o del sistema
     * @param comprometidoEnPozos {@code null} cuando {@code aportes} no lo informo
     */
    public record Salida(
            UUID cuentaId,
            Dinero disponible,
            Dinero retenido,
            OffsetDateTime alCorteDe,
            UUID titularId,
            Dinero comprometidoEnPozos) {

        /** La forma sin titular ni compromisos: lo que devolvia antes de H2.S1.M5. */
        public Salida(UUID cuentaId, Dinero disponible, Dinero retenido, OffsetDateTime alCorteDe) {
            this(cuentaId, disponible, retenido, alCorteDe, null, null);
        }
    }
}

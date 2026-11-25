package bo.aportaya.identidad.aplicacion;

import static bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.MOTIVO_REQUERIDO;
import static bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.NOMBRE_INVALIDO;
import static bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.VENCIMIENTO_INVALIDO;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

/** Las validaciones de forma de CU-136: nombre, motivo y vencimiento dentro del tope configurado. */
final class ValidacionesDeCuentaDeServicio {

    private static final int LARGO_MAXIMO_NOMBRE = 80;
    private static final int LARGO_MAXIMO_MOTIVO = 160;

    private ValidacionesDeCuentaDeServicio() {}

    static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.length() > LARGO_MAXIMO_NOMBRE) {
            throw new ErrorDeNegocio(NOMBRE_INVALIDO, "El nombre es obligatorio (hasta 80 caracteres).");
        }
    }

    static void validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank() || motivo.length() > LARGO_MAXIMO_MOTIVO) {
            throw new ErrorDeNegocio(MOTIVO_REQUERIDO, "Escribi el motivo (hasta 160 caracteres).");
        }
    }

    /** Vence en el futuro y dentro del tope configurado ({@code aportaya.cuentas-servicio.vigencia-maxima}). */
    static void validarVencimiento(OffsetDateTime vence, Duration vigenciaMaxima, OffsetDateTime ahora) {
        if (vence == null || !vence.isAfter(ahora) || vence.isAfter(ahora.plus(vigenciaMaxima))) {
            throw new ErrorDeNegocio(
                    VENCIMIENTO_INVALIDO,
                    "El vencimiento tiene que ser futuro y no pasar de la vigencia maxima.",
                    Map.of("vigenciaMaxima", vigenciaMaxima.toString()));
        }
    }
}

package bo.aportaya.identidad.dominio;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;

/**
 * GU-17 (decision de Pablo, 2026-11-26): la fecha de nacimiento del personal interno la declara quien da el alta (o la
 * completa despues): no hay valor por omision, no puede ser futura y la persona tiene que haber cumplido la edad minima
 * hoy (cumplirla hoy mismo alcanza). Quien nacio un 29 de febrero cumple el 1 de marzo del anio no bisiesto.
 */
public final class FechaDeNacimientoDelOperador {
    /** A9: el personal interno es mayor de edad. */
    public static final int EDAD_MINIMA = 18;

    private FechaDeNacimientoDelOperador() {}

    /** AP-CU08-13 (ausente o futura) y AP-CU08-14 (menor de {@link #EDAD_MINIMA} anos). */
    public static void exigirReal(LocalDate fecha, LocalDate hoy) {
        if (fecha == null || fecha.isAfter(hoy)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(8, 13), "La fecha de nacimiento tiene que ser real: no puede faltar ni ser futura.");
        }
        if (fecha.isAfter(hoy.minusYears(EDAD_MINIMA))) {
            throw new ErrorDeNegocio(
                    CodigoError.de(8, 14), "El personal interno tiene que ser mayor de " + EDAD_MINIMA + " anos.");
        }
    }
}

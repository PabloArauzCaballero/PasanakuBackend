package bo.aportaya.grupos.dominio;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Un dia no habil CON su procedencia: el mecanismo que impide cargar un feriado inventado.
 *
 * <p>CU-59: «un feriado sin fuente no se carga». Este registro es la validacion de ese borde, pura,
 * sin base: la fuente es obligatoria, el alcance debe estar completo, la fecha no puede estar en el
 * pasado en una carga anual y la descripcion que se guarda lleva la fuente escrita (la columna
 * {@code dia_no_habil.descripcion} es hoy el unico lugar donde puede quedar la constancia).
 *
 * <p>Un dia SINTETICO (de prueba, de simulacro o de ensayo) queda marcado en la propia descripcion y
 * no puede confundirse con uno oficial. Que escriba en {@code catalogo.dia_no_habil} es otra
 * cuestion: ese esquema es de solo lectura para los servicios (ADR-029) y quien lo siembra es una
 * decision abierta, registrada en la ficha del carril O2.
 */
public record FeriadoConProcedencia(
        LocalDate fecha,
        String descripcion,
        AlcanceDeCalendario alcance,
        Optional<UUID> grupoId,
        String fuente,
        boolean sintetico) {

    /** Largo de {@code dia_no_habil.descripcion}. */
    public static final int LARGO_MAXIMO = 120;

    public static final String MARCA_SINTETICA = "[SINTETICO - no apto para produccion] ";

    /** Valida y arma el feriado. {@code hoy} se inyecta: la fecha de «pasado» no sale del reloj de la maquina. */
    public static FeriadoConProcedencia declarar(
            LocalDate fecha,
            String descripcion,
            AlcanceDeCalendario alcance,
            Optional<UUID> grupoId,
            String fuente,
            boolean sintetico,
            LocalDate hoy) {
        if (fuente == null || fuente.isBlank()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(59, 4), "Un feriado sin decreto, resolucion o calendario oficial no se carga.");
        }
        if (!alcance.completoCon(grupoId)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(59, 2), "Un feriado de grupo necesita decir de que grupo se trata.");
        }
        if (fecha.isBefore(hoy)) {
            throw new ErrorDeNegocio(CodigoError.de(59, 3), "No se carga un feriado con fecha vencida: " + fecha + ".");
        }
        if (descripcion == null || descripcion.isBlank() || descripcion.strip().length() < 3) {
            throw new ErrorDeNegocio(
                    CodigoError.de(59, 4), "El feriado necesita una descripcion de al menos 3 letras.");
        }
        var cargado =
                new FeriadoConProcedencia(fecha, descripcion.strip(), alcance, grupoId, fuente.strip(), sintetico);
        if (cargado.paraGuardar().length() > LARGO_MAXIMO) {
            throw new ErrorDeNegocio(
                    CodigoError.de(59, 4),
                    "La descripcion con la fuente supera " + LARGO_MAXIMO + " caracteres: abreviala.");
        }
        return cargado;
    }

    /** Lo que se escribe en {@code descripcion}: marca (si es sintetico), texto y fuente. */
    public String paraGuardar() {
        return (sintetico ? MARCA_SINTETICA : "") + descripcion + " | Fuente: " + fuente;
    }
}

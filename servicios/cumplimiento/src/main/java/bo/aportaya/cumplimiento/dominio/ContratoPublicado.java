package bo.aportaya.cumplimiento.dominio;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Un contrato de adhesion que rige hoy, tal como se publica.
 *
 * <p>Vive en el dominio y no en {@code infraestructura} porque lo lee la web, y
 * {@code laWebNoTieneReglas} prohibe que un controlador toque un tipo del repositorio.
 * Es la regla que impide que el contrato HTTP quede atado a la forma de una tabla: el
 * dia que la columna se llame distinto, cambia el repositorio y nada mas.
 *
 * <p>No lleva {@code estado}: todo lo que sale como publicado esta vigente, y repetirlo
 * invita a volver a filtrar donde ya se filtro.
 *
 * <p>{@code numeroRegistro} y {@code fechaRegistro} son opcionales a proposito: un
 * contrato de adhesion se registra ante ASFI, y hasta que ese registro exista no hay
 * numero que mostrar. Un numero inventado en este campo es peor que un campo vacio.
 */
public record ContratoPublicado(
        UUID id,
        String codigo,
        short version,
        String tipo,
        String urlDocumento,
        String hashDocumento,
        OffsetDateTime vigenteDesde,
        Optional<String> numeroRegistro,
        Optional<LocalDate> fechaRegistro) {}

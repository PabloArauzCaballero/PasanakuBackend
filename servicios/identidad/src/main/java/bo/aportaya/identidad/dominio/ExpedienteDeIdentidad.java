package bo.aportaya.identidad.dominio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lo que ve quien revisa un expediente en el portal de riesgo.
 *
 * <p><b>El numero del documento no viaja</b>: solo el tipo y el lugar de expedicion.
 * Quien decide mira la foto y la compara con lo declarado; para eso no hace falta que
 * la cola sea un padron de numeros de cedula, y una lista que los lleva es una
 * filtracion esperando que alguien la exporte.
 *
 * <p>Vive en el dominio y no en infraestructura porque lo devuelve el caso de uso y lo
 * consume la pagina — y la pagina no depende de infraestructura (ArquitecturaTest).
 */
public record ExpedienteDeIdentidad(
        UUID verificacionId,
        UUID usuarioId,
        String nombreCompleto,
        String documento,
        String estado,
        OffsetDateTime iniciadaEn,
        OffsetDateTime resueltaEn,
        String motivoRechazo,
        List<String> fotos) {}

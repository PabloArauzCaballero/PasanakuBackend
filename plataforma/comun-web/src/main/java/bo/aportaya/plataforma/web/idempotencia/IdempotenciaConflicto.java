package bo.aportaya.plataforma.web.idempotencia;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;

/**
 * La misma clave de idempotencia, la misma operacion, pero un cuerpo distinto al del
 * primer intento.
 *
 * <p>No es un replay: el cliente esta usando la clave para OTRA cosa, y eso es un
 * defecto del cliente, no de la red. Por eso {@code 409} y no {@code 200} — a
 * diferencia de {@link OperacionRepetida}, que es exactamente el mismo cuerpo.
 *
 * <p>Subclase de {@link ErrorDeNegocio} y no un tipo aparte: hereda {@code detalle()} y
 * el {@code codigo}, y el manejador la distingue por tipo (mas especifico que
 * {@code ErrorDeNegocio} generico), igual que ya hace con {@link OperacionRepetida}
 * respecto de {@code ErrorDeDominio}.
 */
public class IdempotenciaConflicto extends ErrorDeNegocio {

    private static final long serialVersionUID = 1L;

    /** {@code AP-CU00-01}: CU 0 porque esto es infraestructura, no un caso de uso. */
    public static final CodigoError CODIGO = CodigoError.de(0, 1);

    public IdempotenciaConflicto() {
        super(CODIGO, "Esa clave de idempotencia ya se uso con una solicitud distinta.");
    }
}

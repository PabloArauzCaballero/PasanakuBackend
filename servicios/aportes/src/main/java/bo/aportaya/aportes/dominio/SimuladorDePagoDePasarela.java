package bo.aportaya.aportes.dominio;

import bo.aportaya.plataforma.dominio.Dinero;

/**
 * Lo que solo un DOBLE sabe hacer: emitir el aviso de pago que la pasarela real mandaria, firmado con el secreto
 * de verificacion del entorno. Permite recorrer el flujo punta a punta en {@code local}/{@code test} sin secreto
 * ni sandbox del proveedor. El adaptador real NO lo implementa.
 */
public interface SimuladorDePagoDePasarela {

    /** El aviso de pago de una orden ya creada, con su monto y un {@code idEvento} determinista. */
    WebhookFirmado emitirWebhookDePago(String referenciaProveedor);

    /**
     * El mismo aviso con {@code idEvento} y monto a eleccion, para ejercitar los bordes del contrato: el MISMO evento
     * dos veces, un monto distinto, una referencia que la pasarela nunca emitio.
     */
    WebhookFirmado emitirWebhookDePago(String referenciaProveedor, String idEvento, Dinero montoInformado);

    /** Lo que la pasarela mandaria al webhook: cuerpo crudo, firma ({@code X-Firma}) y {@code X-Timestamp}. */
    record WebhookFirmado(String proveedorCodigo, String cuerpoCrudo, String firma, String timestamp) {}
}

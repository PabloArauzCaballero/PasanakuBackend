package bo.aportaya.identidad.dominio;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;

/**
 * Por donde recibe la verificacion quien se registra.
 *
 * <p><b>No son dos formas de lo mismo.</b> Un codigo por SMS verifica que el TELEFONO
 * es suyo; un enlace por correo verifica que el CORREO es suyo. El catalogo de tokens
 * ya los distingue —`VERIFICACION_TELEFONO` admite SMS, WhatsApp y llamada;
 * `VERIFICACION_CORREO` admite correo— con su propia vigencia y su propio largo de
 * codigo. Mandar el codigo del telefono al correo no verificaria el telefono: seria
 * decir que si sin haber comprobado nada.
 *
 * <p>Lo que elige la persona es <b>cual de sus dos contactos confirma primero</b>.
 */
public enum CanalDeVerificacion {
    SMS,
    CORREO;

    /** El proposito del catalogo de tokens que le corresponde. */
    public String proposito() {
        return this == CORREO ? "VERIFICACION_CORREO" : "VERIFICACION_TELEFONO";
    }

    /** El canal de entrega, con el nombre que usa `token_verificacion.canal_entrega`. */
    public String canalDeEntrega() {
        return name();
    }

    /**
     * Elegir el correo sin dar un correo no lleva a ningun lado: el token no tendria
     * destino y la persona quedaria esperando algo que no se envio nunca.
     */
    public void exigirDestino(String correo) {
        if (this == CORREO && (correo == null || correo.isBlank())) {
            throw new ErrorDeDominio("Para verificar por correo hace falta tu correo");
        }
    }
}

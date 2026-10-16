package bo.aportaya.identidad.dominio.puertos;

import java.time.Duration;

/** Canal que entrega el codigo de confirmacion del correo durante el alta. */
public interface CorreoDeVerificacion {

    void enviarCodigo(String destino, String codigo, Duration vigencia);
}

package bo.aportaya.identidad.dominio.puertos;

import java.time.Duration;

/** Canal que entrega por correo un codigo de un solo uso. */
public interface CorreoDeVerificacion {

    /** El codigo que confirma el correo durante el alta. */
    void enviarCodigo(String destino, String codigo, Duration vigencia);

    /** El codigo del segundo factor al iniciar sesion. Mismo canal, otro texto. */
    void enviarCodigoDeAcceso(String destino, String codigo, Duration vigencia);
}

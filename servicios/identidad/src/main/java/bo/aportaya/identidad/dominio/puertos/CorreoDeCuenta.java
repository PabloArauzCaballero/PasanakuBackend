package bo.aportaya.identidad.dominio.puertos;

import bo.aportaya.identidad.dominio.AlcanceDeCuenta;
import java.time.Duration;

/**
 * Canal que entrega al correo institucional del operador el enlace de un solo uso (P-1, P-4).
 *
 * <p>El enlace lleva el token en el fragmento ({@code #token=...}): el navegador no lo manda al
 * servidor y no queda en los registros de acceso (regla 90.2.2). Las implementaciones no lo
 * escriben en ningun log.
 */
public interface CorreoDeCuenta {

    void enviarInvitacion(String destino, String enlace, AlcanceDeCuenta alcance, Duration vigencia);
}

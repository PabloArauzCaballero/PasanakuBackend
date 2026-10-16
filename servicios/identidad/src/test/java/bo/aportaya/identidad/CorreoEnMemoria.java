package bo.aportaya.identidad;

import bo.aportaya.identidad.dominio.puertos.CorreoDeVerificacion;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DOBLE de {@link CorreoDeVerificacion}: guarda lo que se habria enviado en vez de
 * salir a Gmail. Sirve para probar que el codigo llega y cual es; que Gmail lo entregue
 * lo prueba {@code GmailApiCorreoDeVerificacionTest} y, de verdad, el envio en TEST.
 */
final class CorreoEnMemoria implements CorreoDeVerificacion {

    record Enviado(String destino, String codigo, Duration vigencia, boolean deAcceso) {}

    final List<Enviado> enviados = new ArrayList<>();
    private RuntimeException fallaAlEnviar;

    /** El proximo envio falla como fallaria el proveedor. */
    CorreoEnMemoria fallar(RuntimeException falla) {
        this.fallaAlEnviar = falla;
        return this;
    }

    Enviado ultimo() {
        return enviados.get(enviados.size() - 1);
    }

    @Override
    public void enviarCodigo(String destino, String codigo, Duration vigencia) {
        registrar(new Enviado(destino, codigo, vigencia, false));
    }

    @Override
    public void enviarCodigoDeAcceso(String destino, String codigo, Duration vigencia) {
        registrar(new Enviado(destino, codigo, vigencia, true));
    }

    private void registrar(Enviado enviado) {
        if (fallaAlEnviar != null) {
            throw fallaAlEnviar;
        }
        enviados.add(enviado);
    }
}

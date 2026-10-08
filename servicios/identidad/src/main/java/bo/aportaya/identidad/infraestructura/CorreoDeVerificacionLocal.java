package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.dominio.puertos.CorreoDeVerificacion;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adaptador sin salida para desarrollo; confirma el envio sin filtrar el codigo al log. */
@Component
@ConditionalOnProperty(name = "aportaya.correo.proveedor", havingValue = "local", matchIfMissing = true)
public class CorreoDeVerificacionLocal implements CorreoDeVerificacion {

    private static final Logger BITACORA = LoggerFactory.getLogger(CorreoDeVerificacionLocal.class);

    @Override
    public void enviarCodigo(String destino, String codigo, Duration vigencia) {
        BITACORA.info("correo de verificacion preparado para un destino local ({} digitos)", codigo.length());
    }
}

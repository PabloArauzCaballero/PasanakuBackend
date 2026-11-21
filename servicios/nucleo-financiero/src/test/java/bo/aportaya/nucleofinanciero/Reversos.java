package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.SalidaReverso;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.UUID;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * CU-14 en sus dos actos (P-13): una persona pide y otra autoriza, cada una en su propia transaccion
 * y con su propio contexto. Las pruebas que solo necesitan "un reverso hecho" pasan por aca.
 */
final class Reversos {

    private Reversos() {}

    static SalidaReverso pedirYAutorizar(
            TransactionTemplate transaccion,
            CU14ReversarTransaccion cu14,
            EntradaReverso entrada,
            ContextoSesion solicita,
            UUID autoriza) {
        var solicitud = transaccion.execute(x -> cu14.solicitar(entrada, solicita));
        return transaccion.execute(x -> cu14.autorizar(solicitud.solicitudId(), otraPersona(solicita, autoriza)));
    }

    static ContextoSesion otraPersona(ContextoSesion de, UUID usuario) {
        return new ContextoSesion(usuario, de.rol(), de.traza(), de.dispositivo());
    }
}

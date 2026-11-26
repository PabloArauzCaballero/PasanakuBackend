package bo.aportaya.identidad.aplicacion;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Lo que el alta del operador necesita de la cuenta: emitir su invitacion de un solo uso (P-1), dentro de la
 * transaccion del alta. Es un puerto para que el alta no conozca el correo ni el token.
 */
public interface InvitadorDeOperador {

    void invitar(DSLContext dsl, UUID operadorId, ContextoSesion quienLoDioDeAlta, String motivo);
}

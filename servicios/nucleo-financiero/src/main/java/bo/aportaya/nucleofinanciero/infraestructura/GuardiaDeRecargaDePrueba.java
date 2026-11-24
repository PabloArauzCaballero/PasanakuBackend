package bo.aportaya.nucleofinanciero.infraestructura;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * El QR de PRUEBA y «Simular pago» no entran a produccion (FLUJO-QR-001).
 *
 * <p>{@link RecargasProveedorLocal} solo existe con los perfiles {@code local} y {@code test}, asi que en
 * produccion con {@code aportaya.recarga.qr.modo=prueba} el arranque ya fallaba, pero por un bean que falta y
 * sin decir por que. Esta guardia lo dice: «Simular pago» acredita saldo sin dinero real, y una configuracion que
 * lo pida en produccion es un error de despliegue, no algo que se degrade en silencio.
 */
@Component
@Profile("production")
@ConditionalOnProperty(name = "aportaya.recarga.qr.modo", havingValue = "prueba")
class GuardiaDeRecargaDePrueba {

    GuardiaDeRecargaDePrueba() {
        throw new IllegalStateException(
                "aportaya.recarga.qr.modo=prueba en produccion: el QR de PRUEBA acredita saldo sin dinero real. "
                        + "Usá el proveedor real (modo=proveedor) o un perfil que no sea production.");
    }
}

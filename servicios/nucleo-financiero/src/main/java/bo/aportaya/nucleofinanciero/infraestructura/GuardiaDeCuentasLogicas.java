package bo.aportaya.nucleofinanciero.infraestructura;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Un mapeo de cuentas logicas SINTETICO no entra a produccion.
 *
 * <p>El entorno de pruebas trae un mapeo provisional de {@code CAJA_EMPRESA}, {@code RESERVA_RESPALDO} y
 * {@code POZO_GRUPO} al arbol 9 del plan de cuentas (seeders/minimos/22-cuentas-de-sistema.json): sirve para
 * ejercitar el consumo de asientos, no para asentar dinero real. Con el perfil {@code production} y el consumo
 * encendido, el proceso NO ARRANCA hasta que contabilidad entregue el mapeo definitivo (ambiguedad A5) y alguien
 * declare {@code CUENTAS_LOGICAS_APTO_PRODUCCION=true}. Es la contracara de {@code apto_produccion=false} de las
 * semillas: la marca no vale si nada la mira al arrancar.
 */
@Component
@Profile("production")
@ConditionalOnProperty(name = "aportaya.contabilidad.consumo-de-asientos.habilitado", havingValue = "true")
class GuardiaDeCuentasLogicas {

    GuardiaDeCuentasLogicas(
            @Value("${aportaya.contabilidad.cuentas-logicas.apto-produccion:false}") boolean aptoProduccion) {
        if (!aptoProduccion) {
            throw new IllegalStateException(
                    "El consumo de asientos esta encendido en produccion con un mapeo de cuentas "
                            + "logicas que no esta declarado apto para produccion (CUENTAS_LOGICAS_APTO_PRODUCCION). "
                            + "El mapeo de TEST es sintetico: apagá CONSUMO_DE_ASIENTOS_HABILITADO o cargá el mapeo "
                            + "definitivo de contabilidad.");
        }
    }
}

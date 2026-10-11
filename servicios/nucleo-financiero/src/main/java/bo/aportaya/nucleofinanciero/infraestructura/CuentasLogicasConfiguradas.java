package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.puertos.CuentasLogicas;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * El mapeo cuenta logica -> codigo del plan, leido de {@code aportaya.contabilidad.cuentas-logicas.<LOGICA>.<MONEDA>}.
 *
 * <p>No hay valores por omision en ningun perfil: el plan de cuentas minimo no tiene reserva de
 * respaldo ni caja de empresa, y asignarles un codigo existente "para que ande" seria afirmar como
 * hecho contable lo que es una pregunta abierta. Hasta que contabilidad lo defina, los eventos con
 * esas cuentas no se registran.
 */
@Component
class CuentasLogicasConfiguradas implements CuentasLogicas {

    private static final String PREFIJO = "aportaya.contabilidad.cuentas-logicas.";

    private final Environment entorno;

    CuentasLogicasConfiguradas(Environment entorno) {
        this.entorno = entorno;
    }

    @Override
    public Optional<String> codigoDe(String cuentaLogica, Moneda moneda) {
        return Optional.ofNullable(entorno.getProperty(PREFIJO + cuentaLogica + "." + moneda.name()))
                .filter(c -> !c.isBlank());
    }
}

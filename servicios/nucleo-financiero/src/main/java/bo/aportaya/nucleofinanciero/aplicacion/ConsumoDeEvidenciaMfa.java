package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.infraestructura.EvidenciaMfaConsumidaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la transaccion propia que {@link EvidenciaMfaConsumidaRepositorio} necesita.
 *
 * <p>Servicio de aplicacion: vive en {@code aplicacion/} respetando el Invariante 2
 * (las transacciones residen en la capa de aplicacion).
 */
@Service
public class ConsumoDeEvidenciaMfa {

    private final Datos datos;
    private final EvidenciaMfaConsumidaRepositorio consumos;
    private final Reloj reloj;

    public ConsumoDeEvidenciaMfa(Datos datos, EvidenciaMfaConsumidaRepositorio consumos, Reloj reloj) {
        this.datos = datos;
        this.consumos = consumos;
        this.reloj = reloj;
    }

    @Transactional
    public boolean consumir(UUID jti, UUID usuarioId, String proposito) {
        ContextoSesion sistema = ContextoSesion.deSistema(usuarioId, new Traza(jti.toString()));
        return datos.conContexto(
                sistema,
                dsl -> consumos.consumir(
                        dsl, jti, usuarioId, proposito, reloj.ahora().atOffset(ZoneOffset.UTC)));
    }
}

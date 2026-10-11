package bo.aportaya.nucleofinanciero;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAsientos;
import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAsientos.EventoRecibido;
import bo.aportaya.nucleofinanciero.infraestructura.AsientoRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaContableRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;

/** El armado comun de las pruebas de consumo de asientos: el mapeo de cuentas logicas y el evento tal como lo emite garantia. */
abstract class BaseDeEventosDeAsientos extends BaseDeCU24 {

    protected static final ObjectMapper JSON = new ObjectMapper();
    protected static final Map<String, String> MAPEO = new HashMap<>();
    protected static ConsumoDeAsientos consumo;

    @BeforeAll
    static void armarConsumo() {
        for (String moneda : List.of("BOB", "USD")) {
            String sufijo = UUID.randomUUID().toString().substring(0, 6);
            for (String logica : List.of("CAJA_EMPRESA", "RESERVA_RESPALDO", "POZO_GRUPO")) {
                String codigo = "L." + logica.substring(0, 3) + "." + moneda + "." + sufijo;
                fixtura.cuentaDeMovimiento(codigo, "ACTIVO", "DEUDORA", moneda);
                MAPEO.put(logica + "." + moneda, codigo);
            }
        }
        consumo = new ConsumoDeAsientos(
                new Datos(dsl),
                registrar,
                new AsientoRepositorio(),
                new CuentaContableRepositorio(),
                (logica, moneda) -> Optional.ofNullable(MAPEO.get(logica + "." + moneda.name())));
    }

    /** El evento tal como lo emite garantia: {@code asiento = {origenTipo, origenId, partidas[{cuenta,debe,haber,moneda}]}}. */
    protected JsonNode carga(
            String origen, UUID origenId, String aDebitar, String aAcreditar, String monto, String moneda) {
        var asiento = JSON.createObjectNode().put("origenTipo", origen).put("origenId", origenId.toString());
        var partidas = asiento.putArray("partidas");
        partidas.addObject()
                .put("cuenta", aDebitar)
                .put("debe", monto)
                .put("haber", "0.00")
                .put("moneda", moneda);
        partidas.addObject()
                .put("cuenta", aAcreditar)
                .put("debe", "0.00")
                .put("haber", monto)
                .put("moneda", moneda);
        // Sin grupoId: asiento_contable.grupo_id es clave foranea a un grupo real y estas pruebas no crean uno.
        return JSON.createObjectNode().set("asiento", asiento);
    }

    protected EventoRecibido evento(UUID id, String tipo, JsonNode carga) {
        return new EventoRecibido(id, tipo, UUID.randomUUID(), carga);
    }

    protected ConsumoDeAsientos.Salida procesar(EventoRecibido e) {
        return transaccion.execute(t -> consumo.procesar(e));
    }

    protected int asientos(UUID origenId) {
        return ((Number) dsl.fetchOne(
                                "SELECT count(*) FROM nucleo_financiero.asiento_contable WHERE origen_id = ?", origenId)
                        .get(0))
                .intValue();
    }

    protected int marcas(UUID eventoId) {
        return ((Number) dsl.fetchOne(
                                "SELECT count(*) FROM nucleo_financiero.evento_consumido WHERE id_evento = ?", eventoId)
                        .get(0))
                .intValue();
    }

    protected static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }
}

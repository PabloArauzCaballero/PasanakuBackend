package bo.aportaya.inversiones.web;

import static bo.aportaya.inversiones.web.MapeoDeInversiones.bob;

import bo.aportaya.inversiones.aplicacion.OperacionesProgramables;
import bo.aportaya.inversiones.web.generado.modelo.Corrida;
import bo.aportaya.inversiones.web.generado.modelo.ListaDeCorridas;
import bo.aportaya.inversiones.web.generado.modelo.SalidaDevengo;
import bo.aportaya.inversiones.web.generado.modelo.SalidaReintento;
import bo.aportaya.inversiones.web.generado.modelo.SalidaSincronizacion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Las operaciones de plataforma de {@code /inversiones} (sincronizar, devengar, reintentar,
 * ver las corridas). La clave de idempotencia que el contrato exige llega hasta
 * {@link OperacionesProgramables}, que la reserva: aca solo se traduce la forma.
 */
@Component
class OperacionesDeLaInversion {

    private final OperacionesProgramables operaciones;

    OperacionesDeLaInversion(OperacionesProgramables operaciones) {
        this.operaciones = operaciones;
    }

    SalidaSincronizacion catalogo(UUID clave, ContextoSesion ctx) {
        return sincronizacion(operaciones.sincronizarCatalogo(clave.toString(), OperacionesProgramables.API, ctx));
    }

    SalidaSincronizacion valores(UUID clave, ContextoSesion ctx) {
        return sincronizacion(operaciones.sincronizarValores(clave.toString(), OperacionesProgramables.API, ctx));
    }

    SalidaDevengo devengo(UUID clave, LocalDate fecha, ContextoSesion ctx) {
        Map<String, String> r = operaciones.devengar(fecha, clave.toString(), OperacionesProgramables.API, ctx);
        var s = new SalidaDevengo();
        s.setFecha(LocalDate.parse(r.get("fecha")));
        s.setDevengadas(Integer.parseInt(r.get("devengadas")));
        s.setYaDevengadas(Integer.parseInt(r.get("yaDevengadas")));
        s.setTotal(bob(new BigDecimal(r.get("total"))));
        return s;
    }

    SalidaReintento reintento(UUID clave, Integer limite, ContextoSesion ctx) {
        int tope = limite == null ? 0 : limite; // cero = el de una vuelta del reloj
        Map<String, String> r =
                operaciones.reintentarInstrucciones(tope, true, clave.toString(), OperacionesProgramables.API, ctx);
        var s = new SalidaReintento();
        s.setAplicadas(Integer.parseInt(r.get("aplicadas")));
        s.setRechazadas(Integer.parseInt(r.get("rechazadas")));
        s.setSiguenPendientes(Integer.parseInt(r.get("siguenPendientes")));
        s.setEsperando(Integer.parseInt(r.get("esperando")));
        s.setAtascadas(Integer.parseInt(r.get("atascadas")));
        s.setCortada(Boolean.parseBoolean(r.get("cortada")));
        return s;
    }

    ListaDeCorridas corridas(String operacion, Integer limite, ContextoSesion ctx) {
        var lista = new ListaDeCorridas();
        operaciones.corridas(operacion, limite == null ? 50 : limite, ctx).forEach(c -> {
            var s = new Corrida();
            s.setCorridaId(c.id());
            s.setOperacion(Corrida.OperacionEnum.fromValue(c.operacion()));
            s.setClave(c.clave());
            s.setOrigen(Corrida.OrigenEnum.fromValue(c.origen()));
            s.setEstado(Corrida.EstadoEnum.fromValue(c.estado()));
            s.setResultado(c.resultado());
            s.setError(c.error());
            s.setIniciadaEn(c.iniciadaEn());
            s.setTerminadaEn(c.terminadaEn() == null ? null : c.terminadaEn().withOffsetSameInstant(ZoneOffset.UTC));
            lista.addCorridasItem(s);
        });
        return lista;
    }

    private static SalidaSincronizacion sincronizacion(Map<String, String> r) {
        var s = new SalidaSincronizacion();
        s.setNuevos(Integer.parseInt(r.get("nuevos")));
        s.setProductos(Integer.parseInt(r.get("productos")));
        return s;
    }
}

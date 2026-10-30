package bo.aportaya.transparencia.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.transparencia.dominio.ArenasDeReputacion;
import bo.aportaya.transparencia.infraestructura.ArenaRepositorio;
import bo.aportaya.transparencia.infraestructura.ModeloRepositorio;
import bo.aportaya.transparencia.infraestructura.ReputacionRepositorio;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El puntaje de reputacion de alguien, para quien tenga que decidir con el.
 *
 * <p>Lo preguntan CU-64 y CU-68 de {@code grupos}: aceptar a alguien en un grupo o
 * dejarle tomar un cupo depende de su historial, y ese historial es de este servicio.
 *
 * <p><b>Quien todavia no tiene puntaje no tiene cero.</b> Cero seria el peor puntaje
 * posible, y quien recien llega no hizo nada malo: se devuelve {@code SIN_HISTORIAL} y
 * quien pregunta decide que hacer con eso. Confundir «no se sabe» con «malo» le cierra
 * la puerta a todo el que empieza.
 *
 * <p><b>La arena (2026-10-30) sale del puntaje, no se guarda.</b> Con historial, es la arena
 * del puntaje dentro de las arenas del modelo con que se calculo. Sin historial, es la arena
 * del puntaje BASE del modelo vigente (Semilla con el v1), nunca la del cero, por la misma
 * razon de arriba. El campo {@code puntaje} sigue diciendo 0 en ese caso: su contrato ya lo
 * consumen otros y no cambia.
 */
@Service
public class ConsultarPuntaje {

    private final Datos datos;
    private final ReputacionRepositorio reputaciones;
    private final ModeloRepositorio modelos;
    private final ArenaRepositorio arenas;
    private final Reloj reloj;

    public ConsultarPuntaje(
            Datos datos,
            ReputacionRepositorio reputaciones,
            ModeloRepositorio modelos,
            ArenaRepositorio arenas,
            Reloj reloj) {
        this.datos = datos;
        this.reputaciones = reputaciones;
        this.modelos = modelos;
        this.arenas = arenas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Puntaje ejecutar(UUID usuarioId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> puntajeVisible(dsl, usuarioId)
                .map(p -> new Puntaje(
                        true,
                        p.puntaje(),
                        p.nivelConfianza(),
                        arenaConHistorial(dsl, p.modeloVersion(), p.puntaje()).orElse(null)))
                .orElseGet(() -> new Puntaje(
                        false,
                        BigDecimal.ZERO,
                        "SIN_HISTORIAL",
                        arenaSinHistorial(dsl).orElse(null))));
    }

    /**
     * El puntaje vigente, leido con rol de sistema SOLO para esta lectura.
     *
     * <p>La politica de fila de {@code puntaje_reputacion} deniega por omision: con el rol del
     * servicio y una sesion PARTICIPANTE no la ve nadie, ni el titular. Sin esto, todo el que tenia
     * historial salia {@code SIN_HISTORIAL} con la arena del puntaje base, y alguien de Campamento
     * pasaba la arena minima de un grupo de Semilla. Quien pregunta casi siempre pregunta por OTRA
     * persona (grupos decide un ingreso), asi que hacer visible la fila al titular no alcanzaba.
     * Quien puede llegar hasta aca lo decide la guardia de la ruta ({@code @Permiso}); lo que sale
     * es la proyeccion del contrato (puntaje, nivel y version del modelo), no la fila.
     */
    private Optional<ReputacionRepositorio.Puntaje> puntajeVisible(DSLContext dsl, UUID usuarioId) {
        return Datos.comoSistema(dsl, () -> reputaciones.puntajeDe(dsl, usuarioId));
    }

    private Optional<ArenasDeReputacion.Ubicacion> arenaConHistorial(
            DSLContext dsl, String modeloVersion, BigDecimal puntaje) {
        return arenas.modeloDeLaVersion(dsl, modeloVersion)
                .flatMap(modeloId -> ArenasDeReputacion.ubicar(arenas.arenasDelModelo(dsl, modeloId), puntaje));
    }

    private Optional<ArenasDeReputacion.Ubicacion> arenaSinHistorial(DSLContext dsl) {
        return modelos.modeloVigente(dsl, reloj.ahora().atOffset(ZoneOffset.UTC))
                .flatMap(modelo ->
                        ArenasDeReputacion.ubicar(arenas.arenasDelModelo(dsl, modelo.id()), modelo.puntajeBase()));
    }

    /**
     * @param arena donde cae el puntaje; nula solo si no hay catalogo de arenas para el modelo
     *     (o el puntaje cae fuera de el), y entonces la respuesta la omite
     */
    public record Puntaje(
            boolean tieneHistorial, BigDecimal puntaje, String nivelDeConfianza, ArenasDeReputacion.Ubicacion arena) {

        /** Sin arena: la forma de antes de las arenas, que siguen usando los dobles de las pruebas web. */
        public Puntaje(boolean tieneHistorial, BigDecimal puntaje, String nivelDeConfianza) {
            this(tieneHistorial, puntaje, nivelDeConfianza, null);
        }
    }
}

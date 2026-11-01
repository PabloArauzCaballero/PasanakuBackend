package bo.aportaya.transparencia.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.transparencia.dominio.EtapasDeReputacion;
import bo.aportaya.transparencia.infraestructura.EtapaRepositorio;
import bo.aportaya.transparencia.infraestructura.ModeloRepositorio;
import bo.aportaya.transparencia.infraestructura.ReputacionRepositorio;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El puntaje de reputacion EXACTO de quien pregunta, con su etapa.
 *
 * <p><b>Solo el propio (decision del dueño 2026-10-30, regla 90.2.8).</b> Si {@code usuarioId}
 * no es el de la sesion, se niega antes de leer nada, aunque quien pregunta comparta un grupo
 * con esa persona. La etapa de otra persona la lee {@code grupos} con su identidad de servicio
 * ({@link ConsultarEtapasDePersonas}), y nunca lleva el puntaje.
 *
 * <p><b>Quien todavia no tiene puntaje no tiene cero.</b> Cero seria el peor puntaje
 * posible, y quien recien llega no hizo nada malo: se devuelve {@code SIN_HISTORIAL}.
 * Confundir «no se sabe» con «malo» le cierra la puerta a todo el que empieza.
 *
 * <p><b>La etapa sale del puntaje, no se guarda.</b> Con historial, es la etapa del puntaje
 * dentro de las etapas del modelo con que se calculo. Sin historial, es la etapa del puntaje
 * BASE del modelo vigente (Etapa 1 · Inicial con el v1), nunca la del cero, por la misma
 * razon de arriba. El campo {@code puntaje} sigue diciendo 0 en ese caso: su contrato ya lo
 * consumen otros y no cambia.
 */
@Service
public class ConsultarPuntaje {

    private final Datos datos;
    private final ReputacionRepositorio reputaciones;
    private final ModeloRepositorio modelos;
    private final EtapaRepositorio etapas;
    private final Reloj reloj;

    public ConsultarPuntaje(
            Datos datos,
            ReputacionRepositorio reputaciones,
            ModeloRepositorio modelos,
            EtapaRepositorio etapas,
            Reloj reloj) {
        this.datos = datos;
        this.reputaciones = reputaciones;
        this.modelos = modelos;
        this.etapas = etapas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Puntaje ejecutar(UUID usuarioId, ContextoSesion ctx) {
        if (!usuarioId.equals(ctx.usuarioId())) {
            throw new AccessDeniedException("el puntaje exacto solo lo ve su dueño");
        }
        return datos.conContexto(ctx, dsl -> puntajeVisible(dsl, usuarioId)
                .map(p -> new Puntaje(
                        true,
                        p.puntaje(),
                        p.nivelConfianza(),
                        etapaConHistorial(dsl, etapas, p.modeloVersion(), p.puntaje())
                                .orElse(null)))
                .orElseGet(() -> new Puntaje(
                        false,
                        BigDecimal.ZERO,
                        "SIN_HISTORIAL",
                        etapaSinHistorial(dsl, etapas, modelos, reloj).orElse(null))));
    }

    /**
     * El puntaje vigente, leido con rol de sistema SOLO para esta lectura.
     *
     * <p>La politica de fila de {@code puntaje_reputacion} deniega por omision: con el rol del
     * servicio y una sesion PARTICIPANTE no la ve nadie, ni el titular. Sin esto, todo el que tenia
     * historial salia {@code SIN_HISTORIAL} con la etapa del puntaje base. Quien puede llegar hasta
     * aca ya lo decidieron la guardia de la ruta y el chequeo de titular de {@link #ejecutar}; lo que
     * sale es la proyeccion del contrato (puntaje, nivel y version del modelo), no la fila.
     */
    static Optional<ReputacionRepositorio.Puntaje> puntajeVisible(
            DSLContext dsl, ReputacionRepositorio reputaciones, UUID usuarioId) {
        return Datos.comoSistema(dsl, () -> reputaciones.puntajeDe(dsl, usuarioId));
    }

    private Optional<ReputacionRepositorio.Puntaje> puntajeVisible(DSLContext dsl, UUID usuarioId) {
        return puntajeVisible(dsl, reputaciones, usuarioId);
    }

    /** La etapa del puntaje dentro de las etapas del modelo con que se calculo. */
    static Optional<EtapasDeReputacion.Ubicacion> etapaConHistorial(
            DSLContext dsl, EtapaRepositorio etapas, String modeloVersion, BigDecimal puntaje) {
        return etapas.modeloDeLaVersion(dsl, modeloVersion)
                .flatMap(modeloId -> EtapasDeReputacion.ubicar(etapas.etapasDelModelo(dsl, modeloId), puntaje));
    }

    /** La etapa del puntaje BASE del modelo vigente: la de quien todavia no tiene historial. */
    static Optional<EtapasDeReputacion.Ubicacion> etapaSinHistorial(
            DSLContext dsl, EtapaRepositorio etapas, ModeloRepositorio modelos, Reloj reloj) {
        return modelos.modeloVigente(dsl, reloj.ahora().atOffset(ZoneOffset.UTC))
                .flatMap(modelo ->
                        EtapasDeReputacion.ubicar(etapas.etapasDelModelo(dsl, modelo.id()), modelo.puntajeBase()));
    }

    /**
     * @param etapa donde cae el puntaje; nula solo si no hay catalogo de etapas para el modelo
     *     (o el puntaje cae fuera de el), y entonces la respuesta la omite
     */
    public record Puntaje(
            boolean tieneHistorial, BigDecimal puntaje, String nivelDeConfianza, EtapasDeReputacion.Ubicacion etapa) {

        /** Sin etapa: la forma de antes del catalogo de etapas, que siguen usando los dobles de las pruebas web. */
        public Puntaje(boolean tieneHistorial, BigDecimal puntaje, String nivelDeConfianza) {
            this(tieneHistorial, puntaje, nivelDeConfianza, null);
        }
    }
}

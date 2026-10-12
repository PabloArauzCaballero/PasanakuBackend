package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.CriterioDeEmparejamiento;
import bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-68 · Postular a un grupo y ser emparejado.
 *
 * <p>El postulante ve **por que** se le propuso ese grupo —monto compatible, gente de
 * su zona, riesgo parecido— y nunca los datos de los demas. Un puntaje sin explicacion
 * es un numero que nadie puede discutir, y un emparejamiento que nadie puede discutir
 * es uno que se acepta por resignacion.
 *
 * <p>Lo que este servicio no sabe llega resuelto: la restriccion vigente vive en
 * `garantia`, el nivel de conocimiento del cliente en `cumplimiento` y la reputacion
 * en `transparencia`. `grupos` no lee esos esquemas (invariante 11); pone el criterio
 * y la decision.
 */
@Service
public class CU68Postular {

    private final Datos datos;
    private final EmparejamientoRepositorio emparejamientos;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU68Postular(Datos datos, EmparejamientoRepositorio emparejamientos, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.emparejamientos = emparejamientos;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Transactional
    public SalidaPostulacion postular(EntradaPostulacion entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            CriterioDeEmparejamiento criterio =
                    PrecondicionesDePostulacion.exigir(emparejamientos, dsl, entrada, ctx.usuarioId(), ahora);
            // El riesgo recomienda; la resolución de admisión pertenece al backoffice.
            var motivos = MotorDeRecomendacion.motivos(criterio, entrada);
            String recomendacion = MotorDeRecomendacion.recomendacion(motivos);

            BigDecimal puntaje = criterio.puntuar(
                    entrada.afinidadReputacion(),
                    entrada.afinidadMonto(),
                    entrada.afinidadGeografia(),
                    entrada.afinidadHistorial());

            UUID solicitud = emparejamientos.postular(
                    dsl,
                    entrada.grupoId(),
                    ctx.usuarioId(),
                    entrada.cuposSolicitados(),
                    entrada.mensaje(),
                    puntaje,
                    ahora);

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.ingreso_solicitado",
                            "solicitud_ingreso",
                            solicitud,
                            Map.ofEntries(
                                    Map.entry("grupoId", entrada.grupoId().toString()),
                                    Map.entry("criterio", criterio.toString()),
                                    Map.entry("versionMotor", criterio.id().toString()),
                                    Map.entry("motivos", List.copyOf(motivos)),
                                    Map.entry("puntaje", puntaje.toPlainString()),
                                    Map.entry("recomendacion", recomendacion),
                                    // Las entradas, para poder reproducir el puntaje con la version del motor.
                                    Map.entry("entradas", MotorDeRecomendacion.entradas(entrada)),
                                    Map.entry("decisionAutomatica", false)),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaPostulacion(solicitud, puntaje, List.copyOf(motivos));
        });
    }

    /**
     * Las mismas precondiciones de {@link #postular}, SIN escribir. Sirve para comprobar antes de un paso
     * irreversible en otro servicio (el canje de una invitacion consume el enlace en {@code identidad}).
     */
    @Transactional(readOnly = true)
    public void comprobar(EntradaPostulacion entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        datos.conContexto(
                ctx, dsl -> PrecondicionesDePostulacion.exigir(emparejamientos, dsl, entrada, ctx.usuarioId(), ahora));
    }

    /**
     * Responder a una propuesta de grupo. Alcanzadas las aceptaciones antes de
     * expirar, la propuesta se materializa en un grupo real.
     */
    @Transactional
    public EstadoPropuesta responder(
            UUID propuestaId,
            UUID postulacionId,
            boolean acepto,
            int aceptacionesNecesarias,
            java.util.Optional<UUID> grupoMaterializado,
            ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            var propuesta = emparejamientos
                    .propuesta(dsl, propuestaId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(68, 7), "Esa propuesta no existe."));

            // Expirada vuelve a la bolsa: nadie queda atado a un grupo que no se armo.
            if (ahora.isAfter(propuesta.expiraEn())) {
                throw new ErrorDeNegocio(CodigoError.de(68, 7), "Esa propuesta ya vencio.");
            }
            if (emparejamientos.responder(dsl, propuestaId, postulacionId, acepto, ahora) == 0) {
                throw new ErrorDeNegocio(CodigoError.de(68, 6), "Ya respondiste a esta propuesta.");
            }

            int aceptaciones = emparejamientos.contarAceptaciones(dsl, propuestaId);
            if (aceptaciones >= aceptacionesNecesarias && grupoMaterializado.isPresent()) {
                emparejamientos.materializar(dsl, propuestaId, grupoMaterializado.get(), aceptaciones);
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "grupos.propuesta_materializada",
                                "propuesta_grupo",
                                propuestaId,
                                Map.of("grupoId", grupoMaterializado.get().toString()),
                                UUID.fromString(ctx.traza().id())));
                return new EstadoPropuesta(aceptaciones, true);
            }

            emparejamientos.anotarAceptacion(dsl, propuestaId, aceptaciones);
            return new EstadoPropuesta(aceptaciones, false);
        });
    }

    public record EntradaPostulacion(
            UUID grupoId,
            short cuposSolicitados,
            String mensaje,
            boolean tieneRestriccionVigente,
            BigDecimal montoQueLevantaLaRestriccion,
            boolean kycSuficiente,
            int reputacion,
            int morososDelGrupo,
            BigDecimal afinidadReputacion,
            BigDecimal afinidadMonto,
            BigDecimal afinidadGeografia,
            BigDecimal afinidadHistorial,
            boolean reputacionSinDatos,
            boolean concentracionSinDatos,
            boolean reputacionExcluyente) {
        /** Con datos completos: el motor no tiene nada que marcar como SIN_DATOS. */
        public EntradaPostulacion(
                UUID grupoId,
                short cuposSolicitados,
                String mensaje,
                boolean tieneRestriccionVigente,
                BigDecimal montoQueLevantaLaRestriccion,
                boolean kycSuficiente,
                int reputacion,
                int morososDelGrupo,
                BigDecimal afinidadReputacion,
                BigDecimal afinidadMonto,
                BigDecimal afinidadGeografia,
                BigDecimal afinidadHistorial) {
            this(
                    grupoId,
                    cuposSolicitados,
                    mensaje,
                    tieneRestriccionVigente,
                    montoQueLevantaLaRestriccion,
                    kycSuficiente,
                    reputacion,
                    morososDelGrupo,
                    afinidadReputacion,
                    afinidadMonto,
                    afinidadGeografia,
                    afinidadHistorial,
                    false,
                    false,
                    false);
        }

        /**
         * Recomendacion con SIN_DATOS: nada se rechaza por reputacion, lo decide una persona (la ruta HTTP usa
         * esta forma).
         */
        public EntradaPostulacion(
                UUID grupoId,
                short cuposSolicitados,
                String mensaje,
                boolean tieneRestriccionVigente,
                BigDecimal montoQueLevantaLaRestriccion,
                boolean kycSuficiente,
                int reputacion,
                int morososDelGrupo,
                BigDecimal afinidadReputacion,
                BigDecimal afinidadMonto,
                BigDecimal afinidadGeografia,
                BigDecimal afinidadHistorial,
                boolean reputacionSinDatos,
                boolean concentracionSinDatos) {
            this(
                    grupoId,
                    cuposSolicitados,
                    mensaje,
                    tieneRestriccionVigente,
                    montoQueLevantaLaRestriccion,
                    kycSuficiente,
                    reputacion,
                    morososDelGrupo,
                    afinidadReputacion,
                    afinidadMonto,
                    afinidadGeografia,
                    afinidadHistorial,
                    reputacionSinDatos,
                    concentracionSinDatos,
                    false);
        }

        /**
         * Evaluacion directa de CU-68 2a: quien TIENE historial y no llega a la reputacion minima es rechazado
         * (AP-CU68-03); quien es nuevo ({@code sinHistorial}) no se excluye por serlo.
         */
        public EntradaPostulacion(
                UUID grupoId,
                short cuposSolicitados,
                String mensaje,
                boolean tieneRestriccionVigente,
                BigDecimal montoQueLevantaLaRestriccion,
                boolean kycSuficiente,
                int reputacion,
                int morososDelGrupo,
                BigDecimal afinidadReputacion,
                BigDecimal afinidadMonto,
                BigDecimal afinidadGeografia,
                BigDecimal afinidadHistorial,
                boolean sinHistorial) {
            this(
                    grupoId,
                    cuposSolicitados,
                    mensaje,
                    tieneRestriccionVigente,
                    montoQueLevantaLaRestriccion,
                    kycSuficiente,
                    reputacion,
                    morososDelGrupo,
                    afinidadReputacion,
                    afinidadMonto,
                    afinidadGeografia,
                    afinidadHistorial,
                    sinHistorial,
                    false,
                    true);
        }
    }

    public record SalidaPostulacion(UUID solicitudId, BigDecimal puntaje, List<String> motivos) {}

    public record EstadoPropuesta(int aceptaciones, boolean materializada) {}
}

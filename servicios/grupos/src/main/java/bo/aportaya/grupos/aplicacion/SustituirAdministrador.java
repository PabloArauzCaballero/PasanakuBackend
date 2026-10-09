package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.SustitucionDeAdministrador;
import bo.aportaya.grupos.infraestructura.SustitucionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sustitución del administrador de un grupo por decisión de backoffice (continuidad tras una revocación o
 * suspensión de su habilitación).
 *
 * <p>Cambia quién administra; NO toca las obligaciones: el administrador saliente sigue siendo participante con
 * sus cupos, sus aportes y sus deudas, y eso queda identificado en el registro. Retirarlo del grupo es otro acto
 * (CU-65) con su propio debido proceso. El registro es append-only y reintentar con la misma clave devuelve el
 * original. Que el entrante esté habilitado como organizador lo comprueba quien llama, fuera de la transacción.
 */
@Service
public class SustituirAdministrador {
    private final Datos datos;
    private final SustitucionRepositorio sustituciones;
    private final Outbox outbox;
    private final Reloj reloj;

    public SustituirAdministrador(Datos datos, SustitucionRepositorio sustituciones, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.sustituciones = sustituciones;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Transactional
    public SustitucionDeAdministrador ejecutar(Entrada entrada, ContextoSesion ctx) {
        if (!"BACKOFFICE".equals(ctx.rol()) && !"ADMIN_PLATAFORMA".equals(ctx.rol())) {
            throw error(8, "Solo backoffice sustituye al administrador de un grupo.");
        }
        if (entrada.clave() == null
                || entrada.grupoId() == null
                || entrada.nuevoAdministradorUsuarioId() == null
                || entrada.motivo() == null
                || entrada.motivo().isBlank()
                || entrada.motivo().length() > 1000) {
            throw error(8, "La sustitucion requiere clave, grupo, nuevo administrador y motivo.");
        }
        return datos.conContexto(ctx, dsl -> {
            sustituciones.bloquearGrupo(dsl, entrada.grupoId());
            var previa = sustituciones.porClave(dsl, entrada.clave());
            if (previa.isPresent()) {
                var p = previa.get();
                var entrante =
                        sustituciones.participanteActivo(dsl, entrada.grupoId(), entrada.nuevoAdministradorUsuarioId());
                // Reintento: igual grupo, actor, motivo y entrante. (El entrante ya es el administrador vigente.)
                boolean igual = p.grupoId().equals(entrada.grupoId())
                        && p.actorId().equals(ctx.usuarioId())
                        && p.motivo().equals(entrada.motivo())
                        && entrante.map(e -> e.id().equals(p.entranteParticipanteId()))
                                .orElse(false);
                if (!igual) throw error(9, "La clave de idempotencia pertenece a otra sustitucion.");
                return p;
            }
            var vigentes = sustituciones.administradoresVigentes(dsl, entrada.grupoId());
            if (vigentes.size() != 1) {
                throw error(10, "El grupo debe tener exactamente un administrador vigente para poder sustituirlo.");
            }
            var saliente = vigentes.get(0);
            var entrante = sustituciones
                    .participanteActivo(dsl, entrada.grupoId(), entrada.nuevoAdministradorUsuarioId())
                    .orElseThrow(() -> error(11, "El nuevo administrador debe ser participante activo del grupo."));
            if (entrante.id().equals(saliente.id())) {
                throw error(12, "Esa persona ya es la administradora del grupo.");
            }
            if (entrante.usuarioId().equals(ctx.usuarioId())) {
                throw error(13, "Quien decide la sustitucion no puede ser quien queda como administrador.");
            }
            String obligaciones = sustituciones.obligacionesDe(dsl, saliente.id());
            sustituciones.marcarAdministrador(dsl, saliente.id(), false);
            sustituciones.marcarAdministrador(dsl, entrante.id(), true);
            OffsetDateTime ahora = reloj.ahora().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
            var registro = new SustitucionDeAdministrador(
                    UUID.randomUUID(),
                    entrada.grupoId(),
                    saliente.id(),
                    entrante.id(),
                    entrada.clave(),
                    entrada.motivo(),
                    ctx.usuarioId(),
                    obligaciones,
                    ahora,
                    UUID.fromString(ctx.traza().id()));
            sustituciones.guardar(dsl, registro);
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "grupos.administrador_sustituido",
                            "grupo",
                            entrada.grupoId(),
                            Map.of(
                                    "sustitucionId", registro.id().toString(),
                                    "salienteParticipanteId", saliente.id().toString(),
                                    "entranteParticipanteId", entrante.id().toString()),
                            registro.correlacionId()));
            return registro;
        });
    }

    private static ErrorDeNegocio error(int numero, String mensaje) {
        return new ErrorDeNegocio(CodigoError.de(68, numero), mensaje);
    }

    public record Entrada(UUID grupoId, UUID nuevoAdministradorUsuarioId, UUID clave, String motivo) {}
}

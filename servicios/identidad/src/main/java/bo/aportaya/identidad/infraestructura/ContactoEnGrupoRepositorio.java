package bo.aportaya.identidad.infraestructura;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * El telefono de un integrante para el perfil del grupo (2026-10-21). Lo lee el proceso interno porque la politica de
 * fila no le deja a un participante ver la fila de otra persona: por eso solo se llama DESPUES de que grupos confirmo
 * que comparten el grupo ({@code ConsultarContactoEnGrupo}). Devuelve el telefono y nada mas.
 *
 * <p>Cada lectura deja rastro en {@code comun.registro_acceso_datos} (TELEFONO, LECTURA) en la misma transaccion: el
 * telefono es un dato sensible y su lectura se audita (regla 90.2.7). La justificacion lleva el grupo, nunca el dato.
 */
@Repository
public class ContactoEnGrupoRepositorio {

    /**
     * De donde salio el acceso. Sin cabecera confiable —la red interna no es perimetro de confianza— se registra la del
     * proceso, como hace {@code auditoria.ReporteRepositorio}.
     */
    private static final String IP_DEL_PROCESO = "127.0.0.1";

    private final Datos datos;
    private final Reloj reloj;

    public ContactoEnGrupoRepositorio(Datos datos, Reloj reloj) {
        this.datos = datos;
        this.reloj = reloj;
    }

    @Transactional
    public Optional<String> telefono(UUID grupoId, UUID usuarioId, ContextoSesion ctx) {
        ContextoSesion interno =
                ContextoSesion.deSistema(ctx.usuarioId(), new Traza(ctx.traza().id()));
        return datos.conContexto(interno, dsl -> {
            Optional<String> telefono = Optional.ofNullable(
                            dsl.fetchOne("SELECT telefono_e164 FROM identidad.usuario WHERE id = ?", usuarioId))
                    .map(fila -> fila.get(0, String.class));
            telefono.ifPresent(t -> dsl.insertInto(DSL.table(DSL.name("comun", "registro_acceso_datos")))
                    .columns(
                            DSL.field("usuario_consultor_id", UUID.class),
                            DSL.field("usuario_afectado_id", UUID.class),
                            DSL.field("tipo_dato", String.class),
                            DSL.field("operacion", String.class),
                            DSL.field("justificacion", String.class),
                            DSL.field("cantidad_registros", Integer.class),
                            DSL.field("ip_origen", Object.class),
                            DSL.field("fecha_hora", OffsetDateTime.class))
                    .values(
                            DSL.val(ctx.usuarioId()),
                            DSL.val(usuarioId),
                            DSL.val("TELEFONO"),
                            DSL.val("LECTURA"),
                            DSL.val("Perfil del integrante: comparten el grupo " + grupoId),
                            DSL.val(1),
                            DSL.field("?::inet", Object.class, IP_DEL_PROCESO),
                            DSL.val(reloj.ahora().atOffset(ZoneOffset.UTC)))
                    .execute());
            return telefono;
        });
    }
}

package bo.aportaya.notificaciones.infraestructura;

import static bo.aportaya.notificaciones.generado.Tables.AVISO_INTERNO;

import bo.aportaya.notificaciones.generado.tables.records.AvisoInternoRecord;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Persistencia de {@code notificaciones.aviso_interno}.
 *
 * <p>Avisos dirigidos a operadores internos o a roles funcionales en el backoffice.
 */
@Component
public class AvisoInternoRepositorio {

    private Condition condicionDestinatario(UUID usuarioId, Collection<String> roles) {
        Condition condUsuario = (usuarioId != null) ? AVISO_INTERNO.USUARIO_ID.eq(usuarioId) : DSL.falseCondition();
        Condition condRoles = (roles != null && !roles.isEmpty()) ? AVISO_INTERNO.ROL_DESTINATARIO.in(roles) : DSL.falseCondition();
        return condUsuario.or(condRoles);
    }

    public List<AvisoInternoRecord> listarPara(
            DSLContext dsl, UUID usuarioId, Collection<String> roles, boolean soloNoLeidos, int limite) {
        Condition filtro = condicionDestinatario(usuarioId, roles);
        if (soloNoLeidos) {
            filtro = filtro.and(AVISO_INTERNO.LEIDO_EN.isNull());
        }
        return dsl.selectFrom(AVISO_INTERNO)
                .where(filtro)
                .orderBy(AVISO_INTERNO.CREADO_EN.desc())
                .limit(limite)
                .fetch();
    }

    public int contarNoLeidos(DSLContext dsl, UUID usuarioId, Collection<String> roles) {
        Condition filtro = condicionDestinatario(usuarioId, roles).and(AVISO_INTERNO.LEIDO_EN.isNull());
        return dsl.fetchCount(dsl.selectFrom(AVISO_INTERNO).where(filtro));
    }

    public Optional<AvisoInternoRecord> buscarPorId(DSLContext dsl, UUID id) {
        return Optional.ofNullable(
                dsl.selectFrom(AVISO_INTERNO)
                        .where(AVISO_INTERNO.ID.eq(id))
                        .fetchOne());
    }

    public Optional<AvisoInternoRecord> marcarLeido(DSLContext dsl, UUID id, OffsetDateTime ahora) {
        AvisoInternoRecord record = dsl.selectFrom(AVISO_INTERNO)
                .where(AVISO_INTERNO.ID.eq(id))
                .fetchOne();
        if (record == null) {
            return Optional.empty();
        }
        if (record.getLeidoEn() == null) {
            record.setLeidoEn(ahora);
            record.store();
        }
        return Optional.of(record);
    }

    public int marcarTodosLeidos(DSLContext dsl, UUID usuarioId, Collection<String> roles, OffsetDateTime ahora) {
        Condition filtro = condicionDestinatario(usuarioId, roles).and(AVISO_INTERNO.LEIDO_EN.isNull());
        return dsl.update(AVISO_INTERNO)
                .set(AVISO_INTERNO.LEIDO_EN, ahora)
                .where(filtro)
                .execute();
    }

    public AvisoInternoRecord crear(
            DSLContext dsl,
            UUID usuarioId,
            String rolDestinatario,
            String titulo,
            String mensaje,
            String tipo,
            String enlace,
            OffsetDateTime ahora) {
        AvisoInternoRecord record = dsl.newRecord(AVISO_INTERNO);
        record.setId(UUID.randomUUID());
        record.setUsuarioId(usuarioId);
        record.setRolDestinatario(rolDestinatario);
        record.setTitulo(titulo);
        record.setMensaje(mensaje);
        record.setTipo(tipo);
        record.setEnlace(enlace);
        record.setCreadoEn(ahora != null ? ahora : OffsetDateTime.now());
        record.store();
        return record;
    }
}

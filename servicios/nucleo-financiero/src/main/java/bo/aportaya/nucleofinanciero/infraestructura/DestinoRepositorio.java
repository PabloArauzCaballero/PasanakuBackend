package bo.aportaya.nucleofinanciero.infraestructura;

import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * De quien es la billetera, y cual es la del grupo.
 *
 * <p>Las dos consultas viven en el esquema de este servicio, y por eso estan aca y no
 * detras de un puerto: {@code cuenta_billetera} tiene {@code usuario_id} y
 * {@code grupo_id}, asi que traducir una persona o un grupo a su cuenta es una lectura
 * propia. Lo unico ajeno es el alias, y eso si se pregunta.
 *
 * <p>Solo cuentas {@code ACTIVA}: acreditar en una cerrada deja plata que nadie puede
 * sacar, y es mejor rechazar la transferencia que descubrirlo despues.
 */
@Repository
public class DestinoRepositorio {

    public Optional<UUID> cuentaDeUsuario(DSLContext dsl, UUID usuarioId) {
        return unaCuenta(dsl, "usuario_id", usuarioId, "USUARIO");
    }

    public Optional<UUID> cuentaDeGrupo(DSLContext dsl, UUID grupoId) {
        return unaCuenta(dsl, "grupo_id", grupoId, "GRUPO");
    }

    /**
     * La cuenta {@code USUARIO} de la persona EN ESA MONEDA, sea cual sea su estado: quien decide si esta operativa es el
     * caso de uso (asi distingue «no existe» de «esta cerrada», que son rechazos distintos). Es unica por
     * {@code uq_cuenta_billetera_titular_moneda}. Se lee con el rol de la sesion en curso: el consumidor corre como sistema.
     */
    public Optional<UUID> cuentaDeUsuarioEn(DSLContext dsl, UUID usuarioId, String moneda) {
        return cuentaEn(dsl, "usuario_id", usuarioId, "USUARIO", moneda);
    }

    /** La cuenta {@code GRUPO} del grupo en esa moneda, sea cual sea su estado (ver {@link #cuentaDeUsuarioEn}). */
    public Optional<UUID> cuentaDeGrupoEn(DSLContext dsl, UUID grupoId, String moneda) {
        return cuentaEn(dsl, "grupo_id", grupoId, "GRUPO", moneda);
    }

    private Optional<UUID> cuentaEn(DSLContext dsl, String columna, UUID valor, String tipo, String moneda) {
        return Optional.ofNullable(dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))
                .where(DSL.field(DSL.name(columna), UUID.class).eq(valor))
                .and(DSL.field("tipo", String.class).eq(tipo))
                .and(DSL.field("moneda", String.class).eq(moneda))
                .orderBy(DSL.field("fecha_apertura").desc())
                .limit(1)
                .fetchOne(DSL.field("id", UUID.class)));
    }

    private Optional<UUID> unaCuenta(DSLContext dsl, String columna, UUID valor, String tipo) {
        // La cuenta destino es de OTRA persona: la politica de fila deja al titular ver solo las suyas.
        // Se lee con rol de sistema y acotado a devolver un id, no la fila (B8, CU-12).
        return Optional.ofNullable(
                bo.aportaya.plataforma.datos.Datos.comoSistema(dsl, () -> dsl.select(DSL.field("id", UUID.class))
                        .from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))
                        .where(DSL.field(DSL.name(columna), UUID.class).eq(valor))
                        .and(DSL.field("tipo", String.class).eq(tipo))
                        .and(DSL.field("estado", String.class).eq("ACTIVA"))
                        .fetchOne(DSL.field("id", UUID.class))));
    }
}

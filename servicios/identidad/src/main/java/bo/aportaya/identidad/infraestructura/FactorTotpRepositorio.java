package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.FACTOR_MFA;
import static bo.aportaya.identidad.generado.Tables.USUARIO;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** El factor TOTP de un usuario en {@code factor_mfa}: leerlo, marcar su uso, enrolarlo. */
@Repository
public class FactorTotpRepositorio {

    static final String TOTP = "TOTP";

    /** El secreto cifrado y el ultimo paso aceptado del TOTP activo y confirmado. */
    public record FactorTotp(UUID id, String secretoCifrado, short versionLlave, Optional<OffsetDateTime> ultimoUso) {}

    public Optional<FactorTotp> activoDe(DSLContext dsl, UUID usuarioId) {
        return dsl.select(FACTOR_MFA.ID, FACTOR_MFA.SECRETO_CIFRADO, FACTOR_MFA.VERSION_LLAVE, FACTOR_MFA.ULTIMO_USO_EN)
                .from(FACTOR_MFA)
                .where(FACTOR_MFA.USUARIO_ID.eq(usuarioId))
                .and(FACTOR_MFA.TIPO.eq(TOTP))
                .and(FACTOR_MFA.ACTIVO.isTrue())
                .and(FACTOR_MFA.CONFIRMADO_EN.isNotNull())
                .orderBy(FACTOR_MFA.ES_PRINCIPAL.desc(), FACTOR_MFA.CONFIRMADO_EN.desc())
                .limit(1)
                .fetchOptional(r -> new FactorTotp(
                        r.get(FACTOR_MFA.ID),
                        r.get(FACTOR_MFA.SECRETO_CIFRADO),
                        r.get(FACTOR_MFA.VERSION_LLAVE),
                        Optional.ofNullable(r.get(FACTOR_MFA.ULTIMO_USO_EN))));
    }

    /**
     * Registra el paso aceptado. La precondicion va en la escritura (regla 96.3.2): si otro
     * pedido ya acepto ese paso o uno posterior, no toca filas y el codigo se rechaza. Es lo
     * que impide reusar un codigo dentro de sus treinta segundos.
     */
    public boolean marcarUso(DSLContext dsl, UUID factorId, OffsetDateTime inicioDelPaso) {
        return dsl.update(FACTOR_MFA)
                        .set(FACTOR_MFA.ULTIMO_USO_EN, inicioDelPaso)
                        .where(FACTOR_MFA.ID.eq(factorId))
                        .and(FACTOR_MFA.ULTIMO_USO_EN.isNull().or(FACTOR_MFA.ULTIMO_USO_EN.lt(inicioDelPaso)))
                        .execute()
                == 1;
    }

    public boolean tieneTotpConfirmado(DSLContext dsl, UUID usuarioId) {
        return activoDe(dsl, usuarioId).isPresent();
    }

    /**
     * Enrolamiento pendiente: borra los intentos previos sin confirmar del mismo usuario e
     * inserta uno nuevo, inactivo. Un factor sin confirmar nunca autentica.
     */
    public UUID iniciar(DSLContext dsl, UUID usuarioId, String secretoCifrado, short versionLlave) {
        dsl.deleteFrom(FACTOR_MFA)
                .where(FACTOR_MFA.USUARIO_ID.eq(usuarioId))
                .and(FACTOR_MFA.TIPO.eq(TOTP))
                .and(FACTOR_MFA.CONFIRMADO_EN.isNull())
                .execute();
        return dsl.insertInto(FACTOR_MFA)
                .set(FACTOR_MFA.USUARIO_ID, usuarioId)
                .set(FACTOR_MFA.TIPO, TOTP)
                .set(FACTOR_MFA.SECRETO_CIFRADO, secretoCifrado)
                .set(FACTOR_MFA.VERSION_LLAVE, versionLlave)
                .set(FACTOR_MFA.ACTIVO, false)
                .set(FACTOR_MFA.ES_PRINCIPAL, false)
                .returning(FACTOR_MFA.ID)
                .fetchOne(FACTOR_MFA.ID);
    }

    /** El enrolamiento pendiente de ESE usuario (nunca el de otro). */
    public Optional<FactorTotp> pendiente(DSLContext dsl, UUID usuarioId, UUID factorId) {
        return dsl.select(FACTOR_MFA.ID, FACTOR_MFA.SECRETO_CIFRADO, FACTOR_MFA.VERSION_LLAVE, FACTOR_MFA.ULTIMO_USO_EN)
                .from(FACTOR_MFA)
                .where(FACTOR_MFA.ID.eq(factorId))
                .and(FACTOR_MFA.USUARIO_ID.eq(usuarioId))
                .and(FACTOR_MFA.TIPO.eq(TOTP))
                .and(FACTOR_MFA.CONFIRMADO_EN.isNull())
                .fetchOptional(r -> new FactorTotp(
                        r.get(FACTOR_MFA.ID),
                        r.get(FACTOR_MFA.SECRETO_CIFRADO),
                        r.get(FACTOR_MFA.VERSION_LLAVE),
                        Optional.ofNullable(r.get(FACTOR_MFA.ULTIMO_USO_EN))));
    }

    /** Activa el factor; la precondicion (sigue pendiente y es del usuario) va en el UPDATE. */
    public boolean confirmar(
            DSLContext dsl, UUID usuarioId, UUID factorId, OffsetDateTime inicioDelPaso, OffsetDateTime ahora) {
        return dsl.update(FACTOR_MFA)
                        .set(FACTOR_MFA.ACTIVO, true)
                        .set(FACTOR_MFA.ES_PRINCIPAL, true)
                        .set(FACTOR_MFA.CONFIRMADO_EN, ahora)
                        .set(FACTOR_MFA.ULTIMO_USO_EN, inicioDelPaso)
                        .where(FACTOR_MFA.ID.eq(factorId))
                        .and(FACTOR_MFA.USUARIO_ID.eq(usuarioId))
                        .and(FACTOR_MFA.CONFIRMADO_EN.isNull())
                        .execute()
                == 1;
    }

    /** El codigo publico del titular: la etiqueta de la cuenta en la app de autenticacion (no es un dato personal). */
    public Optional<String> codigoPublicoDe(DSLContext dsl, UUID usuarioId) {
        return dsl.select(USUARIO.CODIGO_PUBLICO)
                .from(USUARIO)
                .where(USUARIO.ID.eq(usuarioId))
                .fetchOptional(USUARIO.CODIGO_PUBLICO);
    }
}

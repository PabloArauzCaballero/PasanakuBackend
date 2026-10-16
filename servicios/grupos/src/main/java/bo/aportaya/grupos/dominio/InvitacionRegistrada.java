package bo.aportaya.grupos.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Lo que se sabe de una invitación guardada: su estado, su vencimiento, su enlace y quién la emitió. */
public record InvitacionRegistrada(
        String estado, short envios, UUID grupoId, OffsetDateTime expiraEn, UUID tokenId, UUID emisorId) {}

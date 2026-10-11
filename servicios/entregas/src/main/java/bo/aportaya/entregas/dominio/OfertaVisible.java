package bo.aportaya.entregas.dominio;

import bo.aportaya.plataforma.dominio.Dinero;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Lo que ve quien compra: nada del vendedor, ni su nombre ni su usuario ni su participante. */
public record OfertaVisible(
        UUID ofertaId,
        UUID grupoId,
        UUID turnoId,
        Dinero derecho,
        Dinero precio,
        Dinero cargos,
        OffsetDateTime vigenteHasta) {}

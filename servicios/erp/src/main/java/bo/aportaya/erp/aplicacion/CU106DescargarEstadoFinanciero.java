package bo.aportaya.erp.aplicacion;

import bo.aportaya.erp.dominio.DocumentoDeEstadoFinanciero;
import bo.aportaya.erp.infraestructura.EstadosFinancierosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-106 · entregar el documento sellado de un estado financiero (backoffice carril A · H7).
 *
 * <p>Lo guardado en {@code datos} es JSONB y Postgres lo normaliza: no son los bytes que se
 * hashearon. Por eso no se devuelve tal cual: se reconstruye el texto canonico con
 * {@link DocumentoDeEstadoFinanciero#canonico} —la misma funcion que lo sello—, se recalcula el
 * SHA-256 y solo se entrega si coincide con el hash guardado. Si no coincide, alguien toco lo
 * guardado (o el sellado cambio): AP-CU106-04 y no se entrega un documento que diga otra cosa.
 */
@Service
public class CU106DescargarEstadoFinanciero {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Datos datos;
    private final EstadosFinancierosRepositorio estados;

    public CU106DescargarEstadoFinanciero(Datos datos, EstadosFinancierosRepositorio estados) {
        this.datos = datos;
        this.estados = estados;
    }

    @Transactional(readOnly = true)
    public Documento descargar(UUID estadoFinancieroId, ContextoSesion ctx) {
        var guardado = datos.conContexto(ctx, dsl -> estados.guardado(dsl, estadoFinancieroId))
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(106, 3), "Ese estado financiero no existe."));
        String texto = reconstruir(guardado.datos());
        if (!DocumentoDeEstadoFinanciero.sha256(texto).equals(guardado.hashContenido())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(106, 4),
                    "El documento guardado ya no corresponde a su hash sellado: no se entrega.");
        }
        return new Documento(
                guardado.id(),
                guardado.tipo(),
                guardado.generadoEn(),
                texto.getBytes(StandardCharsets.UTF_8),
                guardado.hashContenido());
    }

    private static String reconstruir(String guardado) {
        try {
            JsonNode nodo = JSON.readTree(guardado);
            var renglones = new ArrayList<DocumentoDeEstadoFinanciero.Renglon>();
            for (JsonNode r : nodo.path("renglones")) {
                renglones.add(new DocumentoDeEstadoFinanciero.Renglon(
                        r.path("concepto").asText(),
                        new BigDecimal(r.path("monto").asText())));
            }
            return DocumentoDeEstadoFinanciero.canonico(nodo.path("tipo").asText(), renglones);
        } catch (IOException | NumberFormatException ilegible) {
            throw new ErrorDeNegocio(
                    CodigoError.de(106, 4), "El documento guardado no se puede reconstruir: no se entrega.");
        }
    }

    public record Documento(UUID id, String tipo, OffsetDateTime generadoEn, byte[] contenido, String hashContenido) {}
}

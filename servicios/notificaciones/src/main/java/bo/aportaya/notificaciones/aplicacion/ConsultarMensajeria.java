package bo.aportaya.notificaciones.aplicacion;

import bo.aportaya.notificaciones.infraestructura.AuditoriaDeMensajeriaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-80 y CU-83 · lo que Auditoria mira de la mensajeria (backoffice carril A · A6): las plantillas con
 * su version y los intentos de envio con su estado de entrega.
 *
 * <p>Sin filtro trae todos los estados: no hay una definicion de cuales son «excepcion» y no se
 * inventa. El permiso lo exige la pagina; aca se validan el filtro y el cursor (AP-CU80-05 y
 * AP-CU83-06). Nada de lo que devuelve identifica a quien recibio el mensaje.
 */
@Service
public class ConsultarMensajeria {

    /** Los de {@code ck_plantilla_mensaje_estado_aprobacion}. */
    private static final List<String> ESTADOS_DE_PLANTILLA =
            List.of("APROBADA", "BORRADOR", "ENVIADA", "PAUSADA", "RECHAZADA");

    /** Los de {@code ck_envio_notificacion_estado}. */
    private static final List<String> ESTADOS_DE_ENVIO = List.of(
            "ACEPTADO_POR_PROVEEDOR", "ENTREGADO", "ENVIADO", "EXPIRADO", "FALLIDO", "LEIDO", "PENDIENTE", "RECHAZADO");

    private final Datos datos;
    private final AuditoriaDeMensajeriaRepositorio auditoria;

    public ConsultarMensajeria(Datos datos, AuditoriaDeMensajeriaRepositorio auditoria) {
        this.datos = datos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public Pagina<Plantilla> plantillas(
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        List<String> filtro = validar(estados, ESTADOS_DE_PLANTILLA, antesDe, antesDeId, CodigoError.de(80, 5));
        var items =
                datos.conContexto(ctx, dsl -> auditoria.plantillas(dsl, filtro, antesDe, antesDeId, limite)).stream()
                        .map(f -> new Plantilla(
                                f.id(),
                                f.codigo(),
                                f.evento(),
                                f.canal(),
                                f.categoriaProveedor(),
                                f.estadoAprobacion(),
                                f.activa(),
                                f.versionId(),
                                f.version(),
                                f.idioma(),
                                f.vigenteDesde(),
                                Optional.ofNullable(f.vigenteHasta())))
                        .toList();
        return pagina(items, limite, Plantilla::vigenteDesde, Plantilla::versionId);
    }

    @Transactional(readOnly = true)
    public Pagina<Envio> envios(
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        List<String> filtro = validar(estados, ESTADOS_DE_ENVIO, antesDe, antesDeId, CodigoError.de(83, 6));
        var items = datos.conContexto(ctx, dsl -> auditoria.envios(dsl, filtro, antesDe, antesDeId, limite)).stream()
                .map(f -> new Envio(
                        f.id(),
                        f.notificacionId(),
                        f.proveedorId(),
                        f.proveedor(),
                        f.versionPlantillaId(),
                        f.canal(),
                        f.estado(),
                        f.orden(),
                        f.intentos(),
                        f.maxIntentos(),
                        // La escala de la columna (NUMERIC(10,4)); UNNECESSARY falla antes que redondear.
                        f.costo().setScale(4, RoundingMode.UNNECESSARY),
                        f.moneda(),
                        Optional.ofNullable(f.codigoError()),
                        f.encoladoEn(),
                        Optional.ofNullable(f.enviadoEn()),
                        Optional.ofNullable(f.entregadoEn()),
                        Optional.ofNullable(f.leidoEn()),
                        Optional.ofNullable(f.proximoReintentoEn()),
                        Optional.ofNullable(f.acuseTipo())
                                .map(tipo -> new Acuse(
                                        tipo,
                                        f.acuseFechaHoraProveedor(),
                                        f.acuseRecibidoEn(),
                                        Optional.ofNullable(f.acuseCodigoError())))))
                .toList();
        return pagina(items, limite, Envio::encoladoEn, Envio::envioId);
    }

    private static List<String> validar(
            List<String> estados,
            List<String> conocidos,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            CodigoError codigo) {
        List<String> filtro = estados == null || estados.isEmpty() ? conocidos : estados;
        if (!conocidos.containsAll(filtro) || antesDe.isPresent() != antesDeId.isPresent()) {
            throw new ErrorDeNegocio(codigo, "Filtro de listado invalido: estado desconocido o cursor a medias.");
        }
        return filtro;
    }

    /** Pagina llena => puede haber mas: el cursor es la ultima fila. Pagina corta => se termino. */
    private static <T> Pagina<T> pagina(
            List<T> items, int limite, Function<T, OffsetDateTime> momento, Function<T, UUID> id) {
        Optional<T> ultima = items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new Pagina<>(items, ultima.map(momento), ultima.map(id));
    }

    public record Pagina<T>(List<T> items, Optional<OffsetDateTime> proximoAntesDe, Optional<UUID> proximoAntesDeId) {}

    public record Plantilla(
            UUID plantillaId,
            String codigo,
            String evento,
            String canal,
            String categoriaProveedor,
            String estadoAprobacion,
            boolean activa,
            UUID versionId,
            int version,
            String idioma,
            OffsetDateTime vigenteDesde,
            Optional<OffsetDateTime> vigenteHasta) {}

    public record Envio(
            UUID envioId,
            UUID notificacionId,
            UUID proveedorId,
            String proveedor,
            UUID versionPlantillaId,
            String canal,
            String estado,
            int orden,
            int intentos,
            int maxIntentos,
            BigDecimal costo,
            String moneda,
            Optional<String> codigoError,
            OffsetDateTime encoladoEn,
            Optional<OffsetDateTime> enviadoEn,
            Optional<OffsetDateTime> entregadoEn,
            Optional<OffsetDateTime> leidoEn,
            Optional<OffsetDateTime> proximoReintentoEn,
            Optional<Acuse> ultimoAcuse) {}

    public record Acuse(
            String tipoEvento,
            OffsetDateTime fechaHoraProveedor,
            OffsetDateTime recibidoEn,
            Optional<String> codigoError) {}
}

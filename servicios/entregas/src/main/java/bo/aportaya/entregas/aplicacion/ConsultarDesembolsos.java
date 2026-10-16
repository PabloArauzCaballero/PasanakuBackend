package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.infraestructura.BandejaDeDesembolsosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-28 · la bandeja de desembolsos que mira Tesoreria (backoffice carril A · H8).
 *
 * <p>Por omision trae las excepciones —lo que alguien tiene que resolver—, no todo el historial. El
 * permiso lo exige la pagina; aca se validan el filtro y el cursor (AP-CU28-07).
 */
@Service
public class ConsultarDesembolsos {

    /** Lo que espera a una persona: rechazos, devoluciones del banco y lo que sigue en vuelo. */
    static final List<String> EXCEPCIONES = List.of("RECHAZADA", "DEVUELTA_POR_BANCO", "EN_PROCESO");

    private static final List<String> ESTADOS = List.of(
            "CREADA",
            "ENVIADA_A_PROVEEDOR",
            "EN_PROCESO",
            "ACREDITADA",
            "RECHAZADA",
            "DEVUELTA_POR_BANCO",
            "CANCELADA");

    private final Datos datos;
    private final BandejaDeDesembolsosRepositorio bandeja;

    public ConsultarDesembolsos(Datos datos, BandejaDeDesembolsosRepositorio bandeja) {
        this.datos = datos;
        this.bandeja = bandeja;
    }

    @Transactional(readOnly = true)
    public Pagina listar(
            List<String> estados,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite,
            ContextoSesion ctx) {
        List<String> filtro = estados == null || estados.isEmpty() ? EXCEPCIONES : estados;
        if (!ESTADOS.containsAll(filtro) || antesDe.isPresent() != antesDeId.isPresent()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(28, 7), "Filtro de listado invalido: estado desconocido o cursor a medias.");
        }
        var items = datos.conContexto(ctx, dsl -> bandeja.listar(dsl, filtro, antesDe, antesDeId, limite)).stream()
                .map(f -> new Desembolso(
                        f.id(),
                        f.entregaId(),
                        f.proveedorId(),
                        Dinero.de(f.monto(), Moneda.valueOf(f.moneda())),
                        f.estado(),
                        f.intentos(),
                        Optional.ofNullable(f.ultimoResultado()),
                        Optional.ofNullable(f.ultimoCodigoError()),
                        Optional.ofNullable(f.reintentableEn()),
                        f.creadaEn(),
                        Optional.ofNullable(f.acreditadaEn())))
                .toList();
        // Pagina llena => puede haber mas: el cursor es la ultima fila. Pagina corta => se termino.
        Optional<Desembolso> ultima =
                items.size() == limite ? Optional.of(items.get(items.size() - 1)) : Optional.empty();
        return new Pagina(items, ultima.map(Desembolso::creadaEn), ultima.map(Desembolso::ordenId));
    }

    public record Pagina(
            List<Desembolso> items, Optional<OffsetDateTime> proximoAntesDe, Optional<UUID> proximoAntesDeId) {}

    public record Desembolso(
            UUID ordenId,
            UUID entregaId,
            UUID proveedorId,
            Dinero monto,
            String estado,
            int intentos,
            Optional<String> ultimoResultado,
            Optional<String> ultimoCodigoError,
            Optional<OffsetDateTime> reintentableEn,
            OffsetDateTime creadaEn,
            Optional<OffsetDateTime> acreditadaEn) {}
}

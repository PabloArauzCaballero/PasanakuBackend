package bo.aportaya.notificaciones.aplicacion;

import bo.aportaya.notificaciones.generado.tables.records.AvisoInternoRecord;
import bo.aportaya.notificaciones.infraestructura.AvisoInternoRepositorio;
import bo.aportaya.notificaciones.web.generado.modelo.AvisoInterno;
import bo.aportaya.notificaciones.web.generado.modelo.EntradaAvisoInterno;
import bo.aportaya.notificaciones.web.generado.modelo.PaginaDeAvisosInternos;
import bo.aportaya.notificaciones.web.generado.modelo.SalidaMarcarTodosLeidos;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso CU-80 (ampliado) · Avisos internos para operadores (la campana del backoffice).
 */
@Service
public class GestionarAvisosInternos {

    private final Datos datos;
    private final AvisoInternoRepositorio repo;
    private final Reloj reloj;

    public GestionarAvisosInternos(Datos datos, AvisoInternoRepositorio repo, Reloj reloj) {
        this.datos = datos;
        this.repo = repo;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PaginaDeAvisosInternos consultar(
            UUID usuarioId, Collection<String> roles, boolean soloNoLeidos, int limite, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            int noLeidos = repo.contarNoLeidos(dsl, usuarioId, roles);
            List<AvisoInternoRecord> registros = repo.listarPara(dsl, usuarioId, roles, soloNoLeidos, limite);
            List<AvisoInterno> elementos = registros.stream().map(this::mapear).toList();
            return new PaginaDeAvisosInternos().totalNoLeidos(noLeidos).elementos(elementos);
        });
    }

    @Transactional
    public Optional<AvisoInterno> marcarLeido(UUID id, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atZone(Reloj.ZONA).toOffsetDateTime();
        return datos.conContexto(ctx, dsl -> repo.marcarLeido(dsl, id, ahora).map(this::mapear));
    }

    @Transactional
    public SalidaMarcarTodosLeidos marcarTodosLeidos(UUID usuarioId, Collection<String> roles, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atZone(Reloj.ZONA).toOffsetDateTime();
        int cant = datos.conContexto(ctx, dsl -> repo.marcarTodosLeidos(dsl, usuarioId, roles, ahora));
        return new SalidaMarcarTodosLeidos().modificados(cant);
    }

    @Transactional
    public AvisoInterno emitir(EntradaAvisoInterno entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atZone(Reloj.ZONA).toOffsetDateTime();
        return datos.conContexto(ctx, dsl -> {
            AvisoInternoRecord rec = repo.crear(
                    dsl,
                    entrada.getUsuarioId(),
                    entrada.getRolDestinatario(),
                    entrada.getTitulo(),
                    entrada.getMensaje(),
                    entrada.getTipo().getValue(),
                    entrada.getEnlace(),
                    ahora);
            return mapear(rec);
        });
    }

    private AvisoInterno mapear(AvisoInternoRecord r) {
        AvisoInterno.TipoEnum tipo;
        try {
            tipo = AvisoInterno.TipoEnum.fromValue(r.getTipo());
        } catch (Exception e) {
            tipo = AvisoInterno.TipoEnum.INFO;
        }
        return new AvisoInterno()
                .id(r.getId())
                .usuarioId(r.getUsuarioId())
                .rolDestinatario(r.getRolDestinatario())
                .titulo(r.getTitulo())
                .mensaje(r.getMensaje())
                .tipo(tipo)
                .enlace(r.getEnlace())
                .leidoEn(r.getLeidoEn())
                .creadoEn(r.getCreadoEn());
    }
}

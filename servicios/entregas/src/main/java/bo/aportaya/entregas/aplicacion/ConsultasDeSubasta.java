package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.dominio.OfertaParaElPago;
import bo.aportaya.entregas.infraestructura.PujaRepositorio;
import bo.aportaya.entregas.infraestructura.PujaRepositorio.Puja;
import bo.aportaya.entregas.infraestructura.SubastaRepositorio;
import bo.aportaya.entregas.infraestructura.SubastaRepositorio.Subasta;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que se lee de una subasta: para cualquier usuario (la lista y el detalle, sin el saliente ni los postores) y
 * para la billetera (el precio a pagarle al saliente). Quien mira solo ve SU puja, nunca la de otro.
 */
@Service
public class ConsultasDeSubasta {

    /** Lo que la billetera puede pagar: el cupo ya es del ganador. */
    private static final Set<String> PAGABLES = Set.of("LIQUIDANDO", "VENDIDA");

    private static final Dinero UN_CENTAVO_BOB = Dinero.de("0.01", bo.aportaya.plataforma.dominio.Moneda.BOB);

    private final Datos datos;
    private final SubastaRepositorio subastas;
    private final PujaRepositorio pujas;
    private final Reloj reloj;

    public ConsultasDeSubasta(Datos datos, SubastaRepositorio subastas, PujaRepositorio pujas, Reloj reloj) {
        this.datos = datos;
        this.subastas = subastas;
        this.pujas = pujas;
        this.reloj = reloj;
    }

    /** Lo que ve cualquiera: sin el saliente ni los postores, salvo la puja propia. */
    public record Vista(
            UUID subastaId,
            UUID grupoId,
            UUID cupoId,
            String nombreDelGrupo,
            String periodicidad,
            Dinero aporte,
            Dinero base,
            Optional<Dinero> masAlto,
            Dinero minimo,
            String estado,
            OffsetDateTime cierraEn,
            int cantidadDePujas,
            boolean soyElSaliente,
            Optional<MiPuja> miPuja) {}

    public record MiPuja(Dinero monto, String estado) {}

    @Transactional(readOnly = true)
    public Optional<Vista> ver(UUID subastaId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> subastas.ver(dsl, subastaId).map(s -> vista(dsl, s, ctx)));
    }

    @Transactional(readOnly = true)
    public Optional<Vista> delCupo(UUID cupoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> subastas.ultimaDelCupo(dsl, cupoId).map(s -> vista(dsl, s, ctx)));
    }

    @Transactional(readOnly = true)
    public List<Vista> abiertas(ContextoSesion ctx, int limite, int pagina) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> subastas.abiertas(dsl, ahora, limite, pagina).stream()
                .map(s -> vista(dsl, s, ctx))
                .toList());
    }

    /** El hecho que pide la billetera para pagarle al saliente: la puja ganadora, sin cargos (ambiguedad A2). */
    @Transactional(readOnly = true)
    public Optional<OfertaParaElPago> paraElPago(UUID subastaId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> subastas.ver(dsl, subastaId))
                .filter(s -> PAGABLES.contains(s.estado()) && s.ganador().isPresent())
                .map(s -> new OfertaParaElPago(
                        s.id(),
                        s.vendedorUsuarioId(),
                        s.ganador().get(),
                        Dinero.cero(s.base().moneda()),
                        s.estado()));
    }

    private Vista vista(DSLContext dsl, Subasta s, ContextoSesion ctx) {
        var vigente = pujas.vigente(dsl, s.id()).map(Puja::monto);
        var masAlto = vigente.or(() -> s.ganador());
        // Un centavo mas que la mas alta (A1: sin incremento minimo definido); sin pujas, la base.
        Dinero minimo = vigente.map(m -> m.mas(Dinero.de(UN_CENTAVO_BOB.monto(), m.moneda())))
                .orElse(s.base());
        return new Vista(
                s.id(),
                s.grupoId(),
                s.cupoId(),
                s.nombreDelGrupo(),
                s.periodicidad(),
                s.aporte(),
                s.base(),
                masAlto,
                minimo,
                s.estado(),
                s.cierraEn(),
                pujas.cantidadDePujas(dsl, s.id()),
                s.vendedorUsuarioId().equals(ctx.usuarioId()),
                pujas.ultimaDe(dsl, s.id(), ctx.usuarioId()).map(p -> new MiPuja(p.monto(), p.estado())));
    }
}

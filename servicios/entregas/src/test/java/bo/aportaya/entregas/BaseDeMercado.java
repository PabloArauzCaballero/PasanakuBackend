package bo.aportaya.entregas;

import bo.aportaya.entregas.aplicacion.GestionDeOfertas;
import bo.aportaya.entregas.aplicacion.RegistroDeOfertas;
import bo.aportaya.entregas.dominio.puertos.HechosDeGrupos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

/** Un turno con vendedor, un comprador miembro del mismo grupo y saldo para comprar. */
abstract class BaseDeMercado extends BaseDeEntregas {

    @BeforeEach
    void reiniciarDobles() {
        fondosDoble.reiniciar();
        gruposDoble.seCae(false);
    }

    protected static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    protected record Caso(
            UUID vendedor,
            FixturaDeEntregas.Escenario e,
            FixturaDeEntregas.Miembro comprador,
            ContextoSesion ctxVendedor,
            ContextoSesion ctxComprador) {}

    protected Caso caso() {
        UUID vendedor = fixtura.usuario();
        var e = fixtura.escenario(vendedor);
        var comprador = fixtura.miembro(e.grupoId());
        gruposDoble.turno(new HechosDeGrupos.Turno(
                e.turnoId(), e.grupoId(), e.cupoId(), e.participanteId(), vendedor, "PROGRAMADO", bob("6000.00")));
        gruposDoble.miembro(e.grupoId(), vendedor, e.participanteId(), true);
        gruposDoble.miembro(e.grupoId(), comprador.usuarioId(), comprador.participanteId(), true);
        fondosDoble.saldo(comprador.usuarioId(), bob("10000.00"));
        return new Caso(vendedor, e, comprador, contextoDe(vendedor), contextoDe(comprador.usuarioId()));
    }

    protected GestionDeOfertas.Entrada oferta(
            Caso c, String precio, String cargos, OffsetDateTime hasta, String clave) {
        return new GestionDeOfertas.Entrada(c.e().turnoId(), bob(precio), bob(cargos), hasta, clave);
    }

    /** Publica una oferta de Bs 5.500 con cargos de Bs 50, vigente un dia. */
    protected RegistroDeOfertas.Publicada publicar(Caso c) {
        return ofertasCU.publicar(
                oferta(c, "5500.00", "50.00", OffsetDateTime.now().plusDays(1), "oferta-" + c.e().turnoId()),
                c.ctxVendedor());
    }

    protected String estadoDeOferta(UUID id) {
        return dsl.fetchOne("SELECT estado FROM entregas.oferta_turno WHERE id = ?", id)
                .get(0, String.class);
    }

    protected String estadoDeCesion(UUID id) {
        return dsl.fetchOne("SELECT estado FROM entregas.cesion_derecho WHERE id = ?", id)
                .get(0, String.class);
    }

    protected BigDecimal numero(String consulta, Object... parametros) {
        return dsl.fetchOne(consulta, parametros).get(0, BigDecimal.class);
    }
}

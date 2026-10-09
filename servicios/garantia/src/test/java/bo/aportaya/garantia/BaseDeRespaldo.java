package bo.aportaya.garantia;

import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo;
import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.EntradaCobertura;
import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.SalidaCobertura;
import bo.aportaya.garantia.aplicacion.CU23ReservarRespaldo.EntradaReserva;
import bo.aportaya.garantia.aplicacion.CU23ReservarRespaldo.SalidaReserva;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Lo comun de las pruebas del respaldo empresarial: un grupo con reserva y sus obligaciones. */
abstract class BaseDeRespaldo extends BaseDeGarantia {

    protected static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    protected static ClaveIdempotencia clave(String prefijo) {
        return new ClaveIdempotencia(prefijo + "-" + UUID.randomUUID());
    }

    /** Un grupo del carril con dos obligaciones faltantes de Bs 500 (faltante total: 1.000). */
    protected record Caso(
            UUID usuario,
            FixturaDeGarantia.Escenario escenario,
            UUID turno,
            UUID obligacionA,
            UUID obligacionB,
            ContextoSesion ctx) {}

    protected Caso caso() {
        UUID usuario = fixtura.usuario();
        var escenario = fixtura.escenario(usuario);
        UUID turno = fixturaDeRespaldo.turno(escenario);
        UUID b = fixturaDeRespaldo.otraObligacion(fixtura, escenario, "500.00");
        return new Caso(usuario, escenario, turno, escenario.obligacionId(), b, contextoDe(usuario));
    }

    protected SalidaReserva reservar(Caso c, String monto) {
        return transaccion.execute(t -> reservaCU.reservar(
                new EntradaReserva(c.escenario().grupoId(), 1, bob(monto), c.usuario(), clave("reserva")), c.ctx()));
    }

    /** Pozo 6.000 con 5.000 confirmados: faltan 1.000, de dos obligaciones de 500. */
    protected EntradaCobertura pedidoDelEjemplo(Caso c, String clave) {
        return new EntradaCobertura(
                c.escenario().grupoId(),
                c.escenario().periodoId(),
                c.turno(),
                bob("6000.00"),
                bob("5000.00"),
                bob("0.00"),
                List.of(
                        new CU23CubrirConRespaldo.Linea(c.obligacionA(), bob("500.00")),
                        new CU23CubrirConRespaldo.Linea(c.obligacionB(), bob("500.00"))),
                OffsetDateTime.now(),
                new ClaveIdempotencia(clave));
    }

    protected SalidaCobertura cubrir(Caso c, EntradaCobertura pedido) {
        return transaccion.execute(t -> respaldoCU.cubrir(pedido, c.ctx()));
    }

    /** El periodo al que pertenece un turno (R-GAR-12 exige que la cobertura lo declare bien). */
    protected UUID periodoDe(UUID turno) {
        return dsl.fetchOne("SELECT periodo_id FROM grupos.turno WHERE id = ?", turno)
                .get(0, UUID.class);
    }

    protected BigDecimal numero(String consulta, Object... parametros) {
        return dsl.fetchOne(consulta, parametros).get(0, BigDecimal.class);
    }
}

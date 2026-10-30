package bo.aportaya.entregas;

import bo.aportaya.entregas.aplicacion.ConsultasDeSubasta;
import bo.aportaya.entregas.aplicacion.SubastaDelCupo;
import bo.aportaya.entregas.aplicacion.TransicionesDeSubasta;
import bo.aportaya.entregas.aplicacion.TransicionesDelCierre;
import bo.aportaya.entregas.infraestructura.PujaRepositorio;
import bo.aportaya.entregas.infraestructura.SubastaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * La subasta del cupo armada a mano contra Postgres real, con {@code grupos} y la billetera como DOBLES declarados
 * (regla 65): {@link DobleDeGruposParaSubasta} y {@link DoblesDelMercado.Fondos}, que conserva el total del sistema.
 */
abstract class BaseDeSubasta extends BaseDeEntregas {

    protected static final AtomicReference<Instant> AHORA = new AtomicReference<>(Instant.now());
    protected static DobleDeGruposParaSubasta gruposSubasta;
    protected static SubastaDelCupo subastaCU;
    protected static ConsultasDeSubasta consultasCU;
    protected static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000136");

    @BeforeAll
    static void armarSubasta() {
        gruposSubasta = new DobleDeGruposParaSubasta();
        var datos = new Datos(dsl);
        var subastas = new SubastaRepositorio();
        var pujas = new PujaRepositorio();
        bo.aportaya.plataforma.dominio.Reloj reloj = AHORA::get;
        // Como en el mercado: el mismo interceptor @Transactional que pondria Spring sobre los pasos locales; la saga
        // (SubastaDelCupo) queda SIN transaccion y las llamadas a grupos y a la billetera, afuera.
        var gestor = transaccion.getTransactionManager();
        var pasos = conTransaccion(
                new TransicionesDeSubasta(datos, subastas, pujas, new Outbox("entregas"), reloj), gestor);
        var cierre = conTransaccion(
                new TransicionesDelCierre(datos, subastas, pujas, new Outbox("entregas"), reloj), gestor);
        subastaCU = new SubastaDelCupo(gruposSubasta, fondosDoble, pasos, cierre, reloj);
        consultasCU = conTransaccion(new ConsultasDeSubasta(datos, subastas, pujas, reloj), gestor);
    }

    @BeforeEach
    void reiniciar() {
        AHORA.set(Instant.now());
        fondosDoble.reiniciar();
        gruposSubasta.reiniciar();
    }

    protected static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    protected ContextoSesion sistema() {
        return ContextoSesion.deSistema(PROCESO, new Traza(UUID.randomUUID().toString()));
    }

    /** Un cupo de un grupo real (las FK existen), su titular y el aporte del grupo. */
    protected record Cupo(UUID saliente, FixturaDeEntregas.Escenario e) {}

    protected Cupo cupoSubastable() {
        UUID saliente = fixtura.usuario();
        var e = fixtura.escenario(saliente);
        gruposSubasta.cupo(e.cupoId(), saliente, e.participanteId(), bob("500.00"), Optional.empty());
        return new Cupo(saliente, e);
    }

    protected UUID postorConSaldo(String saldo) {
        UUID postor = fixtura.usuario();
        fondosDoble.saldo(postor, bob(saldo));
        return postor;
    }

    protected UUID abrir(Cupo c, String base) {
        gruposSubasta.sesionDe(c.saliente());
        return subastaCU
                .abrir(
                        new SubastaDelCupo.Apertura(
                                c.e().grupoId(), c.e().cupoId(), bob(base), 24, "subasta:" + c.e().cupoId()),
                        contextoDe(c.saliente()))
                .subastaId();
    }

    protected SubastaDelCupo.ResultadoDePuja pujar(UUID subasta, UUID postor, String monto) {
        return pujar(subasta, postor, monto, "puja:" + UUID.randomUUID());
    }

    protected SubastaDelCupo.ResultadoDePuja pujar(UUID subasta, UUID postor, String monto, String clave) {
        gruposSubasta.sesionDe(postor);
        return subastaCU.pujar(subasta, bob(monto), true, clave, contextoDe(postor));
    }

    /**
     * Pasa el plazo (24 h) y sigue la saga de ESA subasta, como lo hace el trabajo programado con cada una. No se usa
     * {@code correrCierres}: la base es compartida y cerraria tambien las subastas que dejaron otras pruebas.
     */
    protected String vencerYCerrar(UUID subasta) {
        AHORA.set(AHORA.get().plus(Duration.ofHours(25)));
        return seguir(subasta);
    }

    protected String seguir(UUID subasta) {
        String estado = subastaCU.avanzar(subasta, sistema());
        subastaCU.devolverSuperadas(Optional.of(subasta), sistema());
        return estado;
    }

    protected String estadoDeSubasta(UUID id) {
        return dsl.fetchOne("SELECT estado FROM entregas.subasta_cupo WHERE id = ?", id)
                .get(0, String.class);
    }

    protected String estadoDePuja(UUID id) {
        return dsl.fetchOne("SELECT estado FROM entregas.puja_subasta WHERE id = ?", id)
                .get(0, String.class);
    }

    protected int eventos(String tipo, UUID subasta) {
        return dsl.fetchOne(
                        "SELECT count(*)::int FROM entregas.evento_dominio WHERE tipo = ? AND agregado_id = ?",
                        tipo,
                        subasta)
                .get(0, Integer.class);
    }

    protected static String codigo(Throwable t) {
        return ((ErrorDeNegocio) t).codigo().toString();
    }
}

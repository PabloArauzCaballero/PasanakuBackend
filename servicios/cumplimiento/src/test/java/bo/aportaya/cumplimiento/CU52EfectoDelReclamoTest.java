package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo.EntradaReclamo;
import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo.EntradaRespuesta;
import bo.aportaya.cumplimiento.aplicacion.CU53ElevarReclamo.EntradaInstancia;
import bo.aportaya.cumplimiento.aplicacion.CU53ElevarReclamo.EntradaResolucion;
import bo.aportaya.cumplimiento.aplicacion.ConsultarEfectoDelReclamo;
import bo.aportaya.cumplimiento.dominio.EfectoDelReclamo.Motivo;
import bo.aportaya.cumplimiento.infraestructura.ReclamoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H13.S1.M6 · Un reclamo en revision no equivale a una sancion firme ni a una deuda ejecutable.
 *
 * <p>Contra la base real, recorriendo el ciclo de vida de un reclamo con los casos de uso que ya
 * existen (CU-52 y CU-53) y mirando, en cada paso, que frena. Se prueba la TRAZABILIDAD: cada caso
 * trae su estado, su resultado, su fecha y el motivo del efecto.
 */
class CU52EfectoDelReclamoTest extends BaseDeCumplimiento {

    private static final int DIAS_PARA_ELEVAR = 30;

    private UUID usuario;
    private String punto;
    private ContextoSesion ctx;
    private ConsultarEfectoDelReclamo efectos;

    @BeforeEach
    void escenario() {
        usuario = fixtura.usuario();
        punto = "APP-" + UUID.randomUUID().toString().substring(0, 8);
        gobiernoFixtura.puntoDeReclamo(punto, "APP", true);
        ctx = ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
        efectos = new ConsultarEfectoDelReclamo(
                new Datos(dsl), new ReclamoRepositorio(), Reloj.delSistema(), DIAS_PARA_ELEVAR);
    }

    private UUID ingresar(String producto) {
        return transaccion
                .execute(t -> reclamoCU.ingresar(
                        new EntradaReclamo(
                                usuario,
                                punto,
                                "COMISION",
                                producto,
                                null,
                                "Me cobraron una comision que no figura en el tarifario publicado.",
                                "APP"),
                        ctx))
                .reclamoId();
    }

    private void responder(UUID reclamo, String resultado) {
        transaccion.execute(t ->
                reclamoCU.responder(new EntradaRespuesta(reclamo, resultado, "Respuesta de la entidad", null), ctx));
    }

    private ConsultarEfectoDelReclamo.Resultado efecto(Optional<String> producto) {
        return transaccion.execute(t -> efectos.delUsuario(usuario, producto, ctx));
    }

    @Test
    @DisplayName("rechaza · mientras el reclamo esta en revision no hay sancion firme ni deuda ejecutable")
    void rechazaMientrasEstaEnRevision() {
        UUID reclamo = ingresar("BILLETERA");

        var resultado = efecto(Optional.empty());

        assertThat(resultado.bloqueaSancionFirme()).isTrue();
        assertThat(resultado.bloqueaDeudaEjecutable()).isTrue();
        assertThat(resultado.casos()).singleElement().satisfies(c -> {
            assertThat(c.reclamoId()).isEqualTo(reclamo);
            assertThat(c.estado()).isEqualTo("INGRESADO");
            assertThat(c.efecto().motivo()).isEqualTo(Motivo.EN_REVISION);
        });
    }

    @Test
    @DisplayName(
            "rechaza · cerrado en contra frena mientras el cliente puede elevar, y deja de frenar vencido el plazo")
    void rechazaHastaQueVenceElPlazoDeElevacion() {
        UUID reclamo = ingresar("BILLETERA");
        responder(reclamo, "DESFAVORABLE");

        var enPlazo = efecto(Optional.empty());
        assertThat(enPlazo.bloqueaDeudaEjecutable()).isTrue();
        assertThat(enPlazo.casos().get(0).efecto().motivo()).isEqualTo(Motivo.PLAZO_DE_ELEVACION_ABIERTO);
        assertThat(enPlazo.casos().get(0).resultado()).isEqualTo("DESFAVORABLE");
        assertThat(enPlazo.casos().get(0).fechaRespuesta()).isNotNull();

        // Se envejece la respuesta: el plazo que la norma da para elevar ya paso.
        dsl.execute(
                "UPDATE cumplimiento.reclamo_cliente SET fecha_respuesta = now() - interval '90 days' WHERE id = ?",
                reclamo);

        var vencido = efecto(Optional.empty());
        assertThat(vencido.bloqueaSancionFirme()).isFalse();
        assertThat(vencido.bloqueaDeudaEjecutable()).isFalse();
        assertThat(vencido.casos().get(0).efecto().motivo()).isEqualTo(Motivo.TERMINADO_SIN_BLOQUEO);
    }

    @Test
    @DisplayName(
            "rechaza · elevado sin resolver frena; resuelto con resarcimiento sigue frenando; resuelto sin monto libera")
    void rechazaSegunLaInstanciaSuperior() {
        UUID sinMonto = ingresar("BILLETERA");
        responder(sinMonto, "DESFAVORABLE");
        var elevadaSinMonto = transaccion.execute(
                t -> instanciaCU.elevar(new EntradaInstancia(sinMonto, "REGULADOR", "EXP-S2-1", true), ctx));
        dsl.execute(
                "UPDATE cumplimiento.reclamo_cliente SET fecha_respuesta = now() - interval '90 days' WHERE id = ?",
                sinMonto);

        // Elevado y sin resolver: frena aunque el plazo de elevacion ya hubiera pasado.
        var abierto = efecto(Optional.empty());
        assertThat(abierto.casos().get(0).efecto().motivo()).isEqualTo(Motivo.INSTANCIA_SIN_RESOLVER);
        assertThat(abierto.bloqueaDeudaEjecutable()).isTrue();

        // Resuelto sin resarcimiento: la instancia confirmo y ya no queda nada abierto.
        transaccion.execute(t -> instanciaCU.resolver(
                new EntradaResolucion(elevadaSinMonto.instanciaId(), "Se confirma la respuesta", null, null), ctx));
        var resuelto = efecto(Optional.empty());
        assertThat(resuelto.bloqueaDeudaEjecutable()).isFalse();
        assertThat(resuelto.bloqueaSancionFirme()).isFalse();

        // Otro reclamo: la instancia ordena devolver. La plataforma debe, no cobra.
        UUID conMonto = ingresar("BILLETERA");
        responder(conMonto, "DESFAVORABLE");
        var elevadaConMonto = transaccion.execute(
                t -> instanciaCU.elevar(new EntradaInstancia(conMonto, "DEFENSORIA", "EXP-S2-2", true), ctx));
        transaccion.execute(t -> instanciaCU.resolver(
                new EntradaResolucion(
                        elevadaConMonto.instanciaId(),
                        "Se ordena devolver",
                        new BigDecimal("18.00"),
                        UUID.randomUUID()),
                ctx));

        var resarcido = efecto(Optional.empty());
        assertThat(resarcido.bloqueaDeudaEjecutable()).isTrue();
        assertThat(resarcido.casos())
                .filteredOn(c -> c.reclamoId().equals(conMonto))
                .singleElement()
                .satisfies(c -> assertThat(c.efecto().motivo()).isEqualTo(Motivo.RESARCIMIENTO_DISPUESTO));
    }

    @Test
    @DisplayName(
            "rechaza · un reclamo de un producto no frena la deuda de otro producto, y la persona sin reclamos queda libre")
    void rechazaSoloLoQueCorresponde() {
        ingresar("BILLETERA");

        var delProducto = efecto(Optional.of("BILLETERA"));
        var deOtro = efecto(Optional.of("OTRO_PRODUCTO"));

        assertThat(delProducto.bloqueaDeudaEjecutable()).isTrue();
        assertThat(deOtro.bloqueaDeudaEjecutable()).isFalse();
        assertThat(deOtro.casos()).isEmpty();
        assertThat(deOtro.motivoGlobal()).isEqualTo(Motivo.SIN_RECLAMOS);

        // Otra persona, otra historia: nada de lo anterior la toca.
        UUID otra = fixtura.usuario();
        var libre = transaccion.execute(t -> efectos.delUsuario(
                otra,
                Optional.empty(),
                ContextoSesion.de(
                        otra, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()))));
        assertThat(libre.bloqueaDeudaEjecutable()).isFalse();
        assertThat(libre.motivoGlobal()).isEqualTo(Motivo.SIN_RECLAMOS);
    }
}

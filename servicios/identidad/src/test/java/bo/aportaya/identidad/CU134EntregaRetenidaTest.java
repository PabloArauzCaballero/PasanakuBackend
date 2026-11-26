package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.CU134LiberarEntrega;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.MedidasDeCuentaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.UUID;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-134 · A-1 (Pablo, 2026-11-26) contra PostgreSQL real: una medida que llego a firme retiene la entrega aunque
 * venza y se cierre, hasta que un operador la libera con su segundo factor y un fundamento (R-SEG-17).
 */
class CU134EntregaRetenidaTest extends EscenarioDeCU134 {

    private static final String FUNDAMENTO = "Medida cumplida y sin deuda exigible pendiente";
    private static CU134LiberarEntrega liberacion;

    @BeforeAll
    static void armarLiberacion() {
        liberacion = new CU134LiberarEntrega(
                new Datos(dsl),
                new MedidasDeCuentaRepositorio(),
                new AccesosRepositorio(),
                (actor, evidencia) -> EVIDENCIA.equals(evidencia),
                new Outbox("identidad"),
                Reloj.delSistema());
    }

    @Test
    @DisplayName(
            "Dada una medida firme cuya suspensión ya venció · Cuando entregas consulta la medida vigente del participante · Entonces la entrega sigue retenida · Y deja de estarlo solo cuando un operador registra la liberación con su segundo factor y un fundamento")
    void firmeVencidaSigueRetenidaHastaLaLiberacion() {
        UUID participante = persona();
        UUID operador = persona();
        UUID id = firmeYCumplida(participante);
        assertThat(retiene(participante)).contains(id);

        assertThatThrownBy(() -> liberar(id, "sin-segundo-factor", operador))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-03"));
        assertThatThrownBy(() -> liberar(id, EVIDENCIA, participante))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-02"));
        assertThat(retiene(participante)).contains(id);

        liberar(id, EVIDENCIA, operador);
        assertThat(retiene(participante)).isEmpty();
        // Idempotente: liberar otra vez no emite un segundo evento.
        liberar(id, EVIDENCIA, operador);
        assertThat(dsl.fetchCount(
                        DSL.table("identidad.evento_dominio"),
                        DSL.field("tipo")
                                .eq("identidad.medida_cuenta_entrega_liberada")
                                .and(DSL.field("agregado_id").eq(id))))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un expediente que no llegó a firme · Cuando un operador intenta liberar la entrega · Entonces se rechaza con AP-CU134-12")
    void sinFirmezaNoHayQueLiberar() {
        UUID id = expedienteDecidido(persona(), persona(), persona());

        assertThatThrownBy(() -> liberar(id, EVIDENCIA, persona()))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-12"));
    }

    @Test
    @DisplayName("rechaza una liberacion sin quien ni fundamento, o antes de la firmeza (R-SEG-17)")
    void laBaseExigeLaLiberacionCompleta() {
        UUID sinFirmeza = expedienteDecidido(persona(), persona(), persona());
        UUID firme = firmeYCumplida(persona());

        assertThat(rechazaLaBase(
                        "UPDATE identidad.expediente_medida_cuenta SET entrega_liberada_en = now() WHERE id = ?",
                        firme))
                .contains("ck_expediente_medida_liberacion_completa");
        assertThat(rechazaLaBase(
                        """
                        UPDATE identidad.expediente_medida_cuenta
                        SET entrega_liberada_en = now(), entrega_liberada_por = propuesta_por,
                            fundamento_liberacion_entrega = 'Fundamento suficientemente largo'
                        WHERE id = ?
                        """,
                        sinFirmeza))
                .contains("ck_expediente_medida_liberacion_completa");
    }

    /** Decidida, firme al vencer la apelacion sin apelar, y cumplida al vencer la suspension. */
    private static UUID firmeYCumplida(UUID participante) {
        UUID id = expedienteDecidido(persona(), persona(), participante);
        var sistema = ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
        dsl.execute(
                "UPDATE identidad.expediente_medida_cuenta SET fecha_limite_apelacion = now() - interval '1 second' WHERE id = ?",
                id);
        transaccion.execute(e -> medidas.vencer(sistema));
        dsl.execute(
                "UPDATE identidad.expediente_medida_cuenta SET vigente_hasta = now() - interval '1 second' WHERE id = ?",
                id);
        transaccion.execute(e -> medidas.vencer(sistema));
        String estado = dsl.fetchValue("SELECT estado FROM identidad.expediente_medida_cuenta WHERE id = ?", id)
                .toString();
        assertThat(estado).isEqualTo("CUMPLIDA");
        return id;
    }

    private static java.util.Optional<UUID> retiene(UUID participante) {
        return transaccion.execute(e -> consulta.medidaQueRetiene(participante, como(persona())));
    }

    private static void liberar(UUID id, String evidencia, UUID por) {
        transaccion.execute(e -> liberacion.ejecutar(id, FUNDAMENTO, evidencia, como(por)));
    }
}

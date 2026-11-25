package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.infraestructura.MedidasDeCuentaRepositorio;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-134 contra PostgreSQL real: apelacion, vencimientos, prescripcion y las pruebas de rechazo de las
 * restricciones que el caso cita (R-SEG-13, R-SEG-14, R-AUD-01).
 */
class CU134ApelacionTest extends EscenarioDeCU134 {

    @Test
    @DisplayName(
            "Dado un expediente propuesto por un analista · Cuando el mismo analista intenta decidirlo · Entonces se"
                    + " rechaza con MISMA_PERSONA")
    void criterio9() {
        UUID analista = persona();
        UUID participante = persona();
        var vista = proponer(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false);
        // R-SEG-13: quien propone no decide; la base lo repite con ck_expediente_medida_decide_otro.
        assertThatThrownBy(() -> decidir(analista, vista.expediente().id(), "CONFIRMADA"))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-08"));
    }

    @Test
    @DisplayName(
            "Dada una propuesta de medida reenviada con la misma clave de idempotencia · Cuando llega por segunda vez"
                    + " · Entonces devuelve el mismo expedienteId y no se crea otro expediente")
    void criterio10() {
        UUID analista = persona();
        UUID participante = persona();
        String clave = UUID.randomUUID().toString();
        var primera =
                proponerConClave(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false, clave);
        var segunda =
                proponerConClave(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false, clave);

        assertThat(segunda.expediente().id()).isEqualTo(primera.expediente().id());
        assertThat(dsl.fetchCount(
                        DSL.table("identidad.expediente_medida_cuenta"),
                        DSL.field("usuario_id").eq(participante)))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dada una apelación que una tercera persona acepta · Cuando la resuelve · Entonces la medida queda"
            + " REVOCADA, la cuenta vuelve a su estado previo · Y se emite identidad.medida_cuenta_revocada sin"
            + " borrar el historial del expediente")
    void criterio11() {
        UUID analista = persona();
        UUID responsable = persona();
        UUID comite = persona();
        UUID participante = persona();
        var id = expedienteDecidido(analista, responsable, participante);
        transaccion.execute(e -> medidas.apelar(
                id, "No fui yo: adjunto la denuncia policial del robo del telefono.", como(participante)));

        var revocada = transaccion.execute(e -> medidas.resolver(
                id,
                "ACEPTADA",
                "La evidencia del afectado acredita el robo del telefono en la fecha de los hechos.",
                EVIDENCIA,
                como(comite)));

        assertThat(revocada.expediente().estado()).isEqualTo("REVOCADA");
        assertThat(estadoDe(participante)).isEqualTo("ACTIVO");
        // R-AUD-04: el evento sale en la misma transaccion; R-AUD-01: el historial no se borra.
        assertThat(dsl.fetchCount(
                        DSL.table("identidad.evento_dominio"),
                        DSL.field("tipo")
                                .eq("identidad.medida_cuenta_revocada")
                                .and(DSL.field("agregado_id").eq(id))))
                .isEqualTo(1);
        assertThat(revocada.lineaDeTiempo())
                .extracting(MedidasDeCuentaRepositorio.Hito::estado)
                .contains("CAUTELAR", "EN_DESCARGO", "DECIDIDA", "EN_APELACION", "REVOCADA");
    }

    @Test
    @DisplayName(
            "Dada una apelación recibida y pendiente de resolución · Cuando vencen los diez días hábiles sin decisión"
                    + " · Entonces la apelación queda ACEPTADA_POR_VENCIMIENTO y la medida se revierte · Y se emite una"
                    + " sola vez identidad.medida_cuenta_revocada")
    void resuelvePorVencimiento() {
        UUID participante = persona();
        UUID id = expedienteDecidido(persona(), persona(), participante);
        var vista =
                transaccion.execute(e -> medidas.apelar(id, "Apelo con los respaldos de acceso.", como(participante)));
        assertThat(vista.apelacion().orElseThrow().fechaLimiteResolucion()).isAfter(OffsetDateTime.now());
        dsl.execute(
                """
                UPDATE identidad.apelacion_medida_cuenta
                SET fecha_limite_resolucion = now() - interval '1 second'
                WHERE expediente_medida_cuenta_id = ?
                """,
                id);

        var sistema = ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
        transaccion.execute(e -> medidas.vencer(sistema));
        transaccion.execute(e -> medidas.vencer(sistema));

        var cerrada = transaccion
                .execute(e -> medidas.consultar(id, como(participante)))
                .orElseThrow();
        assertThat(cerrada.apelacion().orElseThrow().estado()).isEqualTo("ACEPTADA_POR_VENCIMIENTO");
        assertThat(cerrada.expediente().estado()).isEqualTo("REVOCADA");
        assertThat(estadoDe(participante)).isEqualTo("ACTIVO");
        assertThat(dsl.fetchCount(
                        DSL.table("identidad.evento_dominio"),
                        DSL.field("tipo")
                                .eq("identidad.medida_cuenta_revocada")
                                .and(DSL.field("agregado_id").eq(id))))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una cuenta suspendida con aportes pendientes en un grupo · Cuando se aplica la suspensión · Entonces"
                    + " ninguna obligación, entrega ni cupo del grupo cambia por la medida")
    void criterio12() {
        UUID analista = persona();
        UUID participante = persona();
        int antes = filasDeDinero();

        proponer(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", true);

        assertThat(estadoDe(participante)).isEqualTo("SUSPENDIDO");
        assertThat(filasDeDinero()).isEqualTo(antes);
    }

    @Test
    @DisplayName("rechaza un segundo expediente abierto por la misma causal (R-SEG-14)")
    void rechazaSegundoExpedienteAbierto() {
        UUID analista = persona();
        UUID participante = persona();
        proponer(analista, participante, "CUENTA_DUPLICADA_O_DE_TERCERO", "SUSPENSION_TEMPORAL", false);
        assertThatThrownBy(() ->
                        proponer(analista, participante, "CUENTA_DUPLICADA_O_DE_TERCERO", "SUSPENSION_TEMPORAL", false))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-06"));
    }

    @Test
    @DisplayName(
            "rechaza que decida o resuelva la apelacion quien ya actuo, y una segunda apelacion (R-SEG-13, R-SEG-15)")
    void rechazaSegregacionYApelacionDuplicada() {
        UUID analista = persona();
        UUID responsable = persona();
        UUID participante = persona();
        var id = expedienteDecidido(analista, responsable, participante);
        transaccion.execute(e ->
                medidas.apelar(id, "Pido revision: la evidencia es de otra cuenta, no de la mia.", como(participante)));

        assertThatThrownBy(() -> transaccion.execute(
                        e -> medidas.resolver(id, "RECHAZADA", "x".repeat(60), EVIDENCIA, como(responsable))))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-08"));
        assertThatThrownBy(() -> transaccion.execute(
                        e -> medidas.apelar(id, "Segunda apelacion que no corresponde.", como(participante))))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-09"));
        // R-SEG-15: la base no deja una decision sin fundamento.
        assertThat(rechazaLaBase(
                        "UPDATE identidad.expediente_medida_cuenta SET fundamento_decision = 'corto' WHERE id = ?", id))
                .contains("ck_expediente_medida_decision_fundada");
    }

    @Test
    @DisplayName("rechaza editar la evidencia o el historial del expediente (R-AUD-01)")
    void rechazaEditarElExpediente() {
        UUID analista = persona();
        UUID participante = persona();
        var id = proponer(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false)
                .expediente()
                .id();
        // Mismo criterio que R-GAR-02 (evidencia inmutable) y R-ORG-05 (una apelacion, por otro) en sus casos.
        assertThat(rechazaLaBase(
                        """
                        UPDATE identidad.evidencia_medida_cuenta SET contenido = 'otra'
                         WHERE expediente_medida_cuenta_id = ?
                        """,
                        id))
                .contains("R-AUD-01");
        assertThat(rechazaLaBase(
                        "DELETE FROM identidad.historial_estado_medida_cuenta WHERE expediente_medida_cuenta_id = ?",
                        id))
                .contains("R-AUD-01");
    }

    // R-SEG-03 (RLS) y R-SEG-10 (operador con TOTP) los aplica la plataforma antes de llegar al organismo:
    // la sesion de backoffice exige TOTP confirmado y cada lectura corre con datos.conContexto. R-REP-01 lo
    // garantiza CU-70 al consumir identidad.medida_cuenta_firme / _revocada, un evento por hecho.
}

package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.CU134ConsultarParticipante;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-134 contra PostgreSQL real: lectura justificada, propuesta, cautelar, descargo y decision. Un test por
 * criterio de aceptacion de la boveda, con el mismo nombre. La apelacion y las restricciones de la base
 * siguen en {@link CU134ApelacionTest}.
 */
class CU134Test extends EscenarioDeCU134 {

    @Test
    @DisplayName(
            "Dado un operador con permiso de lectura de participantes · Cuando abre la ficha de un participante con"
                    + " una justificación válida · Entonces ve el teléfono y el correo enmascarados · Y existe una fila en"
                    + " registro_acceso_datos con operacion LECTURA y su justificación")
    void criterio1() {
        UUID operador = persona();
        UUID participante = persona();
        dsl.execute("UPDATE identidad.usuario SET correo = 'juana@ejemplo.bo' WHERE id = ?", participante);

        var ficha = transaccion.execute(e -> consulta.ficha(
                participante, "Ticket de soporte por cobro duplicado", null, "10.0.0.1", como(operador)));

        assertThat(ficha).isPresent();
        assertThat(ficha.get().telefonoEnmascarado())
                .contains("•")
                .doesNotContain(telefonoDe(participante).substring(5));
        assertThat(ficha.get().correoEnmascarado()).isEqualTo("j•••@e•••.bo");
        // R-SEG-02: la lectura deja rastro con justificacion, en la misma transaccion.
        assertThat(dsl.fetchCount(
                        DSL.table("comun.registro_acceso_datos"),
                        DSL.field("usuario_afectado_id")
                                .eq(participante)
                                .and(DSL.field("operacion").eq("LECTURA"))
                                .and(DSL.field("justificacion").eq("Ticket de soporte por cobro duplicado"))))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dado un operador que busca participantes sin justificación · Cuando envía la búsqueda · Entonces se"
            + " rechaza con JUSTIFICACION_INSUFICIENTE y no se devuelve ninguna fila")
    void criterio2() {
        UUID operador = persona();
        assertThatThrownBy(() -> consulta.buscar(
                        new CU134ConsultarParticipante.Busqueda(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                "corta",
                                null,
                                Optional.empty(),
                                Optional.empty(),
                                20),
                        "10.0.0.1",
                        como(operador)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-01"));
    }

    @Test
    @DisplayName("Dado un operador sin segundo factor reciente · Cuando pide revelar el teléfono de un participante ·"
            + " Entonces se rechaza con SEGUNDO_FACTOR_REQUERIDO")
    void criterio3() {
        UUID operador = persona();
        UUID participante = persona();
        // R-SEG-12: sin evidencia de step-up no se lee el dato completo de un tercero.
        assertThatThrownBy(() -> consulta.revelar(
                        participante,
                        "TELEFONO",
                        "Verificar titularidad por llamada",
                        "sin-evidencia",
                        "10.0.0.1",
                        como(operador)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-03"));
    }

    @Test
    @DisplayName(
            "Dado un operador que también es participante · Cuando intenta abrir su propia ficha o proponer una medida"
                    + " sobre su cuenta · Entonces se rechaza con ACTUA_SOBRE_SI_MISMO")
    void criterio4() {
        UUID operador = persona();
        // R-SEG-04: nadie actua sobre si mismo.
        assertThatThrownBy(() ->
                        consulta.ficha(operador, "Revision de mi propia cuenta", null, "10.0.0.1", como(operador)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-02"));
        assertThatThrownBy(() -> proponer(operador, operador, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-02"));
    }

    @Test
    @DisplayName("Dado un participante con deuda vencida · Cuando un analista intenta suspender su cuenta por mora ·"
            + " Entonces se rechaza con CAUSAL_FUERA_DE_CATALOGO y se indica el caso de restricción por mora")
    void criterio5() {
        UUID analista = persona();
        UUID participante = persona();
        assertThatThrownBy(() -> proponer(analista, participante, "MORA", "SUSPENSION_TEMPORAL", false))
                .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> {
                    assertThat(e.codigo().valor()).isEqualTo("AP-CU134-04");
                    assertThat(e.getMessage()).contains("mora");
                });
    }

    @Test
    @DisplayName("Dada una propuesta de suspensión por abuso contra participantes · Cuando se la marca como cautelar ·"
            + " Entonces se rechaza con CAUTELAR_NO_ADMITIDA")
    void criterio6() {
        UUID analista = persona();
        UUID participante = persona();
        assertThatThrownBy(() ->
                        proponer(analista, participante, "ABUSO_CONTRA_PARTICIPANTES", "SUSPENSION_TEMPORAL", true))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-05"));
    }

    @Test
    @DisplayName(
            "Dada una cuenta comprometida con evidencia de sesiones anómalas · Cuando el analista propone un bloqueo"
                    + " cautelar · Entonces la cuenta queda BLOQUEADO_SEGURIDAD, sus sesiones quedan revocadas y el"
                    + " expediente queda CAUTELAR · Y al acreditarse la notificación se guarda la fecha límite de descargo"
                    + " y el expediente pasa a EN_DESCARGO")
    void criterio7() {
        UUID analista = persona();
        UUID participante = persona();
        fixtura.sesionAbierta(participante);

        var vista = proponer(analista, participante, "CUENTA_COMPROMETIDA", "BLOQUEO_CAUTELAR", true);

        assertThat(vista.expediente().estado()).isEqualTo("CAUTELAR");
        assertThat(estadoDe(participante)).isEqualTo("BLOQUEADO_SEGURIDAD");
        // R-SEG-09: ninguna sesion ni refresco sigue vivo tras el bloqueo.
        assertThat(dsl.fetchCount(
                        DSL.table("identidad.sesion"),
                        DSL.field("usuario_id")
                                .eq(participante)
                                .and(DSL.field("revocada_en").isNull())))
                .isZero();

        var notificada = transaccion.execute(
                e -> medidas.notificar(vista.expediente().id(), OffsetDateTime.now(), como(analista)));
        assertThat(notificada.expediente().estado()).isEqualTo("EN_DESCARGO");
        assertThat(notificada.expediente().fechaLimiteDescargo())
                .isAfter(notificada.expediente().notificadaEn());
    }

    @Test
    @DisplayName(
            "Dado un expediente con el plazo de descargo abierto y sin descargo · Cuando se intenta decidir · Entonces"
                    + " se rechaza con PLAZO_DESCARGO_ABIERTO")
    void criterio8() {
        UUID analista = persona();
        UUID responsable = persona();
        UUID participante = persona();
        var vista = proponer(analista, participante, "FRAUDE_CON_EVIDENCIA", "SUSPENSION_TEMPORAL", false);
        transaccion.execute(e -> medidas.notificar(vista.expediente().id(), OffsetDateTime.now(), como(analista)));

        assertThatThrownBy(() -> decidir(responsable, vista.expediente().id(), "CONFIRMADA"))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU134-07"));
    }
}

package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.dominio.AlcanceDeCuenta;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-09 §11: la recuperacion asistida a cuatro ojos, el enfriamiento y R-SEG-16, contra la base real. */
class CU09RecuperacionDeAccesoTest extends BaseDeCuentaDeOperador {

    @Test
    @DisplayName(
            "Dado un operador que pidió recuperar su clave · Cuando él mismo intenta aprobar la solicitud · Entonces"
                    + " se rechaza con APROBACION_REQUERIDA · Y la base lo rechaza aunque se evite la aplicación"
                    + " (R-SEG-16)")
    void criterio10() {
        UUID operador = operadorConClaveInutilizable("+59176100005");
        String correoDelOperador = correoDe(operador);

        transaccion.execute(e -> {
            recuperaciones.solicitar(correoDelOperador, "CLAVE", "Perdi mi clave", origen());
            return null;
        });
        UUID solicitud = solicitudDe(operador);

        assertThatThrownByAprobar(solicitud, comoOperador(operador));
        // R-SEG-16: aunque alguien se saltee la aplicacion, la base lo rechaza.
        assertThat(rechazaLaBase(
                        """
                        UPDATE identidad.solicitud_recuperacion_acceso
                        SET estado = 'APROBADA', decidida_por = '%s', decidida_en = now(),
                            motivo_decision = 'me apruebo yo', enfriamiento_hasta = now()
                        WHERE id = '%s'
                        """
                                .formatted(operador, solicitud)))
                .contains("ck_recuperacion_no_autoaprobada");
        assertThat(estadoDeLaSolicitud(solicitud)).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName(
            "Dado una solicitud aprobada con enfriamiento vigente · Cuando corre el trabajo de emisión · Entonces no"
                    + " se emite ninguna invitación · Y pasado el enfriamiento se emite una sola, aunque el trabajo corra"
                    + " dos veces")
    void criterio11() {
        UUID operador = operadorConClaveInutilizable("+59176100006");
        UUID aprobador = operadorConClaveInutilizable("+59176100007");
        transaccion.execute(e -> {
            recuperaciones.solicitar(correoDe(operador), "CLAVE", "Perdi mi clave", origen());
            return null;
        });
        UUID solicitud = solicitudDe(operador);
        transaccion.execute(e -> recuperaciones.aprobar(
                solicitud, "Verificado en persona con documento", "evidencia-sintetica", comoOperador(aprobador)));

        assertThat(correrElTrabajo()).isZero();
        assertThat(correo.enlaces).isEmpty();

        dsl.execute(
                """
                UPDATE identidad.solicitud_recuperacion_acceso
                SET enfriamiento_hasta = now() - interval '1 minute' WHERE id = ?
                """,
                solicitud);
        assertThat(correrElTrabajo()).isEqualTo(1);
        assertThat(correrElTrabajo()).isZero();
        assertThat(correo.enlaces).hasSize(1);
        assertThat(correo.alcances).containsExactly(AlcanceDeCuenta.CLAVE);
        assertThat(estadoDeLaSolicitud(solicitud)).isEqualTo("EMITIDA");
    }

    @Test
    @DisplayName("rechaza por R-SEG-16")
    void rechazaRSEG13() {
        UUID operador = operadorConClaveInutilizable("+59176100009");
        UUID solicitud = abrirSolicitud(operador);

        // Aprobada sin aprobador, sin fecha ni enfriamiento: la base no la acepta.
        assertThat(rechazaLaBase(
                        "UPDATE identidad.solicitud_recuperacion_acceso SET estado = 'APROBADA' WHERE id = '%s'"
                                .formatted(solicitud)))
                .contains("ck_recuperacion_decision_completa");
        // Ni una segunda solicitud viva del mismo operador y alcance.
        assertThat(rechazaLaBase(
                        """
                        INSERT INTO identidad.solicitud_recuperacion_acceso
                            (id, usuario_id, alcance, estado, motivo, clave_idempotencia)
                        VALUES (gen_random_uuid(), '%s', 'CLAVE', 'PENDIENTE', 'otra', 'REC:duplicada')
                        """
                                .formatted(operador)))
                .contains("uq_recuperacion_viva");
    }

    @Test
    @DisplayName("rechaza revelar si el correo existe: el pedido no deja rastro de un correo desconocido")
    void rechazaRevelarElCorreo() {
        int antes = dsl.fetchCount(org.jooq.impl.DSL.table("identidad.solicitud_recuperacion_acceso"));

        // No lanza, no responde distinto y no deja rastro: nadie puede usarlo para averiguar quien es operador.
        transaccion.execute(e -> {
            recuperaciones.solicitar("nadie@ejemplo.test", "CLAVE", null, origen());
            return null;
        });

        assertThat(dsl.fetchCount(org.jooq.impl.DSL.table("identidad.solicitud_recuperacion_acceso")))
                .isEqualTo(antes);
        assertThat(correo.enlaces).isEmpty();
    }

    @Test
    @DisplayName("reintento del pedido vivo no duplica la solicitud")
    void reintentoDelPedido() {
        UUID operador = operadorConClaveInutilizable("+59176100012");
        String correoDelOperador = correoDe(operador);

        for (int i = 0; i < 2; i++) {
            transaccion.execute(e -> {
                recuperaciones.solicitar(correoDelOperador, "FACTOR", "Perdi el telefono", origen());
                return null;
            });
        }

        assertThat(dsl.fetchCount(
                        org.jooq.impl.DSL.table("identidad.solicitud_recuperacion_acceso"),
                        org.jooq.impl.DSL.field("usuario_id").eq(operador)))
                .isEqualTo(1);
    }
}

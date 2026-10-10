package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.Resultado;
import bo.aportaya.garantia.aplicacion.CU23CubrirFaltanteDelCorte;
import bo.aportaya.garantia.aplicacion.CU23ReservarRespaldo.EntradaReserva;
import bo.aportaya.garantia.dominio.puertos.RecaudoDelPeriodo;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H8.S1.M6 · SIMULACRO de una falta extraordinaria de caja, contra PostgreSQL real.
 *
 * <p>Respaldo insuficiente sobre un compromiso que ya existe: se abre el incidente, la deuda se conserva y se
 * ejecuta la contencion. El simulacro es verificable porque deja rastro que se puede leer: quien es el
 * responsable, que obligaciones faltan y por cuanto, que se le dice al grupo y que decisiones quedan para una
 * persona. Los roles a los que se escala son nombres del catalogo, no personas inventadas; el tope de capacidad
 * y toda cifra son SINTETICOS (ambiguedad A2 del plan del carril C).
 */
class CU133ContingenciaTest extends BaseDeRespaldo {

    private static final ObjectMapper JSON = new ObjectMapper();

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private JsonNode eventoDe(String tipo, UUID grupo) throws Exception {
        var fila = dsl.fetchOne(
                "SELECT payload::text FROM garantia.evento_dominio WHERE tipo = ? AND payload->>'grupoId' = ?",
                tipo,
                grupo.toString());
        return fila == null ? null : JSON.readTree(fila.get(0, String.class));
    }

    private int eventos(String tipo, UUID grupo) {
        return contar(
                "SELECT count(*)::int FROM garantia.evento_dominio WHERE tipo = ? AND payload->>'grupoId' = ?",
                tipo,
                grupo.toString());
    }

    private String estadoDelPlan(UUID grupo) {
        var f = dsl.fetchOne(
                "SELECT estado FROM garantia.plan_contingencia WHERE grupo_id = ? AND disparador = 'FONDO_AGOTADO'",
                grupo);
        return f == null ? null : f.get(0, String.class);
    }

    private boolean puedeReservarOtroGrupo() {
        var otro = caso();
        try {
            reservar(otro, "100.00");
            return true;
        } catch (ErrorDeNegocio e) {
            assertThat(e.codigo().valor()).isEqualTo("AP-CU23-10");
            return false;
        }
    }

    @Test
    @DisplayName(
            "SIMULACRO · Dado respaldo insuficiente sobre un compromiso existente · Cuando se detecta · Entonces abre incidente, conserva la deuda, suspende reservas nuevas, escala con responsables y obligaciones, y comunica al grupo; al reponer el respaldo se cierra")
    void simulacroDeFaltaExtraordinaria() throws Exception {
        fixturaDeRespaldo.capacidad("50000.00");
        var c = caso();
        var reserva = reservar(c, "800.00");
        var pedido = pedidoDelEjemplo(c, "cobertura-" + c.turno());

        // 1 · Se detecta: faltan 1.000, hay 800.
        var salida = cubrir(c, pedido);

        assertThat(salida.resultado()).isEqualTo(Resultado.INSUFICIENTE);
        // 2 · Incidente (el de siempre, para quien ya lo escuchaba) y deuda conservada: nada se cubrio ni se movio.
        assertThat(eventos("garantia.respaldo_insuficiente", c.escenario().grupoId()))
                .isEqualTo(1);
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("0");
        assertThat(contar("SELECT count(*)::int FROM garantia.cobertura_respaldo WHERE turno_id = ?", c.turno()))
                .isZero();

        // 3 · Contencion registrada en el plan de contingencia del grupo.
        assertThat(estadoDelPlan(c.escenario().grupoId())).isEqualTo("APROBADO");
        var plan = dsl.fetchOne(
                "SELECT tipo, requiere_acuerdo, impacto_estimado, descripcion FROM garantia.plan_contingencia WHERE grupo_id = ?",
                c.escenario().grupoId());
        assertThat(plan.get("requiere_acuerdo", Boolean.class)).isFalse();
        assertThat(plan.get("impacto_estimado", BigDecimal.class)).isEqualByComparingTo("200.00");
        assertThat(plan.get("descripcion", String.class))
                .contains("faltan 1000.00")
                .contains("se conserva");

        // 4 · Escalamiento VERIFICABLE: responsables, obligaciones y comunicacion salen en el evento.
        JsonNode e =
                eventoDe("garantia.contingencia_de_caja_activada", c.escenario().grupoId());
        assertThat(e).isNotNull();
        assertThat(e.get("responsableDeLaReserva").asText())
                .isEqualTo(c.usuario().toString());
        assertThat(e.get("rolesAEscalar"))
                .extracting(JsonNode::asText)
                .containsExactly("TESORERIA", "RESPONSABLE_RIESGOS");
        assertThat(e.get("medidas"))
                .extracting(JsonNode::asText)
                .contains("CONSERVAR_DEUDA", "ENTREGA_EN_ESPERA", "SUSPENDER_NUEVAS_RESERVAS", "COMUNICAR_AL_GRUPO");
        assertThat(e.get("obligaciones")).hasSize(2);
        assertThat(e.get("obligaciones"))
                .extracting(o -> o.get("obligacionId").asText())
                .containsExactlyInAnyOrder(
                        c.obligacionA().toString(), c.obligacionB().toString());
        BigDecimal suma = BigDecimal.ZERO;
        for (JsonNode o : e.get("obligaciones")) {
            suma = suma.add(new BigDecimal(o.get("monto").asText().replace("BOB ", "")));
        }
        assertThat(suma).isEqualByComparingTo("1000.00");
        assertThat(e.get("sinCubrir").asText()).contains("200.00");
        // La comunicacion dice hechos: cuanto falta y que se conserva; ni un porcentaje ni una fecha prometida.
        String mensaje = e.get("comunicacionAlGrupo").asText();
        assertThat(mensaje).contains("1000.00").contains("deuda se conserva").contains("en espera");
        assertThat(mensaje).doesNotContain("%").doesNotContainIgnoringCase("probab");

        // 5 · Reintento ANTES de reponer: sigue una sola contencion y un solo escalamiento.
        assertThat(cubrir(c, pedidoDelEjemplo(c, "cobertura-otra-" + c.turno())).resultado())
                .isEqualTo(Resultado.INSUFICIENTE);
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.plan_contingencia WHERE grupo_id = ?",
                        c.escenario().grupoId()))
                .isEqualTo(1);
        assertThat(eventos(
                        "garantia.contingencia_de_caja_activada", c.escenario().grupoId()))
                .isEqualTo(1);

        // 6 · Nuevos grupos: ninguna reserva nueva mientras dure. La repeticion de una reserva ya hecha sigue siendo
        // idempotente.
        assertThat(puedeReservarOtroGrupo()).isFalse();
        var misma = reserva.reservaId();
        assertThat(transaccion
                        .execute(t -> reservaCU.reservar(
                                new EntradaReserva(
                                        c.escenario().grupoId(),
                                        1,
                                        bob("800.00"),
                                        c.usuario(),
                                        claveDeReserva(reserva)),
                                c.ctx()))
                        .reservaId())
                .isEqualTo(misma);

        // 7 · La persona repone el respaldo (ampliar NO esta bloqueado) y el reintento cubre una sola vez.
        transaccion.execute(t -> reservaCU.ampliar(reserva.reservaId(), bob("200.00"), clave("ampliar"), c.ctx()));
        var cubierto = cubrir(c, pedido);

        assertThat(cubierto.resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(estadoDelPlan(c.escenario().grupoId())).isEqualTo("EJECUTADO");
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.plan_contingencia WHERE grupo_id = ? AND ejecutado_en IS NOT NULL",
                        c.escenario().grupoId()))
                .isEqualTo(1);
        assertThat(eventos(
                        "garantia.contingencia_de_caja_resuelta", c.escenario().grupoId()))
                .isEqualTo(1);
        assertThat(puedeReservarOtroGrupo()).isTrue();
    }

    /** La clave con la que reservar() creo la reserva, leida de la fila (idempotencia: misma clave, misma reserva). */
    private ClaveIdempotencia claveDeReserva(bo.aportaya.garantia.aplicacion.CU23ReservarRespaldo.SalidaReserva r) {
        return new ClaveIdempotencia(
                dsl.fetchOne("SELECT clave_idempotencia FROM garantia.reserva_respaldo WHERE id = ?", r.reservaId())
                        .get(0, String.class));
    }

    @Test
    @DisplayName(
            "LIMITE · Dado un grupo SIN reserva · Cuando se pide cubrir · Entonces tambien hay contingencia (compromiso sin respaldo)")
    void sinReservaTambienEscala() throws Exception {
        fixturaDeRespaldo.capacidad("50000.00");
        var c = caso();

        var salida = cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));

        assertThat(salida.resultado()).isEqualTo(Resultado.SIN_RESERVA);
        assertThat(estadoDelPlan(c.escenario().grupoId())).isEqualTo("APROBADO");
        JsonNode e =
                eventoDe("garantia.contingencia_de_caja_activada", c.escenario().grupoId());
        assertThat(e.get("motivo").asText()).isEqualTo("SIN_RESERVA");
        assertThat(e.get("responsableDeLaReserva").asText()).isEmpty();
        assertThat(e.get("obligaciones")).hasSize(2);
    }

    @Test
    @DisplayName(
            "LIMITE · Dado un faltante que desaparece porque llegaron pagos tardios · Cuando aportes confirma faltante cero · Entonces la contingencia se cierra aunque nadie cubra nada")
    void elFaltanteDesapareceSinCobertura() {
        fixturaDeRespaldo.capacidad("50000.00");
        var c = caso();
        reservar(c, "800.00");
        cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));
        assertThat(estadoDelPlan(c.escenario().grupoId())).isEqualTo("APROBADO");
        var sinFaltante = new RecaudoDelPeriodo.Recaudo(
                c.escenario().periodoId(),
                c.escenario().grupoId(),
                OffsetDateTime.now(),
                bob("6000.00"),
                bob("6000.00"),
                bob("0.00"),
                List.of());
        // Spring abre la transaccion de cerrarContingencia en el proxy; aca se arma a mano, como hace BaseDeEntregas:
        // el cierre se confirma por su cuenta y RECIEN DESPUES se rechaza el pedido, igual que en el servicio real.
        var conTransaccionPropia =
                new bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo(
                        new bo.aportaya.plataforma.datos.Datos(dsl),
                        new bo.aportaya.garantia.infraestructura.RespaldoRepositorio(),
                        new bo.aportaya.garantia.infraestructura.CoberturaRespaldoRepositorio(),
                        new bo.aportaya.plataforma.mensajeria.Outbox("garantia"),
                        bo.aportaya.plataforma.dominio.Reloj.delSistema(),
                        contingencia) {
                    @Override
                    public boolean cerrarContingencia(
                            UUID grupoId, UUID turnoId, bo.aportaya.plataforma.dominio.ContextoSesion ctx) {
                        return transaccion.execute(x -> super.cerrarContingencia(grupoId, turnoId, ctx));
                    }
                };
        var servicio = new CU23CubrirFaltanteDelCorte(p -> Optional.of(sinFaltante), conTransaccionPropia);

        assertThatThrownBy(() -> servicio.cubrir(
                        new CU23CubrirFaltanteDelCorte.Pedido(
                                c.escenario().grupoId(),
                                c.escenario().periodoId(),
                                c.turno(),
                                null,
                                clave("sin-faltante")),
                        c.ctx()))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU23-09"));

        assertThat(estadoDelPlan(c.escenario().grupoId())).isEqualTo("EJECUTADO");
    }
}

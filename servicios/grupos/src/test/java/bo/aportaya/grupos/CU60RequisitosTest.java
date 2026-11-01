package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU60Sortear.Compromiso;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-60 · Los requisitos previos al compromiso del sorteo: la cuota 1 cobrada en cada cupo (R-GRP-20, con
 * {@code aportes} respondiendo o caido) y la firma de la version VIGENTE del reglamento. El resto del caso vive en
 * {@link CU60Test}; los dos comparten {@link BaseDeCU60} y su doble de {@code aportes}.
 */
class CU60RequisitosTest extends BaseDeCU60 {

    /** R-GRP-20 · AP-CU60-11. */
    @Test
    @DisplayName(
            "Dado un grupo conformado con un cupo que no pagó la cuota 1 · Cuando se intenta comprometer el sorteo · Entonces se rechaza con AP-CU60-11 y el detalle nombra el cupo pendiente · Y no existe sorteo")
    void rechazaSinPrimeraCuota() {
        UUID grupo = fixtura.grupoConformado(6);
        fixtura.cuposOcupados(grupo, 6);
        UUID cupoDos = dsl.fetchOne("SELECT id FROM grupos.cupo WHERE grupo_id = ? AND numero = 2", grupo)
                .get(0, UUID.class);
        aportes.sinPagar.add(cupoDos);

        assertThatThrownBy(() ->
                        transaccion.execute(e -> sortear.comprometer(grupo, List.of(), Optional.empty(), contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(error -> {
                    var negocio = (ErrorDeNegocio) error;
                    assertThat(negocio.codigo().toString()).isEqualTo("AP-CU60-11");
                    assertThat(negocio.detalle()).containsEntry("cuposPendientes", List.of(2));
                });
        assertThat(dsl.fetchCount(
                        DSL.table("grupos.sorteo_turnos"), DSL.field("grupo_id").eq(grupo)))
                .isZero();
    }

    /** R-GRP-20 · AP-CU60-12: fallo cerrado. */
    @Test
    @DisplayName(
            "Dado que aportes no responde · Cuando se intenta comprometer el sorteo · Entonces se rechaza con AP-CU60-12: sin confirmar la plata no se sortea")
    void rechazaSinRespuestaDeAportes() {
        UUID grupo = fixtura.grupoConformado(6);
        fixtura.cuposOcupados(grupo, 6);
        aportes.caido = true;

        assertThatThrownBy(() ->
                        transaccion.execute(e -> sortear.comprometer(grupo, List.of(), Optional.empty(), contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(error ->
                        assertThat(((ErrorDeNegocio) error).codigo().toString()).isEqualTo("AP-CU60-12"));
        assertThat(dsl.fetchCount(
                        DSL.table("grupos.sorteo_turnos"), DSL.field("grupo_id").eq(grupo)))
                .isZero();
    }

    /** Un reglamento del grupo, firmado por todos sus participantes; devuelve el id de la version. */
    private UUID reglamentoFirmadoPor(UUID grupo, List<UUID> participantes) {
        UUID reglamento = UUID.randomUUID();
        dsl.execute(
                """
                INSERT INTO grupos.reglamento_grupo
                    (id, grupo_id, version, contenido, hash_contenido, clausulas_mora, clausulas_abandono,
                     vigente_desde, redactado_por)
                VALUES (?, ?, 1, 'reglas de prueba', repeat('a', 64), 'mora', 'abandono', now(), ?)
                """,
                reglamento,
                grupo,
                organizador);
        for (UUID participante : participantes) {
            dsl.execute(
                    """
                    INSERT INTO grupos.aceptacion_reglamento
                        (reglamento_id, participante_id, aceptado_en, hash_firmado, ip_origen)
                    VALUES (?, ?, now(), repeat('b', 64), '127.0.0.1'::inet)
                    """,
                    reglamento,
                    participante);
        }
        return reglamento;
    }

    @Test
    @DisplayName(
            "Dado un grupo cuyos tres participantes firmaron la versión vigente del reglamento · Cuando se compromete el sorteo · Entonces procede: la firma vigente alcanza")
    void aceptaFirmaDeLaVersionVigente() {
        UUID grupo = fixtura.grupoConformado(3);
        List<UUID> participantes = fixtura.participantesConCupo(grupo, 3);
        reglamentoFirmadoPor(grupo, participantes);

        Compromiso compromiso =
                transaccion.execute(e -> sortear.comprometer(grupo, List.of(), Optional.empty(), contexto()));

        assertThat(estadoDelSorteo(compromiso.sorteoId())).isEqualTo("COMPROMETIDO");
    }

    @Test
    @DisplayName(
            "Dado un grupo cuyo reglamento firmado fue reemplazado por una versión nueva que nadie firmó · Cuando se intenta comprometer el sorteo · Entonces se rechaza con AP-CU60-05 y nombra los cupos sin firma: la firma de la versión anterior no vale")
    void rechazaFirmaDeVersionAnterior() {
        UUID grupo = fixtura.grupoConformado(3);
        List<UUID> participantes = fixtura.participantesConCupo(grupo, 3);
        reglamentoFirmadoPor(grupo, participantes);
        // Al iniciar el grupo se publica la version 2 y la 1 deja de regir: nadie firmo la nueva.
        dsl.execute("UPDATE grupos.reglamento_grupo SET vigente_hasta = now() WHERE grupo_id = ?", grupo);
        dsl.execute(
                """
                INSERT INTO grupos.reglamento_grupo
                    (grupo_id, version, contenido, hash_contenido, clausulas_mora, clausulas_abandono,
                     vigente_desde, redactado_por)
                VALUES (?, 2, 'reglas con el plantel real', repeat('c', 64), 'mora', 'abandono', now(), ?)
                """,
                grupo,
                organizador);

        assertThatThrownBy(() ->
                        transaccion.execute(e -> sortear.comprometer(grupo, List.of(), Optional.empty(), contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(error -> {
                    var negocio = (ErrorDeNegocio) error;
                    assertThat(negocio.codigo().toString()).isEqualTo("AP-CU60-05");
                    assertThat(negocio.detalle()).containsEntry("cuposSinFirma", List.of(1, 2, 3));
                });
    }
}

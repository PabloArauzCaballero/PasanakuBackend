package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El tope de participaciones (decision del dueño 2026-10-20) sobre PostgreSQL real: cuentan los grupos vivos en los
 * que la persona participa sin organizarlos. Los que organiza, los terminados y aquellos de los que salio no cuentan.
 */
class CU69LimiteDeParticipacionesTest extends BaseDeCU60 {

    private UUID participarEn(UUID usuario, String estadoDelGrupo, boolean organiza, String estado) {
        UUID grupo = fixtura.grupoConformado(4);
        dsl.execute("UPDATE grupos.grupo SET estado = ? WHERE id = ?", estadoDelGrupo, grupo);
        dsl.execute(
                """
                INSERT INTO grupos.participante
                  (id, grupo_id, usuario_id, estado, es_organizador, fecha_ingreso,
                   reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, now(), 0, 0, 0)
                """,
                grupo,
                usuario,
                estado,
                organiza);
        return grupo;
    }

    @Test
    @DisplayName("Dada una persona que ya participa en el maximo de grupos vivos · Cuando intenta entrar a otro · "
            + "Entonces recibe AP-CU69-15")
    void alTopeNoEntra() {
        UUID persona = fixtura.usuario();
        participarEn(persona, "ABIERTO_A_INSCRIPCION", false, "ACEPTADO_PENDIENTE_FIRMA");
        participarEn(persona, "EN_CURSO", false, "ACTIVO");
        var conTopeDeDos = new EmparejamientoRepositorio(2);

        assertThat(conTopeDeDos.participacionesVivas(dsl, persona)).isEqualTo(2);
        assertThatThrownBy(() -> conTopeDeDos.exigirLugarParaParticipar(dsl, persona, "tope"))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-15"));
    }

    @Test
    @DisplayName("Dada una persona que organiza grupos o ya salio de otros · Cuando se cuentan sus participaciones · "
            + "Entonces solo cuentan los grupos vivos donde participa sin organizar")
    void soloCuentanLasVivasSinOrganizar() {
        UUID persona = fixtura.usuario();
        participarEn(persona, "ABIERTO_A_INSCRIPCION", true, "ACTIVO");
        participarEn(persona, "FINALIZADO", false, "ACTIVO");
        participarEn(persona, "EN_CURSO", false, "RETIRADO");
        participarEn(persona, "EN_CURSO", false, "ACTIVO");
        var conTopeDeDos = new EmparejamientoRepositorio(2);

        assertThat(conTopeDeDos.participacionesVivas(dsl, persona)).isEqualTo(1);
        conTopeDeDos.exigirLugarParaParticipar(dsl, persona, "tope");
    }
}

package bo.aportaya.identidad;

import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.USUARIO_INTERNO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** GU-17 / R-SEG-18: solo el personal interno puede estar «sin dato» de fecha de nacimiento; lo verifica la base. */
class CU08FechaDeNacimientoDeLaBaseTest extends BaseDeGobiernoCU08 {

    private static UUID usuarioSinFecha() {
        UUID id = UUID.randomUUID();
        dsl.insertInto(USUARIO)
                .set(USUARIO.ID, id)
                .set(
                        USUARIO.CODIGO_PUBLICO,
                        "T-" + id.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT))
                .set(USUARIO.NOMBRES, "Prueba")
                .set(USUARIO.APELLIDOS, "Sintetica")
                .set(
                        USUARIO.TELEFONO_E164,
                        "+5917" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999))
                .set(USUARIO.ESTADO, "ACTIVO")
                .set(USUARIO.NIVEL_KYC, "NINGUNO")
                .set(USUARIO.IDIOMA, "ES_BO")
                .set(USUARIO.ZONA_HORARIA, "America/La_Paz")
                .set(USUARIO.FECHA_REGISTRO, OffsetDateTime.now(ZoneOffset.UTC))
                .set(USUARIO.VERSION, 1)
                .execute();
        return id;
    }

    @Test
    @DisplayName(
            "Dado un usuario que no es personal interno · Cuando se intenta guardarlo sin fecha de nacimiento · Entonces la base lo rechaza (R-SEG-18)")
    void criterioParticipanteSinFecha() {
        enTransaccionQueSeDeshace(() -> {
            usuarioSinFecha();
            assertThatThrownBy(() -> dsl.execute("SET CONSTRAINTS tg_fecha_nacimiento_obligatoria IMMEDIATE"))
                    .hasMessageContaining("R-SEG-18");
        });
    }

    @Test
    @DisplayName(
            "Dado un operador del personal interno · Cuando se lo guarda sin fecha de nacimiento · Entonces se acepta como sin dato")
    void criterioOperadorSinFecha() {
        enTransaccionQueSeDeshace(() -> {
            UUID id = usuarioSinFecha();
            dsl.insertInto(USUARIO_INTERNO)
                    .set(USUARIO_INTERNO.ID, UUID.randomUUID())
                    .set(USUARIO_INTERNO.USUARIO_ID, id)
                    .set(USUARIO_INTERNO.DEPARTAMENTO, "Sistemas")
                    .set(USUARIO_INTERNO.CARGO, "Operador de pruebas")
                    .set(USUARIO_INTERNO.DEBE_CAMBIAR_CONTRASENA, false)
                    .execute();
            assertThatCode(() -> dsl.execute("SET CONSTRAINTS tg_fecha_nacimiento_obligatoria IMMEDIATE"))
                    .doesNotThrowAnyException();
            assertThat(dsl.select(USUARIO.FECHA_NACIMIENTO)
                            .from(USUARIO)
                            .where(USUARIO.ID.eq(id))
                            .fetchOne(USUARIO.FECHA_NACIMIENTO))
                    .isNull();
        });
    }
}

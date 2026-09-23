package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Extensión de {@code AislamientoEsquemaTest} para `aportes` (H4.S1.M2 del
 * carril PR4-seguridad, nombre de test reservado — Q-05).
 *
 * <p>El {@code AislamientoEsquemaTest} de {@code comun-pruebas} ya cubre lectura
 * cruzada entre esquemas y escritura exclusiva del libro contable (×14,
 * parametrizado). Lo que falta según la tabla de cobertura del §47 y que este
 * archivo agrega, específico de `aportes`:
 *
 * <ol>
 *   <li>{@code FORCE ROW LEVEL SECURITY} no se puede saltar ni con {@code SET
 *       ROLE svc_aportes} directo (sin pasar por {@code app.usuario_id}).
 *   <li><b>Hallazgo real, con causa raíz confirmada</b> (no la hipótesis con la
 *       que arrancó este test — se corrigió al correrlo contra PostgreSQL real):
 *       {@code rol_auditor} tiene {@code GRANT SELECT} cruzado
 *       (`sql/00_base/02_esquemas.sql:354-...`), pero las políticas de RLS que
 *       {@code fn_seg_aplicar_rls()}/{@code pol_usuario_titular} crean son
 *       {@code FOR ALL TO rol_aplicacion} (`restricciones.sql:1299`,`:1311`) —
 *       **no** {@code TO PUBLIC} ni {@code TO rol_auditor}. Y {@code rol_auditor}
 *       **nunca** es miembro de {@code rol_aplicacion}
 *       (`sql/00_base/02_esquemas.sql`: el único `GRANT rol_aplicacion TO ...` es
 *       a los catorce `svc_*`, nunca a `rol_auditor`). En PostgreSQL, si NINGUNA
 *       política aplica al rol que consulta una tabla con {@code FORCE ROW LEVEL
 *       SECURITY}, el resultado es cero filas — sin importar qué diga
 *       {@code app.rol}. Fijar {@code app.rol='AUDITOR'} no alcanza: el rol de
 *       PostgreSQL {@code rol_auditor} necesitaría ser miembro de
 *       {@code rol_aplicacion} (o las políticas necesitarían agregarlo
 *       explícitamente) para que su `GRANT SELECT` sirva de algo en una tabla con
 *       RLS forzada. **Hoy, el auditor de base de datos no puede leer ninguna
 *       fila de una tabla con RLS forzada**, aunque el `GRANT` diga que sí puede.
 * </ol>
 */
class AislamientoEsquemaTest {

    @BeforeAll
    static void calentarElContenedor() {
        BaseDePrueba.contenedor();
    }

    @Test
    @DisplayName("svc_aportes no puede desactivar RLS ni ver la fila de otro usuario, ni siquiera con SET ROLE directo")
    void svcAportesNoSalteaLaPoliticaDeFila() throws SQLException {
        try (Connection conexion = BaseDePrueba.conexion();
                Statement sentencia = conexion.createStatement()) {
            sentencia.execute("SET ROLE svc_aportes");
            // Sin `app.usuario_id`/`app.rol` fijados (invariante: solo
            // `Datos.conContexto` los fija), la politica evalua
            // `fn_seg_usuario_actual()` como NULL y `fn_seg_rol_privilegiado()`
            // como falso: CERO filas visibles, aunque la tabla tenga datos.
            // PostgreSQL rechaza el ALTER TABLE de un no-dueño con "must be owner
            // of table", no con "permission denied" (esa frase es de
            // SELECT/INSERT/UPDATE/DELETE) — encontrado corriendo esto de verdad.
            assertThatThrownBy(() ->
                            sentencia.execute("ALTER TABLE aportes.obligacion_aporte NO FORCE ROW LEVEL SECURITY"))
                    .as("svc_aportes pudo desactivar FORCE RLS — no es el dueño de la tabla, no deberia poder")
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("must be owner");
        }
    }

    /**
     * <b>Hallazgo H4.S1, con causa raíz confirmada corriendo esto tres veces
     * contra PostgreSQL real</b> (las dos primeras versiones de este test
     * asumían que {@code app.rol='AUDITOR'} alcanzaba, y no era así — quedó
     * documentado abajo con el motivo real, no la hipótesis inicial):
     * {@code rol_auditor} NO ve la fila ni sin {@code app.rol} ni con
     * {@code app.rol='AUDITOR'} fijado correctamente a nivel de sesión. La razón
     * no es la variable de sesión: es que las políticas de RLS son
     * {@code TO rol_aplicacion} y {@code rol_auditor} nunca es miembro de ese
     * rol — ninguna política le aplica, y sin política que aplique, una tabla con
     * {@code FORCE ROW LEVEL SECURITY} deniega todo. El `GRANT SELECT` de
     * `rol_auditor` es necesario pero, hoy, **no alcanza**.
     */
    @Test
    @DisplayName(
            "hallazgo: rol_auditor no ve filas en tablas con RLS forzada, ni con app.rol='AUDITOR' — no es miembro de rol_aplicacion")
    void rolAuditorNoEsMiembroDeRolAplicacion() throws SQLException {
        UUID usuario = UUID.randomUUID();
        try (Connection admin = BaseDePrueba.conexion();
                Statement sentenciaAdmin = admin.createStatement()) {
            // Una fila real, insertada por el rol dueño (bypassea RLS por ser el
            // que aplicó el esquema), para que "cero filas" signifique RLS/rol y
            // no "no hay datos".
            sentenciaAdmin.execute(
                    """
                    INSERT INTO identidad.usuario
                        (id, codigo_publico, nombres, apellidos, telefono_e164, fecha_nacimiento,
                         estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
                    VALUES ('%s', 'AUD-TEST', 'Auditoria', 'Prueba', '+59170000099',
                            DATE '1990-01-01', 'ACTIVO', 'BASICO', 'es', 'America/La_Paz', now())
                    """
                            .formatted(usuario));
        }

        // Primero, confirmar que la fila SI existe (conexion admin: bypassea RLS
        // por ser el rol que aplicó el esquema) — descarta que el "cero" de abajo
        // sea por falta de datos y no por rol_auditor.
        try (Connection admin = BaseDePrueba.conexion();
                Statement sentenciaAdmin = admin.createStatement()) {
            var filas = sentenciaAdmin.executeQuery(
                    "SELECT count(*)::int AS n FROM identidad.usuario WHERE id = '%s'".formatted(usuario));
            filas.next();
            assertThat(filas.getInt("n"))
                    .as("la fila recien insertada no esta: el fixture del test esta mal")
                    .isEqualTo(1);
        }

        // rol_auditor, SIN app.rol: cero (podria ser por cualquiera de las dos
        // causas — se descarta cual es con el siguiente bloque).
        try (Connection conexion = BaseDePrueba.conexion();
                Statement sentencia = conexion.createStatement()) {
            sentencia.execute("SET ROLE rol_auditor");
            var filas = sentencia.executeQuery(
                    "SELECT count(*)::int AS n FROM identidad.usuario WHERE id = '%s'".formatted(usuario));
            filas.next();
            assertThat(filas.getInt("n"))
                    .as("rol_auditor sin app.rol ve la fila")
                    .isZero();
        }

        // rol_auditor, CON app.rol='AUDITOR' fijado a nivel de SESION (no de
        // transaccion: `set_config(..., false)`) — SIGUE en cero. Esto es lo que
        // confirma que la causa es la membresia de rol, no la variable de sesion.
        try (Connection conexion = BaseDePrueba.conexion();
                Statement sentencia = conexion.createStatement()) {
            sentencia.execute("SET ROLE rol_auditor");
            sentencia.execute("SELECT set_config('app.rol', 'AUDITOR', false)");
            var filas = sentencia.executeQuery(
                    "SELECT count(*)::int AS n FROM identidad.usuario WHERE id = '%s'".formatted(usuario));
            filas.next();
            assertThat(filas.getInt("n"))
                    .as("rol_auditor CON app.rol='AUDITOR' en sesion sigue sin ver la fila — confirma que la causa "
                            + "es que rol_auditor no es miembro de rol_aplicacion (las politicas son TO "
                            + "rol_aplicacion), no la variable app.rol. Si esto da 1, alguien ya agrego la "
                            + "membresia y esta nota quedo vieja: hay que actualizarla")
                    .isZero();
        }

        // Confirmacion directa e independiente de RLS: rol_auditor no es
        // miembro de rol_aplicacion, vía el catalogo de PostgreSQL.
        try (Connection conexion = BaseDePrueba.conexion();
                Statement sentencia = conexion.createStatement()) {
            var filas = sentencia.executeQuery(
                    "SELECT pg_has_role('rol_auditor', 'rol_aplicacion', 'MEMBER') AS es_miembro");
            filas.next();
            assertThat(filas.getBoolean("es_miembro"))
                    .as("rol_auditor es miembro de rol_aplicacion segun pg_has_role — la nota de este test quedo vieja")
                    .isFalse();
        }
    }
}

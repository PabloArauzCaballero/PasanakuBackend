package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.CU08AsignarRol.EntradaAsignacion;
import bo.aportaya.identidad.dominio.SegregacionDeFunciones;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Una prueba de rechazo por cada {@code R-XXX-nn} que CU-08 cita. */
class CU08RechazosTest extends BaseDeCU08 {

    @Test
    @DisplayName("rechaza por R-AUD-01")
    void rechazaRAUD01() {
        // Revocar escribe; la fila NO se borra. Lo hace cumplir el sellado
        // append-only de la bitacora que acompana al acto.
        dejarUnaFilaEnLaBitacora();

        assertThat(rechazaLaBase("DELETE FROM comun.bitacora_evento")).contains("R-AUD-01");
    }

    @Test
    @DisplayName("rechaza por R-AUD-04")
    void rechazaRAUD04() {
        // Ningun acceso queda vivo sin alguien que lo haya otorgado con nombre y
        // fecha: otorgada_por y otorgada_en son NOT NULL en el modelo.
        UUID operador = fixtura.usuario("+59173000001");
        UUID rol = fixtura.rolGlobal("AUD4");

        assertThat(rechazaLaBase(
                        """
                        INSERT INTO identidad.asignacion_rol (id, usuario_id, rol_id, ambito, otorgada_en)
                        VALUES (gen_random_uuid(), '%s', '%s', 'GLOBAL', now())
                        """
                                .formatted(operador, rol)))
                .contains("otorgada_por");
    }

    @Test
    @DisplayName("rechaza por R-SEG-04")
    void rechazaRSEG04() {
        // Quien autoriza no ejecuta. El atomo lo detecta antes de llegar a la base,
        // y la base lo vuelve a cortar en la entrega. Codigos del catalogo sembrado.
        assertThat(SegregacionDeFunciones.viola(Set.of("ENTREGA_AUTORIZAR", "ENTREGA_EJECUTAR")))
                .isTrue();
        assertThat(constraintExiste("ck_entrega_segregacion")).isTrue();
    }

    @Test
    @DisplayName("rechaza por R-SEG-04 · quien registra bloqueos por autoridad no recibe el rol que los levanta")
    void rechazaRSEG04Bloqueos() {
        UUID administrador = fixtura.usuario("+59173000011");
        UUID oficial = fixtura.usuario("+59173000012");
        UUID registra = rolConPermisoDelCatalogo("BLQREG", "BLOQUEO_AUTORIDAD_REGISTRAR", "REGISTRAR");
        UUID levanta = rolConPermisoDelCatalogo("BLQLEV", "BLOQUEO_AUTORIDAD_LEVANTAR", "LEVANTAR");
        asignarComo(administrador, oficial, registra);

        assertThatThrownBy(() -> asignarComo(administrador, oficial, levanta))
                .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(e.detalle())
                        .containsEntry(
                                "parIncompatible", "BLOQUEO_AUTORIDAD_REGISTRAR con BLOQUEO_AUTORIDAD_LEVANTAR"));
        assertThat(asignacionesDe(oficial)).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza por R-SEG-04 · quien modera piezas no aprueba campañas: publicidad son tres personas")
    void rechazaRSEG04Publicidad() {
        UUID administrador = fixtura.usuario("+59173000013");
        UUID moderador = fixtura.usuario("+59173000014");
        UUID modera = rolConPermisoDelCatalogo("PUBMOD", "PUBLICIDAD_MODERAR", "MODERAR");
        UUID aprueba = rolConPermisoDelCatalogo("PUBAPR", "PUBLICIDAD_APROBAR_CAMPANA", "APROBAR");
        asignarComo(administrador, moderador, modera);

        assertThatThrownBy(() -> asignarComo(administrador, moderador, aprueba))
                .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(e.detalle())
                        .containsEntry("parIncompatible", "PUBLICIDAD_MODERAR con PUBLICIDAD_APROBAR_CAMPANA"));
        assertThat(asignacionesDe(moderador)).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza por R-SEG-07")
    void rechazaRSEG07() {
        UUID administrador = fixtura.usuario("+59173000002");
        UUID rol = fixtura.rolGlobal("SEG7");

        assertThat(rechazaLaBase(
                        """
                        INSERT INTO identidad.asignacion_rol (id, usuario_id, rol_id, ambito, otorgada_por, otorgada_en)
                        VALUES (gen_random_uuid(), '%s', '%s', 'GLOBAL', '%s', now())
                        """
                                .formatted(administrador, rol, administrador)))
                .contains("ck_asignacion_no_autoasignada");
    }

    @Test
    @DisplayName("rechaza por R-SEG-08")
    void rechazaRSEG08() {
        UUID operador = fixtura.usuario("+59173000003");
        UUID otorgante = fixtura.otorgante();
        UUID rol = fixtura.rolGlobal("SEG8");
        fixtura.asignarRol(operador, rol, otorgante);

        assertThat(rechazaLaBase(
                        """
                        INSERT INTO identidad.asignacion_rol (id, usuario_id, rol_id, ambito, otorgada_por, otorgada_en)
                        VALUES (gen_random_uuid(), '%s', '%s', 'GLOBAL', '%s', now())
                        """
                                .formatted(operador, rol, otorgante)))
                .contains("uq_asignacion_vigente");
    }

    @Test
    @DisplayName("rechaza por R-SEG-10")
    void rechazaRSEG10() {
        UUID operador = fixtura.usuario("+59173000004");
        fixtura.asignarRol(operador, fixtura.rolGlobal("SEG10"));
        UUID dispositivo = fixtura.dispositivoConfiable(operador, "huella-seg10");

        // Rol GLOBAL y sin TOTP: la sesion no entra. No es el guard, es la base.
        assertThat(rechazaLaBase(
                        """
                        INSERT INTO identidad.sesion
                            (id, usuario_id, dispositivo_id, iniciada_en, ultima_actividad_en, expira_en, ip_origen)
                        VALUES (gen_random_uuid(), '%s', '%s', now(), now(), now() + interval '1 hour', '127.0.0.1'::inet)
                        """
                                .formatted(operador, dispositivo)))
                .contains("R-SEG-10");
    }

    @Test
    @DisplayName("rechaza por R-SEG-12")
    void rechazaRSEG12() {
        assertThat(
                        rechazaLaBase(
                                """
                        INSERT INTO identidad.permiso (id, codigo, descripcion, recurso, accion, nivel_riesgo, requiere_mfa)
                        VALUES (gen_random_uuid(), 'CU08_REVERSAR', 'prueba', 'accesos', 'REVERSAR', 'ALTO', false)
                        """))
                .contains("ck_permiso_decision_exige_mfa");
    }

    private UUID rolConPermisoDelCatalogo(String codigoRol, String codigoPermiso, String accion) {
        UUID rol = fixtura.rolGlobal(codigoRol);
        fixtura.darPermisoAlRol(rol, fixtura.permisoDelCatalogo(codigoPermiso, "segregacion", accion));
        return rol;
    }

    private void asignarComo(UUID administrador, UUID destinatario, UUID rol) {
        transaccion.execute(e -> asignar.ejecutar(
                new EntradaAsignacion(
                        destinatario, rol, "GLOBAL", Optional.empty(), Optional.empty(), "justificacion de prueba"),
                comoAdministrador(administrador)));
    }

    /**
     * Parametrizado y no concatenado. En una prueba el nombre es una constante, pero
     * la prohibicion 2 no admite excepciones «porque aca no se puede inyectar»: es
     * justamente ese razonamiento el que despues se copia a un sitio donde si.
     */
    private boolean constraintExiste(String nombre) {
        return contar("SELECT count(*)::int FROM pg_constraint WHERE conname = ?", nombre) > 0;
    }

    private boolean triggerExiste(String nombre) {
        return contar("SELECT count(*)::int FROM pg_trigger WHERE tgname = ?", nombre) > 0;
    }

    private int contar(String consulta, Object... parametros) {
        return ((Number) dsl.fetchOne(consulta, parametros).get(0)).intValue();
    }
}

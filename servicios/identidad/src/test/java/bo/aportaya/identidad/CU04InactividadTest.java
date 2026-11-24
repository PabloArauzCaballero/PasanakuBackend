package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.RenovarSesion;
import bo.aportaya.identidad.infraestructura.UsuarioRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GU-15 / ADR-038 §1 (P-8) · la sesion del backoffice caduca tras 15 minutos sin actividad: el refresco de un operador
 * que llega despues de ese plazo se niega y revoca la sesion con motivo {@code INACTIVIDAD} (que el vigia publica).
 * El participante no tiene este corte.
 */
class CU04InactividadTest extends BaseDeCU04 {

    private static final String SEARCH_PATH_DEL_SERVICIO = "SET LOCAL search_path TO identidad, catalogo, comun";

    private RenovarSesion refresco;

    @BeforeEach
    void armarRefresco() {
        dsl.execute(
                """
                INSERT INTO identidad.politica_token
                    (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora,
                     cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos,
                     exige_dispositivo_conocido, invalida_anteriores, vigente_desde)
                VALUES ('REFRESCO_SESION', 43200, 64, 1, 0, 0, 200, 'COOKIE_HTTPONLY', false, false,
                        '2026-01-01T00:00:00-04:00')
                ON CONFLICT (proposito, vigente_desde) DO NOTHING
                """);
        refresco = new RenovarSesion(
                new Datos(dsl), new UsuarioRepositorio(), Reloj.delSistema(), Ids.seguros(), Duration.ofMinutes(15));
    }

    private static String telefono() {
        return "+59176" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
    }

    /** La sesion se abre como participante y DESPUES se le da un rol GLOBAL: R-SEG-10 mira el alta de la sesion. */
    private UUID operadorConSesion(UUID[] sesion) {
        UUID usuario = participanteConCredencial(telefono());
        sesion[0] = fixtura.sesionAbierta(usuario);
        fixtura.asignarRol(
                usuario, fixtura.rolGlobal("OP_" + UUID.randomUUID().toString().substring(0, 8)));
        return usuario;
    }

    private void inactivaDesde(UUID sesion, String intervalo) {
        dsl.execute(
                "UPDATE identidad.sesion SET ultima_actividad_en = now() - ?::interval WHERE id = ?",
                intervalo,
                sesion);
    }

    private String emitir(UUID usuario, UUID sesion) {
        return transaccion
                .execute(e -> {
                    dsl.execute(SEARCH_PATH_DEL_SERVICIO);
                    return refresco.emitir(
                            usuario,
                            sesion,
                            "127.0.0.1",
                            "prueba",
                            UUID.randomUUID().toString());
                })
                .token();
    }

    private RenovarSesion.Renovacion renovar(String token) {
        return transaccion.execute(e -> {
            dsl.execute(SEARCH_PATH_DEL_SERVICIO);
            return refresco.renovar(
                    token, "127.0.0.1", "prueba", UUID.randomUUID().toString());
        });
    }

    private String motivoDe(UUID sesion) {
        Object motivo = dsl.fetchValue("SELECT motivo_revocacion FROM identidad.sesion WHERE id = ?", sesion);
        return motivo == null ? null : motivo.toString();
    }

    @Test
    @DisplayName(
            "Dado un operador sin actividad hace 16 min · Cuando refresca · Entonces se niega y la sesion queda revocada por INACTIVIDAD")
    void operadorInactivo() {
        UUID[] sesion = new UUID[1];
        UUID usuario = operadorConSesion(sesion);
        String token = emitir(usuario, sesion[0]);
        inactivaDesde(sesion[0], "16 minutes");

        assertThat(renovar(token).renovada()).isFalse();
        assertThat(motivoDe(sesion[0])).isEqualTo("INACTIVIDAD");
    }

    @Test
    @DisplayName(
            "Dado un operador con actividad hace 14 min · Cuando refresca · Entonces renueva y su actividad se actualiza")
    void operadorActivo() {
        UUID[] sesion = new UUID[1];
        UUID usuario = operadorConSesion(sesion);
        String token = emitir(usuario, sesion[0]);
        inactivaDesde(sesion[0], "14 minutes");

        assertThat(renovar(token).renovada()).isTrue();
        assertThat(motivoDe(sesion[0])).isNull();
        assertThat(dsl.fetchValue(
                        "SELECT ultima_actividad_en > now() - interval '1 minute' FROM identidad.sesion WHERE id = ?",
                        sesion[0]))
                .isEqualTo(true);
    }

    @Test
    @DisplayName(
            "Dado un participante sin actividad hace 16 min · Cuando refresca · Entonces renueva: el corte es solo del backoffice")
    void participanteNoTieneCorte() {
        UUID usuario = participanteConCredencial(telefono());
        UUID sesion = fixtura.sesionAbierta(usuario);
        String token = emitir(usuario, sesion);
        inactivaDesde(sesion, "16 minutes");

        assertThat(renovar(token).renovada()).isTrue();
        assertThat(motivoDe(sesion)).isNull();
    }
}

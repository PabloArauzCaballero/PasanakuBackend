package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.RenovarSesion;
import bo.aportaya.identidad.infraestructura.UsuarioRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ADR-010 · el refresh rotado del backoffice, contra la base real: la revocacion por reuso
 * la hace el trigger {@code fn_seg_detectar_reuso_refresco} (R-SEG-09), asi que solo se
 * puede probar con PostgreSQL de verdad.
 */
class CU04RenovacionTest extends BaseDeCU04 {

    private RenovarSesion refresco;

    @BeforeEach
    void armarRefresco() {
        // La base de prueba trae el esquema, no las semillas: la politica se siembra aca,
        // con los mismos valores que seeders/minimos/13-politicas-de-token.json.
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
        refresco = new RenovarSesion(new Datos(dsl), new UsuarioRepositorio(), Reloj.delSistema(), Ids.seguros());
    }

    @Test
    @DisplayName("Dado un refresh vivo · Cuando se usa · Entonces entrega otro de la misma familia y el usado queda consumido")
    void rotacion() {
        UUID usuario = participanteConCredencial("+59171000021");
        UUID sesion = fixtura.sesionAbierta(usuario);
        String primero = emitir(usuario, sesion);

        var renovacion = renovar(primero);

        assertThat(renovacion.renovada()).isTrue();
        assertThat(renovacion.usuarioId()).contains(usuario);
        String segundo = renovacion.siguiente().orElseThrow().token();
        assertThat(segundo).isNotEqualTo(primero).hasSize(64);
        assertThat(estadoDe(primero)).isEqualTo("CONSUMIDO");
        assertThat(estadoDe(segundo)).isEqualTo("EMITIDO");
        assertThat(dsl.fetchValue(
                        "SELECT count(DISTINCT familia_id) FROM identidad.token_verificacion WHERE usuario_id = ? AND tipo_token = 'REFRESCO'",
                        usuario))
                .isEqualTo(1L);
        assertThat(dsl.fetchValue(
                        "SELECT refresco_familia_id IS NOT NULL FROM identidad.sesion WHERE id = ?", sesion))
                .isEqualTo(true);
    }

    @Test
    @DisplayName("Dado un refresh ya rotado hace rato · Cuando alguien lo reusa · Entonces se revoca la familia y la sesion, y el refresh nuevo tampoco sirve")
    void reusoRevocaLaFamilia() {
        UUID usuario = participanteConCredencial("+59171000022");
        UUID sesion = fixtura.sesionAbierta(usuario);
        String robado = emitir(usuario, sesion);
        String legitimo = renovar(robado).siguiente().orElseThrow().token();
        // Fuera de la ventana de gracia: el uso duplicado ya no es una carrera del mismo navegador.
        dsl.execute(
                "UPDATE identidad.token_verificacion SET consumido_en = now() - interval '1 minute' WHERE hash_token = encode(digest(?, 'sha256'), 'hex')",
                robado);

        assertThat(renovar(robado).renovada()).isFalse();

        assertThat(estadoDe(legitimo)).isEqualTo("INVALIDADO");
        assertThat(dsl.fetchValue("SELECT revocada_en IS NOT NULL FROM identidad.sesion WHERE id = ?", sesion))
                .isEqualTo(true);
        assertThat(renovar(legitimo).renovada()).isFalse();
    }

    @Test
    @DisplayName("Dado dos usos casi simultaneos del mismo refresh (dos pestanas) · Entonces el segundo se rechaza SIN revocar la sesion")
    void graciaParaLaCarrera() {
        UUID usuario = participanteConCredencial("+59171000023");
        UUID sesion = fixtura.sesionAbierta(usuario);
        String primero = emitir(usuario, sesion);
        String segundo = renovar(primero).siguiente().orElseThrow().token();

        assertThat(renovar(primero).renovada()).isFalse();

        assertThat(estadoDe(segundo)).isEqualTo("EMITIDO");
        assertThat(dsl.fetchValue("SELECT revocada_en IS NULL FROM identidad.sesion WHERE id = ?", sesion))
                .isEqualTo(true);
        assertThat(renovar(segundo).renovada()).isTrue();
    }

    @Test
    @DisplayName("Sin cookie o con un valor que no es un refresh emitido · Entonces se rechaza")
    void desconocidoOAusente() {
        assertThat(renovar(null).renovada()).isFalse();
        assertThat(renovar("").renovada()).isFalse();
        assertThat(renovar("f".repeat(64)).renovada()).isFalse();
    }

    @Test
    @DisplayName("Dado un refresh vencido · Entonces se rechaza")
    void vencido() {
        UUID usuario = participanteConCredencial("+59171000024");
        String token = emitir(usuario, fixtura.sesionAbierta(usuario));
        dsl.execute(
                "UPDATE identidad.token_verificacion SET expira_en = now() - interval '1 second' WHERE hash_token = encode(digest(?, 'sha256'), 'hex')",
                token);

        assertThat(renovar(token).renovada()).isFalse();
    }

    @Test
    @DisplayName("Dado una sesion revocada (p. ej. cambio de credencial) · Entonces su refresh ya no renueva")
    void sesionRevocada() {
        UUID usuario = participanteConCredencial("+59171000025");
        UUID sesion = fixtura.sesionAbierta(usuario);
        String token = emitir(usuario, sesion);
        dsl.execute("UPDATE identidad.sesion SET revocada_en = now(), motivo_revocacion = 'prueba' WHERE id = ?", sesion);

        assertThat(renovar(token).renovada()).isFalse();
    }

    @Test
    @DisplayName("El refresh en claro no se guarda: en la base solo esta su SHA-256")
    void soloElHash() {
        UUID usuario = participanteConCredencial("+59171000026");
        String token = emitir(usuario, fixtura.sesionAbierta(usuario));

        assertThat(dsl.fetchValue(
                        "SELECT count(*) FROM identidad.token_verificacion WHERE hash_token = ?", token))
                .isEqualTo(0L);
        assertThat(estadoDe(token)).isEqualTo("EMITIDO");
    }

    // Con el search_path del rol del servicio (svc_identidad), no el del administrador de la
    // prueba: sin esto el caso de uso usaba digest() de pgcrypto —que vive en `public`—, los
    // tests pasaban y en runtime el ingreso web daba 500 (H10.S3.M1).
    private static final String SEARCH_PATH_DEL_SERVICIO = "SET LOCAL search_path TO identidad, catalogo, comun";

    private String emitir(UUID usuario, UUID sesion) {
        return transaccion.execute(e -> {
                    dsl.execute(SEARCH_PATH_DEL_SERVICIO);
                    return refresco.emitir(usuario, sesion, "127.0.0.1", "prueba", UUID.randomUUID().toString());
                })
                .token();
    }

    private RenovarSesion.Renovacion renovar(String token) {
        return transaccion.execute(e -> {
            dsl.execute(SEARCH_PATH_DEL_SERVICIO);
            return refresco.renovar(token, "127.0.0.1", "prueba", UUID.randomUUID().toString());
        });
    }

    private String estadoDe(String token) {
        return dsl.fetchValue(
                "SELECT estado FROM identidad.token_verificacion WHERE hash_token = encode(digest(?, 'sha256'), 'hex')",
                token).toString();
    }
}

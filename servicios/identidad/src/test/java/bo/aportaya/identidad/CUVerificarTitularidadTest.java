package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.VerificarTitularidad;
import bo.aportaya.identidad.dominio.DocumentoDeIdentidad;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril I (quinta vuelta) · CU-18 → CU-01: {@code VerificarTitularidad} contra PostgreSQL real y con el ROL REAL del servicio
 * ({@code svc_identidad}, que SI pasa por la politica de fila: el dueno de la base la saltearia y la prueba mentiria).
 *
 * <p>Antes la lectura corria con el contexto del que preguntaba. La politica de fila deja ver {@code usuario} y
 * {@code documento_identidad} al titular y a los roles de cumplimiento, no a una credencial de servicio: preguntando
 * {@code entregas} por OTRA persona la fila no se veia y el veredicto era siempre «no». Ahora la lectura es de sistema, acotada
 * a un booleano; quien puede llegar hasta aca lo decide la guardia de la ruta (probada en {@code UsuariosAutorizacionNegativaWebTest}).
 */
class CUVerificarTitularidadTest extends BaseDeCU08 {

    private static final String PIMIENTA = "pimienta-de-prueba-no-es-la-de-produccion";

    private final VerificarTitularidad verificar = new VerificarTitularidad(new Datos(dsl), PIMIENTA);

    private UUID titularConDocumento(String documento) {
        UUID usuario = fixtura.usuario(
                "+5917" + (6_000_000 + ThreadLocalRandom.current().nextInt(3_000_000)));
        dsl.execute(
                """
                INSERT INTO identidad.documento_identidad
                    (id, usuario_id, tipo, numero_cifrado, version_llave, hash_numero, pais_emision, estado)
                VALUES (gen_random_uuid(), ?, 'CI', 'cifrado-de-prueba', 1, ?, 'BO', 'APROBADA')
                """,
                usuario,
                DocumentoDeIdentidad.hashDeNumero(documento, PIMIENTA));
        return usuario;
    }

    /** Quien pregunta es un servicio, no el titular ni backoffice: la politica de fila NO le abre la fila. */
    private boolean comoServicio(UUID usuario, String nombre, String documento) {
        var ctx = ContextoSesion.de(
                UUID.randomUUID(),
                "SERVICIO_ENTREGAS",
                new Traza(UUID.randomUUID().toString()));
        return transaccion.execute(t -> {
            dsl.execute("SET LOCAL ROLE svc_identidad");
            return verificar.coincide(usuario, nombre, documento, ctx);
        });
    }

    @Test
    @DisplayName(
            "correcto: nombre y documento del titular (sin tildes ni mayusculas) coinciden aunque pregunte un servicio"
                    + " por OTRA persona")
    void coincideElTitular() {
        UUID titular = titularConDocumento("7654321");

        assertThat(comoServicio(titular, "ANA  quispe", "7654321")).isTrue();
    }

    @Test
    @DisplayName("rechaza: otro nombre, otro documento, un usuario sin documento y un usuario inexistente NO coinciden")
    void rechazaLoQueNoCoincide() {
        UUID titular = titularConDocumento("7654322");
        UUID sinDocumento = fixtura.usuario(
                "+5917" + (6_000_000 + ThreadLocalRandom.current().nextInt(3_000_000)));

        assertThat(comoServicio(titular, "Pablo Arauz", "7654322")).isFalse();
        assertThat(comoServicio(titular, "Ana Quispe", "1111111")).isFalse();
        assertThat(comoServicio(sinDocumento, "Ana Quispe", "7654322")).isFalse();
        assertThat(comoServicio(UUID.randomUUID(), "Ana Quispe", "7654322")).isFalse();
    }
}

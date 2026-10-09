package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.aplicacion.ConfirmarVerificacionCorreo;
import bo.aportaya.identidad.aplicacion.SolicitarVerificacionCorreo;
import bo.aportaya.identidad.dominio.CanalDeVerificacion;
import bo.aportaya.identidad.dominio.DocumentoDeIdentidad;
import bo.aportaya.identidad.dominio.puertos.CorreoDeVerificacion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Prueba el recorrido real: emitir, rechazar, confirmar y crear una sola cuenta. */
@SpringBootTest
class CU01VerificacionCorreoTest {

    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @DynamicPropertySource
    static void configuracion(DynamicPropertyRegistry registro) {
        var contenedor = BaseDePrueba.contenedor();
        registro.add("spring.datasource.url", contenedor::getJdbcUrl);
        registro.add("spring.datasource.username", contenedor::getUsername);
        registro.add("spring.datasource.password", contenedor::getPassword);
        registro.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registro.add("aportaya.jwt.jwks-uri", () -> "http://identidad:8080/.well-known/jwks.json");
        registro.add("SEGURIDAD_PIMIENTA", () -> "pimienta-de-prueba-correo");
        registro.add("ARCHIVOS_URL", () -> "http://archivos-de-prueba:9000");
        registro.add("ARCHIVOS_USUARIO", () -> "prueba");
        registro.add("ARCHIVOS_CLAVE", () -> "prueba");
    }

    @Autowired
    private SolicitarVerificacionCorreo solicitar;

    @Autowired
    private ConfirmarVerificacionCorreo confirmar;

    @Autowired
    private CU01RegistrarUsuario registrar;

    @Autowired
    private DSLContext dsl;

    @MockitoBean
    private CorreoDeVerificacion correo;

    private final AtomicReference<String> codigoEntregado = new AtomicReference<>();

    @BeforeEach
    void capturarCodigo() {
        dsl.execute(
                """
                INSERT INTO identidad.politica_token
                    (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion,
                     max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia,
                     canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde)
                VALUES ('VERIFICACION_CORREO', 600, 6, 5, 2, 60, 5, 'CORREO', false, true,
                        '2026-10-07T00:00:00-04:00'::timestamptz)
                ON CONFLICT (proposito, vigente_desde) DO NOTHING
                """);
        codigoEntregado.set(null);
        doAnswer(invocacion -> {
                    codigoEntregado.set(invocacion.getArgument(1, String.class));
                    return null;
                })
                .when(correo)
                .enviarCodigo(any(), any(), any());
    }

    @Test
    @DisplayName("el codigo incorrecto no confirma; el correcto habilita exactamente un alta")
    void recorridoCompleto() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String destino = "alta-" + sufijo + "@example.com";
        ContextoSesion contexto = contexto();
        UUID idempotencia = UUID.randomUUID();
        var emitida = solicitar.ejecutar(destino, idempotencia, "127.0.0.1", "prueba-integracion", contexto);

        assertThat(codigoEntregado.get()).matches("\\d{6}");
        assertThatThrownBy(() -> solicitar.ejecutar(
                        "otro-" + destino, idempotencia, "127.0.0.1", "prueba-integracion", contexto))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("otro correo");
        String incorrecto = codigoEntregado.get().equals("999999") ? "000000" : "999999";
        assertThatThrownBy(() -> confirmar.ejecutar(
                        emitida.verificacionId(), destino, incorrecto, "127.0.0.1", "prueba-integracion", contexto))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no es correcto");
        assertThat(intentosFallidos(emitida.verificacionId())).isEqualTo(1);
        assertThat(estado(emitida.verificacionId())).isEqualTo("ENVIADO");

        confirmar.ejecutar(
                emitida.verificacionId(), destino, codigoEntregado.get(), "127.0.0.1", "prueba-integracion", contexto);
        assertThat(estado(emitida.verificacionId())).isEqualTo("CONSUMIDO");

        var creada = registrar.ejecutar(entrada(destino, emitida.verificacionId(), sufijo), contexto);
        assertThat(creada.usuarioId()).isNotNull();
        assertThat(usuarioDelToken(emitida.verificacionId())).isEqualTo(creada.usuarioId());

        String otroDestino = "otro-" + sufijo + "@example.com";
        long antes = usuariosConCorreo(otroDestino);
        assertThatThrownBy(() -> registrar.ejecutar(
                        entrada(otroDestino, emitida.verificacionId(), "z" + sufijo.substring(1)), contexto))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("Confirma el codigo correcto");
        assertThat(usuariosConCorreo(otroDestino)).isEqualTo(antes);
    }

    private CU01RegistrarUsuario.EntradaRegistro entrada(String correo, UUID verificacion, String sufijo) {
        String digitos = Integer.toUnsignedString(sufijo.hashCode());
        digitos = (digitos + "00000000").substring(0, 8);
        String documento = ("12345" + digitos).substring(0, 10);
        return new CU01RegistrarUsuario.EntradaRegistro(
                "+5917" + digitos.substring(0, 7),
                "Ana",
                "Quispe",
                LocalDate.of(1990, 1, 1),
                correo,
                CanalDeVerificacion.CORREO,
                verificacion,
                DocumentoDeIdentidad.de(
                        DocumentoDeIdentidad.Tipo.CI, documento, "pimienta-de-prueba-correo", "BO", "LP"),
                "cifrado:" + documento,
                "clave-fuerte-2026".toCharArray(),
                List.of(UUID.randomUUID()),
                true,
                "127.0.0.1",
                "prueba-integracion");
    }

    private ContextoSesion contexto() {
        return ContextoSesion.deSistema(PROCESO, new Traza(UUID.randomUUID().toString()));
    }

    private int intentosFallidos(UUID id) {
        return ((Number) dsl.fetchOne("SELECT intentos_fallidos FROM identidad.token_verificacion WHERE id = ?", id)
                        .get(0))
                .intValue();
    }

    private String estado(UUID id) {
        return String.valueOf(dsl.fetchOne("SELECT estado FROM identidad.token_verificacion WHERE id = ?", id)
                .get(0));
    }

    private UUID usuarioDelToken(UUID id) {
        return (UUID) dsl.fetchOne("SELECT usuario_id FROM identidad.token_verificacion WHERE id = ?", id)
                .get(0);
    }

    private long usuariosConCorreo(String correo) {
        return ((Number) dsl.fetchOne("SELECT count(*) FROM identidad.usuario WHERE correo = ?", correo)
                        .get(0))
                .longValue();
    }
}

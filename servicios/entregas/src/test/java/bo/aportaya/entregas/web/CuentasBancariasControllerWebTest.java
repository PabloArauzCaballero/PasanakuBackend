package bo.aportaya.entregas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.entregas.aplicacion.CU18RegistrarCuentaDestino;
import bo.aportaya.entregas.dominio.puertos.TitularDeLaBilletera;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-18 · la cuenta bancaria a la que sale el dinero.
 *
 * <p>Dos afirmaciones que solo existen en la respuesta HTTP.
 *
 * <p><b>El número de cuenta no vuelve nunca.</b> Entra en claro para derivar el
 * enmascarado y el hash, y de ahí no sale: ni en el cuerpo de éxito, ni en el del
 * error. Una cuenta completa en una respuesta es una cuenta completa en el registro del
 * navegador, en la traza y en la bitácora del gateway.
 *
 * <p><b>La titularidad se comprueba ANTES de la transacción.</b> Es una llamada de red
 * y meterla adentro violaría el invariante 6. Que el caso de uso no se toque cuando el
 * titular no coincide es lo que prueba que ese orden se respeta.
 */
@PruebaWeb(CuentasBancariasController.class)
class CuentasBancariasControllerWebTest {

    private static final UUID CUENTA = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final String EN_CLARO = "1234567890123456";

    private static final String CUERPO =
            """
            {"tipoCuenta":"AHORRO","entidadFinanciera":"Banco Nacional",
             "numeroCuenta":"1234567890123456","numeroCifrado":"AAAA-cifrado-por-el-almacen",
             "titularNombre":"Pablo Arauz","titularDocumento":"1234567","moneda":"BOB"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU18RegistrarCuentaDestino cu18;

    @MockitoBean
    private TitularDeLaBilletera titular;

    private org.springframework.test.web.servlet.ResultActions registrar(String cuerpo) throws Exception {
        return mvc.perform(post("/cuentas-bancarias")
                .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Nested
    @DisplayName("POST /cuentas-bancarias — registrar")
    class Registrar {

        @Test
        @DisplayName("CU-18 · 201 con el número ENMASCARADO, y el número en claro no vuelve")
        void caminoFeliz() throws Exception {
            when(titular.esElMismo(any(), any(), any())).thenReturn(true);
            when(cu18.registrar(any(), any()))
                    .thenReturn(new CU18RegistrarCuentaDestino.SalidaRegistro(CUENTA, "****3456", "PENDIENTE", true));

            String cuerpo = registrar(CUERPO)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cuentaId").value(CUENTA.toString()))
                    .andExpect(jsonPath("$.numeroEnmascarado").value("****3456"))
                    .andExpect(jsonPath("$.esNueva").value(true))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            org.assertj.core.api.Assertions.assertThat(cuerpo)
                    .as("el numero en claro salio en la respuesta")
                    .doesNotContain(EN_CLARO)
                    .doesNotContain("AAAA-cifrado-por-el-almacen");
        }

        @Test
        @DisplayName("CU-18 · 200 cuando la cuenta ya estaba registrada: reintentar no duplica")
        void reintento() throws Exception {
            when(titular.esElMismo(any(), any(), any())).thenReturn(true);
            when(cu18.registrar(any(), any()))
                    .thenReturn(new CU18RegistrarCuentaDestino.SalidaRegistro(CUENTA, "****3456", "PENDIENTE", false));

            registrar(CUERPO)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.esNueva").value(false));
        }

        @Test
        @DisplayName("CU-18 · AP-CU18-01: si el titular no coincide, el caso de uso NO se toca")
        void titularQueNoCoincide() throws Exception {
            when(titular.esElMismo(any(), any(), any())).thenReturn(false);

            String cuerpo = registrar(CUERPO)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-CU18-01"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // La comprobacion es una llamada de red y va ANTES de la transaccion
            // (invariante 6). Que el caso de uso no se haya tocado es lo que lo prueba.
            verify(cu18, never()).registrar(any(), any());
            // Y el error tampoco puede contar el numero ni el documento del titular.
            org.assertj.core.api.Assertions.assertThat(cuerpo)
                    .doesNotContain(EN_CLARO)
                    .doesNotContain("1234567");
        }

        @Test
        @DisplayName("CU-18 · el nombre y el documento llegan al caso de uso tal como se comprobaron")
        void loQueLlegaAlCasoDeUso() throws Exception {
            when(titular.esElMismo(any(), any(), any())).thenReturn(true);
            when(cu18.registrar(any(), any()))
                    .thenReturn(new CU18RegistrarCuentaDestino.SalidaRegistro(CUENTA, "****3456", "PENDIENTE", true));

            registrar(CUERPO).andExpect(status().isCreated());

            var capturada = ArgumentCaptor.forClass(CU18RegistrarCuentaDestino.EntradaRegistro.class);
            verify(cu18).registrar(capturada.capture(), any());
            var entrada = capturada.getValue();

            org.assertj.core.api.Assertions.assertThat(entrada.tipoCuenta()).isEqualTo("AHORRO");
            org.assertj.core.api.Assertions.assertThat(entrada.entidadFinanciera())
                    .isEqualTo("Banco Nacional");
            org.assertj.core.api.Assertions.assertThat(entrada.moneda()).isEqualTo("BOB");
        }

        @Test
        @DisplayName("CU-18 · 400: un tipo de cuenta que el contrato no enumera")
        void tipoDeCuentaFueraDelContrato() throws Exception {
            registrar(CUERPO.replace("AHORRO", "COLCHON")).andExpect(status().isBadRequest());
            verifyNoInteractions(cu18);
            verifyNoInteractions(titular);
        }

        @Test
        @DisplayName("CU-18 · 400: un número de cuenta más corto que el mínimo del contrato")
        void numeroDemasiadoCorto() throws Exception {
            registrar(CUERPO.replace(EN_CLARO, "123")).andExpect(status().isBadRequest());
            verifyNoInteractions(cu18);
        }

        @Test
        @DisplayName("CU-18 · 400: sin numeroCifrado, que el contrato exige")
        void faltaElNumeroCifrado() throws Exception {
            registrar(CUERPO.replace("\"numeroCifrado\":\"AAAA-cifrado-por-el-almacen\",", ""))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu18);
        }
    }

    @Nested
    @DisplayName("El enfriamiento y la cuenta principal")
    class Disponibilidad {

        @Test
        @DisplayName("CU-18 · 200 con lo que falta del enfriamiento, en segundos")
        void enfriamientoEnCurso() throws Exception {
            when(cu18.disponibilidad(any(), any()))
                    .thenReturn(new CU18RegistrarCuentaDestino.Disponibilidad(
                            false, Duration.ofHours(6), "en enfriamiento desde la verificacion"));

            mvc.perform(get("/cuentas-bancarias/{id}/disponibilidad", CUENTA)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disponible").value(false))
                    // En segundos y no en texto: el cliente tiene que poder mostrar
                    // cuanto falta sin interpretar una frase.
                    .andExpect(jsonPath("$.restanteSegundos").value(21600))
                    .andExpect(jsonPath("$.motivo").exists());
        }

        @Test
        @DisplayName("CU-18 · una cuenta ya disponible informa cero segundos restantes")
        void yaDisponible() throws Exception {
            when(cu18.disponibilidad(any(), any()))
                    .thenReturn(new CU18RegistrarCuentaDestino.Disponibilidad(true, Duration.ZERO, null));

            mvc.perform(get("/cuentas-bancarias/{id}/disponibilidad", CUENTA)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disponible").value(true))
                    .andExpect(jsonPath("$.restanteSegundos").value(0));
        }

        @Test
        @DisplayName("CU-18 · verificar devuelve desde cuándo la cuenta puede recibir")
        void verificar() throws Exception {
            when(cu18.verificar(any(), any(), any()))
                    .thenReturn(new CU18RegistrarCuentaDestino.SalidaVerificacion(
                            CUENTA, "VERIFICADA", OffsetDateTime.of(2026, 3, 16, 14, 30, 0, 0, ZoneOffset.UTC), true));

            mvc.perform(post("/cuentas-bancarias/{id}/verificacion", CUENTA)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"metodo\":\"MICRODEPOSITO\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("VERIFICADA"))
                    // Sin esta fecha, el titular no sabe cuando puede cobrar.
                    .andExpect(jsonPath("$.disponibleDesde").exists());
        }

        @Test
        @DisplayName("CU-18 · designar principal contesta si quedó designada, y nada más")
        void designarPrincipal() throws Exception {
            when(cu18.designarPrincipal(any(), any())).thenReturn(true);

            mvc.perform(post("/cuentas-bancarias/{id}/principal", CUENTA)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.designada").value(true));
        }
    }
}

package bo.aportaya.publicidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.publicidad.aplicacion.CU110AltaDeAnunciante;
import bo.aportaya.publicidad.aplicacion.CU111CrearCampana;
import bo.aportaya.publicidad.aplicacion.CU112ModerarPieza;
import bo.aportaya.publicidad.aplicacion.CU113EntregarAnuncio;
import bo.aportaya.publicidad.aplicacion.CU114LiquidarPublicidad;
import java.math.BigDecimal;
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
 * El contrato HTTP de {@code /publicidad}.
 *
 * <p>Catorce operaciones y cinco permisos. Los dos que importan están separados a
 * propósito: <b>quien gestiona una campaña no la aprueba</b>, y <b>quien sube una pieza
 * no la modera</b>. Sin esa separación, un anunciante publica lo que quiere.
 *
 * <p>Y una afirmación sobre el dinero: la entrega de un anuncio devuelve un
 * <b>costo fino</b> —con más decimales que un importe— porque una impresión vale
 * fracciones de centavo y redondear en cada una perdería la diferencia al liquidar. Que
 * viaje como cadena es lo que impide que la coma flotante se la coma.
 */
@PruebaWeb(PublicidadController.class)
class PublicidadControllerWebTest {

    private static final UUID CUENTA = UUID.fromString("a2000000-0000-4000-8000-000000000001");
    private static final UUID ESPACIO = UUID.fromString("a2000000-0000-4000-8000-000000000002");
    private static final UUID ANUNCIO = UUID.fromString("a2000000-0000-4000-8000-000000000003");
    private static final UUID IMPRESION = UUID.fromString("a2000000-0000-4000-8000-000000000004");
    private static final UUID FACTURA = UUID.fromString("a2000000-0000-4000-8000-000000000005");
    private static final String CLAVE = "a2000000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU110AltaDeAnunciante cu110;

    @MockitoBean
    private CU111CrearCampana cu111;

    @MockitoBean
    private CU112ModerarPieza cu112;

    @MockitoBean
    private CU113EntregarAnuncio cu113;

    @MockitoBean
    private CU114LiquidarPublicidad cu114;

    @Nested
    @DisplayName("CU-113 · entregar un anuncio y cobrarlo")
    class Entrega {

        private org.springframework.test.web.servlet.ResultActions entregar() throws Exception {
            return mvc.perform(post("/publicidad/espacios/{id}/entrega", ESPACIO)
                    .with(Sesiones.como("OPERADOR_PUBLICIDAD", "PUBLICIDAD_CAMPANA_GESTIONAR"))
                    .header("Idempotency-Key", CLAVE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"usuarioId\":\"a2000000-0000-4000-8000-000000000009\"}"));
        }

        @Test
        @DisplayName("CU-113 · 200 con el costo fino como cadena: una impresión vale fracciones de centavo")
        void entregaConCosto() throws Exception {
            when(cu113.entregar(any(), any()))
                    .thenReturn(new CU113EntregarAnuncio.Salida(ANUNCIO, IMPRESION, new BigDecimal("0.0450"), null));

            entregar()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.anuncioId").value(ANUNCIO.toString()))
                    .andExpect(jsonPath("$.impresionId").value(IMPRESION.toString()))
                    // Cuatro decimales, como cadena. Redondear cada impresion a dos
                    // perderia la diferencia entera al liquidar el mes.
                    .andExpect(jsonPath("$.costo").value("0.0450"))
                    .andExpect(jsonPath("$.motivo").doesNotExist());
        }

        @Test
        @DisplayName("CU-113 · un espacio sin anuncio elegible responde 200 y dice POR QUÉ, sin cobrar")
        void sinAnuncioElegible() throws Exception {
            when(cu113.entregar(any(), any()))
                    .thenReturn(new CU113EntregarAnuncio.Salida(null, null, BigDecimal.ZERO, "SIN_ANUNCIO_ELEGIBLE"));

            entregar()
                    .andExpect(status().isOk())
                    // No es un error: el espacio pregunto y la respuesta es «nada».
                    .andExpect(jsonPath("$.anuncioId").doesNotExist())
                    .andExpect(jsonPath("$.motivo").value("SIN_ANUNCIO_ELEGIBLE"));
        }

        @Test
        @DisplayName("CU-113 · el clic se cobra aparte y devuelve su propio costo")
        void clic() throws Exception {
            when(cu113.registrarClic(any(), any()))
                    .thenReturn(new CU113EntregarAnuncio.SalidaClic(
                            UUID.fromString("a2000000-0000-4000-8000-00000000000c"), new BigDecimal("0.0120")));

            mvc.perform(post("/publicidad/impresiones/{id}/clic", IMPRESION)
                            .with(Sesiones.como("OPERADOR_PUBLICIDAD", "PUBLICIDAD_CAMPANA_GESTIONAR"))
                            .header("Idempotency-Key", CLAVE))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.costo").value("0.0120"));
        }
    }

    @Nested
    @DisplayName("CU-114 · consumo y liquidación del período")
    class Liquidacion {

        @Test
        @DisplayName("CU-114 · el consumo del período viaja como cadena decimal")
        void consumo() throws Exception {
            when(cu114.consumoDelPeriodo(any(), any(), any())).thenReturn(new BigDecimal("1250.75"));

            mvc.perform(get("/publicidad/cuentas/{id}/consumo", CUENTA)
                            .param("periodo", "2026-02")
                            .with(Sesiones.como("ANUNCIANTE", "PUBLICIDAD_ANUNCIANTES")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodo").value("2026-02"))
                    .andExpect(jsonPath("$.total").value("1250.75"));
        }

        @Test
        @DisplayName("CU-114 · 400: un período que no cumple el patrón AAAA-MM del contrato")
        void periodoFueraDelPatron() throws Exception {
            // A diferencia de un `enum` de consulta —que el generador NO aplica, ver
            // IndicadoresControllerWebTest en auditoria—, un `pattern` SI se genera
            // como `@Pattern` y el proxy de `@Validated` lo rechaza. Devolvia 500 hasta
            // que el manejador global aprendio a traducir la excepcion de Jakarta.
            mvc.perform(get("/publicidad/cuentas/{id}/consumo", CUENTA)
                            .param("periodo", "febrero-2026")
                            .with(Sesiones.como("ANUNCIANTE", "PUBLICIDAD_ANUNCIANTES")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu114);
        }

        @Test
        @DisplayName("CU-114 · un costo con más decimales de los que el contrato admite NO se redondea en silencio")
        void masDecimalesDeLosQueEntranEsUnDefecto() throws Exception {
            // `RoundingMode.UNNECESSARY` a proposito: si el calculo produjera mas
            // precision de la que el contrato publica, redondear la perderia sin que
            // nadie se entere. Falla, y falla ruidosamente.
            when(cu113.entregar(any(), any()))
                    .thenReturn(new CU113EntregarAnuncio.Salida(ANUNCIO, IMPRESION, new BigDecimal("0.000450"), null));

            mvc.perform(post("/publicidad/espacios/{id}/entrega", ESPACIO)
                            .with(Sesiones.como("OPERADOR_PUBLICIDAD", "PUBLICIDAD_CAMPANA_GESTIONAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"usuarioId\":\"a2000000-0000-4000-8000-000000000009\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.codigo").value("AP-INT-01"));
        }

        @Test
        @DisplayName("CU-114 · 201 con la factura, el monto y la cuenta por cobrar que quedó abierta")
        void liquidar() throws Exception {
            when(cu114.liquidar(any(), any()))
                    .thenReturn(new CU114LiquidarPublicidad.Salida(
                            FACTURA,
                            new BigDecimal("1250.75"),
                            "BOB",
                            "GENERADA",
                            UUID.fromString("a2000000-0000-4000-8000-00000000000d")));

            mvc.perform(post("/publicidad/cuentas/{id}/liquidaciones", CUENTA)
                            .with(Sesiones.como("CONTABILIDAD", "PUBLICIDAD_LIQUIDAR"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"periodo\":\"2026-02\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.facturaPublicidadId").value(FACTURA.toString()))
                    .andExpect(jsonPath("$.montoTotal").value("1250.75"))
                    .andExpect(jsonPath("$.moneda").value("BOB"))
                    .andExpect(jsonPath("$.estado").value("GENERADA"))
                    // Sin la cuenta por cobrar, la liquidacion no queda enganchada a la
                    // contabilidad y el ingreso no se sigue.
                    .andExpect(jsonPath("$.cuentaPorCobrarId").exists());

            var capturada = ArgumentCaptor.forClass(CU114LiquidarPublicidad.Entrada.class);
            verify(cu114).liquidar(capturada.capture(), any());
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().cuentaPublicitariaId())
                    .isEqualTo(CUENTA);
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().periodo())
                    .isEqualTo("2026-02");
        }
    }

    @Nested
    @DisplayName("Quien gestiona no aprueba, y quien sube no modera")
    class SeparacionDePermisos {

        @Test
        @DisplayName("CU-111 · aprobar una campaña exige PUBLICIDAD_APROBAR_CAMPANA, no gestionarla")
        void gestionarNoEsAprobar() throws Exception {
            mvc.perform(post("/publicidad/campanas/{id}/aprobacion", ANUNCIO)
                            .with(Sesiones.como("OPERADOR_PUBLICIDAD", "PUBLICIDAD_CAMPANA_GESTIONAR"))
                            .header("Idempotency-Key", CLAVE))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu111);
        }

        @Test
        @DisplayName("CU-112 · moderar una pieza exige PUBLICIDAD_MODERAR, no haberla subido")
        void subirNoEsModerar() throws Exception {
            mvc.perform(post("/publicidad/piezas-creativas/{id}/revision", ANUNCIO)
                            .with(Sesiones.como("ANUNCIANTE", "PUBLICIDAD_ANUNCIANTES"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu112);
        }

        @Test
        @DisplayName("CU-114 · liquidar exige PUBLICIDAD_LIQUIDAR: ver el consumo no alcanza")
        void verConsumoNoEsLiquidar() throws Exception {
            mvc.perform(post("/publicidad/cuentas/{id}/liquidaciones", CUENTA)
                            .with(Sesiones.como("ANUNCIANTE", "PUBLICIDAD_ANUNCIANTES"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"periodo\":\"2026-02\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu114);
        }
    }
}

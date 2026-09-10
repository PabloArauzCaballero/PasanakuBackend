package bo.aportaya.tarifas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.tarifas.aplicacion.CU30CotizarComision;
import bo.aportaya.tarifas.aplicacion.CU31DevengarComision;
import bo.aportaya.tarifas.aplicacion.CU33DevolverComision;
import bo.aportaya.tarifas.dominio.CalculoDeComision;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
 * CU-30, CU-31 y CU-33 · cotizar una comisión, devengarla y devolverla.
 *
 * <p>Lo que se fija acá es la promesa de la cotización: <b>el usuario ve el desglose en
 * lenguaje llano ANTES de aceptar</b>, y el total que ve es el que se le cobra. Una
 * cotización sin desglose obliga a aceptar un número sin saber de qué está hecho; y si
 * el impuesto no viajara separado, el usuario no podría distinguir lo que cobra la
 * plataforma de lo que se lleva el fisco.
 *
 * <p>Y la separación que sostiene el control: <b>devolver una comisión exige
 * REVERSO_AUTORIZAR</b>, no el permiso con el que se cobró.
 */
@PruebaWeb(ComisionesController.class)
class ComisionesControllerWebTest {

    private static final UUID COTIZACION = UUID.fromString("b5000000-0000-4000-8000-000000000001");
    private static final UUID DEVENGO = UUID.fromString("b5000000-0000-4000-8000-000000000002");
    private static final UUID REFERENCIA = UUID.fromString("b5000000-0000-4000-8000-000000000003");
    private static final String CLAVE = "b5000000-0000-4000-8000-0000000000ff";

    private static final String CUERPO =
            """
            {"codigoTarifario":"TAR-2026","hechoGenerador":"RETIRO",
             "referenciaTipo":"ORDEN_RETIRO",
             "referenciaId":"b5000000-0000-4000-8000-000000000003",
             "montoBase":{"monto":"1000.00","moneda":"BOB"}}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU30CotizarComision cu30;

    @MockitoBean
    private CU31DevengarComision cu31;

    @MockitoBean
    private CU33DevolverComision cu33;

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private org.springframework.test.web.servlet.ResultActions cotizar(String cuerpo) throws Exception {
        return mvc.perform(post("/comisiones/cotizaciones")
                .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Nested
    @DisplayName("CU-30 · la cotización que el usuario ve antes de aceptar")
    class Cotizar {

        @Test
        @DisplayName("CU-30 · 201 con comisión, impuesto y total SEPARADOS, y el desglose en llano")
        void cotizacionConDesglose() throws Exception {
            when(cu30.cotizar(any(), any()))
                    .thenReturn(new CU30CotizarComision.SalidaCotizacion(
                            COTIZACION,
                            bob("15.00"),
                            bob("1.95"),
                            bob("16.95"),
                            List.of(
                                    new CalculoDeComision.LineaDesglose(
                                            "COMISION", "1,5 % del monto retirado", bob("15.00")),
                                    new CalculoDeComision.LineaDesglose("IVA", "13 % sobre la comision", bob("1.95"))),
                            OffsetDateTime.of(2026, 3, 15, 15, 0, 0, 0, ZoneOffset.UTC),
                            true));

            cotizar(CUERPO)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cotizacionId").value(COTIZACION.toString()))
                    // Separados: si viajara solo el total, el usuario no podria
                    // distinguir lo que cobra la plataforma de lo que se lleva el fisco.
                    .andExpect(jsonPath("$.montoComision.monto").value("15.00"))
                    .andExpect(jsonPath("$.montoImpuesto.monto").value("1.95"))
                    .andExpect(jsonPath("$.montoTotal.monto").value("16.95"))
                    // En lenguaje llano: es lo que se lee antes de aceptar.
                    .andExpect(jsonPath("$.desglose[0].detalle").value("1,5 % del monto retirado"))
                    .andExpect(jsonPath("$.desglose[1].monto.monto").value("1.95"))
                    // Y hasta cuando vale: una cotizacion sin vencimiento se acepta
                    // meses despues con el tarifario viejo.
                    .andExpect(jsonPath("$.validaHasta").exists());
        }

        @Test
        @DisplayName("CU-30 · 200 cuando el reintento devuelve la cotización que ya existía")
        void reintento() throws Exception {
            when(cu30.cotizar(any(), any()))
                    .thenReturn(new CU30CotizarComision.SalidaCotizacion(
                            COTIZACION, bob("15.00"), bob("1.95"), bob("16.95"), List.of(), null, false));

            cotizar(CUERPO)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.esNueva").value(false));
        }

        @Test
        @DisplayName("CU-30 · la clave de la CABECERA y el monto base llegan intactos al cálculo")
        void loQueLlegaAlCalculo() throws Exception {
            when(cu30.cotizar(any(), any()))
                    .thenReturn(new CU30CotizarComision.SalidaCotizacion(
                            COTIZACION, bob("15.00"), bob("1.95"), bob("16.95"), List.of(), null, true));

            cotizar(CUERPO).andExpect(status().isCreated());

            var capturada = ArgumentCaptor.forClass(CU30CotizarComision.EntradaCotizacion.class);
            verify(cu30).cotizar(capturada.capture(), any());
            var entrada = capturada.getValue();

            org.assertj.core.api.Assertions.assertThat(entrada.claveIdempotencia())
                    .isEqualTo(CLAVE);
            org.assertj.core.api.Assertions.assertThat(entrada.montoBase()).isEqualByComparingTo(bob("1000.00"));
            org.assertj.core.api.Assertions.assertThat(entrada.codigoTarifario())
                    .isEqualTo("TAR-2026");
            org.assertj.core.api.Assertions.assertThat(entrada.referenciaId()).isEqualTo(REFERENCIA);
            // Sin descuento ni grupo declarados, llegan vacios y no nulos.
            org.assertj.core.api.Assertions.assertThat(entrada.descuento()).isEmpty();
            org.assertj.core.api.Assertions.assertThat(entrada.grupoId()).isEmpty();
        }

        @Test
        @DisplayName("CU-30 · 400: un tipo de referencia que el contrato no enumera")
        void referenciaFueraDelContrato() throws Exception {
            cotizar(CUERPO.replace("ORDEN_RETIRO", "UNA_APUESTA")).andExpect(status().isBadRequest());
            verifyNoInteractions(cu30);
        }

        @Test
        @DisplayName("CU-30 · 400: sin monto base no hay nada que cotizar")
        void faltaElMontoBase() throws Exception {
            cotizar(
                            """
                            {"codigoTarifario":"TAR-2026","hechoGenerador":"RETIRO",
                             "referenciaTipo":"ORDEN_RETIRO",
                             "referenciaId":"b5000000-0000-4000-8000-000000000003"}
                            """)
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu30);
        }

        @Test
        @DisplayName("CU-30 · aceptar la cotización contesta si quedó aceptada")
        void aceptar() throws Exception {
            when(cu30.aceptar(eq(COTIZACION), any())).thenReturn(true);

            mvc.perform(post("/comisiones/cotizaciones/{id}/aceptacion", COTIZACION)
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.aceptada").value(true));
        }
    }

    @Test
    @DisplayName("CU-33 · devolver una comisión exige REVERSO_AUTORIZAR: cobrarla no alcanza")
    void devolverNoSeHaceConElPermisoDeCobrar() throws Exception {
        // Quien cobra la comision no puede ademas devolverla: seria poder mover el
        // ingreso de la plataforma en los dos sentidos sin que nadie mas firme.
        mvc.perform(post("/comisiones/{id}/devoluciones", DEVENGO)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu33);
    }
}

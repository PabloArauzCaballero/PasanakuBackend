package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.PagoDeRecargaDePrueba;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** «Simular pago» del QR de prueba: lo que solo se ve en HTTP (404 sin doble, permiso, forma de la respuesta). */
@PruebaWeb(RecargasDePruebaController.class)
class RecargasDePruebaControllerWebTest {

    private static final String CLAVE = "eeeeeeee-0000-4000-8000-0000000000ff";
    private static final UUID ORDEN = UUID.fromString("9b3e8dff-d7e2-4c41-8cdc-b1be7320ca40");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PagoDeRecargaDePrueba pagoDePrueba;

    @Test
    @DisplayName("pago de prueba · 200 con el saldo resultante cuando el ambiente tiene el doble de prueba")
    void pagoDePrueba() throws Exception {
        when(pagoDePrueba.disponible()).thenReturn(true);
        when(pagoDePrueba.pagar(eq(ORDEN), any()))
                .thenReturn(new CU10RecargarSaldo.SalidaAcreditacion(
                        ORDEN, UUID.randomUUID(), Dinero.de("650.00", Moneda.BOB)));

        mvc.perform(pagar("BILLETERA_OPERAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenRecargaId").value(ORDEN.toString()))
                .andExpect(jsonPath("$.saldoDespues.monto").value("650.00"));
    }

    @Test
    @DisplayName("pago de prueba · 404 como ruta inexistente sin el doble; 403 con solo BILLETERA_VER; 401 sin sesion")
    void pagoDePruebaRechazos() throws Exception {
        when(pagoDePrueba.disponible()).thenReturn(false);

        mvc.perform(pagar("BILLETERA_OPERAR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("AP-VAL-05"));
        mvc.perform(pagar("BILLETERA_VER")).andExpect(status().isForbidden());
        mvc.perform(post("/recargas-de-prueba/{id}/pago", ORDEN).header("Idempotency-Key", CLAVE))
                .andExpect(status().isUnauthorized());
        verify(pagoDePrueba, never()).pagar(any(), any());
    }

    private static MockHttpServletRequestBuilder pagar(String... permisos) {
        return post("/recargas-de-prueba/{id}/pago", ORDEN)
                .with(Sesiones.como("PARTICIPANTE", permisos))
                .header("Idempotency-Key", CLAVE);
    }
}

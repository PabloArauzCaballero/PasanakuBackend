package bo.aportaya.aportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.aportes.aplicacion.CU100RecibirWebhookPasarela;
import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago;
import bo.aportaya.aportes.aplicacion.CU99EnrutarProveedor;
import bo.aportaya.aportes.aplicacion.ConsultarPlataformaDePagos;
import bo.aportaya.aportes.dominio.PlataformaDePagos.Proveedor;
import bo.aportaya.aportes.dominio.PlataformaDePagos.Webhook;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Sistemas › Proveedores y Webhooks en HTTP: forma, lo que no sale, 400 y 403. El 401 lo cubre SeguridadWebTest. */
@PruebaWeb(PagosController.class)
class PlataformaDePagosWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarPlataformaDePagos plataforma;

    @MockitoBean
    private CU19ReembolsarPago cu19;

    @MockitoBean
    private CU99EnrutarProveedor cu99;

    @MockitoBean
    private CU100RecibirWebhookPasarela cu100;

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Test
    @DisplayName("correcto · proveedores sin credenciales ni url")
    void proveedores() throws Exception {
        when(plataforma.proveedores(any()))
                .thenReturn(List.of(new Proveedor(ID, "QRB", "Pasarela QR", "PASARELA", true, true, 1)));
        mvc.perform(get("/pagos/proveedores").with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("QRB"))
                .andExpect(jsonPath("$[0].tipo").value("PASARELA"))
                .andExpect(jsonPath("$[0].referenciaCredenciales").doesNotExist())
                .andExpect(jsonPath("$[0].urlBase").doesNotExist());
    }

    @Test
    @DisplayName("correcto · webhooks sin payload ni firma; sin procesar no trae procesadoEn; limite por omision 50")
    void webhooks() throws Exception {
        when(plataforma.webhooks(eq(50), any()))
                .thenReturn(List.of(new Webhook(
                        ID,
                        "QRB",
                        "pago.confirmado",
                        "FALLIDO",
                        false,
                        OffsetDateTime.parse("2026-10-29T10:00:00Z"),
                        Optional.empty(),
                        3)));
        mvc.perform(get("/pagos/webhooks-recibidos").with(Sesiones.como("AUDITOR_INTERNO", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("FALLIDO"))
                .andExpect(jsonPath("$[0].firmaValida").value(false))
                .andExpect(jsonPath("$[0].procesadoEn").doesNotExist())
                .andExpect(jsonPath("$[0].payloadCrudo").doesNotExist())
                .andExpect(jsonPath("$[0].firma").doesNotExist());
    }

    @Test
    @DisplayName("invalido · limite 0 y 101: 400 sin consultar")
    void limiteFuera() throws Exception {
        for (String l : List.of("0", "101")) {
            mvc.perform(get("/pagos/webhooks-recibidos?limite=" + l)
                            .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(plataforma);
    }

    @Test
    @DisplayName("autorizacion · 403 con un permiso de operacion (el que reembolsa no ve integraciones)")
    void sinPermiso() throws Exception {
        mvc.perform(get("/pagos/proveedores").with(Sesiones.como("TESORERIA", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pagos/webhooks-recibidos").with(Sesiones.como("TESORERIA", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(plataforma);
    }
}

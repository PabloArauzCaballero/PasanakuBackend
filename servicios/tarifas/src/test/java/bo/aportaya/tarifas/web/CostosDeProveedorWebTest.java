package bo.aportaya.tarifas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.tarifas.aplicacion.CU34PublicarTarifario;
import bo.aportaya.tarifas.aplicacion.CU35CerrarLiquidacion;
import bo.aportaya.tarifas.aplicacion.CU36ResolverPrecio;
import bo.aportaya.tarifas.aplicacion.ConsultarCostosDeProveedor;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarioVigente;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarios;
import bo.aportaya.tarifas.dominio.CostoDeProveedor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Sistemas › Proveedores · costos en HTTP: el decimal viaja como texto exacto, el periodo se valida, 403 sin permiso. */
@PruebaWeb(TarifasController.class)
class CostosDeProveedorWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarCostosDeProveedor costos;

    @MockitoBean
    private ConsultarTarifarioVigente vigentes;

    @MockitoBean
    private ConsultarTarifarios historial;

    @MockitoBean
    private CU34PublicarTarifario cu34;

    @MockitoBean
    private CU35CerrarLiquidacion cu35;

    @MockitoBean
    private CU36ResolverPrecio cu36;

    @Test
    @DisplayName("correcto · el total viaja como texto exacto con su moneda")
    void exacto() throws Exception {
        UUID p = UUID.fromString("00000000-0000-4000-8000-000000000001");
        when(costos.delPeriodo(eq("2026-09"), any()))
                .thenReturn(List.of(new CostoDeProveedor(p, "2026-09", "BOB", new BigDecimal("0.30"), 2)));
        mvc.perform(get("/tarifas/costos-de-proveedor?periodo=2026-09")
                        .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].costoTotal").value("0.30"))
                .andExpect(jsonPath("$[0].moneda").value("BOB"))
                .andExpect(jsonPath("$[0].operaciones").value(2));
    }

    @Test
    @DisplayName("invalido · sin periodo, mes 13 o con otro formato: 400 sin consultar")
    void periodoInvalido() throws Exception {
        for (String q : List.of("", "?periodo=2026-13", "?periodo=2026-9", "?periodo=sep-2026")) {
            mvc.perform(get("/tarifas/costos-de-proveedor" + q)
                            .with(Sesiones.como("ADMIN_PLATAFORMA", "TABLERO_SISTEMAS_VER")))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(costos);
    }

    @Test
    @DisplayName("autorizacion · 403 con el permiso de publicar tarifario")
    void sinPermiso() throws Exception {
        mvc.perform(get("/tarifas/costos-de-proveedor?periodo=2026-09")
                        .with(Sesiones.como("COMERCIAL", "TARIFARIO_PUBLICAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(costos);
    }
}

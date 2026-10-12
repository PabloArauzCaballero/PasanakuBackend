package bo.aportaya.entregas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.aplicacion.CU22LiquidarEntrega;
import bo.aportaya.entregas.aplicacion.ComprarOfertaDeTurno;
import bo.aportaya.entregas.aplicacion.GestionDeOfertas;
import bo.aportaya.entregas.dominio.OfertaParaElPago;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * H9.S1.M5 · el PRODUCTOR del contrato que {@code nucleo-financiero} espera para pagar al vendedor:
 * {@code GET /entregas/mercado/ofertas/{id}} con permiso de identidad de servicio {@code SERVICIO_ENTREGAS}.
 * El consumidor es {@code OfertasPorHttp} de nucleo (su prueba lee el mismo cuerpo).
 *
 * <p>Tres niveles: correcto (id, vendedor, precio y estado, y nada mas), limite (cada estado de la oferta llega tal cual)
 * e invalido (usuario final, sin sesion, otra identidad de servicio, oferta inexistente, id mal formado, metodo equivocado).
 */
@PruebaWeb(EntregasController.class)
@Import(MercadoWeb.class)
class OfertaParaPagoWebTest {

    private static final UUID OFERTA = UUID.fromString("b8100000-0000-4000-8000-000000000001");
    private static final UUID VENDEDOR = UUID.fromString("b8100000-0000-4000-8000-000000000002");
    private static final UUID COMPRADOR = UUID.fromString("b8100000-0000-4000-8000-000000000003");
    private static final String RUTA = "/entregas/mercado/ofertas/{id}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GestionDeOfertas ofertas;

    @MockitoBean
    private ComprarOfertaDeTurno compra;

    @MockitoBean
    private CU22LiquidarEntrega cu22;

    @MockitoBean
    private CU22EntregarPozoCompleto pozo;

    private static OfertaParaElPago oferta(String estado) {
        return new OfertaParaElPago(
                OFERTA, VENDEDOR, Dinero.de("5500.00", Moneda.BOB), Dinero.de("50.00", Moneda.BOB), estado);
    }

    @Test
    @DisplayName("200: con la identidad de servicio de entregas devuelve id, vendedor, precio y cargos como cadena y estado")
    void correcto() throws Exception {
        when(ofertas.verParaElPago(eq(OFERTA), any())).thenReturn(Optional.of(oferta("RESERVADA")));

        mvc.perform(get(RUTA, OFERTA).with(Sesiones.como("SERVICIO_ENTREGAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ofertaId").value(OFERTA.toString()))
                .andExpect(jsonPath("$.vendedorUsuarioId").value(VENDEDOR.toString()))
                .andExpect(jsonPath("$.precio.monto").value("5500.00"))
                .andExpect(jsonPath("$.precio.moneda").value("BOB"))
                .andExpect(jsonPath("$.cargos.monto").value("50.00"))
                .andExpect(jsonPath("$.cargos.moneda").value("BOB"))
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.length()").value(5));
        verify(ofertas).verParaElPago(eq(OFERTA), any());
    }

    @Test
    @DisplayName("200: no filtra el grupo, el turno, el derecho ni al comprador (respuesta minima: los cargos SI viajan)")
    void respuestaMinima() throws Exception {
        when(ofertas.verParaElPago(eq(OFERTA), any())).thenReturn(Optional.of(oferta("LIQUIDANDO")));

        var cuerpo = mvc.perform(get(RUTA, OFERTA).with(Sesiones.como("SERVICIO_ENTREGAS")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("grupoId")
                .doesNotContain("turnoId")
                .doesNotContain("derecho")
                .doesNotContain(COMPRADOR.toString());
    }

    @Test
    @DisplayName("200 (limite): cada estado de la oferta llega tal cual; decidir si se paga es de nucleo")
    void cadaEstadoLlegaTalCual() throws Exception {
        for (String estado : new String[] {"PUBLICADA", "RESERVADA", "LIQUIDANDO", "VENDIDA", "CANCELADA", "VENCIDA"}) {
            when(ofertas.verParaElPago(eq(OFERTA), any())).thenReturn(Optional.of(oferta(estado)));

            mvc.perform(get(RUTA, OFERTA).with(Sesiones.como("SERVICIO_ENTREGAS")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value(estado));
        }
    }

    @Test
    @DisplayName("422 AP-CU130-05: una oferta que no existe no se inventa (nucleo no paga)")
    void ofertaInexistente() throws Exception {
        when(ofertas.verParaElPago(eq(OFERTA), any())).thenReturn(Optional.empty());

        mvc.perform(get(RUTA, OFERTA).with(Sesiones.como("SERVICIO_ENTREGAS")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU130-05"));
    }

    @Test
    @DisplayName("403: un participante, aunque sea el vendedor o el comprador, no consulta el hecho de pago")
    void usuarioFinalNoLoConsulta() throws Exception {
        mvc.perform(get(RUTA, OFERTA).with(Sesiones.como("PARTICIPANTE"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA, OFERTA).with(Sesiones.como("PARTICIPANTE", "ENTREGA_EJECUTAR", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ofertas);
    }

    @Test
    @DisplayName("403: otra identidad de servicio (inversiones), el backoffice y un rol sin nada tampoco")
    void otrasIdentidadesNo() throws Exception {
        mvc.perform(get(RUTA, OFERTA).with(Sesiones.como("SERVICIO_INVERSIONES")))
                .andExpect(status().isForbidden());
        mvc.perform(get(RUTA, OFERTA).with(Sesiones.como("ADMIN_PLATAFORMA"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA, OFERTA).with(Sesiones.sinPermisos())).andExpect(status().isForbidden());
        verifyNoInteractions(ofertas);
    }

    @Test
    @DisplayName("401: sin sesion")
    void sinSesion() throws Exception {
        mvc.perform(get(RUTA, OFERTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(ofertas);
    }

    @Test
    @DisplayName("400: un id que no es UUID se rechaza antes de llegar al caso de uso")
    void idMalFormado() throws Exception {
        mvc.perform(get(RUTA, "no-es-un-uuid").with(Sesiones.como("SERVICIO_ENTREGAS")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ofertas);
    }

    @Test
    @DisplayName("la consulta solo lee: un PUT sobre la oferta no existe")
    void soloLectura() throws Exception {
        mvc.perform(put(RUTA, OFERTA).with(Sesiones.como("SERVICIO_ENTREGAS"))).andExpect(status().is4xxClientError());
        verifyNoInteractions(ofertas);
    }
}

package bo.aportaya.entregas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto;
import bo.aportaya.entregas.aplicacion.CU22LiquidarEntrega;
import bo.aportaya.entregas.aplicacion.ComprarOfertaDeTurno;
import bo.aportaya.entregas.aplicacion.GestionDeOfertas;
import bo.aportaya.entregas.aplicacion.RegistroDeOfertas;
import bo.aportaya.entregas.dominio.OfertaVisible;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El contrato HTTP de {@code /entregas/mercado}. */
@PruebaWeb(EntregasController.class)
@Import(MercadoWeb.class)
class MercadoWebTest {

    private static final UUID OFERTA = UUID.fromString("b8000000-0000-4000-8000-000000000001");
    private static final UUID TURNO = UUID.fromString("b8000000-0000-4000-8000-000000000002");
    private static final UUID CESION = UUID.fromString("b8000000-0000-4000-8000-000000000003");
    private static final String CLAVE = "b8000000-0000-4000-8000-0000000000ff";

    private static final String CUERPO =
            """
            {"turnoId":"b8000000-0000-4000-8000-000000000002","precio":{"monto":"5500.00","moneda":"BOB"},
             "cargos":{"monto":"50.00","moneda":"BOB"},"vigenteHasta":"2026-10-20T12:00:00Z"}
            """;

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

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName("POST /ofertas · 201 y la clave de la cabecera se vuelve clave de operacion; 200 si es el reintento")
    void publicar() throws Exception {
        when(ofertas.publicar(any(), any())).thenReturn(new RegistroDeOfertas.Publicada(OFERTA, true));
        mvc.perform(post("/entregas/mercado/ofertas")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ofertaId").value(OFERTA.toString()));

        when(ofertas.publicar(any(), any())).thenReturn(new RegistroDeOfertas.Publicada(OFERTA, false));
        mvc.perform(post("/entregas/mercado/ofertas")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esNueva").value(false));
    }

    @Test
    @DisplayName("POST /ofertas · 403 sin ser participante, 401 sin sesion, 400 sin clave o con el precio como numero")
    void guardiaYValidacion() throws Exception {
        mvc.perform(post("/entregas/mercado/ofertas")
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isForbidden());
        mvc.perform(post("/entregas/mercado/ofertas")
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/entregas/mercado/ofertas")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/entregas/mercado/ofertas")
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.replace("\"5500.00\"", "5500.0")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ofertas);
    }

    @Test
    @DisplayName("GET /ofertas · 200 con importes como cadena y SIN ningun dato del vendedor")
    void listar() throws Exception {
        when(ofertas.listar(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(new OfertaVisible(
                        OFERTA,
                        UUID.randomUUID(),
                        TURNO,
                        bob("6000.00"),
                        bob("5500.00"),
                        bob("50.00"),
                        OffsetDateTime.parse("2026-10-20T12:00:00Z"))));

        mvc.perform(get("/entregas/mercado/ofertas?limite=10&pagina=0").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ofertas[0].precio.monto").value("5500.00"))
                .andExpect(jsonPath("$.ofertas[0].vendedor").doesNotExist())
                .andExpect(jsonPath("$.ofertas[0].vendedorUsuarioId").doesNotExist());
        mvc.perform(get("/entregas/mercado/ofertas?limite=1000").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /compra · 200 si quedo liquidada, 202 si sigue en curso, 422 con el codigo si pierde la carrera")
    void comprar() throws Exception {
        when(compra.comprar(any(), any(), any()))
                .thenReturn(new ComprarOfertaDeTurno.Resultado(CESION, "LIQUIDADA", true));
        mvc.perform(post("/entregas/mercado/ofertas/{id}/compra", OFERTA)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liquidada").value(true));

        when(compra.comprar(any(), any(), any()))
                .thenReturn(new ComprarOfertaDeTurno.Resultado(CESION, "TITULO_ASIGNADO", false));
        mvc.perform(post("/entregas/mercado/ofertas/{id}/compra", OFERTA)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("TITULO_ASIGNADO"))
                .andExpect(jsonPath("$.liquidada").value(false));

        when(compra.comprar(any(), any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(130, 5), "Esa oferta no esta disponible."));
        mvc.perform(post("/entregas/mercado/ofertas/{id}/compra", OFERTA)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU130-05"));
    }

    @Test
    @DisplayName("POST /cancelacion · 200 para el participante; 403 para quien no lo es")
    void cancelar() throws Exception {
        when(ofertas.cancelar(any(), any())).thenReturn(true);

        mvc.perform(post("/entregas/mercado/ofertas/{id}/cancelacion", OFERTA).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelada").value(true));
        mvc.perform(post("/entregas/mercado/ofertas/{id}/cancelacion", OFERTA)
                        .with(Sesiones.como("OPERADOR", "ENTREGA_EJECUTAR")))
                .andExpect(status().isForbidden());
    }
}

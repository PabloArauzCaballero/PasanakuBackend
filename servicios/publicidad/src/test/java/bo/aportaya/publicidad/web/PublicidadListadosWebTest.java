package bo.aportaya.publicidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.publicidad.aplicacion.CU110AltaDeAnunciante;
import bo.aportaya.publicidad.aplicacion.CU111CrearCampana;
import bo.aportaya.publicidad.aplicacion.CU112ModerarPieza;
import bo.aportaya.publicidad.aplicacion.CU113EntregarAnuncio;
import bo.aportaya.publicidad.aplicacion.CU114LiquidarPublicidad;
import bo.aportaya.publicidad.aplicacion.ConsultarListadosDePublicidad;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Backoffice carril A · H7.S1 · el contrato HTTP de los tres listados de publicidad.
 *
 * <p>Cada listado con su permiso, y ninguno abre al otro: el anunciante no ve la cola de campanas
 * de Operaciones y Operaciones no modera. Lo que hacen las consultas contra la base vive en
 * {@code CU110ListadoTest}, {@code CU111ListadoTest} y {@code CU112ListadoTest}.
 */
@PruebaWeb(PublicidadController.class)
class PublicidadListadosWebTest {

    private static final UUID ANUNCIANTE = UUID.fromString("a2000000-0000-4000-8000-0000000000b1");
    private static final UUID CUENTA = UUID.fromString("a2000000-0000-4000-8000-0000000000b2");
    private static final UUID CAMPANA = UUID.fromString("a2000000-0000-4000-8000-0000000000b3");
    private static final UUID PIEZA = UUID.fromString("a2000000-0000-4000-8000-0000000000b4");
    private static final OffsetDateTime CUANDO = OffsetDateTime.of(2026, 10, 1, 12, 0, 0, 0, ZoneOffset.UTC);

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

    @MockitoBean
    private ConsultarListadosDePublicidad listados;

    @Test
    @DisplayName("200: anunciantes con su cuenta y el limite como Dinero; sin limite, null")
    void anunciantes() throws Exception {
        when(listados.anunciantes(any(), any(), any(), eq(25), any()))
                .thenReturn(new ConsultarListadosDePublicidad.Pagina<>(
                        List.of(
                                new ConsultarListadosDePublicidad.FilaAnunciante(
                                        ANUNCIANTE,
                                        "ORGANIZADOR",
                                        "Pasanaku del barrio",
                                        "ACTIVO",
                                        CUANDO,
                                        Optional.of(CUENTA),
                                        Optional.of("ACTIVA"),
                                        Optional.of(new BigDecimal("9000.00")),
                                        Optional.of("BOB")),
                                new ConsultarListadosDePublicidad.FilaAnunciante(
                                        UUID.randomUUID(),
                                        "SOCIO_COMERCIAL",
                                        "Ferreteria",
                                        "ACTIVO",
                                        CUANDO,
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty())),
                        Optional.empty(),
                        Optional.empty()));

        String cuerpo = mvc.perform(get("/publicidad/anunciantes")
                        .with(Sesiones.como("OPERADOR_PUBLICIDAD", "PUBLICIDAD_ANUNCIANTES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].razonSocialFacturacion").value("Pasanaku del barrio"))
                .andExpect(jsonPath("$.items[0].limiteGastoMensual.monto").value("9000.00"))
                .andExpect(jsonPath("$.items[0].limiteGastoMensual.moneda").value("BOB"))
                .andExpect(jsonPath("$.items[1].limiteGastoMensual").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(cuerpo).doesNotContain("numeroDocumento").doesNotContain("organizadorId");
    }

    @Test
    @DisplayName("200: la cola de campanas trae presupuestos como Dinero, y el filtro de estados llega entero")
    void campanas() throws Exception {
        when(listados.campanas(any(), any(), any(), any(Integer.class), any()))
                .thenReturn(new ConsultarListadosDePublicidad.Pagina<>(
                        List.of(new ConsultarListadosDePublicidad.FilaCampana(
                                CAMPANA,
                                CUENTA,
                                ANUNCIANTE,
                                "Campana de octubre",
                                "VISIBILIDAD_MARCA",
                                new BigDecimal("900.00"),
                                new BigDecimal("0.00"),
                                "BOB",
                                CUANDO,
                                Optional.empty(),
                                "EN_REVISION",
                                Optional.empty())),
                        Optional.of(CUANDO),
                        Optional.of(CAMPANA)));

        mvc.perform(get("/publicidad/campanas")
                        .param("estados", "EN_REVISION,PAUSADA")
                        .with(Sesiones.como("OPERADOR_PUBLICIDAD", "PUBLICIDAD_APROBAR_CAMPANA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].presupuestoTotal.monto").value("900.00"))
                .andExpect(jsonPath("$.items[0].estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.proximoId").value(CAMPANA.toString()));
        verify(listados).campanas(eq(List.of("EN_REVISION", "PAUSADA")), any(), any(), eq(25), any());
    }

    @Test
    @DisplayName("200: la cola de moderacion; el estadoModeracion llega al caso de uso")
    void piezas() throws Exception {
        when(listados.piezas(any(), any(), any(), any(Integer.class), any()))
                .thenReturn(new ConsultarListadosDePublicidad.Pagina<>(
                        List.of(new ConsultarListadosDePublicidad.FilaPieza(
                                PIEZA,
                                ANUNCIANTE,
                                "Aporta con QR",
                                Optional.empty(),
                                "https://cdn.ejemplo/a.png",
                                "IMAGEN",
                                "PENDIENTE",
                                CUANDO)),
                        Optional.empty(),
                        Optional.empty()));

        mvc.perform(get("/publicidad/piezas-creativas")
                        .param("estadoModeracion", "PENDIENTE")
                        .with(Sesiones.como("MODERADOR_CONTENIDO", "PUBLICIDAD_MODERAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].piezaCreativaId").value(PIEZA.toString()))
                .andExpect(jsonPath("$.items[0].texto").doesNotExist());
        verify(listados).piezas(eq("PENDIENTE"), any(), any(), eq(25), any());
    }

    @Test
    @DisplayName("403: cada listado con su permiso; el anunciante no ve la cola de Operaciones ni modera")
    void permisos() throws Exception {
        mvc.perform(get("/publicidad/campanas").with(Sesiones.como("ANUNCIANTE", "PUBLICIDAD_CAMPANA_GESTIONAR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/publicidad/piezas-creativas")
                        .with(Sesiones.como(
                                "OPERADOR_PUBLICIDAD", "PUBLICIDAD_ANUNCIANTES", "PUBLICIDAD_APROBAR_CAMPANA")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/publicidad/anunciantes").with(Sesiones.como("MODERADOR_CONTENIDO", "PUBLICIDAD_MODERAR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/publicidad/anunciantes")).andExpect(status().isUnauthorized());
        verifyNoInteractions(listados);
    }

    @Test
    @DisplayName("422: filtro invalido con el codigo del CU; 400 si el limite pasa de 100")
    void errores() throws Exception {
        when(listados.piezas(any(), any(), any(), any(Integer.class), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(112, 4), "Filtro de listado invalido."));

        mvc.perform(get("/publicidad/piezas-creativas")
                        .param("estadoModeracion", "EN_DUDA")
                        .with(Sesiones.como("MODERADOR_CONTENIDO", "PUBLICIDAD_MODERAR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU112-04"));
        mvc.perform(get("/publicidad/campanas")
                        .param("limite", "101")
                        .with(Sesiones.como("OPERADOR_PUBLICIDAD", "PUBLICIDAD_APROBAR_CAMPANA")))
                .andExpect(status().isBadRequest());
    }
}

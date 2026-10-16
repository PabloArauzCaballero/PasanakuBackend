package bo.aportaya.cumplimiento.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.cumplimiento.aplicacion.CU02ElevarDiligencia;
import bo.aportaya.cumplimiento.aplicacion.CU03DeclararPep;
import bo.aportaya.cumplimiento.aplicacion.CU05AceptarContrato;
import bo.aportaya.cumplimiento.aplicacion.CU05ConsultarContratosVigentes;
import bo.aportaya.cumplimiento.aplicacion.CU54RegistrarRiesgoOperativo;
import bo.aportaya.cumplimiento.aplicacion.CU55GestionarIncidente;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejasLft;
import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento;
import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento.EventoDeRiesgo;
import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento.Pagina;
import bo.aportaya.cumplimiento.aplicacion.ConsultarTableroDeCumplimiento.Requerimiento;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Backoffice carril A · H9 · las seis colas de cumplimiento, riesgo y gobierno: permiso por rol con
 * AUDITORIA_LEER como alternativa, forma de la respuesta (respuesta minima, dinero con moneda) y rechazos
 * con codigo. El recorrido contra la base vive en {@code CU43..CU94ColaTest}.
 */
@PruebaWeb({UifController.class, CumplimientoController.class})
@Import(TableroDeCumplimientoWeb.class)
class ColasDeCumplimientoWebTest {
    private static final UUID ID = UUID.fromString("c9000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime EN = OffsetDateTime.of(2026, 7, 1, 8, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarTableroDeCumplimiento tablero;

    @MockitoBean
    private CU02ElevarDiligencia cu02;

    @MockitoBean
    private CU03DeclararPep cu03;

    @MockitoBean
    private CU05AceptarContrato cu05;

    @MockitoBean
    private CU05ConsultarContratosVigentes contratosVigentes;

    @MockitoBean
    private CU54RegistrarRiesgoOperativo cu54;

    @MockitoBean
    private CU55GestionarIncidente cu55;

    @MockitoBean
    private ConsultarBandejasLft bandejas;

    @MockitoBean
    private bo.aportaya.cumplimiento.aplicacion.CU44InvestigarYReportar cu44;

    @MockitoBean
    private bo.aportaya.cumplimiento.aplicacion.CU44DecidirComoRevisor decisionLft;

    @Test
    @DisplayName("200: el oficial ve los requerimientos sin alcance ni documento, con el plazo y el cursor")
    void requerimientos() throws Exception {
        var fila = new Requerimiento(
                ID,
                "ASFI",
                "OF-1",
                EN,
                EN.plusDays(5),
                "RECIBIDO",
                Optional.empty(),
                true,
                Optional.empty(),
                Optional.empty());
        when(tablero.requerimientos(any(), any(), any(), anyInt(), any()))
                .thenReturn(new Pagina<>(List.of(fila), Optional.of(EN), Optional.of(ID)));

        String cuerpo = mvc.perform(get("/uif/requerimientos").with(Sesiones.como("OFICIAL_CUMPLIMIENTO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].autoridad").value("ASFI"))
                .andExpect(jsonPath("$.items[0].conBloqueoDeSaldo").value(true))
                .andExpect(jsonPath("$.proximoAntesDeId").value(ID.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("alcance")
                .doesNotContain("documento");
    }

    @Test
    @DisplayName("200: Riesgos ve los eventos con las perdidas como Dinero en su moneda")
    void eventos() throws Exception {
        var fila = new EventoDeRiesgo(
                ID,
                "ERO-1",
                "FALLAS_SISTEMAS",
                "TECNOLOGIA_INFORMACION",
                "PAGOS",
                EN,
                EN,
                new BigDecimal("1000.00"),
                new BigDecimal("250.00"),
                new BigDecimal("750.00"),
                "BOB",
                "REGISTRADO",
                false);
        when(tablero.eventos(any(), any(), any(), anyInt(), any()))
                .thenReturn(new Pagina<>(List.of(fila), Optional.empty(), Optional.empty()));

        mvc.perform(get("/cumplimiento/riesgos/eventos").with(Sesiones.como("RESPONSABLE_RIESGOS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].perdidaNeta.monto").value("750.00"))
                .andExpect(jsonPath("$.items[0].perdidaNeta.moneda").value("BOB"))
                .andExpect(jsonPath("$.proximoAntesDeId").doesNotExist());
    }

    @Test
    @DisplayName("200: AUDITORIA_LEER lee las seis colas")
    void auditoriaLeeTodo() throws Exception {
        when(tablero.reportes(any(), any(), any(), anyInt(), any())).thenReturn(vacia());
        when(tablero.requerimientos(any(), any(), any(), anyInt(), any())).thenReturn(vacia());
        when(tablero.eventos(any(), any(), any(), anyInt(), any())).thenReturn(vacia());
        when(tablero.incidentes(any(), any(), any(), anyInt(), any())).thenReturn(vacia());
        when(tablero.pruebas(any(), any(), anyInt(), any())).thenReturn(vacia());
        when(tablero.actas(any(), any(), anyInt(), any())).thenReturn(vacia());
        for (String ruta : RUTAS) {
            mvc.perform(get(ruta).with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("403: un participante no ve ninguna cola, y la consulta ni se entera")
    void participanteNoVe() throws Exception {
        for (String ruta : RUTAS) {
            mvc.perform(get(ruta).with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        }
        verifyNoInteractions(tablero);
    }

    @Test
    @DisplayName("403: Seguridad no lee la cola de Riesgos ni Riesgos la de Seguridad")
    void rolesSeparados() throws Exception {
        mvc.perform(get("/cumplimiento/riesgos/eventos").with(Sesiones.como("RESPONSABLE_SEGURIDAD")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/cumplimiento/seguridad/incidentes").with(Sesiones.como("RESPONSABLE_RIESGOS")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(tablero);
    }

    @Test
    @DisplayName("401: sin token no hay colas")
    void sinToken() throws Exception {
        for (String ruta : RUTAS) {
            mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(tablero);
    }

    @Test
    @DisplayName("422: un filtro invalido sale con el codigo del CU, nunca como 500")
    void filtroInvalido() throws Exception {
        when(tablero.incidentes(any(), any(), any(), anyInt(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(55, 4), "Filtro de listado invalido."));

        mvc.perform(get("/cumplimiento/seguridad/incidentes")
                        .param("estados", "PERDIDO")
                        .with(Sesiones.como("RESPONSABLE_SEGURIDAD")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU55-04"));
    }

    @Test
    @DisplayName("400: limite fuera de 1..100 lo corta el contrato antes del caso de uso")
    void limiteFueraDeRango() throws Exception {
        mvc.perform(get("/cumplimiento/comites/actas").param("limite", "0").with(Sesiones.como("OFICIAL_CUMPLIMIENTO")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tablero);
    }

    private static final List<String> RUTAS = List.of(
            "/uif/reportes",
            "/uif/requerimientos",
            "/cumplimiento/riesgos/eventos",
            "/cumplimiento/seguridad/incidentes",
            "/cumplimiento/continuidad/pruebas",
            "/cumplimiento/comites/actas");

    private static <T, C> Pagina<T, C> vacia() {
        return new Pagina<>(List.of(), Optional.empty(), Optional.empty());
    }

    // Las acciones del backoffice (carril A · H9) cuelgan de los controladores: sin este doble no cargan.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AccionesDeCumplimientoWeb accionesDeCumplimiento;
}

package bo.aportaya.nucleofinanciero.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * CU-17 · {@code GET /billetera/bloqueos}, la bandeja de Cumplimiento: la abren BLOQUEO_AUTORIDAD_REGISTRAR o
 * _LEVANTAR y nadie mas; la respuesta es minima (ni numero de oficio ni documento) y el filtro es lista blanca.
 */
@PruebaWeb(
        value = BilleteraController.class,
        properties = {"aportaya.retiro.doble-aprobacion-desde=5000.00"})
@org.springframework.context.annotation.Import(FondeoDeLaBilletera.class)
@MockitoBean(types = bo.aportaya.nucleofinanciero.aplicacion.CU13LiberarRetencionDelMercado.class)
class BloqueoListadoWebTest extends BaseDeOperacionesWeb {

    private static final UUID BLOQUEO = UUID.fromString("eeeeeeee-0000-4000-8000-0000000000c1");
    private static final OffsetDateTime RECIBIDO = OffsetDateTime.of(2026, 11, 20, 15, 0, 0, 0, ZoneOffset.UTC);

    private static CU17BloquearPorAutoridad.PaginaDeBloqueos unaFila() {
        var fila = new CU17BloquearPorAutoridad.FilaDelListado(
                BLOQUEO,
                CUENTA,
                "UIF",
                "INMOVILIZACION",
                "PARCIAL",
                "VIGENTE",
                Optional.of(Dinero.de("250.00", Moneda.BOB)),
                Optional.of(Sesiones.USUARIO),
                Optional.empty(),
                RECIBIDO,
                Optional.empty(),
                Optional.empty(),
                false);
        return new CU17BloquearPorAutoridad.PaginaDeBloqueos(List.of(fila), Optional.empty(), Optional.empty());
    }

    @ParameterizedTest(name = "correcto · {0} con {1} ve la bandeja: 200 con la respuesta minima")
    @CsvSource({"ANALISTA_CUMPLIMIENTO, BLOQUEO_AUTORIDAD_REGISTRAR", "OFICIAL_CUMPLIMIENTO, BLOQUEO_AUTORIDAD_LEVANTAR"
    })
    void conPermiso(String rol, String permiso) throws Exception {
        when(cu17.listar(any(), any(), any(), anyInt(), any())).thenReturn(unaFila());

        mvc.perform(get("/billetera/bloqueos").with(Sesiones.como(rol, permiso)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].bloqueoId").value(BLOQUEO.toString()))
                .andExpect(jsonPath("$.items[0].estado").value("VIGENTE"))
                .andExpect(jsonPath("$.items[0].registradoPor").value(Sesiones.USUARIO.toString()))
                .andExpect(jsonPath("$.items[0].montoBloqueado.monto").value("250.00"))
                .andExpect(jsonPath("$.items[0].montoBloqueado.moneda").value("BOB"))
                .andExpect(jsonPath("$.items[0].puedeLevantar").value(false))
                // Respuesta minima: lo que identifica la orden ante la autoridad no viaja en la bandeja.
                .andExpect(jsonPath("$.items[0].numeroOficio").doesNotExist())
                .andExpect(jsonPath("$.items[0].documentoUrl").doesNotExist())
                .andExpect(jsonPath("$.items[0].hashDocumento").doesNotExist());
    }

    @Test
    @DisplayName("limite · el filtro y el limite llegan al caso de uso; sin limite, 25")
    void filtroYLimite() throws Exception {
        when(cu17.listar(any(), any(), any(), anyInt(), any())).thenReturn(unaFila());

        mvc.perform(get("/billetera/bloqueos")
                        .param("estado", "LEVANTADO")
                        .param("limite", "100")
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "BLOQUEO_AUTORIDAD_LEVANTAR")))
                .andExpect(status().isOk());
        verify(cu17).listar(eq(Optional.of("LEVANTADO")), eq(Optional.empty()), eq(Optional.empty()), eq(100), any());

        mvc.perform(get("/billetera/bloqueos")
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "BLOQUEO_AUTORIDAD_LEVANTAR")))
                .andExpect(status().isOk());
        verify(cu17).listar(eq(Optional.empty()), eq(Optional.empty()), eq(Optional.empty()), eq(25), any());
    }

    @ParameterizedTest(name = "invalido · {0} con {1}: 403 y no se lee nada")
    @CsvSource({
        "ANALISTA_CUMPLIMIENTO, DATOS_SENSIBLES_LEER",
        "BACKOFFICE, AUDITORIA_LEER",
        "PARTICIPANTE, BILLETERA_VER",
        "PARTICIPANTE, SERVICIO_ENTREGAS"
    })
    void sinPermiso(String rol, String permiso) throws Exception {
        mvc.perform(get("/billetera/bloqueos").with(Sesiones.como(rol, permiso)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu17);
    }

    @Test
    @DisplayName("invalido · sin token 401")
    void sinToken() throws Exception {
        mvc.perform(get("/billetera/bloqueos")).andExpect(status().isUnauthorized());
        verifyNoInteractions(cu17);
    }

    @ParameterizedTest(name = "invalido · {0}={1}: 400 y no se lee nada")
    @CsvSource({"estado, BORRADO", "estado, vigente", "limite, 0", "limite, 101", "antesDeId, no-es-uuid"})
    void entradaInvalida(String parametro, String valor) throws Exception {
        mvc.perform(get("/billetera/bloqueos")
                        .param(parametro, valor)
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "BLOQUEO_AUTORIDAD_LEVANTAR")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu17);
    }

    @Test
    @DisplayName("invalido · cursor incompleto: 422 AP-CU17-06")
    void cursorIncompleto() throws Exception {
        when(cu17.listar(any(), any(), any(), anyInt(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(17, 6), "El cursor del listado va completo."));

        mvc.perform(get("/billetera/bloqueos")
                        .param("antesDe", "2026-11-20T15:00:00Z")
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "BLOQUEO_AUTORIDAD_LEVANTAR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU17-06"));
    }
}

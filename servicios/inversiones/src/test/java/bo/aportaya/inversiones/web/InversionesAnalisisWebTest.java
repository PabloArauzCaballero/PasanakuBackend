package bo.aportaya.inversiones.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.inversiones.aplicacion.CU129SimularCaja;
import bo.aportaya.inversiones.aplicacion.VistaCorrida;
import bo.aportaya.inversiones.aplicacion.VistaDistribucion;
import bo.aportaya.inversiones.dominio.SimulacionDeCaja;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

/**
 * El contrato HTTP de lo que {@code /inversiones} ofrece para entender y operar una inversion sin
 * moverla: devengo, reintento de instrucciones, corridas, un producto, los intereses repartidos,
 * la comision estimada, el reporte de comprobantes y la simulacion de caja.
 */
@PruebaWeb(InversionesController.class)
@Import({OperacionesDeLaInversion.class, AnalisisDeLaInversion.class})
class InversionesAnalisisWebTest extends BaseWebInversiones {

    private static final String MONTO = "\"monto\":{\"monto\":\"1000.00\",\"moneda\":\"BOB\"}";
    private static final String SIMULACION = "{" + MONTO + ",\"horizonteDias\":365}";

    @Test
    @DisplayName("CU-123 · el devengo exige ADMIN_PLATAFORMA y una fecha del contrato, y la clave llega a la corrida")
    void devengo() throws Exception {
        when(operaciones.devengar(any(), any(), any(), any()))
                .thenReturn(Map.of("fecha", "2026-10-12", "devengadas", "3", "yaDevengadas", "1", "total", "12.34"));

        mvc.perform(post("/inversiones/devengos")
                        .with(Sesiones.como("OPERACIONES", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-10-12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devengadas").value(3))
                .andExpect(jsonPath("$.total.monto").value("12.34"));
        // La clave que el contrato exige llega a la reserva de la corrida, y la operacion se pide como API.
        verify(operaciones).devengar(eq(LocalDate.of(2026, 10, 12)), eq(CLAVE), eq("API"), any());
        mvc.perform(post("/inversiones/devengos")
                        .with(Sesiones.como("OPERACIONES", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"hoy\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CU-122 · reintentar las instrucciones exige ADMIN_PLATAFORMA, la clave y un limite del contrato")
    void reintento() throws Exception {
        when(operaciones.reintentarInstrucciones(eq(50), eq(true), any(), any(), any()))
                .thenReturn(Map.of(
                        "aplicadas",
                        "2",
                        "rechazadas",
                        "0",
                        "siguenPendientes",
                        "1",
                        "esperando",
                        "0",
                        "atascadas",
                        "1",
                        "cortada",
                        "false"));

        mvc.perform(post("/inversiones/instrucciones/reintento")
                        .with(Sesiones.como("OPERACIONES", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limite\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicadas").value(2))
                .andExpect(jsonPath("$.atascadas").value(1))
                .andExpect(jsonPath("$.cortada").value(false));
        verify(operaciones).reintentarInstrucciones(eq(50), eq(true), eq(CLAVE), eq("API"), any());

        mvc.perform(post("/inversiones/instrucciones/reintento")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/inversiones/instrucciones/reintento")
                        .with(Sesiones.como("OPERACIONES", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limite\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/inversiones/instrucciones/reintento")
                        .with(Sesiones.como("OPERACIONES", "ADMIN_PLATAFORMA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CU-123 · una clave repetida con otro contenido sale como 422 con su codigo del contrato")
    void claveConOtroContenido() throws Exception {
        when(operaciones.devengar(any(), any(), any(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(123, 3), "clave usada con otro contenido"));

        mvc.perform(post("/inversiones/devengos")
                        .with(Sesiones.como("OPERACIONES", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-10-12\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU123-03"));
    }

    @Test
    @DisplayName("CU-123 · las corridas de los trabajos las ve solo plataforma")
    void corridas() throws Exception {
        var ahora = OffsetDateTime.of(2026, 10, 12, 4, 30, 0, 0, ZoneOffset.UTC);
        when(operaciones.corridas(eq("DEVENGAR"), eq(10), any()))
                .thenReturn(List.of(new VistaCorrida(
                        POSICION,
                        "DEVENGAR",
                        "DEV:2026-10-12",
                        "PROGRAMADO",
                        "OK",
                        Map.of("devengadas", "3"),
                        null,
                        ahora,
                        ahora.plusSeconds(2))));

        mvc.perform(get("/inversiones/corridas")
                        .param("operacion", "DEVENGAR")
                        .param("limite", "10")
                        .with(Sesiones.como("OPERACIONES", "ADMIN_PLATAFORMA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corridas[0].estado").value("OK"))
                .andExpect(jsonPath("$.corridas[0].origen").value("PROGRAMADO"))
                .andExpect(jsonPath("$.corridas[0].resultado.devengadas").value("3"));
        mvc.perform(get("/inversiones/corridas").with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CU-120 · un producto que no existe sale como 422 AP-CU120-01")
    void productoInexistente() throws Exception {
        when(cu120.producto(eq("NO-EXISTE"), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(120, 1), "Ese producto no existe."));

        mvc.perform(get("/inversiones/productos/{codigo}", "NO-EXISTE")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU120-01"));
    }

    @Test
    @DisplayName("CU-126 · los intereses repartidos salen con su periodo, su retencion y el estado de la conciliacion")
    void distribuciones() throws Exception {
        var ahora = OffsetDateTime.of(2026, 10, 12, 4, 30, 0, 0, ZoneOffset.UTC);
        when(cu126.listar(eq(POSICION), any()))
                .thenReturn(List.of(new VistaDistribucion(
                        RESCATE,
                        RESCATE,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 10, 12),
                        new BigDecimal("100.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("13.00"),
                        new BigDecimal("87.00"),
                        "BOB",
                        "CONCILIADA",
                        null,
                        ahora)));

        mvc.perform(get("/inversiones/posiciones/{id}/distribuciones", POSICION)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distribuciones[0].estado").value("CONCILIADA"))
                .andExpect(jsonPath("$.distribuciones[0].retencion.monto").value("13.00"))
                .andExpect(jsonPath("$.distribuciones[0].interesNeto.monto").value("87.00"));
    }

    @Test
    @DisplayName("CU-127 · un producto sin comision configurada sale como 422 AP-CU127-01")
    void comisionNoConfigurada() throws Exception {
        when(cu127.estimar(eq(POSICION), any()))
                .thenThrow(
                        new ErrorDeNegocio(CodigoError.de(127, 1), "Este producto no cobra comision por resultado."));

        mvc.perform(get("/inversiones/posiciones/{id}/comision", POSICION)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU127-01"));
    }

    @Test
    @DisplayName("CU-128 · el reporte exige las dos fechas, rechaza un periodo invalido y el CSV sale como text/csv")
    void reporteFiscal() throws Exception {
        mvc.perform(get("/inversiones/reportes/fiscal")
                        .param("desde", "2026-01-01")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isBadRequest());
        when(cu128.reporte(eq(LocalDate.of(2026, 12, 31)), eq(LocalDate.of(2026, 1, 1)), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(128, 1), "El periodo es invalido."));
        mvc.perform(get("/inversiones/reportes/fiscal")
                        .param("desde", "2026-12-31")
                        .param("hasta", "2026-01-01")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU128-01"));

        when(cu128.exportarCsv(any(), any(), any())).thenReturn("fecha,comprobante\n");
        mvc.perform(get("/inversiones/reportes/fiscal/exportacion")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-12-31")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string("fecha,comprobante\n"));
        mvc.perform(get("/inversiones/reportes/fiscal")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-12-31")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName(
            "CU-129 · la simulacion no recibe ninguna cuenta, sale con usaSaldoReal=false y un importe invalido es 422")
    void simulacion() throws Exception {
        when(cu129.simular(any(), any()))
                .thenReturn(new CU129SimularCaja.Salida(
                        Dinero.de("1000.00", Moneda.BOB),
                        365,
                        List.of(SimulacionDeCaja.billetera(new BigDecimal("1000.00"))),
                        false,
                        "advertencia"));

        mvc.perform(post("/inversiones/simulaciones/caja")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIMULACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usaSaldoReal").value(false))
                .andExpect(jsonPath("$.alternativas[0].tipo").value("BILLETERA"))
                .andExpect(
                        jsonPath("$.alternativas[0].netoCuandoDisponible.monto").value("1000.00"));

        mvc.perform(post("/inversiones/simulaciones/caja")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + MONTO + ",\"horizonteDias\":365,\"cuentaBilleteraId\":\"" + CUENTA + "\"}"))
                // una cuenta no tiene donde llegar: la simulacion no recibe ninguna. H13.S1.M2: el campo de mas
                // se RECHAZA (400) en vez de ignorarse, y la simulacion NO se ejecuta una segunda vez.
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verify(cu129, org.mockito.Mockito.times(1)).simular(any(), any());
        mvc.perform(post("/inversiones/simulaciones/caja")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIMULACION.replace("365", "99999")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu121, cu124);
    }
}

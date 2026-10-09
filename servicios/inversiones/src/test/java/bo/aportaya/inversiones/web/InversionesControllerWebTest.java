package bo.aportaya.inversiones.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.inversiones.aplicacion.CU121OrdenarInversion;
import bo.aportaya.inversiones.aplicacion.CU123Devengo;
import bo.aportaya.inversiones.aplicacion.CU123Posiciones;
import bo.aportaya.inversiones.aplicacion.VistaRescate;
import bo.aportaya.inversiones.dominio.OrigenDatos;
import bo.aportaya.inversiones.dominio.TipoProducto;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

/** El contrato HTTP de InversionesController: ordenes, permisos que las separan, posiciones, rescates y devengo diario. */
@PruebaWeb(InversionesController.class)
class InversionesControllerWebTest extends BaseWebInversiones {

    @Test
    @DisplayName("CU-121 · 201 con el estado intermedio dicho y la clave de idempotencia llegando al caso de uso")
    void ordenar() throws Exception {
        when(cu121.ordenar(any(), any())).thenReturn(vistaDeOrden("ENVIADA"));

        mvc.perform(post("/inversiones/ordenes")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden("{\"monto\":\"1000.00\",\"moneda\":\"BOB\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ENVIADA"))
                .andExpect(jsonPath("$.monto.monto").value("1000.00"))
                .andExpect(jsonPath("$.consentimientoId").value(CONSENTIMIENTO.toString()))
                .andExpect(jsonPath("$.mensaje").value("mensaje"));

        ArgumentCaptor<CU121OrdenarInversion.EntradaOrden> entrada =
                ArgumentCaptor.forClass(CU121OrdenarInversion.EntradaOrden.class);
        ArgumentCaptor<ContextoSesion> sesion = ArgumentCaptor.forClass(ContextoSesion.class);
        verify(cu121).ordenar(entrada.capture(), sesion.capture());
        assertThat(entrada.getValue().clave()).isEqualTo(CLAVE);
        assertThat(entrada.getValue().monto().monto()).isEqualByComparingTo("1000.00");
        assertThat(entrada.getValue().textoHash()).isEqualTo(HASH);
        // La identidad es la de la sesion verificada; el cuerpo no trae ninguna.
        assertThat(sesion.getValue().usuarioId()).isEqualTo(Sesiones.USUARIO);
    }

    @ParameterizedTest(name = "400 · importe invalido: {0}")
    @ValueSource(
            strings = {
                "{\"monto\":1000,\"moneda\":\"BOB\"}",
                "{\"monto\":\"1000.001\",\"moneda\":\"BOB\"}",
                "{\"monto\":\"1000.00\"}",
                "{\"monto\":\"1000.00\",\"moneda\":\"EUR\"}",
                "{\"monto\":\"abc\",\"moneda\":\"BOB\"}"
            })
    @DisplayName(
            "CU-121 · un importe que no es cadena decimal al centavo con moneda del contrato no llega al caso de uso")
    void importeInvalido(String monto) throws Exception {
        mvc.perform(post("/inversiones/ordenes")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden(monto)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu121);
    }

    @Test
    @DisplayName("CU-121 · 400 sin Idempotency-Key, y 400 con un hash de texto que no es SHA-256")
    void sinClaveOHashInvalido() throws Exception {
        mvc.perform(post("/inversiones/ordenes")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden("{\"monto\":\"1000.00\",\"moneda\":\"BOB\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/inversiones/ordenes")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden("{\"monto\":\"1000.00\",\"moneda\":\"BOB\"}")
                                .replace(HASH, "zz")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu121);
    }

    @Test
    @DisplayName("CU-121 · una regla de negocio sale como 422 con su codigo AP-CU121-05 (saldo afectado a un pozo)")
    void saldoAfectadoAUnPozo() throws Exception {
        when(cu121.ordenar(any(), any()))
                .thenThrow(new ErrorDeNegocio(
                        CodigoError.de(121, 5), "Parte de tu saldo esta afectada a un pozo y no se puede invertir."));

        mvc.perform(post("/inversiones/ordenes")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden("{\"monto\":\"1000.00\",\"moneda\":\"BOB\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU121-05"));
    }

    @Test
    @DisplayName("Quien solo puede VER no puede ordenar ni rescatar; quien opera no administra el catalogo")
    void permisosSeparados() throws Exception {
        mvc.perform(post("/inversiones/ordenes")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden("{\"monto\":\"1000.00\",\"moneda\":\"BOB\"}")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/inversiones/posiciones/{id}/rescates", POSICION)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/inversiones/productos/sincronizacion")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isForbidden());
        mvc.perform(post("/inversiones/devengos")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-10-12\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu120, cu121, cu123, cu123Devengo, cu124);
    }

    @Test
    @DisplayName("CU-122 · ver una orden pasa SIEMPRE por la sesion: el caso de uso recibe al usuario autenticado")
    void verOrdenUsaLaSesion() throws Exception {
        when(cu122.ver(eq(ORDEN), any())).thenReturn(vistaDeOrden("CONFIRMADA"));

        mvc.perform(get("/inversiones/ordenes/{id}", ORDEN).with(Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        ArgumentCaptor<ContextoSesion> sesion = ArgumentCaptor.forClass(ContextoSesion.class);
        verify(cu122).ver(eq(ORDEN), sesion.capture());
        assertThat(sesion.getValue().usuarioId()).isEqualTo(Sesiones.OTRO_USUARIO);
    }

    @Test
    @DisplayName("CU-122 · sincronizar una orden y CU-125 sincronizar un rescate exigen BILLETERA_OPERAR")
    void sincronizaciones() throws Exception {
        when(cu122.sincronizar(eq(ORDEN), any())).thenReturn(vistaDeOrden("INCIERTA"));
        mvc.perform(post("/inversiones/ordenes/{id}/sincronizacion", ORDEN)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INCIERTA"));
        mvc.perform(post("/inversiones/rescates/{id}/sincronizacion", RESCATE)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cu125);
    }

    @Test
    @DisplayName(
            "CU-123 · la posicion de un fondo muestra fecha del dato, variacion NEGATIVA, dato desactualizado y advertencia, todo en cadenas")
    void posicionDeFondo() throws Exception {
        var valoracion = new CU123Posiciones.Valoracion(
                new BigDecimal("10.000000"),
                new BigDecimal("98.500000"),
                LocalDate.of(2026, 10, 12),
                5,
                true,
                new BigDecimal("985.00"),
                new BigDecimal("-15.00"),
                new BigDecimal("0.00"),
                new BigDecimal("985.00"),
                OrigenDatos.SINTETICO);
        when(cu123.ver(eq(POSICION), any()))
                .thenReturn(new CU123Posiciones.VistaPosicion(
                        POSICION,
                        TipoProducto.FONDO,
                        "FONDO-T",
                        "ABIERTA",
                        new BigDecimal("1000.00"),
                        "BOB",
                        LocalDate.of(2026, 10, 12),
                        null,
                        new BigDecimal("1000.00"),
                        Optional.of(valoracion),
                        Optional.empty(),
                        OrigenDatos.SINTETICO,
                        "Invertir puede generar perdidas."));

        mvc.perform(get("/inversiones/posiciones/{id}", POSICION).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valoracion.valorCuota").value("98.500000"))
                .andExpect(jsonPath("$.valoracion.variacion.monto").value("-15.00"))
                .andExpect(jsonPath("$.valoracion.desactualizado").value(true))
                .andExpect(jsonPath("$.valoracion.fechaValor").value("2026-10-12"))
                .andExpect(
                        jsonPath("$.valoracion.advertencia").value(org.hamcrest.Matchers.containsString("SINTETICO")))
                .andExpect(jsonPath("$.origenDatos").value("SINTETICO"))
                .andExpect(jsonPath("$.advertencia").value("Invertir puede generar perdidas."));
    }

    @Test
    @DisplayName("CU-124 · 201 con el corte y el estado PENDIENTE; las cuotas vienen como cadena a seis decimales")
    void rescate() throws Exception {
        when(cu124.solicitar(any(), any()))
                .thenReturn(new VistaRescate(
                        RESCATE,
                        POSICION,
                        "PARCIAL",
                        "PENDIENTE",
                        new BigDecimal("4.000000"),
                        LocalDate.of(2026, 10, 12),
                        null,
                        null,
                        null,
                        "Rescate pendiente: el dinero NO esta disponible hasta que el aliado confirme los fondos."));

        mvc.perform(post("/inversiones/posiciones/{id}/rescates", POSICION)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cuotas\":\"4.000000\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.corte.fechaValor").value("2026-10-12"))
                .andExpect(jsonPath("$.cuotas").value("4.000000"));
    }

    @ParameterizedTest(name = "400 · cuotas invalidas: {0}")
    @ValueSource(strings = {"4", "4.0", "-1.000000", "cuatro"})
    @DisplayName("CU-124 · cuotas que no son cadena decimal a seis decimales no llegan al caso de uso")
    void cuotasInvalidas(String cuotas) throws Exception {
        mvc.perform(post("/inversiones/posiciones/{id}/rescates", POSICION)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cuotas\":\"" + cuotas + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cu124);
    }

    @Test
    @DisplayName("CU-124 · el rechazo por doble disponibilidad sale como 422 AP-CU124-02")
    void dobleDisponibilidad() throws Exception {
        when(cu124.solicitar(any(), any())).thenThrow(new ErrorDeNegocio(CodigoError.de(124, 2), "ya comprometidas"));

        mvc.perform(post("/inversiones/posiciones/{id}/rescates", POSICION)
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cuotas\":\"4.000000\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU124-02"));
    }

    @Test
    @DisplayName("CU-123 · el devengo exige ADMIN_PLATAFORMA y una fecha del contrato")
    void devengo() throws Exception {
        when(cu123Devengo.devengar(any(), any()))
                .thenReturn(new CU123Devengo.SalidaDevengo(LocalDate.of(2026, 10, 12), 3, 1, new BigDecimal("12.34")));

        mvc.perform(post("/inversiones/devengos")
                        .with(Sesiones.como("OPERACIONES", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-10-12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devengadas").value(3))
                .andExpect(jsonPath("$.total.monto").value("12.34"));
        mvc.perform(post("/inversiones/devengos")
                        .with(Sesiones.como("OPERACIONES", "ADMIN_PLATAFORMA"))
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"hoy\"}"))
                .andExpect(status().isBadRequest());
    }
}

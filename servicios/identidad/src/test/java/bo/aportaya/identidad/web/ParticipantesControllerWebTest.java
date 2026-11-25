package bo.aportaya.identidad.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.identidad.aplicacion.CU134ConsultarParticipante;
import bo.aportaya.identidad.aplicacion.CU134GestionarMedida;
import bo.aportaya.identidad.infraestructura.MedidasDeCuentaRepositorio;
import bo.aportaya.identidad.infraestructura.ParticipantesRepositorio;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Matriz del contrato HTTP de CU-134 (ADR-043): camino feliz, validacion, 404 y permiso. */
@PruebaWeb(ParticipantesController.class)
class ParticipantesControllerWebTest {

    private static final UUID PARTICIPANTE = UUID.fromString("33333333-4444-4555-8666-777777777777");
    private static final UUID EXPEDIENTE = UUID.fromString("44444444-5555-4666-8777-888888888888");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-25T10:00:00-04:00");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU134ConsultarParticipante consulta;

    @MockitoBean
    private CU134GestionarMedida medidas;

    @Test
    @DisplayName("buscar: 200 con el directorio, sin contacto")
    void buscarOk() throws Exception {
        when(consulta.buscar(any(), anyString(), any()))
                .thenReturn(List.of(new CU134ConsultarParticipante.Resumen(fila(), "Juana P.")));

        mvc.perform(post("/identidad/participantes/busquedas")
                        .with(Sesiones.como("SOPORTE", "PARTICIPANTES_LEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificacion\":\"Ticket 1234 de cobro duplicado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filas[0].nombreAbreviado").value("Juana P."))
                .andExpect(jsonPath("$.filas[0].telefonoE164").doesNotExist());
    }

    @Test
    @DisplayName("buscar: 400 si falta la justificacion, sin llegar al caso de uso")
    void buscarSinJustificacion() throws Exception {
        mvc.perform(post("/identidad/participantes/busquedas")
                        .with(Sesiones.como("SOPORTE", "PARTICIPANTES_LEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(consulta);
    }

    @Test
    @DisplayName("ficha: 200 enmascarada, y 404 si no existe")
    void fichaOkY404() throws Exception {
        when(consulta.ficha(eq(PARTICIPANTE), anyString(), any(), anyString(), any()))
                .thenReturn(Optional.of(new CU134ConsultarParticipante.FichaEnmascarada(
                        fila(), "Juana Perez", AHORA, "+591 7•••••12", "j•••@e•••.bo", List.of())));

        mvc.perform(post("/identidad/participantes/{id}/ficha", PARTICIPANTE)
                        .with(Sesiones.como("SOPORTE", "PARTICIPANTES_LEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificacion\":\"Ticket 1234 de cobro duplicado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefonoEnmascarado").value("+591 7•••••12"));

        mvc.perform(post("/identidad/participantes/{id}/ficha", UUID.randomUUID())
                        .with(Sesiones.como("SOPORTE", "PARTICIPANTES_LEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificacion\":\"Ticket 1234 de cobro duplicado\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("revelar: 422 AP-CU134-03 sin segundo factor valido")
    void revelarSinSegundoFactor() throws Exception {
        when(consulta.revelar(any(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(134, 3), "Confirma con tu segundo factor."));

        mvc.perform(post("/identidad/participantes/{id}/revelaciones", PARTICIPANTE)
                        .with(Sesiones.como("OFICIAL_CUMPLIMIENTO", "DATOS_SENSIBLES_LEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campo\":\"TELEFONO\",\"justificacion\":\"Verificar titularidad\","
                                + "\"evidenciaMfa\":\"evidencia-vencida-de-prueba\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU134-03"));
    }

    @Test
    @DisplayName("proponer: 201 con Idempotency-Key, y 403 sin el permiso")
    void proponerOkY403() throws Exception {
        when(medidas.proponer(eq(PARTICIPANTE), any(), anyString(), any())).thenReturn(vista("CAUTELAR"));
        String cuerpo = "{\"causal\":\"CUENTA_COMPROMETIDA\",\"medida\":\"BLOQUEO_CAUTELAR\",\"cautelar\":true,"
                + "\"motivo\":\"Accesos desde tres paises en una hora con cambio de dispositivo\","
                + "\"evidencias\":[{\"descripcion\":\"Registro de intentos de acceso\"}]}";

        mvc.perform(post("/identidad/participantes/{id}/medidas", PARTICIPANTE)
                        .with(Sesiones.como("FRAUD_ANALYST", "PARTICIPANTES_MEDIDA_PROPONER"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CAUTELAR"))
                .andExpect(jsonPath("$.estadoCuenta").value("BLOQUEADO_SEGURIDAD"));

        mvc.perform(post("/identidad/participantes/{id}/medidas", PARTICIPANTE)
                        .with(Sesiones.como("SOPORTE", "PARTICIPANTES_LEER"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("consultar expediente: 200 con su linea de tiempo")
    void consultarExpedienteOk() throws Exception {
        when(medidas.consultar(eq(EXPEDIENTE), any())).thenReturn(Optional.of(vista("EN_DESCARGO")));

        mvc.perform(get("/identidad/medidas-cuenta/{id}", EXPEDIENTE).with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineaDeTiempo[0].estado").value("CAUTELAR"));
    }

    @Test
    @DisplayName("decidir: 200 con permiso de decision, y 422 AP-CU134-08 si decide quien propuso")
    void decidirOkYMismaPersona() throws Exception {
        String cuerpo = "{\"resultado\":\"CONFIRMADA\",\"fundamento\":\""
                + "La evidencia del sistema y el descargo se ponderaron y sostienen la medida propuesta.\","
                + "\"evidenciaMfa\":\"evidencia-de-step-up-de-prueba\"}";
        when(medidas.decidir(eq(EXPEDIENTE), anyString(), anyString(), anyString(), any()))
                .thenReturn(vista("DECIDIDA"))
                .thenThrow(new ErrorDeNegocio(CodigoError.de(134, 8), "Quien propuso no decide."));

        mvc.perform(post("/identidad/medidas-cuenta/{id}/decision", EXPEDIENTE)
                        .with(Sesiones.como("RESPONSABLE_SEGURIDAD", "PARTICIPANTES_MEDIDA_DECIDIR"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DECIDIDA"));

        mvc.perform(post("/identidad/medidas-cuenta/{id}/decision", EXPEDIENTE)
                        .with(Sesiones.como("RESPONSABLE_SEGURIDAD", "PARTICIPANTES_MEDIDA_DECIDIR"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU134-08"));
    }

    @Test
    @DisplayName("medida vigente: 200 con el booleano y sin la causal")
    void medidaVigente() throws Exception {
        when(consulta.medidaQueRetiene(eq(PARTICIPANTE), any())).thenReturn(Optional.of(EXPEDIENTE));

        mvc.perform(get("/identidad/participantes/{id}/medida-vigente", PARTICIPANTE)
                        .with(Sesiones.como("SERVICIO_ENTREGAS", "PARTICIPANTES_MEDIDA_VIGENTE_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retenerEntrega").value(true))
                .andExpect(jsonPath("$.causal").doesNotExist());
    }

    @Test
    @DisplayName("medida vigente: la credencial de entregas no abre la ficha ni el directorio (403)")
    void credencialDeEntregasNoLeeFichas() throws Exception {
        mvc.perform(post("/identidad/participantes/{id}/ficha", PARTICIPANTE)
                        .with(Sesiones.como("SERVICIO_ENTREGAS", "PARTICIPANTES_MEDIDA_VIGENTE_LEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificacion\":\"Ticket SOP-0001 de prueba\"}"))
                .andExpect(status().isForbidden());
    }

    private static ParticipantesRepositorio.Fila fila() {
        return new ParticipantesRepositorio.Fila(
                PARTICIPANTE, "AY-33333333", "Juana", "Perez", "ACTIVO", "BASICO", AHORA, false, false, false);
    }

    private static CU134GestionarMedida.Vista vista(String estado) {
        var e = new MedidasDeCuentaRepositorio.Expediente(
                EXPEDIENTE,
                PARTICIPANTE,
                UUID.randomUUID(),
                "CUENTA_COMPROMETIDA",
                "BLOQUEO_CAUTELAR",
                true,
                "Accesos anomalos",
                estado,
                "ACTIVO",
                UUID.randomUUID(),
                AHORA,
                null,
                null,
                null,
                null,
                null,
                null,
                AHORA.plusDays(90),
                AHORA.plusMonths(12),
                null,
                null);
        return new CU134GestionarMedida.Vista(
                e,
                List.of(new MedidasDeCuentaRepositorio.Hito(null, "CAUTELAR", null, AHORA)),
                Optional.empty(),
                "BLOQUEADO_SEGURIDAD");
    }
}

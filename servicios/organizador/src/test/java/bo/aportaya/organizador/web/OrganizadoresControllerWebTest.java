package bo.aportaya.organizador.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.organizador.aplicacion.CU90PostularOrganizador;
import bo.aportaya.organizador.aplicacion.CU91FirmarContrato;
import bo.aportaya.organizador.aplicacion.CU92EvaluarDesempeno;
import bo.aportaya.organizador.aplicacion.CU93SancionarOrganizador;
import bo.aportaya.organizador.aplicacion.ConsultarHabilitacion;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-90 a CU-93 · habilitar, contratar y sancionar a un organizador.
 *
 * <p>Un organizador administra plata de otros, así que este controlador es el que
 * implementa el {@code debido-proceso} completo: se postula, se aprueba, se firma, y si
 * incumple se lo sanciona <b>con derecho a apelar y con otro que resuelve</b>.
 *
 * <p>Lo que la capa web fija:
 *
 * <p><b>Postularse es del participante; aprobar es de la plataforma.</b> Si el mismo
 * permiso sirviera para los dos, cualquiera se habilitaría solo.
 *
 * <p><b>Firmar el contrato es del organizador, no de quien lo emite.</b> Una firma que
 * puede poner el emisor no es una firma.
 *
 * <p><b>La sanción dice hasta cuándo se puede apelar</b>, y quien apela no resuelve su
 * propia apelación.
 */
@PruebaWeb(OrganizadoresController.class)
class OrganizadoresControllerWebTest {

    private static final UUID ORGANIZADOR = UUID.fromString("b8000000-0000-4000-8000-000000000001");
    private static final UUID SOLICITUD = UUID.fromString("b8000000-0000-4000-8000-000000000002");
    private static final UUID CONTRATO = UUID.fromString("b8000000-0000-4000-8000-000000000003");
    private static final UUID SANCION = UUID.fromString("b8000000-0000-4000-8000-000000000004");
    private static final UUID KYC = UUID.fromString("b8000000-0000-4000-8000-000000000005");

    private static final String POSTULACION =
            """
            {"motivacion":"Quiero organizar el pasanaku de mi barrio",
             "experienciaDeclarada":"Tres anios llevando grupos informales",
             "kycReforzadoId":"b8000000-0000-4000-8000-000000000005",
             "reputacion":"87.50"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarHabilitacion habilitaciones;

    @MockitoBean
    private CU90PostularOrganizador cu90;

    @MockitoBean
    private CU91FirmarContrato cu91;

    @MockitoBean
    private CU92EvaluarDesempeno cu92;

    @MockitoBean
    private CU93SancionarOrganizador cu93;

    @Nested
    @DisplayName("CU-90 · postularse y ser habilitado son dos actos de dos personas")
    class Habilitacion {

        @Test
        @DisplayName("CU-90 · 201 con lo que TODAVÍA le falta al postulante")
        void postulacionDiceQueFalta() throws Exception {
            when(cu90.postular(any(), any()))
                    .thenReturn(new CU90PostularOrganizador.SalidaPostulacion(SOLICITUD, "PENDIENTE", true, List.of()));

            mvc.perform(post("/organizadores/postulaciones")
                            .with(Sesiones.como("PARTICIPANTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(POSTULACION))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.solicitudId").value(SOLICITUD.toString()))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                    // La lista de faltantes es lo que convierte un «no» en algo
                    // accionable: sin ella, el postulante no sabe que corregir.
                    .andExpect(jsonPath("$.faltantes").isArray());
        }

        @Test
        @DisplayName("CU-90 · postularse dos veces responde 200: no se abre otra solicitud")
        void postularDosVeces() throws Exception {
            when(cu90.postular(any(), any()))
                    .thenReturn(
                            new CU90PostularOrganizador.SalidaPostulacion(SOLICITUD, "PENDIENTE", false, List.of()));

            mvc.perform(post("/organizadores/postulaciones")
                            .with(Sesiones.como("PARTICIPANTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(POSTULACION))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.esNueva").value(false));
        }

        @Test
        @DisplayName("CU-90 · un participante NO se aprueba a sí mismo la postulación")
        void nadieSeApruebaSolo() throws Exception {
            mvc.perform(post("/organizadores/postulaciones/{id}/aprobacion", SOLICITUD)
                            .with(Sesiones.como("PARTICIPANTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"medidos\":{}}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu90);
        }

        @Test
        @DisplayName("CU-90 · 400: sin el KYC reforzado, que el contrato exige")
        void sinKycReforzado() throws Exception {
            mvc.perform(post("/organizadores/postulaciones")
                            .with(Sesiones.como("PARTICIPANTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(POSTULACION.replace(
                                    "\"kycReforzadoId\":\"b8000000-0000-4000-8000-000000000005\",", "")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu90);
        }

        @Test
        @DisplayName("CU-90 · la habilitación publica los límites: cuántos grupos y hasta qué monto")
        void habilitacionConSusLimites() throws Exception {
            when(habilitaciones.ejecutar(any(), any()))
                    .thenReturn(new ConsultarHabilitacion.Habilitacion(
                            true, "INTERMEDIO", 5, new BigDecimal("50000.00"), 3));

            mvc.perform(get("/organizadores/{id}/habilitacion", ORGANIZADOR)
                            .with(Sesiones.como("ORGANIZADOR", "GRUPO_CREAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.habilitado").value(true))
                    .andExpect(jsonPath("$.nivel").value("INTERMEDIO"))
                    // Los tres numeros con los que `grupos` decide si le deja crear uno
                    // mas, sin leer este esquema (invariante 11).
                    .andExpect(jsonPath("$.limiteDeGrupos").value(5))
                    .andExpect(jsonPath("$.limiteDeMonto").value("50000.00"))
                    .andExpect(jsonPath("$.gruposActivos").value(3));
        }
    }

    @Nested
    @DisplayName("CU-91 · quien emite el contrato no lo firma")
    class Contrato {

        @Test
        @DisplayName("CU-91 · firmar es del ORGANIZADOR: quien lo emitió no puede firmarlo por él")
        void elEmisorNoFirma() throws Exception {
            mvc.perform(post("/organizadores/contratos/{id}/firma", CONTRATO)
                            .with(Sesiones.como("ADMIN_PLATAFORMA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenFirmaId\":\"b8000000-0000-4000-8000-00000000000a\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu91);
        }

        @Test
        @DisplayName("CU-91 · el organizador firma y queda la constancia de cuándo")
        void elOrganizadorFirma() throws Exception {
            when(cu91.firmar(any(), any(), any()))
                    .thenReturn(new CU91FirmarContrato.SalidaFirma(
                            CONTRATO, OffsetDateTime.of(2026, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC), true));

            mvc.perform(post("/organizadores/contratos/{id}/firma", CONTRATO)
                            .with(Sesiones.como("ORGANIZADOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenFirmaId\":\"b8000000-0000-4000-8000-00000000000a\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contratoId").value(CONTRATO.toString()))
                    // Sin la fecha, no hay forma de saber desde cuando obliga.
                    .andExpect(jsonPath("$.firmadoEn").exists());
        }
    }
}

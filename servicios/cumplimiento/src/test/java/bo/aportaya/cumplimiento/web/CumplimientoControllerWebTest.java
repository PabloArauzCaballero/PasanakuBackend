package bo.aportaya.cumplimiento.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.cumplimiento.aplicacion.CU02ElevarDiligencia;
import bo.aportaya.cumplimiento.aplicacion.CU03DeclararPep;
import bo.aportaya.cumplimiento.aplicacion.CU05AceptarContrato;
import bo.aportaya.cumplimiento.aplicacion.CU05ConsultarContratosVigentes;
import bo.aportaya.cumplimiento.aplicacion.CU54RegistrarRiesgoOperativo;
import bo.aportaya.cumplimiento.aplicacion.CU55GestionarIncidente;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato HTTP de {@code /cumplimiento}.
 *
 * <p>Cinco permisos, y los tres del final —analista, riesgos, seguridad— corresponden a
 * tres funciones que la norma exige separadas. Que el participante no pueda elevarse su
 * propio nivel de diligencia es el más importante de los tres: sería subirse los
 * límites solo.
 *
 * <p>Lo demás que se fija acá son <b>plazos</b>. Un incidente de seguridad y un evento
 * de riesgo operativo tienen plazo regulatorio de reporte, y la respuesta lo publica:
 * un plazo que solo vive en la base es un plazo que se vence sin que nadie lo vea.
 */
@PruebaWeb(CumplimientoController.class)
class CumplimientoControllerWebTest {

    private static final UUID USUARIO = UUID.fromString("c1000000-0000-4000-8000-000000000001");
    private static final UUID CONTRATO = UUID.fromString("c1000000-0000-4000-8000-000000000002");
    private static final UUID EVENTO = UUID.fromString("c1000000-0000-4000-8000-000000000003");
    private static final UUID INCIDENTE = UUID.fromString("c1000000-0000-4000-8000-000000000004");
    private static final UUID PLAN = UUID.fromString("c1000000-0000-4000-8000-000000000005");
    private static final String CLAVE = "c1000000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

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

    @Nested
    @DisplayName("CU-05 y CU-02 · nadie se eleva su propio nivel de diligencia")
    class Adhesion {

        @Test
        @DisplayName("CU-05 · aceptar el contrato devuelve el HASH de la evidencia y cuándo se aceptó")
        void aceptarContrato() throws Exception {
            when(cu05.ejecutar(any(), any()))
                    .thenReturn(new CU05AceptarContrato.SalidaAceptacion(
                            UUID.fromString("c1000000-0000-4000-8000-00000000000a"),
                            "ab".repeat(32),
                            OffsetDateTime.of(2026, 3, 15, 14, 30, 0, 0, ZoneOffset.UTC)));

            mvc.perform(
                            post("/cumplimiento/contratos/{id}/aceptaciones", CONTRATO)
                                    .with(Sesiones.como("PARTICIPANTE"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"usuarioId":"c1000000-0000-4000-8000-000000000001","version":3,
                                     "consentimientos":[]}
                                    """))
                    .andExpect(status().isCreated())
                    // El hash es la evidencia de QUE texto acepto: sin el, una version
                    // posterior del contrato reemplaza en silencio lo que se firmo.
                    .andExpect(jsonPath("$.hashEvidencia").value("ab".repeat(32)))
                    .andExpect(jsonPath("$.aceptadoEn").exists());
        }

        @Test
        @DisplayName("CU-02 · un PARTICIPANTE no puede elevar su propio nivel de diligencia")
        void nadieSeElevaSolo() throws Exception {
            // Elevar la diligencia sube los limites de operacion. Que lo pudiera pedir
            // el propio interesado seria subirse el techo sin que nadie lo evalue.
            mvc.perform(post("/cumplimiento/usuarios/{id}/diligencia", USUARIO)
                            .with(Sesiones.como("PARTICIPANTE"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu02);
        }

        @Test
        @DisplayName("CU-03 · declarar PEP es del propio usuario: el analista no declara por él")
        void elAnalistaNoDeclaraPorElUsuario() throws Exception {
            mvc.perform(post("/cumplimiento/usuarios/{id}/pep", USUARIO)
                            .with(Sesiones.como("ANALISTA_CUMPLIMIENTO"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"esPep\":true}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu03);
        }
    }

    @Nested
    @DisplayName("CU-54 y CU-55 · los plazos regulatorios viajan en la respuesta")
    class Plazos {

        @Test
        @DisplayName("CU-54 · el evento de riesgo devuelve la pérdida NETA y el plan de acción que abrió")
        void riesgoOperativo() throws Exception {
            when(cu54.ejecutar(any(), any()))
                    .thenReturn(new CU54RegistrarRiesgoOperativo.SalidaEvento(
                            EVENTO, "ROP-2026-0042", "12500.00", "BOB", Optional.of(PLAN)));

            mvc.perform(
                            post("/cumplimiento/riesgos/eventos")
                                    .with(Sesiones.como("RESPONSABLE_RIESGOS"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"categoriaEvento":"FALLAS_SISTEMAS","factorRiesgo":"TECNOLOGIA_INFORMACION",
                                     "lineaNegocio":"BILLETERA","descripcion":"Caida del proveedor de QR por seis horas",
                                     "fechaOcurrencia":"2026-03-10T08:00:00Z","fechaDeteccion":"2026-03-10T08:30:00Z",
                                     "perdidaBruta":"15000.00","recuperacion":"2500.00","moneda":"BOB"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigo").value("ROP-2026-0042"))
                    // NETA: la bruta menos lo recuperado. Reportar la bruta infla la
                    // perdida del periodo y la neta sin decirlo la esconde.
                    .andExpect(jsonPath("$.perdidaNeta").value("12500.00"))
                    .andExpect(jsonPath("$.moneda").value("BOB"))
                    .andExpect(jsonPath("$.planAccionId").value(PLAN.toString()));
        }

        @Test
        @DisplayName("CU-54 · 400: una categoría de evento fuera del catálogo del regulador")
        void categoriaFueraDelCatalogo() throws Exception {
            mvc.perform(
                            post("/cumplimiento/riesgos/eventos")
                                    .with(Sesiones.como("RESPONSABLE_RIESGOS"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"categoriaEvento":"MALA_SUERTE","factorRiesgo":"PERSONAS",
                                     "lineaNegocio":"BILLETERA","descripcion":"x",
                                     "fechaOcurrencia":"2026-03-10T08:00:00Z","fechaDeteccion":"2026-03-10T08:30:00Z",
                                     "perdidaBruta":"1.00","moneda":"BOB"}
                                    """))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu54);
        }

        @Test
        @DisplayName("CU-55 · el incidente devuelve su PLAZO de reporte y si hay que avisar a los titulares")
        void incidenteConPlazos() throws Exception {
            when(cu55.registrar(any(), any()))
                    .thenReturn(new CU55GestionarIncidente.SalidaIncidente(
                            INCIDENTE,
                            "INC-2026-0007",
                            OffsetDateTime.of(2026, 3, 11, 8, 0, 0, 0, ZoneOffset.UTC),
                            true,
                            Optional.of(OffsetDateTime.of(2026, 3, 13, 8, 0, 0, 0, ZoneOffset.UTC)),
                            Optional.empty(),
                            OffsetDateTime.of(2026, 3, 10, 8, 30, 0, 0, ZoneOffset.UTC)));

            mvc.perform(
                            post("/cumplimiento/seguridad/incidentes")
                                    .with(Sesiones.como("RESPONSABLE_SEGURIDAD"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"tipo":"FUGA_DE_DATOS","severidad":"CRITICA",
                                     "datosPersonalesAfectados":true,"usuariosAfectados":1200,
                                     "detectadoEn":"2026-03-10T08:30:00Z"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigo").value("INC-2026-0007"))
                    // Los dos plazos: al organismo y a los titulares. Son distintos y
                    // corren en paralelo; publicarlos es lo que permite cumplirlos.
                    .andExpect(jsonPath("$.plazoReporte").exists())
                    .andExpect(jsonPath("$.requiereNotificarTitulares").value(true))
                    .andExpect(jsonPath("$.plazoNotificacion").exists());
        }

        @Test
        @DisplayName("CU-55 · sin datos personales afectados NO hay plazo de notificación a titulares")
        void sinDatosPersonalesNoHayPlazoDeNotificacion() throws Exception {
            when(cu55.registrar(any(), any()))
                    .thenReturn(new CU55GestionarIncidente.SalidaIncidente(
                            INCIDENTE,
                            "INC-2026-0008",
                            OffsetDateTime.of(2026, 3, 11, 8, 0, 0, 0, ZoneOffset.UTC),
                            false,
                            Optional.empty(),
                            Optional.empty(),
                            OffsetDateTime.of(2026, 3, 10, 8, 30, 0, 0, ZoneOffset.UTC)));

            mvc.perform(
                            post("/cumplimiento/seguridad/incidentes")
                                    .with(Sesiones.como("RESPONSABLE_SEGURIDAD"))
                                    .header("Idempotency-Key", CLAVE)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"tipo":"DENEGACION_SERVICIO","severidad":"MEDIA",
                                     "datosPersonalesAfectados":false,"usuariosAfectados":0,
                                     "detectadoEn":"2026-03-10T08:30:00Z"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.requiereNotificarTitulares").value(false))
                    .andExpect(jsonPath("$.plazoNotificacion").doesNotExist());
        }

        @Test
        @DisplayName("CU-55 · reportar al organismo responde 204: la constancia queda del lado del servicio")
        void reportarAlOrganismo() throws Exception {
            mvc.perform(post("/cumplimiento/seguridad/incidentes/{id}/reportes", INCIDENTE)
                            .with(Sesiones.como("RESPONSABLE_SEGURIDAD"))
                            .header("Idempotency-Key", CLAVE))
                    .andExpect(status().isNoContent());

            verify(cu55).reportarAlOrganismo(eq(INCIDENTE), any());
        }

        @Test
        @DisplayName("CU-55 · registrar un incidente de seguridad NO es del responsable de riesgos")
        void seguridadYRiesgosSonFuncionesDistintas() throws Exception {
            // La norma las quiere separadas: quien mide la perdida no es quien declara
            // que hubo una brecha.
            mvc.perform(post("/cumplimiento/seguridad/incidentes")
                            .with(Sesiones.como("RESPONSABLE_RIESGOS"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu55);
        }
    }
}

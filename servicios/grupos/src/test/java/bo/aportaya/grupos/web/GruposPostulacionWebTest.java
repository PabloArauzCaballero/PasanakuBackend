package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69Enlace;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
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
 * CU-68 · postularse a un grupo, y lo que este servicio le contesta a los otros.
 *
 * <p>Separado de {@link GruposControllerWebTest} por la regla de tamano de archivo del
 * proyecto: son dos temas distintos —el alta de un grupo y el ingreso de una persona— y
 * juntos pasaban las trescientas lineas.
 *
 * <p>Lo que se fija aca: <b>la postulacion devuelve el puntaje y sus motivos</b>. Un
 * rechazo sin motivos es un rechazo que no se puede discutir. Y los tres hechos que
 * deciden —politica del grupo, restriccion vigente, reputacion— los resuelve el
 * servicio antes de llamar al caso de uso; ninguno sale del cuerpo de la peticion.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
class GruposPostulacionWebTest {

    private static final UUID GRUPO = UUID.fromString("c4000000-0000-4000-8000-000000000001");
    private static final UUID SOLICITUD = UUID.fromString("c4000000-0000-4000-8000-000000000002");
    private static final String CLAVE = "c4000000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU20CrearGrupo cu20;

    @MockitoBean
    private CU59CalcularPlazo cu59;

    @MockitoBean
    private CU64TraspasarCupo cu64;

    @MockitoBean
    private CU65Retirarse cu65;

    @MockitoBean
    private CU68Postular cu68;

    @MockitoBean
    private CU69Invitar cu69;

    @MockitoBean
    private CU69Enlace enlaces;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    @MockitoBean
    private RespuestasAOtrosServicios respuestas;

    @MockitoBean
    private SorteoDelGrupo sorteo;

    @Nested
    @DisplayName("CU-68 · postularse a un grupo")
    class Postular {

        @Test
        @DisplayName("CU-68 · 201 con el puntaje y LOS MOTIVOS: un rechazo sin motivos no se discute")
        void postulacionConMotivos() throws Exception {
            // Los tres hechos que la pagina resuelve ANTES de llamar al caso de uso: la
            // politica del grupo, la restriccion del postulante y su reputacion. Ninguno
            // sale del cuerpo.
            when(consultas.politicaDelGrupo(any(), any()))
                    .thenReturn(java.util.Optional.of(new bo.aportaya.grupos.dominio.PoliticaDelGrupo(
                            "ESTANDAR", 60, new BigDecimal("0.66"), 2)));
            when(afuera.restriccion(any())).thenReturn(new HechosDeOtrosServicios.Restriccion(false, BigDecimal.ZERO));
            when(afuera.reputacion(any()))
                    .thenReturn(new HechosDeOtrosServicios.Reputacion(true, new BigDecimal("87")));
            when(afuera.morososDelGrupo(any())).thenReturn(0);

            when(cu68.postular(any(), any()))
                    .thenReturn(new CU68Postular.SalidaPostulacion(
                            SOLICITUD,
                            new BigDecimal("72.50"),
                            List.of("reputacion suficiente", "sin restriccion vigente")));

            mvc.perform(post("/grupos/{id}/postulaciones", GRUPO)
                            .with(Sesiones.como("PARTICIPANTE"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cuposSolicitados\":1,\"mensaje\":\"Me lo recomendo una vecina\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.solicitudId").value(SOLICITUD.toString()))
                    // Cadena decimal: es un puntaje con el que se decide sobre personas.
                    .andExpect(jsonPath("$.puntaje").value("72.50"))
                    .andExpect(jsonPath("$.motivos[0]").value("reputacion suficiente"));
        }

        @Test
        @DisplayName("CU-68 · 400: sin cupos solicitados, que el contrato exige")
        void sinCuposSolicitados() throws Exception {
            mvc.perform(post("/grupos/{id}/postulaciones", GRUPO)
                            .with(Sesiones.como("PARTICIPANTE"))
                            .header("Idempotency-Key", CLAVE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mensaje\":\"quiero entrar\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu68);
        }
    }

    @Test
    @DisplayName("resolver un alias contesta si existe y su identificador, y nada más")
    void resolverAlias() throws Exception {
        when(respuestas.resolverAlias(any(), any()))
                .thenReturn(org.springframework.http.ResponseEntity.ok(
                        new bo.aportaya.grupos.web.generado.modelo.AliasResuelto()));

        mvc.perform(get("/grupos/participantes/alias/{alias}", "pablo.a")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isOk());

        // El alias llega entero: es lo unico con lo que `nucleo-financiero` puede
        // resolver a quien transferir.
        verify(respuestas).resolverAlias(org.mockito.ArgumentMatchers.eq("pablo.a"), any());
    }
}

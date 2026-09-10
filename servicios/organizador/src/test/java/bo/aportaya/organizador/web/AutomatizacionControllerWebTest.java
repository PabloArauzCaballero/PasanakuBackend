package bo.aportaya.organizador.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.organizador.aplicacion.CU95DefinirAutomatizacion;
import bo.aportaya.organizador.aplicacion.CU96EjecutarTarea;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-95 y CU-96 · las reglas que hacen cosas solas, y las tareas que producen.
 *
 * <p>Una regla de automatización mueve plata sin que nadie apriete un botón, y por eso
 * el contrato tiene dos cerrojos que esta clase fija.
 *
 * <p><b>Definir y activar una regla son de ADMIN_PLATAFORMA; programar y ejecutar tareas
 * son del ORGANIZADOR.</b> Quien opera el día a día no puede además reescribir la regla
 * que lo controla.
 *
 * <p><b>La regla declara si requiere confirmación humana, y la respuesta lo repite al
 * activarla.</b> Una regla que cobra sola y no dice que cobra sola es la forma más
 * rápida de vaciar cuentas por un `cron` mal escrito.
 */
@PruebaWeb(AutomatizacionController.class)
class AutomatizacionControllerWebTest {

    private static final UUID REGLA = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID TAREA = UUID.fromString("b9000000-0000-4000-8000-000000000002");
    private static final UUID GRUPO = UUID.fromString("b9000000-0000-4000-8000-000000000003");

    private static final String REGLA_NUEVA =
            """
            {"codigo":"MORA_DIA_5","descripcion":"Aplica mora al quinto dia de vencido",
             "disparador":"CRON","expresionDisparo":"0 0 6 * * *",
             "condicion":"diasVencido >= 5","accion":"APLICAR_MORA",
             "requiereConfirmacionHumana":true,"prioridad":10}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU95DefinirAutomatizacion cu95;

    @MockitoBean
    private CU96EjecutarTarea cu96;

    @Nested
    @DisplayName("CU-95 · definir y activar una regla es de la plataforma")
    class Reglas {

        @Test
        @DisplayName("CU-95 · 201, y la respuesta repite si la regla exige confirmación humana")
        void definirRegla() throws Exception {
            when(cu95.definir(any(), any()))
                    .thenReturn(new CU95DefinirAutomatizacion.SalidaRegla(REGLA, "MORA_DIA_5", false, true));

            mvc.perform(post("/automatizacion/reglas")
                            .with(Sesiones.como("ADMIN_PLATAFORMA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGLA_NUEVA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reglaId").value(REGLA.toString()))
                    .andExpect(jsonPath("$.codigo").value("MORA_DIA_5"))
                    // Nace INACTIVA: definir no es activar, y una regla que arranca
                    // corriendo se estrena en produccion sin que nadie la haya simulado.
                    .andExpect(jsonPath("$.activa").value(false))
                    .andExpect(jsonPath("$.requiereConfirmacionHumana").value(true));
        }

        @Test
        @DisplayName("CU-95 · la expresión y la condición llegan al caso de uso tal cual se escribieron")
        void laExpresionLlegaEntera() throws Exception {
            when(cu95.definir(any(), any()))
                    .thenReturn(new CU95DefinirAutomatizacion.SalidaRegla(REGLA, "MORA_DIA_5", false, true));

            mvc.perform(post("/automatizacion/reglas")
                            .with(Sesiones.como("ADMIN_PLATAFORMA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGLA_NUEVA))
                    .andExpect(status().isCreated());

            var capturada = ArgumentCaptor.forClass(CU95DefinirAutomatizacion.EntradaRegla.class);
            verify(cu95).definir(capturada.capture(), any());
            var entrada = capturada.getValue();

            // Una expresion recortada o reescrita en la pagina es una regla que hace
            // otra cosa de la que alguien aprobo.
            org.assertj.core.api.Assertions.assertThat(entrada.expresionDisparo())
                    .isEqualTo("0 0 6 * * *");
            org.assertj.core.api.Assertions.assertThat(entrada.condicion()).isEqualTo("diasVencido >= 5");
            org.assertj.core.api.Assertions.assertThat(entrada.accion()).isEqualTo("APLICAR_MORA");
            org.assertj.core.api.Assertions.assertThat(entrada.requiereConfirmacionHumana())
                    .isTrue();
        }

        @Test
        @DisplayName("CU-95 · 400: una acción que el catálogo cerrado del contrato no enumera")
        void accionFueraDelCatalogo() throws Exception {
            // El catalogo de acciones es cerrado a proposito: una regla no ejecuta
            // codigo arbitrario, elige de una lista.
            mvc.perform(post("/automatizacion/reglas")
                            .with(Sesiones.como("ADMIN_PLATAFORMA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGLA_NUEVA.replace("APLICAR_MORA", "EJECUTAR_SQL")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu95);
        }

        @Test
        @DisplayName("CU-95 · 400: un disparador que el contrato no enumera")
        void disparadorFueraDelContrato() throws Exception {
            mvc.perform(post("/automatizacion/reglas")
                            .with(Sesiones.como("ADMIN_PLATAFORMA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGLA_NUEVA.replace("\"disparador\":\"CRON\"", "\"disparador\":\"WEBHOOK\"")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu95);
        }

        @Test
        @DisplayName("CU-95 · un ORGANIZADOR no define ni activa reglas: solo las ejecuta")
        void elOrganizadorNoEscribeLasReglas() throws Exception {
            mvc.perform(post("/automatizacion/reglas")
                            .with(Sesiones.como("ORGANIZADOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGLA_NUEVA))
                    .andExpect(status().isForbidden());

            mvc.perform(post("/automatizacion/reglas/{id}/activacion", REGLA).with(Sesiones.como("ORGANIZADOR")))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(cu95);
        }

        @Test
        @DisplayName("CU-95 · activar devuelve la regla ya activa")
        void activar() throws Exception {
            when(cu95.activar(eq(REGLA), any()))
                    .thenReturn(new CU95DefinirAutomatizacion.SalidaRegla(REGLA, "MORA_DIA_5", true, true));

            mvc.perform(post("/automatizacion/reglas/{id}/activacion", REGLA).with(Sesiones.como("ADMIN_PLATAFORMA")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activa").value(true));
        }
    }

    @Nested
    @DisplayName("CU-96 · las tareas que la regla produce")
    class Tareas {

        @Test
        @DisplayName("CU-96 · la tarea nace con su clave de idempotencia: correrla dos veces no duplica")
        void tareaConClave() throws Exception {
            when(cu96.programar(any(), any()))
                    .thenReturn(new CU96EjecutarTarea.SalidaProgramacion(
                            TAREA, "REQUIERE_APROBACION", "auto-MORA_DIA_5-2026-03-15", true));

            mvc.perform(
                            post("/automatizacion/tareas")
                                    .with(Sesiones.como("ORGANIZADOR"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"reglaId":"b9000000-0000-4000-8000-000000000001",
                                     "grupoId":"b9000000-0000-4000-8000-000000000003",
                                     "programadaPara":"2026-03-15T06:00:00Z"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tareaId").value(TAREA.toString()))
                    // Derivada del hecho disparador, no aleatoria: es lo que hace que
                    // dos replicas del worker produzcan un efecto y no dos.
                    .andExpect(jsonPath("$.claveIdempotencia").value("auto-MORA_DIA_5-2026-03-15"))
                    // Y si la regla exige confirmacion, la tarea espera en vez de correr.
                    .andExpect(jsonPath("$.estado").value("REQUIERE_APROBACION"));
        }

        @Test
        @DisplayName("CU-96 · 400: sin `programadaPara`, que el contrato exige")
        void faltaLaFechaDeProgramacion() throws Exception {
            mvc.perform(
                            post("/automatizacion/tareas")
                                    .with(Sesiones.como("ORGANIZADOR"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"reglaId":"b9000000-0000-4000-8000-000000000001",
                                     "grupoId":"b9000000-0000-4000-8000-000000000003"}
                                    """))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu96);
        }

        @Test
        @DisplayName("CU-96 · un fallo se anota con su número de intento y el mensaje del error")
        void ejecucionFallida() throws Exception {
            when(cu96.anotarEjecucion(any(), any()))
                    .thenReturn(new CU96EjecutarTarea.SalidaEjecucion(
                            UUID.fromString("b9000000-0000-4000-8000-00000000000e"), "FALLIDA", 2, true));

            mvc.perform(
                            post("/automatizacion/tareas/{id}/ejecuciones", TAREA)
                                    .with(Sesiones.como("ORGANIZADOR"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"iniciada":"2026-03-15T06:00:00Z","resultado":"ERROR",
                                     "registrosAfectados":0,"detalleJson":"{}",
                                     "mensajeError":"la obligacion ya estaba pagada"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estadoDeLaTarea").value("FALLIDA"))
                    // Sin el numero de intento, un trabajo que falla en bucle no se
                    // distingue de uno que fallo una vez.
                    .andExpect(jsonPath("$.intentos").value(2));

            var capturada = ArgumentCaptor.forClass(CU96EjecutarTarea.EntradaEjecucion.class);
            verify(cu96).anotarEjecucion(capturada.capture(), any());
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().mensajeError())
                    .isEqualTo("la obligacion ya estaba pagada");
            org.assertj.core.api.Assertions.assertThat(capturada.getValue().tareaId())
                    .isEqualTo(TAREA);
        }

        @Test
        @DisplayName("CU-96 · 400: un resultado de ejecución que el contrato no enumera")
        void resultadoFueraDelContrato() throws Exception {
            mvc.perform(
                            post("/automatizacion/tareas/{id}/ejecuciones", TAREA)
                                    .with(Sesiones.como("ORGANIZADOR"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {"iniciada":"2026-03-15T06:00:00Z","resultado":"MAS_O_MENOS",
                                     "registrosAfectados":0,"detalleJson":"{}"}
                                    """))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu96);
        }
    }
}

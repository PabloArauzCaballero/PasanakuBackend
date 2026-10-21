package bo.aportaya.auditoria.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.auditoria.aplicacion.ConsultarBitacora;
import bo.aportaya.auditoria.web.generado.modelo.EventoBitacora;
import bo.aportaya.auditoria.web.generado.modelo.PaginaDeBitacora;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
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

@PruebaWeb(AuditoriaController.class)
class BitacoraControllerWebTest {

    private static final UUID EVENTO_ID = UUID.fromString("b17a0000-0000-4000-8000-000000000001");
    private static final UUID ENTIDAD_ID = UUID.fromString("e17a0000-0000-4000-8000-000000000001");
    private static final UUID CORRELATION_ID = UUID.fromString("c17a0000-0000-4000-8000-000000000001");
    private static final OffsetDateTime FECHA = OffsetDateTime.of(2026, 10, 18, 14, 30, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarBitacora consultarBitacora;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.GestionarCatalogosOperativos catalogosCasoDeUso;

    @MockitoBean
    private bo.aportaya.auditoria.aplicacion.CU07EjercerDerechos cu07;

    @Test
    @DisplayName("401: sin sesion no lee la bitacora")
    void sinSesionRechaza() throws Exception {
        mvc.perform(get("/auditoria/bitacora")).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultarBitacora);
    }

    @Test
    @DisplayName("403: sin permiso AUDITORIA_LEER no lee la bitacora")
    void sinPermisoRechaza() throws Exception {
        mvc.perform(get("/auditoria/bitacora").with(Sesiones.como("PARTICIPANTE", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(consultarBitacora);
    }

    @Test
    @DisplayName("200: con AUDITORIA_LEER lista eventos de la bitacora")
    void listarEventosExitoso() throws Exception {
        EventoBitacora evento = new EventoBitacora(
                EVENTO_ID,
                101L,
                "USUARIO",
                ENTIDAD_ID,
                "ACTUALIZACION",
                "PANEL_ADMIN",
                CORRELATION_ID,
                "hash1234567890abcdef",
                FECHA);
        evento.setActorRol("ADMIN_PLATAFORMA");
        evento.setMotivo("Actualizacion de perfil");

        PaginaDeBitacora pagina = new PaginaDeBitacora(List.of(evento));
        pagina.setTotalAproximado(1);

        when(consultarBitacora.listar(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pagina);

        mvc.perform(get("/auditoria/bitacora")
                        .param("entidad", "USUARIO")
                        .param("limite", "10")
                        .with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(EVENTO_ID.toString()))
                .andExpect(jsonPath("$.items[0].entidad").value("USUARIO"))
                .andExpect(jsonPath("$.items[0].accion").value("ACTUALIZACION"))
                .andExpect(jsonPath("$.items[0].actorRol").value("ADMIN_PLATAFORMA"))
                .andExpect(jsonPath("$.items[0].hashRegistro").value("hash1234567890abcdef"))
                .andExpect(jsonPath("$.totalAproximado").value(1));
    }

    @Test
    @DisplayName("200: consulta un evento de bitacora por ID")
    void consultarEventoPorId() throws Exception {
        EventoBitacora evento = new EventoBitacora(
                EVENTO_ID,
                101L,
                "CONFIGURACION",
                ENTIDAD_ID,
                "CAMBIO_CONFIGURACION",
                "PANEL_ADMIN",
                CORRELATION_ID,
                "hashRegistroX",
                FECHA);
        evento.setHashAnterior("hashAnteriorY");

        when(consultarBitacora.buscarPorId(eq(EVENTO_ID), any())).thenReturn(Optional.of(evento));

        mvc.perform(get("/auditoria/bitacora/{id}", EVENTO_ID).with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EVENTO_ID.toString()))
                .andExpect(jsonPath("$.entidad").value("CONFIGURACION"))
                .andExpect(jsonPath("$.hashRegistro").value("hashRegistroX"))
                .andExpect(jsonPath("$.hashAnterior").value("hashAnteriorY"));
    }

    @Test
    @DisplayName("404: evento de bitacora inexistente")
    void consultarEventoInexistente() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        when(consultarBitacora.buscarPorId(eq(idInexistente), any())).thenReturn(Optional.empty());

        mvc.perform(get("/auditoria/bitacora/{id}", idInexistente).with(Sesiones.como("AUDITOR", "AUDITORIA_LEER")))
                .andExpect(status().isNotFound());
    }
}

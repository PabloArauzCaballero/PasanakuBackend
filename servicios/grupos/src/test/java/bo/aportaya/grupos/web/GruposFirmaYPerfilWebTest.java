package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.aplicacion.CU69FirmarReglamento;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** CU-69 · la firma del reglamento, mis limites y el perfil compartido en el borde (2026-10-20). Datos sinteticos. */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
            "aportaya.grupos.admision.humana-obligatoria=false",
            "aportaya.grupos.inicio.porcentaje=75",
        })
@Import(IntegrantesDelGrupo.class)
@MockitoBean(
        types = {
            CanjearInvitacion.class,
            CU20CrearGrupo.class,
            CU59CalcularPlazo.class,
            CU64TraspasarCupo.class,
            bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta.class,
            CU65Retirarse.class,
            CU68Postular.class,
            CU68AceptarIngreso.class,
            InvitacionesWeb.class,
            AdmisionDelGrupo.class,
            CU69Invitar.class,
            RespuestasAOtrosServicios.class,
            SorteoDelGrupo.class,
            CU69EnlaceDeGrupo.class,
            Consultas.class,
            GruposDeBackoffice.class,
            bo.aportaya.grupos.aplicacion.CU69IniciarGrupo.class
        })
class GruposFirmaYPerfilWebTest extends BaseDeDetalleYFirmaWeb {

    @Test
    @DisplayName("CU-69 · 200: firmar el reglamento devuelve si el grupo quedo conformado")
    void firmar() throws Exception {
        when(firma.firmar(eq(GRUPO), eq(HASH), any(), any()))
                .thenReturn(new CU69FirmarReglamento.Firmado(PARTICIPANTE, "CONFORMADO", true));
        mvc.perform(post("/grupos/{g}/reglamento/firma", GRUPO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hashReglamento\":\"" + HASH + "\",\"aceptaReglamento\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupoConformado").value(true))
                .andExpect(jsonPath("$.estadoDelGrupo").value("CONFORMADO"));
    }

    @Test
    @DisplayName("CU-69 · 422: sin aceptar el reglamento no hay firma, aunque el hash sea el correcto")
    void sinAceptarNoSeFirma() throws Exception {
        mvc.perform(post("/grupos/{g}/reglamento/firma", GRUPO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hashReglamento\":\"" + HASH + "\",\"aceptaReglamento\":false}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU69-05"));
        verifyNoInteractions(firma);
    }

    @Test
    @DisplayName("CU-69 · 400: un hash con otra forma no llega al caso de uso")
    void hashMalFormado() throws Exception {
        mvc.perform(post("/grupos/{g}/reglamento/firma", GRUPO)
                        .with(Sesiones.como("PARTICIPANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hashReglamento\":\"abc\",\"aceptaReglamento\":true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(firma);
    }

    @Test
    @DisplayName("CU-69 · 200: mis limites dicen cuantos grupos organizo y en cuantos participo")
    void misLimites() throws Exception {
        when(limites.consultar(any()))
                .thenReturn(new bo.aportaya.grupos.aplicacion.ConsultarMisLimites.Limites(1, 2, 3, 4));
        mvc.perform(get("/grupos/mis-limites").with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizados").value(1))
                .andExpect(jsonPath("$.limiteDeOrganizar").value(2))
                .andExpect(jsonPath("$.participaciones").value(3))
                .andExpect(jsonPath("$.limiteDeParticipar").value(4));
        mvc.perform(get("/grupos/mis-limites")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CU-69 · 401: sin sesion no se ve el detalle ni se firma")
    void sinSesion() throws Exception {
        mvc.perform(get("/grupos/{g}/detalle", GRUPO)).andExpect(status().isUnauthorized());
        mvc.perform(post("/grupos/{g}/reglamento/firma", GRUPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hashReglamento\":\"" + HASH + "\",\"aceptaReglamento\":true}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(detalle, firma);
    }

    @Test
    @DisplayName("perfil · 200: dice si quien pregunta y el otro comparten el grupo, y nada mas")
    void compartenGrupo() throws Exception {
        when(detalle.comparten(eq(GRUPO), eq(PARTICIPANTE), any())).thenReturn(true);
        mvc.perform(get("/grupos/{g}/integrantes/{u}/comparte", GRUPO, PARTICIPANTE)
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparten").value(true))
                .andExpect(jsonPath("$.estado").doesNotExist())
                .andExpect(jsonPath("$.cupoId").doesNotExist());
    }

    @Test
    @DisplayName("perfil · 200 false: quien no comparte el grupo recibe un no")
    void noCompartenGrupo() throws Exception {
        when(detalle.comparten(eq(GRUPO), eq(PARTICIPANTE), any())).thenReturn(false);
        mvc.perform(get("/grupos/{g}/integrantes/{u}/comparte", GRUPO, PARTICIPANTE)
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparten").value(false));
    }

    @Test
    @DisplayName("perfil · 401 sin sesion, 403 sin el permiso de participante, 400 con un id que no es UUID")
    void compartenSinPermiso() throws Exception {
        mvc.perform(get("/grupos/{g}/integrantes/{u}/comparte", GRUPO, PARTICIPANTE))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/grupos/{g}/integrantes/{u}/comparte", GRUPO, PARTICIPANTE)
                        .with(Sesiones.como("OTRO_PERMISO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/grupos/{g}/integrantes/{u}/comparte", GRUPO, "no-es-uuid")
                        .with(Sesiones.como("PARTICIPANTE")))
                .andExpect(status().isBadRequest());
    }
}

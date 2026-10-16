package bo.aportaya.cumplimiento.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejaDeReclamos;
import bo.aportaya.cumplimiento.aplicacion.ConsultarEfectoDelReclamo;
import bo.aportaya.cumplimiento.aplicacion.ConsultarEfectoDelReclamo.Caso;
import bo.aportaya.cumplimiento.aplicacion.ConsultarEfectoDelReclamo.Resultado;
import bo.aportaya.cumplimiento.dominio.EfectoDelReclamo;
import bo.aportaya.cumplimiento.dominio.EfectoDelReclamo.Motivo;
import bo.aportaya.plataforma.pruebas.EfectoDeReclamosDeEnsayo;
import bo.aportaya.plataforma.pruebas.EfectoDeReclamosDeEnsayo.Decision;
import bo.aportaya.plataforma.pruebas.EfectoDeReclamosDeEnsayo.Que;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * H13.S1.M6 · La ruta HTTP del efecto de reclamo: contrato del productor y matriz negativa.
 *
 * <p>Es la prueba de CONTRATO del lado de {@code cumplimiento}: la respuesta real del controlador
 * se le da a la lectura de referencia del consumidor ({@link EfectoDeReclamosDeEnsayo#leer}), la
 * misma que usan garantia y organizador contra el doble. Si el productor cambia la forma, cae aca.
 *
 * <p>Dos personas: la titular de los reclamos y otra que conoce su identificador. Quien no tiene
 * {@code RECLAMO_VER} —sea participante, administrador de grupo, o el token de otro servicio— recibe
 * el mismo 403, y el caso de uso ni se entera.
 */
@PruebaWeb(ReclamosController.class)
class ReclamosControllerWebTest {

    private static final UUID TITULAR = Sesiones.USUARIO;
    private static final UUID OTRA = Sesiones.OTRO_USUARIO;
    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 3, 15, 14, 30, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarEfectoDelReclamo efecto;

    // La bandeja, la ficha y la respuesta de CU-52 (backoffice carril A · H3) viven en el mismo controlador.
    @MockitoBean
    private ConsultarBandejaDeReclamos bandejaDeReclamos;

    @MockitoBean
    private CU52AtenderReclamo cu52;

    private static RequestPostProcessor personal() {
        return Sesiones.de(OTRA, "SOPORTE", "RECLAMO_VER", "RECLAMO_ATENDER");
    }

    private static Caso casoQueFrena() {
        return new Caso(
                UUID.fromString("a3000000-0000-4000-8000-0000000000c1"),
                "RC-2026-0001",
                "COBRO_INDEBIDO",
                "APORTE",
                "RESPONDIDO",
                null,
                AHORA.minusDays(3),
                AHORA.minusDays(1),
                new EfectoDelReclamo(true, true, Motivo.EN_REVISION));
    }

    @Test
    @DisplayName("el personal con RECLAMO_VER recibe el efecto con el rastro de cada caso, y el consumidor lo entiende")
    void personalConsulta() throws Exception {
        when(efecto.delUsuario(eq(TITULAR), eq(Optional.empty()), any()))
                .thenReturn(new Resultado(TITULAR, true, true, null, List.of(casoQueFrena())));

        String cuerpo = mvc.perform(
                        get("/reclamos/usuarios/{id}/efecto", TITULAR).with(personal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(TITULAR.toString()))
                .andExpect(jsonPath("$.bloqueaSancionFirme").value(true))
                .andExpect(jsonPath("$.bloqueaDeudaEjecutable").value(true))
                .andExpect(jsonPath("$.casos[0].motivo").value("EN_REVISION"))
                .andExpect(jsonPath("$.casos[0].codigo").value("RC-2026-0001"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Contrato de punta a punta: lo que el productor devuelve, el consumidor de referencia lo lee y FRENA.
        assertThat(EfectoDeReclamosDeEnsayo.leer(200, cuerpo, Que.DEUDA_EJECUTABLE))
                .isEqualTo(Decision.FRENA);
        assertThat(EfectoDeReclamosDeEnsayo.leer(200, cuerpo, Que.SANCION_FIRME))
                .isEqualTo(Decision.FRENA);
        // No viaja el texto del reclamo ni de su respuesta (respuesta minima, 96.5.3).
        assertThat(cuerpo).doesNotContain("descripcion").doesNotContain("respuesta\"");
    }

    @Test
    @DisplayName("sin reclamos la respuesta lleva SIN_RECLAMOS y el consumidor de referencia PROCEDE")
    void sinReclamos() throws Exception {
        when(efecto.delUsuario(any(), any(), any()))
                .thenReturn(new Resultado(TITULAR, false, false, Motivo.SIN_RECLAMOS, List.of()));

        String cuerpo = mvc.perform(
                        get("/reclamos/usuarios/{id}/efecto", TITULAR).with(personal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoGlobal").value("SIN_RECLAMOS"))
                .andExpect(jsonPath("$.casos").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(EfectoDeReclamosDeEnsayo.leer(200, cuerpo, Que.DEUDA_EJECUTABLE))
                .isEqualTo(Decision.PROCEDE);
        assertThat(EfectoDeReclamosDeEnsayo.leer(200, cuerpo, Que.SANCION_FIRME))
                .isEqualTo(Decision.PROCEDE);
    }

    @Test
    @DisplayName("el filtro por producto llega al caso de uso")
    void filtroPorProducto() throws Exception {
        when(efecto.delUsuario(any(), any(), any()))
                .thenReturn(new Resultado(TITULAR, false, false, Motivo.SIN_RECLAMOS, List.of()));

        mvc.perform(get("/reclamos/usuarios/{id}/efecto", TITULAR)
                        .param("producto", "APORTE")
                        .with(personal()))
                .andExpect(status().isOk());

        verify(efecto).delUsuario(eq(TITULAR), eq(Optional.of("APORTE")), any());
    }

    @Test
    @DisplayName("rechaza: sin token no se consultan los reclamos de nadie (401)")
    void sinToken() throws Exception {
        mvc.perform(get("/reclamos/usuarios/{id}/efecto", TITULAR)).andExpect(status().isUnauthorized());
        verifyNoInteractions(efecto);
    }

    @Test
    @DisplayName("rechaza: otro participante que conoce el UUID no ve los reclamos del titular")
    void participanteAjeno() throws Exception {
        mvc.perform(get("/reclamos/usuarios/{id}/efecto", TITULAR)
                        .with(Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_VER", "BILLETERA_OPERAR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
        verifyNoInteractions(efecto);
    }

    @Test
    @DisplayName("rechaza: ni el titular ve su propio rastro por esta ruta de operacion (rol insuficiente)")
    void elTitularNoEsPersonal() throws Exception {
        mvc.perform(get("/reclamos/usuarios/{id}/efecto", TITULAR).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(efecto);
    }

    @Test
    @DisplayName("rechaza: administrar un grupo no da derecho a leer los reclamos de cualquier persona")
    void administradorDeGrupoNoAlcanza() throws Exception {
        mvc.perform(get("/reclamos/usuarios/{id}/efecto", TITULAR)
                        .with(Sesiones.comoOtro("ORGANIZADOR", "GRUPO_ADMINISTRAR", "GRUPO_CREAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(efecto);
    }

    @Test
    @DisplayName("rechaza: el token de servicio de otro servicio (entregas, inversiones) no ve reclamos")
    void tokenDeServicioNoEsPersonal() throws Exception {
        mvc.perform(get("/reclamos/usuarios/{id}/efecto", TITULAR)
                        .with(Sesiones.como("SERVICIO_ENTREGAS", "ENTREGA_EJECUTAR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reclamos/usuarios/{id}/efecto", TITULAR)
                        .with(Sesiones.como("SERVICIO_INVERSIONES", "ENTREGA_EJECUTAR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(efecto);
    }

    @Test
    @DisplayName("rechaza: el 403 no distingue a una persona que existe de una que no")
    void laNegativaNoRevelaExistencia() throws Exception {
        String existente = mvc.perform(get("/reclamos/usuarios/{id}/efecto", TITULAR)
                        .with(Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_VER")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String inexistente = mvc.perform(get("/reclamos/usuarios/{id}/efecto", UUID.randomUUID())
                        .with(Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_VER")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(sinTraza(existente)).isEqualTo(sinTraza(inexistente));
        assertThat(existente).doesNotContain(TITULAR.toString());
    }

    @Test
    @DisplayName("rechaza: un identificador que no es un UUID es 400 y no llega al caso de uso")
    void identificadorMalFormado() throws Exception {
        mvc.perform(get("/reclamos/usuarios/{id}/efecto", "no-soy-un-uuid").with(personal()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(efecto);
    }

    private static String sinTraza(String cuerpo) {
        return cuerpo.replaceAll("\"trazaId\"\\s*:\\s*\"[^\"]*\"", "\"trazaId\":\"-\"");
    }
}

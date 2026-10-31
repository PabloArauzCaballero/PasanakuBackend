package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.GrupoIniciado;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * CU-69 · iniciar al 75 % y CU-20 · crear con etapa minima, en el borde (2026-10-30). La matriz negativa de iniciar:
 * sin sesion 401, sin GRUPO_ADMINISTRAR 403, sin Idempotency-Key 400, y quien organiza OTRO grupo 422 AP-CU69-07 (la
 * decision sobre el recurso la toma el caso de uso). La etapa del organizador es un doble con catalogo SINTETICO.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
            "aportaya.grupos.admision.humana-obligatoria=false",
        })
@MockitoBean(
        types = {
            CanjearInvitacion.class, CU59CalcularPlazo.class, CU64TraspasarCupo.class, CU65Retirarse.class,
            CU68Postular.class, CU68AceptarIngreso.class, InvitacionesWeb.class, AdmisionDelGrupo.class,
            CU69Invitar.class, RespuestasAOtrosServicios.class, SorteoDelGrupo.class, CU69EnlaceDeGrupo.class,
            Consultas.class, GruposDeBackoffice.class
        })
class GruposInicioYEtapaWebTest {

    private static final UUID GRUPO = UUID.fromString("a7e10000-0000-4000-8000-000000000001");
    private static final String CLAVE = "a7e10000-0000-4000-8000-0000000000ff";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IntegrantesDelGrupo integrantes;

    @MockitoBean
    private CU20CrearGrupo cu20;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    @Nested
    @DisplayName("CU-69 · iniciar el grupo")
    class Iniciar {

        private ResultActions iniciar(org.springframework.test.web.servlet.request.RequestPostProcessor sesion)
                throws Exception {
            return mvc.perform(post("/grupos/{g}/inicio", GRUPO).with(sesion).header("Idempotency-Key", CLAVE));
        }

        @Test
        @DisplayName("200: el grupo iniciado, achicado a sus cupos ocupados")
        void inicia() throws Exception {
            when(integrantes.iniciar(eq(GRUPO), eq(UUID.fromString(CLAVE)), any()))
                    .thenReturn(new GrupoIniciado(GRUPO, GrupoIniciado.EstadoEnum.CONFORMADO, 8, 8));
            iniciar(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CONFORMADO"))
                    .andExpect(jsonPath("$.cuposTotales").value(8))
                    .andExpect(jsonPath("$.numPeriodos").value(8));
        }

        @Test
        @DisplayName("401 sin sesion · 403 sin GRUPO_ADMINISTRAR · 400 sin Idempotency-Key: el caso de uso no corre")
        void matrizNegativa() throws Exception {
            mvc.perform(post("/grupos/{g}/inicio", GRUPO).header("Idempotency-Key", CLAVE))
                    .andExpect(status().isUnauthorized());
            iniciar(Sesiones.como("PARTICIPANTE")).andExpect(status().isForbidden());
            mvc.perform(post("/grupos/{g}/inicio", GRUPO).with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(integrantes);
        }

        @Test
        @DisplayName("422 AP-CU69-07: con GRUPO_ADMINISTRAR pero organizando OTRO grupo")
        void organizadorDeOtroGrupo() throws Exception {
            when(integrantes.iniciar(eq(GRUPO), any(), any()))
                    .thenThrow(
                            new ErrorDeNegocio(CodigoError.de(69, 7), "Solo quien organiza el grupo puede iniciarlo."));
            iniciar(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-CU69-07"));
        }

        @Test
        @DisplayName("422 AP-CU69-16 con el conteo en el detalle, para que la app diga «faltan N»")
        void umbralNoAlcanzado() throws Exception {
            when(integrantes.iniciar(eq(GRUPO), any(), any()))
                    .thenThrow(new ErrorDeNegocio(
                            CodigoError.de(69, 16),
                            "Todavia no se llego al minimo.",
                            Map.of("ocupados", 7, "minimoRequerido", 8, "cuposTotales", 10)));
            iniciar(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-CU69-16"))
                    .andExpect(jsonPath("$.detalle.ocupados").value(7))
                    .andExpect(jsonPath("$.detalle.minimoRequerido").value(8))
                    .andExpect(jsonPath("$.detalle.cuposTotales").value(10));
        }
    }

    @Nested
    @DisplayName("CU-20 · crear con etapa minima")
    class Crear {

        @BeforeEach
        void organizadorHabilitadoEnMedia() {
            when(afuera.organizadorHabilitadoDelUsuario(any())).thenReturn(Optional.of(UUID.randomUUID()));
            when(afuera.limiteDeGruposDelOrganizador(any())).thenReturn(OptionalInt.of(3));
            when(afuera.nivelDeKyc(any())).thenReturn("BASICO");
            when(afuera.etapaDe(any())).thenReturn(EtapasDePrueba.con("600.00"));
            when(cu20.ejecutar(any(), any())).thenReturn(new CU20CrearGrupo.SalidaCreacion(GRUPO, "0.00"));
        }

        private ResultActions crear(String etapaMinima, int cupos, String aporte) throws Exception {
            String etapa = etapaMinima == null ? "" : ",\"etapaMinima\":\"" + etapaMinima + "\"";
            return mvc.perform(post("/grupos")
                    .with(Sesiones.como("ORGANIZADOR", "GRUPO_CREAR"))
                    .header("Idempotency-Key", CLAVE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            """
                            {"nombre":"Pasanaku del barrio","montoAporte":{"monto":"%s","moneda":"BOB"},
                             "periodicidad":"MENSUAL","cupos":%d,"diaCobro":5,
                             "modalidadTurnos":"SORTEO_ALEATORIO","fechaDeInicio":"2026-12-01"%s}
                            """
                                    .formatted(aporte, cupos, etapa)));
        }

        private BigDecimal minimaGuardada() {
            var entrada = ArgumentCaptor.forClass(CU20CrearGrupo.EntradaCreacion.class);
            verify(cu20).ejecutar(entrada.capture(), any());
            return entrada.getValue().reputacionMinima();
        }

        @Test
        @DisplayName("sin etapaMinima: el grupo pide la etapa del organizador (MEDIA → desde 500.00)")
        void porOmisionLaPropia() throws Exception {
            crear(null, 10, "500.00").andExpect(status().isAccepted());
            assertThat(minimaGuardada()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("una etapa por debajo de la propia se puede pedir (BAJA → 0.00, sin minima)")
        void unaMasBaja() throws Exception {
            crear("BAJA", 10, "500.00").andExpect(status().isAccepted());
            assertThat(minimaGuardada()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("422 AP-CU20-08: por encima de la propia, o una que no esta en el catalogo")
        void etapaInvalida() throws Exception {
            crear("ALTA", 10, "500.00")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-CU20-08"));
            crear("OTRA", 10, "500.00")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-CU20-08"));
            verify(cu20, never()).ejecutar(any(), any());
        }

        @Test
        @DisplayName("422 AP-CU20-07: MEDIA (VIGENTE: 10 cupos, 500.00 BOB) no desbloquea 12 cupos ni 500.01")
        void desbloqueoExcedido() throws Exception {
            for (var pedido : new Object[][] {{12, "500.00"}, {10, "500.01"}}) {
                crear(null, (int) pedido[0], (String) pedido[1])
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.codigo").value("AP-CU20-07"))
                        .andExpect(jsonPath("$.detalle.cuposMax").value(10))
                        .andExpect(jsonPath("$.detalle.aporteMax").value("500.00"))
                        .andExpect(jsonPath("$.detalle.moneda").value("BOB"));
            }
            verify(cu20, never()).ejecutar(any(), any());
        }

        @Test
        @DisplayName("transparencia no contesta: no se crea (falla cerrado)")
        void sinTransparencia() throws Exception {
            when(afuera.etapaDe(any())).thenReturn(Optional.empty());
            crear(null, 10, "500.00")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("AP-DOM-01"));
            verify(cu20, never()).ejecutar(any(), any());
        }

        @Test
        @DisplayName("reintento con la misma clave: no se pregunta la etapa y el caso de uso devuelve el alta original")
        void reintentoNoPreguntaLaEtapa() throws Exception {
            when(cu20.yaCreado(any(), any())).thenReturn(true);
            when(afuera.etapaDe(any())).thenReturn(Optional.empty());
            crear(null, 10, "500.00")
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.grupoId").value(GRUPO.toString()));
            verify(afuera, never()).etapaDe(any());
            var entrada = ArgumentCaptor.forClass(CU20CrearGrupo.EntradaCreacion.class);
            verify(cu20).ejecutar(entrada.capture(), any());
            assertThat(entrada.getValue().etapaMinimaPedida()).isNull();
        }

        @Test
        @DisplayName("la etapa PEDIDA viaja al caso de uso (es la que entra en la huella del alta)")
        void laEtapaPedidaViaja() throws Exception {
            crear("BAJA", 10, "500.00").andExpect(status().isAccepted());
            var entrada = ArgumentCaptor.forClass(CU20CrearGrupo.EntradaCreacion.class);
            verify(cu20).ejecutar(entrada.capture(), any());
            assertThat(entrada.getValue().etapaMinimaPedida()).isEqualTo("BAJA");
        }

        @Test
        @DisplayName("400: una etapaMinima con otra forma no llega a preguntar nada")
        void etapaMalFormada() throws Exception {
            crear("media", 10, "500.00").andExpect(status().isBadRequest());
            verifyNoInteractions(cu20);
        }
    }
}

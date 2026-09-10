package bo.aportaya.tarifas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.tarifas.aplicacion.CU34PublicarTarifario;
import bo.aportaya.tarifas.aplicacion.CU35CerrarLiquidacion;
import bo.aportaya.tarifas.aplicacion.CU36ResolverPrecio;
import bo.aportaya.tarifas.aplicacion.ConsultarTarifarioVigente;
import bo.aportaya.tarifas.dominio.SegmentoAplicable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
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
 * CU-34, CU-35 y CU-36 · publicar el tarifario, resolver el precio y cerrar el período.
 *
 * <p>La afirmación central es regulatoria: <b>un cambio de tarifa que perjudica al
 * usuario no entra en vigencia el mismo día</b>. La respuesta de la publicación dice si
 * requiere preaviso y desde cuándo rige, y esos dos campos son lo que un usuario —o el
 * regulador— usa para saber que el plazo se respetó. Publicar y poner vigente son dos
 * actos separados justamente para que el preaviso exista.
 *
 * <p>Y una de control: <b>publicar el tarifario y editar el catálogo tienen permisos
 * distintos</b>. Quien crea un segmento no puede además subir los precios.
 */
@PruebaWeb(TarifasController.class)
class TarifasControllerWebTest {

    private static final UUID TARIFARIO = UUID.fromString("b7000000-0000-4000-8000-000000000001");
    private static final UUID CAMBIO = UUID.fromString("b7000000-0000-4000-8000-000000000002");

    private static final String PUBLICACION =
            """
            {"tarifarioBaseId":"b7000000-0000-4000-8000-000000000001","nombre":"Tarifario 2026-Q2",
             "tipoCambio":"INCREMENTO","diasPreaviso":30,
             "aprobadoPor":"b7000000-0000-4000-8000-00000000000a",
             "actaComite":"ACTA-2026-014","urlPublicacion":"https://aportaya.bo/tarifas/2026q2",
             "hashDocumento":"%s","canalAviso":"CORREO","permiteRescisionSinCosto":true}
            """
                    .formatted("ab".repeat(32));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultarTarifarioVigente vigentes;

    @MockitoBean
    private CU34PublicarTarifario cu34;

    @MockitoBean
    private CU35CerrarLiquidacion cu35;

    @MockitoBean
    private CU36ResolverPrecio cu36;

    @Nested
    @DisplayName("CU-34 · publicar un tarifario nuevo")
    class Publicar {

        private org.springframework.test.web.servlet.ResultActions publicar(String cuerpo) throws Exception {
            return mvc.perform(post("/tarifas/tarifarios")
                    .with(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_PUBLICAR"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo));
        }

        @Test
        @DisplayName("CU-34 · 201 en preaviso: un incremento NO entra en vigencia el mismo día")
        void incrementoRequierePreaviso() throws Exception {
            when(cu34.publicar(any(), any()))
                    .thenReturn(new CU34PublicarTarifario.SalidaPublicacion(
                            TARIFARIO,
                            CAMBIO,
                            3,
                            "EN_PREAVISO",
                            OffsetDateTime.of(2026, 4, 15, 0, 0, 0, 0, ZoneOffset.UTC),
                            true));

            publicar(PUBLICACION)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tarifarioNuevoId").value(TARIFARIO.toString()))
                    .andExpect(jsonPath("$.version").value(3))
                    .andExpect(jsonPath("$.estado").value("EN_PREAVISO"))
                    // Los dos campos con los que se demuestra que el plazo se respeto.
                    .andExpect(jsonPath("$.requierePreaviso").value(true))
                    .andExpect(jsonPath("$.entraEnVigencia").exists());
        }

        @Test
        @DisplayName("CU-34 · una reducción no necesita preaviso y puede quedar vigente")
        void reduccionSinPreaviso() throws Exception {
            when(cu34.publicar(any(), any()))
                    .thenReturn(
                            new CU34PublicarTarifario.SalidaPublicacion(TARIFARIO, CAMBIO, 4, "VIGENTE", null, false));

            publicar(PUBLICACION.replace("INCREMENTO", "REDUCCION"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.requierePreaviso").value(false))
                    .andExpect(jsonPath("$.estado").value("VIGENTE"));
        }

        @Test
        @DisplayName("CU-34 · el acta, el aprobador y el hash del documento llegan al caso de uso")
        void laEvidenciaDelCambioLlegaEntera() throws Exception {
            when(cu34.publicar(any(), any()))
                    .thenReturn(new CU34PublicarTarifario.SalidaPublicacion(
                            TARIFARIO, CAMBIO, 3, "EN_PREAVISO", null, true));

            publicar(PUBLICACION).andExpect(status().isCreated());

            var capturada = ArgumentCaptor.forClass(CU34PublicarTarifario.EntradaPublicacion.class);
            verify(cu34).publicar(capturada.capture(), any());
            var entrada = capturada.getValue();

            // Subir precios sin acta ni aprobador es lo que un regulador pide primero.
            org.assertj.core.api.Assertions.assertThat(entrada.actaComite()).isEqualTo("ACTA-2026-014");
            org.assertj.core.api.Assertions.assertThat(entrada.hashDocumento()).isEqualTo("ab".repeat(32));
            org.assertj.core.api.Assertions.assertThat(entrada.diasPreaviso()).isEqualTo(30);
            org.assertj.core.api.Assertions.assertThat(entrada.permiteRescisionSinCosto())
                    .isTrue();
        }

        @Test
        @DisplayName("CU-34 · 400: un tipo de cambio que el contrato no enumera")
        void tipoDeCambioFueraDelContrato() throws Exception {
            publicar(PUBLICACION.replace("INCREMENTO", "AJUSTE_TECNICO")).andExpect(status().isBadRequest());
            verifyNoInteractions(cu34);
        }

        @Test
        @DisplayName("CU-34 · 400: sin acta de comité no se publica un tarifario")
        void sinActaNoSePublica() throws Exception {
            publicar(PUBLICACION.replace("\"actaComite\":\"ACTA-2026-014\",", ""))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cu34);
        }

        @Test
        @DisplayName("CU-34 · poner vigente es un acto SEPARADO de publicar")
        void ponerVigenteEsOtroActo() throws Exception {
            when(cu34.ponerVigente(eq(TARIFARIO), any()))
                    .thenReturn(new CU34PublicarTarifario.SalidaVigencia(
                            TARIFARIO, "VIGENTE", OffsetDateTime.of(2026, 4, 15, 0, 0, 0, 0, ZoneOffset.UTC)));

            // Si publicar dejara vigente de una, el preaviso no existiria.
            mvc.perform(post("/tarifas/tarifarios/{id}/vigencia", TARIFARIO)
                            .with(Sesiones.como("ADMIN_PLATAFORMA", "TARIFARIO_PUBLICAR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("VIGENTE"))
                    .andExpect(jsonPath("$.desde").exists());
        }

        @Test
        @DisplayName("CU-34 · publicar exige TARIFARIO_PUBLICAR: editar el catálogo no alcanza")
        void editarCatalogoNoEsPublicarPrecios() throws Exception {
            mvc.perform(post("/tarifas/tarifarios")
                            .with(Sesiones.como("ADMIN_PLATAFORMA", "CATALOGO_EDITAR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PUBLICACION))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(cu34);
        }
    }

    @Nested
    @DisplayName("CU-36 · qué segmento le corresponde a este usuario")
    class ResolverPrecio {

        @Test
        @DisplayName("CU-36 · 200 con el segmento elegido y el MOTIVO de la elección")
        void segmentoConMotivo() throws Exception {
            when(cu36.resolver(any(), any()))
                    .thenReturn(new SegmentoAplicable.Eleccion("ANTIGUEDAD_12M", "12 meses sin mora", true));

            mvc.perform(post("/tarifas/precios/resolver")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"hechos\":{\"mesesSinMora\":12}}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.codigo").value("ANTIGUEDAD_12M"))
                    // El motivo es lo que le explica al usuario por que le toca ese
                    // precio. Sin el, un descuento parece arbitrario y su ausencia
                    // tambien.
                    .andExpect(jsonPath("$.motivo").value("12 meses sin mora"))
                    .andExpect(jsonPath("$.evaluable").value(true));
        }

        @Test
        @DisplayName("CU-36 · quien no califica recibe el motivo, no un campo vacío")
        void sinSegmentoTambienHayMotivo() throws Exception {
            when(cu36.resolver(any(), any())).thenReturn(SegmentoAplicable.NINGUNO);

            mvc.perform(post("/tarifas/precios/resolver")
                            .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"hechos\":{}}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.codigo").doesNotExist())
                    .andExpect(jsonPath("$.motivo").value("No califica para ningun beneficio"));
        }
    }

    @Test
    @DisplayName("CU-34 · el tarifario vigente contesta si hay uno, sin exponer su contenido")
    void consultarVigente() throws Exception {
        when(vigentes.ejecutar(any(), any())).thenReturn(Optional.of(TARIFARIO));

        String cuerpo = mvc.perform(get("/tarifas/vigentes/{codigo}", "TAR-2026")
                        .with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(true))
                .andExpect(jsonPath("$.tarifarioId").value(TARIFARIO.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Dos campos: si hay y cual. Los precios se piden cotizando, no listando.
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("monto")
                .doesNotContain("porcentaje");
    }
}

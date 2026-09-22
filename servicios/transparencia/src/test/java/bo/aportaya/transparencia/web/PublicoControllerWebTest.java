package bo.aportaya.transparencia.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.transparencia.aplicacion.CU61VerificarSorteo;
import bo.aportaya.transparencia.aplicacion.CU73VerificarCadena;
import bo.aportaya.transparencia.aplicacion.ListarBloques;
import bo.aportaya.transparencia.dominio.puertos.PaquetesDeSorteo;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-61, CU-72 y CU-73 · las tres rutas que un tercero SIN cuenta puede verificar.
 *
 * <p>Estas rutas existen para que alguien de afuera compruebe que el sorteo no estuvo
 * arreglado y que la cadena de eventos no se tocó. <b>Si pidieran sesión, no servirían
 * para nada</b>: el que desconfía es justamente el que no tiene cuenta.
 *
 * <p>La otra mitad del contrato: publicar lo suficiente para <b>rehacer el cálculo</b>
 * —la semilla, las entropías, el método y el orden— y ningún dato de persona. Un
 * verificador que tiene que confiar en el «verifica: true» no está verificando nada.
 */
@PruebaWeb(PublicoController.class)
class PublicoControllerWebTest {

    private static final UUID SORTEO = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("e0000000-0000-4000-8000-000000000002");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CU61VerificarSorteo cu61;

    @MockitoBean
    private CU73VerificarCadena cu73;

    @MockitoBean
    private PaquetesDeSorteo paquetes;

    @MockitoBean
    private ListarBloques bloques;

    @Nested
    @DisplayName("GET /publico/sorteos/{id}/verificacion")
    class Sorteo {

        private static CU61VerificarSorteo.PaqueteDeSorteo paquete() {
            return new CU61VerificarSorteo.PaqueteDeSorteo(
                    SORTEO,
                    "ab".repeat(32),
                    "semilla-revelada",
                    List.of("ent-1", "ent-2"),
                    "SHA256",
                    List.of(1, 2, 3),
                    List.of(2, 3, 1));
        }

        @Test
        @DisplayName("CU-61 · 200 SIN sesión, y con todo lo necesario para rehacer el sorteo")
        void verificaSinSesion() throws Exception {
            when(paquetes.de(SORTEO)).thenReturn(Optional.of(paquete()));
            when(cu61.verificar(any(), any()))
                    .thenReturn(new CU61VerificarSorteo.SalidaSorteo(
                            true,
                            "ab".repeat(32),
                            "ab".repeat(32),
                            true,
                            null,
                            "semilla-revelada",
                            List.of("ent-1", "ent-2"),
                            "SHA256",
                            List.of(2, 3, 1)));

            // Sin `.with(Sesiones...)`: el que desconfia es el que no tiene cuenta.
            mvc.perform(get("/publico/sorteos/{id}/verificacion", SORTEO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verifica").value(true))
                    .andExpect(jsonPath("$.hashEsperado").value("ab".repeat(32)))
                    .andExpect(jsonPath("$.hashRecomputado").value("ab".repeat(32)))
                    // Sin la semilla, las entropias y el metodo, el tercero solo puede
                    // creernos — que es lo contrario de verificar.
                    .andExpect(jsonPath("$.paquete.semilla").value("semilla-revelada"))
                    .andExpect(jsonPath("$.paquete.entropias[0]").value("ent-1"))
                    .andExpect(jsonPath("$.paquete.metodo").value("SHA256"))
                    .andExpect(jsonPath("$.paquete.cupos[0]").value(2));
        }

        @Test
        @DisplayName("CU-61 · un sorteo que NO verifica dice cuál fue el primer cupo discrepante")
        void sorteoQueNoVerifica() throws Exception {
            when(paquetes.de(SORTEO)).thenReturn(Optional.of(paquete()));
            when(cu61.verificar(any(), any()))
                    .thenReturn(new CU61VerificarSorteo.SalidaSorteo(
                            false,
                            "ab".repeat(32),
                            "cd".repeat(32),
                            false,
                            2,
                            "semilla-revelada",
                            List.of("ent-1"),
                            "SHA256",
                            List.of(3, 1, 2)));

            mvc.perform(get("/publico/sorteos/{id}/verificacion", SORTEO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verifica").value(false))
                    .andExpect(jsonPath("$.ordenCoincide").value(false))
                    // Decir DÓNDE falla es lo que permite auditarlo; un «false» pelado
                    // obliga a creer que efectivamente falla.
                    .andExpect(jsonPath("$.primerCupoDiscrepante").value(2));
        }

        @Test
        @DisplayName("CU-61 · un sorteo sin paquete publicado responde 200 y NO verifica")
        void sorteoSinPaquete() throws Exception {
            when(paquetes.de(any())).thenReturn(Optional.empty());

            mvc.perform(get("/publico/sorteos/{id}/verificacion", SORTEO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verifica").value(false))
                    .andExpect(jsonPath("$.paquete.metodo").value("NO_DISPONIBLE"));

            // Sin paquete no hay nada que recomputar: llamar al caso de uso seria
            // pedirle que verifique la nada.
            verify(cu61, never()).verificar(any(), any());
        }

        @Test
        @DisplayName("CU-61 · 400: un identificador de sorteo que no es un identificador")
        void sorteoQueNoEsUuid() throws Exception {
            mvc.perform(get("/publico/sorteos/{id}/verificacion", "el-de-enero"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /publico/grupos/{id} — la cadena y sus eslabones")
    class Cadena {

        @Test
        @DisplayName("CU-73 · 200 SIN sesión: íntegra, con cuántos bloques se verificaron")
        void cadenaIntegra() throws Exception {
            when(cu73.verificar(any(), any()))
                    .thenReturn(new CU73VerificarCadena.SalidaCadena(
                            true, 42, null, null, OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC)));

            mvc.perform(get("/publico/grupos/{id}/verificacion", GRUPO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.integra").value(true))
                    .andExpect(jsonPath("$.bloquesVerificados").value(42))
                    .andExpect(jsonPath("$.primerBloqueFallido").doesNotExist())
                    .andExpect(jsonPath("$.ultimoSellado").exists());
        }

        @Test
        @DisplayName("CU-73 · una cadena rota dice qué bloque y qué componente falló")
        void cadenaRota() throws Exception {
            when(cu73.verificar(any(), any()))
                    .thenReturn(new CU73VerificarCadena.SalidaCadena(
                            false,
                            17,
                            18L,
                            "HASH_ANTERIOR",
                            OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC)));

            mvc.perform(get("/publico/grupos/{id}/verificacion", GRUPO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.integra").value(false))
                    .andExpect(jsonPath("$.primerBloqueFallido").value(18))
                    .andExpect(jsonPath("$.componenteFallido").value("HASH_ANTERIOR"));
        }

        @Test
        @DisplayName("CU-72 · los eslabones se publican con sus tres hashes, para rehacer el encadenado")
        void bloquesPublicados() throws Exception {
            when(bloques.ejecutar(any(), any()))
                    .thenReturn(List.of(new ListarBloques.Bloque(
                            7L,
                            "aa".repeat(32),
                            "bb".repeat(32),
                            "cc".repeat(32),
                            OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                            OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                            310)));

            mvc.perform(get("/publico/grupos/{id}/bloques", GRUPO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].numeroBloque").value(7))
                    // Los tres: sin el anterior no hay cadena, sin la raiz no hay
                    // contenido, sin el propio no hay eslabon.
                    .andExpect(jsonPath("$[0].hashAnterior").value("aa".repeat(32)))
                    .andExpect(jsonPath("$[0].raizMerkle").value("bb".repeat(32)))
                    .andExpect(jsonPath("$[0].hashBloque").value("cc".repeat(32)))
                    .andExpect(jsonPath("$[0].cantidadEventos").value(310));
        }

        @Test
        @DisplayName("CU-72 · los bloques publicados no llevan ningún dato de persona")
        void losBloquesNoLlevanDatosDePersona() throws Exception {
            when(bloques.ejecutar(any(), any()))
                    .thenReturn(List.of(new ListarBloques.Bloque(
                            7L,
                            "aa".repeat(32),
                            "bb".repeat(32),
                            "cc".repeat(32),
                            OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                            OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                            310)));

            String cuerpo = mvc.perform(get("/publico/grupos/{id}/bloques", GRUPO))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Es una ruta abierta a internet: lo unico que sale son hashes y conteos.
            org.assertj.core.api.Assertions.assertThat(cuerpo)
                    .doesNotContain("usuario")
                    .doesNotContain("nombre")
                    .doesNotContain("documento")
                    .doesNotContain("telefono")
                    .doesNotContain("monto");
        }
    }
}

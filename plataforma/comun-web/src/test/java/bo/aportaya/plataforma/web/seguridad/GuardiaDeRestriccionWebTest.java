package bo.aportaya.plataforma.web.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import bo.aportaya.plataforma.web.errores.ControladorDeEnsayo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CU-134 (D-6): una cuenta bajo medida entra con {@code alcance=RESTRINGIDO} y solo llega a las rutas marcadas
 * con {@link PermitidoEnRestriccion}. Tres niveles del contrato: la ruta de la lista pasa, la ruta fuera de la
 * lista se niega aunque el permiso sea el correcto, y un token sin el reclamo se comporta como siempre.
 */
@PruebaWeb(ControladorDeEnsayo.class)
class GuardiaDeRestriccionWebTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("CU-134 · correcto: una sesion restringida llega a una ruta de la lista cerrada")
    void restringidaEnLaLista() throws Exception {
        mvc.perform(get("/ensayo/en-restriccion").with(Sesiones.restringida("PARTICIPANTE")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CU-134 · limite: fuera de la lista, 403 aunque el token traiga el permiso exacto")
    void restringidaFueraDeLaLista() throws Exception {
        mvc.perform(get("/ensayo/recursos/{id}", Sesiones.USUARIO)
                        .with(Sesiones.restringida("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AP-SEG-01"));
    }

    @Test
    @DisplayName("CU-134 · restringida sigue necesitando el permiso de la ruta de la lista")
    void restringidaSinElPermisoDeLaRuta() throws Exception {
        mvc.perform(get("/ensayo/en-restriccion").with(Sesiones.restringida("ORGANIZADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CU-134 · sin el reclamo de alcance, la sesion es completa como siempre")
    void sinReclamoEsCompleta() throws Exception {
        mvc.perform(get("/ensayo/recursos/{id}", Sesiones.USUARIO).with(Sesiones.como("PARTICIPANTE", "BILLETERA_VER")))
                .andExpect(status().isOk());
    }
}

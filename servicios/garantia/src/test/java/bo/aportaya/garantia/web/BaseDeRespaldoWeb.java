package bo.aportaya.garantia.web;

import bo.aportaya.garantia.aplicacion.CU23CubrirFaltanteDelCorte;
import bo.aportaya.garantia.aplicacion.CU23RecuperarRespaldo;
import bo.aportaya.garantia.aplicacion.CU23ReservarRespaldo;
import bo.aportaya.garantia.aplicacion.CU23ReversarCobertura;
import bo.aportaya.garantia.aplicacion.CU29DevolverFondo;
import bo.aportaya.garantia.aplicacion.CU66ReemplazarParticipante;
import bo.aportaya.garantia.aplicacion.CU67DisolverGrupo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lo comun de las pruebas web de {@code /garantia/respaldo}: la sesion MVC, los dobles de los
 * casos de uso y las constantes del escenario.
 *
 * <p>La anotacion {@code @PruebaWeb} va en cada clase concreta: no es heredable, y los
 * controladores y la configuracion del corte los declara quien la usa.
 */
abstract class BaseDeRespaldoWeb {

    protected static final UUID TURNO = UUID.fromString("c3000000-0000-4000-8000-000000000002");
    protected static final UUID RESERVA = UUID.fromString("c3000000-0000-4000-8000-000000000005");
    protected static final String CLAVE = "c3000000-0000-4000-8000-0000000000ff";

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected CU23ReservarRespaldo reservar;

    @MockitoBean
    protected CU23CubrirFaltanteDelCorte cubrir;

    @MockitoBean
    protected CU23ReversarCobertura reversar;

    @MockitoBean
    protected CU23RecuperarRespaldo recuperar;

    // El controlador de /garantia tambien sirve fondo, reemplazo y disolucion: aca no se prueban.
    @MockitoBean
    protected CU29DevolverFondo cu29;

    @MockitoBean
    protected CU66ReemplazarParticipante cu66;

    @MockitoBean
    protected CU67DisolverGrupo cu67;

    protected static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }
}

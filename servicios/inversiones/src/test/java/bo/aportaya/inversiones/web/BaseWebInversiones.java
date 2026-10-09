package bo.aportaya.inversiones.web;

import bo.aportaya.inversiones.aplicacion.CU120Catalogo;
import bo.aportaya.inversiones.aplicacion.CU121OrdenarInversion;
import bo.aportaya.inversiones.aplicacion.CU122ConfirmarPosicion;
import bo.aportaya.inversiones.aplicacion.CU123Devengo;
import bo.aportaya.inversiones.aplicacion.CU123Posiciones;
import bo.aportaya.inversiones.aplicacion.CU124SolicitarRescate;
import bo.aportaya.inversiones.aplicacion.CU125LiquidarRescate;
import bo.aportaya.inversiones.aplicacion.VistaOrden;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lo comun de las pruebas del contrato HTTP de {@code /inversiones}: constantes, MockMvc y los
 * casos de uso doblados. La anotacion {@code @PruebaWeb} va en cada clase concreta.
 */
abstract class BaseWebInversiones {

    protected static final String CLAVE = "f0000000-0000-4000-8000-0000000000ff";
    protected static final UUID ORDEN = UUID.fromString("f0000000-0000-4000-8000-000000000001");
    protected static final UUID VERSION = UUID.fromString("f0000000-0000-4000-8000-000000000002");
    protected static final UUID CONSENTIMIENTO = UUID.fromString("f0000000-0000-4000-8000-000000000003");
    protected static final UUID POSICION = UUID.fromString("f0000000-0000-4000-8000-000000000004");
    protected static final UUID RESCATE = UUID.fromString("f0000000-0000-4000-8000-000000000005");
    protected static final UUID CUENTA = UUID.fromString("f0000000-0000-4000-8000-000000000006");
    protected static final String HASH = "ab".repeat(32);

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected CU120Catalogo cu120;

    @MockitoBean
    protected CU121OrdenarInversion cu121;

    @MockitoBean
    protected CU122ConfirmarPosicion cu122;

    @MockitoBean
    protected CU123Posiciones cu123;

    @MockitoBean
    protected CU123Devengo cu123Devengo;

    @MockitoBean
    protected CU124SolicitarRescate cu124;

    @MockitoBean
    protected CU125LiquidarRescate cu125;

    protected static String orden(String monto) {
        return "{\"productoCodigo\":\"DPF-T\",\"versionCondicionesId\":\"" + VERSION + "\",\"textoHash\":\"" + HASH
                + "\",\"cuentaBilleteraId\":\"" + CUENTA + "\",\"monto\":" + monto + "}";
    }

    protected VistaOrden vistaDeOrden(String estado) {
        return new VistaOrden(
                ORDEN,
                estado,
                "DPF-T",
                new BigDecimal("1000.00"),
                "BOB",
                VERSION,
                CONSENTIMIENTO,
                null,
                null,
                null,
                "mensaje");
    }
}

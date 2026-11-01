package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU69FirmarReglamento;
import bo.aportaya.grupos.aplicacion.ConsultarDetalleDelGrupo;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lo comun de las pruebas del detalle del grupo y de la firma del reglamento en el borde (2026-10-20): constantes,
 * MockMvc, los casos de uso doblados y un detalle sintetico. La anotacion {@code @PruebaWeb} va en cada clase
 * concreta, como en {@code BaseWebInversiones}.
 */
abstract class BaseDeDetalleYFirmaWeb {

    protected static final UUID GRUPO = UUID.fromString("c6900000-0000-4000-8000-0000000000f1");
    protected static final UUID CUPO = UUID.fromString("c6900000-0000-4000-8000-0000000000f2");
    protected static final UUID PARTICIPANTE = UUID.fromString("c6900000-0000-4000-8000-0000000000f3");
    protected static final String HASH = "c".repeat(64);

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected ConsultarDetalleDelGrupo detalle;

    @MockitoBean
    protected CU69FirmarReglamento firma;

    /** La etapa minima se nombra con el catalogo de transparencia (doble, catalogo SINTETICO). */
    @MockitoBean
    protected HechosDeOtrosServicios afuera;

    @MockitoBean
    protected bo.aportaya.grupos.aplicacion.ConsultarMisLimites limites;

    protected static ConsultarDetalleDelGrupo.Detalle unDetalle() {
        return new ConsultarDetalleDelGrupo.Detalle(
                GRUPO,
                "Pasanaku del barrio",
                "ABIERTO_A_INSCRIPCION",
                new BigDecimal("500.00"),
                "BOB",
                "MENSUAL",
                2,
                2,
                false,
                true,
                false,
                "Reglamento de prueba.",
                HASH,
                null,
                null,
                List.of(
                        new ConsultarDetalleDelGrupo.Integrante(1, CUPO, "Rosa Quispe", "FIRMADO", true, false, null),
                        new ConsultarDetalleDelGrupo.Integrante(
                                2, UUID.randomUUID(), "Juan Mamani", "PENDIENTE_FIRMA", false, true, null)));
    }
}

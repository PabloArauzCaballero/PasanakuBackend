package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.CalendarioDeAportes;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * H8.S1.M1 · el hecho que `grupos` le da a `aportes` para generar las obligaciones de un periodo:
 * {@code GET /grupos/periodos/{id}/calendario-de-aportes}. Productor del contrato; el consumidor es
 * {@code CalendarioDelPeriodoPorHttp} en `aportes` (su prueba usa un cuerpo con esta misma forma).
 *
 * <p>Tres niveles: lo correcto (titulares, importes y fechas tal cual), el limite (periodo sin cupos
 * ocupados) y lo invalido (sin sesion, sin permiso, periodo desconocido, id mal formado, metodo equivocado).
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
        })
class GruposCalendarioDeAportesWebTest {

    private static final UUID PERIODO = UUID.fromString("c8100000-0000-4000-8000-000000000001");
    private static final UUID GRUPO = UUID.fromString("c8100000-0000-4000-8000-000000000002");
    private static final UUID CUPO_1 = UUID.fromString("c8100000-0000-4000-8000-0000000000a1");
    private static final UUID CUPO_2 = UUID.fromString("c8100000-0000-4000-8000-0000000000a2");
    private static final UUID TITULAR_1 = UUID.fromString("c8100000-0000-4000-8000-0000000000b1");
    private static final UUID TITULAR_2 = UUID.fromString("c8100000-0000-4000-8000-0000000000b2");
    private static final String RUTA = "/grupos/periodos/{id}/calendario-de-aportes";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CanjearInvitacion canjearInvitacion;

    @MockitoBean
    private CU20CrearGrupo cu20;

    @MockitoBean
    private CU59CalcularPlazo cu59;

    @MockitoBean
    private CU64TraspasarCupo cu64;

    @MockitoBean
    private CU65Retirarse cu65;

    @MockitoBean
    private CU68Postular cu68;

    @MockitoBean
    private CU68AceptarIngreso cu68Decision;

    @MockitoBean
    private InvitacionesWeb invitaciones;

    @MockitoBean
    private AdmisionDelGrupo admision;

    @MockitoBean
    private CU69Invitar cu69;

    @MockitoBean
    private Consultas consultas;

    @MockitoBean
    private HechosDeOtrosServicios afuera;

    @MockitoBean
    private RespuestasAOtrosServicios respuestas;

    @MockitoBean
    private SorteoDelGrupo sorteo;

    private static CalendarioDeAportes calendario(List<CalendarioDeAportes.Cupo> cupos) {
        return new CalendarioDeAportes(
                PERIODO,
                GRUPO,
                3,
                "ABIERTO",
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 8),
                LocalDate.of(2026, 6, 12),
                cupos);
    }

    @Test
    @DisplayName("200: devuelve el turno, las fechas y, por cupo ocupado, su titular y su importe como cadena")
    void devuelveElCalendario() throws Exception {
        when(consultas.calendarioDeAportes(eq(PERIODO), any()))
                .thenReturn(Optional.of(calendario(List.of(
                        new CalendarioDeAportes.Cupo(CUPO_1, TITULAR_1, Dinero.de("500.00", Moneda.BOB)),
                        new CalendarioDeAportes.Cupo(CUPO_2, TITULAR_2, Dinero.de("250.00", Moneda.BOB))))));

        mvc.perform(get(RUTA, PERIODO).with(Sesiones.como("ADMIN_PLATAFORMA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodoId").value(PERIODO.toString()))
                .andExpect(jsonPath("$.grupoId").value(GRUPO.toString()))
                .andExpect(jsonPath("$.numero").value(3))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.fechaLimitePago").value("2026-06-05"))
                .andExpect(jsonPath("$.fechaFinGracia").value("2026-06-08"))
                .andExpect(jsonPath("$.fechaEntregaPrevista").value("2026-06-12"))
                .andExpect(jsonPath("$.cupos.length()").value(2))
                .andExpect(jsonPath("$.cupos[0].cupoId").value(CUPO_1.toString()))
                .andExpect(jsonPath("$.cupos[0].participanteId").value(TITULAR_1.toString()))
                .andExpect(jsonPath("$.cupos[0].montoEsperado.monto").value("500.00"))
                .andExpect(jsonPath("$.cupos[0].montoEsperado.moneda").value("BOB"))
                .andExpect(jsonPath("$.cupos[1].montoEsperado.monto").value("250.00"));
        verify(consultas).calendarioDeAportes(eq(PERIODO), any());
    }

    @Test
    @DisplayName("200: un periodo sin cupos ocupados se informa vacio, sin inventar a nadie (limite)")
    void periodoSinCuposOcupados() throws Exception {
        when(consultas.calendarioDeAportes(eq(PERIODO), any())).thenReturn(Optional.of(calendario(List.of())));

        mvc.perform(get(RUTA, PERIODO).with(Sesiones.como("ADMIN_PLATAFORMA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cupos.length()").value(0));
    }

    @Test
    @DisplayName("422 AP-CU21-08: un periodo que no existe no se inventa")
    void periodoDesconocido() throws Exception {
        when(consultas.calendarioDeAportes(eq(PERIODO), any())).thenReturn(Optional.empty());

        mvc.perform(get(RUTA, PERIODO).with(Sesiones.como("ADMIN_PLATAFORMA")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AP-CU21-08"));
    }

    @Test
    @DisplayName("403: un participante, aunque sea titular de un cupo del periodo, no ve a los demas titulares")
    void participanteNoLoVe() throws Exception {
        mvc.perform(get(RUTA, PERIODO).with(Sesiones.como("PARTICIPANTE"))).andExpect(status().isForbidden());
        verifyNoInteractions(consultas);
    }

    @Test
    @DisplayName("403: ni el organizador del grupo con GRUPO_ADMINISTRAR ni un usuario sin permisos")
    void organizadorYSinPermisosNoLoVen() throws Exception {
        mvc.perform(get(RUTA, PERIODO).with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR")))
                .andExpect(status().isForbidden());
        mvc.perform(get(RUTA, PERIODO).with(Sesiones.sinPermisos())).andExpect(status().isForbidden());
        verifyNoInteractions(consultas);
    }

    @Test
    @DisplayName("401: sin sesion")
    void sinSesion() throws Exception {
        mvc.perform(get(RUTA, PERIODO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultas);
    }

    @Test
    @DisplayName("400: un id que no es UUID se rechaza antes de llegar a la consulta")
    void idMalFormado() throws Exception {
        mvc.perform(get(RUTA, "no-es-un-uuid").with(Sesiones.como("ADMIN_PLATAFORMA")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(consultas);
    }

    @Test
    @DisplayName("el calendario solo se lee: no hay POST que lo escriba")
    void soloLectura() throws Exception {
        mvc.perform(post(RUTA, PERIODO).with(Sesiones.como("ADMIN_PLATAFORMA"))).andExpect(status().is4xxClientError());
        verifyNoInteractions(consultas);
    }
}

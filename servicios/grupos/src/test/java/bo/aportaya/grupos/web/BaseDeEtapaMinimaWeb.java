package bo.aportaya.grupos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU20CrearGrupo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68Postular;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.PoliticaDelGrupo;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.Sesiones;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Lo comun de las pruebas de la etapa minima en el borde (2026-10-30): constantes, MockMvc, los casos de uso doblados,
 * un grupo que pide MEDIA y las peticiones de cada camino. La anotacion {@code @PruebaWeb} va en cada clase concreta,
 * como en {@code BaseWebInversiones}.
 */
abstract class BaseDeEtapaMinimaWeb {

    protected static final UUID GRUPO = UUID.fromString("a7e00000-0000-4000-8000-000000000001");
    protected static final UUID SOLICITUD = UUID.fromString("a7e00000-0000-4000-8000-000000000002");
    protected static final UUID INVITACION = UUID.fromString("a7e00000-0000-4000-8000-000000000003");
    protected static final UUID CUPO = UUID.fromString("a7e00000-0000-4000-8000-000000000004");
    protected static final UUID ENTRANTE = UUID.fromString("a7e00000-0000-4000-8000-000000000005");
    protected static final String CLAVE = "a7e00000-0000-4000-8000-0000000000ff";
    protected static final String CODIGO = UUID.randomUUID() + "." + "ab".repeat(32);

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected CU20CrearGrupo cu20;

    @MockitoBean
    protected CU64TraspasarCupo cu64;

    @MockitoBean
    protected CU68Postular cu68;

    @MockitoBean
    protected CU68AceptarIngreso cu68Decision;

    @MockitoBean
    protected CU69Invitar cu69;

    @MockitoBean
    protected CanjearInvitacion canjes;

    @MockitoBean
    protected CU69EnlaceDeGrupo enlacesDeGrupo;

    @MockitoBean
    protected Consultas consultas;

    @MockitoBean
    protected HechosDeOtrosServicios afuera;

    @BeforeEach
    void grupoQuePideMedia() {
        // 10 cupos de 500.00 BOB: justo lo que desbloquea MEDIA.
        when(consultas.condicionesDeIngreso(eq(GRUPO), any()))
                .thenReturn(EtapasDePrueba.grupo(GRUPO, EtapasDePrueba.MEDIA, 10, "500.00"));
        when(consultas.politicaDelGrupo(eq(GRUPO), any()))
                .thenReturn(Optional.of(new PoliticaDelGrupo("BASICO", 500, new BigDecimal("0.600"), 1)));
        when(afuera.restriccion(any())).thenReturn(new HechosDeOtrosServicios.Restriccion(false, BigDecimal.ZERO));
        when(afuera.reputacion(any())).thenReturn(new HechosDeOtrosServicios.Reputacion(true, new BigDecimal("400")));
        when(afuera.nivelDeKyc(any())).thenReturn("BASICO");
    }

    protected ResultActions postular() throws Exception {
        return mvc.perform(post("/grupos/{g}/postulaciones", GRUPO)
                .with(Sesiones.como("PARTICIPANTE"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cuposSolicitados\":1}"));
    }

    protected static void rechazoPorEtapa(ResultActions r, String codigo) throws Exception {
        r.andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.detalle.etapaMinima").value("MEDIA"));
    }

    protected ResultActions canjear() throws Exception {
        return mvc.perform(post("/grupos/{g}/invitaciones/{i}/canje", GRUPO, INVITACION)
                .with(Sesiones.como("PARTICIPANTE"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + "a".repeat(64) + "\",\"cuposSolicitados\":1}"));
    }

    protected ResultActions decidir(String decision) throws Exception {
        return mvc.perform(post("/grupos/solicitudes/{s}/decision", SOLICITUD)
                .with(Sesiones.como("ORGANIZADOR", "GRUPO_ADMINISTRAR"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\"}"));
    }

    protected ResultActions traspasar() throws Exception {
        return mvc.perform(post("/grupos/{g}/cupos/{c}/traspasos", GRUPO, CUPO)
                .with(Sesiones.como("PARTICIPANTE"))
                .header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioEntranteId\":\"" + ENTRANTE + "\",\"motivo\":\"Me mudo de ciudad.\"}"));
    }
}

package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bo.aportaya.grupos.aplicacion.CU69Enlace;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.EntradaAceptacionInvitacion;
import bo.aportaya.grupos.web.generado.modelo.EntradaEnlaceInvitacion;
import bo.aportaya.grupos.web.generado.modelo.EntradaInvitacion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Las decisiones entre servicios ocurren antes de aceptar la invitación. */
class InvitacionesWebTest {
    private static final String TELEFONO = "+59176543210";
    private static final String SECRETO = "a".repeat(64);

    private final UUID grupoId = UUID.randomUUID();
    private final UUID tokenId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final ContextoSesion ctx = ContextoSesion.de(
            usuarioId, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    private final CU69Invitar invitar = mock(CU69Invitar.class);
    private final CU69Enlace enlaces = mock(CU69Enlace.class);
    private final Consultas consultas = mock(Consultas.class);
    private final HechosDeOtrosServicios afuera = mock(HechosDeOtrosServicios.class);
    private final SesionDeLaPeticion sesion = mock(SesionDeLaPeticion.class);
    private final HttpServletRequest peticion = mock(HttpServletRequest.class);
    private final InvitacionesWeb web = new InvitacionesWeb(invitar, enlaces, consultas, afuera, sesion, peticion, 3);

    @BeforeEach
    void sesionAutenticada() {
        when(sesion.actual()).thenReturn(ctx);
    }

    @Test
    void soloEntregaElEnlaceCuandoLaInvitacionFueCreada() {
        var cuerpo = new EntradaInvitacion(TELEFONO, EntradaInvitacion.CanalEnum.ENLACE);
        when(afuera.usuarioDelTelefono(TELEFONO)).thenReturn(Optional.empty());
        when(afuera.tokenDeInvitacion(eq("ENLACE"), any()))
                .thenReturn(new HechosDeOtrosServicios.TokenDeInvitacion(tokenId, SECRETO));
        when(invitar.invitar(any(), eq(ctx)))
                .thenReturn(new CU69Invitar.Resultado(Optional.of(UUID.randomUUID()), "Lista"));

        var salida = web.invitar(grupoId, cuerpo);

        assertThat(salida.getEnlace()).isEqualTo("aportaya://unirse/" + tokenId + "." + SECRETO);
        assertThat(salida.getMensaje()).isEqualTo("Lista");
    }

    @Test
    void respetaSupresionYParticipanteExistenteSinEmitirToken() {
        var cuerpo = new EntradaInvitacion(TELEFONO, EntradaInvitacion.CanalEnum.ENLACE);
        when(invitar.invitar(any(), eq(ctx)))
                .thenReturn(new CU69Invitar.Resultado(Optional.empty(), "Solicitud recibida"));
        when(afuera.contactoSuprimido(TELEFONO, "INVITACION_GRUPO")).thenReturn(true);

        assertThat(web.invitar(grupoId, cuerpo).getEnlace()).isNull();
        verify(afuera, never()).tokenDeInvitacion(any(), any());

        when(afuera.contactoSuprimido(TELEFONO, "INVITACION_GRUPO")).thenReturn(false);
        when(afuera.usuarioDelTelefono(TELEFONO)).thenReturn(Optional.of(usuarioId));
        when(consultas.yaEsParticipante(grupoId, usuarioId, ctx)).thenReturn(true);

        assertThat(web.invitar(grupoId, cuerpo).getEnlace()).isNull();
        verify(afuera, never()).tokenDeInvitacion(any(), any());
    }

    @Test
    void consultaValidaElSecretoAntesDeMostrarElReglamento() {
        when(enlaces.datosDe(tokenId, ctx)).thenReturn(detalle());
        var cuerpo = new EntradaEnlaceInvitacion(tokenId, SECRETO);

        assertThatThrownBy(() -> web.consultar(cuerpo))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no es valida");

        when(afuera.enlaceDeInvitacionValido(tokenId, SECRETO, TELEFONO, "NINGUNO"))
                .thenReturn(true);
        var salida = web.consultar(cuerpo);
        assertThat(salida.getGrupoId()).isEqualTo(grupoId);
        assertThat(salida.getReglamento()).isEqualTo("Reglamento visible");
    }

    @Test
    void aceptarExigeTokenRestriccionReputacionYReglamento() {
        when(enlaces.datosDe(tokenId, ctx)).thenReturn(detalle());
        var cuerpo = new EntradaAceptacionInvitacion(tokenId, SECRETO, "hash", true);

        assertThatThrownBy(() -> web.aceptar(cuerpo))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no es valida");

        when(afuera.enlaceDeInvitacionValido(tokenId, SECRETO, TELEFONO, "BASICO"))
                .thenReturn(true);
        when(afuera.restriccion(usuarioId)).thenReturn(new HechosDeOtrosServicios.Restriccion(true, BigDecimal.TEN));
        assertThatThrownBy(() -> web.aceptar(cuerpo))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("restricción");

        when(afuera.restriccion(usuarioId)).thenReturn(new HechosDeOtrosServicios.Restriccion(false, BigDecimal.ZERO));
        when(afuera.reputacion(usuarioId)).thenReturn(new HechosDeOtrosServicios.Reputacion(true, BigDecimal.ONE));
        assertThatThrownBy(() -> web.aceptar(cuerpo))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("reputación");

        when(afuera.reputacion(usuarioId)).thenReturn(new HechosDeOtrosServicios.Reputacion(true, BigDecimal.TEN));
        cuerpo.setAceptaReglamento(false);
        assertThatThrownBy(() -> web.aceptar(cuerpo))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("reglamento");

        cuerpo.setAceptaReglamento(true);
        UUID participanteId = UUID.randomUUID();
        when(peticion.getRemoteAddr()).thenReturn("127.0.0.1");
        when(enlaces.aceptar(tokenId, "hash", "127.0.0.1", BigDecimal.TEN, ctx))
                .thenReturn(new CU69Enlace.Aceptada(grupoId, participanteId));
        assertThat(web.aceptar(cuerpo).getParticipanteId()).isEqualTo(participanteId);
    }

    private CU69Enlace.Detalle detalle() {
        return new CU69Enlace.Detalle(
                grupoId,
                TELEFONO,
                UUID.randomUUID(),
                "Grupo",
                BigDecimal.TEN,
                "BOB",
                "MENSUAL",
                "BASICO",
                BigDecimal.TEN,
                UUID.randomUUID(),
                "Reglamento visible",
                "hash");
    }
}

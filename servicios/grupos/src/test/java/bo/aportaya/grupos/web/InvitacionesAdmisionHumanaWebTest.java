package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bo.aportaya.grupos.aplicacion.CU69Enlace;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.EntradaAceptacionInvitacion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Interruptor {@code aportaya.grupos.admision.humana-obligatoria}, puerta 1: aceptar por enlace (heredada de TEST) crea al
 * participante ACTIVO sin expediente humano. Con el interruptor ENCENDIDO se cierra sin tocar nada (ni identidad, ni cupos); con
 * el APAGADO conserva el comportamiento de TEST. DECISION PENDIENTE de negocio y cumplimiento cual es el modo definitivo.
 */
class InvitacionesAdmisionHumanaWebTest {
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

    private InvitacionesWeb web(boolean humanaObligatoria) {
        when(sesion.actual()).thenReturn(ctx);
        return new InvitacionesWeb(invitar, enlaces, consultas, afuera, sesion, peticion, 3, humanaObligatoria);
    }

    @Test
    @DisplayName("ENCENDIDO · aceptar por enlace se rechaza con AP-CU69-10 y no valida, consume ni crea nada")
    void encendidoCierraLaPuerta() {
        var web = web(true);
        var cuerpo = new EntradaAceptacionInvitacion(tokenId, SECRETO, "hash", true);

        assertThatThrownBy(() -> web.aceptar(cuerpo)).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo("AP-CU69-10"));

        verifyNoInteractions(enlaces, afuera, invitar);
    }

    @Test
    @DisplayName("APAGADO · aceptar por enlace conserva el comportamiento de TEST: crea al participante")
    void apagadoConservaElComportamientoDeTest() {
        var web = web(false);
        UUID participante = UUID.randomUUID();
        when(enlaces.datosDe(tokenId, ctx))
                .thenReturn(new CU69Enlace.Detalle(
                        grupoId,
                        TELEFONO,
                        UUID.randomUUID(),
                        "Grupo",
                        new BigDecimal("500.00"),
                        "BOB",
                        "MENSUAL",
                        "BASICO",
                        BigDecimal.ONE,
                        UUID.randomUUID(),
                        "Reglamento",
                        "hash"));
        when(afuera.enlaceDeInvitacionValido(tokenId, SECRETO, TELEFONO, "BASICO"))
                .thenReturn(true);
        when(afuera.restriccion(usuarioId)).thenReturn(new HechosDeOtrosServicios.Restriccion(false, BigDecimal.ZERO));
        when(afuera.reputacion(usuarioId)).thenReturn(new HechosDeOtrosServicios.Reputacion(true, BigDecimal.TEN));
        when(peticion.getRemoteAddr()).thenReturn("127.0.0.1");
        when(enlaces.aceptar(tokenId, "hash", "127.0.0.1", BigDecimal.TEN, ctx))
                .thenReturn(new CU69Enlace.Aceptada(grupoId, participante));

        var salida = web.aceptar(new EntradaAceptacionInvitacion(tokenId, SECRETO, "hash", true));

        assertThat(salida.getParticipanteId()).isEqualTo(participante);
    }
}

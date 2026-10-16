package bo.aportaya.entregas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.pruebas.DependidoDeEnsayo;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/**
 * {@link HechosDeGruposPorHttp} contra un {@code grupos} de ensayo con la forma de
 * {@code GET /grupos/participaciones}, en tres niveles. Lo que se fija: responde SOLO sobre la persona de la
 * sesion, y que cualquier cosa que no sea una respuesta entendible deniega (vacio), nunca «es miembro».
 */
class HechosDeGruposPorHttpTest {

    private static final UUID PERSONA = UUID.fromString("fa000000-0000-4000-8000-000000000001");
    private static final UUID OTRA = UUID.fromString("fa000000-0000-4000-8000-000000000002");
    private static final UUID GRUPO_A = UUID.fromString("fa000000-0000-4000-8000-0000000000a1");
    private static final UUID GRUPO_B = UUID.fromString("fa000000-0000-4000-8000-0000000000b1");
    private static final UUID PARTICIPANTE = UUID.fromString("fa000000-0000-4000-8000-0000000000c1");

    private static final String PARTICIPACIONES =
            "[{\"grupoId\":\"" + GRUPO_A + "\",\"participanteId\":\"" + PARTICIPANTE + "\",\"estado\":\"ACTIVO\"},"
                    + "{\"grupoId\":\"" + GRUPO_B + "\",\"participanteId\":\"" + UUID.randomUUID()
                    + "\",\"estado\":\"SUSPENDIDO\"}]";

    private final DependidoDeEnsayo grupos = new DependidoDeEnsayo();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        grupos.close();
    }

    private HechosDeGruposPorHttp adaptador() {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(300);
        fabrica.setReadTimeout(300);
        return new HechosDeGruposPorHttp(
                RestClient.builder().requestFactory(fabrica), grupos.urlBase(), ConfiguracionDeCircuito.porOmision());
    }

    private static void sesionDe(UUID persona) {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .subject(persona.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    @DisplayName("CORRECTO · la membresia sale de las participaciones de la sesion: activo, no activo y ajeno")
    void membresia() {
        grupos.responder(200, PARTICIPACIONES);
        sesionDe(PERSONA);
        var hechos = adaptador();

        assertThat(hechos.miembro(GRUPO_A, PERSONA)).hasValueSatisfying(m -> {
            assertThat(m.participanteId()).isEqualTo(PARTICIPANTE);
            assertThat(m.activo()).isTrue();
        });
        assertThat(hechos.miembro(GRUPO_B, PERSONA))
                .hasValueSatisfying(m -> assertThat(m.activo()).isFalse());
        assertThat(hechos.miembro(UUID.randomUUID(), PERSONA))
                .as("no es miembro de ese grupo: no se sabe de el, se deniega")
                .isEmpty();
        assertThat(hechos.gruposActivosDe(PERSONA)).contains(java.util.Set.of(GRUPO_A));
    }

    @Test
    @DisplayName("LIMITE · sin participaciones el conjunto es vacio pero SABIDO; el turno no lo publica grupos (vacio)")
    void sinParticipacionesYTurno() {
        grupos.responder(200, "[]");
        sesionDe(PERSONA);
        var hechos = adaptador();

        assertThat(hechos.gruposActivosDe(PERSONA)).contains(java.util.Set.of());
        assertThat(hechos.turno(UUID.randomUUID()))
                .as("grupos no expone la consulta de turno: publicar una oferta sigue denegado")
                .isEmpty();
    }

    @Test
    @DisplayName("INVALIDO · sin sesion, o preguntando por OTRA persona, no sale ninguna peticion")
    void soloLaPersonaDeLaSesion() {
        grupos.responder(200, PARTICIPACIONES);
        var hechos = adaptador();

        assertThat(hechos.miembro(GRUPO_A, PERSONA)).isEmpty();
        sesionDe(OTRA);
        assertThat(hechos.miembro(GRUPO_A, PERSONA)).isEmpty();
        assertThat(hechos.gruposActivosDe(PERSONA)).isEmpty();
        assertThat(grupos.peticionesRecibidas()).isZero();
    }

    @Test
    @DisplayName("INVALIDO · 401, 500, JSON roto, lento y caido: nunca se supone que alguien es miembro")
    void respuestasQueNoSonElContrato() {
        sesionDe(PERSONA);
        for (int estado : new int[] {401, 403, 500}) {
            grupos.responder(estado, "{}");
            assertThat(adaptador().miembro(GRUPO_A, PERSONA))
                    .as("HTTP %d", estado)
                    .isEmpty();
        }
        grupos.responder(200, "esto no es json");
        assertThat(adaptador().miembro(GRUPO_A, PERSONA)).isEmpty();
        assertThat(adaptador().gruposActivosDe(PERSONA)).isEmpty();

        grupos.responder(200, PARTICIPACIONES).tardar(1500);
        assertThat(adaptador().miembro(GRUPO_A, PERSONA))
                .as("lento: tiempo maximo")
                .isEmpty();

        grupos.caer();
        assertThat(adaptador().miembro(GRUPO_A, PERSONA)).as("caido").isEmpty();
    }
}

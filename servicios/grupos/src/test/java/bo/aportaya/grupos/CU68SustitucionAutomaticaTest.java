package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bo.aportaya.grupos.aplicacion.SustitucionAutomatica;
import bo.aportaya.grupos.aplicacion.SustitucionAutomatica.Resultado;
import bo.aportaya.grupos.aplicacion.SustituirAdministrador;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.infraestructura.SustitucionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * H5.S1.M7 · la sustitucion del administrador al REVOCARSE la habilitacion del organizador, detras del interruptor
 * {@code aportaya.grupos.sustitucion.automatica}. Manual por defecto: solo se avisa a backoffice. Automatico: se sustituye con
 * una regla sintetica (el activo mas antiguo que organizador confirma habilitado). Los dos modos, contra PostgreSQL real.
 * DECISION PENDIENTE de negocio y cumplimiento sobre cual rige.
 */
class CU68SustitucionAutomaticaTest extends BaseDeCU68 {

    private final HechosDeOtrosServicios afuera = mock(HechosDeOtrosServicios.class);
    private UUID organizador;
    private UUID grupo;
    private List<UUID> participantes;
    private List<UUID> usuarios;

    @BeforeEach
    void prepararGrupo() {
        UUID duenoUsuario = fixtura.usuario();
        organizador = UUID.randomUUID();
        dslFixtura.execute(
                "INSERT INTO organizador.organizador (id, usuario_id, estado, nivel, limite_grupos_simultaneos, limite_monto_administrado, grupos_activos, grupos_historicos, calificacion_promedio, indice_morosidad_cartera, fecha_postulacion) VALUES (?,?,'SUSPENDIDO','ESTANDAR',100,100000,0,0,5,0,now())",
                organizador,
                duenoUsuario);
        grupo = fixtura.grupoConformado(4);
        var creados = fixtura.participantesConCupo(grupo, 4);
        participantes = creados;
        usuarios = creados.stream()
                .map(p -> dslFixtura
                        .fetchOne("SELECT usuario_id FROM grupos.participante WHERE id=?", p)
                        .get(0, UUID.class))
                .toList();
        // El primero administra; los demas ingresaron despues, en ese orden (fecha_ingreso ascendente).
        dslFixtura.execute("UPDATE grupos.participante SET es_organizador=true WHERE id=?", participantes.get(0));
        dslFixtura.execute("UPDATE grupos.grupo SET organizador_id=? WHERE id=?", organizador, grupo);
        for (int i = 0; i < participantes.size(); i++) {
            dslFixtura.execute(
                    "UPDATE grupos.participante SET fecha_ingreso = now() - make_interval(days => ?) WHERE id=?",
                    10 - i,
                    participantes.get(i));
        }
    }

    private SustitucionAutomatica reaccion(boolean automatica) {
        var datos = new Datos(dsl);
        var sustituir = new SustituirAdministrador(
                datos, new SustitucionRepositorio(), new Outbox("grupos"), Reloj.delSistema());
        return new SustitucionAutomatica(
                datos, transaccion, new SustitucionRepositorio(), sustituir, afuera, new Outbox("grupos"), automatica);
    }

    private void habilitados(UUID... usuariosHabilitados) {
        // Mockito: re-stubbing un metodo que ya lanza obliga a limpiar el doble antes.
        org.mockito.Mockito.reset(afuera);
        when(afuera.organizadorHabilitadoDelUsuario(any())).thenReturn(Optional.empty());
        for (UUID u : usuariosHabilitados) {
            when(afuera.organizadorHabilitadoDelUsuario(u)).thenReturn(Optional.of(UUID.randomUUID()));
        }
    }

    private UUID administrador() {
        return dslFixtura
                .fetchOne("SELECT id FROM grupos.participante WHERE grupo_id=? AND es_organizador", grupo)
                .get(0, UUID.class);
    }

    private int avisosDeBackoffice() {
        return contar(
                "SELECT count(*)::int FROM grupos.evento_dominio WHERE tipo='grupos.sustitucion_requerida' AND agregado_id=?",
                grupo);
    }

    private int sustituciones() {
        return contar("SELECT count(*)::int FROM grupos.sustitucion_administrador WHERE grupo_id=?", grupo);
    }

    @Test
    // MANUAL (por omision) · la revocacion NO cambia al administrador: se avisa a backoffice una sola vez por evento
    void manualSoloAvisa() {
        habilitados(usuarios.get(1), usuarios.get(2));
        UUID evento = UUID.randomUUID();

        var efectos = reaccion(false).alRevocarseElOrganizador(evento, organizador, UUID.randomUUID());

        assertThat(efectos).singleElement().satisfies(e -> {
            assertThat(e.grupoId()).isEqualTo(grupo);
            assertThat(e.resultado()).isEqualTo(Resultado.ESCALADO_A_BACKOFFICE);
        });
        assertThat(administrador()).isEqualTo(participantes.get(0));
        assertThat(sustituciones()).isZero();
        assertThat(avisosDeBackoffice()).isEqualTo(1);

        // Duplicado: el mismo evento entregado dos veces no repite el aviso.
        reaccion(false).alRevocarseElOrganizador(evento, organizador, UUID.randomUUID());
        assertThat(avisosDeBackoffice()).isEqualTo(1);
    }

    @Test
    // AUTOMATICA · sustituye por el participante activo mas antiguo que organizador confirma habilitado, y deja el
    // registro
    void automaticaSustituyePorElMasAntiguoHabilitado() {
        // El segundo (mas antiguo) NO esta habilitado: queda el tercero.
        habilitados(usuarios.get(2), usuarios.get(3));
        UUID evento = UUID.randomUUID();

        var efectos = reaccion(true).alRevocarseElOrganizador(evento, organizador, UUID.randomUUID());

        assertThat(efectos).singleElement().satisfies(e -> {
            assertThat(e.resultado()).isEqualTo(Resultado.SUSTITUIDO);
            assertThat(e.entranteParticipanteId()).contains(participantes.get(2));
        });
        assertThat(administrador()).isEqualTo(participantes.get(2));
        assertThat(contar("SELECT count(*)::int FROM grupos.participante WHERE grupo_id=? AND es_organizador", grupo))
                .isEqualTo(1);
        // El saliente conserva su participacion y sus cupos (no se retira).
        assertThat(dslFixtura
                        .fetchOne("SELECT estado FROM grupos.participante WHERE id=?", participantes.get(0))
                        .get(0))
                .isEqualTo("ACTIVO");
        assertThat(sustituciones()).isEqualTo(1);
        var motivo = dslFixtura
                .fetchOne("SELECT motivo FROM grupos.sustitucion_administrador WHERE grupo_id=?", grupo)
                .get(0, String.class);
        assertThat(motivo).startsWith("AUTOMATICA");
        assertThat(avisosDeBackoffice()).isZero();
    }

    @Test
    // AUTOMATICA · reintento del mismo evento: una sola sustitucion y el mismo resultado (idempotencia)
    void automaticaEsIdempotente() {
        habilitados(usuarios.get(2));
        UUID evento = UUID.randomUUID();
        var primera = reaccion(true).alRevocarseElOrganizador(evento, organizador, UUID.randomUUID());

        var segunda = reaccion(true).alRevocarseElOrganizador(evento, organizador, UUID.randomUUID());

        assertThat(segunda).isEqualTo(primera);
        assertThat(sustituciones()).isEqualTo(1);
        assertThat(administrador()).isEqualTo(participantes.get(2));
        assertThat(contar(
                        "SELECT count(*)::int FROM grupos.evento_dominio WHERE tipo='grupos.administrador_sustituido' AND agregado_id=?",
                        grupo))
                .isEqualTo(1);
    }

    @Test
    // AUTOMATICA · si nadie esta habilitado no se sustituye a nadie: se escala a backoffice con su motivo
    void automaticaSinCandidatoEscala() {
        habilitados();

        var efectos = reaccion(true).alRevocarseElOrganizador(UUID.randomUUID(), organizador, UUID.randomUUID());

        assertThat(efectos).singleElement().satisfies(e -> assertThat(e.resultado())
                .isEqualTo(Resultado.SIN_CANDIDATO_HABILITADO));
        assertThat(administrador()).isEqualTo(participantes.get(0));
        assertThat(sustituciones()).isZero();
        assertThat(avisosDeBackoffice()).isEqualTo(1);
        assertThat(dslFixtura
                        .fetchOne(
                                "SELECT payload->>'motivo' FROM grupos.evento_dominio WHERE tipo='grupos.sustitucion_requerida' AND agregado_id=?",
                                grupo)
                        .get(0, String.class))
                .isEqualTo("SIN_CANDIDATO_HABILITADO");
    }

    @Test
    // AUTOMATICA · si organizador no responde (5xx) el evento falla cerrado: no cambia nadie y se puede reprocesar
    void automaticaConOrganizadorCaidoFallaCerrado() {
        when(afuera.organizadorHabilitadoDelUsuario(any()))
                .thenThrow(new ErrorDeDominio("El servicio organizador respondio 503"));
        UUID evento = UUID.randomUUID();

        assertThatThrownBy(() -> reaccion(true).alRevocarseElOrganizador(evento, organizador, UUID.randomUUID()))
                .isInstanceOf(ErrorDeDominio.class);

        assertThat(administrador()).isEqualTo(participantes.get(0));
        assertThat(sustituciones()).isZero();
        // No quedo marcado como consumido: el reproceso puede completarlo.
        habilitados(usuarios.get(1));
        reaccion(true).alRevocarseElOrganizador(evento, organizador, UUID.randomUUID());
        assertThat(administrador()).isEqualTo(participantes.get(1));
    }

    @Test
    // rechaza la sustitucion automatica pedida por una persona sin rol de sistema: solo el proceso la invoca
    void rechazaLaAutomaticaDePersonas() {
        var sustituir = new SustituirAdministrador(
                new Datos(dsl), new SustitucionRepositorio(), new Outbox("grupos"), Reloj.delSistema());
        var persona = ContextoSesion.de(
                usuarios.get(1), "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));

        assertThatThrownBy(() -> transaccion.execute(tx -> sustituir.ejecutarAutomatica(
                        new SustituirAdministrador.Entrada(grupo, usuarios.get(1), UUID.randomUUID(), "yo mismo"),
                        persona)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU68-08"));
        assertThat(administrador()).isEqualTo(participantes.get(0));
    }

    @Test
    // solo los grupos VIVOS de ese organizador se tocan: uno finalizado y uno de otro organizador quedan igual
    void soloLosGruposVivosDeEseOrganizador() {
        dslFixtura.execute("UPDATE grupos.grupo SET estado='FINALIZADO' WHERE id=?", grupo);
        habilitados(usuarios.get(1));

        var efectos = reaccion(true).alRevocarseElOrganizador(UUID.randomUUID(), organizador, UUID.randomUUID());

        assertThat(efectos).isEmpty();
        assertThat(administrador()).isEqualTo(participantes.get(0));

        var otro = reaccion(true).alRevocarseElOrganizador(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertThat(otro).isEmpty();
    }
}

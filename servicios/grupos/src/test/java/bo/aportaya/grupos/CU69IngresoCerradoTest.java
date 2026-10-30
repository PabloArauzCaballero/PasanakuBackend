package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68Postular.EntradaPostulacion;
import bo.aportaya.grupos.aplicacion.CU69Enlace;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.aplicacion.CU69IniciarGrupo;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.infraestructura.AdmisionRepositorio;
import bo.aportaya.grupos.infraestructura.CalendarioDelGrupoRepositorio;
import bo.aportaya.grupos.infraestructura.EmparejamientoRepositorio;
import bo.aportaya.grupos.infraestructura.InvitacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · Una vez iniciado, el grupo no recibe a nadie mas por NINGUN camino (decision del dueño 2026-10-30). Antes,
 * el enlace de invitacion y el enlace del grupo aceptaban tambien grupos CONFORMADO. Sobre PostgreSQL real; datos
 * sinteticos.
 *
 * <p>El grupo tiene 4 cupos, 3 ocupados (organizador incluido) y uno libre: ceil(0,75 × 4) = 3, se puede iniciar. Antes
 * de iniciarlo quedan sembrados una invitacion vigente, un enlace del grupo, una solicitud pendiente con la propuesta
 * de aceptarla y otra solicitud pendiente sin propuesta. Despues de iniciarlo, cada camino responde AP-CU69-17 y nadie
 * entra.
 */
class CU69IngresoCerradoTest extends BaseDeCU68 {

    private static final String HASH = "e".repeat(64);

    private CU69Invitar invitar;
    private CU69Enlace enlace;
    private CanjearInvitacion canjes;
    private CU69EnlaceDeGrupo enlacesDeGrupo;
    private CU68AceptarIngreso admision;
    private CU69IniciarGrupo inicio;

    private UUID grupo;
    private UUID organizador;
    private UUID invitacion;
    private String codigoDelEnlace;
    private UUID conPropuesta;
    private UUID propuesta;
    private UUID sinPropuesta;

    @BeforeEach
    void preparar() {
        invitar = new CU69Invitar(
                new Datos(dsl), new InvitacionRepositorio(), new Outbox("grupos"), Reloj.delSistema(), Ids.seguros());
        enlace = new CU69Enlace(new Datos(dsl), new Outbox("grupos"));
        canjes = new CanjearInvitacion(new Datos(dsl), invitar, postularCU);
        enlacesDeGrupo = new CU69EnlaceDeGrupo(
                new Datos(dsl),
                new AdmisionRepositorio(),
                new EmparejamientoRepositorio(),
                postularCU,
                Reloj.delSistema(),
                7);
        admision = new CU68AceptarIngreso(
                new Datos(dsl), new AdmisionRepositorio(), new Outbox("grupos"), Reloj.delSistema());
        inicio = new CU69IniciarGrupo(
                new Datos(dsl), new AdmisionRepositorio(), new CalendarioDelGrupoRepositorio(), Reloj.delSistema(), 75);
        criterioVigente();
        sembrarGrupoAbierto();

        var emitida = transaccion.execute(t -> invitar.invitar(
                new CU69Invitar.EntradaInvitacion(
                        grupo,
                        "+59176100001",
                        "Contacto",
                        "ENLACE",
                        false,
                        false,
                        3,
                        fixtura.tokenDeInvitacion(),
                        OffsetDateTime.now().plusDays(7)),
                de(organizador)));
        invitacion = emitida.invitacionId().orElseThrow();
        codigoDelEnlace = transaccion
                .execute(t -> enlacesDeGrupo.generar(grupo, de(organizador)))
                .codigo();
        UUID quienPidio = fixtura.usuario();
        conPropuesta = transaccion
                .execute(t -> postularCU.postular(entrada(), de(quienPidio)))
                .solicitudId();
        propuesta = transaccion
                .execute(t -> admision.proponer(
                        new CU68AceptarIngreso.Entrada(
                                conPropuesta, UUID.randomUUID(), "ACEPTAR", "Lo conozco.", 0, null),
                        de(organizador)))
                .id();
        UUID otroQuePidio = fixtura.usuario();
        sinPropuesta = transaccion
                .execute(t -> postularCU.postular(entrada(), de(otroQuePidio)))
                .solicitudId();

        var iniciado = transaccion.execute(t -> inicio.iniciar(grupo, UUID.randomUUID(), de(organizador)));
        assertThat(iniciado.cuposTotales()).isEqualTo(3);
    }

    /** 4 cupos: el organizador y dos integrantes firmaron (OCUPADO), el 4 esta LIBRE. Con reglamento y configuracion. */
    private void sembrarGrupoAbierto() {
        grupo = fixtura.grupoAbierto(4);
        organizador = fixtura.usuario();
        for (int numero = 1; numero <= 4; numero++) {
            UUID participante = null;
            if (numero <= 3) {
                participante = UUID.randomUUID();
                dslFixtura.execute(
                        """
                        INSERT INTO grupos.participante
                          (id, grupo_id, usuario_id, estado, es_organizador, fecha_ingreso,
                           reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                        VALUES (?, ?, ?, 'ACTIVO', ?, now(), 0, 0, 0)
                        """,
                        participante,
                        grupo,
                        numero == 1 ? organizador : fixtura.usuario(),
                        numero == 1);
            }
            dslFixtura.execute(
                    """
                    INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en)
                    VALUES (gen_random_uuid(), ?, ?, ?, ?, 1.00, now())
                    """,
                    grupo,
                    (short) numero,
                    participante,
                    participante == null ? "LIBRE" : "OCUPADO");
        }
        dslFixtura.execute("UPDATE grupos.grupo SET cupos_ocupados = 3 WHERE id = ?", grupo);
        dslFixtura.execute(
                """
                INSERT INTO grupos.reglamento_grupo
                  (grupo_id, version, contenido, hash_contenido, clausulas_mora, clausulas_abandono, vigente_desde,
                   redactado_por)
                VALUES (?, 1, 'Reglamento de prueba.', ?, 'Recargo.', 'Retiro.', now(), ?)
                """,
                grupo,
                HASH,
                organizador);
        dslFixtura.execute(
                """
                INSERT INTO grupos.configuracion_grupo
                  (grupo_id, permite_cupos_multiples, max_cupos_por_persona, permite_permuta_turnos,
                   requiere_avalista, permite_ingreso_tardio, hora_limite_pago, tolerancia_monto_parcial)
                VALUES (?, false, 1, false, false, false, TIME '23:59', 0.00)
                """,
                grupo);
    }

    private EntradaPostulacion entrada() {
        BigDecimal media = new BigDecimal("0.50");
        return new EntradaPostulacion(
                grupo, (short) 1, "quiero entrar", false, BigDecimal.ZERO, true, 700, 0, media, media, media, media);
    }

    private static ContextoSesion de(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    private static void cerrado(ThrowingCallable intento) {
        assertThatThrownBy(intento).isInstanceOf(ErrorDeNegocio.class).satisfies(e -> assertThat(
                        ((ErrorDeNegocio) e).codigo().valor())
                .isEqualTo("AP-CU69-17"));
    }

    private int integrantes() {
        return contar("SELECT count(*) FROM grupos.participante WHERE grupo_id = ? AND estado <> 'RETIRADO'", grupo);
    }

    @Test
    @DisplayName("CU-69 · iniciado: aceptar por el enlace de invitacion responde AP-CU69-17 y nadie entra")
    void aceptarPorEnlaceDeInvitacion() {
        UUID token = (UUID) dsl.fetchValue("SELECT token_id FROM grupos.invitacion WHERE id = ?", invitacion);
        UUID invitado = fixtura.usuario();

        cerrado(() -> transaccion.execute(
                t -> enlace.aceptar(token, UUID.randomUUID(), HASH, "127.0.0.1", BigDecimal.ZERO, de(invitado))));

        assertThat(integrantes()).isEqualTo(3);
        assertThat(contar("SELECT count(*) FROM grupos.invitacion WHERE id = ? AND estado = 'ENVIADA'", invitacion))
                .as("la invitacion no se consumio")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("CU-69 · iniciado: el canje responde AP-CU69-17 ANTES de consumir el enlace en identidad")
    void canjeAntesDeConsumir() {
        UUID invitado = fixtura.usuario();
        cerrado(() -> transaccion.executeWithoutResult(
                t -> canjes.comprobarAntesDeConsumir(invitacion, entrada(), de(invitado))));
    }

    @Test
    @DisplayName("CU-69 · iniciado: postular y pedir por el enlace del grupo responden AP-CU69-17, sin solicitud nueva")
    void postularYEnlaceDelGrupo() {
        int solicitudes = contar("SELECT count(*) FROM grupos.solicitud_ingreso WHERE grupo_id = ?", grupo);
        UUID alguien = fixtura.usuario();

        cerrado(() -> transaccion.execute(t -> postularCU.postular(entrada(), de(alguien))));
        cerrado(() ->
                transaccion.execute(t -> enlacesDeGrupo.solicitar(codigoDelEnlace, entrada(), null, de(alguien))));

        assertThat(contar("SELECT count(*) FROM grupos.solicitud_ingreso WHERE grupo_id = ?", grupo))
                .isEqualTo(solicitudes);
    }

    @Test
    @DisplayName(
            "CU-69 · iniciado: no se invita (ni se pide el enlace a identidad) y no se genera otro enlace del grupo")
    void invitarYGenerarEnlace() {
        cerrado(() -> transaccion.executeWithoutResult(t -> invitar.comprobarEmisor(grupo, de(organizador))));
        cerrado(() -> transaccion.execute(t -> invitar.invitar(
                new CU69Invitar.EntradaInvitacion(
                        grupo,
                        "+59176100002",
                        "Otro",
                        "ENLACE",
                        false,
                        false,
                        3,
                        fixtura.tokenDeInvitacion(),
                        OffsetDateTime.now().plusDays(7)),
                de(organizador))));
        // Generar el enlace ya no es un ingreso sino compartir cupos: iniciado, no le queda ninguno (AP-CU69-01).
        assertThatThrownBy(() -> transaccion.execute(t -> enlacesDeGrupo.generar(grupo, de(organizador))))
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-01"));
    }

    @Test
    @DisplayName("CU-69 · iniciado: aceptar una solicitud (organizador, propuesta o resolucion) responde AP-CU69-17")
    void decidirAceptar() {
        cerrado(() ->
                transaccion.execute(t -> admision.decidir(sinPropuesta, true, null, BigDecimal.ZERO, de(organizador))));
        cerrado(() -> transaccion.execute(t -> admision.proponer(
                new CU68AceptarIngreso.Entrada(sinPropuesta, UUID.randomUUID(), "ACEPTAR", "Lo conozco.", 0, null),
                de(organizador))));
        UUID backoffice = fixtura.usuario();
        cerrado(() -> transaccion.execute(t -> admision.resolver(
                new CU68AceptarIngreso.Entrada(conPropuesta, UUID.randomUUID(), "ACEPTAR", "Revisado.", 1, propuesta),
                ContextoSesion.de(
                        backoffice, "BACKOFFICE", new Traza(UUID.randomUUID().toString())))));

        assertThat(integrantes()).isEqualTo(3);
        assertThat(contar("SELECT count(*) FROM grupos.cupo WHERE grupo_id = ? AND estado = 'RESERVADO'", grupo))
                .isZero();
    }

    @Test
    @DisplayName("CU-69 · iniciado: rechazar una solicitud pendiente sigue permitido (cerrar no deja colgados)")
    void rechazarSigue() {
        var r = transaccion.execute(
                t -> admision.decidir(sinPropuesta, false, "El grupo ya inicio.", BigDecimal.ZERO, de(organizador)));

        assertThat(r.estado()).isEqualTo("RECHAZADA");
    }
}

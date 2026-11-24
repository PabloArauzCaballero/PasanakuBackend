package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.CU135GobernarRoles;
import bo.aportaya.identidad.aplicacion.CU135GobernarRoles.PedidoDeCreacion;
import bo.aportaya.identidad.aplicacion.CU135GobernarRoles.PedidoDeEdicion;
import bo.aportaya.identidad.aplicacion.CU135GobernarRoles.Resultado;
import bo.aportaya.identidad.infraestructura.RolesPersonalizadosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-135 contra PostgreSQL real (GU-16): crear, editar y retirar roles personalizados, la segregacion de funciones
 * (R-SEG-04), los cuatro ojos y que nada se borra (R-AUD-01: un rol retirado conserva su fila).
 *
 * <p>Doble declarado (regla 65): la evidencia step-up es la de {@link BaseDeGobiernoCU08}; la validacion real del JWT
 * se prueba en {@code EvidenciaDeStepUpJwtTest}.
 */
class CU135GobernarRolesTest extends BaseDeGobiernoCU08 {

    private static CU135GobernarRoles casoDeUso;

    @BeforeAll
    static void armarCU135() {
        casoDeUso = new CU135GobernarRoles(
                new Datos(dsl),
                new RolesPersonalizadosRepositorio(),
                accesos,
                DOBLE_DE_STEP_UP,
                new Outbox("identidad"),
                Reloj.delSistema());
    }

    private static PedidoDeCreacion pedido(String codigo, Set<String> permisos) {
        return new PedidoDeCreacion(
                codigo, "Rol de prueba", "descripcion", permisos, "Nuevo puesto aprobado", EVIDENCIA);
    }

    private static String codigoNuevo() {
        return "GU_F_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private static String permisoBajo() {
        String codigo = "GU_F_P_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        fixtura.permiso(codigo, "tablero", "VER", false);
        return codigo;
    }

    private static UUID rolDeSistema() {
        UUID id = UUID.randomUUID();
        dsl.execute(
                "INSERT INTO identidad.rol (id, codigo, nombre, ambito, es_sistema) VALUES (?, ?, 'sistema', 'GLOBAL',"
                        + " true)",
                id,
                codigoNuevo());
        return id;
    }

    @Test
    @DisplayName(
            "Dado un operador con ROLES_INTERNOS_GESTIONAR y step-up vigente · Cuando crea un rol personalizado con"
                + " permisos de riesgo bajo o medio · Entonces el rol existe como no de sistema, con sus permisos, y"
                + " deja identidad.rol_creado con actor y motivo")
    void creaUnRolPersonalizado() {
        enTransaccionQueSeDeshace(() -> {
            UUID actor = operador();
            String permiso = permisoBajo();
            String codigo = codigoNuevo();

            Resultado r = casoDeUso.crear(pedido(codigo, Set.of(permiso)), como(actor));

            assertThat(r.estado()).isEqualTo("APLICADO");
            assertThat(columna("SELECT es_sistema FROM identidad.rol WHERE id = ?", r.rolId()))
                    .isEqualTo(false);
            assertThat(columna("SELECT creado_por FROM identidad.rol WHERE id = ?", r.rolId()))
                    .isEqualTo(actor);
            assertThat(columna("SELECT count(*)::int FROM identidad.rol_permiso WHERE rol_id = ?", r.rolId()))
                    .isEqualTo(1);
            assertThat(eventos("identidad.rol_creado", r.rolId())).isEqualTo(1);
            assertThat(columna(
                            """
                            SELECT payload->>'actorId' || '|' || (payload->>'origen') || '|' || (payload->>'motivo')
                              FROM identidad.evento_dominio WHERE agregado_id = ?
                            """,
                            r.rolId()))
                    .isEqualTo(actor + "|PANEL_ADMIN|Nuevo puesto aprobado");
        });
    }

    @Test
    @DisplayName("Dado un operador con ROLES_INTERNOS_GESTIONAR y step-up vigente · Cuando crea un rol con"
            + " ENTREGA_AUTORIZAR y ENTREGA_EJECUTAR · Entonces se rechaza con PAR_INCOMPATIBLE y no se escribe"
            + " ningun rol")
    void rechazaElParIncompatible() {
        enTransaccionQueSeDeshace(() -> {
            fixtura.permisoDelCatalogo("ENTREGA_AUTORIZAR", "entrega_fondo", "AUTORIZAR");
            fixtura.permisoDelCatalogo("ENTREGA_EJECUTAR", "entrega_fondo", "EJECUTAR");
            String codigo = codigoNuevo();

            // R-SEG-04: nadie autoriza y ejecuta la misma operacion, tampoco un rol nuevo.
            assertThatThrownBy(() -> casoDeUso.crear(
                            pedido(codigo, Set.of("ENTREGA_AUTORIZAR", "ENTREGA_EJECUTAR")), como(operador())))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU135-05"));

            assertThat(columna("SELECT count(*)::int FROM identidad.rol WHERE codigo = ?", codigo))
                    .isEqualTo(0);
            assertThat(columna("SELECT count(*)::int FROM identidad.solicitud_cambio_rol"))
                    .isEqualTo(0);
        });
    }

    @Test
    @DisplayName(
            "Dado un rol de sistema del catalogo · Cuando se intenta editarlo o retirarlo · Entonces se rechaza con"
                    + " ROL_DE_SISTEMA y el rol queda intacto")
    void rechazaEditarOReitrarUnRolDeSistema() {
        enTransaccionQueSeDeshace(() -> {
            UUID sistema = rolDeSistema();
            UUID actor = operador();

            assertThatThrownBy(() -> casoDeUso.editar(
                            sistema,
                            new PedidoDeEdicion("Otro nombre", null, Set.of(permisoBajo()), "Ajuste", EVIDENCIA),
                            como(actor)))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU135-06"));
            assertThatThrownBy(() -> casoDeUso.retirar(sistema, "Ya no se usa", EVIDENCIA, como(actor)))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU135-06"));

            assertThat(columna("SELECT nombre FROM identidad.rol WHERE id = ?", sistema))
                    .isEqualTo("sistema");
            assertThat(columna("SELECT retirado_en FROM identidad.rol WHERE id = ?", sistema))
                    .isNull();
        });
    }

    @Test
    @DisplayName(
            "Dado un operador que crea un rol con un permiso de riesgo alto · Cuando otra persona aprueba la solicitud"
                    + " · Entonces el rol existe recien al aprobar y el evento nombra a quien propuso")
    void cuatroOjosCreaElRolAlAprobar() {
        enTransaccionQueSeDeshace(() -> {
            fixtura.permisoDelCatalogo("RETIRO_APROBAR", "orden_retiro", "APROBAR");
            UUID proponente = operador();
            UUID aprobador = operador();
            String codigo = codigoNuevo();

            Resultado pendiente = casoDeUso.crear(pedido(codigo, Set.of("RETIRO_APROBAR")), como(proponente));

            assertThat(pendiente.estado()).isEqualTo("PENDIENTE_DE_APROBACION");
            assertThat(pendiente.rolId()).isNull();
            assertThat(columna("SELECT count(*)::int FROM identidad.rol WHERE codigo = ?", codigo))
                    .isEqualTo(0);
            assertThat(eventos("identidad.rol_solicitado", pendiente.solicitudId()))
                    .isEqualTo(1);

            Resultado aprobado =
                    casoDeUso.aprobar(pendiente.solicitudId(), "Revisado y aprobado", EVIDENCIA, como(aprobador));

            assertThat(aprobado.estado()).isEqualTo("APLICADO");
            assertThat(columna("SELECT codigo FROM identidad.rol WHERE id = ?", aprobado.rolId()))
                    .isEqualTo(codigo);
            assertThat(columna(
                            "SELECT payload->>'propuestaPor' FROM identidad.evento_dominio WHERE agregado_id = ?",
                            aprobado.rolId()))
                    .isEqualTo(proponente.toString());
            assertThat(columna(
                            "SELECT estado FROM identidad.solicitud_cambio_rol WHERE id = ?", pendiente.solicitudId()))
                    .isEqualTo("APROBADA");
        });
    }

    @Test
    @DisplayName(
            "Dada una solicitud pendiente · Cuando la persona que la propuso intenta aprobarla · Entonces se rechaza"
                    + " con APROBADOR_ES_EL_PROPONENTE y sigue pendiente")
    void rechazaQueElProponenteResuelva() {
        enTransaccionQueSeDeshace(() -> {
            fixtura.permisoDelCatalogo("RETIRO_APROBAR", "orden_retiro", "APROBAR");
            UUID proponente = operador();
            Resultado pendiente = casoDeUso.crear(pedido(codigoNuevo(), Set.of("RETIRO_APROBAR")), como(proponente));

            assertThatThrownBy(() ->
                            casoDeUso.aprobar(pendiente.solicitudId(), "Me lo apruebo", EVIDENCIA, como(proponente)))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU135-12"));

            assertThat(columna(
                            "SELECT estado FROM identidad.solicitud_cambio_rol WHERE id = ?", pendiente.solicitudId()))
                    .isEqualTo("PENDIENTE");
        });
    }

    @Test
    @DisplayName(
            "Dado un rol con un titular vigente que ya tiene otro rol · Cuando se le agrega un permiso que con el otro"
                    + " rol forma un par incompatible · Entonces se rechaza con ASIGNADO_QUEDARIA_EN_CONFLICTO")
    void rechazaDejarAUnTitularEnConflicto() {
        enTransaccionQueSeDeshace(() -> {
            UUID autorizar = fixtura.permisoDelCatalogo("ENTREGA_AUTORIZAR", "entrega_fondo", "AUTORIZAR");
            fixtura.permisoDelCatalogo("ENTREGA_EJECUTAR", "entrega_fondo", "EJECUTAR");
            UUID actor = operador();
            UUID titular = operador();
            UUID otroRol = rolLlamado(
                    "GU_F_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            fixtura.darPermisoAlRol(otroRol, autorizar);
            Resultado creado = casoDeUso.crear(pedido(codigoNuevo(), Set.of(permisoBajo())), como(actor));
            fixtura.asignarRol(titular, otroRol, operador());
            fixtura.asignarRol(titular, creado.rolId(), operador());

            assertThatThrownBy(() -> casoDeUso.editar(
                            creado.rolId(),
                            new PedidoDeEdicion("Rol de prueba", null, Set.of("ENTREGA_EJECUTAR"), "Ajuste", EVIDENCIA),
                            como(actor)))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU135-13"));

            assertThat(columna("SELECT count(*)::int FROM identidad.rol_permiso WHERE rol_id = ?", creado.rolId()))
                    .isEqualTo(1);
        });
    }

    @Test
    @DisplayName(
            "Dado un rol personalizado con un titular vigente · Cuando se intenta retirarlo · Entonces se rechaza con"
                    + " CON_ASIGNACIONES_VIGENTES y sin titulares queda retirado y no se asigna mas")
    void retiraSoloSinTitulares() {
        enTransaccionQueSeDeshace(() -> {
            UUID actor = operador();
            UUID titular = operador();
            Resultado creado = casoDeUso.crear(pedido(codigoNuevo(), Set.of(permisoBajo())), como(actor));
            fixtura.asignarRol(titular, creado.rolId(), operador());

            assertThatThrownBy(() -> casoDeUso.retirar(creado.rolId(), "Ya no se usa", EVIDENCIA, como(actor)))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU135-10"));

            dsl.execute(
                    "UPDATE identidad.asignacion_rol SET revocada_en = now(), motivo_revocacion = 'prueba' WHERE"
                            + " rol_id = ?",
                    creado.rolId());
            casoDeUso.retirar(creado.rolId(), "Ya no se usa", EVIDENCIA, como(actor));

            // R-AUD-01: la fila no se borra; solo deja de poder asignarse.
            assertThat(columna("SELECT retirado_en IS NOT NULL FROM identidad.rol WHERE id = ?", creado.rolId()))
                    .isEqualTo(true);
            assertThat(eventos("identidad.rol_retirado", creado.rolId())).isEqualTo(1);
            assertThatThrownBy(() -> asignar(operador(), operador(), creado.rolId()))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU08-01"));
        });
    }

    @Test
    @DisplayName("Rechaza sin evidencia de step-up valida, sin escribir nada")
    void rechazaSinStepUp() {
        enTransaccionQueSeDeshace(() -> {
            String codigo = codigoNuevo();
            PedidoDeCreacion sinEvidencia =
                    new PedidoDeCreacion(codigo, "Rol de prueba", null, Set.of(permisoBajo()), "Motivo", "otra-cosa");

            assertThatThrownBy(() -> casoDeUso.crear(sinEvidencia, como(operador())))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU135-09"));

            assertThat(columna("SELECT count(*)::int FROM identidad.rol WHERE codigo = ?", codigo))
                    .isEqualTo(0);
        });
    }

    @Test
    @DisplayName("Rechaza un codigo repetido y uno de identidad de servicio")
    void rechazaCodigosNoValidos() {
        enTransaccionQueSeDeshace(() -> {
            UUID actor = operador();
            String permiso = permisoBajo();
            String codigo = codigoNuevo();
            casoDeUso.crear(pedido(codigo, Set.of(permiso)), como(actor));

            assertThatThrownBy(() -> casoDeUso.crear(pedido(codigo, Set.of(permiso)), como(actor)))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU135-02"));
            assertThatThrownBy(() -> casoDeUso.crear(pedido("SERVICIO_NUEVO", Set.of(permiso)), como(actor)))
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU135-01"));
        });
    }
}

package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.Argon2Hasheador;
import bo.aportaya.identidad.infraestructura.FiltroDeUsuariosInternos;
import bo.aportaya.identidad.infraestructura.RolesYPermisosRepositorio;
import bo.aportaya.identidad.infraestructura.UsuariosInternosRepositorio;
import bo.aportaya.identidad.web.generado.modelo.EntradaAltaUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAsignarRolesUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoDetalle;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoResumen;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Las lecturas de personal interno contra la base real (Testcontainers): bloqueo de {@code bloqueo_cuenta} (GU-10),
 * segundo factor confirmado y listado paginado (GU-17), y roles que no se otorgan a personal interno (GU-13).
 * Todos los datos son sinteticos: correos {@code @ejemplo.test} y telefonos al azar.
 */
class UsuariosInternosLecturaRepositorioTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-24T12:00:00Z");

    private static DSLContext dsl;
    private static TransactionTemplate tx;
    private static FixturaDeIdentidad fixtura;
    private static UsuariosInternosRepositorio repositorio;

    @BeforeAll
    static void preparar() {
        var pg = BaseDePrueba.contenedor();
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        fixtura = new FixturaDeIdentidad(dsl);
        var accesos = new AccesosRepositorio();
        repositorio = new UsuariosInternosRepositorio(
                accesos, new Argon2Hasheador("pimienta-de-prueba"), new RolesYPermisosRepositorio(accesos));
    }

    @Test
    @DisplayName("GU-10 · el bloqueo que se ve es el vigente de bloqueo_cuenta; liberado o vencido no se ve")
    void elBloqueoSaleDeBloqueoCuenta() {
        String rol = rolNuevo();
        UUID temporal = alta(rol).getUsuarioId();
        UUID indefinido = alta(rol).getUsuarioId();
        UUID liberado = alta(rol).getUsuarioId();
        UUID vencido = alta(rol).getUsuarioId();
        bloquear(temporal, "INTENTOS_FALLIDOS", AHORA.plusHours(1), null);
        bloquear(indefinido, "FRAUDE", null, null);
        bloquear(liberado, "INTENTOS_FALLIDOS", AHORA.plusHours(1), AHORA.minusMinutes(5));
        bloquear(vencido, "INTENTOS_FALLIDOS", AHORA.minusMinutes(1), null);

        UsuarioInternoDetalle fichaTemporal = consultar(temporal);
        assertThat(fichaTemporal.getBloqueadoHasta()).isEqualTo(AHORA.plusHours(1));
        assertThat(fichaTemporal.getMotivoBloqueo()).isEqualTo("INTENTOS_FALLIDOS");

        UsuarioInternoDetalle fichaIndefinida = consultar(indefinido);
        assertThat(fichaIndefinida.getBloqueadoHasta()).isNull();
        assertThat(fichaIndefinida.getMotivoBloqueo()).isEqualTo("FRAUDE");

        for (UUID sinBloqueo : List.of(liberado, vencido)) {
            assertThat(consultar(sinBloqueo).getBloqueadoHasta()).isNull();
            assertThat(consultar(sinBloqueo).getMotivoBloqueo()).isNull();
        }

        var lista = tx.execute(s -> repositorio.listar(dsl, filtro(rol, 10, null, null), AHORA));
        assertThat(lista)
                .filteredOn(u -> u.getUsuarioId().equals(temporal))
                .singleElement()
                .satisfies(u -> assertThat(u.getBloqueadoHasta()).isEqualTo(AHORA.plusHours(1)));
    }

    @Test
    @DisplayName("GU-17 · un factor sin confirmar o inactivo no cuenta como segundo factor; uno activo y confirmado si")
    void elSegundoFactorEsElConfirmado() {
        String rol = rolNuevo();
        UUID sinConfirmar = alta(rol).getUsuarioId();
        UUID inactivo = alta(rol).getUsuarioId();
        UUID enrolado = alta(rol).getUsuarioId();
        fixtura.factor(sinConfirmar, "TOTP", true, false);
        fixtura.factor(inactivo, "TOTP", false, true);
        fixtura.factor(enrolado, "TOTP", true, true);

        assertThat(consultar(sinConfirmar).getTieneSegundoFactor()).isFalse();
        assertThat(consultar(inactivo).getTieneSegundoFactor()).isFalse();
        assertThat(consultar(enrolado).getTieneSegundoFactor()).isTrue();
    }

    @Test
    @DisplayName("GU-17 · el listado pagina por (creadoEn, id) descendente: dos paginas sin repetir ni saltear")
    void elListadoPagina() {
        String rol = rolNuevo();
        // Las tres altas comparten fecha de registro (AHORA): el orden lo desempata el id, que es lo que se prueba.
        List<UUID> perfiles =
                List.of(alta(rol).getId(), alta(rol).getId(), alta(rol).getId());
        alta(rolNuevo()); // de otro rol: el filtro por rol la deja afuera aunque tenga la misma fecha

        List<UsuarioInternoResumen> primera =
                tx.execute(s -> repositorio.listar(dsl, filtro(rol, 2, null, null), AHORA));
        UsuarioInternoResumen ultima = primera.get(primera.size() - 1);
        List<UsuarioInternoResumen> segunda =
                tx.execute(s -> repositorio.listar(dsl, filtro(rol, 2, ultima.getCreadoEn(), ultima.getId()), AHORA));

        assertThat(primera).hasSize(2);
        assertThat(segunda).hasSize(1);
        List<UUID> recorridos = new ArrayList<>();
        primera.forEach(u -> recorridos.add(u.getId()));
        segunda.forEach(u -> recorridos.add(u.getId()));
        assertThat(recorridos)
                .containsExactlyElementsOf(perfiles.stream()
                        // El orden de uuid en PostgreSQL es el de su texto hexadecimal, no el de UUID.compareTo.
                        .sorted(Comparator.comparing(UUID::toString).reversed())
                        .toList());
        assertThat(primera).allSatisfy(u -> assertThat(u.getCreadoEn()).isEqualTo(AHORA));
    }

    @Test
    @DisplayName(
            "GU-13 · asignar a un operador un rol de servicio o de grupo se rechaza con AP-CU08-08 y no cambia nada")
    void losRolesAjenosNoSeAsignan() {
        String propio = rolNuevo();
        UUID operador = alta(propio).getUsuarioId();
        UUID administrador = fixtura.usuario(telefonoUnico());
        String deServicio = "SERVICIO_" + sufijo().toUpperCase(Locale.ROOT);
        fixtura.rolGlobal(deServicio);
        String deGrupo = "GRUPO_" + sufijo().toUpperCase(Locale.ROOT);
        dsl.execute(
                "INSERT INTO identidad.rol (id, codigo, nombre, ambito, es_sistema) VALUES (?, ?, ?, 'GRUPO', false)",
                UUID.randomUUID(),
                deGrupo,
                deGrupo);

        for (String ajeno : List.of(deServicio, deGrupo)) {
            var entrada = new EntradaAsignarRolesUsuarioInterno(List.of(propio, ajeno), "motivo de prueba sintetico");
            assertThatThrownBy(() ->
                            tx.execute(s -> repositorio.asignarRoles(dsl, operador, entrada, administrador, AHORA)))
                    .isInstanceOfSatisfying(ErrorDeNegocio.class, e -> {
                        assertThat(e.codigo().valor()).isEqualTo("AP-CU08-08");
                        assertThat(e.detalle()).containsEntry("roles", List.of(ajeno));
                    });
        }
        assertThat(consultar(operador).getRoles()).containsExactly(propio);
    }

    private static FiltroDeUsuariosInternos filtro(String rol, int limite, OffsetDateTime antesDe, UUID antesDeId) {
        return new FiltroDeUsuariosInternos(null, rol, null, limite, antesDe, antesDeId);
    }

    private static UsuarioInternoDetalle consultar(UUID usuarioId) {
        return tx.execute(s -> repositorio.consultar(dsl, usuarioId, AHORA)).orElseThrow();
    }

    private static void bloquear(UUID usuarioId, String motivo, OffsetDateTime hasta, OffsetDateTime liberadaEn) {
        dsl.execute(
                """
                INSERT INTO identidad.bloqueo_cuenta (id, usuario_id, motivo, bloqueada_en, desbloquea_en, liberada_en)
                VALUES (gen_random_uuid(), ?, ?, ?::timestamptz, ?::timestamptz, ?::timestamptz)
                """,
                usuarioId,
                motivo,
                AHORA.minusHours(2).toString(),
                hasta == null ? null : hasta.toString(),
                liberadaEn == null ? null : liberadaEn.toString());
    }

    private static String rolNuevo() {
        String rol = "ROL_" + sufijo().toUpperCase(Locale.ROOT);
        fixtura.rolGlobal(rol);
        return rol;
    }

    private static UsuarioInternoDetalle alta(String rol) {
        UUID otorgante = fixtura.otorgante();
        String sufijo = sufijo();
        var entrada = new EntradaAltaUsuarioInterno(
                "Prueba",
                "Lectura",
                "lectura." + sufijo + "@ejemplo.test",
                telefonoUnico(),
                "Sistemas",
                "Operador de pruebas",
                rol);
        return tx.execute(s -> repositorio.crear(dsl, entrada, otorgante, AHORA));
    }

    private static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** E.164 boliviano de ocho digitos, distinto en cada llamada: la base de pruebas es compartida. */
    private static String telefonoUnico() {
        return "+5917%07d".formatted(Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000L));
    }
}

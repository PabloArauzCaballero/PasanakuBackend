package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.ActivarRolOrganizador;
import bo.aportaya.identidad.infraestructura.FirmaDeOrganizadorRepositorio;
import bo.aportaya.identidad.infraestructura.clientes.OrganizadorPorHttp;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * CU-90 vía básica, lado identidad: firma del contrato de organizador y rol ORGANIZADOR, contra
 * PostgreSQL real y un doble de {@code organizador} (correcto, límite e inválido).
 */
class CU90RolOrganizadorTest extends BaseDeCU04 {

    private static final String HASH = "a".repeat(64);

    /** Doble de organizador: registra con qué token se le pidió firmar y responde lo configurado. */
    static final class OrganizadorDoble extends OrganizadorPorHttp {
        final List<UUID> tokensRecibidos = new ArrayList<>();
        boolean habilitado = true;

        OrganizadorDoble() {
            super(RestClient.builder(), "http://no-se-usa");
        }

        @Override
        public boolean firmar(UUID contratoId, UUID tokenFirmaId) {
            tokensRecibidos.add(tokenFirmaId);
            return true;
        }

        @Override
        public boolean habilitadoConContrato() {
            return habilitado;
        }
    }

    private static FirmaDeOrganizadorRepositorio firmas;

    @BeforeAll
    static void armarFirmas() {
        firmas = new FirmaDeOrganizadorRepositorio(new Datos(dsl), transaccion);
        dsl.execute(
                """
                INSERT INTO identidad.politica_token
                    (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora,
                     cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos,
                     exige_dispositivo_conocido, invalida_anteriores, vigente_desde)
                SELECT 'FIRMA_CONTRATO', 900, 32, 1, 5, 0, 10, 'APP', false, false, now() - interval '1 day'
                 WHERE NOT EXISTS (SELECT 1 FROM identidad.politica_token WHERE proposito = 'FIRMA_CONTRATO')
                """);
    }

    private ActivarRolOrganizador caso(OrganizadorDoble organizador) {
        return new ActivarRolOrganizador(firmas, organizador, Reloj.delSistema());
    }

    private UUID rol(String codigo) {
        dsl.execute(
                """
                INSERT INTO identidad.rol (id, codigo, nombre, ambito, es_sistema)
                SELECT gen_random_uuid(), ?, ?, 'GLOBAL', true
                 WHERE NOT EXISTS (SELECT 1 FROM identidad.rol WHERE codigo = ?)
                """,
                codigo,
                codigo,
                codigo);
        return dsl.fetchOne("SELECT id FROM identidad.rol WHERE codigo = ?", codigo)
                .get(0, UUID.class);
    }

    private UUID usuarioNuevo() {
        return fixtura.usuario("+59176" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
    }

    private ContextoSesion ctx(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    private int contar(String sql, Object... p) {
        return ((Number) dsl.fetchOne(sql, p).get(0)).intValue();
    }

    @Test
    @DisplayName("correcto: con KYC aprobado y contrato firmado queda ORGANIZADOR, otorgado por quien aprobó su KYC")
    void concedeElRol() {
        UUID usuario = usuarioNuevo();
        UUID revisor = fixtura.otorgante();
        fixtura.asignarRol(usuario, rol("PARTICIPANTE"), revisor);
        rol("ORGANIZADOR");
        var organizador = new OrganizadorDoble();
        UUID contrato = UUID.randomUUID();

        boolean nuevo =
                caso(organizador).ejecutar(usuario, contrato, HASH, "198.51.100.7", "pruebas/1.0", ctx(usuario));

        assertThat(nuevo).isTrue();
        assertThat(contar(
                        "SELECT count(*) FROM identidad.asignacion_rol a JOIN identidad.rol r ON r.id=a.rol_id"
                                + " WHERE a.usuario_id=? AND r.codigo='ORGANIZADOR' AND a.otorgada_por=?",
                        usuario,
                        revisor))
                .isEqualTo(1);
        assertThat(organizador.tokensRecibidos).hasSize(1);
        assertThat(contar(
                        "SELECT count(*) FROM identidad.token_verificacion WHERE id=? AND usuario_id=?"
                                + " AND proposito='FIRMA_CONTRATO' AND estado='CONSUMIDO'",
                        organizador.tokensRecibidos.get(0),
                        usuario))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("límite: repetir no duplica el rol ni el token de firma")
    void idempotente() {
        UUID usuario = usuarioNuevo();
        fixtura.asignarRol(usuario, rol("PARTICIPANTE"));
        rol("ORGANIZADOR");
        var organizador = new OrganizadorDoble();
        UUID contrato = UUID.randomUUID();
        var cu = caso(organizador);

        cu.ejecutar(usuario, contrato, HASH, "198.51.100.7", "pruebas/1.0", ctx(usuario));
        boolean otraVez = cu.ejecutar(usuario, contrato, HASH, "198.51.100.7", "pruebas/1.0", ctx(usuario));

        assertThat(otraVez).isFalse();
        assertThat(organizador.tokensRecibidos).hasSize(2).allMatch(t -> t.equals(organizador.tokensRecibidos.get(0)));
        assertThat(contar(
                        "SELECT count(*) FROM identidad.asignacion_rol a JOIN identidad.rol r ON r.id=a.rol_id"
                                + " WHERE a.usuario_id=? AND r.codigo='ORGANIZADOR'",
                        usuario))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("inválido: sin KYC aprobado no hay firma ni rol (AP-CU90-14)")
    void sinKyc() {
        UUID usuario = usuarioNuevo();
        var organizador = new OrganizadorDoble();

        assertThatThrownBy(() -> caso(organizador)
                        .ejecutar(usuario, UUID.randomUUID(), HASH, "198.51.100.7", "pruebas/1.0", ctx(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("en verificación");
        assertThat(organizador.tokensRecibidos).isEmpty();
        assertThat(contar("SELECT count(*) FROM identidad.token_verificacion WHERE usuario_id=?", usuario))
                .isZero();
    }

    @Test
    @DisplayName("inválido: si organizador no confirma la habilitación, no se concede el rol (AP-CU90-13)")
    void organizadorNoConfirma() {
        UUID usuario = usuarioNuevo();
        fixtura.asignarRol(usuario, rol("PARTICIPANTE"));
        rol("ORGANIZADOR");
        var organizador = new OrganizadorDoble();
        organizador.habilitado = false;

        assertThatThrownBy(() -> caso(organizador)
                        .ejecutar(usuario, UUID.randomUUID(), HASH, "198.51.100.7", "pruebas/1.0", ctx(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("No pudimos firmar");
        assertThat(contar(
                        "SELECT count(*) FROM identidad.asignacion_rol a JOIN identidad.rol r ON r.id=a.rol_id"
                                + " WHERE a.usuario_id=? AND r.codigo='ORGANIZADOR'",
                        usuario))
                .isZero();
    }

    @Test
    @DisplayName("inválido: nadie activa el rol de otra persona (AP-CU90-12)")
    void otraPersona() {
        UUID usuario = usuarioNuevo();
        UUID otro = usuarioNuevo();

        assertThatThrownBy(() -> caso(new OrganizadorDoble())
                        .ejecutar(otro, UUID.randomUUID(), HASH, "198.51.100.7", "pruebas/1.0", ctx(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("tu propio contrato");
    }
}

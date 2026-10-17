package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.IndicadoresDeIdentidad;
import bo.aportaya.identidad.infraestructura.IndicadoresDeIdentidadRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tableros · H5.S1.M1 — usuarios nuevos y KYC aprobados por dia. Se mide el DELTA de cada prueba. */
class CUIndicadoresDeIdentidadTest extends BaseDeCU08 {

    private static final AtomicLong TELEFONO = new AtomicLong(System.nanoTime() % 1_000_000);
    private final IndicadoresDeIdentidadRepositorio repositorio = new IndicadoresDeIdentidadRepositorio();

    private static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/La_Paz"));
    }

    private static ContextoSesion sistema() {
        return ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
    }

    private <T> T leer(java.util.function.Function<org.jooq.DSLContext, T> consulta) {
        return transaccion.execute(t -> new Datos(dsl).conContexto(sistema(), consulta));
    }

    private UUID usuarioNuevo() {
        return fixtura.usuario("+59177" + String.format("%06d", TELEFONO.incrementAndGet() % 1_000_000));
    }

    private void kyc(UUID usuario, String estado, String resueltaEn) {
        dsl.execute(
                """
                INSERT INTO identidad.verificacion_kyc (id, usuario_id, nivel_solicitado, estado, iniciada_en, resuelta_en)
                VALUES (gen_random_uuid(), ?, 'BASICO', ?, now(), CAST(? AS timestamptz))
                """,
                usuario,
                estado,
                resueltaEn);
    }

    @Test
    @DisplayName("cuadre: registrar una persona hoy suma uno a los usuarios nuevos del día")
    void nuevo() {
        long antes = leer(d -> repositorio.registrados(d, hoy(), hoy()));

        usuarioNuevo();

        assertThat(leer(d -> repositorio.registrados(d, hoy(), hoy())) - antes).isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: un KYC aprobado se cuenta el día en que se resolvió, no el día en que empezó")
    void kycPorResolucion() {
        LocalDate d = LocalDate.of(2160, 3, 3);
        long antes = leer(x -> repositorio.kycAprobados(x, d, d));

        kyc(usuarioNuevo(), "APROBADA", d + " 10:00:00-04");

        assertThat(leer(x -> repositorio.kycAprobados(x, d, d)) - antes).isEqualTo(1);
    }

    @Test
    @DisplayName("límite: un período sin altas no tiene valor (SIN_DATOS)")
    void sinDatos() {
        var c = leer(d -> new IndicadoresDeIdentidad(repositorio).calcular(d, PeriodoDeTablero.de("1980-01-01")));

        assertThat(c)
                .extracting(x -> x.indicador().codigo())
                .containsExactly("USUARIOS_NUEVOS", "EMBUDO_KYC_APROBADO", "USUARIOS_ACTIVOS_HOY");
        assertThat(c).allSatisfy(x -> assertThat(x.indicador().valor()).isNull());
    }

    @Test
    @DisplayName("rechaza contar como aprobado un KYC RECHAZADO")
    void rechazadoNoCuenta() {
        LocalDate d = LocalDate.of(2160, 4, 4);
        long antes = leer(x -> repositorio.kycAprobados(x, d, d));

        kyc(usuarioNuevo(), "RECHAZADA", d + " 10:00:00-04");

        long despues = leer(x -> repositorio.kycAprobados(x, d, d));
        assertThat(despues).isEqualTo(antes);
    }

    private void sesion(UUID usuario, String inicio, String ultima) {
        UUID dispositivo = fixtura.dispositivoConfiable(usuario, "huella-" + UUID.randomUUID());
        dsl.execute(
                """
                INSERT INTO identidad.sesion
                    (id, usuario_id, dispositivo_id, iniciada_en, ultima_actividad_en, expira_en, ip_origen)
                VALUES (gen_random_uuid(), ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz),
                        CAST(? AS timestamptz) + interval '1 day', '10.0.0.1')
                """,
                usuario,
                dispositivo,
                inicio,
                ultima,
                ultima);
    }

    @Test
    @DisplayName("cuadre: una persona con dos sesiones el mismo día cuenta UNA vez; otra que la usó ese día, otra más")
    void activosDistintos() {
        LocalDate d = LocalDate.of(2161, 5, 5);
        long antes = leer(x -> repositorio.activos(x, d, d));
        UUID una = usuarioNuevo();
        sesion(una, d + " 09:00:00-04", d + " 09:30:00-04");
        sesion(una, d + " 18:00:00-04", d + " 18:10:00-04");
        // Empezó el día anterior y siguió usándola hoy: también estuvo activa.
        sesion(usuarioNuevo(), d.minusDays(1) + " 23:00:00-04", d + " 00:20:00-04");

        long despues = leer(x -> repositorio.activos(x, d, d));
        assertThat(despues - antes).isEqualTo(2);
    }

    @Test
    @DisplayName("rechaza contar a quien no usó su sesión ese día (terminó antes o empezó después)")
    void fueraDelDia() {
        LocalDate d = LocalDate.of(2161, 6, 6);
        long antes = leer(x -> repositorio.activos(x, d, d));
        sesion(usuarioNuevo(), d.minusDays(2) + " 10:00:00-04", d.minusDays(1) + " 23:59:00-04");
        sesion(usuarioNuevo(), d.plusDays(1) + " 00:01:00-04", d.plusDays(1) + " 01:00:00-04");

        long despues = leer(x -> repositorio.activos(x, d, d));
        assertThat(despues).isEqualTo(antes);
    }
}

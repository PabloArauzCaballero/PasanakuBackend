package bo.aportaya.tarifas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.tarifas.dominio.ParametroDelCatalogo;
import bo.aportaya.tarifas.dominio.PropuestaDeParametro.Catalogo;
import bo.aportaya.tarifas.dominio.VersionDelParametro;
import bo.aportaya.tarifas.infraestructura.ParametroRepositorio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-132 con el repositorio doblado: las ramas del caso de uso (propuesta, reintento, cuatro ojos, carrera). La
 * persistencia real (CHECK, triggers, UNIQUE) se prueba contra la base, no acá. Datos sintéticos.
 */
class RegistroDeParametrosTest {

    private static final Instant AHORA = Instant.parse("2026-11-21T12:00:00Z");
    private static final OffsetDateTime VIGENTE = OffsetDateTime.parse("2026-12-01T00:00:00Z");
    private static final UUID PARAMETRO = UUID.fromString("c1320000-0000-4000-8000-000000000001");
    private static final UUID VERSION = UUID.fromString("c1320000-0000-4000-8000-000000000002");
    private static final UUID CLAVE = UUID.fromString("c1320000-0000-4000-8000-0000000000ff");
    private static final UUID PROPONE = UUID.fromString("c1320000-0000-4000-8000-00000000000a");
    private static final UUID APRUEBA = UUID.fromString("c1320000-0000-4000-8000-00000000000b");

    private ParametroRepositorio repo;
    private CU132RegistroDeParametros cu132;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void armar() {
        repo = mock(ParametroRepositorio.class);
        var datos = mock(Datos.class);
        when(datos.conContexto(any(), any()))
                .thenAnswer(i -> ((Function<Object, Object>) i.getArgument(1)).apply(null));
        cu132 = new CU132RegistroDeParametros(datos, repo, Reloj.fijo(AHORA));
        when(repo.porCodigo(any(), eq("DIAS_DE_GRACIA")))
                .thenReturn(Optional.of(new ParametroDelCatalogo(
                        PARAMETRO, "Días de gracia", new Catalogo("DIAS_DE_GRACIA", Optional.of("DIAS"), false))));
    }

    private static ContextoSesion de(UUID usuario) {
        return ContextoSesion.de(
                usuario, "ADMIN_PLATAFORMA", new Traza(UUID.randomUUID().toString()));
    }

    private static CU132RegistroDeParametros.Propuesta propuesta(String valor) {
        return new CU132RegistroDeParametros.Propuesta(
                Optional.empty(),
                new BigDecimal(valor),
                Optional.empty(),
                VIGENTE,
                "Acta sintética",
                "REF-PRUEBA-1",
                LocalDate.parse("2026-11-20"));
    }

    private static VersionDelParametro version(String valor, String estado, Optional<UUID> aprobadoPor) {
        return new VersionDelParametro(
                VERSION,
                PARAMETRO,
                "DIAS_DE_GRACIA",
                Optional.empty(),
                new BigDecimal(valor),
                Optional.empty(),
                VIGENTE,
                PROPONE,
                aprobadoPor,
                estado);
    }

    @Test
    @DisplayName("Correcto · propone y queda PROPUESTA con quien propone tomado del contexto")
    void propone() {
        when(repo.proponer(any(), any())).thenReturn(Optional.of(VERSION));
        when(repo.version(any(), eq(VERSION))).thenReturn(Optional.of(version("5", "PROPUESTA", Optional.empty())));

        var v = cu132.proponer("DIAS_DE_GRACIA", propuesta("5"), CLAVE, de(PROPONE));

        assertThat(v.estado()).isEqualTo("PROPUESTA");
        var nueva = org.mockito.ArgumentCaptor.forClass(ParametroRepositorio.Nueva.class);
        verify(repo).proponer(any(), nueva.capture());
        assertThat(nueva.getValue().propuestoPor()).isEqualTo(PROPONE);
        assertThat(nueva.getValue().claveIdempotencia()).isEqualTo(CLAVE);
    }

    @Test
    @DisplayName("Límite · la misma clave con el mismo contenido devuelve la misma versión, sin otra fila")
    void reintentoDevuelveLaMisma() {
        when(repo.proponer(any(), any())).thenReturn(Optional.empty());
        when(repo.porClave(any(), eq(CLAVE))).thenReturn(Optional.of(version("5", "PROPUESTA", Optional.empty())));

        assertThat(cu132.proponer("DIAS_DE_GRACIA", propuesta("5.00"), CLAVE, de(PROPONE))
                        .id())
                .isEqualTo(VERSION);
    }

    @Test
    @DisplayName("Inválido · la misma clave con otro valor es 422 AP-CU132-05")
    void claveReutilizada() {
        when(repo.proponer(any(), any())).thenReturn(Optional.empty());
        when(repo.porClave(any(), eq(CLAVE))).thenReturn(Optional.of(version("5", "PROPUESTA", Optional.empty())));

        assertThatThrownBy(() -> cu132.proponer("DIAS_DE_GRACIA", propuesta("6"), CLAVE, de(PROPONE)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("clave");
    }

    @Test
    @DisplayName("Inválido · código fuera del catálogo y valor incoherente no llegan a escribir")
    void inexistenteEIncoherente() {
        assertThatThrownBy(() -> cu132.proponer("NO_EXISTE", propuesta("1"), CLAVE, de(PROPONE)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> cu132.proponer("DIAS_DE_GRACIA", propuesta("-1"), CLAVE, de(PROPONE)))
                .isInstanceOf(ErrorDeNegocio.class);
        verify(repo, never()).proponer(any(), any());
    }

    @Test
    @DisplayName("Correcto · otra persona aprueba")
    void otraPersonaAprueba() {
        when(repo.version(any(), eq(VERSION)))
                .thenReturn(Optional.of(version("5", "PROPUESTA", Optional.empty())))
                .thenReturn(Optional.of(version("5", "APROBADA", Optional.of(APRUEBA))));
        when(repo.aprobar(any(), eq(VERSION), eq(APRUEBA), any())).thenReturn(true);

        assertThat(cu132.aprobar(VERSION, de(APRUEBA)).estado()).isEqualTo("APROBADA");
    }

    @Test
    @DisplayName("Inválido · quien propuso no aprueba lo suyo (AP-CU132-03), ni un proceso del sistema")
    void mismaPersonaNoAprueba() {
        when(repo.version(any(), eq(VERSION))).thenReturn(Optional.of(version("5", "PROPUESTA", Optional.empty())));

        assertThatThrownBy(() -> cu132.aprobar(VERSION, de(PROPONE))).isInstanceOf(ErrorDeNegocio.class);
        var sistema = ContextoSesion.deSistema(
                UUID.randomUUID(), new Traza(UUID.randomUUID().toString()));
        assertThatThrownBy(() -> cu132.aprobar(VERSION, sistema)).isInstanceOf(ErrorDeNegocio.class);
        verify(repo, never()).aprobar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Límite · repetir la aprobación con el mismo aprobador devuelve la misma versión, sin reescribir")
    void reintentoDeAprobacion() {
        when(repo.version(any(), eq(VERSION))).thenReturn(Optional.of(version("5", "APROBADA", Optional.of(APRUEBA))));

        assertThat(cu132.aprobar(VERSION, de(APRUEBA)).estado()).isEqualTo("APROBADA");
        verify(repo, never()).aprobar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Inválido · ya aprobada por otro, o la carrera perdida (0 filas), es AP-CU132-04")
    void yaResuelta() {
        UUID tercero = UUID.randomUUID();
        when(repo.version(any(), eq(VERSION))).thenReturn(Optional.of(version("5", "APROBADA", Optional.of(tercero))));
        assertThatThrownBy(() -> cu132.aprobar(VERSION, de(APRUEBA))).isInstanceOf(ErrorDeNegocio.class);

        when(repo.version(any(), eq(VERSION))).thenReturn(Optional.of(version("5", "PROPUESTA", Optional.empty())));
        when(repo.aprobar(any(), eq(VERSION), eq(APRUEBA), any())).thenReturn(false);
        assertThatThrownBy(() -> cu132.aprobar(VERSION, de(APRUEBA)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("primero");
    }

    @Test
    @DisplayName("Catálogo · vigente es la aprobada más reciente ya en vigor; la futura y la propuesta no lo son")
    void catalogo() {
        var params = List.of(new ParametroDelCatalogo(
                PARAMETRO, "Días de gracia", new Catalogo("DIAS_DE_GRACIA", Optional.of("DIAS"), false)));
        when(repo.catalogo(any())).thenReturn(params);
        OffsetDateTime pasada = AHORA.atOffset(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime futura = AHORA.atOffset(ZoneOffset.UTC).plusDays(1);
        var enVigor = conFecha(UUID.randomUUID(), "APROBADA", pasada);
        var futuraAprobada = conFecha(UUID.randomUUID(), "APROBADA", futura);
        var propuesta = conFecha(UUID.randomUUID(), "PROPUESTA", futura);
        when(repo.vivas(any())).thenReturn(List.of(futuraAprobada, propuesta, enVigor));

        var e = cu132.catalogo(de(APRUEBA)).getFirst();

        assertThat(e.vigentes()).containsExactly(enVigor);
        assertThat(e.propuestas()).containsExactly(propuesta);
    }

    private static VersionDelParametro conFecha(UUID id, String estado, OffsetDateTime desde) {
        return new VersionDelParametro(
                id,
                PARAMETRO,
                "DIAS_DE_GRACIA",
                Optional.empty(),
                BigDecimal.ONE,
                Optional.empty(),
                desde,
                PROPONE,
                Optional.empty(),
                estado);
    }
}

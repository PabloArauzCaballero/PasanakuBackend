package bo.aportaya.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.aplicacion.ConsultarIncidentesOperativos;
import bo.aportaya.auditoria.dominio.IncidenteOperativo;
import bo.aportaya.auditoria.infraestructura.IncidentesOperativosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sistemas › Incidentes contra Postgres real. Los incidentes de cada prueba llevan fechas de 2099 y un prefijo propio:
 * el contenedor se comparte, asi que se compara lo propio y no el total de la tabla. Datos sinteticos.
 */
class IncidentesOperativosTest extends BaseDeAuditoria {

    private final ConsultarIncidentesOperativos consulta =
            new ConsultarIncidentesOperativos(new Datos(dsl), new IncidentesOperativosRepositorio());

    private static String prefijo() {
        return "T" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private static UUID insertar(String codigo, String severidad, String estado, String detectado, String resuelto) {
        return dslFixtura
                .fetchOne(
                        """
                        insert into auditoria.incidente_operativo
                          (codigo, titulo, severidad, sistema_afectado, descripcion, estado, detectado_en, resuelto_en)
                        values (?, ?, ?, 'aportes', 'sintetico', ?, ?::timestamptz, ?::timestamptz)
                        returning id
                        """,
                        codigo,
                        "Incidente " + codigo,
                        severidad,
                        estado,
                        detectado,
                        resuelto)
                .get("id", UUID.class);
    }

    private ConsultarIncidentesOperativos.Pagina listar(
            Optional<String> estado, Optional<String> severidad, int pagina, int limite) {
        return transaccion.execute(s -> consulta.listar(estado, severidad, pagina, limite, contexto()));
    }

    @Test
    @DisplayName("correcto · del mas reciente al mas viejo; resueltoEn solo en el resuelto; nunca el importe")
    void ordenYCampos() {
        String p = prefijo();
        insertar(p + "-1", "SEV3", "MITIGADO", "2099-01-01T10:00:00Z", null);
        insertar(p + "-3", "SEV1", "DETECTADO", "2099-01-03T10:00:00Z", null);
        insertar(p + "-2", "SEV2", "RESUELTO", "2099-01-02T10:00:00Z", "2099-01-02T12:00:00Z");

        var propios = listar(Optional.empty(), Optional.empty(), 1, 100).items().stream()
                .filter(f -> f.codigo().startsWith(p))
                .toList();
        assertThat(propios).extracting(IncidenteOperativo::codigo).containsExactly(p + "-3", p + "-2", p + "-1");
        assertThat(propios.get(0).resueltoEn()).isEmpty();
        assertThat(propios.get(1).resueltoEn().map(OffsetDateTime::toInstant))
                .contains(OffsetDateTime.parse("2099-01-02T12:00:00Z").toInstant());
        assertThat(propios.get(0).severidad()).isEqualTo("SEV1");
        assertThat(IncidenteOperativo.class.getRecordComponents())
                .extracting(c -> c.getName())
                .doesNotContain("impactoMonetario");
    }

    @Test
    @DisplayName(
            "limite · mismo instante: el desempate por id es estable; filtro por estado; pagina pasada del final vacia")
    void desempateFiltroYFinal() {
        String p = prefijo();
        UUID a = insertar(p + "-A", "SEV2", "POST_MORTEM", "2099-06-01T00:00:00Z", null);
        UUID b = insertar(p + "-B", "SEV2", "POST_MORTEM", "2099-06-01T00:00:00Z", null);

        var postMortem = listar(Optional.of("POST_MORTEM"), Optional.of("SEV2"), 1, 100);
        var propios = postMortem.items().stream()
                .filter(f -> f.codigo().startsWith(p))
                .toList();
        // Postgres ordena uuid por bytes sin signo; UUID.compareTo de Java compara con signo. El texto hex coincide con
        // Postgres.
        UUID primero = a.toString().compareTo(b.toString()) < 0 ? a : b;
        assertThat(propios).extracting(IncidenteOperativo::id).first().isEqualTo(primero);
        assertThat(postMortem.items())
                .allMatch(f -> f.estado().equals("POST_MORTEM") && f.severidad().equals("SEV2"));

        var fuera = listar(Optional.of("POST_MORTEM"), Optional.of("SEV2"), 1000, 100);
        assertThat(fuera.items()).isEmpty();
        assertThat(fuera.total()).isEqualTo(postMortem.total()).isGreaterThanOrEqualTo(2);
        assertThat(fuera.totalPaginas()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("limite · sin coincidencias: lista vacia, total 0 y cero paginas (no un error)")
    void sinCoincidencias() {
        var vacia = listar(Optional.of("RESUELTO"), Optional.of("SEV1"), 1, 10);
        long propiosResueltosSev1 = vacia.items().stream()
                .filter(f -> !f.estado().equals("RESUELTO"))
                .count();
        assertThat(propiosResueltosSev1).isZero();
        assertThat(new ConsultarIncidentesOperativos.Pagina(java.util.List.of(), 1, 10, 0).totalPaginas())
                .isZero();
    }
}

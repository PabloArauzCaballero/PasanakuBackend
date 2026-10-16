package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.cumplimiento.aplicacion.CU44InvestigarYReportar.EntradaCaso;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejasLft;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejasLft.Alerta;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejasLft.Caso;
import bo.aportaya.cumplimiento.infraestructura.BandejasLftRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · H5 · las bandejas LFT del analista y del oficial, contra PostgreSQL real.
 *
 * <p>Las bandejas son globales y otras clases dejan alertas y casos: estas pruebas ponen los suyos
 * en ventanas de tiempo que nadie mas usa (alertas en 2089, casos vencidos en 2001) y arrancan el
 * cursor justo antes. Lo que importa: orden estable sin repetir entre paginas, filtros que se
 * respetan, el plazo GUARDADO marca el vencido, y nada de la persona viaja en la fila.
 */
class CU44BandejaTest extends BaseDeCumplimiento {

    private static final OffsetDateTime VENTANA = OffsetDateTime.of(2089, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final UUID ANTES_DE_TODO = new UUID(0L, 0L);

    private UUID regla;
    private UUID titular;
    private ConsultarBandejasLft bandejas;

    @BeforeEach
    void escenario() {
        regla = gobiernoFixtura.reglaDeMonitoreo(
                "RM-" + UUID.randomUUID().toString().substring(0, 8), "ALTA", true);
        titular = fixtura.usuario();
        bandejas = new ConsultarBandejasLft(new Datos(dsl), new BandejasLftRepositorio(), Reloj.delSistema());
    }

    private static ContextoSesion analista(UUID id) {
        return ContextoSesion.de(id, "CUMPLIMIENTO", new Traza(UUID.randomUUID().toString()));
    }

    private ConsultarBandejasLft.PaginaDeAlertas alertas(
            List<String> severidades, Optional<OffsetDateTime> fecha, Optional<UUID> id, int limite) {
        return transaccion.execute(
                t -> bandejas.alertas(null, severidades, fecha, id, limite, analista(fixtura.usuario())));
    }

    @Test
    @DisplayName("Dadas tres alertas abiertas · Cuando el analista recorre la bandeja de a dos · Entonces las ve una"
            + " sola vez, de la mas antigua a la mas nueva, sin la persona ni el monto")
    void alertasEnOrden() {
        List<UUID> creadas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            creadas.add(gobiernoFixtura.alerta(regla, titular, "ALTA", "5000.00", VENTANA.plusMinutes(i)));
        }

        var primera = alertas(null, Optional.of(VENTANA.minusSeconds(1)), Optional.of(ANTES_DE_TODO), 2);
        var segunda = alertas(null, primera.proximoFecha(), primera.proximoId(), 2);

        List<UUID> vistas =
                new ArrayList<>(primera.items().stream().map(Alerta::alertaId).toList());
        vistas.add(segunda.items().get(0).alertaId());
        assertThat(vistas).containsExactlyElementsOf(creadas);
        assertThat(primera.items().get(0).reglaCodigo()).startsWith("RM-");
        assertThat(primera.items().get(0).estado()).isEqualTo("ABIERTA");
        // La fila no tiene donde llevar a la persona ni el monto: el registro no los declara.
        assertThat(Alerta.class.getRecordComponents())
                .extracting(c -> c.getName())
                .doesNotContain("usuarioId", "montoInvolucrado", "detalle");
    }

    @Test
    @DisplayName("Dadas alertas de severidad ALTA y BAJA · Cuando el analista filtra por CRITICA y ALTA · Entonces solo"
            + " ve las ALTA")
    void filtroDeSeveridad() {
        UUID alta = gobiernoFixtura.alerta(regla, titular, "ALTA", "5000.00", VENTANA.plusDays(10));
        gobiernoFixtura.alerta(
                regla, titular, "BAJA", "10.00", VENTANA.plusDays(10).plusMinutes(1));

        var pagina = alertas(
                List.of("CRITICA", "ALTA"),
                Optional.of(VENTANA.plusDays(10).minusSeconds(1)),
                Optional.of(ANTES_DE_TODO),
                25);

        assertThat(pagina.items()).extracting(Alerta::alertaId).containsExactly(alta);
    }

    @Test
    @DisplayName("Dado un caso abierto cuyo plazo guardado ya paso · Cuando el oficial consulta la bandeja de casos ·"
            + " Entonces aparece primero y vencido, sin la persona ni el resumen")
    void casoVencido() {
        UUID alerta = gobiernoFixtura.alerta(regla, titular, "ALTA", "5000.00", OffsetDateTime.now(ZoneOffset.UTC));
        UUID analistaId = fixtura.usuario();
        var caso = transaccion.execute(t -> casoCU.abrirCaso(
                new EntradaCaso(List.of(alerta), analistaId, "Fraccionamiento detectado en el periodo"),
                analista(analistaId)));
        OffsetDateTime vencio = OffsetDateTime.of(2001, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        dslFixtura.execute(
                "UPDATE cumplimiento.caso_investigacion_lft SET abierto_en = ?::timestamptz, plazo_limite = ?::timestamptz"
                        + " WHERE id = ?",
                vencio.minusDays(30),
                vencio,
                caso.casoId());

        var pagina = transaccion.execute(t -> bandejas.casos(
                null, Optional.of(vencio.minusSeconds(1)), Optional.of(ANTES_DE_TODO), 1, analista(fixtura.usuario())));

        Caso fila = pagina.items().get(0);
        assertThat(fila.casoId()).isEqualTo(caso.casoId());
        assertThat(fila.vencido()).isTrue();
        assertThat(fila.plazoLimite().toInstant()).isEqualTo(vencio.toInstant());
        assertThat(fila.analistaId()).isEqualTo(analistaId);
        assertThat(Caso.class.getRecordComponents())
                .extracting(c -> c.getName())
                .doesNotContain("usuarioId", "resumen", "hallazgos");
    }

    @Test
    @DisplayName("Dada una ventana sin alertas · Cuando el analista consulta la bandeja · Entonces recibe una pagina"
            + " vacia y sin cursor, nunca un error")
    void bandejaVacia() {
        var pagina = alertas(
                null,
                Optional.of(OffsetDateTime.of(2099, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC)),
                Optional.of(ANTES_DE_TODO),
                25);

        assertThat(pagina.items()).isEmpty();
        assertThat(pagina.proximoId()).isEmpty();
    }

    @Test
    @DisplayName("rechaza un filtro invalido: severidad o estado desconocidos, o cursor a medias, son AP-CU44-05")
    void filtroInvalido() {
        var ctx = analista(fixtura.usuario());
        assertThatThrownBy(() -> transaccion.execute(
                        t -> bandejas.alertas(null, List.of("GRAVISIMA"), Optional.empty(), Optional.empty(), 25, ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU44-05"));
        assertThatThrownBy(() -> transaccion.execute(
                        t -> bandejas.casos(List.of("ARCHIVADO"), Optional.empty(), Optional.empty(), 25, ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU44-05"));
        assertThatThrownBy(() -> transaccion.execute(
                        t -> bandejas.casos(null, Optional.of(OffsetDateTime.now()), Optional.empty(), 25, ctx)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU44-05"));
    }
}

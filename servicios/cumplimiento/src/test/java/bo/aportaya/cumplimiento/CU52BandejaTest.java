package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo.EntradaProrroga;
import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo.EntradaReclamo;
import bo.aportaya.cumplimiento.aplicacion.CU52AtenderReclamo.EntradaRespuesta;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejaDeReclamos;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejaDeReclamos.Fila;
import bo.aportaya.cumplimiento.aplicacion.ConsultarBandejaDeReclamos.Pagina;
import bo.aportaya.cumplimiento.infraestructura.BandejaDeReclamosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · H3 · la bandeja y la ficha del Punto de Reclamo, contra PostgreSQL real.
 *
 * <p>Lo que importa: el que vence antes va primero (la prorroga cuenta como limite), la cola no
 * repite ni salta reclamos entre paginas, lo cerrado sale de la vista por omision, y {@code vencido}
 * compara el limite GUARDADO con la hora sin recalcular ningun plazo (R-CON-01).
 */
class CU52BandejaTest extends BaseDeCumplimiento {

    private static final String DESCRIPCION =
            "Me cobraron una comision que no figura en el tarifario publicado del mes.";

    private String punto;
    private ConsultarBandejaDeReclamos bandeja;

    @BeforeEach
    void escenario() {
        // La bandeja es GLOBAL: sin esto, los reclamos que dejaron otras clases se mezclan en el orden.
        uif.limpiar();
        punto = "APP-" + UUID.randomUUID().toString().substring(0, 8);
        gobiernoFixtura.puntoDeReclamo(punto, "APP", true);
        bandeja =
                new ConsultarBandejaDeReclamos(new Datos(dsl), new BandejaDeReclamosRepositorio(), Reloj.delSistema());
    }

    private static ContextoSesion operador(UUID id) {
        return ContextoSesion.de(id, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private UUID ingresar(String monto) {
        UUID usuario = fixtura.usuario();
        var ctx = ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
        return transaccion
                .execute(t -> reclamoCU.ingresar(
                        new EntradaReclamo(
                                usuario,
                                punto,
                                "COMISION",
                                "BILLETERA",
                                monto == null ? null : new BigDecimal(monto),
                                DESCRIPCION,
                                "APP"),
                        ctx))
                .reclamoId();
    }

    private Pagina pagina(List<String> estados, Optional<OffsetDateTime> limite, Optional<UUID> id, int tamano) {
        return transaccion.execute(t -> bandeja.bandeja(estados, limite, id, tamano, operador(UUID.randomUUID())));
    }

    @Test
    @DisplayName("Dados tres reclamos abiertos, uno con prorroga · Cuando el operador recorre la bandeja de a dos"
            + " · Entonces los ve una sola vez, el que vence antes primero, y el prorrogado al final")
    void recorrePorVencimiento() {
        UUID a = ingresar("18.00");
        UUID prorrogado = ingresar(null);
        UUID c = ingresar("7.50");
        var plazo = dsl.fetchOne("SELECT plazo_respuesta FROM cumplimiento.reclamo_cliente WHERE id = ?", prorrogado)
                .get(0, OffsetDateTime.class);
        transaccion.execute(t -> {
            reclamoCU.prorrogar(
                    new EntradaProrroga(prorrogado, plazo.plusDays(3), OffsetDateTime.now(ZoneOffset.UTC), null, null),
                    operador(fixtura.usuario()));
            return null;
        });

        Pagina primera = pagina(null, Optional.empty(), Optional.empty(), 2);
        Pagina segunda = pagina(null, primera.proximoLimite(), primera.proximoId(), 2);

        assertThat(primera.items()).hasSize(2);
        assertThat(segunda.items()).hasSize(1);
        assertThat(segunda.proximoId()).isEmpty();
        List<Fila> todas = new ArrayList<>(primera.items());
        todas.addAll(segunda.items());
        assertThat(todas).extracting(Fila::reclamoId).containsExactlyInAnyOrder(a, prorrogado, c);
        assertThat(todas)
                .isSortedAccordingTo(Comparator.comparing(Fila::limiteVigente).thenComparing(Fila::reclamoId));
        assertThat(todas.get(2).reclamoId()).isEqualTo(prorrogado);
        assertThat(todas.get(2).limiteVigente())
                .isEqualTo(todas.get(2).plazoProrrogadoHasta().orElseThrow());
        assertThat(todas).filteredOn(f -> f.reclamoId().equals(a)).first().satisfies(f -> assertThat(f.montoReclamado())
                .contains("18.00"));
    }

    @Test
    @DisplayName("Dado un reclamo ya respondido · Cuando se consulta la bandeja sin filtro · Entonces no aparece;"
            + " con el filtro CERRADO si")
    void loCerradoSaleDeLaVistaPorOmision() {
        UUID abierto = ingresar(null);
        UUID cerrado = ingresar(null);
        transaccion.execute(t -> reclamoCU.responder(
                new EntradaRespuesta(cerrado, "DESFAVORABLE", "Se reviso el cobro y corresponde.", null),
                operador(fixtura.usuario())));

        assertThat(pagina(null, Optional.empty(), Optional.empty(), 25).items())
                .extracting(Fila::reclamoId)
                .containsExactly(abierto);
        assertThat(pagina(List.of("CERRADO"), Optional.empty(), Optional.empty(), 25)
                        .items())
                .extracting(Fila::reclamoId)
                .containsExactly(cerrado);
    }

    @Test
    @DisplayName("Dado un reclamo abierto cuyo limite guardado ya paso · Cuando se consulta la bandeja · Entonces"
            + " aparece vencido, sin que su plazo cambie")
    void vencidoComparaElLimiteGuardado() {
        UUID reclamo = ingresar(null);
        OffsetDateTime pasado = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).withNano(0);
        // Un reclamo ingresado hace dos semanas cuyo plazo vencio ayer (ck_reclamo_plazo exige ingreso < plazo).
        dslFixtura.execute(
                """
                        UPDATE cumplimiento.reclamo_cliente SET fecha_ingreso = ?::timestamptz, plazo_respuesta = ?::timestamptz
                         WHERE id = ?
                        """,
                pasado.minusDays(13),
                pasado,
                reclamo);

        Fila fila = pagina(null, Optional.empty(), Optional.empty(), 25).items().get(0);

        assertThat(fila.vencido()).isTrue();
        assertThat(fila.plazoRespuesta().toInstant()).isEqualTo(pasado.toInstant());
    }

    @Test
    @DisplayName("Dada una bandeja sin reclamos abiertos · Cuando el operador la consulta · Entonces recibe una"
            + " pagina vacia y sin cursor, nunca un error")
    void bandejaVacia() {
        Pagina vacia = pagina(null, Optional.empty(), Optional.empty(), 25);

        assertThat(vacia.items()).isEmpty();
        assertThat(vacia.proximoLimite()).isEmpty();
        assertThat(vacia.proximoId()).isEmpty();
    }

    @Test
    @DisplayName("Dado un reclamo ingresado · Cuando quien lo atiende abre su ficha · Entonces ve el texto del"
            + " reclamo y todavia ninguna respuesta")
    void fichaConElTexto() {
        UUID reclamo = ingresar("18.00");

        var ficha = transaccion.execute(t -> bandeja.ficha(reclamo, operador(UUID.randomUUID())));

        assertThat(ficha.descripcion()).isEqualTo(DESCRIPCION);
        assertThat(ficha.resultado()).isEmpty();
        assertThat(ficha.reclamo().estado()).isEqualTo("INGRESADO");
    }

    @Test
    @DisplayName("rechaza un filtro invalido: estado desconocido o cursor a medias son AP-CU52-07")
    void filtroInvalido() {
        assertThatThrownBy(() -> pagina(List.of("PERDIDO"), Optional.empty(), Optional.empty(), 25))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU52-07"));
        assertThatThrownBy(() -> pagina(null, Optional.of(OffsetDateTime.now()), Optional.empty(), 25))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU52-07"));
    }

    @Test
    @DisplayName("rechaza la ficha de un reclamo inexistente: AP-CU52-05, no un 500")
    void fichaInexistente() {
        assertThatThrownBy(
                        () -> transaccion.execute(t -> bandeja.ficha(UUID.randomUUID(), operador(UUID.randomUUID()))))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU52-05"));
    }
}

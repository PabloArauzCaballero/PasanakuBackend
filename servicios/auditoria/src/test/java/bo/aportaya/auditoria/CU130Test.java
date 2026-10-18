package bo.aportaya.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.auditoria.aplicacion.CU130GobernarFichaDeTabla;
import bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente;
import bo.aportaya.auditoria.dominio.FichaDeGobierno;
import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio;
import bo.aportaya.auditoria.infraestructura.FichaGobiernoRepositorio;
import bo.aportaya.auditoria.infraestructura.PoliticaRetencionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-130 · Gobernar la ficha de cada tabla del catalogo de datos.
 *
 * <p>Los cuatro criterios contra PostgreSQL real: la version como condicion de la escritura solo se prueba con la
 * base, no con un doble. Cada prueba usa una tabla distinta del catalogo para no depender del orden.
 */
class CU130Test extends BaseDeAuditoria {

    private static CU130GobernarFichaDeTabla cu130;

    @BeforeAll
    static void armarCU130() {
        ObjectMapper mapper = new ObjectMapper();
        cu130 = new CU130GobernarFichaDeTabla(
                new Datos(dsl),
                new FichaGobiernoRepositorio(),
                new PoliticaRetencionRepositorio(),
                new BitacoraRepositorio(),
                new CatalogoDeDatosVigente(mapper),
                Reloj.delSistema(),
                mapper);
    }

    @Test
    @DisplayName("Dada una tabla del catálogo sin ficha guardada · Cuando se consulta el catálogo de datos · Entonces"
            + " la tabla figura activa y detectada, sin responsable")
    void criterio1() {
        var vista = transaccion.execute(e -> cu130.consultar(contexto()));

        FichaDeGobierno ficha = vista.fichas()
                .getOrDefault(FichaDeGobierno.clave("auditoria", "ticket_soporte"), FichaDeGobierno.porOmision());
        assertThat(ficha.estado()).isEqualTo("ACTIVA");
        assertThat(ficha.estadoRevision()).isEqualTo("DETECTADA");
        assertThat(ficha.responsable()).isEmpty();
        assertThat(ficha.version()).isZero();
    }

    @Test
    @DisplayName("Dado un operador con permiso para editar el catálogo de datos · Cuando guarda la ficha de una tabla"
            + " como candidata a retiro · Entonces el catálogo la muestra así y la bitácora registra el cambio")
    void criterio2() {
        FichaDeGobierno guardada = transaccion.execute(e -> cu130.guardar(
                "auditoria", "regla_cumplimiento", ficha("CANDIDATA_A_RETIRO", "Sistemas", 0), contexto()));

        assertThat(guardada.estado()).isEqualTo("CANDIDATA_A_RETIRO");
        assertThat(guardada.version()).isEqualTo(1);
        assertThat(guardada.revisadoPor()).contains(OPERADOR);

        var vista = transaccion.execute(e -> cu130.consultar(contexto()));
        assertThat(vista.fichas().get("auditoria.regla_cumplimiento").estado()).isEqualTo("CANDIDATA_A_RETIRO");

        Number eventos = (Number) dslFixtura
                .fetchOne("SELECT count(*) FROM comun.bitacora_evento b JOIN auditoria.ficha_gobierno_tabla f"
                        + " ON f.id = b.entidad_id WHERE f.esquema = 'auditoria'"
                        + " AND f.tabla = 'regla_cumplimiento' AND b.accion = 'CAMBIO_CONFIGURACION'"
                        + " AND b.valor_nuevo ->> 'estado' = 'CANDIDATA_A_RETIRO'")
                .get(0);
        assertThat(eventos.intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("Dadas dos ediciones de la misma ficha leídas en la misma versión · Cuando la segunda se guarda"
            + " después de la primera · Entonces se rechaza con AP-CU130-01 y la ficha conserva lo de la primera")
    void criterio3() {
        transaccion.execute(
                e -> cu130.guardar("auditoria", "incidente_operativo", ficha("ACTIVA", "Primera", 0), contexto()));

        assertThatThrownBy(() -> transaccion.execute(e ->
                        cu130.guardar("auditoria", "incidente_operativo", ficha("RETIRADA", "Segunda", 0), contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .extracting(e -> ((ErrorDeNegocio) e).codigo().valor())
                .isEqualTo("AP-CU130-01");

        var vista = transaccion.execute(e -> cu130.consultar(contexto()));
        FichaDeGobierno actual = vista.fichas().get("auditoria.incidente_operativo");
        assertThat(actual.responsable()).contains("Primera");
        assertThat(actual.estado()).isEqualTo("ACTIVA");
        assertThat(actual.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("Dada una tabla que no está en el catálogo · Cuando se intenta guardar su ficha · Entonces se rechaza"
            + " con AP-CU130-02 y no se crea ninguna fila")
    void criterio4() {
        assertThatThrownBy(() -> transaccion.execute(
                        e -> cu130.guardar("auditoria", "tabla_que_no_existe", ficha("ACTIVA", null, 0), contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .extracting(e -> ((ErrorDeNegocio) e).codigo().valor())
                .isEqualTo("AP-CU130-02");

        Number filas = (Number) dslFixtura
                .fetchOne("SELECT count(*) FROM auditoria.ficha_gobierno_tabla WHERE tabla = 'tabla_que_no_existe'")
                .get(0);
        assertThat(filas.intValue()).isZero();
    }

    @Test
    @DisplayName("Rechaza una politica de retencion que no existe con AP-CU130-03 y no guarda la ficha")
    void rechazaRetencionDesconocida() {
        FichaDeGobierno conRetencionInventada = new FichaDeGobierno(
                "ACTIVA",
                "APROBADA",
                Optional.empty(),
                Optional.of(UUID.randomUUID()),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0);

        assertThatThrownBy(() -> transaccion.execute(
                        e -> cu130.guardar("auditoria", "ticket_soporte", conRetencionInventada, contexto())))
                .extracting(e -> ((ErrorDeNegocio) e).codigo().valor())
                .isEqualTo("AP-CU130-03");
        assertThat(transaccion.execute(e -> cu130.consultar(contexto())).fichas())
                .doesNotContainKey("auditoria.ticket_soporte");
    }

    private static FichaDeGobierno ficha(String estado, String responsable, int version) {
        return new FichaDeGobierno(
                estado,
                "APROBADA",
                Optional.ofNullable(responsable),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                version);
    }
}

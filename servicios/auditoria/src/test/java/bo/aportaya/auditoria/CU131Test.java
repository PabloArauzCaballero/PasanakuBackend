package bo.aportaya.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.auditoria.aplicacion.CU131RevisarColumna;
import bo.aportaya.auditoria.aplicacion.CatalogoDeDatosVigente;
import bo.aportaya.auditoria.dominio.RevisionDeColumna;
import bo.aportaya.auditoria.infraestructura.BitacoraRepositorio;
import bo.aportaya.auditoria.infraestructura.RevisionColumnaRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-131 · Revisar cada columna del catalogo de datos. Los cuatro criterios contra PostgreSQL real; cada prueba usa
 * una columna distinta de {@code auditoria.ticket_soporte} para no depender del orden.
 */
class CU131Test extends BaseDeAuditoria {

    private static CU131RevisarColumna cu131;

    @BeforeAll
    static void armarCU131() {
        ObjectMapper mapper = new ObjectMapper();
        cu131 = new CU131RevisarColumna(
                new Datos(dsl),
                new RevisionColumnaRepositorio(),
                new BitacoraRepositorio(),
                new CatalogoDeDatosVigente(mapper),
                Reloj.delSistema(),
                mapper);
    }

    @Test
    @DisplayName("Dada una columna del catálogo sin revisión · Cuando se consulta el catálogo de datos · Entonces la"
            + " columna figura sin revisar")
    void criterio1() {
        var revisiones = transaccion.execute(e -> cu131.consultar(contexto()));
        assertThat(revisiones).doesNotContainKey(RevisionDeColumna.clave("auditoria", "ticket_soporte", "id"));
    }

    @Test
    @DisplayName("Dado un operador con permiso para editar el catálogo de datos · Cuando aprueba una columna con"
            + " confianza alta · Entonces el catálogo la muestra aprobada y la bitácora registra la revisión")
    void criterio2() {
        RevisionDeColumna guardada = transaccion.execute(e -> cu131.guardar(
                "auditoria", "ticket_soporte", "estado", revision("APROBADA", "ALTA", null, 0), contexto()));

        assertThat(guardada.decision()).isEqualTo("APROBADA");
        assertThat(guardada.confianza()).contains("ALTA");
        assertThat(guardada.version()).isEqualTo(1);
        assertThat(guardada.revisadoPor()).contains(OPERADOR);

        var revisiones = transaccion.execute(e -> cu131.consultar(contexto()));
        assertThat(revisiones.get("auditoria.ticket_soporte.estado").decision()).isEqualTo("APROBADA");

        Number eventos = (Number) dslFixtura
                .fetchOne(
                        """
                        SELECT count(*) FROM comun.bitacora_evento b
                        JOIN auditoria.revision_columna r ON r.id = b.entidad_id
                        WHERE r.esquema = ? AND r.tabla = ? AND r.columna = ? AND b.accion = 'CAMBIO_CONFIGURACION'
                          AND b.valor_nuevo ->> 'decision' = 'APROBADA'
                        """,
                        "auditoria",
                        "ticket_soporte",
                        "estado")
                .get(0);
        assertThat(eventos.intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("Dada una columna que se rechaza sin motivo · Cuando se intenta guardar la revisión · Entonces se"
            + " rechaza con AP-CU131-02 y la columna sigue sin revisar")
    void criterio3() {
        assertThatThrownBy(() -> transaccion.execute(e -> cu131.guardar(
                        "auditoria", "ticket_soporte", "categoria", revision("RECHAZADA", null, "   ", 0), contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .extracting(e -> ((ErrorDeNegocio) e).codigo().valor())
                .isEqualTo("AP-CU131-02");
        Map<String, RevisionDeColumna> despues = transaccion.execute(e -> cu131.consultar(contexto()));
        assertThat(despues).doesNotContainKey("auditoria.ticket_soporte.categoria");
    }

    @Test
    @DisplayName("Dada una columna que no está en el catálogo · Cuando se intenta revisarla · Entonces se rechaza con"
            + " AP-CU131-01 y no se crea ninguna fila")
    void criterio4() {
        assertThatThrownBy(() -> transaccion.execute(e -> cu131.guardar(
                        "auditoria",
                        "ticket_soporte",
                        "columna_inventada",
                        revision("APROBADA", null, null, 0),
                        contexto())))
                .isInstanceOf(ErrorDeNegocio.class)
                .extracting(e -> ((ErrorDeNegocio) e).codigo().valor())
                .isEqualTo("AP-CU131-01");
        Number filas = (Number) dslFixtura
                .fetchOne("SELECT count(*) FROM auditoria.revision_columna WHERE columna = 'columna_inventada'")
                .get(0);
        assertThat(filas.intValue()).isZero();
    }

    @Test
    @DisplayName("Concurrencia: la segunda revision leida en la misma version se rechaza con AP-CU131-03 y la primera"
            + " queda")
    void concurrencia() {
        transaccion.execute(e -> cu131.guardar(
                "auditoria",
                "ticket_soporte",
                "prioridad",
                revision("NECESITA_REVISION", "BAJA", "Primera", 0),
                contexto()));
        assertThatThrownBy(() -> transaccion.execute(e -> cu131.guardar(
                        "auditoria",
                        "ticket_soporte",
                        "prioridad",
                        revision("APROBADA", null, "Segunda", 0),
                        contexto())))
                .extracting(e -> ((ErrorDeNegocio) e).codigo().valor())
                .isEqualTo("AP-CU131-03");
        RevisionDeColumna actual =
                transaccion.execute(e -> cu131.consultar(contexto())).get("auditoria.ticket_soporte.prioridad");
        assertThat(actual.motivo()).contains("Primera");
        assertThat(actual.version()).isEqualTo(1);
    }

    private static RevisionDeColumna revision(String decision, String confianza, String motivo, int version) {
        return new RevisionDeColumna(
                decision,
                Optional.ofNullable(confianza),
                Optional.ofNullable(motivo),
                Optional.empty(),
                Optional.empty(),
                version);
    }
}

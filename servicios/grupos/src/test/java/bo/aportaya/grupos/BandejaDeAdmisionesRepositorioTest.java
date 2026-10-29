package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso.FilaDeBandeja;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H4.B.M1 · la bandeja de «Admisiones» del backoffice contra PostgreSQL real: solicitudes de VARIOS grupos,
 * las mas nuevas primero, con el codigo del grupo y el estado con los nombres de la pantalla. La base se
 * comparte con otras clases, asi que cada afirmacion mira solo las filas que esta prueba creo.
 */
class BandejaDeAdmisionesRepositorioTest extends BaseDeCU68 {

    private CU68AceptarIngreso cu() {
        return new CU68AceptarIngreso(new Datos(dsl), new Outbox("grupos"));
    }

    /** SOPORTE: un rol del backoffice que la RLS de solicitud_ingreso NO deja pasar por si sola. */
    private ContextoSesion soporte() {
        return ContextoSesion.de(
                UUID.randomUUID(), "SOPORTE", new Traza(UUID.randomUUID().toString()));
    }

    private UUID solicitud(UUID grupo, UUID usuario, String estado, String nombreVisible, String haceCuanto) {
        UUID id = UUID.randomUUID();
        boolean resuelta = !"PENDIENTE".equals(estado);
        dslFixtura.execute(
                """
                INSERT INTO grupos.solicitud_ingreso
                    (id, grupo_id, usuario_id, cupos_solicitados, mensaje, estado, nombre_visible,
                     fecha_solicitud, revisada_por, fecha_resolucion)
                VALUES (?, ?, ?, 1, 'quiero entrar', ?, ?, now() - ?::interval, ?, ?::timestamptz)
                """,
                id,
                grupo,
                usuario,
                estado,
                nombreVisible,
                haceCuanto,
                resuelta ? fixtura.usuario() : null,
                resuelta ? java.time.OffsetDateTime.now() : null);
        return id;
    }

    private record Armado(UUID grupoA, UUID grupoB, UUID pendienteA, UUID aprobadaA, UUID rechazadaB, UUID pendienteB) {
        Set<UUID> todas() {
            return Set.of(pendienteA, aprobadaA, rechazadaB, pendienteB);
        }
    }

    private Armado dejarArmadas() {
        UUID a = fixtura.grupoConformado(3);
        UUID b = fixtura.grupoConformado(3);
        return new Armado(
                a,
                b,
                solicitud(a, fixtura.usuario(), "PENDIENTE", "Ana Rojas", "3 hours"),
                solicitud(a, fixtura.usuario(), "APROBADA", null, "2 hours"),
                solicitud(b, fixtura.usuario(), "RECHAZADA", null, "1 hour"),
                solicitud(b, fixtura.usuario(), "PENDIENTE", null, "10 minutes"));
    }

    private static List<FilaDeBandeja> solo(List<FilaDeBandeja> filas, Set<UUID> ids) {
        return filas.stream().filter(f -> ids.contains(f.id())).toList();
    }

    @Test
    @DisplayName(
            "Dado un rol del backoffice que la RLS no deja pasar · Cuando abre la bandeja · Entonces ve las solicitudes de TODOS los grupos, las mas nuevas primero")
    void variosGruposDeLaMasNuevaALaMasVieja() {
        var a = dejarArmadas();

        var filas = solo(cu().bandeja(null, null, soporte()), a.todas());

        assertThat(filas)
                .extracting(FilaDeBandeja::id)
                .containsExactly(a.pendienteB(), a.rechazadaB(), a.aprobadaA(), a.pendienteA());
        assertThat(filas).extracting(FilaDeBandeja::grupoId).containsOnly(a.grupoA(), a.grupoB());
    }

    @Test
    @DisplayName("trae el codigo publico del grupo y el nombre con que se presento quien pidio entrar")
    void traeElCodigoDelGrupoYElNombre() {
        var a = dejarArmadas();
        String codigoA = dslFixtura
                .fetchOne("SELECT codigo_publico FROM grupos.grupo WHERE id = ?", a.grupoA())
                .get(0, String.class);

        var fila = solo(cu().bandeja(null, null, soporte()), Set.of(a.pendienteA()))
                .get(0);

        assertThat(fila.grupoCodigo()).isEqualTo(codigoA);
        assertThat(fila.nombreVisible()).isEqualTo("Ana Rojas");
        assertThat(fila.estado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("el estado sale con los nombres de la pantalla: lo que la base guarda como APROBADA es ACEPTADA")
    void aprobadaSeLlamaAceptada() {
        var a = dejarArmadas();

        var todas = solo(cu().bandeja(null, null, soporte()), a.todas());

        assertThat(todas)
                .filteredOn(f -> f.id().equals(a.aprobadaA()))
                .extracting(FilaDeBandeja::estado)
                .containsExactly("ACEPTADA");
        assertThat(todas).extracting(FilaDeBandeja::estado).doesNotContain("APROBADA");
    }

    @Test
    @DisplayName("filtrar por ACEPTADA trae solo las aprobadas; por PENDIENTE, solo las pendientes de los dos grupos")
    void filtraPorEstadoConLosNombresDeLaPantalla() {
        var a = dejarArmadas();

        assertThat(solo(cu().bandeja("ACEPTADA", null, soporte()), a.todas()))
                .extracting(FilaDeBandeja::id)
                .containsExactly(a.aprobadaA());
        assertThat(solo(cu().bandeja("PENDIENTE", null, soporte()), a.todas()))
                .extracting(FilaDeBandeja::id)
                .containsExactly(a.pendienteB(), a.pendienteA());
        assertThat(solo(cu().bandeja("EXPIRADA", null, soporte()), a.todas())).isEmpty();
    }

    @Test
    @DisplayName("un estado que no existe no coincide con ninguna fila (no revienta ni devuelve todo)")
    void estadoDesconocidoNoCoincide() {
        var a = dejarArmadas();

        assertThat(solo(cu().bandeja("TAL_VEZ", null, soporte()), a.todas())).isEmpty();
    }

    @Test
    @DisplayName("el limite acota la lista: 1 devuelve una sola fila y uno fuera de rango se acota a 1..200")
    void limiteAcotaLaLista() {
        dejarArmadas();

        assertThat(cu().bandeja(null, 1, soporte())).hasSize(1);
        assertThat(cu().bandeja(null, 0, soporte())).hasSize(1);
        assertThat(cu().bandeja(null, 100_000, soporte())).hasSizeLessThanOrEqualTo(200);
    }
}

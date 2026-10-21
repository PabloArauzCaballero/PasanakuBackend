package bo.aportaya.grupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.aplicacion.CU69FirmarReglamento;
import bo.aportaya.grupos.aplicacion.ConsultarDetalleDelGrupo;
import bo.aportaya.grupos.infraestructura.CalendarioDelGrupoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · Firmar el reglamento, conformar el grupo y sortear (2026-10-20), de punta a punta sobre PostgreSQL real:
 * tres integrantes pendientes firman, el grupo se conforma con su calendario, el organizador sortea y el detalle
 * muestra a cada uno con su turno. Datos sinteticos.
 */
class CU69FirmaYSorteoTest extends BaseDeCU60 {

    private static final String HASH = "c".repeat(64);

    private CU69FirmarReglamento firma;
    private ConsultarDetalleDelGrupo detalle;
    private UUID grupo;
    private final List<UUID> usuarios = new ArrayList<>();

    @BeforeEach
    void preparar() {
        firma = new CU69FirmarReglamento(new Datos(dsl), new CalendarioDelGrupoRepositorio(), Reloj.delSistema());
        detalle = new ConsultarDetalleDelGrupo(new Datos(dsl));
        grupo = fixtura.grupoConformado(3);
        dsl.execute("UPDATE grupos.grupo SET estado = 'ABIERTO_A_INSCRIPCION' WHERE id = ?", grupo);
        dsl.execute(
                """
                INSERT INTO grupos.reglamento_grupo
                  (grupo_id, version, contenido, hash_contenido, clausulas_mora, clausulas_abandono, vigente_desde,
                   redactado_por)
                VALUES (?, 1, 'Reglamento de prueba.', ?, 'Recargo.', 'Retiro.', now(), ?)
                """,
                grupo,
                HASH,
                organizador);
        usuarios.clear();
        String[] nombres = {"Rosa Quispe", "Juan Mamani", "Ana Choque"};
        for (int numero = 1; numero <= 3; numero++) {
            UUID usuario = numero == 1 ? organizador : fixtura.usuario();
            UUID participante = UUID.randomUUID();
            dsl.execute(
                    """
                    INSERT INTO grupos.participante
                      (id, grupo_id, usuario_id, alias, estado, es_organizador, fecha_ingreso,
                       reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                    VALUES (?, ?, ?, ?, 'ACEPTADO_PENDIENTE_FIRMA', ?, now(), 0, 0, 0)
                    """,
                    participante,
                    grupo,
                    usuario,
                    nombres[numero - 1],
                    numero == 1);
            dsl.execute(
                    """
                    INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en)
                    VALUES (gen_random_uuid(), ?, ?, ?, 'RESERVADO', 1.00, now())
                    """,
                    grupo,
                    (short) numero,
                    participante);
            usuarios.add(usuario);
        }
    }

    private ContextoSesion de(UUID usuario) {
        return ContextoSesion.de(
                usuario, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));
    }

    private CU69FirmarReglamento.Firmado firmar(UUID usuario) {
        return transaccion.execute(e -> firma.firmar(grupo, HASH, "127.0.0.1", de(usuario)));
    }

    private int periodos() {
        return dsl.fetchOne("SELECT count(*)::int FROM grupos.periodo WHERE grupo_id = ?", grupo)
                .get(0, Integer.class);
    }

    @Test
    @DisplayName("Dado un integrante aceptado que todavia no firmo · Cuando firma el reglamento vigente · "
            + "Entonces queda ACTIVO, su cupo OCUPADO y el detalle lo muestra FIRMADO")
    void firmarDejaActivoYOcupado() {
        var f = firmar(usuarios.get(1));

        assertThat(f.grupoConformado()).isFalse();
        var d = transaccion.execute(e -> detalle.consultar(grupo, de(usuarios.get(1))));
        assertThat(d.integrantes())
                .extracting(ConsultarDetalleDelGrupo.Integrante::estado)
                .containsExactly("PENDIENTE_FIRMA", "FIRMADO", "PENDIENTE_FIRMA");
        assertThat(d.integrantes())
                .extracting(ConsultarDetalleDelGrupo.Integrante::nombre)
                .containsExactly("Rosa Quispe", "Juan Mamani", "Ana Choque");
        assertThat(d.debeFirmar()).isFalse();
        assertThat(d.puedeSortear()).isFalse();
    }

    @Test
    @DisplayName("Dado un grupo con todos sus cupos reservados · Cuando firma el ultimo integrante · "
            + "Entonces el grupo queda CONFORMADO con un periodo por cupo y el organizador puede sortear")
    void laUltimaFirmaConformaYArmaElCalendario() {
        firmar(usuarios.get(1));
        firmar(usuarios.get(2));
        var ultima = firmar(usuarios.get(0));

        assertThat(ultima.grupoConformado()).isTrue();
        assertThat(ultima.estadoDelGrupo()).isEqualTo("CONFORMADO");
        assertThat(periodos()).isEqualTo(3);
        var d = transaccion.execute(e -> detalle.consultar(grupo, de(organizador)));
        assertThat(d.puedeSortear()).isTrue();
        // Repetir la firma no repite nada: ni otra aceptacion ni otro calendario.
        firmar(usuarios.get(0));
        assertThat(periodos()).isEqualTo(3);
        assertThat(dsl.fetchOne(
                                "SELECT count(*)::int FROM grupos.aceptacion_reglamento a JOIN grupos.participante p ON p.id = a.participante_id WHERE p.grupo_id = ?",
                                grupo)
                        .get(0, Integer.class))
                .isEqualTo(3);
    }

    @Test
    @DisplayName("Dado un grupo conformado por las firmas · Cuando el organizador compromete y revela el sorteo · "
            + "Entonces cada integrante tiene un turno distinto en el detalle")
    void sorteoDePuntaAPunta() {
        firmar(usuarios.get(0));
        firmar(usuarios.get(1));
        firmar(usuarios.get(2));

        var compromiso = transaccion.execute(e -> sortear.comprometer(grupo, List.of(), Optional.empty(), contexto()));
        transaccion.execute(
                e -> sortear.revelar(compromiso.sorteoId(), null, null, List.of(), null, Optional.empty(), contexto()));

        assertThat(turnosDe(grupo)).isEqualTo(3);
        var d = transaccion.execute(e -> detalle.consultar(grupo, de(usuarios.get(2))));
        assertThat(d.integrantes())
                .extracting(ConsultarDetalleDelGrupo.Integrante::turno)
                .containsExactlyInAnyOrder(1, 2, 3);
        assertThat(d.sorteoEstado()).isEqualTo("REVELADO");
        assertThat(d.puedeSortear()).isFalse();
    }

    @Test
    void firmarOtroTextoNoFirma() {
        assertThatThrownBy(() ->
                        transaccion.execute(e -> firma.firmar(grupo, "d".repeat(64), "127.0.0.1", de(usuarios.get(1)))))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-05"));
    }

    @Test
    void quienNoEsIntegranteNiFirmaNiVeElDetalle() {
        UUID ajeno = fixtura.usuario();
        assertThatThrownBy(() -> firmar(ajeno))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-13"));
        assertThatThrownBy(() -> transaccion.execute(e -> detalle.consultar(grupo, de(ajeno))))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-14"));
    }
}

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

    /**
     * Un grupo creado antes de que el organizador reservara el cupo 1 (antes del 2026-10-20): el organizador quedo
     * pendiente de firma y SIN cupo, y alguien que entro por el enlace ocupa el cupo 1. Quedan {@code libres} cupos.
     */
    private UUID grupoConOrganizadorSinCupo(int libres, String estadoDelOrganizador) {
        UUID viejo = fixtura.grupoConformado(1 + libres);
        dsl.execute("UPDATE grupos.grupo SET estado = 'ABIERTO_A_INSCRIPCION', cupos_ocupados = 1 WHERE id = ?", viejo);
        dsl.execute(
                """
                INSERT INTO grupos.reglamento_grupo
                  (grupo_id, version, contenido, hash_contenido, clausulas_mora, clausulas_abandono, vigente_desde,
                   redactado_por)
                VALUES (?, 1, 'Reglamento de prueba.', ?, 'Recargo.', 'Retiro.', now(), ?)
                """,
                viejo,
                HASH,
                organizador);
        UUID delEnlace = UUID.randomUUID();
        for (var fila : List.of(
                new Object[] {UUID.randomUUID(), organizador, "Rosa Quispe", estadoDelOrganizador, true},
                new Object[] {delEnlace, fixtura.usuario(), "Juan Mamani", "ACTIVO", false})) {
            dsl.execute(
                    """
                    INSERT INTO grupos.participante
                      (id, grupo_id, usuario_id, alias, estado, es_organizador, fecha_ingreso,
                       reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                    VALUES (?, ?, ?, ?, ?, ?, now(), 0, 0, 0)
                    """,
                    fila[0],
                    viejo,
                    fila[1],
                    fila[2],
                    fila[3],
                    fila[4]);
        }
        dsl.execute(
                """
                INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en)
                VALUES (gen_random_uuid(), ?, 1, ?, 'OCUPADO', 1.00, now())
                """,
                viejo,
                delEnlace);
        for (int numero = 2; numero <= 1 + libres; numero++) {
            dsl.execute(
                    "INSERT INTO grupos.cupo (id, grupo_id, numero, estado, fraccion) VALUES (gen_random_uuid(), ?, ?, 'LIBRE', 1.00)",
                    viejo,
                    (short) numero);
        }
        return viejo;
    }

    private int ocupadosSegunElGrupo(UUID grupoId) {
        return dsl.fetchOne("SELECT cupos_ocupados::int FROM grupos.grupo WHERE id = ?", grupoId)
                .get(0, Integer.class);
    }

    @Test
    @DisplayName("Dado el organizador de un grupo viejo, pendiente de firma y sin cupo · Cuando firma · "
            + "Entonces ocupa el primer cupo libre, figura FIRMADO y el grupo cuenta dos cupos ocupados")
    void elOrganizadorSinCupoAlFirmarOcupaElPrimerLibre() {
        UUID viejo = grupoConOrganizadorSinCupo(2, "ACEPTADO_PENDIENTE_FIRMA");

        var f = transaccion.execute(e -> firma.firmar(viejo, HASH, "127.0.0.1", de(organizador)));

        assertThat(f.grupoConformado()).isFalse();
        var d = transaccion.execute(e -> detalle.consultar(viejo, de(organizador)));
        assertThat(d.integrantes())
                .extracting(ConsultarDetalleDelGrupo.Integrante::estado)
                .containsExactly("FIRMADO", "FIRMADO", "LIBRE");
        assertThat(d.integrantes())
                .extracting(ConsultarDetalleDelGrupo.Integrante::nombre)
                .containsExactly("Juan Mamani", "Rosa Quispe", null);
        assertThat(d.integrantes().get(1).soyYo()).isTrue();
        assertThat(d.integrantes().get(1).esOrganizador()).isTrue();
        assertThat(d.cuposOcupados()).isEqualTo(2);
        assertThat(ocupadosSegunElGrupo(viejo)).isEqualTo(2);
        assertThat(d.debeFirmar()).isFalse();
    }

    @Test
    @DisplayName("Dado el organizador que ya firmo y quedo ACTIVO sin cupo · Cuando abre el grupo y vuelve a firmar · "
            + "Entonces se le pide firmar, recupera un cupo y su firma no se duplica")
    void quienYaFirmoSinCupoLoRecupera() {
        UUID viejo = grupoConOrganizadorSinCupo(2, "ACTIVO");
        assertThat(transaccion
                        .execute(e -> detalle.consultar(viejo, de(organizador)))
                        .debeFirmar())
                .isTrue();

        transaccion.execute(e -> firma.firmar(viejo, HASH, "127.0.0.1", de(organizador)));
        // Repetir no toma otro cupo.
        transaccion.execute(e -> firma.firmar(viejo, HASH, "127.0.0.1", de(organizador)));

        var d = transaccion.execute(e -> detalle.consultar(viejo, de(organizador)));
        assertThat(d.debeFirmar()).isFalse();
        assertThat(d.integrantes())
                .extracting(ConsultarDetalleDelGrupo.Integrante::estado)
                .containsExactly("FIRMADO", "FIRMADO", "LIBRE");
        assertThat(ocupadosSegunElGrupo(viejo)).isEqualTo(2);
        assertThat(dsl.fetchOne(
                                "SELECT count(*)::int FROM grupos.aceptacion_reglamento a JOIN grupos.participante p ON p.id = a.participante_id WHERE p.grupo_id = ?",
                                viejo)
                        .get(0, Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("Dado un grupo sin cupos libres y el organizador sin cupo · Cuando firma · "
            + "Entonces se rechaza con AP-CU69-01 y sigue pendiente de firma")
    void sinCupoLibreNoFirma() {
        // Tres cupos (el minimo de un grupo): el 1 del que entro por el enlace y el 2 y el 3 de otras personas.
        UUID viejo = grupoConOrganizadorSinCupo(2, "ACEPTADO_PENDIENTE_FIRMA");
        for (int numero = 2; numero <= 3; numero++) {
            UUID otro = UUID.randomUUID();
            dsl.execute(
                    """
                    INSERT INTO grupos.participante
                      (id, grupo_id, usuario_id, alias, estado, es_organizador, fecha_ingreso,
                       reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                    VALUES (?, ?, ?, 'Ana Choque', 'ACTIVO', false, now(), 0, 0, 0)
                    """,
                    otro,
                    viejo,
                    fixtura.usuario());
            dsl.execute(
                    "UPDATE grupos.cupo SET participante_id = ?, estado = 'OCUPADO', asignado_en = now() WHERE grupo_id = ? AND numero = ?",
                    otro,
                    viejo,
                    (short) numero);
        }
        dsl.execute("UPDATE grupos.grupo SET cupos_ocupados = 3 WHERE id = ?", viejo);

        assertThatThrownBy(() -> transaccion.execute(e -> firma.firmar(viejo, HASH, "127.0.0.1", de(organizador))))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-01"));
        assertThat(dsl.fetchOne(
                                "SELECT estado FROM grupos.participante WHERE grupo_id = ? AND usuario_id = ?",
                                viejo,
                                organizador)
                        .get(0, String.class))
                .isEqualTo("ACEPTADO_PENDIENTE_FIRMA");
        assertThat(ocupadosSegunElGrupo(viejo)).isEqualTo(3);
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

package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.entregas.dominio.puertos.HechosDeGrupos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H9.S1.M1-M3 · Ofertas del derecho a cobrar un turno, contra PostgreSQL real. */
class CU130OfertasTest extends BaseDeMercado {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private void rechaza(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo(codigo));
    }

    @Test
    @DisplayName(
            "CORRECTO · Dado el titular de un turno · Cuando publica el derecho a Bs 5.500 con cargos de Bs 50 y vigencia de un dia · Entonces queda PUBLICADA con su moneda, su precio y el valor del derecho")
    void publicar() {
        var c = caso();

        var publicada = publicar(c);

        assertThat(publicada.esNueva()).isTrue();
        var fila = dsl.fetchOne(
                "SELECT estado, moneda, monto_derecho, monto_precio, monto_cargos FROM entregas.oferta_turno WHERE id = ?",
                publicada.ofertaId());
        assertThat(fila.get(0, String.class)).isEqualTo("PUBLICADA");
        assertThat(fila.get(1, String.class)).isEqualTo("BOB");
        assertThat(numero("SELECT monto_derecho FROM entregas.oferta_turno WHERE id = ?", publicada.ofertaId()))
                .isEqualByComparingTo("6000.00");
        assertThat(numero("SELECT monto_precio FROM entregas.oferta_turno WHERE id = ?", publicada.ofertaId()))
                .isEqualByComparingTo("5500.00");
    }

    @Test
    @DisplayName(
            "Dada la misma clave dos veces · Cuando se publica · Entonces es la misma oferta; con otro precio la clave se rechaza (idempotencia)")
    void idempotente() {
        var c = caso();
        var hasta = OffsetDateTime.now().plusDays(1);
        var primera = ofertasCU.publicar(oferta(c, "5500.00", "50.00", hasta, "k-" + c.e().turnoId()), c.ctxVendedor());
        var segunda = ofertasCU.publicar(oferta(c, "5500.00", "50.00", hasta, "k-" + c.e().turnoId()), c.ctxVendedor());

        assertThat(segunda.ofertaId()).isEqualTo(primera.ofertaId());
        assertThat(segunda.esNueva()).isFalse();
        assertThat(contar("SELECT count(*)::int FROM entregas.oferta_turno WHERE turno_id = ?", c.e().turnoId()))
                .isEqualTo(1);
        rechaza(
                () -> ofertasCU.publicar(oferta(c, "5400.00", "50.00", hasta, "k-" + c.e().turnoId()), c.ctxVendedor()),
                "AP-CU130-04");
    }

    @Test
    @DisplayName(
            "INVALIDO · TITULAR AJENO · Dado un turno que no es suyo · Cuando otra persona lo ofrece · Entonces AP-CU130-01 y no se escribe nada")
    void turnoAjeno() {
        var c = caso();
        var otro = contextoDe(fixtura.usuario());

        rechaza(
                () -> ofertasCU.publicar(
                        oferta(c, "5500.00", "50.00", OffsetDateTime.now().plusDays(1), "ajena-" + c.e().turnoId()),
                        otro),
                "AP-CU130-01");
        assertThat(contar("SELECT count(*)::int FROM entregas.oferta_turno WHERE turno_id = ?", c.e().turnoId()))
                .isZero();
    }

    @Test
    @DisplayName(
            "INVALIDO · TURNO YA COBRADO O CON ENTREGA · Dado un turno cobrado, o con el pozo ya fondeado · Cuando se ofrece · Entonces AP-CU130-02")
    void turnoNoVendible() {
        var cobrado = caso();
        gruposDoble.turno(new HechosDeGrupos.Turno(
                cobrado.e().turnoId(),
                cobrado.e().grupoId(),
                cobrado.e().cupoId(),
                cobrado.e().participanteId(),
                cobrado.vendedor(),
                "COBRADO",
                bob("6000.00")));
        rechaza(() -> publicar(cobrado), "AP-CU130-02");

        var conEntrega = caso();
        aportesDoble.responde(new bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo.Recaudo(
                conEntrega.e().periodoId(),
                conEntrega.e().grupoId(),
                OffsetDateTime.now(),
                bob("6000.00"),
                bob("6000.00"),
                bob("0.00")));
        pozoCU.fondear(
                new bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto.Entrada(
                        conEntrega.e().grupoId(),
                        conEntrega.e().periodoId(),
                        conEntrega.e().turnoId(),
                        conEntrega.e().cupoId(),
                        conEntrega.e().participanteId(),
                        "BILLETERA_MOVIL",
                        java.time.LocalDate.now(),
                        "f-" + conEntrega.e().turnoId()),
                conEntrega.ctxVendedor());
        rechaza(() -> publicar(conEntrega), "AP-CU130-02");
    }

    @Test
    @DisplayName(
            "INVALIDO · OFERTA DUPLICADA · Dado un turno con oferta activa · Cuando se publica otra con otra clave · Entonces AP-CU130-04; y la base lo impide aunque se evite el caso de uso")
    void duplicada() {
        var c = caso();
        var primera = publicar(c);

        rechaza(
                () -> ofertasCU.publicar(
                        oferta(c, "5000.00", "0.00", OffsetDateTime.now().plusDays(1), "otra-" + c.e().turnoId()),
                        c.ctxVendedor()),
                "AP-CU130-04");
        String error = rechazaLaBase(
                """
                INSERT INTO entregas.oferta_turno
                    (id, grupo_id, turno_id, cupo_id, participante_origen_id, vendedor_usuario_id, moneda,
                     monto_derecho, monto_precio, monto_cargos, estado, vigente_hasta, publicada_en,
                     clave_idempotencia, version)
                SELECT gen_random_uuid(), grupo_id, turno_id, cupo_id, participante_origen_id, vendedor_usuario_id,
                       moneda, monto_derecho, monto_precio, monto_cargos, 'PUBLICADA', vigente_hasta, now(),
                       'directa-' || gen_random_uuid(), 0
                  FROM entregas.oferta_turno WHERE id = '"""
                        + primera.ofertaId() + "'");
        assertThat(error).contains("uq_oferta_turno_activa");
    }

    @Test
    @DisplayName(
            "INVALIDO · PRECIO, CARGOS, VIGENCIA Y MONEDA · Dado cada dato fuera de regla · Cuando se publica · Entonces AP-CU130-03 y nada queda escrito")
    void datosInvalidos() {
        var c = caso();
        var futuro = OffsetDateTime.now().plusDays(1);
        List<GestionDeOfertasCaso> casos = List.of(
                new GestionDeOfertasCaso("precio cero", oferta(c, "0.00", "0.00", futuro, "a-" + c.e().turnoId())),
                new GestionDeOfertasCaso(
                        "cargos mayores al precio", oferta(c, "100.00", "100.01", futuro, "b-" + c.e().turnoId())),
                new GestionDeOfertasCaso(
                        "vigencia pasada",
                        oferta(c, "100.00", "0.00", OffsetDateTime.now().minusMinutes(1), "c-" + c.e().turnoId())),
                new GestionDeOfertasCaso(
                        "vigencia mayor al maximo",
                        oferta(c, "100.00", "0.00", OffsetDateTime.now().plusDays(30), "d-" + c.e().turnoId())),
                new GestionDeOfertasCaso(
                        "moneda distinta",
                        new bo.aportaya.entregas.aplicacion.GestionDeOfertas.Entrada(
                                c.e().turnoId(),
                                Dinero.de("100.00", Moneda.USD),
                                Dinero.de("0.00", Moneda.USD),
                                futuro,
                                "e-" + c.e().turnoId())));

        for (var caso : casos) {
            assertThatThrownBy(() -> ofertasCU.publicar(caso.entrada(), c.ctxVendedor()))
                    .as(caso.nombre())
                    .isInstanceOfSatisfying(
                            ErrorDeNegocio.class,
                            e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU130-03"));
        }
        assertThat(contar("SELECT count(*)::int FROM entregas.oferta_turno WHERE turno_id = ?", c.e().turnoId()))
                .isZero();
    }

    private record GestionDeOfertasCaso(
            String nombre, bo.aportaya.entregas.aplicacion.GestionDeOfertas.Entrada entrada) {}

    @Test
    @DisplayName(
            "INVALIDO · GRUPOS CAIDO · Dado que grupos no responde · Cuando se publica o se lista · Entonces AP-CU130-08 (deniega, no inventa un titular)")
    void gruposCaido() {
        var c = caso();
        gruposDoble.seCae(true);

        rechaza(() -> publicar(c), "AP-CU130-08");
        rechaza(() -> ofertasCU.listar(c.ctxComprador(), 10, 0), "AP-CU130-08");
        assertThat(contar("SELECT count(*)::int FROM entregas.oferta_turno WHERE turno_id = ?", c.e().turnoId()))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un comprador miembro del grupo · Cuando lista · Entonces ve monto, precio, cargos y vigencia SIN datos del vendedor; no la ve quien no es miembro, ni el propio vendedor")
    void listadoFiltradoPorPermisos() {
        var c = caso();
        var publicada = publicar(c);
        var forastero = contextoDe(fixtura.usuario());

        var vistas = ofertasCU.listar(c.ctxComprador(), 10, 0);

        assertThat(vistas).hasSize(1);
        var v = vistas.get(0);
        assertThat(v.ofertaId()).isEqualTo(publicada.ofertaId());
        assertThat(v.precio()).isEqualTo(bob("5500.00"));
        assertThat(v.cargos()).isEqualTo(bob("50.00"));
        assertThat(v.derecho()).isEqualTo(bob("6000.00"));
        // El tipo de la vista no tiene donde llevar al vendedor: ni su usuario ni su participante.
        assertThat(java.util.Arrays.stream(v.getClass().getRecordComponents()).map(rc -> rc.getName()))
                .noneMatch(n -> n.contains("vendedor") || n.contains("usuario") || n.contains("participante"));
        gruposDoble.miembro(UUID.randomUUID(), forastero.usuarioId(), UUID.randomUUID(), true);
        assertThat(ofertasCU.listar(forastero, 10, 0)).isEmpty();
        assertThat(ofertasCU.listar(c.ctxVendedor(), 10, 0)).isEmpty();
    }

    @Test
    @DisplayName(
            "Dadas ofertas vencidas, canceladas o reservadas · Cuando se lista · Entonces solo salen las elegibles; y la paginacion es determinista")
    void soloElegiblesYPaginacion() {
        var c1 = caso();
        var c2 = caso();
        // mismo comprador en dos grupos distintos
        gruposDoble.miembro(
                c2.e().grupoId(), c1.comprador().usuarioId(), c1.comprador().participanteId(), true);
        var a = publicar(c1);
        var b = publicar(c2);
        var vencida = caso();
        gruposDoble.miembro(
                vencida.e().grupoId(),
                c1.comprador().usuarioId(),
                c1.comprador().participanteId(),
                true);
        var v = publicar(vencida);
        dslFixtura.execute(
                "UPDATE entregas.oferta_turno SET vigente_hasta = now() - interval '1 minute' WHERE id = ?",
                v.ofertaId());

        var todas = ofertasCU.listar(c1.ctxComprador(), 10, 0);
        var primera = ofertasCU.listar(c1.ctxComprador(), 1, 0);
        var segunda = ofertasCU.listar(c1.ctxComprador(), 1, 1);
        var tercera = ofertasCU.listar(c1.ctxComprador(), 1, 2);

        assertThat(todas).extracting(x -> x.ofertaId()).containsExactlyInAnyOrder(a.ofertaId(), b.ofertaId());
        assertThat(primera).hasSize(1);
        assertThat(segunda).hasSize(1);
        assertThat(tercera).isEmpty();
        assertThat(primera.get(0).ofertaId()).isNotEqualTo(segunda.get(0).ofertaId());
        assertThat(ofertasCU.cancelar(a.ofertaId(), c1.ctxVendedor())).isTrue();
        assertThat(ofertasCU.listar(c1.ctxComprador(), 10, 0))
                .extracting(x -> x.ofertaId())
                .containsExactly(b.ofertaId());
    }

    @Test
    @DisplayName(
            "Dado el dueno de la oferta · Cuando cancela dos veces · Entonces la primera cancela y la segunda es inocua; un tercero recibe AP-CU130-05 sin saber si existe")
    void cancelar() {
        var c = caso();
        var p = publicar(c);
        var tercero = contextoDe(fixtura.usuario());

        rechaza(() -> ofertasCU.cancelar(p.ofertaId(), tercero), "AP-CU130-05");
        rechaza(() -> ofertasCU.cancelar(UUID.randomUUID(), tercero), "AP-CU130-05");
        assertThat(ofertasCU.cancelar(p.ofertaId(), c.ctxVendedor())).isTrue();
        assertThat(ofertasCU.cancelar(p.ofertaId(), c.ctxVendedor())).isFalse();
        assertThat(estadoDeOferta(p.ofertaId())).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName(
            "Dada una oferta vencida y otra con compra en curso · Cuando corre el vencimiento · Entonces vence solo la publicada: una compra en curso no se vence")
    void vencimiento() {
        var vencible = caso();
        var enCurso = caso();
        var a = publicar(vencible);
        var b = publicar(enCurso);
        dslFixtura.execute(
                "UPDATE entregas.oferta_turno SET vigente_hasta = now() - interval '1 minute' WHERE id IN (?, ?)",
                a.ofertaId(),
                b.ofertaId());
        dslFixtura.execute("UPDATE entregas.oferta_turno SET estado = 'RESERVADA' WHERE id = ?", b.ofertaId());

        int vencidas = ofertasCU.vencer(vencible.ctxVendedor());

        assertThat(vencidas).isEqualTo(1);
        assertThat(estadoDeOferta(a.ofertaId())).isEqualTo("VENCIDA");
        assertThat(estadoDeOferta(b.ofertaId())).isEqualTo("RESERVADA");
    }
}

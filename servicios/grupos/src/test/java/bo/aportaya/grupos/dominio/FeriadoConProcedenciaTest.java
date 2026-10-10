package bo.aportaya.grupos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El mecanismo que impide cargar un feriado inventado: sin fuente, sin alcance completo o vencido, no entra. */
class FeriadoConProcedenciaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 9);

    private static void rechazaCon(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ErrorDeNegocio.class, e -> assertThat(
                        e.codigo().valor())
                .isEqualTo(codigo));
    }

    @Test
    @DisplayName("Un feriado con fuente se guarda con la fuente escrita en la descripcion")
    void conFuente() {
        var f = FeriadoConProcedencia.declarar(
                LocalDate.of(2026, 11, 2),
                "Todos Santos",
                AlcanceDeCalendario.NACIONAL,
                Optional.empty(),
                "Ley 2200 de 2001",
                false,
                HOY);

        assertThat(f.paraGuardar()).isEqualTo("Todos Santos | Fuente: Ley 2200 de 2001");
        assertThat(f.paraGuardar().length()).isLessThanOrEqualTo(FeriadoConProcedencia.LARGO_MAXIMO);
    }

    @Test
    @DisplayName("Un feriado sintetico lleva la marca y no se puede confundir con uno oficial")
    void sintetico() {
        var f = FeriadoConProcedencia.declarar(
                LocalDate.of(2026, 11, 3),
                "asueto de simulacro",
                AlcanceDeCalendario.PLATAFORMA,
                Optional.empty(),
                "simulacro O2",
                true,
                HOY);

        assertThat(f.paraGuardar()).startsWith(FeriadoConProcedencia.MARCA_SINTETICA);
    }

    @Test
    @DisplayName("Sin fuente, o con la fuente en blanco, no se carga (AP-CU59-04)")
    void sinFuente() {
        for (String fuente : new String[] {null, "", "   "}) {
            rechazaCon(
                    () -> FeriadoConProcedencia.declarar(
                            LocalDate.of(2026, 11, 2),
                            "Todos Santos",
                            AlcanceDeCalendario.NACIONAL,
                            Optional.empty(),
                            fuente,
                            false,
                            HOY),
                    "AP-CU59-04");
        }
    }

    @Test
    @DisplayName("Un feriado de grupo sin grupo no se carga (AP-CU59-02); con grupo, si")
    void grupoSinGrupo() {
        rechazaCon(
                () -> FeriadoConProcedencia.declarar(
                        LocalDate.of(2026, 11, 2),
                        "asueto del grupo",
                        AlcanceDeCalendario.GRUPO,
                        Optional.empty(),
                        "acta del grupo",
                        false,
                        HOY),
                "AP-CU59-02");

        var conGrupo = FeriadoConProcedencia.declarar(
                LocalDate.of(2026, 11, 2),
                "asueto del grupo",
                AlcanceDeCalendario.GRUPO,
                Optional.of(UUID.randomUUID()),
                "acta del grupo",
                false,
                HOY);
        assertThat(conGrupo.grupoId()).isPresent();
    }

    @Test
    @DisplayName("Una fecha vencida no se carga (AP-CU59-03), pero hoy mismo si")
    void fechaVencida() {
        rechazaCon(
                () -> FeriadoConProcedencia.declarar(
                        HOY.minusDays(1),
                        "ayer",
                        AlcanceDeCalendario.NACIONAL,
                        Optional.empty(),
                        "decreto",
                        false,
                        HOY),
                "AP-CU59-03");

        var hoy = FeriadoConProcedencia.declarar(
                HOY, "hoy", AlcanceDeCalendario.NACIONAL, Optional.empty(), "decreto", false, HOY);
        assertThat(hoy.fecha()).isEqualTo(HOY);
    }

    @Test
    @DisplayName("La descripcion con la fuente no puede pasar del largo de la columna ni quedar en menos de 3 letras")
    void largos() {
        rechazaCon(
                () -> FeriadoConProcedencia.declarar(
                        LocalDate.of(2026, 11, 2),
                        "x".repeat(100),
                        AlcanceDeCalendario.NACIONAL,
                        Optional.empty(),
                        "y".repeat(30),
                        false,
                        HOY),
                "AP-CU59-04");
        rechazaCon(
                () -> FeriadoConProcedencia.declarar(
                        LocalDate.of(2026, 11, 2),
                        "ab",
                        AlcanceDeCalendario.NACIONAL,
                        Optional.empty(),
                        "d",
                        false,
                        HOY),
                "AP-CU59-04");
    }
}

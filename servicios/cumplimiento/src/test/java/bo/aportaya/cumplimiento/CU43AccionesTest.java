package bo.aportaya.cumplimiento;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-43 por HTTP (carril A · H9): generar, aprobar y enviar con el actor de la sesion. */
class CU43AccionesTest extends EscenarioDeAcciones {

    /** Periodos de 2019, uno por prueba: no chocan con los meses recientes que usa CU43Test. */
    private static final AtomicInteger MES = new AtomicInteger(1);

    private String codigo;
    private String periodo;

    @BeforeEach
    void escenario() {
        codigo = "ACC43-" + UUID.randomUUID().toString().substring(0, 6);
        uif.catalogoDeReporte(codigo, "UIF", 15);
        periodo = "2019-%02d".formatted(MES.getAndIncrement());
    }

    private String constancia() {
        return "C-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName(
            "Dado un periodo sin reporte · Cuando el oficial lo genera desde el backoffice dos veces · Entonces el segundo pedido devuelve el mismo reporte marcado como ya existente")
    void generarDosVeces() {
        var ctx = operador(fixtura.usuario());

        var primero = transaccion.execute(t -> acciones().generarReporte(periodo, codigo, "PCC-01", ctx));
        var segundo = transaccion.execute(t -> acciones().generarReporte(periodo, codigo, "PCC-01", ctx));

        assertThat(primero.esNuevo()).isTrue();
        assertThat(segundo.esNuevo()).isFalse();
        assertThat(segundo.salida().reporteId()).isEqualTo(primero.salida().reporteId());
        assertThat(primero.salida().reporteEnCero()).isTrue();
    }

    @Test
    @DisplayName(
            "Dado un reporte generado por otra persona · Cuando el oficial lo aprueba y envía desde el backoffice · Entonces el aprobador registrado es quien lo pidió, nunca un id enviado por el cliente")
    void aprobadorDeLaSesion() {
        var generador = operador(fixtura.usuario());
        var aprobador = operador(fixtura.usuario());
        var reporte = transaccion.execute(t -> acciones().generarReporte(periodo, codigo, "PCC-01", generador));

        var envio = transaccion.execute(
                t -> acciones().aprobarYEnviar(periodo, codigo, "PORTAL_WEB", constancia(), true, 0, aprobador));

        assertThat(envio.esNuevo()).isTrue();
        assertThat(dsl.fetchOne(
                                "SELECT aprobado_por FROM cumplimiento.reporte_regulatorio WHERE id = ?",
                                reporte.salida().reporteId())
                        .get(0, UUID.class))
                .isEqualTo(aprobador.usuarioId());
    }

    @Test
    @DisplayName(
            "reintento: el mismo aprobador enviando otra vez recibe el envio que ya hay y no se registra un segundo")
    void reintentoDelEnvio() {
        var generador = operador(fixtura.usuario());
        var aprobador = operador(fixtura.usuario());
        var reporte = transaccion.execute(t -> acciones().generarReporte(periodo, codigo, "PCC-01", generador));
        transaccion.execute(
                t -> acciones().aprobarYEnviar(periodo, codigo, "PORTAL_WEB", constancia(), true, 0, aprobador));

        var otra = transaccion.execute(
                t -> acciones().aprobarYEnviar(periodo, codigo, "PORTAL_WEB", constancia(), true, 0, aprobador));

        assertThat(otra.esNuevo()).isFalse();
        assertThat(otra.salida().reporteId()).isEqualTo(reporte.salida().reporteId());
        assertThat(contar(
                        "SELECT count(*)::int FROM cumplimiento.envio_regulatorio WHERE reporte_regulatorio_id = ?",
                        reporte.salida().reporteId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza que quien genero el reporte lo apruebe, con AP-CU43-02 (R-SEG-04)")
    void rechazaAutoaprobacion() {
        var generador = operador(fixtura.usuario());
        transaccion.execute(t -> acciones().generarReporte(periodo, codigo, "PCC-01", generador));

        rechazaCon(
                "AP-CU43-02",
                () -> transaccion.execute(t ->
                        acciones().aprobarYEnviar(periodo, codigo, "PORTAL_WEB", constancia(), true, 0, generador)));
    }

    @Test
    @DisplayName("rechaza con AP-CU43-03 un segundo aprobador sobre un reporte ya enviado")
    void rechazaSegundoAprobador() {
        var generador = operador(fixtura.usuario());
        transaccion.execute(t -> acciones().generarReporte(periodo, codigo, "PCC-01", generador));
        transaccion.execute(t -> acciones()
                .aprobarYEnviar(periodo, codigo, "PORTAL_WEB", constancia(), true, 0, operador(fixtura.usuario())));

        rechazaCon(
                "AP-CU43-03",
                () -> transaccion.execute(t -> acciones()
                        .aprobarYEnviar(
                                periodo, codigo, "PORTAL_WEB", constancia(), true, 0, operador(fixtura.usuario()))));
    }

    @Test
    @DisplayName("rechaza con AP-CU43-01 un catalogo que no existe")
    void rechazaCatalogoInexistente() {
        var ctx = operador(fixtura.usuario());
        rechazaCon(
                "AP-CU43-01",
                () -> transaccion.execute(
                        t -> acciones().generarReporte(periodo, "NO-EXISTE-" + UUID.randomUUID(), "PCC-01", ctx)));
    }
}

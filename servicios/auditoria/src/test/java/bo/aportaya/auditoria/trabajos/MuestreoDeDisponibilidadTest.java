package bo.aportaya.auditoria.trabajos;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.aplicacion.RegistroDeDisponibilidad;
import bo.aportaya.auditoria.dominio.SaludDeServicio;
import bo.aportaya.auditoria.dominio.SaludDeServicio.Estado;
import bo.aportaya.auditoria.infraestructura.SondaDeSalud;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** sistemas/servicios · el disparador pasa lo que contesto la sonda, con la hora tomada ANTES de preguntar. */
class MuestreoDeDisponibilidadTest {

    @Test
    @DisplayName("muestreo · registra lo que contesto la sonda con la hora de antes de la consulta")
    void registraLoDeLaSonda() {
        Instant[] ahora = {Instant.parse("2026-10-29T01:02:03Z")};
        var contestado = List.of(
                new SaludDeServicio("identidad", Estado.ARRIBA, Optional.of(4L)),
                new SaludDeServicio("erp", Estado.CAIDO, Optional.empty()));
        SondaDeSalud sonda = new SondaDeSalud("", "http://%s", Duration.ofSeconds(1)) {
            @Override
            public List<SaludDeServicio> consultar() {
                ahora[0] = ahora[0].plusSeconds(2); // la consulta tarda: el minuto medido no cambia
                return contestado;
            }
        };
        List<Object[]> registrados = new ArrayList<>();
        var registro = new RegistroDeDisponibilidad(null, null) {
            @Override
            public int registrar(List<SaludDeServicio> salud, Instant medidoEn) {
                registrados.add(new Object[] {salud, medidoEn});
                return salud.size();
            }
        };

        new MuestreoDeDisponibilidad(sonda, registro, () -> ahora[0]).correr();

        assertThat(registrados).hasSize(1);
        assertThat(registrados.get(0)[0]).isEqualTo(contestado);
        assertThat(registrados.get(0)[1]).isEqualTo(Instant.parse("2026-10-29T01:02:03Z"));
    }
}

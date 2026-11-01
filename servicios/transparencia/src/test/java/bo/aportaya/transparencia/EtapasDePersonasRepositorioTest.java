package bo.aportaya.transparencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.transparencia.aplicacion.ConsultarEtapasDePersonas;
import bo.aportaya.transparencia.infraestructura.EtapaRepositorio;
import bo.aportaya.transparencia.infraestructura.ModeloRepositorio;
import bo.aportaya.transparencia.infraestructura.ReputacionRepositorio;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La etapa de otras personas, sin su puntaje, como la pide grupos (2026-10-30) contra PostgreSQL
 * real. Correcto (la etapa de otro con el rol real), limite (varias, repetidas, sin historial y 60)
 * e invalido (ninguna o mas de 60). Datos sinteticos.
 */
class EtapasDePersonasRepositorioTest extends BaseDeEtapas {

    private ConsultarEtapasDePersonas etapasDePersonas;

    @BeforeEach
    void preparar() {
        fixtura.catalogoDeReputacion();
        etapasDePersonas = new ConsultarEtapasDePersonas(
                new Datos(dsl),
                new ReputacionRepositorio(),
                new ModeloRepositorio(),
                new EtapaRepositorio(),
                Reloj.delSistema(),
                MAXIMO_DE_PERSONAS);
    }

    /**
     * Como la llama grupos en produccion: rol de base {@code svc_transparencia} y la sesion de la
     * identidad de servicio {@code SERVICIO_GRUPOS} (no una persona).
     */
    private List<ConsultarEtapasDePersonas.EtapaDePersona> etapasComoServicio(List<UUID> usuarios) {
        return transaccion.execute(s -> {
            dsl.execute("SET LOCAL ROLE svc_transparencia");
            return etapasDePersonas.ejecutar(
                    usuarios,
                    ContextoSesion.de(
                            UUID.randomUUID(),
                            "SERVICIO_GRUPOS",
                            new Traza(UUID.randomUUID().toString())));
        });
    }

    @Test
    @DisplayName(
            "etapa · el servicio de grupos lee la etapa de otra persona con el rol real (420 → RECUPERACION), sin puntaje")
    void servicioLeeLaEtapaDeOtro() {
        // Es el uso de grupos: decide un ingreso con la etapa de OTRA persona. Bajo la politica de
        // fila (denegar por omision) esa fila no se ve con el rol del servicio, y sin leerla como
        // sistema alguien en recuperacion pasaba por Etapa 1 · Inicial, la del puntaje base.
        UUID postulante = fixtura.usuario();
        puntaje(postulante, "420.00", fixtura.modeloVigente(), "v1");

        var etapas = etapasComoServicio(List.of(postulante));

        assertThat(etapas).hasSize(1);
        assertThat(etapas.get(0).usuarioId()).isEqualTo(postulante);
        assertThat(etapas.get(0).tieneHistorial()).isTrue();
        assertThat(etapas.get(0).etapa().codigo()).isEqualTo("RECUPERACION");
        assertThat(etapas.get(0).etapa().nombre()).isEqualTo("En recuperación");
        assertThat(etapas.get(0).etapa().desde()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName(
            "etapa · varias personas: sin historial es la del puntaje base, el repetido sale una vez y se respeta el orden")
    void servicioVariasPersonas() {
        UUID ana = fixtura.usuario();
        UUID beto = fixtura.usuario();
        UUID nueva = fixtura.usuario();
        UUID sinCuenta = UUID.randomUUID();
        puntaje(ana, "812.50", fixtura.modeloVigente(), "v1");
        puntaje(beto, "550.00", fixtura.modeloVigente(), "v1");

        var etapas = etapasComoServicio(List.of(beto, nueva, ana, beto, sinCuenta));

        assertThat(etapas)
                .extracting(ConsultarEtapasDePersonas.EtapaDePersona::usuarioId)
                .containsExactly(beto, nueva, ana, sinCuenta);
        assertThat(etapas)
                .extracting(e -> e.etapa().codigo())
                .containsExactly("EN_MARCHA", "INICIAL", "SOLIDO", "INICIAL");
        assertThat(etapas)
                .extracting(ConsultarEtapasDePersonas.EtapaDePersona::tieneHistorial)
                .containsExactly(true, false, true, false);
    }

    @Test
    @DisplayName("etapa · inválido: ninguna persona o más de 60 distintas se rechaza antes de leer")
    void servicioFueraDelContrato() {
        var sesentaYUna =
                java.util.stream.Stream.generate(UUID::randomUUID).limit(61).toList();
        var sesenta =
                java.util.stream.Stream.generate(UUID::randomUUID).limit(60).toList();

        assertThatThrownBy(() -> etapasComoServicio(List.of()))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThatThrownBy(() -> etapasComoServicio(sesentaYUna))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThat(etapasComoServicio(sesenta)).as("60 es el borde de arriba").hasSize(60);
    }
}

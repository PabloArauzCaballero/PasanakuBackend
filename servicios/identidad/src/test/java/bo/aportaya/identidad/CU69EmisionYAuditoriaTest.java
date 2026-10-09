package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.aplicacion.EmitirTokenDeInvitacion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * CU-69 · límite de emisión y regla de reemisión del token de invitación.
 *
 * <p>Amenazas: abuso de emisión (spam por un emisor, también simultáneo) y enlaces viejos vivos tras
 * reemitir sin una regla explícita.
 */
class CU69EmisionYAuditoriaTest extends BaseDeEmisionDeInvitacion {

    @Test
    void superadoElTopeDiarioNoSeEmiteMasPeroElReintentoRecuperaElEnlace() {
        var primeras = new java.util.ArrayList<EmitirTokenDeInvitacion.Emitido>();
        var claves = new java.util.ArrayList<UUID>();
        for (int i = 0; i < TOPE_DIARIO; i++) {
            claves.add(UUID.randomUUID());
            primeras.add(emitir(claves.get(i), UUID.randomUUID(), telefono));
        }
        assertThatThrownBy(() -> emitir(UUID.randomUUID(), UUID.randomUUID(), telefono))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU69-08"));
        // Un reintento idéntico NO cuenta como emisión nueva: devuelve el mismo enlace.
        assertThat(emitirMismo(claves.get(0), primeras.get(0))).isEqualTo(primeras.get(0));
        // El tope es por emisor: otra persona emite normalmente.
        var otro = fixtura.usuario(
                "+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)));
        assertThat(tx.<EmitirTokenDeInvitacion.Emitido>execute(
                        s -> emitir.ejecutar(entrada(UUID.randomUUID(), UUID.randomUUID(), telefono), ctx(otro))))
                .isNotNull();
        // Pasadas 24 horas la ventana se libera.
        AHORA.set(AHORA.get().plusSeconds(86400L + 1));
        assertThat(emitir(UUID.randomUUID(), UUID.randomUUID(), telefono)).isNotNull();
    }

    @Test
    void emisionesSimultaneasDelMismoEmisorNoSuperanElTope() throws Exception {
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(TOPE_DIARIO + 3)) {
            var inicio = new java.util.concurrent.CountDownLatch(1);
            var resultados = new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < TOPE_DIARIO + 3; i++) {
                resultados.add(pool.submit(() -> {
                    inicio.await();
                    try {
                        emitir(UUID.randomUUID(), UUID.randomUUID(), telefono);
                        return true;
                    } catch (ErrorDeNegocio rechazada) {
                        return false;
                    }
                }));
            }
            inicio.countDown();
            int emitidas = 0;
            for (var r : resultados) if (r.get(30, java.util.concurrent.TimeUnit.SECONDS)) emitidas++;
            assertThat(emitidas).isEqualTo(TOPE_DIARIO);
        }
    }

    @Test
    void reemisionConLaPoliticaActualConservaElEnlaceAnteriorPorReglaExplicita() {
        var uno = emitir(UUID.randomUUID(), grupo, telefono);
        var dos = emitir(UUID.randomUUID(), grupo, telefono);
        assertThat(dos.tokenId()).isNotEqualTo(uno.tokenId());
        // invalida_anteriores = false: ambos siguen vivos; no se revoca nada por accidente.
        assertThat(estado(uno)).isEqualTo("EMITIDO");
        assertThat(estado(dos)).isEqualTo("EMITIDO");
    }

    @Test
    void reemisionConPoliticaQueInvalidaRevocaSoloLasVivasDeEseGrupoYTelefono() {
        var otroGrupo = emitir(UUID.randomUUID(), UUID.randomUUID(), telefono);
        String otroTelefono =
                "+591" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000));
        var otroDestino = emitir(UUID.randomUUID(), grupo, otroTelefono);
        var vieja = emitir(UUID.randomUUID(), grupo, telefono);
        AHORA.set(BASE.plusSeconds(86400L * 401));
        var nueva = emitir(UUID.randomUUID(), grupo, telefono);
        assertThat(estado(vieja)).isEqualTo("INVALIDADO");
        assertThat(motivo(vieja)).isEqualTo("REEMITIDA_POR_NUEVA_EMISION");
        assertThat(estado(nueva)).isEqualTo("EMITIDO");
        assertThat(estado(otroGrupo)).isEqualTo("EMITIDO");
        assertThat(estado(otroDestino)).isEqualTo("EMITIDO");
        // La anterior ya no se puede canjear; la nueva sí.
        assertThat(canjear(vieja, grupo, destinatario)).isEmpty();
        assertThat(canjear(nueva, grupo, destinatario)).isPresent();
    }
}

package bo.aportaya.transparencia.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.transparencia.aplicacion.CU75EmitirCertificado.EntradaCertificado;
import bo.aportaya.transparencia.dominio.puertos.VerificacionDeIdentidad;
import bo.aportaya.transparencia.infraestructura.CadenaRepositorio;
import bo.aportaya.transparencia.infraestructura.CertificadoRepositorio;
import bo.aportaya.transparencia.infraestructura.SnapshotRepositorio;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-75 · F1.L5.M21: lo que se firma sale de la foto del titular, y la identidad la verifica {@code identidad}.
 *
 * <p>Antes, el cuerpo traia {@code disponibles} (los valores) e {@code identidadVerificada} (un booleano), y el
 * servidor firmaba con su clave lo que le mandaban. Doble de {@code identidad} en tres niveles: verificada, el limite
 * de una foto que no es del titular, y no verificada / sin respuesta (falla cerrado). La base es un doble.
 */
class CertificadoConDatosDelServidorTest {

    private static final UUID TITULAR = UUID.fromString("e7500000-0000-4000-8000-000000000001");
    private static final UUID FOTO = UUID.fromString("e7500000-0000-4000-8000-000000000002");
    private static final Map<String, String> REALES = Map.of("puntaje", "512.00", "nivel", "EN_CRECIMIENTO");

    private SnapshotRepositorio snapshots;
    private CertificadoRepositorio certificados;
    private boolean identidadVerificada;
    private CU75EmitirCertificado cu75;
    private final ContextoSesion ctx = ContextoSesion.de(
            TITULAR, "PARTICIPANTE", new Traza(UUID.randomUUID().toString()));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void armar() {
        var datos = mock(Datos.class);
        when(datos.conContexto(any(), any()))
                .thenAnswer(i -> ((Function<Object, Object>) i.getArgument(1)).apply(null));
        snapshots = mock(SnapshotRepositorio.class);
        certificados = mock(CertificadoRepositorio.class);
        when(certificados.certificadoPorSnapshot(any(), any())).thenReturn(Optional.empty());
        when(certificados.emitirCertificado(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());
        when(snapshots.disponiblesDelTitular(any(), eq(FOTO), eq(TITULAR))).thenReturn(Optional.of(REALES));
        identidadVerificada = true;
        VerificacionDeIdentidad identidad = usuario -> identidadVerificada;
        cu75 = new CU75EmitirCertificado(
                datos,
                certificados,
                mock(CadenaRepositorio.class),
                mock(Outbox.class),
                Reloj.fijo(Instant.parse("2026-11-21T12:00:00Z")),
                new SecureRandom(),
                snapshots,
                identidad,
                "clave-de-firma-solo-para-pruebas",
                "https://ejemplo.invalid");
    }

    @Test
    @DisplayName("Correcto · identidad verificada: el contenido firmado son los valores REALES de la foto")
    void elContenidoSaleDeLaFoto() {
        var salida = cu75.emitir(new EntradaCertificado(TITULAR, FOTO, Set.of("puntaje", "nivel"), 90), ctx);

        assertThat(salida.contenido()).containsExactlyInAnyOrderEntriesOf(REALES);
    }

    @Test
    @DisplayName("Límite · una foto que no es del titular es «no hay foto»: no se firma nada")
    void fotoAjenaNoSeFirma() {
        UUID ajena = UUID.randomUUID();
        when(snapshots.disponiblesDelTitular(any(), eq(ajena), eq(TITULAR))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cu75.emitir(new EntradaCertificado(TITULAR, ajena, Set.of("puntaje"), 90), ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("foto de reputacion");
        verifyNoInteractions(certificados);
    }

    @Test
    @DisplayName("Inválido · identidad no verificada (o identidad no respondió): no se emite")
    void sinIdentidadNoSeEmite() {
        identidadVerificada = false;

        assertThatThrownBy(() -> cu75.emitir(new EntradaCertificado(TITULAR, FOTO, Set.of("puntaje"), 90), ctx))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("verificacion de identidad");
        verifyNoInteractions(snapshots, certificados);
    }
}

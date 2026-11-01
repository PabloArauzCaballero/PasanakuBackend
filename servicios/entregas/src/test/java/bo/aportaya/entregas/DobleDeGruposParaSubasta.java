package bo.aportaya.entregas;

import bo.aportaya.entregas.dominio.puertos.GruposParaSubasta;
import bo.aportaya.plataforma.dominio.Dinero;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DOBLE declarado de {@code grupos} para la subasta (regla 65): cumple el contrato de {@code grupos.yaml}
 * (subastabilidad, elegibilidad-para-subasta, traspasos-por-subasta). Lo verificado contra el es {@code entregas}; el
 * contrato real de {@code grupos} se prueba en {@code CU64SubastaTest} de grupos.
 */
final class DobleDeGruposParaSubasta implements GruposParaSubasta {

    private final Map<UUID, Cupo> cupos = new HashMap<>();
    private final Map<UUID, String> noElegibles = new HashMap<>();
    private final Set<UUID> traspasadas = new HashSet<>();
    private boolean caido;
    private boolean rechazaTraspaso;
    private UUID usuarioDeLaSesion;
    private final Map<UUID, UUID> titulares = new HashMap<>();
    final AtomicInteger traspasosNuevos = new AtomicInteger();

    synchronized void reiniciar() {
        cupos.clear();
        noElegibles.clear();
        traspasadas.clear();
        titulares.clear();
        caido = false;
        rechazaTraspaso = false;
        traspasosNuevos.set(0);
    }

    synchronized void cupo(
            UUID cupoId, UUID titularUsuario, UUID participanteId, Dinero aporte, Optional<String> motivo) {
        cupos.put(cupoId, new Cupo(participanteId, motivo.isEmpty(), motivo, aporte, "Pasanaku de prueba", "MENSUAL"));
        titulares.put(cupoId, titularUsuario);
    }

    synchronized void noElegible(UUID usuario, String motivo) {
        noElegibles.put(usuario, motivo);
    }

    synchronized void seCae(boolean caido) {
        this.caido = caido;
    }

    synchronized void rechazaElTraspaso(boolean rechaza) {
        this.rechazaTraspaso = rechaza;
    }

    synchronized boolean fueTraspasada(UUID subastaId) {
        return traspasadas.contains(subastaId);
    }

    /** El doble no ve el token: quien llama le dice de quien es la sesion (como lo haria el token propagado). */
    synchronized void sesionDe(UUID usuario) {
        this.usuarioDeLaSesion = usuario;
    }

    @Override
    public synchronized Subastabilidad subastabilidad(UUID grupoId, UUID cupoId) {
        if (caido) {
            return new Subastabilidad(Respuesta.NO_DISPONIBLE, Optional.empty());
        }
        var cupo = cupos.get(cupoId);
        if (cupo == null) {
            return new Subastabilidad(Respuesta.NO_EXISTE, Optional.empty());
        }
        if (!titulares.get(cupoId).equals(usuarioDeLaSesion)) {
            return new Subastabilidad(Respuesta.NO_ES_TITULAR, Optional.empty());
        }
        return new Subastabilidad(Respuesta.OK, Optional.of(cupo));
    }

    @Override
    public synchronized Elegibilidad elegibilidad(UUID grupoId) {
        if (caido) {
            return new Elegibilidad(Respuesta.NO_DISPONIBLE, false, Optional.empty());
        }
        var motivo = Optional.ofNullable(noElegibles.get(usuarioDeLaSesion));
        return new Elegibilidad(Respuesta.OK, motivo.isEmpty(), motivo);
    }

    @Override
    public synchronized Traspaso traspasar(
            UUID grupoId, UUID cupoId, UUID subastaId, UUID ganadorUsuarioId, UUID salienteParticipanteId) {
        if (caido) {
            return new Traspaso(Respuesta.NO_DISPONIBLE, Optional.empty());
        }
        if (rechazaTraspaso) {
            return new Traspaso(Respuesta.RECHAZADO, Optional.empty());
        }
        if (traspasadas.add(subastaId)) {
            traspasosNuevos.incrementAndGet();
        }
        return new Traspaso(
                Respuesta.OK,
                Optional.of(UUID.nameUUIDFromBytes(subastaId.toString().getBytes())));
    }
}

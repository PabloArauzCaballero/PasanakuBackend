package bo.aportaya.transparencia.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.transparencia.dominio.EtapasDeReputacion;
import bo.aportaya.transparencia.infraestructura.EtapaRepositorio;
import bo.aportaya.transparencia.infraestructura.ModeloRepositorio;
import bo.aportaya.transparencia.infraestructura.ReputacionRepositorio;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La etapa de una o varias personas, sin su puntaje: lo que {@code grupos} necesita de la
 * reputacion de OTRA persona (privacidad del puntaje, decision del dueño 2026-10-30, regla 90.2.8).
 *
 * <p>Quien llega hasta aca lo decide la ruta: solo la identidad de servicio de grupos
 * ({@code @Permiso("SERVICIO_GRUPOS")}). Lo que sale es la etapa —codigo, nombre, orden y el
 * {@code desde} del tramo—; nunca el puntaje ni el avance dentro del tramo, que juntos dirian el
 * puntaje con dos decimales.
 *
 * <p>Sin historial NO es cero: {@code tieneHistorial=false} y la etapa del puntaje BASE del modelo
 * vigente, como en {@link ConsultarPuntaje}. Una persona sin cuenta en este servicio viaja igual:
 * transparencia no sabe quien existe en identidad, y no tiene por que.
 *
 * <p>Cada identificador repetido se responde una vez, en el orden en que aparecio por primera vez.
 * El tope de 60 lo hace cumplir el contrato en la ruta; aca se vuelve a exigir, porque esta clase
 * no sabe quien la llama.
 */
@Service
public class ConsultarEtapasDePersonas {

    /** {@code maxItems} del contrato ({@code transparencia.yaml}, {@code consultarEtapasDePersonas}). */
    static final int MAXIMO_DE_PERSONAS = 60;

    private final Datos datos;
    private final ReputacionRepositorio reputaciones;
    private final ModeloRepositorio modelos;
    private final EtapaRepositorio etapas;
    private final Reloj reloj;

    public ConsultarEtapasDePersonas(
            Datos datos,
            ReputacionRepositorio reputaciones,
            ModeloRepositorio modelos,
            EtapaRepositorio etapas,
            Reloj reloj) {
        this.datos = datos;
        this.reputaciones = reputaciones;
        this.modelos = modelos;
        this.etapas = etapas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<EtapaDePersona> ejecutar(List<UUID> usuarios, ContextoSesion ctx) {
        var unicos = new LinkedHashSet<>(usuarios);
        if (unicos.isEmpty() || unicos.size() > MAXIMO_DE_PERSONAS || unicos.contains(null)) {
            // ConstraintViolationException y no IllegalArgumentException: sale 400 por el manejador
            // global, como la misma violacion detectada en la ruta (regla 96.2.4: nunca un 500).
            throw new ConstraintViolationException(
                    "usuarioId: entre 1 y " + MAXIMO_DE_PERSONAS + " personas distintas", Set.of());
        }
        return datos.conContexto(ctx, dsl -> {
            // La del puntaje base se calcula una sola vez: es la misma para todos los que no tienen historial.
            Optional<EtapasDeReputacion.Ubicacion> base = Optional.empty();
            boolean baseCalculada = false;
            var salida = new ArrayList<EtapaDePersona>(unicos.size());
            for (UUID usuario : unicos) {
                var puntaje = ConsultarPuntaje.puntajeVisible(dsl, reputaciones, usuario);
                if (puntaje.isPresent()) {
                    var p = puntaje.get();
                    var etapa = ConsultarPuntaje.etapaConHistorial(dsl, etapas, p.modeloVersion(), p.puntaje());
                    salida.add(new EtapaDePersona(
                            usuario,
                            true,
                            etapa.map(EtapasDeReputacion.Ubicacion::etapa).orElse(null)));
                } else {
                    if (!baseCalculada) {
                        base = ConsultarPuntaje.etapaSinHistorial(dsl, etapas, modelos, reloj);
                        baseCalculada = true;
                    }
                    salida.add(new EtapaDePersona(
                            usuario,
                            false,
                            base.map(EtapasDeReputacion.Ubicacion::etapa).orElse(null)));
                }
            }
            return salida;
        });
    }

    /**
     * @param etapa la del puntaje (o la del puntaje base sin historial); nula solo si no hay
     *     catalogo de etapas, y entonces quien llama falla cerrado (regla 98.2.4)
     */
    public record EtapaDePersona(UUID usuarioId, boolean tieneHistorial, EtapasDeReputacion.Etapa etapa) {}
}

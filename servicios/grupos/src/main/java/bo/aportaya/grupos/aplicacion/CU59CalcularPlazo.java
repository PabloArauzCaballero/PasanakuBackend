package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.dominio.AlcanceDeCalendario;
import bo.aportaya.grupos.dominio.CalendarioVacio;
import bo.aportaya.grupos.dominio.VencimientoLocal;
import bo.aportaya.grupos.infraestructura.CalendarioRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CalendarioHabil;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.PlazoHabil;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-59 · Calcular una fecha límite en días hábiles.
 *
 * <p>Devuelve tambien **que dias se saltearon**, porque un plazo que no se puede
 * explicar no se puede defender: ni ante el cliente que pregunta por que su fecha es
 * esa, ni ante el supervisor que pregunta lo mismo seis meses despues.
 *
 * <p>El resultado se guarda donde se use, y **no se recalcula**. Si se declara un
 * feriado despues, ese plazo no se mueve: mover un plazo guardado seria reescribir el
 * pasado, y para el cliente seria peor — le cambiamos la fecha que ya le habiamos dicho.
 */
@Service
public class CU59CalcularPlazo {

    private final Datos datos;
    private final CalendarioRepositorio calendario;

    public CU59CalcularPlazo(Datos datos, CalendarioRepositorio calendario) {
        this.datos = datos;
        this.calendario = calendario;
    }

    @Transactional
    public SalidaPlazo ejecutar(EntradaPlazo entrada, ContextoSesion ctx) {
        AlcanceDeCalendario alcance = AlcanceDeCalendario.valueOf(entrada.alcance());
        if (!alcance.completoCon(entrada.referenciaId())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(59, 2), "Un feriado de grupo necesita decir de que grupo se trata.");
        }

        return datos.conContexto(ctx, dsl -> {
            if (!calendario.hayCalendarioPara(dsl, entrada.desde().getYear())) {
                throw new CalendarioVacio(entrada.desde().getYear());
            }

            // Se pide una ventana generosa: con el peor calendario imaginable, `dias`
            // habiles nunca caen mas alla del triple en dias corridos.
            LocalDate hasta = entrada.desde().plusDays(entrada.dias() * 3L + 14);
            Map<LocalDate, String> noHabiles =
                    calendario.noHabilesEntre(dsl, entrada.desde(), hasta, alcance, entrada.referenciaId());

            CalendarioHabil habil = VencimientoLocal.calendario(noHabiles);

            LocalDate limite = PlazoHabil.sumar(entrada.desde(), entrada.dias(), habil);
            List<DiaSalteado> salteados = noHabiles.entrySet().stream()
                    .filter(dia -> !dia.getKey().isBefore(entrada.desde())
                            && !dia.getKey().isAfter(limite))
                    .map(dia -> new DiaSalteado(dia.getKey(), dia.getValue()))
                    .toList();

            return new SalidaPlazo(limite, salteados);
        });
    }

    /**
     * Corre al siguiente habil. A favor del cliente: nunca al anterior.
     *
     * <p>Habil es lo mismo que en {@link #ejecutar}: ni feriado del calendario ni sabado ni domingo.
     * Antes solo miraba el calendario, y un feriado de viernes se corria a un sabado.
     */
    @Transactional
    public LocalDate correrSiCae(LocalDate vencimiento, AlcanceDeCalendario alcance, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            Map<LocalDate, String> noHabiles = calendario.noHabilesEntre(
                    dsl,
                    vencimiento,
                    vencimiento.plusDays(VencimientoLocal.VENTANA_DE_BUSQUEDA_EN_DIAS),
                    alcance,
                    Optional.empty());
            return PlazoHabil.siguienteHabil(vencimiento, VencimientoLocal.calendario(noHabiles));
        });
    }

    /**
     * H7.S1.M3 · el vencimiento con sus tres fechas explicitas (UTC, contractual, de liquidacion).
     *
     * <p>El calendario hace falta para el anio de la fecha contractual y, si la liquidacion cruza de
     * anio, tambien para el siguiente: sin calendario no se calcula (CALENDARIO_VACIO), nunca se
     * cuenta corrido por omision. Los feriados salen de {@code catalogo.dia_no_habil} tal como estan
     * sembrados: aca no se inventa ninguno.
     */
    @Transactional(readOnly = true)
    public SalidaVencimiento vencimientoLocal(EntradaVencimiento entrada, ContextoSesion ctx) {
        AlcanceDeCalendario alcance = AlcanceDeCalendario.valueOf(entrada.alcance());
        if (!alcance.completoCon(entrada.referenciaId())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(59, 2), "Un feriado de grupo necesita decir de que grupo se trata.");
        }
        ZoneId zona = zonaDe(entrada.zona());
        LocalDate contractual = VencimientoLocal.diaEn(entrada.instante(), zona);

        return datos.conContexto(ctx, dsl -> {
            if (!calendario.hayCalendarioPara(dsl, contractual.getYear())) {
                throw new CalendarioVacio(contractual.getYear());
            }
            Map<LocalDate, String> noHabiles = calendario.noHabilesEntre(
                    dsl,
                    contractual,
                    contractual.plusDays(VencimientoLocal.VENTANA_DE_BUSQUEDA_EN_DIAS),
                    alcance,
                    entrada.referenciaId());
            var vencimiento = VencimientoLocal.de(entrada.instante(), zona, noHabiles);
            int anioDeLiquidacion = vencimiento.fechaDeLiquidacion().getYear();
            if (anioDeLiquidacion != contractual.getYear() && !calendario.hayCalendarioPara(dsl, anioDeLiquidacion)) {
                throw new CalendarioVacio(anioDeLiquidacion);
            }
            return new SalidaVencimiento(
                    vencimiento.instante(),
                    zona.getId(),
                    vencimiento.fechaEnUtc(),
                    vencimiento.fechaContractual(),
                    vencimiento.fechaDeLiquidacion(),
                    vencimiento.corridaPorNoHabil(),
                    vencimiento.diasSalteados().stream()
                            .map(d -> new DiaSalteado(d.fecha(), d.motivo()))
                            .toList());
        });
    }

    /** La zona del contrato. Sin zona pedida, la de la plataforma; una zona que no existe se rechaza. */
    static ZoneId zonaDe(Optional<String> pedida) {
        if (pedida.isEmpty() || pedida.get().isBlank()) {
            return bo.aportaya.plataforma.dominio.Reloj.ZONA;
        }
        try {
            return ZoneId.of(pedida.get().strip());
        } catch (DateTimeException e) {
            throw new ErrorDeNegocio(
                    CodigoError.de(59, 6), "La zona horaria no existe: use un identificador IANA como America/La_Paz.");
        }
    }

    public record EntradaPlazo(LocalDate desde, int dias, String alcance, Optional<UUID> referenciaId) {}

    public record SalidaPlazo(LocalDate fechaLimite, List<DiaSalteado> diasSalteados) {}

    public record EntradaVencimiento(
            Instant instante, String alcance, Optional<UUID> referenciaId, Optional<String> zona) {}

    /** Las tres fechas, la zona que las separa y por que la de liquidacion se corrio (si se corrio). */
    public record SalidaVencimiento(
            Instant instanteUtc,
            String zonaHoraria,
            LocalDate fechaEnUtc,
            LocalDate fechaContractual,
            LocalDate fechaDeLiquidacion,
            boolean corridaPorNoHabil,
            List<DiaSalteado> diasSalteados) {}

    public record DiaSalteado(LocalDate fecha, String descripcion) {}
}

package bo.aportaya.aportes.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-21 · La cuota 1 nace cuando el organizador inicia el grupo (R-GRP-20, decision del dueño 2026-10-30).
 *
 * <p>Es la mitad de {@code aportes} del evento {@code grupos.grupo_iniciado}: con el grupo recien CONFORMADO y el
 * periodo 1 todavia PROGRAMADO, se abre la obligacion {@code APORTE_PERIODICO} de cada cupo ocupado, para que el
 * sorteo pueda exigir que todas esten pagadas antes de comprometerse (CU-60, AP-CU60-11). El evento trae todo lo que
 * hace falta —periodo, cupos y montos— porque un consumidor no tiene el token de nadie para preguntarle a {@code grupos}
 * (invariante 11: este servicio no lee su esquema).
 *
 * <p>Reutiliza {@link RegistroDelCalendario}, el mismo registro que CU-21 usa al abrir un periodo: feriados, fechas de
 * liquidacion y {@code ON CONFLICT DO NOTHING} sobre R-GRP-03. Idempotente dos veces: por {@code event_id} (la marca de
 * {@link Consumidos} va en la misma transaccion que las obligaciones) y, si {@code grupos} re-emitiera el inicio con otro
 * id, por la unicidad periodo + cupo de la obligacion, que deja cero nuevas y no emite nada.
 *
 * <p>Una carga que no cierra (sin cupos, un monto que no es positivo, un periodo que no es el 1) se RECHAZA con excepcion
 * antes de tocar la base y no se marca consumida: abrir cuotas a medias es peor que reintentar.
 */
@Service
public class CU21AbrirPrimeraCuota {

    /** Quien consume, para la marca de eventos ya procesados. */
    public static final String CONSUMIDOR = "aportes.grupo-iniciado";

    /** El identificador de proceso del actor Sistema; no es una persona ni una fila de usuario. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-00000000a5e3");

    /** El estado del periodo 1 al iniciar el grupo: lo crea asi {@code CalendarioDelGrupoRepositorio} en grupos. */
    private static final String PERIODO_AL_INICIAR = "PROGRAMADO";

    public enum Resultado {
        ABIERTA,
        DUPLICADO
    }

    /** Lo que {@code grupos.grupo_iniciado} trae, ya con tipos: el borde con Kafka valida y convierte. */
    public record GrupoIniciado(
            UUID eventoId,
            UUID correlationId,
            UUID grupoId,
            UUID periodoId,
            int numeroDePeriodo,
            LocalDate fechaLimitePago,
            LocalDate fechaFinGracia,
            List<Cupo> cupos) {}

    public record Cupo(UUID cupoId, UUID participanteId, Dinero montoAporte) {}

    public record Salida(Resultado resultado, int creadas) {}

    private final Datos datos;
    private final Consumidos consumidos;
    private final RegistroDelCalendario registro;

    public CU21AbrirPrimeraCuota(Datos datos, Consumidos consumidos, RegistroDelCalendario registro) {
        this.datos = datos;
        this.consumidos = consumidos;
        this.registro = registro;
    }

    @Transactional
    public Salida procesar(GrupoIniciado e) {
        validar(e);
        ContextoSesion ctx =
                ContextoSesion.deSistema(PROCESO, new Traza(e.correlationId().toString()));
        boolean nuevo = datos.conContexto(ctx, dsl -> consumidos.registrar(dsl, e.eventoId(), CONSUMIDOR));
        if (!nuevo) {
            return new Salida(Resultado.DUPLICADO, 0);
        }
        var periodo = new CalendarioDelPeriodo.Periodo(
                e.periodoId(),
                e.grupoId(),
                e.numeroDePeriodo(),
                PERIODO_AL_INICIAR,
                e.fechaLimitePago(),
                e.fechaFinGracia(),
                e.cupos().stream()
                        .map(c -> new CalendarioDelPeriodo.Cupo(c.cupoId(), c.participanteId(), c.montoAporte()))
                        .toList());
        // Misma transaccion que la marca de consumido: o quedan la marca y las obligaciones, o ninguna.
        var salida = registro.registrar(periodo, ctx);
        return new Salida(Resultado.ABIERTA, salida.creadas());
    }

    /** Lo que el contrato del evento promete y, si no se cumple, no se procesa. */
    static void validar(GrupoIniciado e) {
        if (e.numeroDePeriodo() != 1) {
            throw new IllegalArgumentException("El inicio del grupo abre el periodo 1, no el " + e.numeroDePeriodo());
        }
        if (e.cupos() == null || e.cupos().isEmpty()) {
            throw new IllegalArgumentException("El grupo iniciado no trae cupos ocupados");
        }
        if (e.fechaFinGracia().isBefore(e.fechaLimitePago())) {
            throw new IllegalArgumentException("El fin de gracia es anterior al vencimiento");
        }
        for (Cupo c : e.cupos()) {
            if (c.montoAporte() == null || c.montoAporte().monto().signum() <= 0) {
                throw new IllegalArgumentException("El cupo " + c.cupoId() + " no tiene un aporte positivo");
            }
        }
    }
}

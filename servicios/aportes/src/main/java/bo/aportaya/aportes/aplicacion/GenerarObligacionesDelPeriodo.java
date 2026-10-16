package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.VencimientoDeObligacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CU-21 (H8.S1.M1) · Generar el calendario de obligaciones de un periodo abierto: UNA por cupo.
 *
 * <p>Quien es titular de cada cupo, de que turno es el periodo y cuanto se aporta lo afirma `grupos`
 * ({@link CalendarioDelPeriodo}); aca no se cree nada que afirme el cliente (la llamada solo trae el
 * id del periodo). La pregunta va FUERA de la transaccion (invariante 6) y, si `grupos` no
 * contesta, no se genera nada (para dinero, denegar por omision).
 *
 * <p>Esta clase NO abre transaccion: la escritura local vive en {@link RegistroDelCalendario}.
 *
 * <p>Sin duplicacion: la unicidad (periodo, cupo) la exige la base (R-GRP-03); correr esto dos veces,
 * o dos pedidos a la vez, deja el mismo calendario y no emite un segundo evento.
 *
 * <p>Fechas: la CONTRACTUAL es la del reglamento ({@code grupos.periodo.fecha_limite_pago}); la que
 * se guarda como {@code fecha_vencimiento} es la de LIQUIDACION, corrida al siguiente dia habil si
 * cae en feriado o fin de semana (CU-59). Las dos viajan en la respuesta y en el evento, que avisa
 * al grupo cuando hubo corrimiento: nadie se entera de un cambio de fecha por su cuenta.
 */
@Service
public class GenerarObligacionesDelPeriodo {

    private final CalendarioDelPeriodo grupos;
    private final RegistroDelCalendario registro;

    public GenerarObligacionesDelPeriodo(CalendarioDelPeriodo grupos, RegistroDelCalendario registro) {
        this.grupos = grupos;
        this.registro = registro;
    }

    /** Una fila del calendario: titular, turno, monto y estado, con las dos fechas. */
    public record Linea(
            UUID obligacionId,
            UUID cupoId,
            UUID titularParticipanteId,
            int turno,
            Dinero monto,
            String estado,
            LocalDate fechaContractual,
            LocalDate fechaDeLiquidacion,
            LocalDate finDeGracia,
            boolean esNueva) {}

    public record Salida(
            UUID periodoId,
            UUID grupoId,
            int turno,
            LocalDate fechaContractual,
            LocalDate fechaDeLiquidacion,
            boolean corridaPorNoHabil,
            List<VencimientoDeObligacion.DiaSalteado> diasSalteados,
            int creadas,
            List<Linea> calendario) {}

    public Salida generar(UUID periodoId, ContextoSesion ctx) {
        var periodo = grupos.deLosCupos(periodoId)
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(21, 8),
                        "No se pudo confirmar el periodo con grupos: no se generan obligaciones."));
        validar(periodoId, periodo);
        return registro.registrar(periodo, ctx);
    }

    private static void validar(UUID pedido, CalendarioDelPeriodo.Periodo p) {
        if (!pedido.equals(p.periodoId())) {
            throw new ErrorDeNegocio(CodigoError.de(21, 8), "Grupos contesto por otro periodo: no se genera nada.");
        }
        if (!"ABIERTO".equals(p.estado())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(21, 4),
                    "Solo un periodo ABIERTO genera obligaciones: este esta " + p.estado() + ".");
        }
        if (p.cupos().isEmpty()) {
            throw new ErrorDeNegocio(
                    CodigoError.de(21, 8), "El periodo no tiene cupos ocupados: no hay a quien cobrarle.");
        }
        Set<UUID> vistos = new HashSet<>();
        Set<bo.aportaya.plataforma.dominio.Moneda> monedas = new HashSet<>();
        for (var c : p.cupos()) {
            if (!vistos.add(c.cupoId())
                    || c.montoEsperado().esCero()
                    || c.montoEsperado().esNegativo()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(21, 8), "Grupos informo un cupo repetido o un importe que no es positivo.");
            }
            monedas.add(c.montoEsperado().moneda());
        }
        if (monedas.size() != 1) {
            throw new ErrorDeNegocio(CodigoError.de(21, 8), "Los cupos del periodo mezclan monedas.");
        }
        if (p.fechaFinGracia().isBefore(p.fechaLimitePago())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(21, 8), "El fin de gracia informado por grupos es anterior al vencimiento.");
        }
    }
}

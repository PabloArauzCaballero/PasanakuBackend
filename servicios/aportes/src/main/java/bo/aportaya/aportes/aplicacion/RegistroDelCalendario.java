package bo.aportaya.aportes.aplicacion;

import bo.aportaya.aportes.dominio.VencimientoDeObligacion;
import bo.aportaya.aportes.infraestructura.CalendarioDeObligacionesRepositorio;
import bo.aportaya.aportes.infraestructura.CalendarioDeObligacionesRepositorio.Fila;
import bo.aportaya.aportes.infraestructura.FeriadosRepositorio;
import bo.aportaya.aportes.infraestructura.ObligacionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La mitad LOCAL de generar las obligaciones de un periodo: una transaccion, el calendario entero.
 *
 * <p>La pregunta a `grupos` ya se hizo afuera (invariante 6). Aca se leen los feriados del catalogo,
 * se calculan las fechas y se crea la obligacion de cada cupo con {@code ON CONFLICT DO NOTHING}
 * sobre R-GRP-03, junto con su evento (outbox).
 */
@Service
public class RegistroDelCalendario {

    private final Datos datos;
    private final FeriadosRepositorio feriados;
    private final CalendarioDeObligacionesRepositorio calendario;
    private final ObligacionRepositorio obligaciones;
    private final Outbox outbox;

    public RegistroDelCalendario(
            Datos datos,
            FeriadosRepositorio feriados,
            CalendarioDeObligacionesRepositorio calendario,
            ObligacionRepositorio obligaciones,
            Outbox outbox) {
        this.datos = datos;
        this.feriados = feriados;
        this.calendario = calendario;
        this.obligaciones = obligaciones;
        this.outbox = outbox;
    }

    @Transactional
    public GenerarObligacionesDelPeriodo.Salida registrar(CalendarioDelPeriodo.Periodo p, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            LocalDate contractual = p.fechaLimitePago();
            if (!feriados.hayCalendarioPara(dsl, contractual.getYear())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(21, 9),
                        "No hay calendario de feriados cargado para " + contractual.getYear()
                                + ": no se calcula el vencimiento.");
            }
            Map<LocalDate, String> noHabiles = feriados.noHabilesEntre(
                    dsl,
                    contractual,
                    p.fechaFinGracia().plusDays(2L * VencimientoDeObligacion.VENTANA_EN_DIAS),
                    p.grupoId());
            var fechas = VencimientoDeObligacion.de(contractual, p.fechaFinGracia(), noHabiles);
            int anioFinal = fechas.finDeGracia().getYear();
            if (anioFinal != contractual.getYear() && !feriados.hayCalendarioPara(dsl, anioFinal)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(21, 9),
                        "No hay calendario de feriados cargado para " + anioFinal + ": no se calcula la gracia.");
            }

            Optional<UUID> politica =
                    obligaciones.politicaDe(dsl, Optional.of(p.grupoId())).map(ObligacionRepositorio.PoliticaFila::id);
            Set<UUID> nuevas = new HashSet<>();
            for (var cupo : p.cupos()) {
                calendario
                        .crear(
                                dsl,
                                new CalendarioDeObligacionesRepositorio.Nueva(
                                        p.grupoId(),
                                        p.periodoId(),
                                        cupo.cupoId(),
                                        cupo.participanteId(),
                                        politica,
                                        cupo.montoEsperado(),
                                        fechas.fechaDeLiquidacion(),
                                        fechas.finDeGracia()))
                        .ifPresent(nuevas::add);
            }

            List<Fila> filas = calendario.delPeriodo(dsl, p.periodoId());
            List<GenerarObligacionesDelPeriodo.Linea> lineas = filas.stream()
                    .map(f -> new GenerarObligacionesDelPeriodo.Linea(
                            f.obligacionId(),
                            f.cupoId(),
                            f.participanteId(),
                            p.numero(),
                            f.monto(),
                            f.estado(),
                            fechas.fechaContractual(),
                            f.vencimiento(),
                            f.finDeGracia(),
                            nuevas.contains(f.obligacionId())))
                    .toList();

            if (!nuevas.isEmpty()) {
                outbox.emitir(
                        dsl,
                        new EventoDominio(
                                "aportes.obligaciones_generadas",
                                "periodo",
                                p.periodoId(),
                                Map.of(
                                        "grupoId", p.grupoId().toString(),
                                        "turno", p.numero(),
                                        "creadas", nuevas.size(),
                                        "fechaContractual",
                                                fechas.fechaContractual().toString(),
                                        "fechaDeLiquidacion",
                                                fechas.fechaDeLiquidacion().toString(),
                                        "corridaPorNoHabil", fechas.corridaPorNoHabil(),
                                        // CU-59 criterio 3: si el vencimiento se corrio, se avisa al grupo.
                                        "notificarAlGrupo", fechas.corridaPorNoHabil()),
                                UUID.fromString(ctx.traza().id())));
            }
            return new GenerarObligacionesDelPeriodo.Salida(
                    p.periodoId(),
                    p.grupoId(),
                    p.numero(),
                    fechas.fechaContractual(),
                    fechas.fechaDeLiquidacion(),
                    fechas.corridaPorNoHabil(),
                    fechas.diasSalteados(),
                    nuevas.size(),
                    lineas);
        });
    }
}

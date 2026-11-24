package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.HechoDeAcceso;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-08 · Baja del empleado (flujo alternativo, GU-14).
 *
 * <p>En un acto: revoca todas sus asignaciones, cierra todas sus sesiones y deja la cuenta en
 * {@code BAJA_LABORAL} (estado propio de la baja del personal interno, decision de Pablo 2026-11-24). No se borra nada ni se anonimiza: el historico de quien otorgo que a quien, y cuando
 * se revoco, es la evidencia que pide R-AUD-01. Una cuenta que ya esta de baja responde lo mismo sin volver a
 * escribir ni a anunciar.
 */
@Service
public class CU08DarDeBaja {

    static final String ESTADO_DE_BAJA = "BAJA_LABORAL";

    private final Datos datos;
    private final GobiernoDeAccesosRepositorio gobierno;
    private final ReglasDeGobierno reglas;
    private final RetiroDeAccesos retiro;
    private final BitacoraDeAccesos bitacora;
    private final Reloj reloj;

    public CU08DarDeBaja(
            Datos datos,
            AccesosRepositorio accesos,
            GobiernoDeAccesosRepositorio gobierno,
            ReglasDeGobierno reglas,
            Outbox outbox,
            Reloj reloj) {
        this.datos = datos;
        this.gobierno = gobierno;
        this.reglas = reglas;
        this.retiro = new RetiroDeAccesos(accesos, gobierno);
        this.bitacora = new BitacoraDeAccesos(outbox, gobierno);
        this.reloj = reloj;
    }

    public record ResultadoDeBaja(
            UUID usuarioId, String estado, int asignacionesRevocadas, int sesionesCerradas, boolean yaEstaba) {}

    @Transactional
    public ResultadoDeBaja ejecutar(UUID operador, String motivo, String evidenciaMfa, ContextoSesion ctx) {
        reglas.exigirStepUp(ctx, evidenciaMfa);
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            UUID usuario = gobierno.operador(dsl, operador).orElseThrow(GobiernoDeAccesos::noHabilitado);
            ReglasDeGobierno.noSobreSiMismo(ctx, usuario);
            if (ESTADO_DE_BAJA.equals(gobierno.estadoDelOperador(dsl, usuario).orElse(""))) {
                return new ResultadoDeBaja(usuario, ESTADO_DE_BAJA, 0, 0, true);
            }

            RetiroDeAccesos.Retiro hecho = retiro.retirar(dsl, usuario, "BAJA", motivo, ahora);
            gobierno.cambiarEstado(dsl, usuario, ESTADO_DE_BAJA);
            bitacora.registrar(
                    dsl,
                    new HechoDeAcceso(
                            HechoDeAcceso.Accion.OPERADOR_DADO_DE_BAJA,
                            Optional.of(ctx.usuarioId()),
                            usuario,
                            motivo,
                            ahora,
                            Map.of(
                                    "estado", ESTADO_DE_BAJA,
                                    "asignacionesRevocadas",
                                            hecho.asignacionesRevocadas().size(),
                                    "asignacionIds",
                                            hecho.asignacionesRevocadas().stream()
                                                    .map(UUID::toString)
                                                    .collect(Collectors.joining(",")),
                                    "sesionesCerradas", hecho.sesionesCerradas())),
                    usuario,
                    ctx);
            return new ResultadoDeBaja(
                    usuario, ESTADO_DE_BAJA, hecho.asignacionesRevocadas().size(), hecho.sesionesCerradas(), false);
        });
    }
}

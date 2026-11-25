package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.HechoDeAcceso;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio.Asignacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-08 · Revocar un rol de operador.
 *
 * <p>Revocar y cerrar las sesiones van en la MISMA transaccion. Separarlas deja una
 * ventana en la que el permiso ya no existe y la sesion que lo usaba sigue viva —y esa
 * ventana es exactamente el momento en que uno revoca porque tiene una razon urgente.
 *
 * <p>Revocar dos veces la misma asignacion no la revoca dos veces ni deja dos rastros: la
 * precondicion {@code revocada_en IS NULL} va en la escritura y la bitacora solo se escribe si la
 * escritura cambio algo.
 */
@Service
public class CU08RevocarRol {

    private final Datos datos;
    private final AccesosRepositorio accesos;
    private final GobiernoDeAccesosRepositorio gobierno;
    private final BitacoraDeAccesos bitacora;
    private final Reloj reloj;

    public CU08RevocarRol(Datos datos, AccesosRepositorio accesos, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.accesos = accesos;
        // Repositorio sin estado: se arma aca para no cambiar la firma que usan las pruebas de CU-08.
        this.gobierno = new GobiernoDeAccesosRepositorio();
        this.bitacora = new BitacoraDeAccesos(outbox, gobierno);
        this.reloj = reloj;
    }

    @Transactional
    public int ejecutar(UUID asignacionId, String motivo, boolean cerrarSesiones, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            Asignacion asignacion = gobierno.asignacion(dsl, asignacionId)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(8, 1), "Esa asignacion no existe."));
            UUID titular = asignacion.usuarioId();

            // Solo cuenta si revocar ESTA asignacion baja el numero de personas activas que
            // pueden administrar accesos a cero (GU-11): no las filas, las personas.
            ReglasDeGobierno.noDejaSinAdministrador(accesos, dsl, List.of(asignacionId), Optional.empty(), ahora);

            if (accesos.revocar(dsl, asignacionId, motivo, ahora) == 0) {
                return 0;
            }
            int sesionesCerradas = cerrarSesiones ? accesos.cerrarSesionesDe(dsl, titular, motivo, ahora) : 0;

            Map<String, Object> detalle = new HashMap<>();
            detalle.put("asignacionId", asignacionId.toString());
            detalle.put("rolId", asignacion.rolId().toString());
            detalle.put("rolCodigo", asignacion.rolCodigo());
            detalle.put("ambito", asignacion.ambito());
            asignacion.ambitoId().ifPresent(id -> detalle.put("ambitoId", id.toString()));
            detalle.put("sesionesCerradas", sesionesCerradas);
            bitacora.registrar(
                    dsl,
                    new HechoDeAcceso(
                            HechoDeAcceso.Accion.ROL_REVOCADO,
                            Optional.of(ctx.usuarioId()),
                            titular,
                            motivo,
                            ahora,
                            detalle),
                    asignacionId,
                    ctx);

            return sesionesCerradas;
        });
    }
}

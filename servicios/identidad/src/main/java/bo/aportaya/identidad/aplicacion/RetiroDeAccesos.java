package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.MotivoDeRevocacion;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio.Asignacion;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Quitarle a un operador TODO acceso en un acto (GU-14): baja del empleado y suspension.
 *
 * <p>Revoca cada asignacion sin revocar (la fila queda, con el motivo: R-AUD-01), cierra todas sus sesiones y
 * antes comprueba que la plataforma no se quede sin administrador de accesos contando al usuario entero como
 * saliente. Suspender ya no deja roles vivos esperando a que alguien reactive la cuenta: reactivar devuelve
 * una cuenta SIN roles, que se vuelven a otorgar uno por uno con su justificacion (revision forzada).
 */
final class RetiroDeAccesos {

    private final AccesosRepositorio accesos;
    private final GobiernoDeAccesosRepositorio gobierno;

    RetiroDeAccesos(AccesosRepositorio accesos, GobiernoDeAccesosRepositorio gobierno) {
        this.accesos = accesos;
        this.gobierno = gobierno;
    }

    record Retiro(List<UUID> asignacionesRevocadas, int sesionesCerradas) {}

    Retiro retirar(DSLContext dsl, UUID usuario, String acto, String motivo, OffsetDateTime ahora) {
        List<Asignacion> vivas = gobierno.sinRevocarDe(dsl, usuario);
        ReglasDeGobierno.noDejaSinAdministrador(
                accesos, dsl, vivas.stream().map(Asignacion::id).toList(), Optional.of(usuario), ahora);

        String enLaFila = MotivoDeRevocacion.de(acto, motivo, MotivoDeRevocacion.LARGO_EN_ASIGNACION);
        List<UUID> revocadas = vivas.stream()
                .map(Asignacion::id)
                .filter(id -> accesos.revocar(dsl, id, enLaFila, ahora) == 1)
                .toList();
        int sesiones = accesos.cerrarSesionesDe(dsl, usuario, enLaFila, ahora);
        return new Retiro(revocadas, sesiones);
    }
}

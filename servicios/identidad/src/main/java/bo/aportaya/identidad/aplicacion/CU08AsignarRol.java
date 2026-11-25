package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.AmbitoDeRol;
import bo.aportaya.identidad.dominio.HechoDeAcceso;
import bo.aportaya.identidad.dominio.PermisosEfectivos;
import bo.aportaya.identidad.dominio.SegregacionDeFunciones;
import bo.aportaya.identidad.infraestructura.AccesoRepositorio;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-08 · Asignar un rol de operador.
 *
 * <p>Una transaccion: valida, escribe la asignacion, la deja en la bitacora y pide el aviso
 * ({@link BitacoraDeAccesos}). La
 * autoasignacion tambien la corta la base ({@code ck_asignacion_no_autoasignada}); se
 * valida antes para dar un mensaje util, no para reemplazarla.
 */
@Service
public class CU08AsignarRol {

    private final Datos datos;
    private final AccesosRepositorio accesos;
    private final AccesoRepositorio factores;
    private final GobiernoDeAccesosRepositorio gobierno;
    private final BitacoraDeAccesos bitacora;
    private final Reloj reloj;

    public CU08AsignarRol(
            Datos datos, AccesosRepositorio accesos, AccesoRepositorio factores, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.accesos = accesos;
        this.factores = factores;
        // Repositorio sin estado: se arma aca para no cambiar la firma que usan las pruebas de CU-08.
        this.gobierno = new GobiernoDeAccesosRepositorio();
        this.bitacora = new BitacoraDeAccesos(outbox, gobierno);
        this.reloj = reloj;
    }

    @Transactional
    public SalidaAsignacion ejecutar(EntradaAsignacion entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            if (entrada.usuarioId().equals(ctx.usuarioId())) {
                throw new ErrorDeNegocio(CodigoError.de(8, 4), "Nadie se amplia sus propios permisos.");
            }

            String ambitoDelRol = accesos.ambitoDelRol(dsl, entrada.rolId())
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(8, 1), "Ese rol no existe."));
            AmbitoDeRol ambito = AmbitoDeRol.valueOf(entrada.ambito());
            if (!ambito.completoCon(entrada.ambitoId())) {
                throw new ErrorDeNegocio(CodigoError.de(8, 2), "Un rol de grupo necesita decir de que grupo se trata.");
            }

            Set<String> resultantes =
                    new TreeSet<>(PermisosEfectivos.de(accesos.asignacionesDe(dsl, entrada.usuarioId()), ahora));
            resultantes.addAll(accesos.permisosDelRol(dsl, entrada.rolId()));
            SegregacionDeFunciones.conflicto(resultantes).ifPresent(par -> {
                throw new ErrorDeNegocio(
                        CodigoError.de(8, 3),
                        "Nadie autoriza y ejecuta la misma operacion.",
                        Map.of("parIncompatible", par));
            });

            UUID asignacionId = accesos.asignar(
                    dsl,
                    entrada.usuarioId(),
                    entrada.rolId(),
                    entrada.ambito(),
                    entrada.ambitoId(),
                    ctx.usuarioId(),
                    entrada.vigenteHasta(),
                    ahora);

            // La asignacion QUEDA ESCRITA aunque falte el TOTP: asignacion_rol no
            // tiene columna de estado y no se inventa una. Lo que no va a poder es
            // abrir sesion, y la base es la que lo impide (R-SEG-10).
            boolean faltaMfa = ambitoDelRol.equals(AmbitoDeRol.GLOBAL.name())
                    && factores.factorActivo(dsl, entrada.usuarioId()).isEmpty();

            bitacora.registrar(
                    dsl, hechoDeAsignacion(dsl, asignacionId, entrada, faltaMfa, ctx, ahora), asignacionId, ctx);

            return new SalidaAsignacion(asignacionId, Set.copyOf(resultantes), faltaMfa, entrada.vigenteHasta());
        });
    }

    /** Quien otorgo que a quien, con que justificacion (CU-08 flujo 3, GU-07). */
    private HechoDeAcceso hechoDeAsignacion(
            DSLContext dsl,
            UUID asignacionId,
            EntradaAsignacion entrada,
            boolean faltaMfa,
            ContextoSesion ctx,
            OffsetDateTime ahora) {
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("asignacionId", asignacionId.toString());
        detalle.put("rolId", entrada.rolId().toString());
        gobierno.asignacion(dsl, asignacionId).ifPresent(a -> detalle.put("rolCodigo", a.rolCodigo()));
        detalle.put("ambito", entrada.ambito());
        entrada.ambitoId().ifPresent(id -> detalle.put("ambitoId", id.toString()));
        entrada.vigenteHasta().ifPresent(hasta -> detalle.put("vigenteHasta", hasta.toString()));
        detalle.put("requiereMfa", faltaMfa);
        return new HechoDeAcceso(
                HechoDeAcceso.Accion.ROL_ASIGNADO,
                Optional.of(ctx.usuarioId()),
                entrada.usuarioId(),
                entrada.justificacion(),
                ahora,
                detalle);
    }

    public record EntradaAsignacion(
            UUID usuarioId,
            UUID rolId,
            String ambito,
            Optional<UUID> ambitoId,
            Optional<OffsetDateTime> vigenteHasta,
            String justificacion) {}

    public record SalidaAsignacion(
            UUID asignacionId,
            Set<String> permisosEfectivos,
            boolean requiereMfa,
            Optional<OffsetDateTime> vigenteHasta) {}
}

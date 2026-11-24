package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.HechoDeAcceso;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio.Asignacion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-08 flujo 6 · El trabajo diario vence las asignaciones cuyo {@code vigente_hasta} ya paso (GU-08, decision
 * P-6).
 *
 * <p>El permiso efectivo ya dejaba de contar al vencer ({@code PermisosEfectivos}); lo que faltaba era el acto:
 * nadie cerraba la fila, nadie cortaba la sesion que se abrio con ese permiso y nadie se enteraba. Ahora cada
 * vencida queda con {@code revocada_en = vigente_hasta} y motivo {@code VENCIMIENTO}, se cierran las sesiones del
 * titular, y cada una deja su {@code identidad.rol_vencido} en la bitacora con el aviso al titular y al oficial
 * de cumplimiento. Una segunda corrida no encuentra nada (precondicion en la escritura).
 */
@Service
public class CU08VencerAccesos {

    static final String MOTIVO = "VENCIMIENTO: vigente_hasta cumplido";

    private final Datos datos;
    private final AccesosRepositorio accesos;
    private final GobiernoDeAccesosRepositorio gobierno;
    private final BitacoraDeAccesos bitacora;
    private final Reloj reloj;
    private final int lote;

    public CU08VencerAccesos(
            Datos datos,
            AccesosRepositorio accesos,
            GobiernoDeAccesosRepositorio gobierno,
            Outbox outbox,
            Reloj reloj,
            @Value("${identidad.accesos.lote-de-vencimiento:500}") int lote) {
        this.datos = datos;
        this.accesos = accesos;
        this.gobierno = gobierno;
        this.bitacora = new BitacoraDeAccesos(outbox, gobierno);
        this.reloj = reloj;
        this.lote = lote;
    }

    /** Vence un lote y devuelve cuantas asignaciones cerro. El trabajo repite mientras el lote venga lleno. */
    @Transactional
    public int ejecutar(ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            List<Asignacion> vencidas = gobierno.vencer(dsl, MOTIVO, ahora, lote);
            Set<UUID> titulares = new LinkedHashSet<>();
            for (Asignacion vencida : vencidas) {
                titulares.add(vencida.usuarioId());
                bitacora.registrar(
                        dsl,
                        new HechoDeAcceso(
                                HechoDeAcceso.Accion.ROL_VENCIDO,
                                Optional.empty(),
                                vencida.usuarioId(),
                                MOTIVO,
                                ahora,
                                Map.of(
                                        "asignacionId", vencida.id().toString(),
                                        "rolCodigo", vencida.rolCodigo(),
                                        "ambito", vencida.ambito(),
                                        "vencioEn",
                                                vencida.vigenteHasta()
                                                        .map(OffsetDateTime::toString)
                                                        .orElse(""))),
                        vencida.id(),
                        ctx);
            }
            titulares.forEach(titular -> accesos.cerrarSesionesDe(dsl, titular, MOTIVO, ahora));
            return vencidas.size();
        });
    }
}

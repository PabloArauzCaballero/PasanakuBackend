package bo.aportaya.auditoria.aplicacion;

import bo.aportaya.auditoria.dominio.DisponibilidadDelServicio;
import bo.aportaya.auditoria.dominio.SaludDeServicio;
import bo.aportaya.auditoria.infraestructura.DisponibilidadRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * sistemas/servicios · guarda lo que contesto cada servicio en un minuto y lee lo medido en la ventana del SLO. La
 * consulta de red (la sonda) NO pasa por aca: se hace antes, fuera de la transaccion, para que un servicio colgado no
 * tenga una conexion de la base tomada mientras espera.
 */
@Service
public class RegistroDeDisponibilidad {

    /** La ventana del SLO: los ultimos 30 dias UTC, contando hoy. */
    public static final int DIAS_DE_VENTANA = 30;

    private static final UUID PROCESO = UUID.fromString("00000000-0000-4000-8000-0000000005a0");

    private final Datos datos;
    private final DisponibilidadRepositorio repositorio;

    public RegistroDeDisponibilidad(Datos datos, DisponibilidadRepositorio repositorio) {
        this.datos = datos;
        this.repositorio = repositorio;
    }

    /**
     * Suma el minuto de {@code medidoEn} a cada servicio. Repetir el mismo minuto no cuenta dos veces.
     *
     * @return cuantos servicios sumaron el minuto
     */
    @Transactional
    public int registrar(List<SaludDeServicio> salud, Instant medidoEn) {
        Instant minuto = medidoEn.truncatedTo(ChronoUnit.MINUTES);
        var ctx = ContextoSesion.deSistema(PROCESO, new Traza(UUID.randomUUID().toString()));
        return datos.conContexto(ctx, dsl -> (int) salud.stream()
                .filter(s -> repositorio.sumarMinuto(dsl, s.servicio(), s.estado(), minuto))
                .count());
    }

    /** Lo medido de cada servicio en la ventana que termina el dia de {@code ahora}. */
    @Transactional(readOnly = true)
    public Map<String, DisponibilidadDelServicio> enLaVentana(Instant ahora) {
        LocalDate desde = ahora.atOffset(ZoneOffset.UTC).toLocalDate().minusDays(DIAS_DE_VENTANA - 1L);
        var ctx = ContextoSesion.deSistema(PROCESO, new Traza(UUID.randomUUID().toString()));
        return datos.conContexto(ctx, dsl -> repositorio.desde(dsl, desde));
    }
}

package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.InformeDeAccesos.Fila;
import bo.aportaya.identidad.dominio.InformeDeAccesos.Seccion;
import bo.aportaya.identidad.infraestructura.InformeDeAccesosRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-08 · Informe de accesos (GU-08, decision P-6): accesos vivos, los que vencen dentro del horizonte (7 dias
 * por omision, configurable) y los operadores con rol GLOBAL que todavia no tienen TOTP confirmado — esos no
 * entran (R-SEG-10) y el informe los separa para que alguien los persiga.
 */
@Service
public class CU08InformeDeAccesos {

    private final Datos datos;
    private final InformeDeAccesosRepositorio informe;
    private final Reloj reloj;
    private final Duration horizonte;

    public CU08InformeDeAccesos(
            Datos datos,
            InformeDeAccesosRepositorio informe,
            Reloj reloj,
            @Value("${identidad.accesos.horizonte-por-vencer:P7D}") Duration horizonte) {
        this.datos = datos;
        this.informe = informe;
        this.reloj = reloj;
        this.horizonte = horizonte;
    }

    public record Pagina(
            Seccion seccion,
            OffsetDateTime generadoEn,
            List<Fila> filas,
            int pagina,
            int limite,
            int total,
            int totalPaginas) {}

    @Transactional(readOnly = true)
    public Pagina ejecutar(Seccion seccion, int pagina, int limite, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        OffsetDateTime hasta = ahora.plus(horizonte);
        return datos.conContexto(ctx, dsl -> {
            int total = informe.contar(dsl, seccion, ahora, hasta);
            List<Fila> filas = informe.pagina(dsl, seccion, ahora, hasta, (pagina - 1) * limite, limite);
            int totalPaginas = total == 0 ? 0 : (total + limite - 1) / limite;
            return new Pagina(seccion, ahora, filas, pagina, limite, total, totalPaginas);
        });
    }
}

package bo.aportaya.tarifas.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Consumidos;
import bo.aportaya.tarifas.dominio.VigenciaDeParametros;
import bo.aportaya.tarifas.infraestructura.CongelamientoRepositorio;
import bo.aportaya.tarifas.infraestructura.ParametroRepositorio;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-132 · F3.S8.M6: congelar los parámetros financieros del grupo al consumir {@code grupos.grupo_iniciado}.
 *
 * <p>Se congela la versión APROBADA que regía cuando el grupo inició ({@code iniciadoEn} del evento, no la hora de
 * proceso): un cambio posterior solo vale para grupos nuevos (ADR-056 §6).
 *
 * <ul>
 *   <li><b>Una sola vez por evento</b>: la marca {@code evento_consumido} y las filas van en la misma transacción.
 *   <li><b>Una sola copia por grupo, parámetro y nivel</b> (R-TAR-17): la unicidad de la base decide; un inicio
 *       re-emitido con otro id no cambia lo congelado.
 *   <li><b>Incompleto no se inventa</b>: si un código no tenía versión aprobada, no se congela nada en su lugar. La
 *       consulta por grupo lo informa (422 con el código) y {@code garantia} falla cerrado.
 * </ul>
 */
@Service
public class CongelarParametrosDelGrupo {

    /** Quien consume, para la marca de eventos ya procesados. */
    public static final String CONSUMIDOR = "tarifas.congelar-parametros-del-grupo";

    /** El proceso que atiende el evento: fijo, para poder leerlo en el registro. */
    private static final UUID PROCESO = UUID.fromString("00000000-0000-0000-0000-0000000001f6");

    public enum Desenlace {
        CONGELADO,
        YA_CONGELADO,
        DUPLICADO
    }

    /** Lo que se usa de {@code grupos.grupo_iniciado}, incluida la instantánea del catálogo de Transparencia. */
    public record GrupoIniciado(
            UUID eventoId, UUID correlationId, UUID grupoId, OffsetDateTime iniciadoEn, List<String> nivelesAplicables) {}

    /** El desenlace y los códigos del catálogo que quedaron sin valor (vacío si el congelamiento está completo). */
    public record Resultado(Desenlace desenlace, Set<String> faltantes) {}

    private final Datos datos;
    private final ParametroRepositorio parametros;
    private final CongelamientoRepositorio congelamientos;
    private final Reloj reloj;
    private final Consumidos consumidos = new Consumidos("tarifas");

    public CongelarParametrosDelGrupo(
            Datos datos, ParametroRepositorio parametros, CongelamientoRepositorio congelamientos, Reloj reloj) {
        this.datos = datos;
        this.parametros = parametros;
        this.congelamientos = congelamientos;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado procesar(GrupoIniciado grupo) {
        ContextoSesion ctx = ContextoSesion.deSistema(
                PROCESO, new Traza(grupo.correlationId().toString()));
        return datos.conContexto(ctx, dsl -> {
            if (!consumidos.registrar(dsl, grupo.eventoId(), CONSUMIDOR)) {
                return new Resultado(Desenlace.DUPLICADO, Set.of());
            }
            var catalogo = parametros.catalogo(dsl);
            var niveles = congelamientos.fijarNivelesAplicables(
                    dsl, grupo.grupoId(), grupo.nivelesAplicables(), reloj.ahora().atOffset(ZoneOffset.UTC));
            var vigentes = VigenciaDeParametros.vigentesAl(
                    parametros.vivas(dsl), grupo.iniciadoEn(), catalogo, niveles);
            int nuevas = congelamientos.congelar(
                    dsl, grupo.grupoId(), vigentes, reloj.ahora().atOffset(ZoneOffset.UTC));
            var congelados = congelamientos.delGrupo(dsl, grupo.grupoId());
            var faltantes = VigenciaDeParametros.faltantes(catalogo, niveles, congelados);
            boolean yaEstaba = nuevas == 0 && !vigentes.isEmpty();
            return new Resultado(yaEstaba ? Desenlace.YA_CONGELADO : Desenlace.CONGELADO, faltantes);
        });
    }
}

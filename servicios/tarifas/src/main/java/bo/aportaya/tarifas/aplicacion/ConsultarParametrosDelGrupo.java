package bo.aportaya.tarifas.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.tarifas.dominio.VigenciaDeParametros;
import bo.aportaya.tarifas.dominio.VigenciaDeParametros.ParametroCongelado;
import bo.aportaya.tarifas.infraestructura.CongelamientoRepositorio;
import bo.aportaya.tarifas.infraestructura.ParametroRepositorio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-132 · F3.S8.M6: los parámetros congelados de un grupo, como los consume {@code garantia} (contrato de L2).
 *
 * <ul>
 *   <li>Sin ninguna fila: el grupo no tiene congelamiento (no inició, o es anterior al registro) → vacío (404).
 *   <li>Con filas pero sin algún código del catálogo: incompleto → {@code AP-CU132-06} (422) nombrando los códigos.
 *       Nunca se completa con la versión de hoy: lo que rige para el grupo es lo que regía al iniciar.
 * </ul>
 */
@Service
public class ConsultarParametrosDelGrupo {

    static final CodigoError INCOMPLETO = CodigoError.de(132, 6);

    private final Datos datos;
    private final ParametroRepositorio parametros;
    private final CongelamientoRepositorio congelamientos;

    public ConsultarParametrosDelGrupo(
            Datos datos, ParametroRepositorio parametros, CongelamientoRepositorio congelamientos) {
        this.datos = datos;
        this.parametros = parametros;
        this.congelamientos = congelamientos;
    }

    @Transactional(readOnly = true)
    public Optional<List<ParametroCongelado>> delGrupo(UUID grupoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> {
            var congelados = congelamientos.delGrupo(dsl, grupoId);
            if (congelados.isEmpty()) {
                return Optional.empty();
            }
            var conValor =
                    congelados.stream().map(ParametroCongelado::parametroId).collect(Collectors.toSet());
            var faltan = VigenciaDeParametros.faltantes(parametros.catalogo(dsl), conValor);
            if (!faltan.isEmpty()) {
                throw new ErrorDeNegocio(
                        INCOMPLETO, "El congelamiento del grupo está incompleto; faltan: " + String.join(", ", faltan));
            }
            return Optional.of(congelados);
        });
    }
}

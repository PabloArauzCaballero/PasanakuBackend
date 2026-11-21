package bo.aportaya.tarifas.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.tarifas.dominio.ParametroDelCatalogo;
import bo.aportaya.tarifas.dominio.PropuestaDeParametro;
import bo.aportaya.tarifas.dominio.VersionDelParametro;
import bo.aportaya.tarifas.dominio.VigenciaDeParametros;
import bo.aportaya.tarifas.infraestructura.ParametroRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-132 · Registrar y aprobar parámetros financieros (F3.S8.M2b).
 *
 * <p>Una persona propone, otra distinta aprueba; las dos salen de su token (R-TAR-14, DECISIONES tanda 7). Lo
 * aprobado no se edita (R-TAR-16). Ningún valor sale de este código: llegan con su procedencia y los decide quien
 * tiene autoridad (F3.S8.M3).
 */
@Service
public class CU132RegistroDeParametros {

    private static final CodigoError INEXISTENTE = CodigoError.de(132, 1);
    private static final CodigoError INCOHERENTE = CodigoError.de(132, 2);
    private static final CodigoError MISMA_PERSONA = CodigoError.de(132, 3);
    private static final CodigoError YA_RESUELTA = CodigoError.de(132, 4);
    private static final CodigoError REINTENTO_DISTINTO = CodigoError.de(132, 5);

    private final Datos datos;
    private final ParametroRepositorio parametros;
    private final Reloj reloj;

    public CU132RegistroDeParametros(Datos datos, ParametroRepositorio parametros, Reloj reloj) {
        this.datos = datos;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    public record Propuesta(
            Optional<String> nivel,
            BigDecimal valor,
            Optional<String> moneda,
            OffsetDateTime vigenteDesde,
            String fuente,
            String referencia,
            LocalDate fecha) {}

    /** Un parámetro del catálogo con su versión vigente (una por nivel) y lo que espera aprobación. */
    public record EnCatalogo(
            ParametroDelCatalogo parametro, List<VersionDelParametro> vigentes, List<VersionDelParametro> propuestas) {}

    @Transactional(readOnly = true)
    public List<EnCatalogo> catalogo(ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            List<VersionDelParametro> vivas = parametros.vivas(dsl);
            return parametros.catalogo(dsl).stream()
                    .map(p -> {
                        var delParametro = vivas.stream()
                                .filter(v -> v.parametroId().equals(p.id()))
                                .toList();
                        var vigentes = VigenciaDeParametros.vigentesAl(delParametro, ahora);
                        var propuestas = delParametro.stream()
                                .filter(v -> "PROPUESTA".equals(v.estado()))
                                .toList();
                        return new EnCatalogo(p, vigentes, propuestas);
                    })
                    .toList();
        });
    }

    @Transactional
    public VersionDelParametro proponer(String codigo, Propuesta propuesta, UUID clave, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            ParametroDelCatalogo parametro = parametros
                    .porCodigo(dsl, codigo)
                    .orElseThrow(() -> new ErrorDeNegocio(INEXISTENTE, "Ese parámetro no está en el catálogo."));
            PropuestaDeParametro.incoherencia(
                            parametro.catalogo(), propuesta.nivel(), propuesta.valor(), propuesta.moneda())
                    .ifPresent(motivo -> {
                        throw new ErrorDeNegocio(INCOHERENTE, motivo);
                    });
            var nueva = new ParametroRepositorio.Nueva(
                    parametro.id(),
                    propuesta.nivel(),
                    propuesta.valor(),
                    propuesta.moneda(),
                    propuesta.vigenteDesde(),
                    propuesta.fuente(),
                    propuesta.referencia(),
                    propuesta.fecha(),
                    ctx.usuarioId(),
                    ahora,
                    clave);
            Optional<UUID> id = parametros.proponer(dsl, nueva);
            if (id.isPresent()) {
                return parametros.version(dsl, id.get()).orElseThrow();
            }
            // Sin id: la clave ya estaba. Es la misma propuesta (reintento) o una distinta con la clave reutilizada.
            VersionDelParametro previa = parametros.porClave(dsl, clave).orElseThrow();
            if (!esLaMisma(previa, nueva)) {
                throw new ErrorDeNegocio(
                        REINTENTO_DISTINTO, "Esa clave de idempotencia ya se usó para otra propuesta.");
            }
            return previa;
        });
    }

    /** Quien propuso la versión, para que la capa web lo compare con quien aprueba ({@code CuatroOjos}). */
    @Transactional(readOnly = true)
    public UUID propuestaPor(UUID versionId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> parametros
                .version(dsl, versionId)
                .map(VersionDelParametro::propuestoPor)
                .orElseThrow(() -> new ErrorDeNegocio(INEXISTENTE, "Esa versión no existe.")));
    }

    /**
     * Segundo acto: otra persona aprueba. Quien aprueba es {@code ctx.usuarioId()}, del token. La capa web ya lo
     * comparó con {@code CuatroOjos}; acá se vuelve a exigir porque el caso de uso no confía en quien lo llama, y la
     * base lo exige una tercera vez ({@code ck_version_parametro_cuatro_ojos}, R-TAR-14).
     */
    @Transactional
    public VersionDelParametro aprobar(UUID versionId, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        return datos.conContexto(ctx, dsl -> {
            VersionDelParametro version = parametros
                    .version(dsl, versionId)
                    .orElseThrow(() -> new ErrorDeNegocio(INEXISTENTE, "Esa versión no existe."));
            if (!"PROPUESTA".equals(version.estado())) {
                // Reintento de la misma aprobación: misma respuesta, sin aprobar dos veces.
                if ("APROBADA".equals(version.estado())
                        && version.aprobadoPor().map(ctx.usuarioId()::equals).orElse(false)) {
                    return version;
                }
                throw new ErrorDeNegocio(YA_RESUELTA, "Esa versión ya fue resuelta.");
            }
            if (ctx.esSistema() || version.propuestoPor().equals(ctx.usuarioId())) {
                throw new ErrorDeNegocio(MISMA_PERSONA, "Quien aprueba un parámetro no puede ser quien lo propuso.");
            }
            ParametroDelCatalogo parametro =
                    parametros.porCodigo(dsl, version.codigo()).orElseThrow();
            if (!PropuestaDeParametro.aprobable(parametro.catalogo())) {
                throw new ErrorDeNegocio(INCOHERENTE, "El parámetro todavía no tiene unidad decidida: no se aprueba.");
            }
            if (!parametros.aprobar(dsl, versionId, ctx.usuarioId(), ahora)) {
                throw new ErrorDeNegocio(YA_RESUELTA, "Otra aprobación resolvió esa versión primero.");
            }
            return parametros.version(dsl, versionId).orElseThrow();
        });
    }

    private static boolean esLaMisma(VersionDelParametro v, ParametroRepositorio.Nueva n) {
        return v.parametroId().equals(n.parametroId())
                && v.nivel().equals(n.nivel())
                && v.valor().compareTo(n.valor()) == 0
                && v.moneda().equals(n.moneda())
                && v.vigenteDesde().isEqual(n.vigenteDesde())
                && v.procedenciaFuente().equals(n.fuente())
                && v.procedenciaReferencia().equals(n.referencia())
                && v.procedenciaFecha().equals(n.fecha())
                && Objects.equals(v.propuestoPor(), n.propuestoPor());
    }
}

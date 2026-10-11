package bo.aportaya.nucleofinanciero.aplicacion;

import bo.aportaya.nucleofinanciero.dominio.VigenciaDeRetencion.Motivo;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.nucleofinanciero.infraestructura.RetencionRepositorio.Retencion;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Las reglas comunes de las operaciones que ejecutan una retencion o mueven la custodia, que
 * llaman OTROS SERVICIOS y no una persona (H2.S1, hallazgo de seguridad de la segunda vuelta).
 *
 * <p><b>Amenaza.</b> Con {@code BILLETERA_OPERAR} cualquier titular podria pagar la retencion de
 * otro a un beneficiario que eligiera, o acreditarse importes de la custodia. <b>Control.</b>
 * (1) la ruta exige una identidad de servicio ({@code @Permiso("SERVICIO_*")}) y (2) esta clase lo
 * vuelve a exigir en el caso de uso, para que ningun otro llamador interno salte la ruta. El
 * servicio llamador es quien identifica a la persona que ya valido; aqui se verifica contra el
 * libro, nunca se confia en lo que dice el cuerpo.
 */
final class ReglasDeEjecucion {

    static final String SERVICIO_ENTREGAS = "SERVICIO_ENTREGAS";
    static final String SERVICIO_INVERSIONES = "SERVICIO_INVERSIONES";

    private ReglasDeEjecucion() {}

    /** Solo la identidad de servicio esperada (o el propio backend) puede ejecutar esta operacion. */
    static void exigirServicio(ContextoSesion ctx, String servicioEsperado, CodigoError codigo) {
        if (!ctx.esSistema() && !servicioEsperado.equals(ctx.rol())) {
            throw new ErrorDeNegocio(codigo, "Esta operacion solo la ejecuta el servicio " + servicioEsperado + ".");
        }
    }

    /**
     * El contexto con que se escribe el libro. El servicio llamador no es el titular, y la politica de
     * fila solo deja ver la billetera a su dueña: el libro lo escribe el sistema, ya autorizado arriba.
     */
    static ContextoSesion comoOperacion(ContextoSesion ctx) {
        return ctx.esSistema() ? ctx : ContextoSesion.deSistema(ctx.usuarioId(), ctx.traza());
    }

    static void exigirEjecutable(Retencion retencion, Motivo esperado, Dinero monto) {
        if (!esperado.name().equals(retencion.motivo())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(13, 5), "Esa retencion es por " + retencion.motivo() + ", no por " + esperado + ".");
        }
        if (!"VIGENTE".equals(retencion.estado())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(13, 3),
                    "Esa retencion ya esta " + retencion.estado() + ": no admite mas operaciones.");
        }
        if (monto.moneda() != retencion.monto().moneda() || !monto.equals(retencion.monto())) {
            // Sin cobro parcial ni redondeo: lo retenido es exactamente lo que se ejecuta. Un residuo de
            // un centavo no se absorbe en silencio, se rechaza.
            throw new ErrorDeNegocio(
                    CodigoError.de(13, 6),
                    "El importe no es el retenido: se retuvo " + retencion.monto() + " y se pidio " + monto + ".",
                    Map.of("retenido", retencion.monto().toString(), "pedido", monto.toString()));
        }
    }

    static ErrorDeNegocio sinCuentaDeSistema(String cual, Moneda moneda) {
        return new ErrorDeNegocio(
                CodigoError.de(13, 7),
                "No hay " + cual + " activa en " + moneda + ": la operacion no se puede asentar.");
    }

    static Optional<UUID> iniciador(ContextoSesion ctx) {
        return ctx.esSistema() ? Optional.empty() : Optional.of(ctx.usuarioId());
    }

    /** Bloquea en orden de identificador: dos operaciones cruzadas se turnan en vez de abrazarse. */
    static void bloquearEnOrden(DSLContext dsl, CuentaBilleteraRepositorio cuentas, List<UUID> ids) {
        List<UUID> ordenados = ids.stream().distinct().sorted().toList();
        Datos.comoSistema(dsl, () -> {
            ordenados.forEach(id -> cuentas.bloquear(dsl, id));
            return null;
        });
    }
}

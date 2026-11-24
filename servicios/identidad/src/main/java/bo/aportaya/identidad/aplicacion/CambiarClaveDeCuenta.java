package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.CorteDeCredencial;
import bo.aportaya.identidad.dominio.PoliticaDeClave;
import bo.aportaya.identidad.infraestructura.UsuarioRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-09 · cambiar la clave propia (GU-03). Es el endpoint que le faltaba a {@link CU09CambiarCredencial}.
 *
 * <p>Delega el cambio, el historial y el corte de sesiones en el caso de uso existente y agrega lo que
 * le toca a la cuenta del operador: apagar la marca de «debe cambiar la clave» y dejar el paso en la
 * bitacora con aviso (P-5). Una sola transaccion: si la bitacora falla, la clave no cambia.
 */
@Service
public class CambiarClaveDeCuenta {

    private final CU09CambiarCredencial cambio;
    private final Datos datos;
    private final UsuarioRepositorio usuarios;
    private final BitacoraDeCuenta bitacora;
    private final Reloj reloj;
    private final PoliticaDeClave politica;

    public CambiarClaveDeCuenta(
            CU09CambiarCredencial cambio,
            Datos datos,
            UsuarioRepositorio usuarios,
            BitacoraDeCuenta bitacora,
            Reloj reloj,
            @Value("${identidad.clave.largo-minimo:8}") int largoMinimoDeClave,
            @Value("${identidad.clave.no-se-repiten:5}") int clavesQueNoSeRepiten) {
        this.cambio = cambio;
        this.datos = datos;
        this.usuarios = usuarios;
        this.bitacora = bitacora;
        this.reloj = reloj;
        this.politica = new PoliticaDeClave(largoMinimoDeClave, clavesQueNoSeRepiten);
    }

    public record Salida(int sesionesCerradas, boolean cierraLaSesionActual) {}

    @Transactional
    public Salida cambiar(String claveActual, String claveNueva, UUID sesionActual, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        var salida = cambio.ejecutar(
                new CU09CambiarCredencial.EntradaCambio(
                        claveActual.toCharArray(),
                        claveNueva.toCharArray(),
                        Optional.ofNullable(sesionActual),
                        Optional.empty(),
                        Optional.empty(),
                        false,
                        Duration.ZERO),
                ctx,
                politica);
        CorteDeCredencial corte = salida.corte();
        datos.conContexto(ctx, dsl -> {
            if (usuarios.perfilDe(dsl, ctx.usuarioId()).esOperador()) {
                dsl.execute(
                        "UPDATE identidad.usuario_interno SET debe_cambiar_contrasena = false WHERE usuario_id = ?",
                        ctx.usuarioId());
                bitacora.registrar(
                        dsl,
                        BitacoraDeCuenta.Accion.CLAVE_CAMBIADA,
                        ctx.usuarioId(),
                        new BitacoraDeCuenta.Actor(ctx.usuarioId(), ctx.rol(), BitacoraDeCuenta.ORIGEN_PANEL),
                        "Cambio de clave propio",
                        Map.of("sesionesCerradas", salida.sesionesCerradas()),
                        UUID.fromString(ctx.traza().id()),
                        ahora);
            }
            return null;
        });
        return new Salida(salida.sesionesCerradas(), corte.alcanzaALaSesionActual());
    }
}

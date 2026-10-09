package bo.aportaya.entregas.aplicacion;

import bo.aportaya.entregas.dominio.OfertaVisible;
import bo.aportaya.entregas.dominio.ReglasDeOferta;
import bo.aportaya.entregas.dominio.puertos.HechosDeGrupos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Publicar, cancelar, vencer y listar las ofertas del derecho a cobrar un turno.
 *
 * <p>Esta clase NO abre transaccion: pregunta a {@code grupos} afuera y delega la escritura en
 * {@link RegistroDeOfertas}. Lo que se pregunta no lo afirma el cliente: que el turno sea de
 * quien lo vende y que quien mira sea miembro del grupo vienen de {@code grupos}, y si no
 * contesta el mercado deniega (no hay modo degradado que invente un titular).
 */
@Service
public class GestionDeOfertas {

    private final HechosDeGrupos grupos;
    private final RegistroDeOfertas registro;
    private final Reloj reloj;
    private final Duration vigenciaMaxima;

    public GestionDeOfertas(
            HechosDeGrupos grupos,
            RegistroDeOfertas registro,
            Reloj reloj,
            @Value("${aportaya.mercado.vigencia-maxima}") Duration vigenciaMaxima) {
        this.grupos = grupos;
        this.registro = registro;
        this.reloj = reloj;
        this.vigenciaMaxima = vigenciaMaxima;
    }

    public record Entrada(UUID turnoId, Dinero precio, Dinero cargos, OffsetDateTime vigenteHasta, String clave) {}

    public RegistroDeOfertas.Publicada publicar(Entrada e, ContextoSesion ctx) {
        var turno = grupos.turno(e.turnoId())
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(130, 8), "No se pudo confirmar el turno con grupos: no se publica nada."));
        ReglasDeOferta.validarPublicacion(
                turno,
                ctx.usuarioId(),
                e.precio(),
                e.cargos(),
                reloj.ahora(),
                e.vigenteHasta().toInstant(),
                vigenciaMaxima);
        return registro.publicar(
                new RegistroDeOfertas.Publicacion(turno, e.precio(), e.cargos(), e.vigenteHasta(), e.clave()), ctx);
    }

    public boolean cancelar(UUID ofertaId, ContextoSesion ctx) {
        return registro.cancelar(ofertaId, ctx);
    }

    /** Solo ofertas de grupos donde quien mira es miembro activo, vigentes y no vendidas. */
    public List<OfertaVisible> listar(ContextoSesion ctx, int limite, int pagina) {
        var misGrupos = grupos.gruposActivosDe(ctx.usuarioId())
                .orElseThrow(() -> new ErrorDeNegocio(
                        CodigoError.de(130, 8), "No se pudo confirmar tus grupos con grupos: no se lista nada."));
        return registro.listar(misGrupos, ctx, limite, pagina);
    }

    public int vencer(ContextoSesion ctx) {
        return registro.vencerLasVencidas(ctx);
    }

    /** Para la prueba de que la vigencia maxima es un dato y no una constante. */
    OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }
}

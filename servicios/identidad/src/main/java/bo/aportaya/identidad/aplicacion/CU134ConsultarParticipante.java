package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.Enmascarado;
import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import bo.aportaya.identidad.infraestructura.MedidasDeCuentaRepositorio;
import bo.aportaya.identidad.infraestructura.ParticipantesRepositorio;
import bo.aportaya.identidad.infraestructura.ParticipantesRepositorio.Fila;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-134 · directorio y ficha del participante. Toda lectura deja su fila en
 * {@code registro_acceso_datos} en la MISMA transaccion (R-SEG-02): si el rastro no se escribe, no se
 * muestra nada. Nadie se lee a si mismo desde el backoffice (R-SEG-04); revelar un dato exige segundo
 * factor (R-SEG-12). La ficha no devuelve saldo, movimientos ni casos de cumplimiento.
 */
@Service
public class CU134ConsultarParticipante {

    private final Datos datos;
    private final ParticipantesRepositorio participantes;
    private final MedidasDeCuentaRepositorio medidas;
    private final EvidenciaDeStepUp evidencias;
    private final Reloj reloj;

    public CU134ConsultarParticipante(
            Datos datos,
            ParticipantesRepositorio participantes,
            MedidasDeCuentaRepositorio medidas,
            EvidenciaDeStepUp evidencias,
            Reloj reloj) {
        this.datos = datos;
        this.participantes = participantes;
        this.medidas = medidas;
        this.evidencias = evidencias;
        this.reloj = reloj;
    }

    public record Busqueda(
            Optional<String> codigoPublico,
            Optional<String> telefonoE164,
            Optional<String> estado,
            String justificacion,
            String ticketSoporteId,
            Optional<OffsetDateTime> antesDe,
            Optional<UUID> antesDeId,
            int limite) {}

    public record Resumen(Fila fila, String nombreAbreviado) {}

    public record FichaEnmascarada(
            Fila resumen,
            String nombreCompleto,
            OffsetDateTime ultimoAcceso,
            String telefonoEnmascarado,
            String correoEnmascarado,
            List<MedidasDeCuentaRepositorio.Expediente> medidas) {}

    @Transactional
    public List<Resumen> buscar(Busqueda b, String ip, ContextoSesion ctx) {
        exigirJustificacion(b.justificacion());
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> {
            List<Fila> filas = participantes.buscar(
                    dsl, b.codigoPublico(), b.telefonoE164(), b.estado(), b.antesDe(), b.antesDeId(),
                    Math.max(1, Math.min(50, b.limite())), ahora);
            // Una fila de rastro por persona devuelta: el rastro dice a quien se vio, no solo que se busco.
            // Sin resultados no se leyo el dato de nadie.
            for (Fila f : filas) {
                participantes.registrarLectura(dsl, ctx.usuarioId(), f.usuarioId(), "FICHA_PARTICIPANTE",
                        "BUSQUEDA", b.justificacion(), b.ticketSoporteId(), 1, ip, ahora);
            }
            return filas.stream()
                    .map(f -> new Resumen(f, Enmascarado.nombreAbreviado(f.nombres(), f.apellidos())))
                    .toList();
        });
    }

    @Transactional
    public Optional<FichaEnmascarada> ficha(
            UUID usuarioId, String justificacion, String ticketSoporteId, String ip, ContextoSesion ctx) {
        exigirJustificacion(justificacion);
        noSobreSiMismo(ctx, usuarioId);
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> participantes.ficha(dsl, usuarioId, ahora).map(f -> {
            participantes.registrarLectura(dsl, ctx.usuarioId(), usuarioId, "FICHA_PARTICIPANTE", "LECTURA",
                    justificacion, ticketSoporteId, 1, ip, ahora);
            return new FichaEnmascarada(
                    f.resumen(),
                    f.resumen().nombres() + " " + f.resumen().apellidos(),
                    f.ultimoAcceso(),
                    Enmascarado.telefono(f.telefonoE164()),
                    Enmascarado.correo(f.correo()),
                    medidas.deUsuario(dsl, usuarioId));
        }));
    }

    /** Un campo por vez, con segundo factor de quien lee y su propia fila de rastro. */
    @Transactional
    public Optional<String> revelar(
            UUID usuarioId, String campo, String justificacion, String evidenciaMfa, String ip, ContextoSesion ctx) {
        exigirJustificacion(justificacion);
        noSobreSiMismo(ctx, usuarioId);
        if (!evidencias.consumir(ctx.usuarioId(), evidenciaMfa)) {
            throw new ErrorDeNegocio(CodigoError.de(134, 3),
                    "Confirma con tu segundo factor antes de ver un dato completo de otra persona (R-SEG-12).");
        }
        OffsetDateTime ahora = ahora();
        return datos.conContexto(ctx, dsl -> participantes.ficha(dsl, usuarioId, ahora).map(f -> {
            String tipo = "CORREO".equals(campo) ? "CORREO" : "TELEFONO";
            participantes.registrarLectura(dsl, ctx.usuarioId(), usuarioId, tipo, "LECTURA", justificacion,
                    null, 1, ip, ahora);
            return "CORREO".equals(campo) ? f.correo() : f.telefonoE164();
        }));
    }

    /** Para {@code entregas} (D-11): solo un booleano y el expediente, nunca la causal. */
    @Transactional(readOnly = true)
    public Optional<UUID> medidaQueRetiene(UUID usuarioId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> medidas.vigenteQueRetiene(dsl, usuarioId));
    }

    static void exigirJustificacion(String justificacion) {
        if (justificacion == null || justificacion.strip().length() < 10) {
            throw new ErrorDeNegocio(CodigoError.de(134, 1),
                    "Escribi por que necesitas ver estos datos (al menos 10 caracteres): queda en el rastro.");
        }
    }

    static void noSobreSiMismo(ContextoSesion ctx, UUID afectado) {
        if (afectado.equals(ctx.usuarioId())) {
            throw new ErrorDeNegocio(CodigoError.de(134, 2),
                    "Tu propia cuenta de participante la ves en la app; desde el backoffice actua otra persona.");
        }
    }

    private OffsetDateTime ahora() {
        return reloj.ahora().atOffset(ZoneOffset.UTC);
    }
}

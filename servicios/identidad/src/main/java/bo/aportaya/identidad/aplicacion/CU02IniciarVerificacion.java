package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.DocumentoDeIdentidad;
import bo.aportaya.identidad.infraestructura.RegistroRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-02 · la verificacion de quien abrio su cuenta sin documento (2026-10-17).
 *
 * <p>El alta ya no exige el carnet: la persona conoce la app primero y envia su documento
 * cuando quiere operar con dinero. Este caso de uso hace lo que el alta hacia con el
 * documento —validar vigencia y duplicado, guardarlo y abrir la verificacion BASICO— y pasa
 * la cuenta de {@code INCOMPLETO_KYC} a {@code PENDIENTE_VERIFICACION}, todo en una
 * transaccion. La aprobacion sigue siendo de una persona revisora.
 *
 * <p>Quien llama ya comprobo que la sesion es del titular. Corre con el contexto del proceso
 * del alta porque escribe las mismas filas que el alta.
 */
@Service
public class CU02IniciarVerificacion {

    /** El mismo proceso del sistema que el alta (UsuariosController): escribe las mismas filas. */
    public static final UUID PROCESO_DE_ALTA = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final Datos datos;
    private final RegistroRepositorio registros;
    private final Reloj reloj;

    public CU02IniciarVerificacion(Datos datos, RegistroRepositorio registros, Reloj reloj) {
        this.datos = datos;
        this.registros = registros;
        this.reloj = reloj;
    }

    @Transactional
    public UUID ejecutar(UUID usuarioId, DocumentoDeIdentidad documento, String numeroCifrado, String trazaId) {
        if (documento.fechaExpiracion() == null) {
            throw new ErrorDeNegocio(CodigoError.de(1, 10), "Falta la fecha de vencimiento de tu documento.");
        }
        if (documento.vencidoAl(reloj.hoy())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(1, 7), "Tu documento esta vencido. Para verificarte hace falta uno vigente.");
        }
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        ContextoSesion ctx = ContextoSesion.deSistema(PROCESO_DE_ALTA, new Traza(trazaId));
        return datos.conContexto(ctx, dsl -> {
            // Primero la transicion, con la precondicion en el UPDATE: si la cuenta ya tenia
            // su verificacion (o dos envios se cruzan), no se abre una segunda.
            if (!registros.pasarARevisionSiFaltabaDocumento(dsl, usuarioId)) {
                throw new ErrorDeNegocio(CodigoError.de(1, 12), "Tu verificacion ya esta en curso.");
            }
            if (registros.documentoYaRegistrado(dsl, documento.hashNumero(), documento.lugarExpedicion())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(1, 3),
                        "Ya hay una cuenta con ese documento. Podes recuperar el acceso.",
                        Map.of("campo", "documento"));
            }
            UUID documentoId = registros.guardarDocumento(dsl, usuarioId, documento, numeroCifrado);
            return registros.iniciarVerificacion(dsl, usuarioId, documentoId, "BASICO", ahora);
        });
    }
}

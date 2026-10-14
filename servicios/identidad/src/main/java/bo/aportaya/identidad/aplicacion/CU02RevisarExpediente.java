package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.ExpedienteDeIdentidad;
import bo.aportaya.identidad.infraestructura.RevisionRepositorio;
import bo.aportaya.plataforma.archivos.AlmacenDeArchivos;
import bo.aportaya.plataforma.archivos.ClaveObjeto;
import bo.aportaya.plataforma.archivos.ContenidoAlmacenado;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-02 · el portal de riesgo: mirar el expediente y decidir **a mano**.
 *
 * <p>Traza en la boveda: es el flujo 4 de CU-01 (la verificacion KYC del alta, 4b
 * «revision manual del expediente»), porque la decision fija {@code verificacion_kyc.estado}.
 * El prefijo CU02 de esta familia de clases es historico; por eso sus rechazos son
 * {@code AP-CU01-08..11} y no {@code AP-CU02-*}, que en la boveda son de CU-02 (elevar
 * debida diligencia, servicio cumplimiento) y significan otra cosa.
 *
 * <p>Empieza siendo decision manual a proposito. Un motor automatico que rechaza sin
 * que nadie mire deja a alguien sin cuenta y sin explicacion, y el dia que se
 * equivoca nadie sabe por que. Primero decide una persona y queda registrado quien;
 * automatizarlo despues es facil, y auditarlo hacia atras no.
 */
@Service
public class CU02RevisarExpediente {

    /** Lo que dura el enlace de una foto. Corto: es evidencia personal, no una imagen publica. */
    public static final Duration VIGENCIA_DEL_ENLACE = Duration.ofMinutes(10);

    private final RevisionRepositorio revisiones;
    private final AlmacenDeArchivos almacen;
    private final Datos datos;
    private final Reloj reloj;

    public CU02RevisarExpediente(RevisionRepositorio revisiones, AlmacenDeArchivos almacen, Datos datos, Reloj reloj) {
        this.revisiones = revisiones;
        this.almacen = almacen;
        this.datos = datos;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<ExpedienteDeIdentidad> cola(String estado, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> revisiones.enEstado(dsl, estado));
    }

    /**
     * Los bytes de una foto, para que los sirva el servicio dueno.
     *
     * <p>El enlace prefirmado de {@link #foto} se firma contra el almacen, y el
     * almacen vive en la red interna: sirve entre servicios y es inservible en un
     * navegador, que no resuelve {@code minio:9000}. Abrir el almacen al mundo para
     * que cargue una imagen seria exactamente lo que el ADR-034 prohibe.
     */
    @Transactional(readOnly = true)
    public ContenidoAlmacenado contenido(UUID verificacionId, String cara, ContextoSesion ctx) {
        String clave = datos.conContexto(ctx, dsl -> revisiones.claveDeFoto(dsl, verificacionId, cara));
        return almacen.leer(ClaveObjeto.de(clave));
    }

    @Transactional(readOnly = true)
    public Enlace foto(UUID verificacionId, String cara, ContextoSesion ctx) {
        String clave = datos.conContexto(ctx, dsl -> revisiones.claveDeFoto(dsl, verificacionId, cara));
        return new Enlace(
                almacen.urlTemporal(ClaveObjeto.de(clave), VIGENCIA_DEL_ENLACE),
                reloj.ahora().atOffset(ZoneOffset.UTC).plus(VIGENCIA_DEL_ENLACE));
    }

    @Transactional
    public void resolver(UUID verificacionId, String decision, String motivo, ContextoSesion ctx) {
        boolean aprueba = "APROBAR".equals(decision);
        // Rechazar sin motivo no se admite: a quien le rechazan la cuenta tiene
        // derecho a saber por que, y el motivo es lo que hace revisable la decision.
        if (!aprueba && (motivo == null || motivo.isBlank())) {
            throw new ErrorDeDominio("Para rechazar hay que decir por que");
        }
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        LocalDate hoy = reloj.hoy();
        datos.conContexto(ctx, dsl -> {
            // Las reglas se aplican ACA, no solo en el backoffice: la interfaz nunca es
            // una barrera (regla 95.6.1). Antes un APROBAR por API pasaba sin fotos.
            ExpedienteDeIdentidad expediente = revisiones
                    .paraDecidir(dsl, verificacionId)
                    .orElseThrow(() -> new ErrorDeDominio("Ese expediente no existe"));
            if (!expediente.esperaDecision()) {
                throw new ErrorDeNegocio(CodigoError.de(1, 8), "Ese expediente ya fue resuelto.");
            }
            if (aprueba && !expediente.completo()) {
                throw new ErrorDeNegocio(
                        CodigoError.de(1, 9),
                        "Faltan fotos: no se aprueba un expediente que no se puede mirar entero.");
            }
            if (aprueba && expediente.fechaExpiracionDocumento() == null) {
                throw new ErrorDeNegocio(
                        CodigoError.de(1, 10),
                        "El documento no tiene fecha de vencimiento: no se puede comprobar que este vigente.");
            }
            if (aprueba && !expediente.documentoVigente(hoy)) {
                throw new ErrorDeNegocio(
                        CodigoError.de(1, 11), "El documento esta vencido: no se aprueba con un documento vencido.");
            }
            revisiones.resolver(
                    dsl,
                    verificacionId,
                    aprueba ? "APROBADA" : "RECHAZADA",
                    aprueba ? null : motivo,
                    ctx.usuarioId(),
                    ahora);
            return null;
        });
    }

    /** Si el documento del expediente vale hoy (fecha de La Paz). Lo muestra la cola. */
    public boolean documentoVigente(ExpedienteDeIdentidad expediente) {
        return expediente.documentoVigente(reloj.hoy());
    }

    public record Enlace(String url, OffsetDateTime vigenteHasta) {}
}

package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.dominio.CanalDeVerificacion;
import bo.aportaya.identidad.dominio.DocumentoDeIdentidad;
import bo.aportaya.identidad.web.generado.modelo.Documento;
import bo.aportaya.identidad.web.generado.modelo.EntradaRegistro;

/** Del cuerpo del alta a la entrada del caso de uso: traduce, sin logica de negocio. */
final class MapeoDelAlta {

    private MapeoDelAlta() {}

    static CU01RegistrarUsuario.EntradaRegistro mapear(
            EntradaRegistro cuerpo, String pimienta, String ip, String agente) {
        // Sin documento, la cuenta se abre y la verificacion se hace despues.
        DocumentoDeIdentidad documento =
                cuerpo.getDocumento() == null ? null : documentoDe(cuerpo.getDocumento(), pimienta);
        return new CU01RegistrarUsuario.EntradaRegistro(
                cuerpo.getTelefonoE164(),
                cuerpo.getNombres(),
                cuerpo.getApellidos(),
                cuerpo.getFechaNacimiento(),
                cuerpo.getCorreo(),
                // El correo es obligatorio en el alta y es el unico canal habilitado.
                cuerpo.getCanalVerificacion() == null
                        ? CanalDeVerificacion.CORREO
                        : CanalDeVerificacion.valueOf(
                                cuerpo.getCanalVerificacion().getValue()),
                cuerpo.getVerificacionCorreoId(),
                documento,
                documento == null ? null : numeroCifradoDe(documento),
                // `char[]` y no `String`: una `String` de clave queda en el pool hasta
                // que el recolector pase, y no hay forma de borrarla antes. El caso de
                // uso limpia este arreglo apenas la hashea.
                cuerpo.getContrasena().toCharArray(),
                cuerpo.getAceptaContratos(),
                true,
                ip,
                agente);
    }

    /** El documento del contrato al del dominio: el alta y {@code iniciarVerificacion} lo comparten. */
    static DocumentoDeIdentidad documentoDe(Documento documento, String pimienta) {
        return DocumentoDeIdentidad.de(
                DocumentoDeIdentidad.Tipo.valueOf(tipoDelModelo(documento.getTipo().getValue())),
                documento.getNumero(),
                pimienta,
                "BO",
                // La extension solo la lleva el CI; el contrato la deja opcional
                // porque depende del tipo, y el dominio es el que la exige.
                documento.getLugarExpedicion() == null
                        ? null
                        : documento.getLugarExpedicion().getValue(),
                documento.getFechaExpiracion());
    }

    /** El cifrado real lo hace el adaptador de archivos; aca la frontera. */
    static String numeroCifradoDe(DocumentoDeIdentidad documento) {
        return "cifrado:" + documento.hashNumero();
    }

    /** El contrato dice {@code CEX}; el {@code .puml} dice {@code CARNET_EXTRANJERIA}. */
    private static String tipoDelModelo(String tipoDelContrato) {
        return "CEX".equals(tipoDelContrato) ? "CARNET_EXTRANJERIA" : tipoDelContrato;
    }
}

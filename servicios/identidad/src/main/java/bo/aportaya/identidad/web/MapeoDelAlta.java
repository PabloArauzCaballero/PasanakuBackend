package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU01RegistrarUsuario;
import bo.aportaya.identidad.dominio.CanalDeVerificacion;
import bo.aportaya.identidad.dominio.DocumentoDeIdentidad;
import bo.aportaya.identidad.web.generado.modelo.EntradaRegistro;

/** Del cuerpo del alta a la entrada del caso de uso: traduce, sin logica de negocio. */
final class MapeoDelAlta {

    private MapeoDelAlta() {}

    static CU01RegistrarUsuario.EntradaRegistro mapear(
            EntradaRegistro cuerpo, String pimienta, String ip, String agente) {
        DocumentoDeIdentidad documento = DocumentoDeIdentidad.de(
                DocumentoDeIdentidad.Tipo.valueOf(
                        tipoDelModelo(cuerpo.getDocumento().getTipo().getValue())),
                cuerpo.getDocumento().getNumero(),
                pimienta,
                "BO",
                // La extension solo la lleva el CI; el contrato la deja opcional
                // porque depende del tipo, y el dominio es el que la exige.
                cuerpo.getDocumento().getLugarExpedicion() == null
                        ? null
                        : cuerpo.getDocumento().getLugarExpedicion().getValue(),
                cuerpo.getDocumento().getFechaExpiracion());
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
                // El cifrado real lo hace el adaptador de archivos; aca la frontera.
                "cifrado:" + documento.hashNumero(),
                // `char[]` y no `String`: una `String` de clave queda en el pool hasta
                // que el recolector pase, y no hay forma de borrarla antes. El caso de
                // uso limpia este arreglo apenas la hashea.
                cuerpo.getContrasena().toCharArray(),
                cuerpo.getAceptaContratos(),
                true,
                ip,
                agente);
    }

    /** El contrato dice {@code CEX}; el {@code .puml} dice {@code CARNET_EXTRANJERIA}. */
    private static String tipoDelModelo(String tipoDelContrato) {
        return "CEX".equals(tipoDelContrato) ? "CARNET_EXTRANJERIA" : tipoDelContrato;
    }
}

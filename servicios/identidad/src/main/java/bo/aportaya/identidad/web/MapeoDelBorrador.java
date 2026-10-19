package bo.aportaya.identidad.web;

import bo.aportaya.identidad.dominio.BorradorOnboardingDatos;
import bo.aportaya.identidad.web.generado.modelo.BorradorOnboarding;

/** Convierte entre el dominio {@link BorradorOnboardingDatos} y el modelo generado {@link BorradorOnboarding}. */
final class MapeoDelBorrador {

    private MapeoDelBorrador() {}

    static BorradorOnboarding aModelo(BorradorOnboardingDatos d) {
        if (d == null) {
            return null;
        }
        var m = new BorradorOnboarding();
        m.setCorreo(d.correo());
        m.setTelefonoE164(d.telefonoE164());
        m.setNombres(d.nombres());
        m.setApellidos(d.apellidos());
        m.setFechaNacimiento(d.fechaNacimiento());
        if (d.tipoDocumento() != null) {
            try {
                m.setTipoDocumento(BorradorOnboarding.TipoDocumentoEnum.fromValue(d.tipoDocumento()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        m.setNumeroDocumento(d.numeroDocumento());
        m.setLugarExpedicion(d.lugarExpedicion());
        m.setFechaExpiracion(d.fechaExpiracion());
        m.setActividadEconomica(d.actividadEconomica());
        m.setDetalleActividad(d.detalleActividad());
        m.setOrigenDeFondos(d.origenDeFondos());
        m.setDetalleOrigen(d.detalleOrigen());
        m.setMontoMensualEstimado(d.montoMensualEstimado());
        if (d.pasoPendiente() != null) {
            try {
                m.setPasoPendiente(BorradorOnboarding.PasoPendienteEnum.fromValue(
                        d.pasoPendiente().toLowerCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return m;
    }

    static BorradorOnboardingDatos aDominio(BorradorOnboarding cuerpo) {
        return new BorradorOnboardingDatos(
                cuerpo.getCorreo(),
                cuerpo.getTelefonoE164(),
                cuerpo.getNombres(),
                cuerpo.getApellidos(),
                cuerpo.getFechaNacimiento(),
                cuerpo.getTipoDocumento() != null ? cuerpo.getTipoDocumento().getValue() : null,
                cuerpo.getNumeroDocumento(),
                cuerpo.getLugarExpedicion(),
                cuerpo.getFechaExpiracion(),
                cuerpo.getActividadEconomica(),
                cuerpo.getDetalleActividad(),
                cuerpo.getOrigenDeFondos(),
                cuerpo.getDetalleOrigen(),
                cuerpo.getMontoMensualEstimado(),
                cuerpo.getPasoPendiente() != null ? cuerpo.getPasoPendiente().getValue() : null);
    }
}

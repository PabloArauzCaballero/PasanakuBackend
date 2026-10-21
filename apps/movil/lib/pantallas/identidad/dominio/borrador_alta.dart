import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';

import 'modelo_alta.dart';

/// Funciones puras para mapear entre el estado del alta y el borrador de onboarding del servidor.
BorradorOnboarding aBorradorOnboarding(EstadoAlta state) {
  final tipoDoc = switch (state.datos.tipoDocumento) {
    'PASAPORTE' => BorradorOnboardingTipoDocumentoEnum.PASAPORTE,
    'CARNET_EXTRANJERO' =>
      BorradorOnboardingTipoDocumentoEnum.CARNET_EXTRANJERO,
    _ => BorradorOnboardingTipoDocumentoEnum.CI,
  };
  final paso = switch (state.paso) {
    PasoAlta.cuenta => BorradorOnboardingPasoPendienteEnum.cuenta,
    PasoAlta.correo => BorradorOnboardingPasoPendienteEnum.correo,
    PasoAlta.documento => BorradorOnboardingPasoPendienteEnum.documento,
    PasoAlta.datos => BorradorOnboardingPasoPendienteEnum.datos,
    PasoAlta.actividad => BorradorOnboardingPasoPendienteEnum.actividad,
    PasoAlta.contrato => BorradorOnboardingPasoPendienteEnum.contrato,
  };
  return BorradorOnboarding(
    correo: state.datos.correo.isNotEmpty ? state.datos.correo : null,
    telefonoE164: state.datos.telefono.isNotEmpty ? state.datos.telefono : null,
    nombres: state.datos.nombres.isNotEmpty ? state.datos.nombres : null,
    apellidos: state.datos.apellidos.isNotEmpty ? state.datos.apellidos : null,
    fechaNacimiento: state.datos.fechaNacimiento,
    tipoDocumento: tipoDoc,
    numeroDocumento: state.datos.numeroDocumento.isNotEmpty
        ? state.datos.numeroDocumento
        : null,
    lugarExpedicion: state.datos.lugarExpedicion,
    fechaExpiracion: state.datos.fechaExpiracion,
    actividadEconomica: state.actividadEconomica.isNotEmpty
        ? state.actividadEconomica
        : null,
    detalleActividad: state.detalleDeLaActividad.isNotEmpty
        ? state.detalleDeLaActividad
        : null,
    origenDeFondos: state.origenDeFondos.isNotEmpty
        ? state.origenDeFondos
        : null,
    detalleOrigen: state.detalleDelOrigen.isNotEmpty
        ? state.detalleDelOrigen
        : null,
    montoMensualEstimado: state.montoMensualEstimado?.toString(),
    pasoPendiente: paso,
  );
}

EstadoAlta restaurarEstadoDesdeBorrador(
  EstadoAlta state,
  BorradorOnboarding borrador,
) {
  var nuevosDatos = state.datos;
  if (borrador.correo != null && borrador.correo!.isNotEmpty) {
    nuevosDatos = nuevosDatos.copiarCon(correo: borrador.correo);
  }
  if (borrador.telefonoE164 != null && borrador.telefonoE164!.isNotEmpty) {
    nuevosDatos = nuevosDatos.copiarCon(telefono: borrador.telefonoE164);
  }
  if (borrador.nombres != null && borrador.nombres!.isNotEmpty) {
    nuevosDatos = nuevosDatos.copiarCon(nombres: borrador.nombres);
  }
  if (borrador.apellidos != null && borrador.apellidos!.isNotEmpty) {
    nuevosDatos = nuevosDatos.copiarCon(apellidos: borrador.apellidos);
  }
  if (borrador.fechaNacimiento != null) {
    nuevosDatos = nuevosDatos.copiarCon(
      fechaNacimiento: borrador.fechaNacimiento,
    );
  }
  if (borrador.tipoDocumento != null) {
    nuevosDatos = nuevosDatos.copiarCon(
      tipoDocumento: borrador.tipoDocumento!.value,
    );
  }
  if (borrador.numeroDocumento != null &&
      borrador.numeroDocumento!.isNotEmpty) {
    nuevosDatos = nuevosDatos.copiarCon(
      numeroDocumento: borrador.numeroDocumento,
    );
  }
  if (borrador.lugarExpedicion != null &&
      borrador.lugarExpedicion!.isNotEmpty) {
    nuevosDatos = nuevosDatos.copiarCon(
      lugarExpedicion: borrador.lugarExpedicion,
    );
  }
  if (borrador.fechaExpiracion != null) {
    nuevosDatos = nuevosDatos.copiarCon(
      fechaExpiracion: borrador.fechaExpiracion,
    );
  }

  final pasoPendiente = switch (borrador.pasoPendiente) {
    BorradorOnboardingPasoPendienteEnum.datos => PasoAlta.datos,
    BorradorOnboardingPasoPendienteEnum.actividad => PasoAlta.actividad,
    BorradorOnboardingPasoPendienteEnum.contrato => PasoAlta.contrato,
    _ => PasoAlta.documento,
  };

  return state.copiarCon(
    datos: nuevosDatos,
    codigoConfirmado: true,
    paso: pasoPendiente,
    actividadEconomica: borrador.actividadEconomica ?? state.actividadEconomica,
    detalleDeLaActividad:
        borrador.detalleActividad ?? state.detalleDeLaActividad,
    origenDeFondos: borrador.origenDeFondos ?? state.origenDeFondos,
    detalleDelOrigen: borrador.detalleOrigen ?? state.detalleDelOrigen,
    montoMensualEstimado: borrador.montoMensualEstimado != null
        ? double.tryParse(borrador.montoMensualEstimado!) ??
              state.montoMensualEstimado
        : state.montoMensualEstimado,
  );
}

import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'cu02_consultar_verificacion.dart';

/// Resumen del KYC propio para el Perfil. El backend vuelve a validar la sesión y titularidad.
final kycDelTitularProvider =
    FutureProvider.family<EstadoDeVerificacion, String>((ref, usuarioId) {
      return ref.read(consultarVerificacionProvider).ejecutar(usuarioId);
    });

List<String> pendientesDeKyc(EstadoDeVerificacion estado) {
  final faltantes = <String>[];
  if (!estado.fotos.contains(CaraDelExpediente.PERFIL_IZQUIERDO)) {
    faltantes.add('Foto de perfil izquierdo');
  }
  if (!estado.fotos.contains(CaraDelExpediente.PERFIL_DERECHO)) {
    faltantes.add('Foto de perfil derecho');
  }
  if (estado.fechaExpiracionDocumento == null) {
    faltantes.add('Fecha de vencimiento del documento');
  }
  return faltantes;
}

bool kycAdmiteCorreccion(EstadoDeVerificacion estado) =>
    estado.estado == EstadoDeVerificacionEstadoEnum.PENDIENTE ||
    estado.estado == EstadoDeVerificacionEstadoEnum.EN_REVISION;

String etiquetaEstadoKyc(EstadoDeVerificacionEstadoEnum estado) =>
    switch (estado) {
      EstadoDeVerificacionEstadoEnum.PENDIENTE => 'Pendiente de revisión',
      EstadoDeVerificacionEstadoEnum.EN_REVISION => 'En revisión humana',
      EstadoDeVerificacionEstadoEnum.APROBADA => 'Aprobada',
      EstadoDeVerificacionEstadoEnum.RECHAZADA => 'Rechazada',
    };

import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/kyc_del_titular.dart';

void main() {
  EstadoDeVerificacion expediente({
    EstadoDeVerificacionEstadoEnum estado =
        EstadoDeVerificacionEstadoEnum.EN_REVISION,
    List<CaraDelExpediente> fotos = const [],
    DateTime? vencimiento,
  }) => EstadoDeVerificacion(
    verificacionId: 'solo-prueba',
    estado: estado,
    fotos: fotos,
    fechaExpiracionDocumento: vencimiento,
  );

  test('enumera únicamente las fotos de perfil y fecha que faltan', () {
    final faltantes = pendientesDeKyc(
      expediente(
        fotos: const [CaraDelExpediente.ANVERSO, CaraDelExpediente.SELFIE],
      ),
    );

    expect(faltantes, [
      'Foto de perfil izquierdo',
      'Foto de perfil derecho',
      'Fecha de vencimiento del documento',
    ]);
  });

  test('no pide completar cuando la evidencia ya está completa', () {
    final faltantes = pendientesDeKyc(
      expediente(
        fotos: CaraDelExpediente.values.toList(),
        vencimiento: DateTime(2030, 1, 15),
      ),
    );

    expect(faltantes, isEmpty);
  });

  test(
    'solo permite correcciones mientras la revisión humana sigue abierta',
    () {
      expect(kycAdmiteCorreccion(expediente()), isTrue);
      expect(
        kycAdmiteCorreccion(
          expediente(estado: EstadoDeVerificacionEstadoEnum.PENDIENTE),
        ),
        isTrue,
      );
      expect(
        kycAdmiteCorreccion(
          expediente(estado: EstadoDeVerificacionEstadoEnum.APROBADA),
        ),
        isFalse,
      );
      expect(
        kycAdmiteCorreccion(
          expediente(estado: EstadoDeVerificacionEstadoEnum.RECHAZADA),
        ),
        isFalse,
      );
    },
  );
}

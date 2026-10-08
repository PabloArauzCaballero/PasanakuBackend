import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/fallo_consulta_invitacion.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  FalloConsultaInvitacion para(String codigo, int estado) =>
      FalloConsultaInvitacion.desde(
        ErrorDeApi(codigo: codigo, trazaId: 'sintetica', estado: estado),
      );

  test('sesión vencida vuelve al ingreso, sin mostrar un error de enlace', () {
    final fallo = para('AP-DESCONOCIDO', 401);
    expect(fallo.requiereIngreso, isTrue);
    expect(fallo.mensaje, isNull);
  });

  test('enlace vencido no ofrece un reintento inútil', () {
    final fallo = para('AP-CU69-05', 422);
    expect(fallo.recuperable, isFalse);
    expect(fallo.mensaje, contains('enlace nuevo'));
  });

  test('sin permiso no expone datos ni ofrece reintento', () {
    final fallo = para('AP-DESCONOCIDO', 403);
    expect(fallo.recuperable, isFalse);
    expect(fallo.mensaje, contains('No tenés acceso'));
  });

  test('fallo de servidor permite reintentar sin exponer respuesta cruda', () {
    final fallo = para('AP-DESCONOCIDO', 500);
    expect(fallo.recuperable, isTrue);
    expect(
      fallo.mensaje,
      'No pudimos verificar la invitación. Probá de nuevo.',
    );
  });

  test('timeout permite reintentar', () {
    final fallo = FalloConsultaInvitacion.desde(const ErrorDeTiempoDeEspera());
    expect(fallo.recuperable, isTrue);
    expect(fallo.mensaje, contains('Probá de nuevo'));
  });
}

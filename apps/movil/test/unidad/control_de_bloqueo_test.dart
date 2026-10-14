import 'package:aportaya_movil/dominio/vigilante_de_sesion.dart';
import 'package:aportaya_movil/proveedores/bloqueo.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test(
    'bloquear publica el motivo y notifica a quienes observan el estado',
    () {
      final control = ControlDeBloqueo();
      var avisos = 0;
      control.addListener(() => avisos++);

      control.bloquear(MotivoDeBloqueo.inactividad);

      expect(control.bloqueada, isTrue);
      expect(control.motivo, MotivoDeBloqueo.inactividad);
      expect(avisos, 1);
      control.dispose();
    },
  );

  test(
    'bloquear de nuevo no repite notificaciones ni cambia el primer motivo',
    () {
      final control = ControlDeBloqueo();
      var avisos = 0;
      control.addListener(() => avisos++);
      control.bloquear(MotivoDeBloqueo.arranque);

      control.bloquear(MotivoDeBloqueo.segundoPlano);

      expect(control.motivo, MotivoDeBloqueo.arranque);
      expect(avisos, 1);
      control.dispose();
    },
  );

  test('desbloquear limpia el motivo y notifica el cambio', () {
    final control = ControlDeBloqueo();
    control.bloquear(MotivoDeBloqueo.segundoPlano);
    var avisos = 0;
    control.addListener(() => avisos++);

    control.desbloquear();

    expect(control.bloqueada, isFalse);
    expect(control.motivo, isNull);
    expect(avisos, 1);
    control.dispose();
  });

  test('al arrancar bloquea con motivo arranque fuera de web', () {
    final control = ControlDeBloqueo(esWeb: false);

    control.bloquearAlArrancar();

    expect(control.bloqueada, isTrue);
    expect(control.motivo, MotivoDeBloqueo.arranque);
    control.dispose();
  });

  test('al arrancar no bloquea en web', () {
    final control = ControlDeBloqueo(esWeb: true);

    control.bloquearAlArrancar();

    expect(control.bloqueada, isFalse);
    expect(control.motivo, isNull);
    control.dispose();
  });

  test('olvidar borra el bloqueo al cerrar la sesión', () {
    final control = ControlDeBloqueo();
    control.bloquear(MotivoDeBloqueo.inactividad);
    var avisos = 0;
    control.addListener(() => avisos++);

    control.olvidar();

    expect(control.bloqueada, isFalse);
    expect(control.motivo, isNull);
    expect(avisos, 1);
    control.dispose();
  });

  test('olvidar sin bloqueo no notifica un cambio inexistente', () {
    final control = ControlDeBloqueo();
    var avisos = 0;
    control.addListener(() => avisos++);

    control.olvidar();

    expect(avisos, 0);
    control.dispose();
  });

  test(
    'un regreso al frente señala un nuevo episodio sin retirar el candado',
    () {
      final control = ControlDeBloqueo(esWeb: false)
        ..bloquear(MotivoDeBloqueo.segundoPlano);
      var avisos = 0;
      control.addListener(() => avisos++);

      control.nuevoEpisodioBiometrico();

      expect(control.bloqueada, isTrue);
      expect(control.episodioBiometrico, 1);
      expect(avisos, 1);
      control.dispose();
    },
  );

  test('el provider publica los cambios del control', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    var avisos = 0;
    final suscripcion = container.listen(bloqueoProvider, (_, _) => avisos++);
    addTearDown(suscripcion.close);

    container.read(bloqueoProvider).bloquear(MotivoDeBloqueo.arranque);

    expect(container.read(bloqueoProvider).bloqueada, isTrue);
    expect(avisos, 1);
  });
}

import 'dart:io';

const _paquete = 'bo.aportaya.aportaya_movil';

Future<int> _patrol(
  String destino,
  String dispositivo,
  List<String> extra,
) async {
  stdout.writeln('Patrol: $destino');
  final proceso = await Process.start(Platform.resolvedExecutable, [
    'scripts/ejecutar_patrol.dart',
    '--no-uninstall',
    '--target',
    destino,
    '--device',
    dispositivo,
    ...extra,
  ], mode: ProcessStartMode.inheritStdio);
  return proceso.exitCode;
}

Future<ProcessResult> _adb(String dispositivo, List<String> argumentos) =>
    Process.run('adb', ['-s', dispositivo, ...argumentos]);

Future<bool> _procesoVivo(String dispositivo) async {
  final resultado = await _adb(dispositivo, ['shell', 'pidof', _paquete]);
  return resultado.exitCode == 0 &&
      (resultado.stdout as String).trim().isNotEmpty;
}

Future<void> main(List<String> argumentos) async {
  if (argumentos.isEmpty || !argumentos.first.startsWith('emulator-')) {
    stderr.writeln(
      'Uso: dart scripts/verificar_reinicio_android.dart '
      'emulator-5554 [argumentos de Patrol]. Solo se permite un emulador.',
    );
    exitCode = 64;
    return;
  }

  final dispositivo = argumentos.first;
  final extra = argumentos.skip(1).toList();
  if (extra.any((argumento) => !argumento.startsWith('--dart-define='))) {
    stderr.writeln('Este gate solo acepta --dart-define=CLAVE=VALOR.');
    exitCode = 64;
    return;
  }
  try {
    final qemu = await _adb(dispositivo, ['shell', 'getprop', 'ro.boot.qemu']);
    if (qemu.exitCode != 0 || (qemu.stdout as String).trim() != '1') {
      throw StateError('El dispositivo no confirmó ser un emulador Android.');
    }

    final sembrar = await _patrol(
      'integration_test/reinicio_sembrar_test.dart',
      dispositivo,
      extra,
    );
    if (sembrar != 0) {
      exitCode = sembrar;
      return;
    }

    // El APK de Patrol puede terminar su proceso al cerrar JUnit. Abrimos
    // explícitamente uno nuevo para demostrar que force-stop mata uno vivo.
    final apertura = await _adb(dispositivo, [
      'shell',
      'am',
      'start',
      '-n',
      '$_paquete/.MainActivity',
    ]);
    if (apertura.exitCode != 0) {
      throw StateError('No se pudo abrir el APK antes de force-stop.');
    }

    var vivo = false;
    for (var intento = 0; intento < 20; intento++) {
      vivo = await _procesoVivo(dispositivo);
      if (vivo) break;
      await Future<void>.delayed(const Duration(milliseconds: 250));
    }
    if (!vivo) {
      throw StateError('No hubo proceso vivo antes de force-stop.');
    }
    stdout.writeln('Proceso Android vivo antes de force-stop: sí');

    final cierre = await _adb(dispositivo, [
      'shell',
      'am',
      'force-stop',
      _paquete,
    ]);
    if (cierre.exitCode != 0 || await _procesoVivo(dispositivo)) {
      throw StateError('force-stop no confirmó el cierre del proceso.');
    }
    stdout.writeln('Proceso Android vivo después de force-stop: no');

    exitCode = await _patrol(
      'integration_test/reinicio_verificar_test.dart',
      dispositivo,
      extra,
    );
    if (exitCode == 0) {
      stdout.writeln('Gate reinicio Android: 2/2 fases PASS.');
    }
  } on Object catch (error) {
    stderr.writeln('Gate de reinicio Android falló: $error');
    exitCode = 1;
  }
}

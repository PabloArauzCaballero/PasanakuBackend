import 'dart:convert';
import 'dart:io';

/// Patrol puede terminar con código 0 aunque Android haya ejecutado cero tests.
/// Ese resultado no satisface el gate E2E.
int? totalDePruebasPatrol(String salida) {
  final coincidencias = RegExp(r'Total:\s*(\d+)').allMatches(salida);
  return coincidencias.isEmpty ? null : int.parse(coincidencias.last.group(1)!);
}

int codigoDeSalidaPatrol(int codigoProceso, String salida) {
  if (codigoProceso != 0) return codigoProceso;
  final total = totalDePruebasPatrol(salida);
  return total == null || total == 0 ? 1 : 0;
}

Future<void> main(List<String> argumentos) async {
  final proceso = await Process.start(Platform.resolvedExecutable, [
    'pub',
    'global',
    'run',
    'patrol_cli:main',
    'test',
    ...argumentos,
  ]);
  final salida = StringBuffer();
  await Future.wait<void>([
    proceso.stdout.transform(utf8.decoder).forEach((parte) {
      salida.write(parte);
      stdout.write(parte);
    }),
    proceso.stderr.transform(utf8.decoder).forEach((parte) {
      salida.write(parte);
      stderr.write(parte);
    }),
  ]);

  final codigoProceso = await proceso.exitCode;
  final codigoFinal = codigoDeSalidaPatrol(codigoProceso, salida.toString());
  if (codigoProceso == 0 && codigoFinal != 0) {
    stderr.writeln('Patrol no ejecutó ninguna prueba; el gate E2E falla.');
  }
  exitCode = codigoFinal;
}

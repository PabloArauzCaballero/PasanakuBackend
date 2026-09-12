import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';

/// CU-60 · sortear los turnos, en sus dos fases: comprometer (solo el hash) y
/// revelar (la semilla, que crea los turnos). Nunca se junta en un paso: sin esa
/// separación quien ejecuta el sorteo podría probar semillas hasta que le convenga.
class ComprometerSorteo extends AsyncNotifier<CompromisoDeSorteo?> {
  static String formularioId(String grupoId) => 'cu60-comprometer-$grupoId';

  @override
  Future<CompromisoDeSorteo?> build() async => null;

  Future<void> enviar({
    required String grupoId,
    List<String>? entropias,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<CompromisoDeSorteo?>();
    final dio = ref.read(dioProvider);
    final clave = ref
        .read(idempotenciaProvider.notifier)
        .claveDe(formularioId(grupoId));
    try {
      final r = await DefaultApi(dio).comprometerSorteo(
        grupoId: grupoId,
        idempotencyKey: clave,
        entradaCompromiso: entropias == null
            ? null
            : EntradaCompromiso(entropias: entropias),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(formularioId(grupoId));
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final comprometerSorteoProvider =
    AsyncNotifierProvider<ComprometerSorteo, CompromisoDeSorteo?>(
      ComprometerSorteo.new,
    );

class RevelarSorteo extends AsyncNotifier<RevelacionDeSorteo?> {
  static String formularioId(String sorteoId) => 'cu60-revelar-$sorteoId';

  @override
  Future<RevelacionDeSorteo?> build() async => null;

  Future<void> enviar({
    required String grupoId,
    required String sorteoId,
    required String semilla,
    List<String>? entropias,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<RevelacionDeSorteo?>();
    final dio = ref.read(dioProvider);
    final clave = ref
        .read(idempotenciaProvider.notifier)
        .claveDe(formularioId(sorteoId));
    try {
      final r = await DefaultApi(dio).revelarSorteo(
        grupoId: grupoId,
        idempotencyKey: clave,
        entradaRevelacion: EntradaRevelacion(
          sorteoId: sorteoId,
          semilla: semilla,
          entropias: entropias,
        ),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(formularioId(sorteoId));
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final revelarSorteoProvider =
    AsyncNotifierProvider<RevelarSorteo, RevelacionDeSorteo?>(
      RevelarSorteo.new,
    );

/// CU-61 · el paquete publicado, para que cualquiera lo reproduzca. Antes del
/// revelado `semillaRevelada` viaja nula: eso es el punto del protocolo.
final paqueteDelSorteoProvider = FutureProvider.autoDispose
    .family<PaqueteDelSorteo, String>((ref, sorteoId) async {
      final dio = ref.watch(dioProvider);
      try {
        final r = await DefaultApi(
          dio,
        ).consultarPaqueteDelSorteo(sorteoId: sorteoId);
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

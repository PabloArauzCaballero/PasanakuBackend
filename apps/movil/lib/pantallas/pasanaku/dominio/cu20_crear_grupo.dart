import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_cliente_organizador/aportaya_cliente_organizador.dart'
    as org;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';

/// CU-20 · crear grupo y congelar tarifario. El grupo nace `BORRADOR`: esta pantalla
/// solo dispara la creación, nunca muestra un precio propio — el que se congela lo
/// calcula `tarifas` y llega en `fondoPorPeriodo` (invariante 10, cero constantes
/// de tarifa en el cliente).
class CrearGrupo extends AsyncNotifier<SalidaGrupo?> {
  static const formularioId = 'cu20-crear-grupo';

  @override
  Future<SalidaGrupo?> build() async => null;

  Future<void> enviar({
    required String nombre,
    required String montoAporte,
    required EntradaGrupoPeriodicidadEnum periodicidad,
    required int cupos,
    required int diaCobro,
    required EntradaGrupoModalidadTurnosEnum modalidadTurnos,
    required DateTime fechaDeInicio,
    String? organizadorId,
    bool permitePermutaDeTurnos = false,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<SalidaGrupo?>();
    final dio = ref.read(dioProvider);
    final clave = ref.read(idempotenciaProvider.notifier).claveDe(formularioId);
    try {
      final r = await DefaultApi(dio).crearGrupo(
        idempotencyKey: clave,
        entradaGrupo: EntradaGrupo(
          nombre: nombre,
          montoAporte: Dinero(monto: montoAporte, moneda: DineroMonedaEnum.BOB),
          periodicidad: periodicidad,
          cupos: cupos,
          diaCobro: diaCobro,
          modalidadTurnos: modalidadTurnos,
          fechaDeInicio: fechaDeInicio,
          organizadorId: organizadorId,
          permitePermutaDeTurnos: permitePermutaDeTurnos,
        ),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(formularioId);
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final crearGrupoProvider = AsyncNotifierProvider<CrearGrupo, SalidaGrupo?>(
  CrearGrupo.new,
);

/// **Hueco declarado (D-16):** el contrato de `organizador`
/// (`GET /organizadores/{id}/habilitacion`) devuelve `habilitado`, `nivel` y los
/// límites del nivel, pero NO la lista descompuesta de los 14 requisitos con
/// cumplido/faltante y umbral por ítem — esa forma (`Faltante[]`) solo viaja en
/// `SalidaHabilitacion`, la RESPUESTA de una mutación (`habilitarOrganizador`,
/// `aprobarPostulacion`), no algo que se pueda pedir de antemano sin disparar un
/// efecto. Por eso esta pantalla muestra estado y límites vigentes, y NO el listado
/// ítem por ítem que pide la maqueta; se pide a `organizador` un endpoint de solo
/// lectura que devuelva los 14 ítems (`planes/informes/carril-M3.md`).
final habilitacionOrganizadorProvider = FutureProvider.autoDispose
    .family<org.Habilitacion, String>((ref, organizadorId) async {
      final dio = ref.watch(dioProvider);
      try {
        final r = await org.DefaultApi(
          dio,
        ).consultarHabilitacion(organizadorId: organizadorId);
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

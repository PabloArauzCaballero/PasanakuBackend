import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_cliente_organizador/aportaya_cliente_organizador.dart'
    as org;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart';
import 'etapa_minima.dart';

/// CU-20 · crear grupo y congelar tarifario. El grupo nace `BORRADOR`: esta pantalla
/// solo dispara la creación, nunca muestra un precio propio — el que se congela lo
/// calcula `tarifas` y llega en `fondoPorPeriodo` (invariante 10, cero constantes
/// de tarifa en el cliente).
class CrearGrupo extends AsyncNotifier<SalidaGrupo?> {
  static const formularioId = 'cu20-crear-grupo';

  @override
  Future<SalidaGrupo?> build() async {
    ref.watch(generacionDeSesionProvider);
    // La etapa elegida vive lo que vive el formulario, no lo que vive su selector: el
    // selector está en un ListView perezoso y se desmonta al desplazarse. Sin esta
    // escucha la elección se liberaba y `enviar` mandaba la etapa propia, más alta.
    ref.listen(etapaMinimaElegidaProvider, (_, _) {});
    return null;
  }

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
    // La elige el selector del formulario; nula = la etapa actual de quien organiza.
    final etapaMinima = ref.read(etapaMinimaElegidaProvider);
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
          etapaMinima: etapaMinima,
        ),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(formularioId);
      if (!ref.mounted) return;
      state = AsyncData(r.data);
    } catch (e) {
      if (!ref.mounted) return;
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

/// `autoDispose` (2026-10-20): al salir de la pantalla el resultado se olvida. Antes era
/// global y «Organizar un grupo nuevo» volvía a mostrar «Grupo creado» del anterior en
/// vez del formulario vacío.
final crearGrupoProvider =
    AsyncNotifierProvider.autoDispose<CrearGrupo, SalidaGrupo?>(CrearGrupo.new);

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
      ref.watch(generacionDeSesionProvider);
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

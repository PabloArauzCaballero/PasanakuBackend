import 'package:aportaya_cliente_entregas/aportaya_cliente_entregas.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../dominio/errores.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart';

/// CU-64 por subasta (2026-10-30): el titular pone su cupo en subasta, cualquiera
/// puja con su saldo (queda retenido) y al cierre gana la más alta. Sin votación.

/// Las subastas abiertas, para cualquier persona (la lista del inicio y la pantalla).
final subastasAbiertasProvider =
    FutureProvider.autoDispose<List<VistaDeSubasta>>((ref) async {
      ref.watch(generacionDeSesionProvider);
      try {
        final r = await DefaultApi(
          ref.read(dioProvider),
        ).listarSubastasDeCupo();
        return r.data?.subastas ?? const [];
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

/// La última subasta de un cupo, en cualquier estado (la ve el saliente).
final subastaDelCupoProvider = FutureProvider.autoDispose
    .family<VistaDeSubasta?, String>((ref, cupoId) async {
      ref.watch(generacionDeSesionProvider);
      try {
        final r = await DefaultApi(
          ref.read(dioProvider),
        ).listarSubastasDeCupo(cupoId: cupoId);
        final lista = r.data?.subastas ?? const [];
        return lista.isEmpty ? null : lista.first;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

/// Una subasta, con la puja más alta y la mía.
final subastaProvider = FutureProvider.autoDispose
    .family<VistaDeSubasta, String>((ref, subastaId) async {
      ref.watch(generacionDeSesionProvider);
      try {
        final r = await DefaultApi(
          ref.read(dioProvider),
        ).consultarSubastaDeCupo(subastaId: subastaId);
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

typedef PedidoDeSubasta = ({
  String grupoId,
  String cupoId,
  String montoBase,
  int horas,
});

/// Abrir la subasta. Una clave por intento (como el traspaso): un fallo de red reusa
/// la clave; un rechazo de regla (422) cierra el intento.
class AbrirSubasta extends AsyncNotifier<VistaDeSubasta?> {
  @override
  Future<VistaDeSubasta?> build() async {
    ref.watch(generacionDeSesionProvider);
    return null;
  }

  Future<void> enviar(PedidoDeSubasta p) async {
    if (state.isLoading) return;
    final claves = ref.read(idempotenciaProvider.notifier);
    final formulario = 'cu64-subasta-${p.cupoId}-${p.montoBase}-${p.horas}';
    state = const AsyncLoading<VistaDeSubasta?>();
    try {
      final r = await DefaultApi(ref.read(dioProvider)).abrirSubastaDeCupo(
        idempotencyKey: claves.claveDe(formulario),
        entradaSubasta: EntradaSubasta(
          grupoId: p.grupoId,
          cupoId: p.cupoId,
          montoBase: Dinero(monto: p.montoBase, moneda: DineroMonedaEnum.BOB),
          horas: switch (p.horas) {
            48 => EntradaSubastaHorasEnum.number48,
            72 => EntradaSubastaHorasEnum.number72,
            _ => EntradaSubastaHorasEnum.number24,
          },
        ),
      );
      claves.cerrar(formulario);
      ref.invalidate(subastaDelCupoProvider(p.cupoId));
      state = AsyncData(r.data);
    } catch (e) {
      final error = errorDeDominio(e);
      if (error is ErrorDeApi && error.estado == 422) claves.cerrar(formulario);
      state = AsyncError(error, StackTrace.current);
    }
  }
}

final abrirSubastaProvider =
    AsyncNotifierProvider.autoDispose<AbrirSubasta, VistaDeSubasta?>(
      AbrirSubasta.new,
    );

/// Pujar. La misma clave para el mismo monto (un 202 «la billetera no respondió» se
/// reanuda repitiendo); otro monto es otra puja.
class Pujar extends AsyncNotifier<SalidaPuja?> {
  @override
  Future<SalidaPuja?> build() async {
    ref.watch(generacionDeSesionProvider);
    return null;
  }

  Future<void> enviar(String subastaId, String monto) async {
    if (state.isLoading) return;
    final claves = ref.read(idempotenciaProvider.notifier);
    final formulario = 'cu64-puja-$subastaId-$monto';
    state = const AsyncLoading<SalidaPuja?>();
    try {
      final r = await DefaultApi(ref.read(dioProvider)).pujarEnSubasta(
        subastaId: subastaId,
        idempotencyKey: claves.claveDe(formulario),
        entradaPuja: EntradaPuja(
          monto: Dinero(monto: monto, moneda: DineroMonedaEnum.BOB),
          aceptoReglamento: true,
        ),
      );
      if (r.data?.estado != SalidaPujaEstadoEnum.PENDIENTE) {
        claves.cerrar(formulario);
      }
      state = AsyncData(r.data);
    } catch (e) {
      final error = errorDeDominio(e);
      if (error is ErrorDeApi && error.estado == 422) claves.cerrar(formulario);
      state = AsyncError(error, StackTrace.current);
    } finally {
      ref.invalidate(subastaProvider(subastaId));
      ref.invalidate(subastasAbiertasProvider);
    }
  }
}

final pujarProvider = AsyncNotifierProvider.autoDispose<Pujar, SalidaPuja?>(
  Pujar.new,
);
